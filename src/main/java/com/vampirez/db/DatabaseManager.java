package com.vampirez.db;

import com.vampirez.VampireZPlugin;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Owns the SQLite connection pool (HikariCP) and runs schema migrations on enable.
 * All managers that need DB access receive this instance via constructor.
 *
 * <p>The pool is small (3 connections) — SQLite serializes writers anyway, and most
 * VampireZ DB activity is bursty per-game-end batches, not steady traffic.
 */
public class DatabaseManager {

    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);

    private final VampireZPlugin plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(VampireZPlugin plugin) {
        this.plugin = plugin;
    }

    /** Open the pool and run schema migrations. Throws {@link RuntimeException} on fatal failure. */
    public void start() {
        File dbFile = new File(plugin.getDataFolder(), "vampirez.db");

        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        cfg.setDriverClassName("org.sqlite.JDBC");
        cfg.setPoolName("VampireZ-SQLite");
        cfg.setMaximumPoolSize(3);
        cfg.setConnectionTestQuery("SELECT 1");
        // SQLite-specific: WAL mode + reasonable sync = good throughput, durable enough for stats.
        cfg.addDataSourceProperty("journal_mode", "WAL");
        cfg.addDataSourceProperty("synchronous", "NORMAL");

        this.dataSource = new HikariDataSource(cfg);

        runMigrations();
        log.info("DatabaseManager ready — vampirez.db ({} bytes)", dbFile.length());
    }

    /** Close the pool. Idempotent. */
    public void stop() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            dataSource = null;
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) throw new SQLException("DatabaseManager not started");
        return dataSource.getConnection();
    }

    private void runMigrations() {
        try (Connection c = getConnection(); Statement s = c.createStatement()) {
            // V1: player_stats table.
            s.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS player_stats (
                        uuid    TEXT PRIMARY KEY NOT NULL,
                        name    TEXT,
                        kills   INTEGER NOT NULL DEFAULT 0,
                        wins    INTEGER NOT NULL DEFAULT 0,
                        losses  INTEGER NOT NULL DEFAULT 0
                    )
                    """);
            // Indexes for leaderboard queries.
            s.executeUpdate("CREATE INDEX IF NOT EXISTS idx_player_stats_kills ON player_stats(kills DESC)");
            s.executeUpdate("CREATE INDEX IF NOT EXISTS idx_player_stats_wins  ON player_stats(wins DESC)");
        } catch (SQLException e) {
            log.error("Schema migration failed", e);
            throw new RuntimeException("DatabaseManager migration failed", e);
        }
    }
}

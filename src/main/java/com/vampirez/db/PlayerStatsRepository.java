package com.vampirez.db;

import com.vampirez.PlayerStatsManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Data access for the {@code player_stats} table. All methods are synchronous and
 * blocking — call them from async tasks ({@link org.bukkit.scheduler.BukkitScheduler#runTaskAsynchronously}).
 */
public class PlayerStatsRepository {

    private final DatabaseManager db;

    public PlayerStatsRepository(DatabaseManager db) {
        this.db = db;
    }

    /** Load every row into memory. Returns a fresh insertion-ordered map. */
    public Map<UUID, PlayerStatsManager.PlayerStats> loadAll() throws SQLException {
        Map<UUID, PlayerStatsManager.PlayerStats> out = new LinkedHashMap<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT uuid, name, kills, wins, losses FROM player_stats");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                try {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    PlayerStatsManager.PlayerStats s = new PlayerStatsManager.PlayerStats();
                    s.name   = rs.getString("name");
                    s.kills  = rs.getInt("kills");
                    s.wins   = rs.getInt("wins");
                    s.losses = rs.getInt("losses");
                    out.put(uuid, s);
                } catch (IllegalArgumentException ignored) {
                    // Skip malformed UUID rows.
                }
            }
        }
        return out;
    }

    /** Upsert one row. */
    public void save(UUID uuid, PlayerStatsManager.PlayerStats s) throws SQLException {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("""
                     INSERT INTO player_stats(uuid, name, kills, wins, losses)
                     VALUES (?, ?, ?, ?, ?)
                     ON CONFLICT(uuid) DO UPDATE SET
                         name = excluded.name,
                         kills = excluded.kills,
                         wins = excluded.wins,
                         losses = excluded.losses
                     """)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, s.name);
            ps.setInt(3, s.kills);
            ps.setInt(4, s.wins);
            ps.setInt(5, s.losses);
            ps.executeUpdate();
        }
    }

    /** Bulk upsert — used for periodic saves and shutdown flush. */
    public void saveAll(Map<UUID, PlayerStatsManager.PlayerStats> entries) throws SQLException {
        if (entries.isEmpty()) return;
        try (Connection c = db.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement("""
                    INSERT INTO player_stats(uuid, name, kills, wins, losses)
                    VALUES (?, ?, ?, ?, ?)
                    ON CONFLICT(uuid) DO UPDATE SET
                        name = excluded.name,
                        kills = excluded.kills,
                        wins = excluded.wins,
                        losses = excluded.losses
                    """)) {
                for (Map.Entry<UUID, PlayerStatsManager.PlayerStats> e : entries.entrySet()) {
                    ps.setString(1, e.getKey().toString());
                    ps.setString(2, e.getValue().name);
                    ps.setInt(3, e.getValue().kills);
                    ps.setInt(4, e.getValue().wins);
                    ps.setInt(5, e.getValue().losses);
                    ps.addBatch();
                }
                ps.executeBatch();
                c.commit();
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    /** Count rows — used to detect first-run for YAML migration. */
    public long count() throws SQLException {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM player_stats");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }
}

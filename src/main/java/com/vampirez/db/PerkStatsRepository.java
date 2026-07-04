package com.vampirez.db;

import com.vampirez.PerkStatsManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Data access for the {@code perk_stats} table (per-perk pick/win/loss telemetry).
 * All methods are synchronous and blocking — call them from async tasks, mirroring
 * {@link PlayerStatsRepository}.
 */
public class PerkStatsRepository {

    private final DatabaseManager db;

    public PerkStatsRepository(DatabaseManager db) {
        this.db = db;
    }

    /** Load every row into memory. Returns a fresh insertion-ordered map keyed by perk id. */
    public Map<String, PerkStatsManager.PerkStats> loadAll() throws SQLException {
        Map<String, PerkStatsManager.PerkStats> out = new LinkedHashMap<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT perk_id, picks, wins, losses FROM perk_stats");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                PerkStatsManager.PerkStats s = new PerkStatsManager.PerkStats();
                s.picks  = rs.getInt("picks");
                s.wins   = rs.getInt("wins");
                s.losses = rs.getInt("losses");
                out.put(rs.getString("perk_id"), s);
            }
        }
        return out;
    }

    /** Bulk upsert — used for game-end saves and shutdown flush. */
    public void saveAll(Map<String, PerkStatsManager.PerkStats> entries) throws SQLException {
        if (entries.isEmpty()) return;
        try (Connection c = db.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement("""
                    INSERT INTO perk_stats(perk_id, picks, wins, losses)
                    VALUES (?, ?, ?, ?)
                    ON CONFLICT(perk_id) DO UPDATE SET
                        picks = excluded.picks,
                        wins = excluded.wins,
                        losses = excluded.losses
                    """)) {
                for (Map.Entry<String, PerkStatsManager.PerkStats> e : entries.entrySet()) {
                    ps.setString(1, e.getKey());
                    ps.setInt(2, e.getValue().picks);
                    ps.setInt(3, e.getValue().wins);
                    ps.setInt(4, e.getValue().losses);
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
}

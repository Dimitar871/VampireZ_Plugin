package com.vampirez;

import com.vampirez.api.event.PlayerPerkGainedEvent;
import com.vampirez.api.event.VampireZGameEndEvent;
import com.vampirez.db.PerkStatsRepository;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Per-perk balance telemetry: how often each perk is picked and how the teams
 * holding it fare. Persisted to SQLite ({@code perk_stats} table), mirroring
 * {@link PlayerStatsManager}; the legacy {@code perk-stats.yml} is imported once
 * on first run. Powers {@code /vz perkstats}.
 */
public class PerkStatsManager implements Listener {

    public static class PerkStats {
        public int picks;
        public int wins;
        public int losses;

        public int getGamesPlayed() { return wins + losses; }

        public double getWinRate() {
            int played = getGamesPlayed();
            return played == 0 ? 0.0 : (double) wins / played;
        }
    }

    private static final Logger log = LoggerFactory.getLogger(PerkStatsManager.class);

    private final JavaPlugin plugin;
    private final GameManager gameManager;
    private final PerkManager perkManager;
    private final PerkStatsRepository repo;
    /** Legacy YAML file — read once on first run for migration into SQLite. */
    private final File legacyStatsFile;
    final Map<String, PerkStats> stats = new LinkedHashMap<>();

    public PerkStatsManager(JavaPlugin plugin, GameManager gameManager, PerkManager perkManager,
                            PerkStatsRepository repo) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.perkManager = perkManager;
        this.repo = repo;
        this.legacyStatsFile = new File(plugin.getDataFolder(), "perk-stats.yml");
        load();
    }

    /** Test-mode constructor — no persistence; tests populate via the record* helpers. */
    PerkStatsManager(JavaPlugin plugin, GameManager gameManager, PerkManager perkManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.perkManager = perkManager;
        this.repo = null;
        this.legacyStatsFile = new File("perk-stats.yml");
    }

    // ===== Bukkit event handlers =====

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPerkGained(PlayerPerkGainedEvent event) {
        // Only count intentional player picks, not internal reapplications
        if (event.getSource() == PlayerPerkGainedEvent.Source.INTERNAL) return;
        recordPick(event.getPerkId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onGameEnd(VampireZGameEndEvent event) {
        // Admin-stopped games don't count toward win/loss
        if (event.getWinner() != VampireZGameEndEvent.Winner.STOPPED) {
            boolean humansWon = event.getWinner() == VampireZGameEndEvent.Winner.HUMANS;
            Set<UUID> winners = humansWon ? gameManager.getHumanTeam() : gameManager.getVampireTeam();
            Set<UUID> losers = humansWon ? gameManager.getVampireTeam() : gameManager.getHumanTeam();

            for (UUID uuid : winners) {
                for (Perk perk : perkManager.getPlayerPerks(uuid)) {
                    recordWin(perk.getId());
                }
            }
            for (UUID uuid : losers) {
                for (Perk perk : perkManager.getPlayerPerks(uuid)) {
                    recordLoss(perk.getId());
                }
            }
        }
        save();
    }

    // ===== Stat recording (package-visible for tests) =====

    void recordPick(String perkId) {
        stats.computeIfAbsent(perkId, k -> new PerkStats()).picks++;
    }

    void recordWin(String perkId) {
        stats.computeIfAbsent(perkId, k -> new PerkStats()).wins++;
    }

    void recordLoss(String perkId) {
        stats.computeIfAbsent(perkId, k -> new PerkStats()).losses++;
    }

    // ===== Queries (for /vz perkstats) =====

    public PerkStats getStats(String perkId) {
        return stats.getOrDefault(perkId, new PerkStats());
    }

    /** Most-picked perks first. Only perks with at least one pick. */
    public List<Map.Entry<String, PerkStats>> getTopByPicks(int limit) {
        return stats.entrySet().stream()
                .filter(e -> e.getValue().picks > 0)
                .sorted(Comparator.<Map.Entry<String, PerkStats>>comparingInt(
                        e -> e.getValue().picks).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Best win-rate first, requiring a minimum number of finished games so a
     * single lucky round doesn't top the chart.
     */
    public List<Map.Entry<String, PerkStats>> getTopByWinRate(int limit, int minGames) {
        return stats.entrySet().stream()
                .filter(e -> e.getValue().getGamesPlayed() >= Math.max(1, minGames))
                .sorted(Comparator.<Map.Entry<String, PerkStats>>comparingDouble(
                        e -> e.getValue().getWinRate()).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    // ===== Persistence (SQLite via PerkStatsRepository) =====

    /** Persist asynchronously: snapshot on the main thread, write on a worker. */
    public void save() {
        if (repo == null) return; // test mode
        Map<String, PerkStats> snapshot = new LinkedHashMap<>(stats);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                repo.saveAll(snapshot);
            } catch (SQLException e) {
                log.warn("Failed to save perk stats", e);
            }
        });
    }

    /** Synchronous save — used on plugin disable when the scheduler is shutting down. */
    public void saveBlocking() {
        if (repo == null) return;
        try {
            repo.saveAll(new LinkedHashMap<>(stats));
        } catch (SQLException e) {
            log.warn("Failed to flush perk stats on shutdown", e);
        }
    }

    private void load() {
        if (repo == null) return;
        try {
            stats.putAll(repo.loadAll());
        } catch (SQLException e) {
            log.error("Failed to load perk stats from DB", e);
            return;
        }

        // First-run migration: if DB is empty AND the legacy YAML exists, import it.
        if (stats.isEmpty() && legacyStatsFile.exists()) {
            migrateFromYaml();
        }
    }

    private void migrateFromYaml() {
        log.info("Migrating perk stats from {} to SQLite...", legacyStatsFile.getName());
        YamlConfiguration config = YamlConfiguration.loadConfiguration(legacyStatsFile);
        int migrated = 0;
        for (String key : config.getKeys(false)) {
            PerkStats s = new PerkStats();
            s.picks  = config.getInt(key + ".times-chosen", 0);
            s.wins   = config.getInt(key + ".wins", 0);
            s.losses = config.getInt(key + ".losses", 0);
            stats.put(key, s);
            migrated++;
        }
        if (migrated > 0) {
            saveBlocking(); // synchronous so we don't lose data if the next op fails
            File backup = new File(plugin.getDataFolder(), "perk-stats.yml.migrated");
            if (legacyStatsFile.renameTo(backup)) {
                log.info("Migrated {} perk records. Old YAML kept as {}", migrated, backup.getName());
            } else {
                log.info("Migrated {} perk records (could not rename old YAML).", migrated);
            }
        }
    }
}

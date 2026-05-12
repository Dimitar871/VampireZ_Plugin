package com.vampirez;

import com.vampirez.api.event.VampireZGameEndEvent;
import com.vampirez.db.PlayerStatsRepository;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class PlayerStatsManager implements Listener {

    public static class PlayerStats {
        public String name;
        public int kills;
        public int wins;
        public int losses;

        public int getGamesPlayed() { return wins + losses; }

        public double getWinRate() {
            int played = getGamesPlayed();
            return played == 0 ? 0.0 : (double) wins / played;
        }
    }

    private static final Logger log = LoggerFactory.getLogger(PlayerStatsManager.class);

    private final JavaPlugin plugin;
    private final GameManager gameManager;
    private final PlayerStatsRepository repo;
    /** Legacy YAML file — read once on first run for migration into SQLite. */
    private final File legacyStatsFile;
    final Map<UUID, PlayerStats> stats = new LinkedHashMap<>();

    public PlayerStatsManager(JavaPlugin plugin, GameManager gameManager, PlayerStatsRepository repo) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.repo = repo;
        this.legacyStatsFile = new File(plugin.getDataFolder(), "player-stats.yml");
        load();
    }

    /** Two-arg constructor kept so existing test fixtures that don't need persistence still work. */
    PlayerStatsManager(JavaPlugin plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.repo = null;
        this.legacyStatsFile = new File(plugin.getDataFolder(), "player-stats.yml");
        // No load — tests populate stats via record* helpers directly.
    }

    // ===== Bukkit event handlers =====

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (gameManager.getState() != GameState.ACTIVE) return;
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer != null && !killer.equals(victim) && gameManager.isInGame(killer.getUniqueId())) {
            recordKill(killer.getUniqueId(), killer.getName());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onGameEnd(VampireZGameEndEvent event) {
        if (event.getWinner() != VampireZGameEndEvent.Winner.STOPPED) {
            boolean humansWon = event.getWinner() == VampireZGameEndEvent.Winner.HUMANS;
            Set<UUID> winners = humansWon ? gameManager.getHumanTeam() : gameManager.getVampireTeam();
            Set<UUID> losers  = humansWon ? gameManager.getVampireTeam() : gameManager.getHumanTeam();

            for (UUID uuid : winners) {
                Player p = Bukkit.getPlayer(uuid);
                recordWin(uuid, p != null ? p.getName() : null);
            }
            for (UUID uuid : losers) {
                Player p = Bukkit.getPlayer(uuid);
                recordLoss(uuid, p != null ? p.getName() : null);
            }
        }
        save();
    }

    // ===== Stat recording (package-visible for tests) =====

    void recordKill(UUID uuid, String name) {
        PlayerStats s = stats.computeIfAbsent(uuid, k -> new PlayerStats());
        if (name != null) s.name = name;
        s.kills++;
    }

    void recordWin(UUID uuid, String name) {
        PlayerStats s = stats.computeIfAbsent(uuid, k -> new PlayerStats());
        if (name != null) s.name = name;
        s.wins++;
    }

    void recordLoss(UUID uuid, String name) {
        PlayerStats s = stats.computeIfAbsent(uuid, k -> new PlayerStats());
        if (name != null) s.name = name;
        s.losses++;
    }

    // ===== Queries =====

    public PlayerStats getStats(UUID uuid) {
        return stats.getOrDefault(uuid, new PlayerStats());
    }

    public List<Map.Entry<UUID, PlayerStats>> getTopByKills(int limit) {
        return topN(limit, Comparator.<Map.Entry<UUID, PlayerStats>>comparingInt(
                e -> e.getValue().kills).reversed());
    }

    public List<Map.Entry<UUID, PlayerStats>> getTopByWins(int limit) {
        return topN(limit, Comparator.<Map.Entry<UUID, PlayerStats>>comparingInt(
                e -> e.getValue().wins).reversed());
    }

    public List<Map.Entry<UUID, PlayerStats>> getTopByWinRate(int limit) {
        return stats.entrySet().stream()
                .filter(e -> e.getValue().getGamesPlayed() > 0)
                .sorted(Comparator.<Map.Entry<UUID, PlayerStats>>comparingDouble(
                        e -> e.getValue().getWinRate()).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    private List<Map.Entry<UUID, PlayerStats>> topN(int limit,
            Comparator<Map.Entry<UUID, PlayerStats>> comparator) {
        return stats.entrySet().stream()
                .filter(e -> e.getValue().kills > 0 || e.getValue().getGamesPlayed() > 0)
                .sorted(comparator)
                .limit(limit)
                .collect(Collectors.toList());
    }

    // ===== Persistence (SQLite via PlayerStatsRepository) =====

    /** Persist all stats to disk asynchronously. Snapshot on the main thread, write on a worker. */
    public void save() {
        if (repo == null) return; // test mode
        // Snapshot on the calling (main) thread to keep the async copy consistent.
        Map<UUID, PlayerStats> snapshot = new LinkedHashMap<>(stats);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                repo.saveAll(snapshot);
            } catch (SQLException e) {
                log.warn("Failed to save player stats", e);
            }
        });
    }

    /** Synchronous save — used on plugin disable when the scheduler is shutting down. */
    public void saveBlocking() {
        if (repo == null) return;
        try {
            repo.saveAll(new LinkedHashMap<>(stats));
        } catch (SQLException e) {
            log.warn("Failed to flush player stats on shutdown", e);
        }
    }

    void load() {
        if (repo == null) return;
        try {
            stats.putAll(repo.loadAll());
        } catch (SQLException e) {
            log.error("Failed to load player stats from DB", e);
            return;
        }

        // First-run migration: if DB is empty AND the legacy YAML exists, import it.
        if (stats.isEmpty() && legacyStatsFile.exists()) {
            migrateFromYaml();
        }
    }

    private void migrateFromYaml() {
        log.info("Migrating player stats from {} to SQLite...", legacyStatsFile.getName());
        YamlConfiguration config = YamlConfiguration.loadConfiguration(legacyStatsFile);
        int migrated = 0;
        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                PlayerStats s = new PlayerStats();
                s.name   = config.getString(key + ".name");
                s.kills  = config.getInt(key + ".kills", 0);
                s.wins   = config.getInt(key + ".wins", 0);
                s.losses = config.getInt(key + ".losses", 0);
                stats.put(uuid, s);
                migrated++;
            } catch (IllegalArgumentException ignored) {}
        }
        if (migrated > 0) {
            saveBlocking(); // synchronous so we don't lose data if next op fails
            // Rename the YAML so we don't re-migrate on next boot.
            File backup = new File(plugin.getDataFolder(), "player-stats.yml.migrated");
            if (legacyStatsFile.renameTo(backup)) {
                log.info("Migrated {} player records. Old YAML kept as {}", migrated, backup.getName());
            } else {
                log.info("Migrated {} player records (could not rename old YAML).", migrated);
            }
        }
    }
}

package com.vampirez;

import com.vampirez.api.event.VampireZGameEndEvent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
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

    private final JavaPlugin plugin;
    private final GameManager gameManager;
    private final File statsFile;
    final Map<UUID, PlayerStats> stats = new LinkedHashMap<>();

    public PlayerStatsManager(JavaPlugin plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.statsFile = new File(plugin.getDataFolder(), "player-stats.yml");
        load();
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

    // ===== Persistence =====

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerStats> entry : stats.entrySet()) {
            String key = entry.getKey().toString();
            PlayerStats s = entry.getValue();
            if (s.name != null) config.set(key + ".name", s.name);
            config.set(key + ".kills", s.kills);
            config.set(key + ".wins", s.wins);
            config.set(key + ".losses", s.losses);
        }
        try {
            config.save(statsFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save player-stats.yml: " + e.getMessage());
        }
    }

    void load() {
        if (!statsFile.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(statsFile);
        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                PlayerStats s = new PlayerStats();
                s.name   = config.getString(key + ".name");
                s.kills  = config.getInt(key + ".kills", 0);
                s.wins   = config.getInt(key + ".wins", 0);
                s.losses = config.getInt(key + ".losses", 0);
                stats.put(uuid, s);
            } catch (IllegalArgumentException ignored) {}
        }
    }
}

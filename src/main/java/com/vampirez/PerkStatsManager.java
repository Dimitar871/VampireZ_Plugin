package com.vampirez;

import com.vampirez.api.event.PlayerPerkGainedEvent;
import com.vampirez.api.event.VampireZGameEndEvent;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PerkStatsManager implements Listener {

    private static final Logger log = LoggerFactory.getLogger(PerkStatsManager.class);

    private final JavaPlugin plugin;
    private final GameManager gameManager;
    private final PerkManager perkManager;
    private final File statsFile;

    // perkId -> [timesChosen, wins, losses]
    private final Map<String, int[]> stats = new LinkedHashMap<>();

    public PerkStatsManager(JavaPlugin plugin, GameManager gameManager, PerkManager perkManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.perkManager = perkManager;
        this.statsFile = new File(plugin.getDataFolder(), "perk-stats.yml");
        load();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPerkGained(PlayerPerkGainedEvent event) {
        // Only count intentional player picks, not internal reapplications
        if (event.getSource() == PlayerPerkGainedEvent.Source.INTERNAL) return;
        stats.computeIfAbsent(event.getPerkId(), k -> new int[3])[0]++;
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
                    stats.computeIfAbsent(perk.getId(), k -> new int[3])[1]++;
                }
            }
            for (UUID uuid : losers) {
                for (Perk perk : perkManager.getPlayerPerks(uuid)) {
                    stats.computeIfAbsent(perk.getId(), k -> new int[3])[2]++;
                }
            }
        }
        save();
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        // Sort by times chosen descending for readability
        List<Map.Entry<String, int[]>> sorted = new ArrayList<>(stats.entrySet());
        sorted.sort((a, b) -> Integer.compare(b.getValue()[0], a.getValue()[0]));
        for (Map.Entry<String, int[]> entry : sorted) {
            String key = entry.getKey();
            int[] d = entry.getValue();
            config.set(key + ".times-chosen", d[0]);
            config.set(key + ".wins", d[1]);
            config.set(key + ".losses", d[2]);
            int played = d[1] + d[2];
            config.set(key + ".win-rate", played == 0 ? 0.0 : Math.round(100.0 * d[1] / played) / 100.0);
        }
        try {
            config.save(statsFile);
        } catch (IOException e) {
            log.warn("Failed to save perk-stats.yml: {}", e.getMessage());
        }
    }

    private void load() {
        if (!statsFile.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(statsFile);
        for (String key : config.getKeys(false)) {
            int timesChosen = config.getInt(key + ".times-chosen", 0);
            int wins = config.getInt(key + ".wins", 0);
            int losses = config.getInt(key + ".losses", 0);
            stats.put(key, new int[]{timesChosen, wins, losses});
        }
    }
}

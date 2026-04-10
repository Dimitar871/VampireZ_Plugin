package com.vampirez;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EconomyManager {

    private final JavaPlugin plugin;
    private final Map<UUID, Integer> gold = new HashMap<>();
    private final Map<UUID, Map<UUID, Long>> recentDamage = new HashMap<>(); // victim -> (attacker -> timestamp)
    private BukkitTask incomeTask;

    private int passiveIncomeAmount;
    private int passiveIncomeIntervalTicks;
    private int killReward;
    private int assistReward;
    private long assistTimeWindowMs;

    public EconomyManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        passiveIncomeAmount = plugin.getConfig().getInt("economy.passive-income-amount", 2);
        passiveIncomeIntervalTicks = plugin.getConfig().getInt("economy.passive-income-interval-ticks", 200);
        killReward = plugin.getConfig().getInt("economy.kill-reward", 15);
        assistReward = plugin.getConfig().getInt("economy.assist-reward", 5);
        assistTimeWindowMs = plugin.getConfig().getLong("economy.assist-time-window-ms", 10000);
    }

    public void startPassiveIncome(GameManager gameManager) {
        incomeTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (gameManager.getState() != GameState.ACTIVE) return;
            for (Player player : gameManager.getJoinedOnlinePlayers()) {
                addGold(player.getUniqueId(), passiveIncomeAmount);
            }
        }, passiveIncomeIntervalTicks, passiveIncomeIntervalTicks);
    }

    public void stopPassiveIncome() {
        if (incomeTask != null) {
            incomeTask.cancel();
            incomeTask = null;
        }
    }

    public void recordDamage(UUID victim, UUID attacker) {
        recentDamage.computeIfAbsent(victim, k -> new HashMap<>()).put(attacker, System.currentTimeMillis());
    }

    public void awardKillRewards(UUID killer, UUID victim) {
        addGold(killer, killReward);
        Player killerPlayer = Bukkit.getPlayer(killer);
        if (killerPlayer != null) {
            killerPlayer.sendMessage(ChatColor.GOLD + "+" + killReward + " gold " + ChatColor.GRAY + "(kill)");
        }

        // Award assists
        Map<UUID, Long> damagers = recentDamage.remove(victim);
        if (damagers != null) {
            long now = System.currentTimeMillis();
            for (Map.Entry<UUID, Long> entry : damagers.entrySet()) {
                UUID assisterId = entry.getKey();
                if (!assisterId.equals(killer) && (now - entry.getValue()) <= assistTimeWindowMs) {
                    addGold(assisterId, assistReward);
                    Player assister = Bukkit.getPlayer(assisterId);
                    if (assister != null) {
                        assister.sendMessage(ChatColor.GOLD + "+" + assistReward + " gold " + ChatColor.GRAY + "(assist)");
                    }
                }
            }
        }
    }

    public void addGold(UUID uuid, int amount) {
        gold.merge(uuid, amount, Integer::sum);
    }

    public boolean removeGold(UUID uuid, int amount) {
        int current = getGold(uuid);
        if (current < amount) return false;
        gold.put(uuid, current - amount);
        return true;
    }

    public int getGold(UUID uuid) {
        return gold.getOrDefault(uuid, 0);
    }

    public boolean hasEnough(UUID uuid, int amount) {
        return getGold(uuid) >= amount;
    }

    public void resetAll() {
        gold.clear();
        recentDamage.clear();
    }

    public void resetPlayer(UUID uuid) {
        gold.remove(uuid);
        recentDamage.remove(uuid);
    }
}

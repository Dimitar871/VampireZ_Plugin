package com.vampirez;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

public class DayNightManager {

    private final JavaPlugin plugin;
    private GameManager gameManager;
    private BukkitTask cycleTask;
    private boolean isNight = false;
    private int ticksInPhase = 0;

    private boolean enabled;
    private int dayDurationTicks;
    private int nightDurationTicks;

    public DayNightManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        enabled = plugin.getConfig().getBoolean("day-night.enabled", true);
        dayDurationTicks = plugin.getConfig().getInt("day-night.day-duration-ticks", 7200);
        nightDurationTicks = plugin.getConfig().getInt("day-night.night-duration-ticks", 4800);
    }

    public void setGameManager(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public void startCycle() {
        if (!enabled) return;
        isNight = false;
        ticksInPhase = 0;

        // Set world to day
        setWorldTime(1000);

        cycleTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (gameManager == null || gameManager.getState() != GameState.ACTIVE) return;

            ticksInPhase++;

            if (!isNight && ticksInPhase >= dayDurationTicks) {
                // Transition to night
                isNight = true;
                ticksInPhase = 0;
                setWorldTime(13000);
                onNightFall();
            } else if (isNight && ticksInPhase >= nightDurationTicks) {
                // Transition to day
                isNight = false;
                ticksInPhase = 0;
                setWorldTime(1000);
                onDayBreak();
            }
        }, 1L, 1L);
    }

    public void stopCycle() {
        if (cycleTask != null) {
            cycleTask.cancel();
            cycleTask = null;
        }
        // Remove all day/night effects
        removeAllEffects();
    }

    private void onNightFall() {
        String msg = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("messages.night-fall", "&4&lNight has fallen! Vampires grow stronger..."));

        if (gameManager != null) {
            for (Player player : gameManager.getJoinedOnlinePlayers()) {
                player.sendMessage(msg);
                player.playSound(player.getLocation(), Sound.ENTITY_WOLF_GROWL, 1.0f, 0.5f);
            }
        }

        applyNightEffects();
    }

    private void onDayBreak() {
        String msg = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("messages.day-break", "&e&lThe sun rises! Vampires are weakened..."));

        if (gameManager != null) {
            for (Player player : gameManager.getJoinedOnlinePlayers()) {
                player.sendMessage(msg);
                player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_AMBIENT, 1.0f, 1.0f);
            }
        }

        applyDayEffects();
    }

    private void applyNightEffects() {
        if (gameManager == null) return;
        for (UUID uuid : gameManager.getVampireTeam()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                // Remove day debuffs
                player.removePotionEffect(PotionEffectType.SLOWNESS);
                // Apply night buffs (Speed only — damage scaling handled by perks)
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 999999, 0, false, false));
            }
        }
    }

    private void applyDayEffects() {
        if (gameManager == null) return;
        for (UUID uuid : gameManager.getVampireTeam()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                // Remove night buffs
                player.removePotionEffect(PotionEffectType.SPEED);
                // Apply day debuffs
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 999999, 0, false, false));
            }
        }
    }

    public void applyEffectsToPlayer(Player player) {
        if (!enabled) return;
        if (gameManager == null) return;
        if (!gameManager.getVampireTeam().contains(player.getUniqueId())) return;

        if (isNight) {
            player.removePotionEffect(PotionEffectType.SLOWNESS);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 999999, 0, false, false));
        } else {
            player.removePotionEffect(PotionEffectType.SPEED);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 999999, 0, false, false));
        }
    }

    private void removeAllEffects() {
        if (gameManager == null) return;
        for (UUID uuid : gameManager.getVampireTeam()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.removePotionEffect(PotionEffectType.SPEED);
                player.removePotionEffect(PotionEffectType.STRENGTH);
                player.removePotionEffect(PotionEffectType.SLOWNESS);
            }
        }
    }

    private void setWorldTime(long time) {
        // Only change time in the arena world, not survival worlds
        if (gameManager != null && gameManager.getHumanSpawn() != null) {
            gameManager.getHumanSpawn().getWorld().setTime(time);
        }
    }

    public boolean isNight() { return isNight; }
    public boolean isEnabled() { return enabled; }
}

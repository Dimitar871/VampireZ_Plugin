package com.vampirez;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VampireLeapListener implements Listener {

    private final JavaPlugin plugin;
    private final GameManager gameManager;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_MS = 8000;

    public VampireLeapListener(JavaPlugin plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (player.getInventory().getItemInMainHand().getType() != Material.GHAST_TEAR) return;

        String itemName = player.getInventory().getItemInMainHand().getItemMeta() != null
                ? player.getInventory().getItemInMainHand().getItemMeta().getDisplayName() : "";
        if (!itemName.contains("Vampire Leap")) return;

        if (gameManager.getState() != GameState.ACTIVE) return;
        if (!gameManager.getVampireTeam().contains(player.getUniqueId())) return;

        event.setCancelled(true);

        // Check cooldown
        long now = System.currentTimeMillis();
        Long lastUsed = cooldowns.get(player.getUniqueId());
        long effectiveCooldown = Perk.getEffectiveCooldown(player, COOLDOWN_MS);
        if (lastUsed != null && (now - lastUsed) < effectiveCooldown) {
            long remaining = (effectiveCooldown - (now - lastUsed)) / 1000 + 1;
            player.sendMessage(ChatColor.RED + "Leap on cooldown! " + remaining + "s remaining");
            return;
        }

        cooldowns.put(player.getUniqueId(), now);

        // Launch forward
        Vector direction = player.getLocation().getDirection();
        direction.multiply(1.5);
        direction.setY(Math.max(direction.getY(), 0.4));
        player.setVelocity(direction);

        // Launch burst particles
        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 50, 0.5, 0.5, 0.5, 0.3);
        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 0.5, 0), 25, 0.4, 0.2, 0.4, 0,
                new Particle.DustOptions(Color.fromRGB(100, 0, 150), 1.8f));
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.8f, 1.5f);

        // Trailing particle effect during leap
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!player.isOnline() || ticks >= 12 || player.isOnGround()) {
                    // Landing burst
                    if (player.isOnline()) {
                        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 30, 0.3, 0.1, 0.3, 0.2);
                        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 0.3, 0), 15, 0.5, 0.1, 0.5, 0,
                                new Particle.DustOptions(Color.fromRGB(80, 0, 120), 1.5f));
                    }
                    cancel();
                    return;
                }
                // Spiral trail
                double angle = ticks * 0.8;
                double radius = 0.4;
                double offsetX = Math.cos(angle) * radius;
                double offsetZ = Math.sin(angle) * radius;
                player.getWorld().spawnParticle(Particle.DUST,
                        player.getLocation().add(offsetX, 0.5, offsetZ), 3, 0.05, 0.05, 0.05, 0,
                        new Particle.DustOptions(Color.fromRGB(130, 0, 180), 1.2f));
                player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 5, 0.2, 0.3, 0.2, 0.05);
                ticks++;
            }
        }.runTaskTimer(plugin, 2L, 1L);
    }

    public void clearCooldowns() {
        cooldowns.clear();
    }
}

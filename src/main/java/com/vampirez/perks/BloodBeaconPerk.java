package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import com.vampirez.VampireZPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class BloodBeaconPerk extends Perk {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, BeaconData> activeBeacons = new HashMap<>();
    private static final long COOLDOWN_MS = 60000;
    private static final int DURATION_TICKS = 20; // 20 seconds (onTick fires every 1s)
    private static final double RADIUS = 8.0;

    private static class BeaconData {
        final Location location;
        int ticksRemaining;

        BeaconData(Location location, int ticks) {
            this.location = location;
            this.ticksRemaining = ticks;
        }
    }

    public BloodBeaconPerk() {
        super("blood_beacon", "Blood Beacon", PerkTier.GOLD, PerkTeam.VAMPIRE,
                Material.BONE,
                "Right-click bone to place a beacon.",
                "Vampires within 8 blocks get Regen I.",
                "Lasts 20s. 60s cooldown.");
    }

    @Override
    public void apply(Player player) {
        ItemStack item = new ItemStack(Material.BONE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.DARK_RED + "Blood Beacon" + ChatColor.GRAY + " (Right-Click)");
            meta.setLore(Arrays.asList(ChatColor.GRAY + "Place a healing beacon", ChatColor.YELLOW + "Cooldown: 60s"));
            item.setItemMeta(meta);
        }
        player.getInventory().addItem(item);
    }

    @Override
    public void remove(Player player) {
        UUID uuid = player.getUniqueId();
        cooldowns.remove(uuid);
        BeaconData data = activeBeacons.remove(uuid);
        if (data != null) {
            data.location.getBlock().setType(Material.AIR);
        }
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.BONE && item.hasItemMeta()
                    && item.getItemMeta().getDisplayName().contains("Blood Beacon")) {
                player.getInventory().remove(item);
            }
        }
    }

    @Override
    public void onInteract(Player player, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.BONE || !item.hasItemMeta()) return;
        if (!item.getItemMeta().getDisplayName().contains("Blood Beacon")) return;

        event.setCancelled(true);
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(uuid);
        if (last != null && (now - last) < getEffectiveCooldown(player, COOLDOWN_MS)) {
            player.sendMessage(ChatColor.RED + "Blood Beacon on cooldown! " + ((getEffectiveCooldown(player, COOLDOWN_MS) - (now - last)) / 1000 + 1) + "s");
            return;
        }

        // Remove existing beacon if any
        BeaconData existing = activeBeacons.remove(uuid);
        if (existing != null) {
            existing.location.getBlock().setType(Material.AIR);
        }

        cooldowns.put(uuid, now);
        Location loc = player.getLocation().getBlock().getLocation();
        loc.getBlock().setType(Material.REDSTONE_BLOCK);
        activeBeacons.put(uuid, new BeaconData(loc, DURATION_TICKS));

        player.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0.5, 1, 0.5), 30, 0.5, 1, 0.5, 0,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(139, 0, 0), 2.0f));
        player.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.5f);
        player.sendMessage(ChatColor.DARK_RED + "Blood Beacon placed! Nearby vampires will regenerate.");
        incrementStat(uuid, "activations");
    }

    @Override
    public void onTick(Player player) {
        UUID uuid = player.getUniqueId();
        BeaconData data = activeBeacons.get(uuid);
        if (data == null) return;

        data.ticksRemaining--;

        // Apply Regen I to nearby teammates
        Location beaconLoc = data.location.clone().add(0.5, 0.5, 0.5);
        VampireZPlugin bbPlugin = (VampireZPlugin) getPlugin();
        if (bbPlugin.getGameManager() == null) return;
        for (Player nearby : bbPlugin.getGameManager().getJoinedOnlinePlayers()) {
            if (!isSameTeam(player, nearby)) continue;
            if (nearby.getLocation().distance(beaconLoc) <= RADIUS) {
                // Use 100-tick duration so Regen I's 50-tick heal timer can fire.
                PotionEffect existingRegen = nearby.getPotionEffect(PotionEffectType.REGENERATION);
                if (existingRegen == null || existingRegen.getDuration() < 30) {
                    nearby.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 0, false, true), true);
                }
            }
        }

        // Particle effect on beacon
        player.getWorld().spawnParticle(Particle.DUST, beaconLoc.add(0, 0.5, 0), 5, 0.3, 0.5, 0.3, 0,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(200, 0, 0), 1.0f));

        if (data.ticksRemaining <= 0) {
            data.location.getBlock().setType(Material.AIR);
            activeBeacons.remove(uuid);
            player.sendMessage(ChatColor.GRAY + "Your Blood Beacon has expired.");
        }
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("activations", "Beacons Placed");
        return labels;
    }
}

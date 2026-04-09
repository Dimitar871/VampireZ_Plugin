package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class CitadelPerk extends Perk {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Location> zoneLocations = new HashMap<>();
    private final Map<UUID, Long> zoneActivations = new HashMap<>();
    private static final long COOLDOWN_MS = 60000;
    private static final long ZONE_DURATION_MS = 8000;
    private static final double ZONE_RADIUS = 5.0;

    public CitadelPerk() {
        super("citadel", "Citadel", PerkTier.PRISMATIC, PerkTeam.HUMAN,
                Material.NETHER_STAR,
                "Right-click: 5-block radius zone,",
                "allies get Resistance I + Regen I (8s, 60s cd)");
    }

    @Override
    public void apply(Player player) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Citadel" + ChatColor.GRAY + " (Right-Click)");
            meta.setLore(Arrays.asList(ChatColor.GRAY + "Create a protective zone", ChatColor.YELLOW + "Cooldown: 60s"));
            item.setItemMeta(meta);
        }
        player.getInventory().addItem(item);
    }

    @Override
    public void remove(Player player) {
        UUID uuid = player.getUniqueId();
        cooldowns.remove(uuid);
        zoneLocations.remove(uuid);
        zoneActivations.remove(uuid);
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.NETHER_STAR && item.hasItemMeta()
                    && item.getItemMeta().getDisplayName().contains("Citadel")) {
                player.getInventory().remove(item);
            }
        }
    }

    @Override
    public void onInteract(Player player, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.NETHER_STAR || !item.hasItemMeta()) return;
        if (!item.getItemMeta().getDisplayName().contains("Citadel")) return;

        event.setCancelled(true);
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(uuid);
        if (last != null && (now - last) < getEffectiveCooldown(player, COOLDOWN_MS)) {
            player.sendMessage(ChatColor.RED + "Citadel on cooldown! " + ((getEffectiveCooldown(player, COOLDOWN_MS) - (now - last)) / 1000 + 1) + "s");
            return;
        }
        cooldowns.put(uuid, now);

        Location loc = player.getLocation().clone();
        zoneLocations.put(uuid, loc);
        zoneActivations.put(uuid, now);

        player.getWorld().playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0, 1, 0), 60, 2, 0.5, 2, 0.05);
        incrementStat(uuid, "activations");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "Citadel activated!");
    }

    @Override
    public void onTick(Player player) {
        UUID uuid = player.getUniqueId();
        Location zoneLoc = zoneLocations.get(uuid);
        Long activation = zoneActivations.get(uuid);
        if (zoneLoc == null || activation == null) return;

        long now = System.currentTimeMillis();
        if ((now - activation) > ZONE_DURATION_MS) {
            zoneLocations.remove(uuid);
            zoneActivations.remove(uuid);
            return;
        }

        // Zone particles
        for (int i = 0; i < 16; i++) {
            double angle = (2 * Math.PI / 16) * i;
            double x = Math.cos(angle) * ZONE_RADIUS;
            double z = Math.sin(angle) * ZONE_RADIUS;
            zoneLoc.getWorld().spawnParticle(Particle.END_ROD, zoneLoc.clone().add(x, 0.3, z), 1, 0, 0, 0, 0);
        }

        for (Entity entity : zoneLoc.getWorld().getNearbyEntities(zoneLoc, ZONE_RADIUS, ZONE_RADIUS, ZONE_RADIUS)) {
            if (!(entity instanceof Player target)) continue;
            if (target.getUniqueId().equals(uuid) || isSameTeam(player, target)) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 30, 0, false, true), true);
                // Use 100-tick duration so Regen I's 50-tick heal timer can fire.
                PotionEffect existingRegen = target.getPotionEffect(PotionEffectType.REGENERATION);
                if (existingRegen == null || existingRegen.getDuration() < 30) {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 0, false, true), true);
                }
                incrementStat(uuid, "allies_buffed");
            }
        }
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("activations", "Activations");
        labels.put("allies_buffed", "Ally Buff Ticks");
        return labels;
    }
}

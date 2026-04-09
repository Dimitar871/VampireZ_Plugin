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
import org.bukkit.attribute.Attribute;

import java.util.*;

public class ConsecratedGroundPerk extends Perk {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Location> zoneLocations = new HashMap<>();
    private final Map<UUID, Long> zoneActivations = new HashMap<>();
    private static final long COOLDOWN_MS = 40000;
    private static final long ZONE_DURATION_MS = 8000;
    private static final double ZONE_RADIUS = 5.0;

    public ConsecratedGroundPerk() {
        super("consecrated_ground", "Consecrated Ground", PerkTier.GOLD, PerkTeam.HUMAN,
                Material.GOLDEN_SHOVEL,
                "Right-click: create holy zone (5 blocks)",
                "Allies regen, enemies take damage (8s, 40s cd)");
    }

    @Override
    public void apply(Player player) {
        ItemStack item = new ItemStack(Material.GOLDEN_SHOVEL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Consecrated Ground" + ChatColor.GRAY + " (Right-Click)");
            meta.setLore(Arrays.asList(ChatColor.GRAY + "Create a holy zone", ChatColor.YELLOW + "Cooldown: 40s"));
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
            if (item != null && item.getType() == Material.GOLDEN_SHOVEL && item.hasItemMeta()
                    && item.getItemMeta().getDisplayName().contains("Consecrated Ground")) {
                player.getInventory().remove(item);
            }
        }
    }

    @Override
    public void onInteract(Player player, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.GOLDEN_SHOVEL || !item.hasItemMeta()) return;
        if (!item.getItemMeta().getDisplayName().contains("Consecrated Ground")) return;

        event.setCancelled(true);
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(uuid);
        if (last != null && (now - last) < getEffectiveCooldown(player, COOLDOWN_MS)) {
            player.sendMessage(ChatColor.RED + "Consecrated Ground on cooldown! " + ((getEffectiveCooldown(player, COOLDOWN_MS) - (now - last)) / 1000 + 1) + "s");
            return;
        }
        cooldowns.put(uuid, now);

        Location loc = player.getLocation().clone();
        zoneLocations.put(uuid, loc);
        zoneActivations.put(uuid, now);

        player.getWorld().playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(0, 1, 0), 80, 2, 0.5, 2, 0.1);
        player.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, 1, 0), 40, 2, 0.5, 2, 0,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 215, 0), 2.0f));
        incrementStat(uuid, "activations");
        player.sendMessage(ChatColor.GOLD + "Consecrated Ground activated!");
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

        // Spawn zone particles - ring pattern + gold dust
        zoneLoc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, zoneLoc.clone().add(0, 0.5, 0), 30, ZONE_RADIUS * 0.4, 0.2, ZONE_RADIUS * 0.4, 0.01);
        // Ring of gold particles
        for (int i = 0; i < 16; i++) {
            double angle = (2 * Math.PI / 16) * i;
            double x = Math.cos(angle) * ZONE_RADIUS;
            double z = Math.sin(angle) * ZONE_RADIUS;
            zoneLoc.getWorld().spawnParticle(Particle.DUST, zoneLoc.clone().add(x, 0.3, z), 2, 0, 0, 0, 0,
                    new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 215, 0), 1.5f));
        }

        for (Entity entity : zoneLoc.getWorld().getNearbyEntities(zoneLoc, ZONE_RADIUS, ZONE_RADIUS, ZONE_RADIUS)) {
            if (!(entity instanceof Player target)) continue;
            if (target.getUniqueId().equals(uuid) || isSameTeam(player, target)) {
                double maxHealth = target.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
                if (target.getHealth() < maxHealth) {
                    target.setHealth(Math.min(target.getHealth() + 1.0, maxHealth));
                    addStat(uuid, "healing_done", 1.0);
                }
                addStat(uuid, "players_affected", 1);
            } else {
                target.damage(1.0);
                addStat(uuid, "damage_dealt", 1.0);
                addStat(uuid, "players_affected", 1);
            }
        }
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("activations", "Activations");
        labels.put("healing_done", "Healing Done");
        labels.put("damage_dealt", "Damage Dealt");
        labels.put("players_affected", "Players Affected");
        return labels;
    }
}

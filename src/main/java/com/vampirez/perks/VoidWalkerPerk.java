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
import org.bukkit.util.Vector;

import java.util.*;

public class VoidWalkerPerk extends Perk {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_MS = 25000;

    public VoidWalkerPerk() {
        super("void_walker", "Void Walker", PerkTier.PRISMATIC, PerkTeam.VAMPIRE,
                Material.ENDER_PEARL,
                "Right-click: teleport to random enemy",
                "within 30 blocks + 3 heart AoE burst",
                "and slowness on arrival (25s cd)");
    }

    @Override
    public void apply(Player player) {
        ItemStack item = new ItemStack(Material.ENDER_PEARL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.DARK_PURPLE + "Void Walker" + ChatColor.GRAY + " (Right-Click)");
            meta.setLore(Arrays.asList(ChatColor.GRAY + "Teleport to enemy + AoE burst", ChatColor.YELLOW + "Cooldown: 25s"));
            item.setItemMeta(meta);
        }
        player.getInventory().addItem(item);
    }

    @Override
    public void remove(Player player) {
        cooldowns.remove(player.getUniqueId());
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.ENDER_PEARL && item.hasItemMeta()
                    && item.getItemMeta().getDisplayName().contains("Void Walker")) {
                player.getInventory().remove(item);
            }
        }
    }

    @Override
    public void onInteract(Player player, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.ENDER_PEARL || !item.hasItemMeta()) return;
        if (!item.getItemMeta().getDisplayName().contains("Void Walker")) return;

        event.setCancelled(true);
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(uuid);
        if (last != null && (now - last) < getEffectiveCooldown(player, COOLDOWN_MS)) {
            player.sendMessage(ChatColor.RED + "Void Walker on cooldown! " + ((getEffectiveCooldown(player, COOLDOWN_MS) - (now - last)) / 1000 + 1) + "s");
            return;
        }

        // Collect enemies within 30 blocks
        List<Player> enemies = new ArrayList<>();
        for (Entity entity : player.getNearbyEntities(30, 30, 30)) {
            if (entity instanceof Player target && !isSameTeam(player, target)) {
                enemies.add(target);
            }
        }

        if (enemies.isEmpty()) {
            player.sendMessage(ChatColor.RED + "No enemies in range!");
            return;
        }

        cooldowns.put(uuid, now);
        Player target = enemies.get(new Random().nextInt(enemies.size()));

        // Black Shield: target blocks the teleport
        if (BlackShieldPerk.isShielded(target.getUniqueId())) {
            BlackShieldPerk.consumeShield(target.getUniqueId(), target, player, "Void Walker");
            return;
        }

        // Teleport behind target
        Location targetLoc = target.getLocation().clone();
        Vector behindDir = targetLoc.getDirection().multiply(-1.5);
        Location teleportLoc = targetLoc.add(behindDir.getX(), 0, behindDir.getZ());
        teleportLoc.setYaw(target.getLocation().getYaw());
        teleportLoc.setPitch(target.getLocation().getPitch());

        // Effects at origin
        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 40, 0.5, 1, 0.5, 0.5);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

        player.teleport(teleportLoc);

        // AoE burst on arrival - 3 hearts to nearby enemies + slowness
        player.getWorld().spawnParticle(Particle.PORTAL, teleportLoc.clone().add(0, 1, 0), 60, 1.5, 1, 1.5, 0.5);
        player.getWorld().spawnParticle(Particle.EXPLOSION, teleportLoc.clone().add(0, 1, 0), 3, 1, 0.5, 1, 0);
        player.getWorld().spawnParticle(Particle.DUST, teleportLoc.clone().add(0, 1, 0), 30, 2, 1, 2, 0,
                new Particle.DustOptions(org.bukkit.Color.PURPLE, 2.0f));
        player.playSound(teleportLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
        player.playSound(teleportLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.5f);

        int hits = 0;
        for (Entity entity : player.getNearbyEntities(4, 4, 4)) {
            if (!(entity instanceof Player nearby)) continue;
            if (isSameTeam(player, nearby)) continue;

            nearby.damage(6.0);
            nearby.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1, false, true), true);
            nearby.sendMessage(ChatColor.DARK_PURPLE + "Void Walker shockwave!");
            hits++;
        }

        player.sendMessage(ChatColor.DARK_PURPLE + "Void Walker! Teleported to " + target.getName() + " - hit " + hits + " enemies!");
        incrementStat(uuid, "teleports");
        addStat(uuid, "aoe_hits", hits);
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("teleports", "Teleports");
        labels.put("aoe_hits", "AoE Hits");
        return labels;
    }
}

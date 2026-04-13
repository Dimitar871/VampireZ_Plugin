package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class ReapersMarkPerk extends Perk {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, UUID> marks = new HashMap<>(); // attacker -> victim
    private final Map<UUID, Long> markTimestamps = new HashMap<>();
    private static final long COOLDOWN_MS = 45000;
    private static final long MARK_DURATION_MS = 30000;

    public ReapersMarkPerk() {
        super("reapers_mark", "Reaper's Mark", PerkTier.PRISMATIC, PerkTeam.VAMPIRE,
                Material.SPECTRAL_ARROW,
                "Right-click: mark nearest enemy. +20% dmg",
                "to marked. Kill within 30s = heal to full (45s cd)");
    }

    @Override
    public void apply(Player player) {
        ItemStack item = new ItemStack(Material.SPECTRAL_ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "Reaper's Mark" + ChatColor.GRAY + " (Right-Click)");
            meta.setLore(Arrays.asList(ChatColor.GRAY + "Mark an enemy for death", ChatColor.YELLOW + "Cooldown: 45s"));
            item.setItemMeta(meta);
        }
        player.getInventory().addItem(item);
    }

    @Override
    public void remove(Player player) {
        UUID uuid = player.getUniqueId();
        cooldowns.remove(uuid);
        UUID markedVictim = marks.remove(uuid);
        markTimestamps.remove(uuid);
        if (markedVictim != null) {
            Player victim = Bukkit.getPlayer(markedVictim);
            if (victim != null) victim.removePotionEffect(PotionEffectType.GLOWING);
        }
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.SPECTRAL_ARROW && item.hasItemMeta()
                    && item.getItemMeta().getDisplayName().contains("Reaper's Mark")) {
                player.getInventory().remove(item);
            }
        }
    }

    @Override
    public void onInteract(Player player, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.SPECTRAL_ARROW || !item.hasItemMeta()) return;
        if (!item.getItemMeta().getDisplayName().contains("Reaper's Mark")) return;

        event.setCancelled(true);
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(uuid);
        if (last != null && (now - last) < getEffectiveCooldown(player, COOLDOWN_MS)) {
            player.sendMessage(ChatColor.RED + "Reaper's Mark on cooldown! " + ((getEffectiveCooldown(player, COOLDOWN_MS) - (now - last)) / 1000 + 1) + "s");
            return;
        }

        // Find nearest enemy within 15 blocks
        Player nearest = null;
        double nearestDist = 15.0;
        for (Entity entity : player.getNearbyEntities(15, 15, 15)) {
            if (!(entity instanceof Player target)) continue;
            if (isSameTeam(player, target)) continue;
            double dist = player.getLocation().distance(target.getLocation());
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = target;
            }
        }

        if (nearest == null) {
            player.sendMessage(ChatColor.RED + "No enemies in range!");
            return;
        }

        cooldowns.put(uuid, now);

        // Remove old mark glow if exists
        UUID oldMark = marks.get(uuid);
        if (oldMark != null) {
            Player oldTarget = Bukkit.getPlayer(oldMark);
            if (oldTarget != null) oldTarget.removePotionEffect(PotionEffectType.GLOWING);
        }

        marks.put(uuid, nearest.getUniqueId());
        markTimestamps.put(uuid, now);

        nearest.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 600, 0, false, true), true);
        nearest.getWorld().spawnParticle(Particle.DUST, nearest.getLocation().add(0, 2, 0), 20, 0.3, 0.3, 0.3, 0,
                new Particle.DustOptions(org.bukkit.Color.RED, 1.5f));
        player.playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.5f, 1.5f);
        nearest.sendMessage(ChatColor.RED + "You have been marked by the Reaper!");
        player.sendMessage(ChatColor.RED + "Marked " + nearest.getName() + "!");
        incrementStat(uuid, "marks");
    }

    @Override
    public void onDamageDealt(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
        UUID attackerUUID = attacker.getUniqueId();
        UUID markedVictim = marks.get(attackerUUID);
        if (markedVictim == null) return;
        if (!victim.getUniqueId().equals(markedVictim)) return;

        Long timestamp = markTimestamps.get(attackerUUID);
        if (timestamp != null && (System.currentTimeMillis() - timestamp) <= MARK_DURATION_MS) {
            event.setDamage(event.getDamage() * 1.2);
        }
    }

    @Override
    public void onKill(Player killer, Player victim) {
        UUID killerUUID = killer.getUniqueId();
        UUID markedVictim = marks.get(killerUUID);
        if (markedVictim == null || !victim.getUniqueId().equals(markedVictim)) return;

        Long timestamp = markTimestamps.get(killerUUID);
        if (timestamp != null && (System.currentTimeMillis() - timestamp) <= MARK_DURATION_MS) {
            double maxHealth = killer.getAttribute(Attribute.MAX_HEALTH).getValue();
            killer.setHealth(maxHealth);
            killer.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, killer.getLocation().add(0, 1, 0), 30, 0.5, 1, 0.5, 0.1);
            killer.playSound(killer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            killer.sendMessage(ChatColor.RED + "Reaper's execution! Healed to full!");
            incrementStat(killerUUID, "executions");
        }

        marks.remove(killerUUID);
        markTimestamps.remove(killerUUID);
        victim.removePotionEffect(PotionEffectType.GLOWING);
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("marks", "Marks");
        labels.put("executions", "Executions");
        return labels;
    }
}

package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class BloodPricePerk extends Perk {

    private final Set<UUID> empowered = new HashSet<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_MS = 20000;

    public BloodPricePerk() {
        super("blood_price", "Blood Price", PerkTier.GOLD, PerkTeam.VAMPIRE,
                Material.WITHER_ROSE,
                "Right-click: sacrifice 3 hearts,",
                "next hit within 8s deals double damage");
    }

    @Override
    public void apply(Player player) {
        ItemStack item = new ItemStack(Material.WITHER_ROSE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.DARK_RED + "Blood Price" + ChatColor.GRAY + " (Right-Click)");
            meta.setLore(Arrays.asList(ChatColor.GRAY + "Sacrifice 3 hearts for power", ChatColor.YELLOW + "Cooldown: 20s"));
            item.setItemMeta(meta);
        }
        player.getInventory().addItem(item);
    }

    @Override
    public void remove(Player player) {
        UUID uuid = player.getUniqueId();
        cooldowns.remove(uuid);
        empowered.remove(uuid);
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.WITHER_ROSE && item.hasItemMeta()
                    && item.getItemMeta().getDisplayName().contains("Blood Price")) {
                player.getInventory().remove(item);
            }
        }
    }

    @Override
    public void onInteract(Player player, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.WITHER_ROSE || !item.hasItemMeta()) return;
        if (!item.getItemMeta().getDisplayName().contains("Blood Price")) return;

        event.setCancelled(true);
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(uuid);
        if (last != null && (now - last) < getEffectiveCooldown(player, COOLDOWN_MS)) {
            player.sendMessage(ChatColor.RED + "Blood Price on cooldown! " + ((getEffectiveCooldown(player, COOLDOWN_MS) - (now - last)) / 1000 + 1) + "s");
            return;
        }

        if (player.getHealth() <= 6.0) {
            player.sendMessage(ChatColor.RED + "Not enough health to sacrifice!");
            return;
        }

        cooldowns.put(uuid, now);
        player.setHealth(player.getHealth() - 6.0);
        empowered.add(uuid);

        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0,
                new Particle.DustOptions(org.bukkit.Color.RED, 1.5f));
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_HURT, 0.5f, 1.5f);
        player.sendMessage(ChatColor.DARK_RED + "Blood Price! Next hit deals double damage!");

        // Remove empowerment after 8s if not used
        Bukkit.getScheduler().runTaskLater(getPlugin(), () -> {
            if (empowered.remove(uuid)) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) p.sendMessage(ChatColor.GRAY + "Blood Price expired.");
            }
        }, 160L);
    }

    @Override
    public void onDamageDealt(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
        if (empowered.remove(attacker.getUniqueId())) {
            event.setDamage(event.getDamage() * 2.0);
            attacker.getWorld().spawnParticle(Particle.DUST, victim.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0,
                    new Particle.DustOptions(org.bukkit.Color.RED, 2.0f));
            attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 0.8f);
            attacker.sendMessage(ChatColor.DARK_RED + "Blood Price empowered hit!");
            incrementStat(attacker.getUniqueId(), "empowered_hits");
        }
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("empowered_hits", "Empowered Hits");
        return labels;
    }
}

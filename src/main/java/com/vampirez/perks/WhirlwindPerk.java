package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.*;

public class WhirlwindPerk extends Perk {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_MS = 12000;

    public WhirlwindPerk() {
        super("whirlwind", "Whirlwind", PerkTier.GOLD, PerkTeam.BOTH,
                Material.BLAZE_ROD,
                "Right-click: deal 3 hearts to all",
                "enemies within 3.5 blocks (12s cd)");
    }

    @Override
    public void apply(Player player) {
        ItemStack item = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + "Whirlwind" + ChatColor.GRAY + " (Right-Click)");
            meta.setLore(Arrays.asList(ChatColor.GRAY + "AoE damage sweep", ChatColor.YELLOW + "Cooldown: 12s"));
            item.setItemMeta(meta);
        }
        player.getInventory().addItem(item);
    }

    @Override
    public void remove(Player player) {
        cooldowns.remove(player.getUniqueId());
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.BLAZE_ROD && item.hasItemMeta()
                    && item.getItemMeta().getDisplayName().contains("Whirlwind")) {
                player.getInventory().remove(item);
            }
        }
    }

    @Override
    public void onInteract(Player player, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.BLAZE_ROD || !item.hasItemMeta()) return;
        if (!item.getItemMeta().getDisplayName().contains("Whirlwind")) return;

        event.setCancelled(true);
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(uuid);
        if (last != null && (now - last) < getEffectiveCooldown(player, COOLDOWN_MS)) {
            player.sendMessage(ChatColor.RED + "Whirlwind on cooldown! " + ((getEffectiveCooldown(player, COOLDOWN_MS) - (now - last)) / 1000 + 1) + "s");
            return;
        }
        cooldowns.put(uuid, now);

        // Tornado AoE effect
        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation().add(0, 1, 0), 25, 2.0, 0.5, 2.0, 0);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 0.5, 0), 30, 1.5, 0.3, 1.5, 0.05);
        // Spiral ring of particles
        for (int i = 0; i < 12; i++) {
            double angle = i * (Math.PI * 2 / 12);
            double radius = 2.5;
            double px = Math.cos(angle) * radius;
            double pz = Math.sin(angle) * radius;
            player.getWorld().spawnParticle(Particle.DUST,
                    player.getLocation().add(px, 0.5 + i * 0.15, pz), 2, 0.05, 0.05, 0.05, 0,
                    new Particle.DustOptions(org.bukkit.Color.fromRGB(200, 200, 255), 1.5f));
        }
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);

        int hits = 0;
        for (Entity entity : player.getNearbyEntities(3.5, 3.5, 3.5)) {
            if (!(entity instanceof Player target)) continue;
            if (isSameTeam(player, target)) continue;

            target.damage(6.0);
            Vector knockback = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(0.5).setY(0.3);
            target.setVelocity(knockback);
            hits++;
        }

        incrementStat(uuid, "activations");
        addStat(uuid, "hits", hits);
        player.sendMessage(ChatColor.YELLOW + "Whirlwind! Hit " + hits + " enemies!");
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("activations", "Activations");
        labels.put("hits", "Enemies Hit");
        return labels;
    }
}

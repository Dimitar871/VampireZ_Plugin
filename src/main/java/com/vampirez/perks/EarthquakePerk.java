package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.*;

public class EarthquakePerk extends Perk {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_MS = 30000;

    public EarthquakePerk() {
        super("earthquake", "Earthquake", PerkTier.PRISMATIC, PerkTeam.BOTH,
                Material.BROWN_DYE,
                "Right-click to slam the ground",
                "6-block AoE knockback + 3 hearts dmg (30s cd)");
    }

    @Override
    public void apply(Player player) {
        ItemStack item = new ItemStack(Material.BROWN_DYE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Earthquake" + ChatColor.GRAY + " (Right-Click)");
            meta.setLore(Arrays.asList(ChatColor.GRAY + "AoE knockback + damage", ChatColor.YELLOW + "Cooldown: 30s"));
            item.setItemMeta(meta);
        }
        player.getInventory().addItem(item);
    }

    @Override
    public void remove(Player player) {
        cooldowns.remove(player.getUniqueId());
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.BROWN_DYE && item.hasItemMeta()
                    && item.getItemMeta().getDisplayName().contains("Earthquake")) {
                player.getInventory().remove(item);
            }
        }
    }

    @Override
    public void onInteract(Player player, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.BROWN_DYE || !item.hasItemMeta()) return;
        if (!item.getItemMeta().getDisplayName().contains("Earthquake")) return;

        event.setCancelled(true);
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(uuid);
        if (last != null && (now - last) < getEffectiveCooldown(player, COOLDOWN_MS)) {
            player.sendMessage(ChatColor.RED + "Earthquake on cooldown! " + ((getEffectiveCooldown(player, COOLDOWN_MS) - (now - last)) / 1000 + 1) + "s");
            return;
        }
        cooldowns.put(uuid, now);

        // Massive seismic slam effect
        player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation(), 20, 3.5, 0.5, 3.5, 0);
        player.getWorld().spawnParticle(Particle.BLOCK, player.getLocation(), 200, 4, 0.3, 4, 0,
                Material.DIRT.createBlockData());
        player.getWorld().spawnParticle(Particle.BLOCK, player.getLocation(), 80, 3, 0.2, 3, 0,
                Material.STONE.createBlockData());
        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 0.5, 0), 40, 4, 0.3, 4, 0,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(139, 90, 43), 2.0f));
        // Shockwave ring
        for (int i = 0; i < 16; i++) {
            double angle = i * (Math.PI * 2 / 16);
            double radius = 5.0;
            double px = Math.cos(angle) * radius;
            double pz = Math.sin(angle) * radius;
            player.getWorld().spawnParticle(Particle.CLOUD,
                    player.getLocation().add(px, 0.3, pz), 3, 0.1, 0.05, 0.1, 0.01);
        }
        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);

        int playersHit = 0;
        for (Entity entity : player.getNearbyEntities(6, 3, 6)) {
            if (entity instanceof LivingEntity target && !entity.getUniqueId().equals(uuid)) {
                target.damage(6.0, player);
                Vector knockback = target.getLocation().toVector()
                        .subtract(player.getLocation().toVector()).normalize()
                        .multiply(1.5).setY(0.6);
                target.setVelocity(knockback);
                if (entity instanceof Player) playersHit++;
            }
        }
        incrementStat(uuid, "activations");
        addStat(uuid, "players_hit", playersHit);
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("activations", "Activations");
        labels.put("players_hit", "Players Hit");
        return labels;
    }
}

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
import org.bukkit.util.Vector;

import java.util.*;

public class ShadowStrikePerk extends Perk {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_MS = 15000;

    public ShadowStrikePerk() {
        super("shadow_strike", "Shadow Strike", PerkTier.GOLD, PerkTeam.VAMPIRE,
                Material.CHORUS_FRUIT,
                "Right-click to teleport behind",
                "the nearest enemy within 15 blocks",
                "15 second cooldown");
    }

    @Override
    public void apply(Player player) {
        ItemStack item = new ItemStack(Material.CHORUS_FRUIT);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.DARK_PURPLE + "Shadow Strike" + ChatColor.GRAY + " (Right-Click)");
            meta.setLore(Arrays.asList(ChatColor.GRAY + "Teleport behind nearest enemy", ChatColor.YELLOW + "Cooldown: 15s"));
            item.setItemMeta(meta);
        }
        player.getInventory().addItem(item);
    }

    @Override
    public void remove(Player player) {
        cooldowns.remove(player.getUniqueId());
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.CHORUS_FRUIT && item.hasItemMeta()
                    && item.getItemMeta().getDisplayName().contains("Shadow Strike")) {
                player.getInventory().remove(item);
            }
        }
    }

    @Override
    public void onInteract(Player player, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.CHORUS_FRUIT || !item.hasItemMeta()) return;
        if (!item.getItemMeta().getDisplayName().contains("Shadow Strike")) return;

        event.setCancelled(true);
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(uuid);
        if (last != null && (now - last) < getEffectiveCooldown(player, COOLDOWN_MS)) {
            player.sendMessage(ChatColor.RED + "Shadow Strike on cooldown! " + ((getEffectiveCooldown(player, COOLDOWN_MS) - (now - last)) / 1000 + 1) + "s");
            return;
        }

        // Find nearest player
        Player target = null;
        double closest = 15.0;
        for (Entity entity : player.getNearbyEntities(15, 15, 15)) {
            if (entity instanceof Player p && !p.getUniqueId().equals(uuid)) {
                double dist = player.getLocation().distance(p.getLocation());
                if (dist < closest) {
                    closest = dist;
                    target = p;
                }
            }
        }

        if (target == null) {
            player.sendMessage(ChatColor.RED + "No enemies nearby!");
            return;
        }

        // Black Shield: target blocks the teleport
        if (BlackShieldPerk.isShielded(target.getUniqueId())) {
            cooldowns.put(uuid, now);
            BlackShieldPerk.consumeShield(target.getUniqueId(), target, player, "Shadow Strike");
            return;
        }

        cooldowns.put(uuid, now);

        // Teleport behind the target
        Location behindTarget = target.getLocation().clone();
        Vector direction = target.getLocation().getDirection().normalize().multiply(-1.5);
        behindTarget.add(direction);
        behindTarget.setY(target.getLocation().getY());
        behindTarget.setDirection(target.getLocation().toVector().subtract(behindTarget.toVector()));

        // Effects at origin
        player.getWorld().spawnParticle(Particle.LARGE_SMOKE, player.getLocation(), 40, 0.3, 0.5, 0.3, 0.05);
        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 25, 0.3, 0.5, 0.3, 0,
                new Particle.DustOptions(org.bukkit.Color.BLACK, 1.5f));
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.5f);

        player.teleport(behindTarget);

        // Effects at destination
        player.getWorld().spawnParticle(Particle.LARGE_SMOKE, player.getLocation(), 40, 0.3, 0.5, 0.3, 0.05);
        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 25, 0.3, 0.5, 0.3, 0,
                new Particle.DustOptions(org.bukkit.Color.BLACK, 1.5f));

        incrementStat(uuid, "teleports");
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("teleports", "Teleports");
        return labels;
    }
}

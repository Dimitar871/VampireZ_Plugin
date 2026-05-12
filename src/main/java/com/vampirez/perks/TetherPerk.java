package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import com.vampirez.MM;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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

public class TetherPerk extends Perk {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_MS = 15000;

    public TetherPerk() {
        super("tether", "Tether", PerkTier.GOLD, PerkTeam.VAMPIRE,
                Material.CHAIN,
                "Right-click: pull nearest enemy",
                "within 30 blocks toward you (15s cd)");
    }

    @Override
    public void apply(Player player) {
        ItemStack item = new ItemStack(Material.CHAIN);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Tether (Right-Click)").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            meta.lore(Arrays.asList(Component.text("Pull nearest enemy").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false), Component.text("Cooldown: 15s").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)));
            item.setItemMeta(meta);
        }
        player.getInventory().addItem(item);
    }

    @Override
    public void remove(Player player) {
        cooldowns.remove(player.getUniqueId());
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.CHAIN && item.hasItemMeta()
                    && item.getItemMeta().getDisplayName().contains("Tether")) {
                player.getInventory().remove(item);
            }
        }
    }

    @Override
    public void onInteract(Player player, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.CHAIN || !item.hasItemMeta()) return;
        if (!item.getItemMeta().getDisplayName().contains("Tether")) return;

        event.setCancelled(true);
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(uuid);
        if (last != null && (now - last) < getEffectiveCooldown(player, COOLDOWN_MS)) {
            player.sendMessage(MM.parse("<red>Tether on cooldown! " + ((getEffectiveCooldown(player, COOLDOWN_MS) - (now - last)) / 1000 + 1) + "s"));
            return;
        }

        // Find nearest enemy
        Player nearest = null;
        double nearestDist = 30.0;
        for (Entity entity : player.getNearbyEntities(30, 30, 30)) {
            if (!(entity instanceof Player target)) continue;
            if (isSameTeam(player, target)) continue;
            double dist = player.getLocation().distance(target.getLocation());
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = target;
            }
        }

        if (nearest == null) {
            player.sendMessage(MM.parse("<red>No enemies in range!"));
            return;
        }

        // Black Shield: target blocks the pull
        if (BlackShieldPerk.isShielded(nearest.getUniqueId())) {
            cooldowns.put(uuid, now);
            BlackShieldPerk.consumeShield(nearest.getUniqueId(), nearest, player, "Tether");
            return;
        }

        cooldowns.put(uuid, now);

        // Pull enemy toward caster
        Vector direction = player.getLocation().toVector().subtract(nearest.getLocation().toVector()).normalize();
        nearest.setVelocity(direction.multiply(4.8).setY(0.6));

        nearest.getWorld().spawnParticle(Particle.CRIT, nearest.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.1);
        player.playSound(player.getLocation(), Sound.BLOCK_CHAIN_BREAK, 1.0f, 0.8f);
        nearest.sendMessage(MM.parse("<red>You've been tethered!"));
        player.sendMessage(MM.parse("<gray>Tethered " + nearest.getName() + "!"));
        incrementStat(uuid, "pulls");
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("pulls", "Pulls");
        return labels;
    }
}

package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import com.vampirez.MM;
import com.vampirez.fx.VFX;
import org.bukkit.Color;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
            meta.displayName(Component.text("Earthquake (Right-Click)").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
            meta.lore(Arrays.asList(Component.text("AoE knockback + damage").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false), Component.text("Cooldown: 30s").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)));
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
            player.sendMessage(MM.parse("<red>Earthquake on cooldown! " + ((getEffectiveCooldown(player, COOLDOWN_MS) - (now - last)) / 1000 + 1) + "s"));
            return;
        }
        cooldowns.put(uuid, now);

        // Massive seismic slam — initial dirt + stone burst (kept for the impact frame)
        player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation(), 20, 3.5, 0.5, 3.5, 0);
        player.getWorld().spawnParticle(Particle.BLOCK, player.getLocation(), 200, 4, 0.3, 4, 0,
                Material.DIRT.createBlockData());
        player.getWorld().spawnParticle(Particle.BLOCK, player.getLocation(), 80, 3, 0.2, 3, 0,
                Material.STONE.createBlockData());
        // Animated expanding shockwave + layered boom
        VFX.fx().shockwave(player.getLocation(), Color.fromRGB(139, 90, 43), 6.0, 30);
        VFX.sound().playExplosion(player.getLocation());

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

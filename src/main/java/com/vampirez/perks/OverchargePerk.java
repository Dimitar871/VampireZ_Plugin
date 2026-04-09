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
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class OverchargePerk extends Perk {

    private final Map<UUID, Long> chargeStart = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long CHARGE_TIME_MS = 3000;
    private static final long COOLDOWN_MS = 20000;

    public OverchargePerk() {
        super("overcharge", "Overcharge", PerkTier.GOLD, PerkTeam.HUMAN,
                Material.TNT,
                "Hold bow 3+s, next shot deals",
                "2x damage + explosion (20s cd).");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {
        UUID uuid = player.getUniqueId();
        chargeStart.remove(uuid);
        cooldowns.remove(uuid);
    }

    @Override
    public void onInteract(Player player, PlayerInteractEvent event) {
        // Start charging when right-clicking with a bow
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (player.getInventory().getItemInMainHand().getType() != Material.BOW) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        // Check cooldown
        Long lastUse = cooldowns.get(uuid);
        if (lastUse != null && (now - lastUse) < getEffectiveCooldown(player, COOLDOWN_MS)) return;

        if (!chargeStart.containsKey(uuid)) {
            chargeStart.put(uuid, now);
            player.sendMessage(ChatColor.GOLD + "Overcharging bow...");
        }
    }

    @Override
    public void onDamageDealt(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Projectile)) return;
        if (!(victim instanceof Player target)) return;

        UUID uuid = attacker.getUniqueId();
        Long start = chargeStart.remove(uuid);
        if (start == null) return;

        long now = System.currentTimeMillis();
        if ((now - start) < CHARGE_TIME_MS) return;

        // Check cooldown
        Long lastUse = cooldowns.get(uuid);
        if (lastUse != null && (now - lastUse) < getEffectiveCooldown(attacker, COOLDOWN_MS)) return;

        cooldowns.put(uuid, now);

        // 2x damage
        event.setDamage(event.getDamage() * 2.0);

        // Explosion effect
        target.getWorld().spawnParticle(Particle.EXPLOSION, target.getLocation().add(0, 1, 0), 3, 0.5, 0.5, 0.5, 0);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.2f);
        attacker.playSound(attacker.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.5f);
        attacker.sendMessage(ChatColor.GOLD + "OVERCHARGED! 2x damage!");
        target.sendMessage(ChatColor.GOLD + "You were hit by an overcharged shot!");
        incrementStat(uuid, "overcharges");
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("overcharges", "Overcharged Shots");
        return labels;
    }
}

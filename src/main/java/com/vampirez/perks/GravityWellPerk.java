package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

import java.util.*;

public class GravityWellPerk extends Perk {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_MS = 3000;

    public GravityWellPerk() {
        super("gravity_well", "Gravity Well", PerkTier.SILVER, PerkTeam.BOTH,
                Material.ENDER_EYE,
                "Melee hits pull the target",
                "1 block toward you (3s cooldown)");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {
        cooldowns.remove(player.getUniqueId());
    }

    @Override
    public void onDamageDealt(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
        if (!(victim instanceof Player target)) return;

        UUID uuid = attacker.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(uuid);
        if (last != null && (now - last) < getEffectiveCooldown(attacker, COOLDOWN_MS)) return;
        cooldowns.put(uuid, now);

        Vector direction = attacker.getLocation().toVector().subtract(target.getLocation().toVector()).normalize();
        direction.setY(0.15);
        Vector pullVelocity = direction.multiply(0.6);
        // Delay 1 tick so it applies after Minecraft's knockback
        Bukkit.getScheduler().runTaskLater(getPlugin(), () -> {
            if (target.isOnline()) target.setVelocity(pullVelocity);
        }, 1L);
        target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0,
                new Particle.DustOptions(Color.PURPLE, 1.5f));
        attacker.playSound(attacker.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 0.5f, 2.0f);
        incrementStat(uuid, "pulls");
        attacker.sendMessage(ChatColor.DARK_PURPLE + "Gravity Well pulls " + target.getName() + "!");
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("pulls", "Pulls");
        return labels;
    }
}

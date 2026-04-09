package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EscapePlanStrongPerk extends Perk {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_MS = 45000;

    public EscapePlanStrongPerk() {
        super("escape_plan_strong", "Escape Plan", PerkTier.PRISMATIC, PerkTeam.HUMAN,
                Material.ENDER_PEARL,
                "Below 4 hearts: Absorption III + Speed II for 5s",
                "45s cooldown.");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {
        cooldowns.remove(player.getUniqueId());
    }

    @Override
    public void onDamageTaken(Player victim, Entity attacker, EntityDamageByEntityEvent event) {
        double healthAfter = victim.getHealth() - event.getFinalDamage();
        if (healthAfter > 8.0) return;

        UUID uuid = victim.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastUsed = cooldowns.get(uuid);
        if (lastUsed != null && (now - lastUsed) < getEffectiveCooldown(victim, COOLDOWN_MS)) return;

        cooldowns.put(uuid, now);
        victim.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, 2, false, true)); // 5s
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 1, false, true));
        victim.getWorld().spawnParticle(Particle.CLOUD, victim.getLocation(), 20, 0.5, 0.5, 0.5, 0.05);
        victim.playSound(victim.getLocation(), Sound.ITEM_TOTEM_USE, 0.5f, 2.0f);
    }
}

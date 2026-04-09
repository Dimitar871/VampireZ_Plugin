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

import java.util.*;

public class SunderPerk extends Perk {

    // attackerUUID -> (targetUUID -> lastProcTime)
    private final Map<UUID, Map<UUID, Long>> cooldowns = new HashMap<>();
    private static final long COOLDOWN_MS = 5000;
    private static final double HP_PERCENT = 0.05; // 5% of current HP

    public SunderPerk() {
        super("sunder", "Sunder", PerkTier.SILVER, PerkTeam.BOTH,
                Material.IRON_AXE,
                "Passive: attacks deal bonus damage",
                "equal to 5% of enemy's current HP.",
                "(5s cooldown per target)");
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

        UUID attackerUUID = attacker.getUniqueId();
        UUID targetUUID = target.getUniqueId();
        long now = System.currentTimeMillis();

        Map<UUID, Long> targetCooldowns = cooldowns.computeIfAbsent(attackerUUID, k -> new HashMap<>());
        Long last = targetCooldowns.get(targetUUID);
        if (last != null && (now - last) < getEffectiveCooldown(attacker, COOLDOWN_MS)) return;

        targetCooldowns.put(targetUUID, now);

        double bonusDamage = target.getHealth() * HP_PERCENT;
        event.setDamage(event.getDamage() + bonusDamage);

        target.getWorld().spawnParticle(Particle.ENCHANTED_HIT, target.getLocation().add(0, 1.5, 0), 8, 0.3, 0.3, 0.3, 0.1);
        attacker.playSound(attacker.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 0.7f);

        addStat(attackerUUID, "bonus_damage", bonusDamage);
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("bonus_damage", "Bonus Damage");
        return labels;
    }
}

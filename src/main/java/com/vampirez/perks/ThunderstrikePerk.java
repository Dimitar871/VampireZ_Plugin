package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ThunderstrikePerk extends Perk {

    private final Map<UUID, Integer> hitCounters = new HashMap<>();

    public ThunderstrikePerk() {
        super("thunderstrike", "Thunderstrike", PerkTier.PRISMATIC, PerkTeam.BOTH,
                Material.LIGHTNING_ROD,
                "Every 5th hit summons lightning",
                "on the target (deals extra damage)");
    }

    @Override
    public void apply(Player player) {
        hitCounters.put(player.getUniqueId(), 0);
    }

    @Override
    public void remove(Player player) {
        hitCounters.remove(player.getUniqueId());
    }

    @Override
    public void onDamageDealt(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
        UUID uuid = attacker.getUniqueId();
        int count = hitCounters.getOrDefault(uuid, 0) + 1;

        if (count >= 5) {
            hitCounters.put(uuid, 0);
            if (victim instanceof LivingEntity) {
                victim.getWorld().strikeLightningEffect(victim.getLocation());
                attacker.playSound(attacker.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.5f, 1.5f);
                // Lightning strike particles
                victim.getWorld().spawnParticle(Particle.DUST, victim.getLocation().add(0, 1, 0), 40, 0.5, 1.5, 0.5, 0,
                        new Particle.DustOptions(Color.fromRGB(100, 200, 255), 2.0f));
                victim.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, victim.getLocation().add(0, 1, 0), 25, 0.5, 1.0, 0.5, 0.1);
                // Extra lightning damage
                ((LivingEntity) victim).damage(4.0, attacker);
            }
        } else {
            hitCounters.put(uuid, count);
            // Electric charge buildup particles (intensity scales with stacks)
            int particleCount = count * 3;
            attacker.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                    attacker.getLocation().add(0, 1.5, 0), particleCount, 0.3, 0.3, 0.3, 0.05);
            if (count >= 3) {
                attacker.getWorld().spawnParticle(Particle.DUST, attacker.getLocation().add(0, 1.5, 0), 5, 0.4, 0.4, 0.4, 0,
                        new Particle.DustOptions(Color.fromRGB(150, 230, 255), 1.0f));
            }
        }
    }
}

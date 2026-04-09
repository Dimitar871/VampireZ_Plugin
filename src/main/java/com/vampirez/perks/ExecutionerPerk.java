package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class ExecutionerPerk extends Perk {

    public ExecutionerPerk() {
        super("executioner", "Executioner", PerkTier.GOLD, PerkTeam.BOTH,
                Material.DIAMOND_AXE,
                "+30% damage to targets below 40% HP");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {}

    @Override
    public void onDamageDealt(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
        if (victim instanceof LivingEntity livingVictim) {
            double healthPercent = livingVictim.getHealth() / livingVictim.getMaxHealth();
            if (healthPercent < 0.4) {
                event.setDamage(event.getDamage() * 1.3);
                attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.5f, 0.5f);
                // Dark red execute particles + skull smoke
                victim.getWorld().spawnParticle(Particle.DUST, victim.getLocation().add(0, 1.5, 0), 20, 0.3, 0.5, 0.3, 0,
                        new Particle.DustOptions(org.bukkit.Color.fromRGB(120, 0, 0), 1.5f));
                victim.getWorld().spawnParticle(Particle.SMOKE, victim.getLocation().add(0, 1.8, 0), 8, 0.2, 0.2, 0.2, 0.03);
                victim.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, victim.getLocation().add(0, 1, 0), 5, 0.3, 0.3, 0.3, 0.02);
            }
        }
    }
}

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

import java.util.Random;

public class ItsCriticalPerk extends Perk {

    private final Random random = new Random();

    public ItsCriticalPerk() {
        super("its_critical", "It's Critical", PerkTier.GOLD, PerkTeam.BOTH,
                Material.DIAMOND_SWORD, "30% chance for 1.5x damage on hit");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {}

    @Override
    public void onDamageDealt(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
        if (random.nextDouble() < 0.3) {
            event.setDamage(event.getDamage() * 1.5);
            // Star-burst crit particles
            attacker.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.15);
            attacker.getWorld().spawnParticle(Particle.ENCHANTED_HIT, victim.getLocation().add(0, 1.2, 0), 12, 0.2, 0.3, 0.2, 0.1);
            attacker.getWorld().spawnParticle(Particle.DUST, victim.getLocation().add(0, 1.5, 0), 8, 0.2, 0.2, 0.2, 0,
                    new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 215, 0), 1.5f));
            attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.2f);
        }
    }
}

package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class FirebrandPerk extends Perk {

    public FirebrandPerk() {
        super("firebrand", "Firebrand", PerkTier.PRISMATIC, PerkTeam.VAMPIRE,
                Material.FIRE_CHARGE,
                "Attacks set target on fire (3s)",
                "+ half heart bonus damage");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {}

    @Override
    public void onDamageDealt(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
        victim.setFireTicks(60); // 3 seconds
        event.setDamage(event.getDamage() + 1.0); // +0.5 heart
        // Fiery eruption particles
        victim.getWorld().spawnParticle(Particle.FLAME, victim.getLocation().add(0, 1, 0), 20, 0.3, 0.6, 0.3, 0.04);
        victim.getWorld().spawnParticle(Particle.LAVA, victim.getLocation().add(0, 0.5, 0), 5, 0.3, 0.2, 0.3, 0);
        victim.getWorld().spawnParticle(Particle.DUST, victim.getLocation().add(0, 1.3, 0), 10, 0.4, 0.5, 0.4, 0,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 100, 0), 1.5f));
        attacker.playSound(attacker.getLocation(), Sound.ITEM_FIRECHARGE_USE, 0.5f, 1.2f);
    }
}

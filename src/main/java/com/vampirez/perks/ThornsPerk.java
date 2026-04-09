package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class ThornsPerk extends Perk {

    public ThornsPerk() {
        super("thorns", "Thorns", PerkTier.SILVER, PerkTeam.HUMAN,
                Material.CACTUS, "Reflect 10% of melee damage back to attacker");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {}

    @Override
    public void onDamageTaken(Player victim, Entity attacker, EntityDamageByEntityEvent event) {
        if (attacker instanceof LivingEntity livingAttacker) {
            double reflected = event.getDamage() * 0.1;
            livingAttacker.damage(reflected);
            victim.getWorld().spawnParticle(Particle.ENCHANTED_HIT, victim.getLocation().add(0, 1, 0), 8, 0.3, 0.3, 0.3, 0.1);
            victim.playSound(victim.getLocation(), Sound.ENCHANT_THORNS_HIT, 0.6f, 1.0f);
        }
    }
}

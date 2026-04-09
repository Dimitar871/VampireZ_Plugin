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

public class GoredrinkPerk extends Perk {

    public GoredrinkPerk() {
        super("goredrink", "Goredrink", PerkTier.SILVER, PerkTeam.BOTH,
                Material.REDSTONE, "15% lifesteal on damage dealt");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {}

    @Override
    public void onDamageDealt(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
        double healAmount = event.getDamage() * 0.15;
        double newHealth = Math.min(attacker.getHealth() + healAmount, attacker.getMaxHealth());
        attacker.setHealth(newHealth);
        attacker.playSound(attacker.getLocation(), Sound.ENTITY_GENERIC_DRINK, 0.5f, 0.6f);

        // Blood drip from victim to attacker
        victim.getWorld().spawnParticle(Particle.DUST, victim.getLocation().add(0, 1.2, 0), 8, 0.2, 0.4, 0.2, 0,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(180, 0, 0), 1.0f));
        attacker.getWorld().spawnParticle(Particle.DUST, attacker.getLocation().add(0, 1.5, 0), 5, 0.2, 0.3, 0.2, 0,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(200, 0, 0), 0.8f));
    }
}

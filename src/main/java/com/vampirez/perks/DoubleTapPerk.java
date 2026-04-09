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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class DoubleTapPerk extends Perk {

    public DoubleTapPerk() {
        super("double_tap", "Double Tap", PerkTier.PRISMATIC, PerkTeam.VAMPIRE,
                Material.SPECTRAL_ARROW,
                "Critical hits deal 1.75x damage",
                "+ Slowness I for 2s on target");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {}

    @Override
    public void onDamageDealt(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
        // Check if the attack was a critical hit (player is falling and sprinting)
        if (attacker.getFallDistance() > 0.0f && !attacker.isOnGround()) {
            // Enhance the crit multiplier from 1.5x to 1.75x
            // Since Minecraft already applies 1.5x, we add the extra 0.25x
            event.setDamage(event.getDamage() * (1.75 / 1.5));

            if (victim instanceof LivingEntity livingVictim) {
                livingVictim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0, false, true)); // 2 seconds
            }

            // Devastating crit particles
            victim.getWorld().spawnParticle(Particle.ENCHANTED_HIT, victim.getLocation().add(0, 1, 0), 25, 0.4, 0.6, 0.4, 0.15);
            victim.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1.5, 0), 20, 0.3, 0.4, 0.3, 0.2);
            victim.getWorld().spawnParticle(Particle.DUST, victim.getLocation().add(0, 1.2, 0), 12, 0.3, 0.5, 0.3, 0,
                    new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 50, 50), 1.8f));
            attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 0.8f);
        }
    }
}

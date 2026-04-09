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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class ErosionPerk extends Perk {

    public ErosionPerk() {
        super("erosion", "Erosion", PerkTier.PRISMATIC, PerkTeam.VAMPIRE,
                Material.NETHERITE_PICKAXE,
                "Hits apply Weakness I for 3s",
                "Stacking duration");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {}

    @Override
    public void onDamageDealt(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
        if (victim instanceof LivingEntity livingVictim) {
            // Stack duration: add 3 seconds to existing weakness
            PotionEffect existing = livingVictim.getPotionEffect(PotionEffectType.WEAKNESS);
            int currentDuration = existing != null ? existing.getDuration() : 0;
            int newDuration = currentDuration + 60; // 3 seconds (60 ticks)

            livingVictim.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, newDuration, 0, false, true), true);
            // Crumbling decay particles
            victim.getWorld().spawnParticle(Particle.SMOKE, victim.getLocation().add(0, 1, 0), 12, 0.3, 0.5, 0.3, 0.03);
            victim.getWorld().spawnParticle(Particle.BLOCK, victim.getLocation().add(0, 0.5, 0), 8, 0.3, 0.2, 0.3, 0,
                    org.bukkit.Material.NETHERRACK.createBlockData());
            victim.getWorld().spawnParticle(Particle.DUST, victim.getLocation().add(0, 1.2, 0), 10, 0.3, 0.4, 0.3, 0,
                    new Particle.DustOptions(org.bukkit.Color.fromRGB(80, 60, 40), 1.2f));
            attacker.playSound(attacker.getLocation(), Sound.BLOCK_GRAVEL_BREAK, 0.5f, 0.8f);
        }
    }
}

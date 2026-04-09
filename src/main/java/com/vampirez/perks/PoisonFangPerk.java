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

public class PoisonFangPerk extends Perk {

    public PoisonFangPerk() {
        super("poison_fang", "Poison Fang", PerkTier.SILVER, PerkTeam.VAMPIRE,
                Material.SPIDER_EYE, "Attacks apply Poison I for 3 seconds");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {}

    @Override
    public void onDamageDealt(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
        if (victim instanceof LivingEntity target) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 0, false, true));
            // Venomous bite particles - green dripping poison
            victim.getWorld().spawnParticle(Particle.WITCH, victim.getLocation().add(0, 1, 0), 12, 0.3, 0.5, 0.3, 0);
            victim.getWorld().spawnParticle(Particle.DUST, victim.getLocation().add(0, 1.5, 0), 15, 0.3, 0.5, 0.3, 0,
                    new Particle.DustOptions(org.bukkit.Color.fromRGB(0, 180, 0), 1.2f));
            victim.getWorld().spawnParticle(Particle.ITEM_SLIME, victim.getLocation().add(0, 0.8, 0), 6, 0.2, 0.2, 0.2, 0.02);
            attacker.playSound(attacker.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, 0.5f, 1.2f);
        }
    }
}

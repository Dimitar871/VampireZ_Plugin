package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class BloodScentPerk extends Perk {

    public BloodScentPerk() {
        super("blood_scent", "Blood Scent", PerkTier.SILVER, PerkTeam.VAMPIRE,
                Material.SPIDER_EYE,
                "Enemies below 50% HP glow,",
                "visible through walls");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {}

    @Override
    public void onTick(Player player) {
        for (Entity entity : player.getNearbyEntities(30, 30, 30)) {
            if (!(entity instanceof Player target)) continue;
            if (isSameTeam(player, target)) continue;

            double maxHealth = target.getAttribute(Attribute.MAX_HEALTH).getValue();
            if (target.getHealth() < maxHealth * 0.5) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 30, 0, false, false));
                // Red particles around glowing targets
                target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0,
                        new Particle.DustOptions(Color.RED, 1.0f));
            }
        }
    }
}

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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public class MirrorShieldPerk extends Perk {

    private final Random random = new Random();

    public MirrorShieldPerk() {
        super("mirror_shield", "Mirror Shield", PerkTier.GOLD, PerkTeam.HUMAN,
                Material.SHIELD,
                "20% chance to negate damage",
                "and reflect it back to attacker");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {}

    @Override
    public void onDamageTaken(Player victim, Entity attacker, EntityDamageByEntityEvent event) {
        if (random.nextDouble() < 0.2) {
            double damage = event.getDamage();
            event.setCancelled(true);
            if (attacker instanceof LivingEntity livingAttacker) {
                livingAttacker.damage(damage);
            }
            victim.getWorld().spawnParticle(Particle.END_ROD, victim.getLocation().add(0, 1.5, 0), 40, 0.5, 0.5, 0.5, 0.05);
            victim.getWorld().spawnParticle(Particle.DUST, victim.getLocation().add(0, 1.5, 0), 30, 0.5, 0.5, 0.5, 0,
                    new Particle.DustOptions(org.bukkit.Color.WHITE, 1.5f));
            victim.playSound(victim.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.5f);
            addStat(victim.getUniqueId(), "damage_reflected", damage);
            incrementStat(victim.getUniqueId(), "reflects");
        }
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("reflects", "Reflects");
        labels.put("damage_reflected", "Damage Reflected");
        return labels;
    }
}

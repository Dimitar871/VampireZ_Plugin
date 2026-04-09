package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

import java.util.LinkedHashMap;
import java.util.Map;

public class RicochetShotPerk extends Perk {

    private static final double BOUNCE_RADIUS = 8.0;

    public RicochetShotPerk() {
        super("ricochet_shot", "Ricochet Shot", PerkTier.GOLD, PerkTeam.HUMAN,
                Material.SPECTRAL_ARROW,
                "Arrows bounce to nearest enemy",
                "within 8 blocks for 50% damage.");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {}

    @Override
    public void onDamageDealt(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
        if (!(victim instanceof Player target)) return;
        if (!(event.getDamager() instanceof Projectile)) return;

        double bounceDamage = event.getDamage() * 0.5;
        Player bounceTarget = null;
        double nearestDist = Double.MAX_VALUE;

        for (Entity entity : victim.getNearbyEntities(BOUNCE_RADIUS, BOUNCE_RADIUS, BOUNCE_RADIUS)) {
            if (!(entity instanceof Player candidate)) continue;
            if (candidate.getUniqueId().equals(attacker.getUniqueId())) continue;
            if (candidate.getUniqueId().equals(victim.getUniqueId())) continue;
            if (isSameTeam(attacker, candidate)) continue;

            double dist = candidate.getLocation().distanceSquared(victim.getLocation());
            if (dist < nearestDist) {
                nearestDist = dist;
                bounceTarget = candidate;
            }
        }

        if (bounceTarget != null) {
            // Spawn arrow trail between victim and bounce target
            Vector from = target.getLocation().add(0, 1, 0).toVector();
            Vector to = bounceTarget.getLocation().add(0, 1, 0).toVector();
            Vector direction = to.clone().subtract(from);
            double distance = direction.length();
            if (distance > 0) {
                direction.normalize();
                for (int i = 0; i < 8; i++) {
                    double d = (distance / 8.0) * i;
                    org.bukkit.Location particleLoc = target.getLocation().add(0, 1, 0).add(direction.clone().multiply(d));
                    target.getWorld().spawnParticle(Particle.DUST, particleLoc, 2, 0.05, 0.05, 0.05, 0,
                            new Particle.DustOptions(Color.YELLOW, 1.0f));
                }
            }

            bounceTarget.damage(bounceDamage);
            bounceTarget.getWorld().spawnParticle(Particle.CRIT, bounceTarget.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.1);
            attacker.playSound(attacker.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 0.7f, 1.8f);
            attacker.sendMessage(ChatColor.YELLOW + "Ricochet! Arrow bounced to " + bounceTarget.getName() + "!");
            bounceTarget.sendMessage(ChatColor.YELLOW + "A ricochet arrow hits you!");
            addStat(attacker.getUniqueId(), "ricochet_damage", bounceDamage);
            incrementStat(attacker.getUniqueId(), "ricochets");
        }
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("ricochets", "Ricochets");
        labels.put("ricochet_damage", "Bounce Damage");
        return labels;
    }
}

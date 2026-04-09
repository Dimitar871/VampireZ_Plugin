package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.LinkedHashMap;
import java.util.Map;

public class ChainLightningPerk extends Perk {

    public ChainLightningPerk() {
        super("chain_lightning", "Chain Lightning", PerkTier.PRISMATIC, PerkTeam.BOTH,
                Material.TRIDENT,
                "Melee hits chain to nearest enemy",
                "within 5 blocks for 50% damage + lightning");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {}

    @Override
    public void onDamageDealt(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
        if (!(victim instanceof Player)) return;

        double chainDamage = event.getDamage() * 0.5;
        Player nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (Entity entity : victim.getNearbyEntities(5, 5, 5)) {
            if (!(entity instanceof Player candidate)) continue;
            if (candidate.getUniqueId().equals(attacker.getUniqueId())) continue;
            if (candidate.getUniqueId().equals(victim.getUniqueId())) continue;
            if (isSameTeam(attacker, candidate)) continue;

            double dist = candidate.getLocation().distanceSquared(victim.getLocation());
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = candidate;
            }
        }

        if (nearest != null) {
            nearest.getWorld().strikeLightningEffect(nearest.getLocation());
            nearest.getWorld().spawnParticle(Particle.DUST, nearest.getLocation().add(0, 1, 0), 30, 0.5, 1, 0.5, 0,
                    new Particle.DustOptions(Color.AQUA, 1.5f));
            attacker.playSound(attacker.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 2.0f);
            nearest.damage(chainDamage);
            addStat(attacker.getUniqueId(), "chain_damage", chainDamage);
            incrementStat(attacker.getUniqueId(), "chains");
            attacker.sendMessage(ChatColor.AQUA + "Chain Lightning arcs to " + nearest.getName() + "!");
            nearest.sendMessage(ChatColor.AQUA + "Chain Lightning strikes you!");
        }
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("chains", "Chains");
        labels.put("chain_damage", "Chain Damage");
        return labels;
    }
}

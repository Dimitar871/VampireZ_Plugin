package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

import java.util.LinkedHashMap;
import java.util.Map;

public class BackstabPerk extends Perk {

    public BackstabPerk() {
        super("backstab", "Backstab", PerkTier.SILVER, PerkTeam.VAMPIRE,
                Material.IRON_SWORD,
                "Deal 30% more damage when hitting",
                "a player from behind.");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {}

    @Override
    public void onDamageDealt(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
        if (!(victim instanceof Player target)) return;

        // Calculate if attacker is behind the victim using horizontal dot product
        Vector victimDir = target.getLocation().getDirection();
        Vector toAttacker = attacker.getLocation().toVector().subtract(target.getLocation().toVector());

        // Only use horizontal (XZ) plane
        victimDir.setY(0).normalize();
        toAttacker.setY(0).normalize();

        double dot = victimDir.dot(toAttacker);

        if (dot < 0) {
            // Attacker is behind the victim
            event.setDamage(event.getDamage() * 1.30);

            // Dark smoke particles on victim
            target.getWorld().spawnParticle(Particle.LARGE_SMOKE, target.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.02);
            target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, 1, 0), 10, 0.2, 0.3, 0.2, 0,
                    new Particle.DustOptions(org.bukkit.Color.fromRGB(30, 0, 30), 1.2f));

            attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.5f, 0.7f);
            incrementStat(attacker.getUniqueId(), "backstabs");
            attacker.sendMessage(ChatColor.DARK_GRAY + "Backstab! 30% bonus damage on " + target.getName());
        }
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("backstabs", "Backstabs");
        return labels;
    }
}

package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.LinkedHashMap;
import java.util.Map;

public class HeadhunterPerk extends Perk {

    public HeadhunterPerk() {
        super("headhunter", "Headhunter", PerkTier.SILVER, PerkTeam.BOTH,
                Material.SKELETON_SKULL,
                "Bow headshots (above chest height)",
                "deal 2x damage.");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {}

    @Override
    public void onDamageDealt(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
        if (!(victim instanceof Player target)) return;
        if (!(event.getDamager() instanceof Projectile)) return;

        // Check if arrow hit above chest height (eye height - 0.3)
        double arrowY = event.getDamager().getLocation().getY();
        double headThreshold = target.getLocation().getY() + target.getEyeHeight() - 0.3;

        if (arrowY >= headThreshold) {
            event.setDamage(event.getDamage() * 2.0);
            target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 2, 0), 20, 0.3, 0.3, 0.3, 0.1);
            attacker.playSound(attacker.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 2.0f);
            attacker.sendMessage(ChatColor.RED + "HEADSHOT! 2x damage!");
            target.sendMessage(ChatColor.RED + "You were headshot!");
            incrementStat(attacker.getUniqueId(), "headshots");
        }
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("headshots", "Headshots");
        return labels;
    }
}

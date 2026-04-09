package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.LinkedHashMap;
import java.util.Map;

public class DeflectPerk extends Perk {

    public DeflectPerk() {
        super("deflect", "Deflect", PerkTier.SILVER, PerkTeam.HUMAN,
                Material.SHIELD,
                "30% chance to fully negate",
                "projectile damage");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {}

    @Override
    public void onDamageTaken(Player victim, Entity attacker, EntityDamageByEntityEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.PROJECTILE) return;
        if (Math.random() < 0.30) {
            event.setCancelled(true);
            victim.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.1);
            victim.playSound(victim.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.5f);
            victim.sendMessage(ChatColor.WHITE + "Deflect! Projectile negated!");
            incrementStat(victim.getUniqueId(), "deflections");
        }
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("deflections", "Deflections");
        return labels;
    }
}

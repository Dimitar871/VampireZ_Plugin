package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.LinkedHashMap;
import java.util.Map;

public class LastStandPerk extends Perk {

    private static final double HP_THRESHOLD = 0.25;
    private static final double DAMAGE_BONUS = 0.25;

    public LastStandPerk() {
        super("last_stand", "Last Stand", PerkTier.GOLD, PerkTeam.BOTH,
                Material.SHIELD,
                "Below 25% HP: immune to knockback",
                "and deal 25% more damage");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {}

    private boolean isLowHealth(Player player) {
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
        return player.getHealth() <= maxHealth * HP_THRESHOLD;
    }

    @Override
    public void onDamageDealt(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
        if (!(victim instanceof Player)) return;
        if (!isLowHealth(attacker)) return;

        double bonus = event.getDamage() * DAMAGE_BONUS;
        event.setDamage(event.getDamage() + bonus);
        addStat(attacker.getUniqueId(), "bonus_damage", bonus);
        attacker.getWorld().spawnParticle(Particle.DUST, attacker.getLocation().add(0, 2, 0), 30, 0.5, 0.5, 0.5, 0,
                new Particle.DustOptions(Color.ORANGE, 1.5f));
        attacker.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, attacker.getLocation().add(0, 2, 0), 10, 0.3, 0.2, 0.3, 0);
    }

    @Override
    public void onDamageTaken(Player victim, Entity attacker, EntityDamageByEntityEvent event) {
        if (!isLowHealth(victim)) return;

        victim.playSound(victim.getLocation(), Sound.ITEM_TOTEM_USE, 0.6f, 1.5f);
        Bukkit.getScheduler().runTaskLater(getPlugin(), () -> victim.setVelocity(victim.getVelocity().multiply(0)), 1L);
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("bonus_damage", "Bonus Damage");
        return labels;
    }
}

package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class SteadyAimPerk extends Perk {

    public SteadyAimPerk() {
        super("steady_aim", "Steady Aim", PerkTier.SILVER, PerkTeam.HUMAN,
                Material.TARGET,
                "+20% projectile damage");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {}

    @Override
    public void onDamageDealt(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
        // Only boost projectile damage
        if (event.getDamager() instanceof Projectile) {
            event.setDamage(event.getDamage() * 1.20);
            attacker.playSound(attacker.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 0.5f, 1.5f);
        }
    }
}

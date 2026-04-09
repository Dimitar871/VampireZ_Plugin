package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class GiantSlayerPerk extends Perk {

    public GiantSlayerPerk() {
        super("giant_slayer", "Giant Slayer", PerkTier.PRISMATIC, PerkTeam.HUMAN,
                Material.RABBIT_FOOT,
                "Permanent Speed I",
                "+25% damage to targets with more max HP than you");
    }

    @Override
    public void apply(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 999999, 0, false, false));
    }

    @Override
    public void remove(Player player) {
        player.removePotionEffect(PotionEffectType.SPEED);
    }

    @Override
    public void onDamageDealt(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
        if (victim instanceof LivingEntity livingVictim) {
            if (livingVictim.getMaxHealth() > attacker.getMaxHealth()) {
                event.setDamage(event.getDamage() * 1.25);
                attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 0.5f, 0.7f);
            }
        }
    }
}

package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;

public class GoliathPerk extends Perk {

    private static final String MODIFIER_NAME = "vampirez_goliath";

    public GoliathPerk() {
        super("goliath", "Goliath", PerkTier.PRISMATIC, PerkTeam.BOTH,
                Material.DIAMOND_CHESTPLATE,
                "+6 max hearts, +10% damage, Slowness I");
    }

    @Override
    public void apply(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        for (AttributeModifier mod : new ArrayList<>(attr.getModifiers())) {
            if (mod.getName().equals(MODIFIER_NAME)) attr.removeModifier(mod);
        }
        attr.addModifier(new AttributeModifier(MODIFIER_NAME, 12.0, AttributeModifier.Operation.ADD_NUMBER));
        player.setHealth(Math.min(player.getHealth() + 12.0, attr.getValue()));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 999999, 0, false, false));
    }

    @Override
    public void remove(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        for (AttributeModifier mod : new ArrayList<>(attr.getModifiers())) {
            if (mod.getName().equals(MODIFIER_NAME)) attr.removeModifier(mod);
        }
        if (player.getHealth() > attr.getValue()) {
            player.setHealth(attr.getValue());
        }
        player.removePotionEffect(PotionEffectType.SLOWNESS);
    }

    @Override
    public void onDamageDealt(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
        event.setDamage(event.getDamage() * 1.10);
        attacker.playSound(attacker.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 0.5f, 0.8f);
        victim.getWorld().spawnParticle(Particle.BLOCK, victim.getLocation(), 12, 0.3, 0.1, 0.3, 0,
                Material.STONE.createBlockData());
        victim.getWorld().spawnParticle(Particle.EXPLOSION, victim.getLocation().add(0, 0.5, 0), 1, 0, 0, 0, 0);
    }
}

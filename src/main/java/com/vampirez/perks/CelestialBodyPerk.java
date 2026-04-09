package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.ArrayList;

public class CelestialBodyPerk extends Perk {

    private static final String MODIFIER_NAME = "vampirez_celestial_body";

    public CelestialBodyPerk() {
        super("celestial_body", "Celestial Body", PerkTier.GOLD, PerkTeam.BOTH,
                Material.GOLDEN_CHESTPLATE,
                "+4 max hearts, -10% damage dealt");
    }

    @Override
    public void apply(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        for (AttributeModifier mod : new ArrayList<>(attr.getModifiers())) {
            if (mod.getName().equals(MODIFIER_NAME)) attr.removeModifier(mod);
        }
        attr.addModifier(new AttributeModifier(MODIFIER_NAME, 8.0, AttributeModifier.Operation.ADD_NUMBER));
        player.setHealth(Math.min(player.getHealth() + 8.0, attr.getValue()));
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
    }

    @Override
    public void onDamageDealt(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
        event.setDamage(event.getDamage() * 0.9);
        attacker.playSound(attacker.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1.0f);
    }
}

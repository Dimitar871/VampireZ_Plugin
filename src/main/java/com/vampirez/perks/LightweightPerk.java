package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.ArrayList;

public class LightweightPerk extends Perk {

    private static final String MODIFIER_NAME = "vampirez_lightweight";

    public LightweightPerk() {
        super("lightweight", "Lightweight", PerkTier.GOLD, PerkTeam.BOTH,
                Material.FEATHER,
                "+25% damage dealt.",
                "-4 max hearts.");
    }

    @Override
    public void apply(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        for (AttributeModifier mod : new ArrayList<>(attr.getModifiers())) {
            if (mod.getName().equals(MODIFIER_NAME)) attr.removeModifier(mod);
        }
        attr.addModifier(new AttributeModifier(MODIFIER_NAME, -8.0, AttributeModifier.Operation.ADD_NUMBER));
        if (player.getHealth() > attr.getValue()) {
            player.setHealth(attr.getValue());
        }
    }

    @Override
    public void remove(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        for (AttributeModifier mod : new ArrayList<>(attr.getModifiers())) {
            if (mod.getName().equals(MODIFIER_NAME)) attr.removeModifier(mod);
        }
        // Health stays where it is — max just went up
    }

    @Override
    public void onDamageDealt(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
        event.setDamage(event.getDamage() * 1.25);
    }
}

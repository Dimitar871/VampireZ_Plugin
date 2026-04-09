package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class SwiftStrikesPerk extends Perk {

    private static final String MODIFIER_NAME = "swift_strikes_speed";

    public SwiftStrikesPerk() {
        super("swift_strikes", "Swift Strikes", PerkTier.SILVER, PerkTeam.BOTH,
                Material.GOLD_INGOT,
                "+15% attack speed");
    }

    @Override
    public void apply(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.ATTACK_SPEED);
        if (attr != null) {
            for (AttributeModifier mod : new ArrayList<>(attr.getModifiers())) {
                if (mod.getName().equals(MODIFIER_NAME)) attr.removeModifier(mod);
            }
            attr.addModifier(new AttributeModifier(MODIFIER_NAME, 0.15, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
        }
    }

    @Override
    public void remove(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.ATTACK_SPEED);
        if (attr != null) {
            attr.getModifiers().stream()
                    .filter(m -> m.getName().equals(MODIFIER_NAME))
                    .forEach(attr::removeModifier);
        }
    }
}

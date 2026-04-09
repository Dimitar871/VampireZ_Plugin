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

public class VitalityPerk extends Perk {

    private static final String MODIFIER_NAME = "vampirez_vitality";

    public VitalityPerk() {
        super("vitality", "Vitality", PerkTier.SILVER, PerkTeam.BOTH,
                Material.APPLE,
                "+2 max hearts");
    }

    @Override
    public void apply(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        for (AttributeModifier mod : new ArrayList<>(attr.getModifiers())) {
            if (mod.getName().equals(MODIFIER_NAME)) attr.removeModifier(mod);
        }
        attr.addModifier(new AttributeModifier(MODIFIER_NAME, 4.0, AttributeModifier.Operation.ADD_NUMBER));
        player.setHealth(Math.min(player.getHealth() + 4.0, attr.getValue()));
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
}

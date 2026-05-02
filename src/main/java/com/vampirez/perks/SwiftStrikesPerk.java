package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;

import java.util.ArrayList;

public class SwiftStrikesPerk extends Perk {

    private static final NamespacedKey MODIFIER_KEY = new NamespacedKey("vampirez", "swift_strikes_speed");

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
                if (MODIFIER_KEY.equals(mod.getKey())) attr.removeModifier(mod);
            }
            attr.addModifier(new AttributeModifier(MODIFIER_KEY, 0.15, AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlotGroup.ANY));
        }
    }

    @Override
    public void remove(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.ATTACK_SPEED);
        if (attr != null) {
            for (AttributeModifier mod : new ArrayList<>(attr.getModifiers())) {
                if (MODIFIER_KEY.equals(mod.getKey())) attr.removeModifier(mod);
            }
        }
    }
}

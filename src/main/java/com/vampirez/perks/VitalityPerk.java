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

public class VitalityPerk extends Perk {

    private static final NamespacedKey MODIFIER_KEY = new NamespacedKey("vampirez", "vitality");

    public VitalityPerk() {
        super("vitality", "Vitality", PerkTier.SILVER, PerkTeam.BOTH,
                Material.APPLE,
                "+2 max hearts");
    }

    @Override
    public void apply(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        for (AttributeModifier mod : new ArrayList<>(attr.getModifiers())) {
            if (MODIFIER_KEY.equals(mod.getKey())) attr.removeModifier(mod);
        }
        attr.addModifier(new AttributeModifier(MODIFIER_KEY, 4.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
        player.setHealth(Math.min(player.getHealth() + 4.0, attr.getValue()));
    }

    @Override
    public void remove(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        for (AttributeModifier mod : new ArrayList<>(attr.getModifiers())) {
            if (MODIFIER_KEY.equals(mod.getKey())) attr.removeModifier(mod);
        }
        if (player.getHealth() > attr.getValue()) {
            player.setHealth(attr.getValue());
        }
    }
}

package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EquipmentSlotGroup;

import java.util.ArrayList;

public class LightweightPerk extends Perk {

    private static final NamespacedKey MODIFIER_KEY = new NamespacedKey("vampirez", "lightweight");

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
            if (MODIFIER_KEY.equals(mod.getKey())) attr.removeModifier(mod);
        }
        attr.addModifier(new AttributeModifier(MODIFIER_KEY, -8.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
        if (player.getHealth() > attr.getValue()) {
            player.setHealth(attr.getValue());
        }
    }

    @Override
    public void remove(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        for (AttributeModifier mod : new ArrayList<>(attr.getModifiers())) {
            if (MODIFIER_KEY.equals(mod.getKey())) attr.removeModifier(mod);
        }
        // Health stays where it is — max just went up
    }

    @Override
    public void onDamageDealt(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
        event.setDamage(event.getDamage() * 1.25);
    }
}

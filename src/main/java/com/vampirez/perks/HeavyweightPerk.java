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

public class HeavyweightPerk extends Perk {

    private static final NamespacedKey MODIFIER_KEY = new NamespacedKey("vampirez", "heavyweight");

    public HeavyweightPerk() {
        super("heavyweight", "Heavyweight", PerkTier.SILVER, PerkTeam.HUMAN,
                Material.IRON_BLOCK,
                "Double your max hearts.",
                "-50% damage dealt.");
    }

    @Override
    public void apply(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        for (AttributeModifier mod : new ArrayList<>(attr.getModifiers())) {
            if (MODIFIER_KEY.equals(mod.getKey())) attr.removeModifier(mod);
        }
        double oldMax = attr.getValue();
        attr.addModifier(new AttributeModifier(MODIFIER_KEY, 1.0, AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlotGroup.ANY));
        player.setHealth(Math.min(player.getHealth() + oldMax, attr.getValue()));
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

    @Override
    public void onDamageDealt(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
        event.setDamage(event.getDamage() * 0.5);
    }
}

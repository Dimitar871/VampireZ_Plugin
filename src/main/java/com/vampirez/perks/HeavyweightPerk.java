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

public class HeavyweightPerk extends Perk {

    private static final String MODIFIER_NAME = "vampirez_heavyweight";

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
            if (mod.getName().equals(MODIFIER_NAME)) attr.removeModifier(mod);
        }
        double oldMax = attr.getValue();
        attr.addModifier(new AttributeModifier(MODIFIER_NAME, 1.0, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
        player.setHealth(Math.min(player.getHealth() + oldMax, attr.getValue()));
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
        event.setDamage(event.getDamage() * 0.5);
    }
}

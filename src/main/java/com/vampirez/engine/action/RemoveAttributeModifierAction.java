package com.vampirez.engine.action;

import com.vampirez.engine.HookContext;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

import java.util.ArrayList;

/**
 * Removes the modifier added by {@link AddAttributeModifierAction} (matched by key).
 * For MAX_HEALTH, clamps the player's current HP down to the new (lower) max if needed.
 */
public class RemoveAttributeModifierAction implements Action {

    private final NamespacedKey key;
    private final Attribute attribute;

    public RemoveAttributeModifierAction(NamespacedKey key, Attribute attribute) {
        this.key = key;
        this.attribute = attribute;
    }

    @Override
    public void run(HookContext ctx) {
        Player owner = ctx.owner;
        if (owner == null) return;
        AttributeInstance attr = owner.getAttribute(attribute);
        if (attr == null) return;

        for (AttributeModifier existing : new ArrayList<>(attr.getModifiers())) {
            if (key.equals(existing.getKey())) attr.removeModifier(existing);
        }

        if (attribute == Attribute.MAX_HEALTH) {
            double newMax = attr.getValue();
            if (owner.getHealth() > newMax) {
                owner.setHealth(newMax);
            }
        }
    }

    public NamespacedKey getKey() { return key; }
    public Attribute getAttribute() { return attribute; }
}

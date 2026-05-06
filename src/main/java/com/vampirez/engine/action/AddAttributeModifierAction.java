package com.vampirez.engine.action;

import com.vampirez.engine.HookContext;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;

import java.util.ArrayList;

/**
 * Adds an {@link AttributeModifier} to the owner. Idempotent: if a modifier with the same
 * key already exists, it's removed first so re-applying (e.g. on respawn) doesn't stack.
 *
 * For {@link Attribute#MAX_HEALTH}, the player's current HP is bumped up by however much the
 * max went up (so +4 max hearts gives 4 free HP, not just a bigger bar). Negative changes
 * clamp current HP down to the new max.
 */
public class AddAttributeModifierAction implements Action {

    private final NamespacedKey key;
    private final Attribute attribute;
    private final AttributeModifier.Operation operation;
    private final double value;
    private final EquipmentSlotGroup slotGroup;

    public AddAttributeModifierAction(NamespacedKey key, Attribute attribute,
                                      AttributeModifier.Operation operation, double value,
                                      EquipmentSlotGroup slotGroup) {
        this.key = key;
        this.attribute = attribute;
        this.operation = operation;
        this.value = value;
        this.slotGroup = slotGroup;
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
            double oldMax = attr.getValue();
            double oldHp = owner.getHealth();
            attr.addModifier(new AttributeModifier(key, value, operation, slotGroup));
            double newMax = attr.getValue();
            double deltaMax = newMax - oldMax;
            double newHp = oldHp + Math.max(0, deltaMax);
            owner.setHealth(Math.min(Math.max(newHp, 0.001), newMax));
        } else {
            attr.addModifier(new AttributeModifier(key, value, operation, slotGroup));
        }
    }

    public NamespacedKey getKey() { return key; }
    public Attribute getAttribute() { return attribute; }
    public AttributeModifier.Operation getOperation() { return operation; }
    public double getValue() { return value; }
}

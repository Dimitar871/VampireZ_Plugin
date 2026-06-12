package com.vampirez;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;

/**
 * Replacement for the deprecated {@code LivingEntity.getMaxHealth()} — reads the
 * MAX_HEALTH attribute (including modifiers from perks / stat anvils).
 *
 * <p>Note: not usable from unit tests — the {@link Attribute} registry only
 * initializes on a real server. Code that must be unit-tested with mocked
 * entities (e.g. HpBelowPercentCondition) keeps the deprecated call instead.
 */
public final class Health {

    private Health() {}

    public static double max(LivingEntity entity) {
        AttributeInstance attr = entity.getAttribute(Attribute.MAX_HEALTH);
        return attr != null ? attr.getValue() : 20.0;
    }

    /** Heals without overshooting the entity's max health. */
    public static void heal(LivingEntity entity, double amount) {
        entity.setHealth(Math.min(entity.getHealth() + amount, max(entity)));
    }
}

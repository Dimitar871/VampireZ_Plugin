package com.vampirez.engine.action;

import com.vampirez.engine.HookContext;
import com.vampirez.engine.condition.Target;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Applies a {@link PotionEffect} to a target. If {@code stackDuration} is true and the target
 * already has the same effect, the new duration is added to the remaining ticks.
 */
public class ApplyPotionAction implements Action {

    private final Target target;
    private final PotionEffectType type;
    private final int durationTicks;
    private final int amplifier;
    private final boolean ambient;
    private final boolean particles;
    private final boolean stackDuration;

    public ApplyPotionAction(Target target, PotionEffectType type, int durationTicks, int amplifier,
                             boolean ambient, boolean particles, boolean stackDuration) {
        this.target = target;
        this.type = type;
        this.durationTicks = durationTicks;
        this.amplifier = amplifier;
        this.ambient = ambient;
        this.particles = particles;
        this.stackDuration = stackDuration;
    }

    @Override
    public void run(HookContext ctx) {
        LivingEntity entity = target.resolveLivingEntity(ctx);
        if (entity == null || type == null) return;

        int finalDuration = durationTicks;
        if (stackDuration) {
            PotionEffect existing = entity.getPotionEffect(type);
            if (existing != null) {
                finalDuration = existing.getDuration() + durationTicks;
            }
        }
        entity.addPotionEffect(new PotionEffect(type, finalDuration, amplifier, ambient, particles), true);
    }
}

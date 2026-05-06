package com.vampirez.engine.action;

import com.vampirez.engine.HookContext;
import com.vampirez.engine.condition.Target;
import org.bukkit.entity.LivingEntity;

/**
 * Sets the fire ticks on a target (lights them on fire).
 */
public class SetFireAction implements Action {

    private final Target target;
    private final int durationTicks;

    public SetFireAction(Target target, int durationTicks) {
        this.target = target;
        this.durationTicks = durationTicks;
    }

    @Override
    public void run(HookContext ctx) {
        LivingEntity entity = target.resolveLivingEntity(ctx);
        if (entity == null) return;
        entity.setFireTicks(durationTicks);
    }
}

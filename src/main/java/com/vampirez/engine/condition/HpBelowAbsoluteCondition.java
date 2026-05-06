package com.vampirez.engine.condition;

import com.vampirez.engine.HookContext;
import org.bukkit.entity.LivingEntity;

public class HpBelowAbsoluteCondition implements Condition {

    private final Target target;
    private final double threshold;

    public HpBelowAbsoluteCondition(Target target, double threshold) {
        this.target = target;
        this.threshold = threshold;
    }

    @Override
    public boolean test(HookContext ctx) {
        LivingEntity entity = target.resolveLivingEntity(ctx);
        if (entity == null) return false;
        return entity.getHealth() <= threshold;
    }
}

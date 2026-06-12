package com.vampirez.engine.condition;

import com.vampirez.engine.HookContext;
import org.bukkit.entity.LivingEntity;

public class HpBelowPercentCondition implements Condition {

    private final Target target;
    private final double threshold;

    public HpBelowPercentCondition(Target target, double threshold) {
        this.target = target;
        this.threshold = threshold;
    }

    @Override
    @SuppressWarnings("deprecation") // getMaxHealth kept deliberately: the Attribute
    // registry (com.vampirez.Health) can't initialize in unit tests, and this
    // condition is covered by HpBelowPercentConditionTest with a mocked entity.
    public boolean test(HookContext ctx) {
        LivingEntity entity = target.resolveLivingEntity(ctx);
        if (entity == null) return false;
        double max = entity.getMaxHealth();
        if (max <= 0) return false;
        return (entity.getHealth() / max) < threshold;
    }
}

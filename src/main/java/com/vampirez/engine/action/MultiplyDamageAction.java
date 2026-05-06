package com.vampirez.engine.action;

import com.vampirez.engine.HookContext;

public class MultiplyDamageAction implements Action {

    private final double factor;

    public MultiplyDamageAction(double factor) {
        this.factor = factor;
    }

    @Override
    public void run(HookContext ctx) {
        if (ctx.damageEvent == null) return;
        ctx.damageEvent.setDamage(ctx.damageEvent.getDamage() * factor);
    }
}

package com.vampirez.engine.action;

import com.vampirez.engine.HookContext;

public class SetDamageAction implements Action {

    private final double value;

    public SetDamageAction(double value) {
        this.value = value;
    }

    @Override
    public void run(HookContext ctx) {
        if (ctx.damageEvent == null) return;
        ctx.damageEvent.setDamage(value);
    }
}

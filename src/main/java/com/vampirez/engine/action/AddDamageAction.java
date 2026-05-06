package com.vampirez.engine.action;

import com.vampirez.engine.HookContext;

/**
 * Adds a flat amount to the current damage event. Use {@link MultiplyDamageAction} for ratios.
 */
public class AddDamageAction implements Action {

    private final double amount;

    public AddDamageAction(double amount) {
        this.amount = amount;
    }

    @Override
    public void run(HookContext ctx) {
        if (ctx.damageEvent == null) return;
        ctx.damageEvent.setDamage(ctx.damageEvent.getDamage() + amount);
    }
}

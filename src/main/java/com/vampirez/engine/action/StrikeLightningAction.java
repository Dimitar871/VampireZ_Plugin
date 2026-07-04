package com.vampirez.engine.action;

import com.vampirez.engine.HookContext;
import com.vampirez.engine.condition.Target;
import org.bukkit.entity.LivingEntity;

/**
 * Visual lightning bolt at the resolved target plus optional bonus damage
 * attributed to the perk owner. Uses {@code strikeLightningEffect} — no fire,
 * no vanilla lightning damage — so the number dealt is exactly {@code damage}.
 */
public class StrikeLightningAction implements Action {

    private final Target target;
    private final double damage;

    public StrikeLightningAction(Target target, double damage) {
        this.target = target;
        this.damage = damage;
    }

    @Override
    public void run(HookContext ctx) {
        LivingEntity struck = target.resolveLivingEntity(ctx);
        if (struck == null) return;

        struck.getWorld().strikeLightningEffect(struck.getLocation());
        if (damage > 0) {
            if (ctx.owner != null) {
                struck.damage(damage, ctx.owner);
            } else {
                struck.damage(damage);
            }
        }
    }
}

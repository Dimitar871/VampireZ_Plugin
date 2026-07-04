package com.vampirez.engine.action;

import com.vampirez.engine.HookContext;
import com.vampirez.engine.condition.Target;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Yanks the resolved target toward the perk owner — hook/grapple-style
 * displacement (unlike {@code set_velocity}, the direction is computed per hit).
 * {@code strength} scales the pull; {@code lift} is a minimum upward component so
 * the target is popped off the ground instead of dragged into it.
 */
public class PullTargetAction implements Action {

    private final Target target;
    private final double strength;
    private final double lift;

    public PullTargetAction(Target target, double strength, double lift) {
        this.target = target;
        this.strength = strength;
        this.lift = lift;
    }

    @Override
    public void run(HookContext ctx) {
        Player owner = ctx.owner;
        LivingEntity pulled = target.resolveLivingEntity(ctx);
        if (owner == null || pulled == null || pulled.equals(owner)) return;

        Vector dir = owner.getLocation().toVector().subtract(pulled.getLocation().toVector());
        if (dir.lengthSquared() == 0) return;
        dir.normalize().multiply(strength);
        if (dir.getY() < lift) dir.setY(lift);
        pulled.setVelocity(dir);
    }
}

package com.vampirez.engine.action;

import com.vampirez.engine.HookContext;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Launches the owner forward in their look direction. {@code magnitude} is the horizontal
 * boost; {@code lift} is added to the Y axis after normalization (small upward kick so the
 * dash clears terrain).
 */
public class DashAction implements Action {

    private final double magnitude;
    private final double lift;

    public DashAction(double magnitude, double lift) {
        this.magnitude = magnitude;
        this.lift = lift;
    }

    @Override
    public void run(HookContext ctx) {
        Player owner = ctx.owner;
        if (owner == null) return;
        Vector dir = owner.getLocation().getDirection();
        dir.setY(0);
        if (dir.lengthSquared() > 0) dir.normalize();
        dir.multiply(magnitude);
        dir.setY(lift);
        owner.setVelocity(dir);
    }
}

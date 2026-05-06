package com.vampirez.engine.condition;

import com.vampirez.engine.HookContext;
import org.bukkit.entity.Player;

public class IsSprintingCondition implements Condition {

    private final Target target;

    public IsSprintingCondition(Target target) {
        this.target = target;
    }

    @Override
    public boolean test(HookContext ctx) {
        Player player = target.resolvePlayer(ctx);
        return player != null && player.isSprinting();
    }
}

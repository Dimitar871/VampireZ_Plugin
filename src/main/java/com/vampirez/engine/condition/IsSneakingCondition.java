package com.vampirez.engine.condition;

import com.vampirez.engine.HookContext;
import org.bukkit.entity.Player;

public class IsSneakingCondition implements Condition {

    private final Target target;

    public IsSneakingCondition(Target target) {
        this.target = target;
    }

    @Override
    public boolean test(HookContext ctx) {
        Player p = target.resolvePlayer(ctx);
        return p != null && p.isSneaking();
    }
}

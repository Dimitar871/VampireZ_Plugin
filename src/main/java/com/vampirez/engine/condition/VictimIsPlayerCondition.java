package com.vampirez.engine.condition;

import com.vampirez.engine.HookContext;

public class VictimIsPlayerCondition implements Condition {

    @Override
    public boolean test(HookContext ctx) {
        return ctx.victimPlayer != null;
    }
}

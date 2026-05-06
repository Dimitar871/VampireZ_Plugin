package com.vampirez.engine.condition;

import com.vampirez.engine.HookContext;

public interface Condition {
    boolean test(HookContext ctx);
}

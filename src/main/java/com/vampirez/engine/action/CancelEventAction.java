package com.vampirez.engine.action;

import com.vampirez.engine.HookContext;

/**
 * Marks the current interact event as cancelled. Used by right-click ability perks to suppress
 * vanilla item behavior (e.g. eating, placing) when the click triggers the perk.
 */
public class CancelEventAction implements Action {

    @Override
    public void run(HookContext ctx) {
        if (ctx.interactEvent != null) ctx.interactEvent.setCancelled(true);
    }
}

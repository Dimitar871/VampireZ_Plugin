package com.vampirez.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when the day/night cycle transitions. Fires after the world time has been updated
 * and vampire potion effects have been applied. Not cancellable.
 */
public class DayPhaseChangeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final boolean nowNight;

    public DayPhaseChangeEvent(boolean nowNight) {
        this.nowNight = nowNight;
    }

    /** True if night just started, false if day just started. */
    public boolean isNowNight() { return nowNight; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}

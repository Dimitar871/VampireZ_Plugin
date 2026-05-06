package com.vampirez.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

/**
 * Fired after a VampireZ game has fully started (teams assigned, players teleported,
 * timers running). Not cancellable — start is decided by {@code GameManager.startGame}.
 */
public class VampireZGameStartEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Set<UUID> humans;
    private final Set<UUID> vampires;
    private final boolean forced;

    public VampireZGameStartEvent(Set<UUID> humans, Set<UUID> vampires, boolean forced) {
        this.humans = Collections.unmodifiableSet(humans);
        this.vampires = Collections.unmodifiableSet(vampires);
        this.forced = forced;
    }

    public Set<UUID> getHumans() { return humans; }
    public Set<UUID> getVampires() { return vampires; }
    public boolean isForced() { return forced; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}

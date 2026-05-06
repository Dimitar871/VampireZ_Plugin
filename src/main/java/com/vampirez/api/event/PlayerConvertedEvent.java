package com.vampirez.api.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Fired when a human is being converted into a vampire.
 *
 * <p>Cancellable only when {@link #getCause()} is {@link Cause#DEATH} or {@link Cause#FORCED}.
 * Cancelling restores the player to the human team — they respawn at the human spawn with
 * full HP, human gear, and their existing perks intact. Cancelling for {@link Cause#DISCONNECT}
 * has no effect (the player has already left the server).</p>
 */
public class PlayerConvertedEvent extends Event implements Cancellable {

    public enum Cause { DEATH, DISCONNECT, FORCED }

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID player;
    private final Cause cause;
    private boolean cancelled;

    public PlayerConvertedEvent(UUID player, Cause cause) {
        this.player = player;
        this.cause = cause;
    }

    public UUID getPlayer() { return player; }
    public Cause getCause() { return cause; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}

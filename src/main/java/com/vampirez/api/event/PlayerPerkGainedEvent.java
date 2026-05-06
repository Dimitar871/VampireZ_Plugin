package com.vampirez.api.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Fired before a perk is added to a player. Cancelling prevents the add and the
 * granting call returns false (or no-op for force-grant).
 */
public class PlayerPerkGainedEvent extends Event implements Cancellable {

    public enum Source { FREE, SHOP, API, TIMED, CONVERSION, INTERNAL }

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID player;
    private final String perkId;
    private final Source source;
    private boolean cancelled;

    public PlayerPerkGainedEvent(UUID player, String perkId, Source source) {
        this.player = player;
        this.perkId = perkId;
        this.source = source;
    }

    public UUID getPlayer() { return player; }
    public String getPerkId() { return perkId; }
    public Source getSource() { return source; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}

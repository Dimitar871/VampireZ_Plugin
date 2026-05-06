package com.vampirez.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Fired after a perk is removed from a player. Not cancellable — by the time this fires,
 * the perk's {@code remove()} has already run.
 */
public class PlayerPerkLostEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID player;
    private final String perkId;

    public PlayerPerkLostEvent(UUID player, String perkId) {
        this.player = player;
        this.perkId = perkId;
    }

    public UUID getPlayer() { return player; }
    public String getPerkId() { return perkId; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}

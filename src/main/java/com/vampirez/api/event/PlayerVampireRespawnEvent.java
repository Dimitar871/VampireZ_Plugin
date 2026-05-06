package com.vampirez.api.event;

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Fired just before a vampire is teleported to the vampire spawn after their
 * respawn delay. Not cancellable.
 */
public class PlayerVampireRespawnEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID player;
    private final Location spawn;

    public PlayerVampireRespawnEvent(UUID player, Location spawn) {
        this.player = player;
        this.spawn = spawn;
    }

    public UUID getPlayer() { return player; }
    public Location getSpawn() { return spawn; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}

package com.vampirez.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a VampireZ game ends, regardless of cause (humans win, vampires win, or
 * admin-stopped via {@code /vz stop}). Not cancellable.
 */
public class VampireZGameEndEvent extends Event {

    public enum Winner { HUMANS, VAMPIRES, STOPPED }

    private static final HandlerList HANDLERS = new HandlerList();

    private final Winner winner;
    private final int durationPlayedSeconds;

    public VampireZGameEndEvent(Winner winner, int durationPlayedSeconds) {
        this.winner = winner;
        this.durationPlayedSeconds = durationPlayedSeconds;
    }

    public Winner getWinner() { return winner; }
    public int getDurationPlayedSeconds() { return durationPlayedSeconds; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}

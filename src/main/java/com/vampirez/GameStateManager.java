package com.vampirez;

/**
 * Owns the {@link GameState} field and the {@code lastStartForced} flag. Pure state
 * holder — transition orchestration still lives on {@link GameManager} (it touches every
 * other subsystem). Future work: move the transition methods themselves here once their
 * dependencies are explicit.
 */
public class GameStateManager {

    private GameState state = GameState.LOBBY;
    private boolean lastStartForced = false;

    public GameState getState() { return state; }
    public void setState(GameState s) { this.state = s; }

    public boolean isLobby()    { return state == GameState.LOBBY; }
    public boolean isStarting() { return state == GameState.STARTING; }
    public boolean isActive()   { return state == GameState.ACTIVE; }
    public boolean isEnding()   { return state == GameState.ENDING; }

    public boolean isLastStartForced()           { return lastStartForced; }
    public void setLastStartForced(boolean v)    { this.lastStartForced = v; }
}

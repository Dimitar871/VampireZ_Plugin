package com.vampirez.api;

import com.vampirez.GameState;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Public API for VampireZ. Other plugins consume this either via
 * {@code Bukkit.getServicesManager().getRegistration(VampireZAPI.class)} or via
 * the static accessor {@code VampireZPlugin.getAPI()}.
 *
 * <p>All methods take {@link UUID} (player may be offline) and {@link String} perk IDs.
 * The interface intentionally never exposes internal types like {@code Perk} or
 * {@code Player} so the implementation can change without breaking consumers.</p>
 */
public interface VampireZAPI {

    // ===== State =====

    GameState getState();
    int getRemainingSeconds();
    /** Clamped to {@code [0, gameDurationSeconds]}. Setting to 0 ends the game on the next tick. */
    void setRemainingSeconds(int seconds);
    /** @return true if the call transitioned LOBBY → STARTING/ACTIVE. */
    boolean startGame(boolean force);
    void stopGame();
    int getMinPlayers();
    int getGameDurationSeconds();

    // ===== Teams =====

    boolean isHuman(UUID player);
    boolean isVampire(UUID player);
    boolean isInGame(UUID player);
    Set<UUID> getHumans();
    Set<UUID> getVampires();
    /** Force-converts an online human to vampire. @return true if the player was a human and converted. */
    boolean forceConvert(UUID player);

    // ===== Perks =====

    Set<String> getAvailablePerkIds();
    /** Adds the perk respecting the per-player max. @return true if added. */
    boolean givePerk(UUID player, String perkId);
    /** Adds the perk bypassing the max-perk cap. @return true if added (false only if perkId unknown or event cancelled). */
    boolean forceGivePerk(UUID player, String perkId);
    /** @return true if the perk was on the player and removed. */
    boolean removePerk(UUID player, String perkId);
    void removeAllPerks(UUID player);
    List<String> getPlayerPerkIds(UUID player);
    boolean hasPerk(UUID player, String perkId);

    // ===== Economy =====

    int getGold(UUID player);
    /** Sets gold to exactly {@code amount}, clamped to {@code >= 0}. */
    void setGold(UUID player, int amount);
    void addGold(UUID player, int amount);
    /** @return true if the player had enough gold and it was deducted. */
    boolean removeGold(UUID player, int amount);

    // ===== Phase =====

    boolean isNight();
    boolean isDayNightEnabled();
    /** No-op if the day/night cycle isn't running. */
    void forceDay();
    /** No-op if the day/night cycle isn't running. */
    void forceNight();
}

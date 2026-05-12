package com.vampirez;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Owns team membership (human / vampire), combat-tag tracking, and the two binary game-state
 * flags ({@code vampiresReleased}, {@code bloodCompassGiven}). Stateless w.r.t. game phase —
 * the state machine lives in {@link GameStateManager} (TBD Phase 2A Step 3).
 *
 * <p>Mutation methods that have side effects beyond the team sets (e.g. teleport, gear,
 * conversion broadcasts) intentionally stay on {@link GameManager} for now and will move
 * here in Step 3 once dependencies are explicit.
 */
public class TeamManager {

    /** Combat-tag window in ms. Configurable via {@code timings.combat-tag-ms} (default 7000). */
    private long combatTagMs = 7_000L;

    private final Set<UUID> humanTeam = new HashSet<>();
    private final Set<UUID> vampireTeam = new HashSet<>();
    private final Map<UUID, Long> lastCombatMs = new HashMap<>();

    private boolean vampiresReleased = true;
    private boolean bloodCompassGiven = false;

    public void setCombatTagMs(long ms) { this.combatTagMs = ms; }

    // ===== Membership =====

    public Set<UUID> getHumanTeam()   { return humanTeam; }
    public Set<UUID> getVampireTeam() { return vampireTeam; }

    public void addHuman(UUID uuid)   { humanTeam.add(uuid); }
    public void addVampire(UUID uuid) { vampireTeam.add(uuid); }

    public boolean removeHuman(UUID uuid)   { return humanTeam.remove(uuid); }
    public boolean removeVampire(UUID uuid) { return vampireTeam.remove(uuid); }

    public boolean isHuman(UUID uuid)   { return humanTeam.contains(uuid); }
    public boolean isVampire(UUID uuid) { return vampireTeam.contains(uuid); }
    public boolean isInGame(UUID uuid)  { return isHuman(uuid) || isVampire(uuid); }

    public boolean isInGame(Player player) { return isInGame(player.getUniqueId()); }

    /** Wipe both teams. Called by {@code resetToLobby}. */
    public void clearTeams() {
        humanTeam.clear();
        vampireTeam.clear();
        lastCombatMs.clear();
    }

    // ===== Combat tag =====

    public void tagCombat(UUID uuid) {
        lastCombatMs.put(uuid, System.currentTimeMillis());
    }

    public boolean isInCombat(UUID uuid) {
        Long last = lastCombatMs.get(uuid);
        return last != null && System.currentTimeMillis() - last < combatTagMs;
    }

    // ===== State flags =====

    public boolean isVampiresReleased()           { return vampiresReleased; }
    public void setVampiresReleased(boolean v)    { this.vampiresReleased = v; }

    public boolean isBloodCompassGiven()          { return bloodCompassGiven; }
    public void setBloodCompassGiven(boolean v)   { this.bloodCompassGiven = v; }
}

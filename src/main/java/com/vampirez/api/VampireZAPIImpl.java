package com.vampirez.api;

import com.vampirez.DayNightManager;
import com.vampirez.EconomyManager;
import com.vampirez.GameManager;
import com.vampirez.GameState;
import com.vampirez.Perk;
import com.vampirez.PerkManager;
import com.vampirez.api.event.PlayerConvertedEvent;
import com.vampirez.api.event.PlayerPerkGainedEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class VampireZAPIImpl implements VampireZAPI {

    private final GameManager gameManager;
    private final PerkManager perkManager;
    private final EconomyManager economyManager;
    private final DayNightManager dayNightManager;

    public VampireZAPIImpl(GameManager gameManager) {
        this.gameManager = gameManager;
        this.perkManager = gameManager.getPerkManager();
        this.economyManager = gameManager.getEconomyManager();
        this.dayNightManager = gameManager.getDayNightManager();
    }

    // ===== State =====

    @Override public GameState getState() { return gameManager.getState(); }
    @Override public int getRemainingSeconds() { return gameManager.getRemainingSeconds(); }
    @Override public void setRemainingSeconds(int seconds) { gameManager.setRemainingSeconds(seconds); }

    @Override
    public boolean startGame(boolean force) {
        GameState before = gameManager.getState();
        gameManager.startGame(force);
        return before == GameState.LOBBY && gameManager.getState() != GameState.LOBBY;
    }

    @Override public void stopGame() { gameManager.stopGame(); }
    @Override public int getMinPlayers() { return gameManager.getMinPlayers(); }
    @Override public int getGameDurationSeconds() { return gameManager.getGameDurationSeconds(); }

    // ===== Teams =====

    @Override public boolean isHuman(UUID p) { return gameManager.isHuman(p); }
    @Override public boolean isVampire(UUID p) { return gameManager.isVampire(p); }
    @Override public boolean isInGame(UUID p) { return gameManager.isInGame(p); }
    @Override public Set<UUID> getHumans() { return Collections.unmodifiableSet(new LinkedHashSet<>(gameManager.getHumanTeam())); }
    @Override public Set<UUID> getVampires() { return Collections.unmodifiableSet(new LinkedHashSet<>(gameManager.getVampireTeam())); }

    @Override
    public boolean forceConvert(UUID uuid) {
        if (!gameManager.isHuman(uuid)) return false;
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline()) return false;
        return gameManager.convertHumanToVampire(player, PlayerConvertedEvent.Cause.FORCED);
    }

    // ===== Perks =====

    @Override
    public Set<String> getAvailablePerkIds() {
        Set<String> ids = new LinkedHashSet<>();
        for (Perk p : perkManager.getAllPerks()) ids.add(p.getId());
        return Collections.unmodifiableSet(ids);
    }

    @Override
    public boolean givePerk(UUID uuid, String perkId) {
        Perk perk = perkManager.getPerkById(perkId);
        if (perk == null) return false;
        return perkManager.addPerkToPlayer(uuid, perk, PlayerPerkGainedEvent.Source.API);
    }

    @Override
    public boolean forceGivePerk(UUID uuid, String perkId) {
        Perk perk = perkManager.getPerkById(perkId);
        if (perk == null) return false;
        return perkManager.forceAddPerkToPlayer(uuid, perk, PlayerPerkGainedEvent.Source.API);
    }

    @Override
    public boolean removePerk(UUID uuid, String perkId) {
        Perk perk = perkManager.getPerkById(perkId);
        if (perk == null) return false;
        if (!perkManager.hasPerk(uuid, perkId)) return false;
        perkManager.removePerk(uuid, perk);
        return true;
    }

    @Override public void removeAllPerks(UUID uuid) { perkManager.removeAllPerks(uuid); }

    @Override
    public List<String> getPlayerPerkIds(UUID uuid) {
        List<String> ids = new ArrayList<>();
        for (Perk p : perkManager.getPlayerPerks(uuid)) ids.add(p.getId());
        return Collections.unmodifiableList(ids);
    }

    @Override public boolean hasPerk(UUID uuid, String perkId) { return perkManager.hasPerk(uuid, perkId); }

    // ===== Economy =====

    @Override public int getGold(UUID uuid) { return economyManager.getGold(uuid); }
    @Override public void setGold(UUID uuid, int amount) { economyManager.setGold(uuid, amount); }
    @Override public void addGold(UUID uuid, int amount) { economyManager.addGold(uuid, amount); }
    @Override public boolean removeGold(UUID uuid, int amount) { return economyManager.removeGold(uuid, amount); }

    // ===== Phase =====

    @Override public boolean isNight() { return dayNightManager.isNight(); }
    @Override public boolean isDayNightEnabled() { return dayNightManager.isEnabled(); }
    @Override public void forceDay() { dayNightManager.forceDay(); }
    @Override public void forceNight() { dayNightManager.forceNight(); }
}

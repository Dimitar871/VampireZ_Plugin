package com.vampirez;

import com.vampirez.api.event.PlayerConvertedEvent;
import com.vampirez.api.event.PlayerVampireRespawnEvent;
import com.vampirez.api.event.VampireZGameEndEvent;
import com.vampirez.api.event.VampireZGameStartEvent;
import com.vampirez.config.PluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class GameManager {

    private static final Logger log = LoggerFactory.getLogger(GameManager.class);

    private final VampireZPlugin plugin;
    private final EconomyManager economyManager;
    private final PerkManager perkManager;
    private final GearManager gearManager;
    private final DayNightManager dayNightManager;
    private final ScoreboardManager scoreboardManager;

    /** Lazy reference — PerkSelectionGUI takes GameManager in its constructor (cycle). */
    private final java.util.function.Supplier<PerkSelectionGUI> perkSelectionGUISupplier;
    private final StatAnvilManager statAnvilManager;
    private final ArenaManager arenaManager;
    private final PlayerStateManager playerStateManager;

    private final SpawnManager spawnManager;
    private final GameAnnouncer announcer;
    private final TeamManager teamManager = new TeamManager();
    private final GameTimerManager timerManager = new GameTimerManager();
    private final GameStateManager stateManager = new GameStateManager();
    private final BossBarManager bossBarManager = new BossBarManager();

    private final Set<UUID> joinedPlayers = new HashSet<>();
    /** Stores how many perks to auto-assign (with tiers) for players who disconnected mid-game. */
    private final Map<UUID, List<PerkTier>> pendingAutoPerks = new HashMap<>();

    private int minPlayers;
    private int gameDurationSeconds;
    private double vampireRatio;
    private int vampireRespawnDelayTicks;
    private int startCountdownSeconds;
    private int autoStartCountdownSeconds;

    public GameManager(VampireZPlugin plugin, EconomyManager economyManager, PerkManager perkManager,
                       GearManager gearManager, DayNightManager dayNightManager, ScoreboardManager scoreboardManager,
                       ArenaManager arenaManager, PlayerStateManager playerStateManager,
                       StatAnvilManager statAnvilManager,
                       java.util.function.Supplier<PerkSelectionGUI> perkSelectionGUISupplier) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.perkManager = perkManager;
        this.gearManager = gearManager;
        this.dayNightManager = dayNightManager;
        this.scoreboardManager = scoreboardManager;
        this.arenaManager = arenaManager;
        this.playerStateManager = playerStateManager;
        this.statAnvilManager = statAnvilManager;
        this.perkSelectionGUISupplier = perkSelectionGUISupplier;
        this.spawnManager = new SpawnManager(plugin, () -> this.arenaManager);
        this.announcer = new GameAnnouncer(plugin, this::getJoinedOnlinePlayers);

        loadConfig();
        scoreboardManager.setupLobbyScoreboard();
    }

    public SpawnManager getSpawnManager() { return spawnManager; }
    public GameAnnouncer getAnnouncer() { return announcer; }

    private void loadConfig() {
        PluginConfig.GameSection game = plugin.getPluginConfig().game;
        minPlayers = game.minPlayers;
        gameDurationSeconds = game.gameDurationSeconds;
        vampireRatio = game.vampireRatio;
        vampireRespawnDelayTicks = game.vampireRespawnDelayTicks;
        startCountdownSeconds = game.startCountdownSeconds;
        autoStartCountdownSeconds = game.lobbyCountdownSeconds;

        perkManager.setMaxPerks(plugin.getPluginConfig().perks.maxPerksPerPlayer);
        teamManager.setCombatTagMs(plugin.getPluginConfig().timings.combatTagMs);

        spawnManager.loadSpawns();
    }

    /**
     * Hot-reload all manager configs from disk. Safe to call during LOBBY only.
     * Re-reads config.yml from disk, then propagates new values to all managers.
     */
    public void reloadAllConfig() {
        plugin.reloadPluginConfig();
        loadConfig();
        economyManager.reloadConfig();
        dayNightManager.reloadConfig();
        perkManager.setDisabledPerks(plugin.getPluginConfig().perks.disabledPerks);
        plugin.reloadDataDrivenPerks();
    }

    public StatAnvilManager getStatAnvilManager()      { return statAnvilManager; }
    public ArenaManager getArenaManager()              { return arenaManager; }
    public PlayerStateManager getPlayerStateManager()  { return playerStateManager; }

    // ===== JOIN / LEAVE =====

    /**
     * Player opts in to the VampireZ game. Saves their survival state and teleports to lobby.
     */
    public boolean joinGame(Player player) {
        UUID uuid = player.getUniqueId();
        if (joinedPlayers.contains(uuid)) {
            player.sendMessage(MM.parse("<red>You are already in the VampireZ lobby!"));
            return false;
        }
        if (stateManager.getState() != GameState.LOBBY) {
            player.sendMessage(MM.parse("<red>A game is already in progress! Wait for it to end."));
            return false;
        }

        // A dead player would snapshot health=0 — restoring that later would kill
        // them on the spot. Respawn first so the saved state is always livable.
        if (player.isDead()) {
            player.spigot().respawn();
        }

        // Save survival state to disk, then clear player
        playerStateManager.saveAndClear(player);
        joinedPlayers.add(uuid);
        // Defensive: wipe any perks left over from a previous game (e.g. from a delayed
        // conversion-GUI auto-pick that fired after resetToLobby).
        perkManager.removeAllPerks(uuid);
        economyManager.resetPlayer(uuid);

        // Teleport to lobby in adventure mode (arena is protected)
        if (spawnManager.getLobbySpawn() != null) {
            player.teleport(spawnManager.getLobbySpawn());
        }
        player.setGameMode(org.bukkit.GameMode.ADVENTURE);

        player.sendMessage(MM.parse("<green>You joined VampireZ! <gray>Waiting for the game to start..."));
        scoreboardManager.showLobbyScoreboard(player);
        scoreboardManager.updateLobbyScoreboard(joinedPlayers.size(), minPlayers, timerManager.getAutoStartCountdown());

        // Announce
        for (UUID jUuid : joinedPlayers) {
            Player p = Bukkit.getPlayer(jUuid);
            if (p != null && !p.equals(player)) {
                p.sendMessage(MM.parse("<yellow>" + player.getName() + " joined! <gray>(" + joinedPlayers.size() + "/" + minPlayers + ")"));
            }
        }

        checkAndTriggerAutoStart();
        return true;
    }

    /**
     * Player opts out of VampireZ. Restores their survival state.
     */
    public void leaveGame(Player player) {
        UUID uuid = player.getUniqueId();
        if (!joinedPlayers.contains(uuid)) {
            player.sendMessage(MM.parse("<red>You are not in a VampireZ game!"));
            return;
        }

        boolean wasInActiveGame = (stateManager.getState() == GameState.ACTIVE || stateManager.getState() == GameState.STARTING) && isInGame(uuid);

        // Remove from teams
        boolean wasHuman = teamManager.getHumanTeam().remove(uuid);
        teamManager.getVampireTeam().remove(uuid);
        perkManager.removeAllPerks(uuid);
        economyManager.resetPlayer(uuid);
        scoreboardManager.removePlayer(uuid);
        joinedPlayers.remove(uuid);

        // Restoring onto a corpse silently loses everything — the respawn that follows
        // wipes the freshly restored inventory. Respawn first.
        if (player.isDead()) {
            player.spigot().respawn();
        }

        // Restore survival state (wipes game items first, then restores saved inventory)
        playerStateManager.restore(player);

        player.sendMessage(MM.parse("<yellow>You left VampireZ. Your inventory has been restored."));

        if (stateManager.getState() == GameState.LOBBY) {
            if (timerManager.autoStartTask != null && getJoinedOnlinePlayers().size() < minPlayers) {
                cancelAutoStartCountdown("Not enough players (" + getJoinedOnlinePlayers().size() + "/" + minPlayers + ").");
            }
            scoreboardManager.updateLobbyScoreboard(joinedPlayers.size(), minPlayers, timerManager.getAutoStartCountdown());
        }

        // Win / team-integrity check
        if (wasInActiveGame) {
            if (stateManager.getState() == GameState.STARTING) {
                // Game hasn't begun — leaving must not award a vampire win.
                switch (afterStartingDropout(teamManager.getHumanTeam().size(), teamManager.getVampireTeam().size())) {
                    case ABORT_GAME -> {
                        announcer.broadcast(MM.parse("<red>All humans left during the countdown — game aborted."));
                        stopGame();
                    }
                    case PROMOTE_HUMAN_TO_VAMPIRE -> promoteRandomHumanToVampire();
                    case CONTINUE -> { }
                }
            } else if (wasHuman && teamManager.getHumanTeam().isEmpty() && !teamManager.getVampireTeam().isEmpty()) {
                endGame(false);
            }
        }
    }

    public boolean isJoined(UUID uuid) {
        return joinedPlayers.contains(uuid);
    }

    public Set<UUID> getJoinedPlayers() {
        return Collections.unmodifiableSet(joinedPlayers);
    }

    /**
     * Returns online players that are in the VampireZ game (joined).
     */
    public List<Player> getJoinedOnlinePlayers() {
        return getOnlineTeamPlayers(joinedPlayers);
    }

    /**
     * Resolve a UUID set to live {@link Player} instances, dropping offline / missing entries.
     * Use this anywhere you'd otherwise write the {@code Bukkit.getPlayer + null + isOnline}
     * trio. Cheap to call (no copy of the underlying Set; just an iterating filter).
     */
    public List<Player> getOnlineTeamPlayers(Set<UUID> uuids) {
        List<Player> players = new ArrayList<>(uuids.size());
        for (UUID uuid : uuids) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) players.add(p);
        }
        return players;
    }

    // ===== AUTO-START =====

    private void checkAndTriggerAutoStart() {
        if (stateManager.getState() != GameState.LOBBY) return;
        if (timerManager.autoStartTask != null) return;
        if (!hasSpawnsSet()) return;
        if (getJoinedOnlinePlayers().size() >= minPlayers) {
            startAutoStartCountdown();
        }
    }

    private void startAutoStartCountdown() {
        timerManager.setAutoStartCountdown(autoStartCountdownSeconds);
        Component prefix = MM.legacy(plugin.getPluginConfig().messages.prefix);
        for (Player p : getJoinedOnlinePlayers()) {
            p.sendMessage(prefix.append(MM.parse("<green>Enough players! Starting in <yellow><bold>" + timerManager.getAutoStartCountdown() + "</bold><green> seconds.")));
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
        }
        scoreboardManager.updateLobbyScoreboard(joinedPlayers.size(), minPlayers, timerManager.getAutoStartCountdown());

        timerManager.autoStartTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (stateManager.getState() != GameState.LOBBY) {
                timerManager.autoStartTask.cancel();
                timerManager.autoStartTask = null;
                timerManager.setAutoStartCountdown(-1);
                return;
            }

            timerManager.setAutoStartCountdown(timerManager.getAutoStartCountdown() - 1);

            if (timerManager.getAutoStartCountdown() == 20 || timerManager.getAutoStartCountdown() == 10) {
                for (Player p : getJoinedOnlinePlayers()) {
                    p.sendMessage(prefix.append(MM.parse("<yellow>Game starting in <bold>" + timerManager.getAutoStartCountdown() + "</bold><yellow> seconds!")));
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                }
            } else if (timerManager.getAutoStartCountdown() <= 5 && timerManager.getAutoStartCountdown() > 0) {
                for (Player p : getJoinedOnlinePlayers()) {
                    p.sendMessage(prefix.append(MM.parse("<red><bold>" + timerManager.getAutoStartCountdown() + "...")));
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f + (timerManager.getAutoStartCountdown() * 0.05f));
                }
            }

            scoreboardManager.updateLobbyScoreboard(joinedPlayers.size(), minPlayers, timerManager.getAutoStartCountdown());

            if (timerManager.getAutoStartCountdown() <= 0) {
                timerManager.autoStartTask.cancel();
                timerManager.autoStartTask = null;
                timerManager.setAutoStartCountdown(-1);
                if (canStart()) {
                    startGame(false);
                }
            }
        }, 20L, 20L);
    }

    private void cancelAutoStartCountdown(String reason) {
        if (timerManager.autoStartTask != null) {
            timerManager.autoStartTask.cancel();
            timerManager.autoStartTask = null;
        }
        timerManager.setAutoStartCountdown(-1);
        if (reason != null) {
            Component prefix = MM.legacy(plugin.getPluginConfig().messages.prefix);
            for (Player p : getJoinedOnlinePlayers()) {
                p.sendMessage(prefix.append(MM.parse("<red>Auto-start cancelled: " + reason)));
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f);
            }
        }
        scoreboardManager.updateLobbyScoreboard(joinedPlayers.size(), minPlayers, -1);
    }

    public int getAutoStartCountdown() { return timerManager.getAutoStartCountdown(); }

    // ===== GAME START =====

    public boolean canStart() {
        return joinedPlayers.size() >= minPlayers && hasSpawnsSet();
    }

    public boolean hasSpawnsSet() { return spawnManager.hasSpawnsSet(); }

    public void startGame(boolean force) {
        if (stateManager.getState() != GameState.LOBBY) return;
        if (!force && !canStart()) return;
        if (!hasSpawnsSet()) return;

        // Cancel any pending auto-start countdown silently
        if (timerManager.autoStartTask != null) {
            timerManager.autoStartTask.cancel();
            timerManager.autoStartTask = null;
        }
        timerManager.setAutoStartCountdown(-1);

        stateManager.setState(GameState.STARTING);
        stateManager.setLastStartForced(force);

        // Auto-assign teams (only from joined players)
        List<Player> players = getJoinedOnlinePlayers();
        Collections.shuffle(players);
        int vampCount = Math.max(1, (int) Math.round(players.size() * vampireRatio));

        teamManager.getHumanTeam().clear();
        teamManager.getVampireTeam().clear();

        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            if (i < vampCount) {
                teamManager.getVampireTeam().add(player.getUniqueId());
            } else {
                teamManager.getHumanTeam().add(player.getUniqueId());
            }
        }

        // Reset all players fully, then teleport and give gear
        for (UUID uuid : teamManager.getHumanTeam()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                // Defensive: clear any leftover perks before assigning loadout.
                // resetPlayerFully handles inventory/attrs/potions; this catches the perk registry.
                perkManager.removeAllPerks(uuid);
                resetPlayerFully(player);
                player.setWalkSpeed(0.2f); // default walk speed
                player.setFlySpeed(0.1f);
                player.teleport(spawnManager.getHumanSpawn());
                gearManager.giveHumanGear(player);
                player.sendMessage(MM.parse("<aqua><bold>You are a HUMAN!</bold> <yellow>You have 45 seconds to loot before the Vampires are released!"));
                player.showTitle(Title.title(
                        MM.parse("<aqua><bold>HUMAN"),
                        MM.parse("<yellow>45 seconds to loot!"),
                        Title.Times.times(java.time.Duration.ofMillis(500), java.time.Duration.ofSeconds(3), java.time.Duration.ofMillis(500))));
            }
        }
        // Vampires: invisible scouts during 45s head-start, no gear
        teamManager.setVampiresReleased(false);
        for (UUID uuid : teamManager.getVampireTeam()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                perkManager.removeAllPerks(uuid);
                resetPlayerFully(player);
                player.setWalkSpeed(0.2f);
                player.setFlySpeed(0.1f);
                player.teleport(spawnManager.getVampireSpawn());
                // Invisibility during scouting phase (extra buffer ticks)
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.INVISIBILITY, 50 * 20, 0, false, false, false));
                // Night vision so they can see
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.NIGHT_VISION, 999999, 0, false, false, true));
                player.sendMessage(MM.parse("<dark_red><bold>You are a VAMPIRE!</bold> <gray>Scout the arena invisibly. Released in 45 seconds."));
                player.showTitle(Title.title(
                        MM.parse("<dark_red><bold>VAMPIRE"),
                        MM.parse("<gray>Scouting phase - 45 seconds..."),
                        Title.Times.times(java.time.Duration.ofMillis(500), java.time.Duration.ofSeconds(3), java.time.Duration.ofMillis(500))));
            }
        }

        // Setup scoreboards for joined players
        for (Player player : getJoinedOnlinePlayers()) {
            scoreboardManager.createGameScoreboard(player, this, economyManager, perkManager, dayNightManager);
        }

        // Open free Silver perk selection for HUMANS only (vampires get theirs on release)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (stateManager.getState() != GameState.ACTIVE) return;
            for (UUID uuid : teamManager.getHumanTeam()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline() && perkSelectionGUISupplier.get() != null) {
                    perkSelectionGUISupplier.get().openFreeSelection(player, PerkTeam.HUMAN);
                }
            }
        }, 20L);

        // Start the game immediately (humans loot while vampires scout)
        beginActiveGame();

        // Start 45-second vampire release countdown
        final int[] vampCountdown = {45};
        timerManager.vampireReleaseTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (stateManager.getState() != GameState.ACTIVE) {
                if (timerManager.vampireReleaseTask != null) { timerManager.vampireReleaseTask.cancel(); timerManager.vampireReleaseTask = null; }
                return;
            }

            // Action bar countdown for vampires
            for (UUID uuid : teamManager.getVampireTeam()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    if (vampCountdown[0] > 0) {
                        player.sendActionBar(MM.parse("<red>\u2694 <gold>Releasing in <red><bold>" + vampCountdown[0] + "</bold><gold>s <red>\u2694"));
                    }
                }
            }

            // Milestone announcements to joined players
            if (vampCountdown[0] == 30 || vampCountdown[0] == 15 || vampCountdown[0] == 10) {
                for (Player player : getJoinedOnlinePlayers()) {
                    player.sendMessage(MM.parse("<red>Vampires release in <bold>" + vampCountdown[0] + "</bold><red> seconds!"));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                }
            }
            if (vampCountdown[0] <= 5 && vampCountdown[0] > 0) {
                for (Player player : getJoinedOnlinePlayers()) {
                    player.sendMessage(MM.parse("<dark_red><bold>" + vampCountdown[0] + "..."));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
                }
            }

            vampCountdown[0]--;
            if (vampCountdown[0] < 0) {
                releaseVampires();
                if (timerManager.vampireReleaseTask != null) { timerManager.vampireReleaseTask.cancel(); timerManager.vampireReleaseTask = null; }
            }
        }, 20L, 20L);
    }

    private void beginActiveGame() {
        stateManager.setState(GameState.ACTIVE);
        timerManager.setRemainingSeconds(gameDurationSeconds);
        timerManager.setActiveStartedAtMs(System.currentTimeMillis());
        timerManager.getFiredTimedMilestones().clear();

        // Disable random ticks (prevents leaf decay, crop growth, etc.)
        if (spawnManager.getHumanSpawn() != null && spawnManager.getHumanSpawn().getWorld() != null) {
            spawnManager.getHumanSpawn().getWorld().setGameRule(org.bukkit.GameRule.RANDOM_TICK_SPEED, 0);
            spawnManager.getHumanSpawn().getWorld().setDifficulty(org.bukkit.Difficulty.HARD);
        }
        if (spawnManager.getVampireSpawn() != null && spawnManager.getVampireSpawn().getWorld() != null) {
            spawnManager.getVampireSpawn().getWorld().setGameRule(org.bukkit.GameRule.RANDOM_TICK_SPEED, 0);
            spawnManager.getVampireSpawn().getWorld().setDifficulty(org.bukkit.Difficulty.HARD);
        }

        announcer.broadcastGameStart(teamManager.isVampiresReleased());

        // Start timer
        timerManager.timerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (stateManager.getState() != GameState.ACTIVE) return;

            timerManager.decrementRemainingSeconds();

            // Timed free perk triggers — fire each milestone at most once per game.
            // Use wall-clock elapsed so /vz settime can't replay or skip milestones.
            int elapsed = getActiveElapsedSeconds();
            PluginConfig.TimingsSection timings = plugin.getPluginConfig().timings;
            for (int milestone : new int[] {
                    timings.freeSilverPerkAtSeconds,
                    timings.freeGoldPerkAtSeconds,
                    timings.freePrismaticPerkAtSeconds }) {
                if (elapsed >= milestone && timerManager.getFiredTimedMilestones().add(milestone)) {
                    triggerTimedPerk();
                }
            }

            // Blood Compass — give to all vampires at the configured remaining-time mark
            if (timerManager.getRemainingSeconds() == timings.bloodCompassUnlockAtRemainingSeconds
                    && !teamManager.isBloodCompassGiven()) {
                teamManager.setBloodCompassGiven(true);
                for (UUID uuid : teamManager.getVampireTeam()) {
                    Player vamp = Bukkit.getPlayer(uuid);
                    if (vamp != null && vamp.isOnline()) {
                        giveBloodCompass(vamp);
                    }
                }
                for (Player p : getJoinedOnlinePlayers()) {
                    p.sendMessage(MM.parse("<dark_red><bold>Blood Compass</bold> <red>activated! Vampires can now sense human locations."));
                    p.playSound(p.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.5f, 1.2f);
                }
            }

            // Update blood compass targets every second
            if (teamManager.isBloodCompassGiven()) {
                updateBloodCompasses();
            }

            // Milestone announcements
            if (timerManager.getRemainingSeconds() == 300 || timerManager.getRemainingSeconds() == 60 || timerManager.getRemainingSeconds() == 30) {
                int min = timerManager.getRemainingSeconds() / 60;
                int sec = timerManager.getRemainingSeconds() % 60;
                String timeMsg = min > 0 ? min + " minute" + (min > 1 ? "s" : "") : sec + " seconds";
                for (Player player : getJoinedOnlinePlayers()) {
                    player.sendMessage(MM.parse("<yellow>" + timeMsg + " remaining!"));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
                }
            }

            if (timerManager.getRemainingSeconds() <= 5 && timerManager.getRemainingSeconds() > 0) {
                for (Player player : getJoinedOnlinePlayers()) {
                    player.sendMessage(MM.parse("<red>" + timerManager.getRemainingSeconds() + "..."));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
                }
            }

            if (timerManager.getRemainingSeconds() <= 0) {
                endGame(true); // Humans win
            }
        }, 20L, 20L);

        // Start scoreboard + boss bar updates (1 Hz)
        timerManager.scoreboardTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            scoreboardManager.updateAllGameScoreboards(this, economyManager, perkManager, dayNightManager);
            bossBarManager.update(
                    timerManager.getRemainingSeconds(),
                    gameDurationSeconds,
                    dayNightManager.isNight(),
                    teamManager.getHumanTeam().size(),
                    teamManager.getVampireTeam().size());
        }, 20L, 20L);

        // Show boss bar to every joined player
        for (Player p : getJoinedOnlinePlayers()) bossBarManager.show(p);
        bossBarManager.setVisible(true);

        // Start economy
        economyManager.startPassiveIncome(this);

        // Start day/night cycle
        dayNightManager.startCycle();

        // Notify external plugins
        Bukkit.getPluginManager().callEvent(new VampireZGameStartEvent(teamManager.getHumanTeam(), teamManager.getVampireTeam(), stateManager.isLastStartForced()));
    }

    // ===== VAMPIRE RELEASE (after 45s scouting phase) =====

    private void releaseVampires() {
        teamManager.setVampiresReleased(true);

        for (UUID uuid : teamManager.getVampireTeam()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                // Remove invisibility
                player.removePotionEffect(PotionEffectType.INVISIBILITY);
                // Teleport to vampire spawn
                if (spawnManager.getVampireSpawn() != null) {
                    player.teleport(spawnManager.getVampireSpawn());
                }
                // Give vampire gear
                gearManager.giveVampireGear(player);
                // Apply perks and effects
                perkManager.reapplyPerks(uuid);
                dayNightManager.applyEffectsToPlayer(player);
                if (statAnvilManager != null) statAnvilManager.reapplyBuffs(player);

                player.showTitle(Title.title(
                        MM.parse("<dark_red><bold>HUNT!"),
                        MM.parse("<red>The Vampires have been released!"),
                        Title.Times.times(java.time.Duration.ofMillis(250), java.time.Duration.ofMillis(1500), java.time.Duration.ofMillis(500))));
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 0.8f);
            }
        }

        // Announce to humans
        for (UUID uuid : teamManager.getHumanTeam()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.showTitle(Title.title(
                        MM.parse("<dark_red><bold>BEWARE!"),
                        MM.parse("<red>The Vampires have been released!"),
                        Title.Times.times(java.time.Duration.ofMillis(250), java.time.Duration.ofMillis(1500), java.time.Duration.ofMillis(500))));
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 0.8f);
                player.sendMessage(MM.parse("<red><bold>The Vampires have been released!</bold> <yellow>The hunt begins!"));
            }
        }

        // Open free Silver perk selection for vampires
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (stateManager.getState() != GameState.ACTIVE) return;
            for (UUID uuid : teamManager.getVampireTeam()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline() && perkSelectionGUISupplier.get() != null) {
                    perkSelectionGUISupplier.get().openFreeSelection(player, PerkTeam.VAMPIRE);
                }
            }
        }, 20L);
    }

    // ===== HUMAN DEATH → CONVERT TO VAMPIRE =====

    public void convertHumanToVampire(Player player) {
        convertHumanToVampire(player, PlayerConvertedEvent.Cause.DEATH);
    }

    /**
     * @return true if the conversion happened, false if the player wasn't a human or
     *         the {@link PlayerConvertedEvent} was cancelled (player respawned as human).
     */
    public boolean convertHumanToVampire(Player player, PlayerConvertedEvent.Cause cause) {
        UUID uuid = player.getUniqueId();
        if (!teamManager.getHumanTeam().contains(uuid)) return false;

        PlayerConvertedEvent event = new PlayerConvertedEvent(uuid, cause);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            // Plugin vetoed the conversion — respawn as human, full HP, gear + perks intact.
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;
                if (player.isDead()) player.spigot().respawn();
                player.setGameMode(GameMode.SURVIVAL);
                if (spawnManager.getHumanSpawn() != null) player.teleport(spawnManager.getHumanSpawn());
                gearManager.giveHumanGear(player);
                perkManager.reapplyPerks(uuid);
                org.bukkit.attribute.AttributeInstance maxHp = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                if (maxHp != null) player.setHealth(maxHp.getValue());
                player.setFoodLevel(20);
                player.setSaturation(20f);
                player.sendMessage(MM.parse("<aqua>You were saved! You remain a Human."));
            }, 40L);
            return false;
        }

        announcer.broadcastConversion(player);

        // Switch teams; true means this was the last human (vampires win)
        boolean humansExtinct = teamManager.convertToVampire(uuid);

        // Reset gold on conversion (always, whether death or disconnect)
        economyManager.resetPlayer(uuid);

        // Remove human-specific perks, get count for replacement
        List<Perk> removedPerks = perkManager.removeTeamSpecificPerks(uuid, PerkTeam.HUMAN);

        // Update scoreboards for team change
        scoreboardManager.updateTeamChangeOnAllScoreboards(player, this);

        // Give vampire gear after short delay (respawn)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            if (player.isDead()) player.spigot().respawn();
            player.setGameMode(GameMode.SURVIVAL);
            gearManager.giveVampireGear(player);
            perkManager.reapplyPerks(uuid);
            dayNightManager.applyEffectsToPlayer(player);
            if (statAnvilManager != null) statAnvilManager.reapplyBuffs(player);

            player.sendMessage(MM.parse("<dark_red>You have become a Vampire! Hunt the remaining humans!"));

            // Give blood compass if already active
            if (teamManager.isBloodCompassGiven()) {
                giveBloodCompass(player);
            }

            // Open free replacement perk selection for each removed human-only perk,
            // each pick at the same tier as the perk it replaces.
            if (!removedPerks.isEmpty() && perkSelectionGUISupplier.get() != null) {
                List<PerkTier> lostTiers = removedPerks.stream().map(Perk::getTier).toList();
                perkSelectionGUISupplier.get().openConversionSelection(player, PerkTeam.VAMPIRE, lostTiers);
            }
        }, 40L); // 2 second delay

        // Check win condition
        if (humansExtinct) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> endGame(false), 20L);
        }
        return true;
    }

    // ===== VAMPIRE RESPAWN =====

    public void respawnVampire(Player player) {
        UUID uuid = player.getUniqueId();
        if (!teamManager.getVampireTeam().contains(uuid)) return;

        player.sendMessage(MM.parse("<red>Respawning in " + (vampireRespawnDelayTicks / 20) + " seconds..."));

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            if (stateManager.getState() != GameState.ACTIVE) return;
            if (player.isDead()) player.spigot().respawn();

            Bukkit.getPluginManager().callEvent(new PlayerVampireRespawnEvent(uuid, spawnManager.getVampireSpawn()));
            if (spawnManager.getVampireSpawn() != null) {
                player.teleport(spawnManager.getVampireSpawn());
            }
            player.setGameMode(GameMode.SURVIVAL);
            gearManager.giveVampireGear(player);
            perkManager.reapplyPerks(uuid);
            dayNightManager.applyEffectsToPlayer(player);
            if (statAnvilManager != null) statAnvilManager.reapplyBuffs(player);
            // Speed II for 5 seconds on respawn, but don't override Homeguard's Speed V
            if (!com.vampirez.perks.HomeguardPerk.hasActiveSpeed(uuid)) {
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.SPEED, 100, 1, false, false));
            }
        }, vampireRespawnDelayTicks);
    }

    // ===== GAME END =====

    public void endGame(boolean humansWin) {
        if (stateManager.getState() == GameState.ENDING || stateManager.getState() == GameState.LOBBY) return;
        stateManager.setState(GameState.ENDING);

        int playedSec = timerManager.getActiveElapsedSeconds();
        Bukkit.getPluginManager().callEvent(new VampireZGameEndEvent(
                humansWin ? VampireZGameEndEvent.Winner.HUMANS : VampireZGameEndEvent.Winner.VAMPIRES,
                playedSec));

        // Stop tasks
        timerManager.cancelAllTasks();
        teamManager.setVampiresReleased(true);
        // Hide boss bar from every joined player
        for (Player p : getJoinedOnlinePlayers()) bossBarManager.hide(p);
        bossBarManager.setVisible(false);
        economyManager.stopPassiveIncome();
        dayNightManager.stopCycle();

        // Close any open perk-selection GUIs BEFORE wiping perks, so no countdown
        // or click handler can re-add perks after the reset (BUG_PERK_PERSISTENCE).
        if (perkSelectionGUISupplier.get() != null) {
            perkSelectionGUISupplier.get().cancelAllOpen();
        }
        // Clear all perks immediately so perk ticks (Undead Horde, etc.) stop.
        // resetAll() also clears cross-player perk state (webs, curses, stacks).
        perkManager.resetAll();

        announcer.broadcastWinner(humansWin);

        // Reset after 10 seconds
        Bukkit.getScheduler().runTaskLater(plugin, this::resetToLobby, 200L);
    }

    public void stopGame() {
        if (stateManager.getState() == GameState.LOBBY) return;

        // Stop tasks and perks immediately (same as endGame)
        stateManager.setState(GameState.ENDING);

        int playedSec = timerManager.getActiveElapsedSeconds();
        Bukkit.getPluginManager().callEvent(new VampireZGameEndEvent(
                VampireZGameEndEvent.Winner.STOPPED, playedSec));
        timerManager.cancelAllTasks();
        teamManager.setVampiresReleased(true);
        // Hide boss bar from every joined player
        for (Player p : getJoinedOnlinePlayers()) bossBarManager.hide(p);
        bossBarManager.setVisible(false);
        economyManager.stopPassiveIncome();
        dayNightManager.stopCycle();
        if (perkSelectionGUISupplier.get() != null) {
            perkSelectionGUISupplier.get().cancelAllOpen();
        }
        perkManager.resetAll();

        // Reset immediately (no 10s delay — server may be shutting down)
        resetToLobby();
    }

    private void resetToLobby() {
        stateManager.setState(GameState.LOBBY);
        timerManager.cancelAutoStartTask();

        // Cleanup
        if (perkSelectionGUISupplier.get() != null) {
            perkSelectionGUISupplier.get().cancelAllOpen();
        }
        for (Perk perk : perkManager.getAllPerks()) {
            perk.clearAllStats();
        }
        perkManager.resetAll();
        economyManager.resetAll();
        scoreboardManager.resetAll();
        if (statAnvilManager != null) statAnvilManager.resetAll();
        teamManager.setVampiresReleased(true);
        teamManager.setBloodCompassGiven(false);
        pendingAutoPerks.clear();

        teamManager.getHumanTeam().clear();
        teamManager.getVampireTeam().clear();

        boolean resettingArena = arenaManager != null && arenaManager.hasTemplate();

        // Wipe all players' game state
        for (UUID uuid : new HashSet<>(joinedPlayers)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                resetPlayerFully(player);
                player.getInventory().clear();
                player.getInventory().setArmorContents(new org.bukkit.inventory.ItemStack[4]);
                player.getInventory().setItemInOffHand(null);
                player.setGameMode(org.bukkit.GameMode.ADVENTURE);
                player.setHealth(player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getBaseValue());
                player.setFoodLevel(20);
                player.setSaturation(20f);
                if (resettingArena) {
                    // Arena world is about to be unloaded — park players in the main world temporarily
                    World holdWorld = Bukkit.getWorlds().get(0);
                    Location hold = holdWorld.getSpawnLocation().clone();
                    hold.setY(holdWorld.getHighestBlockYAt(hold.getBlockX(), hold.getBlockZ()) + 1.5);
                    player.teleport(hold);
                } else if (spawnManager.getLobbySpawn() != null) {
                    // No arena reset — go straight to lobby spawn, no intermediate teleport
                    player.teleport(spawnManager.getLobbySpawn());
                }
                // NOTE: the saved-state file is deliberately KEPT. The player stays in the
                // VampireZ lobby for the next round; their pre-join inventory is restored
                // by /vz leave or restoreLobbyPlayers() on shutdown — both read this file.
                // (Deleting it here was the v2.5 item-loss bug: restore() then no-op'd.)
            } else {
                joinedPlayers.remove(uuid);
            }
        }

        if (resettingArena) {
            // Reset the arena world, then bring everyone to the fresh lobby spawn
            arenaManager.resetArena(() -> {
                spawnManager.loadSpawns();
                log.info("Arena world has been reset and reloaded.");

                scoreboardManager.setupLobbyScoreboard();
                for (UUID uuid : new HashSet<>(joinedPlayers)) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null && player.isOnline()) {
                        if (spawnManager.getLobbySpawn() != null) player.teleport(spawnManager.getLobbySpawn());
                        scoreboardManager.showLobbyScoreboard(player);
                    }
                }
                scoreboardManager.updateLobbyScoreboard(joinedPlayers.size(), minPlayers, timerManager.getAutoStartCountdown());
            });
        } else {
            // No arena reset — set up lobby scoreboard immediately
            scoreboardManager.setupLobbyScoreboard();
            for (UUID uuid : new HashSet<>(joinedPlayers)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    scoreboardManager.showLobbyScoreboard(player);
                }
            }
            scoreboardManager.updateLobbyScoreboard(joinedPlayers.size(), minPlayers, timerManager.getAutoStartCountdown());
        }
    }

    /**
     * Fully resets a player to a clean lobby state: game mode, inventory, potion effects,
     * and ALL attribute modifiers that perks may have added.
     */
    public void resetPlayerFully(Player player) {
        // If the player is dead, force respawn by scheduling after spigot handles it
        if (player.isDead()) {
            player.spigot().respawn();
        }

        player.setGameMode(GameMode.SURVIVAL);
        player.getInventory().clear();
        player.getInventory().setArmorContents(new org.bukkit.inventory.ItemStack[4]);
        player.setFoodLevel(20);
        player.setSaturation(20f);

        // Remove ALL potion effects
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        // Reset all attributes and clear any custom modifiers perks may have added
        org.bukkit.attribute.Attribute[] attributesToReset = {
                org.bukkit.attribute.Attribute.MAX_HEALTH,
                org.bukkit.attribute.Attribute.ATTACK_SPEED,
                org.bukkit.attribute.Attribute.MOVEMENT_SPEED,
                org.bukkit.attribute.Attribute.ARMOR,
                org.bukkit.attribute.Attribute.ARMOR_TOUGHNESS,
                org.bukkit.attribute.Attribute.KNOCKBACK_RESISTANCE
        };
        for (org.bukkit.attribute.Attribute attr : attributesToReset) {
            org.bukkit.attribute.AttributeInstance instance = player.getAttribute(attr);
            if (instance != null) {
                // Remove all custom modifiers
                for (org.bukkit.attribute.AttributeModifier mod : new java.util.ArrayList<>(instance.getModifiers())) {
                    instance.removeModifier(mod);
                }
            }
        }

        // Hardcode vanilla defaults (Paper 1.21 getDefaultValue() returns wrong values for some attributes)
        player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(20.0);
        player.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED).setBaseValue(0.1);
        player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_SPEED).setBaseValue(4.0);
        player.getAttribute(org.bukkit.attribute.Attribute.ARMOR).setBaseValue(0.0);
        player.getAttribute(org.bukkit.attribute.Attribute.ARMOR_TOUGHNESS).setBaseValue(0.0);
        player.getAttribute(org.bukkit.attribute.Attribute.KNOCKBACK_RESISTANCE).setBaseValue(0.0);

        player.setWalkSpeed(0.2f);
        player.setFlySpeed(0.1f);
        player.setHealth(Health.max(player));
    }

    // ===== PLAYER JOIN/QUIT =====

    public void handlePlayerJoin(Player player) {
        UUID uuid = player.getUniqueId();

        // Re-show boss bar to reconnecting players if a game is in progress
        if (bossBarManager.isVisible() && joinedPlayers.contains(uuid)) bossBarManager.show(player);

        // Case 1: Reconnecting to an active game (they quit mid-game)
        if (joinedPlayers.contains(uuid) && (stateManager.getState() == GameState.ACTIVE || stateManager.getState() == GameState.STARTING)) {
            // Joined the lobby but was offline when teams were assigned (or removed
            // from their team after dropping during the STARTING countdown) — without
            // a team they'd be a ghost: no gear, no scoreboard, uncounted by win
            // conditions. Join as vampire, consistent with the mid-game disconnect
            // rule, owed at least the starting freebie pick.
            if (!teamManager.isInGame(uuid)) {
                teamManager.addVampire(uuid);
                pendingAutoPerks.putIfAbsent(uuid, new ArrayList<>(List.of(PerkTier.SILVER)));
                player.sendMessage(MM.parse("<dark_red>The game started without you — you join the hunt as a Vampire!"));
            }
            if (teamManager.getVampireTeam().contains(uuid)) {
                resetPlayerFully(player);
                player.setGameMode(GameMode.SURVIVAL);

                if (!teamManager.isVampiresReleased()) {
                    // Still in scouting phase — no gear, invisible, teleport to vampire spawn
                    if (spawnManager.getVampireSpawn() != null) player.teleport(spawnManager.getVampireSpawn());
                    player.addPotionEffect(new PotionEffect(
                            PotionEffectType.INVISIBILITY, 50 * 20, 0, false, false, false));
                    player.addPotionEffect(new PotionEffect(
                            PotionEffectType.NIGHT_VISION, 999999, 0, false, false, true));
                    // Auto-assign pending perks WITHOUT applying — releaseVampires()
                    // runs reapplyPerks() on release; applying here too would double-grant
                    // items from item-granting perks.
                    List<PerkTier> pending = pendingAutoPerks.remove(uuid);
                    if (pending != null) {
                        for (PerkTier tier : pending) {
                            List<Perk> options = perkManager.getRandomPerks(tier, PerkTeam.VAMPIRE, 1, uuid);
                            if (!options.isEmpty()) {
                                perkManager.addPerkToPlayer(uuid, options.get(0),
                                        com.vampirez.api.event.PlayerPerkGainedEvent.Source.INTERNAL, false);
                            }
                        }
                    }
                    scoreboardManager.createGameScoreboard(player, this, economyManager, perkManager, dayNightManager);
                    player.sendMessage(MM.parse("<dark_red>You reconnected as a Vampire! <gray>Scouting phase — wait for release."));
                } else {
                    // Vampires already released — give gear normally
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (!player.isOnline() || stateManager.getState() != GameState.ACTIVE) return;
                        if (spawnManager.getVampireSpawn() != null) player.teleport(spawnManager.getVampireSpawn());
                        gearManager.giveVampireGear(player);

                        // Auto-assign pending perks from disconnect WITHOUT applying —
                        // the reapplyPerks() below applies everything exactly once
                        // (applying twice double-granted items from item-granting perks).
                        List<PerkTier> pending = pendingAutoPerks.remove(uuid);
                        if (pending != null) {
                            for (PerkTier tier : pending) {
                                List<Perk> options = perkManager.getRandomPerks(tier, PerkTeam.VAMPIRE, 1, uuid);
                                if (!options.isEmpty()) {
                                    perkManager.addPerkToPlayer(uuid, options.get(0),
                                            com.vampirez.api.event.PlayerPerkGainedEvent.Source.INTERNAL, false);
                                }
                            }
                            int count = perkManager.getPlayerPerkCount(uuid);
                            player.sendMessage(MM.parse("<green>" + count + " perk(s) auto-assigned."));
                        }

                        perkManager.reapplyPerks(uuid);
                        dayNightManager.applyEffectsToPlayer(player);
                        if (statAnvilManager != null) statAnvilManager.reapplyBuffs(player);
                        scoreboardManager.createGameScoreboard(player, this, economyManager, perkManager, dayNightManager);
                        if (teamManager.isBloodCompassGiven()) giveBloodCompass(player);
                        player.sendMessage(MM.parse("<dark_red>You reconnected as a Vampire! Hunt the humans!"));
                    }, 10L);
                }
            }
            return;
        }

        // Case 2: Reconnecting to lobby (quit during LOBBY state)
        if (joinedPlayers.contains(uuid) && stateManager.getState() == GameState.LOBBY) {
            if (spawnManager.getLobbySpawn() != null) player.teleport(spawnManager.getLobbySpawn());
            player.setGameMode(org.bukkit.GameMode.ADVENTURE);
            scoreboardManager.showLobbyScoreboard(player);
            scoreboardManager.updateLobbyScoreboard(joinedPlayers.size(), minPlayers, timerManager.getAutoStartCountdown());
            checkAndTriggerAutoStart();
            player.sendMessage(MM.parse("<green>Welcome back to the VampireZ lobby!"));
            return;
        }

        // Case 3: Player has a saved state from a previous session (game ended while offline, or crash)
        if (playerStateManager != null && playerStateManager.hasSavedState(uuid) && !joinedPlayers.contains(uuid)) {
            playerStateManager.restore(player);
            player.sendMessage(MM.parse("<yellow>Your inventory was restored from a previous VampireZ session."));
            return;
        }

        // Case 4: Normal join — not in VampireZ, do nothing
    }

    public void handlePlayerQuit(Player player) {
        UUID uuid = player.getUniqueId();

        if (!joinedPlayers.contains(uuid)) return; // Not in VampireZ, ignore

        if (stateManager.getState() == GameState.LOBBY) {
            // Keep in joinedPlayers so they rejoin the lobby automatically
            scoreboardManager.removePlayer(uuid);
            // PlayerQuitEvent fires while the quitter still counts as online —
            // subtract them or the countdown survives dropping below the minimum.
            int onlineAfterQuit = getJoinedOnlinePlayers().size() - 1;
            if (timerManager.autoStartTask != null && onlineAfterQuit < minPlayers) {
                cancelAutoStartCountdown("Not enough players online (" + onlineAfterQuit + "/" + minPlayers + ").");
            }
            scoreboardManager.updateLobbyScoreboard(joinedPlayers.size(), minPlayers, timerManager.getAutoStartCountdown());
            return;
        }

        if (stateManager.getState() == GameState.STARTING) {
            // The game hasn't begun — a drop during the countdown must NOT convert
            // (a lag spike at the wrong second would force a whole round as vampire)
            // and must NOT hand vampires a win. Remove from the team; if they
            // reconnect mid-game, Case 1's teamless fallback makes them a vampire.
            boolean wasOnTeam = teamManager.removeHuman(uuid) || teamManager.removeVampire(uuid);
            perkManager.removeAllPerks(uuid);
            economyManager.resetPlayer(uuid);
            scoreboardManager.removePlayer(uuid);

            if (wasOnTeam) {
                switch (afterStartingDropout(teamManager.getHumanTeam().size(), teamManager.getVampireTeam().size())) {
                    case ABORT_GAME -> {
                        announcer.broadcast(MM.parse("<red>All humans dropped during the countdown — game aborted."));
                        stopGame();
                    }
                    case PROMOTE_HUMAN_TO_VAMPIRE -> promoteRandomHumanToVampire();
                    case CONTINUE -> { }
                }
            }
            return;
        }

        if (stateManager.getState() == GameState.ACTIVE) {
            // --- Compensation for reconnect: one auto-perk per perk ACTUALLY held,
            // same tier (human-only perks roll vampire equivalents on reconnect).
            // Reconstructing freebies from elapsed time over-paid: the starting pick
            // could be counted twice (once as freebie, once as human-only replacement).
            boolean startingFreebieGranted =
                    teamManager.isVampiresReleased() || teamManager.getHumanTeam().contains(uuid);
            List<PerkTier> perksToGive =
                    quitCompensationTiers(perkManager.getPlayerPerks(uuid), startingFreebieGranted);

            if (teamManager.getHumanTeam().contains(uuid)) {
                // Convert to vampire (event is informational only — cancellation can't be acted on after disconnect)
                Bukkit.getPluginManager().callEvent(new PlayerConvertedEvent(uuid, PlayerConvertedEvent.Cause.DISCONNECT));
                teamManager.convertToVampire(uuid);

                Component dcPrefix = MM.legacy(plugin.getPluginConfig().messages.prefix);
                for (Player p : getJoinedOnlinePlayers()) {
                    p.sendMessage(dcPrefix.append(MM.parse("<red>" + player.getName() + " disconnected and has been converted to a Vampire!")));
                }

                // Check win condition
                if (teamManager.getHumanTeam().isEmpty() && !teamManager.getVampireTeam().isEmpty()) {
                    endGame(false);
                    return;
                }
            }

            // Store pending perks for reconnect auto-assign
            pendingAutoPerks.put(uuid, perksToGive);

            // Clear perks and economy
            perkManager.removeAllPerks(uuid);
            economyManager.resetPlayer(uuid);
            scoreboardManager.removePlayer(uuid);

            // KEEP in joinedPlayers and teamManager.getVampireTeam() — they can reconnect
            return;
        }

        // ENDING state — keep in joinedPlayers so resetToLobby's offline-cleanup branch
        // picks them up. Their saved-state .yml stays on disk so handlePlayerJoin's Case 3
        // can restore their pre-game inventory on reconnect. Removing here would orphan
        // them: resetToLobby would never see them, and they'd reconnect still holding
        // the gear they had at quit time.
        scoreboardManager.removePlayer(uuid);
    }

    /**
     * Promotes a random remaining human to the vampire team — used when every
     * vampire dropped during the STARTING countdown (a game needs at least one).
     * Runs the normal conversion flow so gear/perk replacement stay consistent.
     */
    private void promoteRandomHumanToVampire() {
        for (UUID candidate : new ArrayList<>(teamManager.getHumanTeam())) {
            Player player = Bukkit.getPlayer(candidate);
            if (player != null && player.isOnline()
                    && convertHumanToVampire(player, PlayerConvertedEvent.Cause.FORCED)) {
                announcer.broadcast(MM.parse("<red>All vampires dropped during the countdown — <dark_red>"
                        + player.getName() + "</dark_red> takes their place!"));
                return;
            }
        }
        // No online humans either — nobody left to play.
        announcer.broadcast(MM.parse("<red>Everyone dropped during the countdown — game aborted."));
        stopGame();
    }

    /** What the game must do after a player drops out during the STARTING countdown. */
    enum StartingDropoutAction {
        /** Teams still viable — carry on. */
        CONTINUE,
        /** No vampires left — promote a random human so the game can begin. */
        PROMOTE_HUMAN_TO_VAMPIRE,
        /** No humans left — the game cannot begin; abort, do NOT award a vampire win. */
        ABORT_GAME
    }

    static StartingDropoutAction afterStartingDropout(int humansLeft, int vampiresLeft) {
        if (humansLeft == 0) return StartingDropoutAction.ABORT_GAME;
        if (vampiresLeft == 0) return StartingDropoutAction.PROMOTE_HUMAN_TO_VAMPIRE;
        return StartingDropoutAction.CONTINUE;
    }

    /**
     * Tiers to auto-assign when a mid-game quitter reconnects: one per perk they
     * actually held, at the same tier. If they held nothing but the starting
     * freebie had already been granted (vampires released, or they were human),
     * they're still owed that one Silver pick.
     */
    static List<PerkTier> quitCompensationTiers(List<Perk> heldPerks, boolean startingFreebieGranted) {
        List<PerkTier> tiers = new ArrayList<>();
        for (Perk perk : heldPerks) {
            tiers.add(perk.getTier());
        }
        if (tiers.isEmpty() && startingFreebieGranted) {
            tiers.add(PerkTier.SILVER);
        }
        return tiers;
    }

    // ===== TIMED PERKS =====

    private static final Random RANDOM = new Random();

    private void triggerTimedPerk() {
        PerkTier[] tiers = PerkTier.values();
        for (Player player : getJoinedOnlinePlayers()) {
            if (!isInGame(player.getUniqueId())) continue;
            PerkTeam team = isVampire(player.getUniqueId()) ? PerkTeam.VAMPIRE : PerkTeam.HUMAN;
            PerkTier randomTier = tiers[RANDOM.nextInt(tiers.length)];
            scheduleTimedPerkRoll(player, randomTier, team);
        }
    }

    /** Defers the roll animation until the player has been out of combat. */
    private void scheduleTimedPerkRoll(Player player, PerkTier finalTier, PerkTeam team) {
        UUID uuid = player.getUniqueId();
        if (!isInCombat(uuid)) {
            startPerkRollAnimation(player, finalTier, team);
            return;
        }
        player.sendActionBar(MM.parse(colorTag(finalTier) + "★ Perk roll queued — resolves when combat ends ★"));
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !isInGame(uuid) || stateManager.getState() != GameState.ACTIVE) {
                    cancel();
                    return;
                }
                if (!isInCombat(uuid)) {
                    cancel();
                    startPerkRollAnimation(player, finalTier, team);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void startPerkRollAnimation(Player player, PerkTier finalTier, PerkTeam team) {
        PerkTier[] tiers = PerkTier.values();
        int[] delays = {2, 2, 2, 3, 3, 3, 4, 4, 4, 5, 5, 6, 6, 8, 10};

        new BukkitRunnable() {
            int frame = 0;
            int ticksUntilNext = 0;

            @Override
            public void run() {
                if (!player.isOnline()) { cancel(); return; }
                if (ticksUntilNext > 0) { ticksUntilNext--; return; }
                if (frame >= delays.length) {
                    // Final reveal
                    player.sendActionBar(MM.parse(colorTag(finalTier) + "\u2605 " + finalTier.getDisplayName() + " Perk! \u2605"));
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
                    // Open GUI after 1 second
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (player.isOnline() && perkSelectionGUISupplier.get() != null) {
                            perkSelectionGUISupplier.get().openTimedSelection(player, finalTier, team);
                        }
                    }, 20L);
                    cancel();
                    return;
                }
                // Show cycling tier
                PerkTier shown = (frame == delays.length - 1) ? finalTier : tiers[frame % tiers.length];
                player.sendActionBar(MM.parse(colorTag(shown) + "\u00BB " + shown.getDisplayName() + " \u00AB"));
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f + (frame * 0.05f));
                ticksUntilNext = delays[frame];
                frame++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ===== BLOOD COMPASS =====

    private static String colorTag(PerkTier tier) {
        return switch (tier) {
            case SILVER -> "<white>";
            case GOLD -> "<gold>";
            case PRISMATIC -> "<light_purple>";
        };
    }

    private void giveBloodCompass(Player vampire) {
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        if (meta != null) {
            meta.displayName(MM.parse("<dark_red>Blood Compass")
                    .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
            meta.lore(java.util.Arrays.asList(
                    MM.parse("<gray>Points toward the nearest human")
                            .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false),
                    MM.parse("<red>The hunt intensifies...")
                            .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false)
            ));
            compass.setItemMeta(meta);
        }
        vampire.getInventory().addItem(compass);
        vampire.sendMessage(MM.parse("<dark_red>You received a <bold>Blood Compass</bold><dark_red>! It points toward the nearest human."));
    }

    private void updateBloodCompasses() {
        List<Player> vampires = getOnlineTeamPlayers(teamManager.getVampireTeam());
        if (vampires.isEmpty()) return;

        // Pre-fetch online humans once per tick instead of re-resolving for every vampire.
        List<Player> humans = getOnlineTeamPlayers(teamManager.getHumanTeam());
        if (humans.isEmpty()) return;

        for (Player vamp : vampires) {
            // Early-exit: skip the whole inventory + distance scan for vampires without a compass.
            if (!vamp.getInventory().contains(Material.COMPASS)) continue;

            Player nearest = null;
            double nearestDist = Double.MAX_VALUE;
            for (Player human : humans) {
                if (!human.getWorld().equals(vamp.getWorld())) continue;
                double dist = human.getLocation().distanceSquared(vamp.getLocation());
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearest = human;
                }
            }
            if (nearest == null) continue;

            Location target = nearest.getLocation();
            for (ItemStack item : vamp.getInventory().getContents()) {
                if (item == null || item.getType() != Material.COMPASS) continue;
                CompassMeta compassMeta = (CompassMeta) item.getItemMeta();
                if (compassMeta != null) {
                    compassMeta.setLodestone(target);
                    compassMeta.setLodestoneTracked(false);
                    item.setItemMeta(compassMeta);
                }
            }
        }
    }

    // ===== GETTERS =====

    public GameState getState() { return stateManager.getState(); }
    public Set<UUID> getHumanTeam() { return teamManager.getHumanTeam(); }
    public Set<UUID> getVampireTeam() { return teamManager.getVampireTeam(); }
    public int getRemainingSeconds() { return timerManager.getRemainingSeconds(); }

    /**
     * Override the active game timer. Clamped to {@code >= 0}; no upper bound, so admins
     * can extend a game past its configured duration. Setting to 0 ends the game on the
     * next tick. Milestone announcements (5/1/0:30 minute marks) won't re-fire if skipped past.
     */
    public void setRemainingSeconds(int seconds) {
        timerManager.setRemainingSeconds(Math.max(0, seconds));
    }
    public int getMinPlayers() { return minPlayers; }
    public int getGameDurationSeconds() { return gameDurationSeconds; }

    public boolean isInGame(UUID uuid) {
        return teamManager.getHumanTeam().contains(uuid) || teamManager.getVampireTeam().contains(uuid);
    }

    public boolean isHuman(UUID uuid) { return teamManager.getHumanTeam().contains(uuid); }
    public boolean isVampire(UUID uuid) { return teamManager.getVampireTeam().contains(uuid); }
    public boolean isVampiresReleased() { return teamManager.isVampiresReleased(); }
    public boolean hasFiredTimedMilestone(int elapsedSeconds) { return timerManager.getFiredTimedMilestones().contains(elapsedSeconds); }
    /** Wall-clock seconds since the active phase began. Immune to {@link #setRemainingSeconds(int)}. */
    public int getActiveElapsedSeconds() { return timerManager.getActiveElapsedSeconds(); }

    public void tagCombat(UUID uuid)        { teamManager.tagCombat(uuid); }
    public boolean isInCombat(UUID uuid)    { return teamManager.isInCombat(uuid); }

    /**
     * Restores saved states for ONLINE players still in the lobby on server shutdown.
     * Offline players in joinedPlayers (e.g. quit mid-game and never returned) intentionally
     * keep their .yml on disk — handlePlayerJoin's Case 3 restores them on next reconnect.
     * Wiping those files here would permanently destroy their pre-game inventory.
     */
    public void restoreLobbyPlayers() {
        if (playerStateManager == null) return;
        for (UUID uuid : new HashSet<>(joinedPlayers)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                playerStateManager.restore(player);
            }
        }
        joinedPlayers.clear();
    }

    public Location getLobbySpawn()   { return spawnManager.getLobbySpawn(); }
    public Location getHumanSpawn()   { return spawnManager.getHumanSpawn(); }
    public Location getVampireSpawn() { return spawnManager.getVampireSpawn(); }

    public void setLobbySpawn(Location loc)   { spawnManager.setLobbySpawn(loc); }
    public void setHumanSpawn(Location loc)   { spawnManager.setHumanSpawn(loc); }
    public void setVampireSpawn(Location loc) { spawnManager.setVampireSpawn(loc); }

    public EconomyManager getEconomyManager() { return economyManager; }
    public PerkManager getPerkManager() { return perkManager; }
    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
    public DayNightManager getDayNightManager() { return dayNightManager; }
    public VampireZPlugin getPlugin() { return plugin; }
}

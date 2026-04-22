package com.vampirez;

import org.bukkit.*;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.*;

public class GameManager {

    private final JavaPlugin plugin;
    private final EconomyManager economyManager;
    private final PerkManager perkManager;
    private final GearManager gearManager;
    private final DayNightManager dayNightManager;
    private final ScoreboardManager scoreboardManager;

    private PerkSelectionGUI perkSelectionGUI;
    private StatAnvilManager statAnvilManager;
    private ArenaManager arenaManager;
    private PlayerStateManager playerStateManager;

    private GameState state = GameState.LOBBY;
    private final Set<UUID> joinedPlayers = new HashSet<>();
    /** Stores how many perks to auto-assign (with tiers) for players who disconnected mid-game. */
    private final Map<UUID, List<PerkTier>> pendingAutoPerks = new HashMap<>();
    private final Set<UUID> humanTeam = new HashSet<>();
    private final Set<UUID> vampireTeam = new HashSet<>();
    /** Tracks the last time each player took or dealt PvP damage (combat tag). */
    private final Map<UUID, Long> lastCombatMs = new HashMap<>();
    private static final long COMBAT_TAG_MS = 7_000L;
    private int remainingSeconds;
    private BukkitTask timerTask;
    private BukkitTask scoreboardTask;
    private BukkitTask countdownTask;
    private BukkitTask vampireReleaseTask;
    private boolean vampiresReleased = true;
    private boolean bloodCompassGiven = false;

    private int minPlayers;
    private int gameDurationSeconds;
    private double vampireRatio;
    private int vampireRespawnDelayTicks;
    private int startCountdownSeconds;

    private Location lobbySpawn;
    private Location humanSpawn;
    private Location vampireSpawn;

    public GameManager(JavaPlugin plugin, EconomyManager economyManager, PerkManager perkManager,
                       GearManager gearManager, DayNightManager dayNightManager, ScoreboardManager scoreboardManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.perkManager = perkManager;
        this.gearManager = gearManager;
        this.dayNightManager = dayNightManager;
        this.scoreboardManager = scoreboardManager;

        loadConfig();
        scoreboardManager.setupLobbyScoreboard();
    }

    private void loadConfig() {
        minPlayers = plugin.getConfig().getInt("game.min-players", 10);
        gameDurationSeconds = plugin.getConfig().getInt("game.game-duration-seconds", 1500);
        vampireRatio = plugin.getConfig().getDouble("game.vampire-ratio", 0.3);
        vampireRespawnDelayTicks = plugin.getConfig().getInt("game.vampire-respawn-delay-ticks", 80);
        startCountdownSeconds = plugin.getConfig().getInt("game.start-countdown-seconds", 15);

        perkManager.setMaxPerks(plugin.getConfig().getInt("perks.max-perks-per-player", 10));

        loadSpawns();
    }

    private void loadSpawns() {
        lobbySpawn = loadLocation("spawns.lobby");
        humanSpawn = loadLocation("spawns.human");
        vampireSpawn = loadLocation("spawns.vampire");
    }

    private Location loadLocation(String path) {
        String worldName = plugin.getConfig().getString(path + ".world", "");
        if (worldName.isEmpty()) return null;

        // If the saved world name starts with the arena base name, resolve to the current arena world
        World world = Bukkit.getWorld(worldName);
        if (world == null && arenaManager != null) {
            String arenaBase = plugin.getConfig().getString("arena.world-name", "vampirez_arena");
            if (worldName.startsWith(arenaBase)) {
                world = arenaManager.getArenaWorld();
            }
        }
        if (world == null) return null;
        return new Location(
            world,
            plugin.getConfig().getDouble(path + ".x"),
            plugin.getConfig().getDouble(path + ".y"),
            plugin.getConfig().getDouble(path + ".z"),
            (float) plugin.getConfig().getDouble(path + ".yaw"),
            (float) plugin.getConfig().getDouble(path + ".pitch")
        );
    }

    public void saveLocation(String path, Location loc) {
        plugin.getConfig().set(path + ".world", loc.getWorld().getName());
        plugin.getConfig().set(path + ".x", loc.getX());
        plugin.getConfig().set(path + ".y", loc.getY());
        plugin.getConfig().set(path + ".z", loc.getZ());
        plugin.getConfig().set(path + ".yaw", loc.getYaw());
        plugin.getConfig().set(path + ".pitch", loc.getPitch());
        plugin.saveConfig();
    }

    public void setPerkSelectionGUI(PerkSelectionGUI gui) {
        this.perkSelectionGUI = gui;
    }

    public void setStatAnvilManager(StatAnvilManager manager) {
        this.statAnvilManager = manager;
    }

    public StatAnvilManager getStatAnvilManager() {
        return statAnvilManager;
    }

    public void setArenaManager(ArenaManager arenaManager) {
        this.arenaManager = arenaManager;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public void setPlayerStateManager(PlayerStateManager psm) {
        this.playerStateManager = psm;
    }

    public PlayerStateManager getPlayerStateManager() {
        return playerStateManager;
    }

    // ===== JOIN / LEAVE =====

    /**
     * Player opts in to the VampireZ game. Saves their survival state and teleports to lobby.
     */
    public boolean joinGame(Player player) {
        UUID uuid = player.getUniqueId();
        if (joinedPlayers.contains(uuid)) {
            player.sendMessage(ChatColor.RED + "You are already in the VampireZ lobby!");
            return false;
        }
        if (state != GameState.LOBBY) {
            player.sendMessage(ChatColor.RED + "A game is already in progress! Wait for it to end.");
            return false;
        }

        // Save survival state to disk, then clear player
        playerStateManager.saveAndClear(player);
        joinedPlayers.add(uuid);

        // Teleport to lobby in adventure mode (arena is protected)
        if (lobbySpawn != null) {
            player.teleport(lobbySpawn);
        }
        player.setGameMode(org.bukkit.GameMode.ADVENTURE);

        player.sendMessage(ChatColor.GREEN + "You joined VampireZ! " + ChatColor.GRAY + "Waiting for the game to start...");
        scoreboardManager.showLobbyScoreboard(player);
        scoreboardManager.updateLobbyScoreboard(joinedPlayers.size(), minPlayers);

        // Announce
        for (UUID jUuid : joinedPlayers) {
            Player p = Bukkit.getPlayer(jUuid);
            if (p != null && !p.equals(player)) {
                p.sendMessage(ChatColor.YELLOW + player.getName() + " joined! " +
                        ChatColor.GRAY + "(" + joinedPlayers.size() + "/" + minPlayers + ")");
            }
        }
        return true;
    }

    /**
     * Player opts out of VampireZ. Restores their survival state.
     */
    public void leaveGame(Player player) {
        UUID uuid = player.getUniqueId();
        if (!joinedPlayers.contains(uuid)) {
            player.sendMessage(ChatColor.RED + "You are not in a VampireZ game!");
            return;
        }

        boolean wasInActiveGame = (state == GameState.ACTIVE || state == GameState.STARTING) && isInGame(uuid);

        // Remove from teams
        boolean wasHuman = humanTeam.remove(uuid);
        vampireTeam.remove(uuid);
        perkManager.removeAllPerks(uuid);
        economyManager.resetPlayer(uuid);
        scoreboardManager.removePlayer(uuid);
        joinedPlayers.remove(uuid);

        // Restore survival state (wipes game items first, then restores saved inventory)
        playerStateManager.restore(player);

        player.sendMessage(ChatColor.YELLOW + "You left VampireZ. Your inventory has been restored.");

        if (state == GameState.LOBBY) {
            scoreboardManager.updateLobbyScoreboard(joinedPlayers.size(), minPlayers);
        }

        // Check win condition
        if (wasInActiveGame && wasHuman && humanTeam.isEmpty() && !vampireTeam.isEmpty()) {
            endGame(false);
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
        List<Player> players = new ArrayList<>();
        for (UUID uuid : joinedPlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                players.add(p);
            }
        }
        return players;
    }

    // ===== GAME START =====

    public boolean canStart() {
        return joinedPlayers.size() >= minPlayers && hasSpawnsSet();
    }

    public boolean hasSpawnsSet() {
        return humanSpawn != null && vampireSpawn != null;
    }

    public void startGame(boolean force) {
        if (state != GameState.LOBBY) return;
        if (!force && !canStart()) return;
        if (!hasSpawnsSet()) return;

        state = GameState.STARTING;

        // Auto-assign teams (only from joined players)
        List<Player> players = getJoinedOnlinePlayers();
        Collections.shuffle(players);
        int vampCount = Math.max(1, (int) Math.round(players.size() * vampireRatio));

        humanTeam.clear();
        vampireTeam.clear();

        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            if (i < vampCount) {
                vampireTeam.add(player.getUniqueId());
            } else {
                humanTeam.add(player.getUniqueId());
            }
        }

        // Reset all players fully, then teleport and give gear
        for (UUID uuid : humanTeam) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                resetPlayerFully(player);
                player.setWalkSpeed(0.2f); // default walk speed
                player.setFlySpeed(0.1f);
                player.teleport(humanSpawn);
                gearManager.giveHumanGear(player);
                player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "You are a HUMAN! " + ChatColor.RESET + ChatColor.YELLOW + "You have 45 seconds to loot before the Vampires are released!");
                player.sendTitle(ChatColor.AQUA + "" + ChatColor.BOLD + "HUMAN", ChatColor.YELLOW + "45 seconds to loot!", 10, 60, 10);
            }
        }
        // Vampires: invisible scouts during 45s head-start, no gear
        vampiresReleased = false;
        for (UUID uuid : vampireTeam) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                resetPlayerFully(player);
                player.setWalkSpeed(0.2f);
                player.setFlySpeed(0.1f);
                player.teleport(vampireSpawn);
                // Invisibility during scouting phase (extra buffer ticks)
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.INVISIBILITY, 50 * 20, 0, false, false, false));
                // Night vision so they can see
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.NIGHT_VISION, 999999, 0, false, false, true));
                player.sendMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + "You are a VAMPIRE! " + ChatColor.RESET + ChatColor.GRAY + "Scout the arena invisibly. Released in 45 seconds.");
                player.sendTitle(ChatColor.DARK_RED + "" + ChatColor.BOLD + "VAMPIRE", ChatColor.GRAY + "Scouting phase - 45 seconds...", 10, 60, 10);
            }
        }

        // Setup scoreboards for joined players
        for (Player player : getJoinedOnlinePlayers()) {
            scoreboardManager.createGameScoreboard(player, this, economyManager, perkManager, dayNightManager);
        }

        // Open free Silver perk selection for HUMANS only (vampires get theirs on release)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (UUID uuid : humanTeam) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline() && perkSelectionGUI != null) {
                    perkSelectionGUI.openFreeSelection(player, PerkTeam.HUMAN);
                }
            }
        }, 20L);

        // Start the game immediately (humans loot while vampires scout)
        beginActiveGame();

        // Start 45-second vampire release countdown
        final int[] vampCountdown = {45};
        vampireReleaseTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (state != GameState.ACTIVE) {
                if (vampireReleaseTask != null) { vampireReleaseTask.cancel(); vampireReleaseTask = null; }
                return;
            }

            // Action bar countdown for vampires
            for (UUID uuid : vampireTeam) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    if (vampCountdown[0] > 0) {
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                                new TextComponent(ChatColor.RED + "\u2694 " + ChatColor.GOLD + "Releasing in " +
                                        ChatColor.RED + "" + ChatColor.BOLD + vampCountdown[0] +
                                        ChatColor.RESET + ChatColor.GOLD + "s " + ChatColor.RED + "\u2694"));
                    }
                }
            }

            // Milestone announcements to joined players
            if (vampCountdown[0] == 30 || vampCountdown[0] == 15 || vampCountdown[0] == 10) {
                for (Player player : getJoinedOnlinePlayers()) {
                    player.sendMessage(ChatColor.RED + "Vampires release in " + ChatColor.BOLD + vampCountdown[0] + ChatColor.RESET + ChatColor.RED + " seconds!");
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                }
            }
            if (vampCountdown[0] <= 5 && vampCountdown[0] > 0) {
                for (Player player : getJoinedOnlinePlayers()) {
                    player.sendMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + vampCountdown[0] + "...");
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
                }
            }

            vampCountdown[0]--;
            if (vampCountdown[0] < 0) {
                releaseVampires();
                if (vampireReleaseTask != null) { vampireReleaseTask.cancel(); vampireReleaseTask = null; }
            }
        }, 20L, 20L);
    }

    private void beginActiveGame() {
        state = GameState.ACTIVE;
        remainingSeconds = gameDurationSeconds;

        // Disable random ticks (prevents leaf decay, crop growth, etc.)
        if (humanSpawn != null && humanSpawn.getWorld() != null) {
            humanSpawn.getWorld().setGameRule(org.bukkit.GameRule.RANDOM_TICK_SPEED, 0);
            humanSpawn.getWorld().setDifficulty(org.bukkit.Difficulty.HARD);
        }
        if (vampireSpawn != null && vampireSpawn.getWorld() != null) {
            vampireSpawn.getWorld().setGameRule(org.bukkit.GameRule.RANDOM_TICK_SPEED, 0);
            vampireSpawn.getWorld().setDifficulty(org.bukkit.Difficulty.HARD);
        }

        // Announce game start
        String startMsg = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("messages.game-start", "&cThe hunt begins!"));
        String prefix = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.prefix", ""));
        for (Player player : getJoinedOnlinePlayers()) {
            player.sendMessage(prefix + startMsg);
            if (vampiresReleased) {
                // Normal start (no delayed spawn active)
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "PvP is now ENABLED! " + ChatColor.RESET + ChatColor.YELLOW + "The hunt begins!");
                player.sendTitle(ChatColor.RED + "" + ChatColor.BOLD + "PvP ENABLED!", ChatColor.YELLOW + "The hunt begins!", 5, 30, 10);
            }
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.0f);
        }

        // Start timer
        timerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (state != GameState.ACTIVE) return;

            remainingSeconds--;

            // Timed free perk triggers
            int elapsed = gameDurationSeconds - remainingSeconds;
            if (elapsed == 300 || elapsed == 600 || elapsed == 900) {
                triggerTimedPerk();
            }

            // Blood Compass — give to all vampires at 10 minutes remaining
            if (remainingSeconds == 600 && !bloodCompassGiven) {
                bloodCompassGiven = true;
                for (UUID uuid : vampireTeam) {
                    Player vamp = Bukkit.getPlayer(uuid);
                    if (vamp != null && vamp.isOnline()) {
                        giveBloodCompass(vamp);
                    }
                }
                for (Player p : getJoinedOnlinePlayers()) {
                    p.sendMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + "Blood Compass " +
                            ChatColor.RESET + ChatColor.RED + "activated! Vampires can now sense human locations.");
                    p.playSound(p.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.5f, 1.2f);
                }
            }

            // Update blood compass targets every second
            if (bloodCompassGiven) {
                updateBloodCompasses();
            }

            // Milestone announcements
            if (remainingSeconds == 300 || remainingSeconds == 60 || remainingSeconds == 30) {
                int min = remainingSeconds / 60;
                int sec = remainingSeconds % 60;
                String timeMsg = min > 0 ? min + " minute" + (min > 1 ? "s" : "") : sec + " seconds";
                for (Player player : getJoinedOnlinePlayers()) {
                    player.sendMessage(ChatColor.YELLOW + timeMsg + " remaining!");
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
                }
            }

            if (remainingSeconds <= 5 && remainingSeconds > 0) {
                for (Player player : getJoinedOnlinePlayers()) {
                    player.sendMessage(ChatColor.RED + "" + remainingSeconds + "...");
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
                }
            }

            if (remainingSeconds <= 0) {
                endGame(true); // Humans win
            }
        }, 20L, 20L);

        // Start scoreboard updates
        scoreboardTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            scoreboardManager.updateAllGameScoreboards(this, economyManager, perkManager, dayNightManager);
        }, 20L, 20L);

        // Start economy
        economyManager.startPassiveIncome(this);

        // Start day/night cycle
        dayNightManager.startCycle();
    }

    // ===== VAMPIRE RELEASE (after 45s scouting phase) =====

    private void releaseVampires() {
        vampiresReleased = true;

        for (UUID uuid : vampireTeam) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                // Remove invisibility
                player.removePotionEffect(PotionEffectType.INVISIBILITY);
                // Teleport to vampire spawn
                if (vampireSpawn != null) {
                    player.teleport(vampireSpawn);
                }
                // Give vampire gear
                gearManager.giveVampireGear(player);
                // Apply perks and effects
                perkManager.reapplyPerks(uuid);
                dayNightManager.applyEffectsToPlayer(player);
                if (statAnvilManager != null) statAnvilManager.reapplyBuffs(player);

                player.sendTitle(ChatColor.DARK_RED + "" + ChatColor.BOLD + "HUNT!",
                        ChatColor.RED + "The Vampires have been released!", 5, 30, 10);
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 0.8f);
            }
        }

        // Announce to humans
        for (UUID uuid : humanTeam) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.sendTitle(ChatColor.DARK_RED + "" + ChatColor.BOLD + "BEWARE!",
                        ChatColor.RED + "The Vampires have been released!", 5, 30, 10);
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 0.8f);
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "The Vampires have been released! " +
                        ChatColor.RESET + ChatColor.YELLOW + "The hunt begins!");
            }
        }

        // Open free Silver perk selection for vampires
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (UUID uuid : vampireTeam) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline() && perkSelectionGUI != null) {
                    perkSelectionGUI.openFreeSelection(player, PerkTeam.VAMPIRE);
                }
            }
        }, 20L);
    }

    // ===== HUMAN DEATH → CONVERT TO VAMPIRE =====

    public void convertHumanToVampire(Player player) {
        UUID uuid = player.getUniqueId();
        if (!humanTeam.contains(uuid)) return;

        // Broadcast conversion
        String deathMsg = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("messages.human-death", "&c{player} has fallen! They rise again as a Vampire!"))
                .replace("{player}", player.getName());
        String prefix = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.prefix", ""));
        for (Player p : getJoinedOnlinePlayers()) {
            p.sendMessage(prefix + deathMsg);
        }

        // Switch teams
        humanTeam.remove(uuid);
        vampireTeam.add(uuid);

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

            player.sendMessage(ChatColor.DARK_RED + "You have become a Vampire! Hunt the remaining humans!");

            // Give blood compass if already active
            if (bloodCompassGiven) {
                giveBloodCompass(player);
            }

            // Open free replacement perk selection for each removed human-only perk
            if (!removedPerks.isEmpty() && perkSelectionGUI != null) {
                perkSelectionGUI.openConversionSelection(player, PerkTeam.VAMPIRE, removedPerks.size());
            }
        }, 40L); // 2 second delay

        // Check win condition
        if (humanTeam.isEmpty()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> endGame(false), 20L);
        }
    }

    // ===== VAMPIRE RESPAWN =====

    public void respawnVampire(Player player) {
        UUID uuid = player.getUniqueId();
        if (!vampireTeam.contains(uuid)) return;

        player.sendMessage(ChatColor.RED + "Respawning in " + (vampireRespawnDelayTicks / 20) + " seconds...");

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            if (state != GameState.ACTIVE) return;
            if (player.isDead()) player.spigot().respawn();

            if (vampireSpawn != null) {
                player.teleport(vampireSpawn);
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
        if (state == GameState.ENDING || state == GameState.LOBBY) return;
        state = GameState.ENDING;

        // Stop tasks
        if (countdownTask != null) { countdownTask.cancel(); countdownTask = null; }
        if (vampireReleaseTask != null) { vampireReleaseTask.cancel(); vampireReleaseTask = null; }
        vampiresReleased = true;
        if (timerTask != null) { timerTask.cancel(); timerTask = null; }
        if (scoreboardTask != null) { scoreboardTask.cancel(); scoreboardTask = null; }
        economyManager.stopPassiveIncome();
        dayNightManager.stopCycle();

        // Clear all perks immediately so perk ticks (Undead Horde, etc.) stop
        perkManager.resetAll();
        com.vampirez.perks.TrapperPerk.clearAllWebs();

        // Announce winner
        String prefix = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.prefix", ""));
        String winMsg;
        if (humansWin) {
            winMsg = ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("messages.humans-win", "&bThe Humans have survived!"));
        } else {
            winMsg = ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("messages.vampires-win", "&4The Vampires have won!"));
        }

        for (Player player : getJoinedOnlinePlayers()) {
            player.sendMessage(prefix + winMsg);
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

            // Fireworks
            if (player.getWorld() != null) {
                Firework fw = player.getWorld().spawn(player.getLocation().add(0, 1, 0), Firework.class);
                FireworkMeta meta = fw.getFireworkMeta();
                meta.addEffect(FireworkEffect.builder()
                        .with(FireworkEffect.Type.BALL_LARGE)
                        .withColor(humansWin ? Color.BLUE : Color.RED)
                        .withFade(Color.WHITE)
                        .flicker(true)
                        .build());
                meta.setPower(1);
                fw.setFireworkMeta(meta);
            }
        }

        // Reset after 10 seconds
        Bukkit.getScheduler().runTaskLater(plugin, this::resetToLobby, 200L);
    }

    public void stopGame() {
        if (state == GameState.LOBBY) return;

        // Stop tasks and perks immediately (same as endGame)
        state = GameState.ENDING;
        if (countdownTask != null) { countdownTask.cancel(); countdownTask = null; }
        if (vampireReleaseTask != null) { vampireReleaseTask.cancel(); vampireReleaseTask = null; }
        vampiresReleased = true;
        if (timerTask != null) { timerTask.cancel(); timerTask = null; }
        if (scoreboardTask != null) { scoreboardTask.cancel(); scoreboardTask = null; }
        economyManager.stopPassiveIncome();
        dayNightManager.stopCycle();
        perkManager.resetAll();
        com.vampirez.perks.TrapperPerk.clearAllWebs();

        // Reset immediately (no 10s delay — server may be shutting down)
        resetToLobby();
    }

    private void resetToLobby() {
        state = GameState.LOBBY;

        // Cleanup
        for (Perk perk : perkManager.getAllPerks()) {
            perk.clearAllStats();
        }
        perkManager.resetAll();
        economyManager.resetAll();
        scoreboardManager.resetAll();
        if (statAnvilManager != null) statAnvilManager.resetAll();
        vampiresReleased = true;
        bloodCompassGiven = false;
        pendingAutoPerks.clear();

        humanTeam.clear();
        vampireTeam.clear();

        // Wipe all players' game state and park them in the main world during arena reset
        for (UUID uuid : new HashSet<>(joinedPlayers)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                resetPlayerFully(player);
                player.getInventory().clear();
                player.getInventory().setArmorContents(null);
                player.getInventory().setItemInOffHand(null);
                player.setGameMode(org.bukkit.GameMode.ADVENTURE);
                player.setHealth(player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getBaseValue());
                player.setFoodLevel(20);
                player.setSaturation(20f);
                // Temporarily send to main world while arena resets
                player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
                // Clear saved state — game ended normally, no need to restore on next login
                if (playerStateManager != null) {
                    playerStateManager.clearSavedState(uuid);
                }
            } else {
                // Offline player — remove from joinedPlayers, keep saved state for restore on next login
                joinedPlayers.remove(uuid);
            }
        }

        // Reset the arena world, then teleport players to lobby in the fresh arena
        if (arenaManager != null && arenaManager.hasTemplate()) {
            arenaManager.resetArena(() -> {
                loadSpawns();
                plugin.getLogger().info("Arena world has been reset and reloaded.");

                scoreboardManager.setupLobbyScoreboard();
                for (UUID uuid : new HashSet<>(joinedPlayers)) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null && player.isOnline()) {
                        if (lobbySpawn != null) {
                            player.teleport(lobbySpawn);
                        }
                        scoreboardManager.showLobbyScoreboard(player);
                    }
                }
                scoreboardManager.updateLobbyScoreboard(joinedPlayers.size(), minPlayers);
            });
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
        player.getInventory().setArmorContents(null);
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
        player.setHealth(player.getMaxHealth());
    }

    // ===== PLAYER JOIN/QUIT =====

    public void handlePlayerJoin(Player player) {
        UUID uuid = player.getUniqueId();

        // Case 1: Reconnecting to an active game (they quit mid-game)
        if (joinedPlayers.contains(uuid) && (state == GameState.ACTIVE || state == GameState.STARTING)) {
            if (vampireTeam.contains(uuid)) {
                resetPlayerFully(player);
                player.setGameMode(GameMode.SURVIVAL);

                if (!vampiresReleased) {
                    // Still in scouting phase — no gear, invisible, teleport to vampire spawn
                    if (vampireSpawn != null) player.teleport(vampireSpawn);
                    player.addPotionEffect(new PotionEffect(
                            PotionEffectType.INVISIBILITY, 50 * 20, 0, false, false, false));
                    player.addPotionEffect(new PotionEffect(
                            PotionEffectType.NIGHT_VISION, 999999, 0, false, false, true));
                    // Auto-assign pending perks (they'll be applied on release)
                    List<PerkTier> pending = pendingAutoPerks.remove(uuid);
                    if (pending != null) {
                        for (PerkTier tier : pending) {
                            List<Perk> options = perkManager.getRandomPerks(tier, PerkTeam.VAMPIRE, 1, uuid);
                            if (!options.isEmpty()) {
                                perkManager.addPerkToPlayer(uuid, options.get(0));
                            }
                        }
                    }
                    scoreboardManager.createGameScoreboard(player, this, economyManager, perkManager, dayNightManager);
                    player.sendMessage(ChatColor.DARK_RED + "You reconnected as a Vampire! " + ChatColor.GRAY + "Scouting phase — wait for release.");
                } else {
                    // Vampires already released — give gear normally
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (!player.isOnline() || state != GameState.ACTIVE) return;
                        if (vampireSpawn != null) player.teleport(vampireSpawn);
                        gearManager.giveVampireGear(player);

                        // Auto-assign pending perks from disconnect
                        List<PerkTier> pending = pendingAutoPerks.remove(uuid);
                        if (pending != null) {
                            for (PerkTier tier : pending) {
                                List<Perk> options = perkManager.getRandomPerks(tier, PerkTeam.VAMPIRE, 1, uuid);
                                if (!options.isEmpty()) {
                                    perkManager.addPerkToPlayer(uuid, options.get(0));
                                }
                            }
                            int count = perkManager.getPlayerPerkCount(uuid);
                            player.sendMessage(ChatColor.GREEN + "" + count + " perk(s) auto-assigned.");
                        }

                        perkManager.reapplyPerks(uuid);
                        dayNightManager.applyEffectsToPlayer(player);
                        if (statAnvilManager != null) statAnvilManager.reapplyBuffs(player);
                        scoreboardManager.createGameScoreboard(player, this, economyManager, perkManager, dayNightManager);
                        if (bloodCompassGiven) giveBloodCompass(player);
                        player.sendMessage(ChatColor.DARK_RED + "You reconnected as a Vampire! Hunt the humans!");
                    }, 10L);
                }
            }
            return;
        }

        // Case 2: Reconnecting to lobby (quit during LOBBY state)
        if (joinedPlayers.contains(uuid) && state == GameState.LOBBY) {
            if (lobbySpawn != null) player.teleport(lobbySpawn);
            player.setGameMode(org.bukkit.GameMode.ADVENTURE);
            scoreboardManager.showLobbyScoreboard(player);
            scoreboardManager.updateLobbyScoreboard(joinedPlayers.size(), minPlayers);
            player.sendMessage(ChatColor.GREEN + "Welcome back to the VampireZ lobby!");
            return;
        }

        // Case 3: Player has a saved state from a previous session (game ended while offline, or crash)
        if (playerStateManager != null && playerStateManager.hasSavedState(uuid) && !joinedPlayers.contains(uuid)) {
            playerStateManager.restore(player);
            player.sendMessage(ChatColor.YELLOW + "Your inventory was restored from a previous VampireZ session.");
            return;
        }

        // Case 4: Normal join — not in VampireZ, do nothing
    }

    public void handlePlayerQuit(Player player) {
        UUID uuid = player.getUniqueId();

        if (!joinedPlayers.contains(uuid)) return; // Not in VampireZ, ignore

        if (state == GameState.LOBBY) {
            // Keep in joinedPlayers so they rejoin the lobby automatically
            scoreboardManager.removePlayer(uuid);
            scoreboardManager.updateLobbyScoreboard(joinedPlayers.size(), minPlayers);
            return;
        }

        if (state == GameState.ACTIVE || state == GameState.STARTING) {
            // --- Build the list of perks to auto-assign on reconnect ---
            List<PerkTier> perksToGive = new ArrayList<>();

            // 1. Starting perk (Silver) — only if already received
            //    Vampires get theirs on release, so skip if still in scouting phase
            if (vampiresReleased || humanTeam.contains(uuid)) {
                perksToGive.add(PerkTier.SILVER);
            }

            // 2. Timed free perks that have already been given
            int elapsed = gameDurationSeconds - remainingSeconds;
            if (elapsed >= 300) perksToGive.add(randomTier());  // 5 min mark
            if (elapsed >= 600) perksToGive.add(randomTier());  // 10 min mark
            if (elapsed >= 900) perksToGive.add(randomTier());  // 15 min mark

            // 3. Replacements for human-only perks (if they were human)
            if (humanTeam.contains(uuid)) {
                // Count human-only perks before removing
                long humanOnlyCount = perkManager.getPlayerPerks(uuid).stream()
                        .filter(p -> p.getTeam() == PerkTeam.HUMAN)
                        .count();
                for (int i = 0; i < humanOnlyCount; i++) {
                    perksToGive.add(randomTier());
                }

                // Convert to vampire
                humanTeam.remove(uuid);
                vampireTeam.add(uuid);

                String prefix = ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("messages.prefix", ""));
                for (Player p : getJoinedOnlinePlayers()) {
                    p.sendMessage(prefix + ChatColor.RED + player.getName() + " disconnected and has been converted to a Vampire!");
                }

                // Check win condition
                if (humanTeam.isEmpty() && !vampireTeam.isEmpty()) {
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

            // KEEP in joinedPlayers and vampireTeam — they can reconnect
            return;
        }

        // ENDING state — just clean up
        joinedPlayers.remove(uuid);
    }

    private PerkTier randomTier() {
        PerkTier[] tiers = PerkTier.values();
        return tiers[RANDOM.nextInt(tiers.length)];
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

    /** Defers the roll animation until the player has been out of combat for {@link #COMBAT_TAG_MS}. */
    private void scheduleTimedPerkRoll(Player player, PerkTier finalTier, PerkTeam team) {
        UUID uuid = player.getUniqueId();
        if (!isInCombat(uuid)) {
            startPerkRollAnimation(player, finalTier, team);
            return;
        }
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                new TextComponent(finalTier.getColor() + "★ Perk roll queued — resolves when combat ends ★"));
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !isInGame(uuid) || state != GameState.ACTIVE) {
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
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            new TextComponent(finalTier.getColor() + "\u2605 " + finalTier.getDisplayName() + " Perk! \u2605"));
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
                    // Open GUI after 1 second
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (player.isOnline() && perkSelectionGUI != null) {
                            perkSelectionGUI.openTimedSelection(player, finalTier, team);
                        }
                    }, 20L);
                    cancel();
                    return;
                }
                // Show cycling tier
                PerkTier shown = (frame == delays.length - 1) ? finalTier : tiers[frame % tiers.length];
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        new TextComponent(shown.getColor() + "\u00BB " + shown.getDisplayName() + " \u00AB"));
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f + (frame * 0.05f));
                ticksUntilNext = delays[frame];
                frame++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ===== BLOOD COMPASS =====

    private void giveBloodCompass(Player vampire) {
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.DARK_RED + "Blood Compass");
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Points toward the nearest human",
                    ChatColor.RED + "The hunt intensifies..."
            ));
            compass.setItemMeta(meta);
        }
        vampire.getInventory().addItem(compass);
        vampire.sendMessage(ChatColor.DARK_RED + "You received a " + ChatColor.BOLD + "Blood Compass" +
                ChatColor.RESET + ChatColor.RED + "! It points toward the nearest human.");
    }

    private void updateBloodCompasses() {
        for (UUID vampUUID : vampireTeam) {
            Player vamp = Bukkit.getPlayer(vampUUID);
            if (vamp == null || !vamp.isOnline()) continue;

            // Find nearest human
            Player nearest = null;
            double nearestDist = Double.MAX_VALUE;
            for (UUID humanUUID : humanTeam) {
                Player human = Bukkit.getPlayer(humanUUID);
                if (human == null || !human.isOnline()) continue;
                if (!human.getWorld().equals(vamp.getWorld())) continue;
                double dist = human.getLocation().distanceSquared(vamp.getLocation());
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearest = human;
                }
            }

            if (nearest == null) continue;

            // Update all compasses in the vampire's inventory to point to nearest human
            Location target = nearest.getLocation();
            for (ItemStack item : vamp.getInventory().getContents()) {
                if (item != null && item.getType() == Material.COMPASS) {
                    CompassMeta compassMeta = (CompassMeta) item.getItemMeta();
                    if (compassMeta != null) {
                        compassMeta.setLodestone(target);
                        compassMeta.setLodestoneTracked(false);
                        item.setItemMeta(compassMeta);
                    }
                }
            }
        }
    }

    // ===== GETTERS =====

    public GameState getState() { return state; }
    public Set<UUID> getHumanTeam() { return humanTeam; }
    public Set<UUID> getVampireTeam() { return vampireTeam; }
    public int getRemainingSeconds() { return remainingSeconds; }
    public int getMinPlayers() { return minPlayers; }
    public int getGameDurationSeconds() { return gameDurationSeconds; }

    public boolean isInGame(UUID uuid) {
        return humanTeam.contains(uuid) || vampireTeam.contains(uuid);
    }

    public boolean isHuman(UUID uuid) { return humanTeam.contains(uuid); }
    public boolean isVampire(UUID uuid) { return vampireTeam.contains(uuid); }
    public boolean isVampiresReleased() { return vampiresReleased; }

    public void tagCombat(UUID uuid) { lastCombatMs.put(uuid, System.currentTimeMillis()); }
    public boolean isInCombat(UUID uuid) {
        Long ts = lastCombatMs.get(uuid);
        return ts != null && System.currentTimeMillis() - ts < COMBAT_TAG_MS;
    }

    /**
     * Restores saved states for all players still in the lobby.
     * Called on server shutdown so no stale save files remain on disk after restart.
     */
    public void restoreLobbyPlayers() {
        if (playerStateManager == null) return;
        // Restore online players' inventories
        for (UUID uuid : new HashSet<>(joinedPlayers)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                playerStateManager.restore(player);
            }
        }
        joinedPlayers.clear();
        // Wipe ALL saved state files — server is shutting down, no game will be running
        playerStateManager.clearAllSavedStates();
    }

    public Location getLobbySpawn() { return lobbySpawn; }
    public Location getHumanSpawn() { return humanSpawn; }
    public Location getVampireSpawn() { return vampireSpawn; }

    public void setLobbySpawn(Location loc) {
        this.lobbySpawn = loc;
        saveLocation("spawns.lobby", loc);
    }

    public void setHumanSpawn(Location loc) {
        this.humanSpawn = loc;
        saveLocation("spawns.human", loc);
    }

    public void setVampireSpawn(Location loc) {
        this.vampireSpawn = loc;
        saveLocation("spawns.vampire", loc);
    }

    public EconomyManager getEconomyManager() { return economyManager; }
    public PerkManager getPerkManager() { return perkManager; }
    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
    public DayNightManager getDayNightManager() { return dayNightManager; }
}

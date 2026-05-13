package com.vampirez;

import com.vampirez.api.VampireZAPI;
import com.vampirez.api.VampireZAPIImpl;
import com.vampirez.config.PluginConfig;
import com.vampirez.db.DatabaseManager;
import com.vampirez.db.PlayerStatsRepository;
import com.vampirez.discord.DiscordBot;
import com.vampirez.discord.DiscordEmbedFactory;
import com.vampirez.discord.DiscordEventListener;
import com.vampirez.discord.DiscordStatusUpdater;
import com.vampirez.engine.DataDrivenPerk;
import com.vampirez.engine.PerkConfigLoader;
import com.vampirez.perks.*;
import de.exlll.configlib.NameFormatters;
import de.exlll.configlib.YamlConfigurationProperties;
import de.exlll.configlib.YamlConfigurations;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.file.Path;

public class VampireZPlugin extends JavaPlugin {

    private static VampireZAPI api;

    private GameManager gameManager;
    private ArenaManager arenaManager;
    private PerkListener perkListener;
    private VampireLeapListener leapListener;
    private StatAnvilManager statAnvilManager;
    private PerkStatsManager perkStatsManager;
    private PlayerStatsManager playerStatsManager;
    private PerkSelectionGUI perkSelectionGUI;
    private PluginConfig pluginConfig;
    private DatabaseManager databaseManager;
    private DiscordBot discordBot;
    private DiscordStatusUpdater discordStatusUpdater;

    private static final YamlConfigurationProperties CONFIG_PROPERTIES =
            YamlConfigurationProperties.newBuilder()
                    .setNameFormatter(NameFormatters.LOWER_KEBAB_CASE)
                    .build();

    public GameManager getGameManager() { return gameManager; }
    public ArenaManager getArenaManager() { return arenaManager; }
    public PerkSelectionGUI getPerkSelectionGUI() { return perkSelectionGUI; }
    public PluginConfig getPluginConfig() { return pluginConfig; }

    /** Persist {@link #pluginConfig} back to disk. Call after mutating typed config (e.g. spawn updates). */
    public void savePluginConfig() {
        YamlConfigurations.save(configPath(), PluginConfig.class, pluginConfig, CONFIG_PROPERTIES);
    }

    /** Reload {@link #pluginConfig} from disk and refresh Bukkit's view. Lobby-only. */
    public void reloadPluginConfig() {
        pluginConfig = YamlConfigurations.update(configPath(), PluginConfig.class, CONFIG_PROPERTIES);
        reloadConfig();
    }

    private Path configPath() {
        return new File(getDataFolder(), "config.yml").toPath();
    }

    /** Public API entry point. Returns null until {@link #onEnable()} has run. */
    public static VampireZAPI getAPI() { return api; }

    @Override
    public void onEnable() {
        getLogger().info("VampireZ Plugin Enabled!");
        saveDefaultConfig();
        saveResource("perks.yml", false);

        // Load typed configuration (creates / migrates fields against the existing config.yml).
        pluginConfig = YamlConfigurations.update(configPath(), PluginConfig.class, CONFIG_PROPERTIES);
        // Reload Bukkit's view of config.yml so getConfig() sees any new keys ConfigLib added.
        reloadConfig();

        // Initialize SQLite (HikariCP pool + schema migrations) before any manager that uses it.
        databaseManager = new DatabaseManager(this);
        databaseManager.start();

        // 0. Initialize arena manager and load the arena world
        arenaManager = new ArenaManager(this);
        if (arenaManager.hasTemplate()) {
            arenaManager.loadArenaWorld();
        } else {
            getLogger().warning("No arena template found! Place your arena world in the 'arena-template' folder.");
        }

        // 1. Build leaf managers (no GameManager dependency)
        EconomyManager economyManager = new EconomyManager(this);
        PerkManager perkManager = new PerkManager();
        GearManager gearManager = new GearManager();
        ScoreboardManager scoreboardManager = new ScoreboardManager();
        PlayerStateManager playerStateManager = new PlayerStateManager(this);
        statAnvilManager = new StatAnvilManager(economyManager);

        // 2. Register all perks and apply disabled list from config
        registerAllPerks(perkManager);
        perkManager.setDisabledPerks(pluginConfig.perks.disabledPerks);

        // 3. DayNightManager needs a lazy GameManager reference (cycle: GameManager → DayNightManager → GameManager)
        DayNightManager dayNightManager = new DayNightManager(this, this::getGameManager);

        // 4. GameManager — fully constructor-injected. PerkSelectionGUI is supplied lazily (cycle: GUI → GameManager → GUI)
        gameManager = new GameManager(this, economyManager, perkManager, gearManager, dayNightManager,
                scoreboardManager, arenaManager, playerStateManager, statAnvilManager,
                this::getPerkSelectionGUI);

        // 5. Build GUIs that depend on GameManager (PerkSelectionGUI completes the lazy cycle)
        PerkShopGUI perkShopGUI = new PerkShopGUI(perkManager, economyManager, gameManager, statAnvilManager);
        perkSelectionGUI = new PerkSelectionGUI(perkManager, gameManager, this);
        PerkTestGUI perkTestGUI = new PerkTestGUI(perkManager);

        // 6. Stats trackers — PlayerStatsManager persists to SQLite via the repository.
        perkStatsManager = new PerkStatsManager(this, gameManager, perkManager);
        playerStatsManager = new PlayerStatsManager(this, gameManager,
                new PlayerStatsRepository(databaseManager));

        // 7. Register public API (services manager + static accessor)
        api = new VampireZAPIImpl(gameManager);
        Bukkit.getServicesManager().register(VampireZAPI.class, api, this, ServicePriority.Normal);
        getLogger().info("VampireZAPI registered with ServicesManager");

        // 7b. Optional Discord bot integration (entire block skipped if discord.enabled=false).
        // Note: reloadPluginConfig() does NOT restart the bot — token/channel changes need a server restart.
        if (pluginConfig.discord.enabled) {
            try {
                DiscordEmbedFactory embeds = new DiscordEmbedFactory(this);
                discordBot = new DiscordBot(this);
                discordStatusUpdater = new DiscordStatusUpdater(this, discordBot, gameManager, embeds);
                getServer().getPluginManager().registerEvents(
                        new DiscordEventListener(this, discordBot, discordStatusUpdater, embeds, gameManager), this);
                discordBot.startAsync();
                discordStatusUpdater.start();
            } catch (Throwable t) {
                getLogger().warning("Discord integration failed to initialize: " + t.getMessage());
            }
        }

        // 8. Leaderboard GUI + commands (Cloud handles dispatch + tab-complete + Brigadier)
        LeaderboardGUI leaderboardGUI = new LeaderboardGUI(playerStatsManager);
        GameCommands gameCommands = new GameCommands(gameManager, perkShopGUI, perkTestGUI, api, leaderboardGUI);
        new VampireZCloudCommands(this, gameCommands, gameManager, api).register();

        // 6. Register event listeners
        getServer().getPluginManager().registerEvents(
                new GameListener(this, gameManager, gearManager, economyManager, perkManager, perkShopGUI), this);
        perkListener = new PerkListener(this, gameManager, perkManager, statAnvilManager);
        getServer().getPluginManager().registerEvents(perkListener, this);
        perkListener.startTickTask();
        getServer().getPluginManager().registerEvents(statAnvilManager, this);

        leapListener = new VampireLeapListener(this, gameManager);
        getServer().getPluginManager().registerEvents(leapListener, this);

        // Listener for debug-tool items (left-click → run command)
        getServer().getPluginManager().registerEvents(new DebugBookManager(), this);

        // PerkShopGUI uses triumph-gui (handles its own click events)
        // PerkSelectionGUI uses triumph-gui (handles its own click + close events)
        // PerkTestGUI uses triumph-gui (handles its own click events)
        getServer().getPluginManager().registerEvents(perkStatsManager, this);
        getServer().getPluginManager().registerEvents(playerStatsManager, this);
        // LeaderboardGUI uses triumph-gui (handles its own click events)

        // 7. Scoreboard join/quit listener (uses joined player count, not total online)
        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onPlayerJoin(PlayerJoinEvent event) {
                scoreboardManager.updateLobbyScoreboard(gameManager.getJoinedPlayers().size(), gameManager.getMinPlayers());
            }
            @EventHandler
            public void onPlayerQuit(PlayerQuitEvent event) {
                Bukkit.getScheduler().runTaskLater(VampireZPlugin.this, () ->
                    scoreboardManager.updateLobbyScoreboard(gameManager.getJoinedPlayers().size(), gameManager.getMinPlayers()), 1L);
            }
        }, this);
    }

    @Override
    public void onDisable() {
        getLogger().info("VampireZ Plugin Disabled!");
        if (perkListener != null) perkListener.stopTickTask();
        if (leapListener != null) leapListener.clearCooldowns();
        if (gameManager != null) {
            if (gameManager.getState() != GameState.LOBBY) {
                gameManager.stopGame();
            }
            // Restore all lobby players' saved states so nothing is stale on restart
            gameManager.restoreLobbyPlayers();
        }
        if (perkStatsManager != null) perkStatsManager.save();
        // Player stats: synchronous flush since the scheduler is shutting down.
        if (playerStatsManager != null) playerStatsManager.saveBlocking();
        if (databaseManager != null) databaseManager.stop();
        // Discord: stop the timers first so they can't fire mid-shutdown, then close the JDA connection.
        if (discordStatusUpdater != null) discordStatusUpdater.stop();
        if (discordBot != null) discordBot.shutdown();
        Bukkit.getServicesManager().unregisterAll(this);
        api = null;
    }

    private void registerAllPerks(PerkManager pm) {
        // ===== Data-driven perks (loaded from perks.yml) =====
        PerkConfigLoader loader = new PerkConfigLoader(getLogger());
        File perksYml = new File(getDataFolder(), "perks.yml");
        for (DataDrivenPerk p : loader.loadAll(perksYml)) {
            pm.registerPerk(p);
        }

        // ===== SILVER PERKS =====
        // Both teams
        pm.registerPerk(new DeftPerk());
        pm.registerPerk(new HeavyHitterPerk());
        pm.registerPerk(new GoredrinkPerk());
        pm.registerPerk(new EscapePlanWeakPerk());
        // Human only
        pm.registerPerk(new FirstAidKitPerk());
        pm.registerPerk(new SteadyAimPerk());
        // Vampire only
        pm.registerPerk(new HomeguardPerk());
        pm.registerPerk(new DiveBomberPerk());
        // New Silver - Human
        pm.registerPerk(new IronWallPerk());
        pm.registerPerk(new ThornsPerk());
        pm.registerPerk(new ArchersQuiverPerk());
        pm.registerPerk(new DeflectPerk());
        pm.registerPerk(new GuardiansOathPerk());
        pm.registerPerk(new ProtectionBoostPerk());
        pm.registerPerk(new ArrowSupplyPerk());
        pm.registerPerk(new SpeedPotionsPerk());
        // New Silver - Both
        pm.registerPerk(new RipostePerk());
        // New Silver - Vampire
        pm.registerPerk(new ScavengerPerk());
        pm.registerPerk(new BloodlustPerk());
        pm.registerPerk(new PackHunterPerk());
        pm.registerPerk(new BoneArmorPerk());
        // Expansion Silver - Both
        pm.registerPerk(new MomentumPerk());
        pm.registerPerk(new GravityWellPerk());
        // Expansion Silver - Human
        pm.registerPerk(new NaturalLeaderPerk());
        pm.registerPerk(new CookerPerk());
        pm.registerPerk(new MedicPerk());
        // Expansion Silver - Vampire
        pm.registerPerk(new BloodScentPerk());
        // Expansion Silver - Both
        pm.registerPerk(new SupplyDropPerk());
        pm.registerPerk(new ScalingPerk());
        pm.registerPerk(new BlackShieldPerk());
        pm.registerPerk(new TrailPerk());
        pm.registerPerk(new ComboShieldPerk());
        pm.registerPerk(new HastePerk());
        // New Silver - Both
        pm.registerPerk(new SunderPerk());
        pm.registerPerk(new RegenerativePerk());
        pm.registerPerk(new HeadhunterPerk());
        // New Silver - Human
        pm.registerPerk(new PorcupinePerk());
        pm.registerPerk(new WarDrumsPerk());
        pm.registerPerk(new FortuneTellerPerk());
        // Lucky Roll - Silver
        pm.registerPerk(new LuckyRollSilverPerk());

        // ===== GOLD PERKS =====
        // Both teams
        pm.registerPerk(new GetExcitedPerk());
        pm.registerPerk(new ItsCriticalPerk());
        // Human only
        pm.registerPerk(new DawnbringersResolvePerk());
        pm.registerPerk(new AllForYouPerk());
        pm.registerPerk(new ArmoredUpPerk());
        pm.registerPerk(new PhoenixDownPerk());
        pm.registerPerk(new MirrorShieldPerk());
        pm.registerPerk(new GoldenGuardPerk());
        pm.registerPerk(new BarricadePerk());
        pm.registerPerk(new StrengthPotionsPerk());
        pm.registerPerk(new ChainArmorPerk());
        // Vampire only
        pm.registerPerk(new BluntForceGoldPerk());
        pm.registerPerk(new ShadowStrikePerk());
        pm.registerPerk(new PhantomStepPerk());
        pm.registerPerk(new BloodPricePerk());
        pm.registerPerk(new TetherPerk());
        // Both teams (Gold)
        pm.registerPerk(new SmokeBombPerk());
        // Expansion Gold - Both
        pm.registerPerk(new LastStandPerk());
        pm.registerPerk(new SiphonPerk());
        pm.registerPerk(new HeartstealPerk());
        pm.registerPerk(new BlackCleaverPerk());
        pm.registerPerk(new BountyHunterPerk());
        pm.registerPerk(new WhirlwindPerk());
        pm.registerPerk(new LifeLinkPerk());
        // Expansion Gold - Human
        pm.registerPerk(new ConsecratedGroundPerk());
        pm.registerPerk(new ShieldPerk());
        // Expansion Gold - Vampire
        pm.registerPerk(new HemophiliaPerk());
        pm.registerPerk(new NocturnalPerk());
        pm.registerPerk(new CorpseExplosionPerk());
        pm.registerPerk(new BloodBeaconPerk());
        pm.registerPerk(new ShadowAmbushPerk());
        // Expansion Gold - Human
        pm.registerPerk(new SunfireCapePerk());
        pm.registerPerk(new RicochetShotPerk());
        pm.registerPerk(new OverchargePerk());
        pm.registerPerk(new AlwaysConnectedPerk());
        // Expansion Gold - Vampire
        pm.registerPerk(new SpiderClimbPerk());
        pm.registerPerk(new PlagueCarrierPerk());
        // Expansion Gold - Both
        pm.registerPerk(new SelfishPerk());
        pm.registerPerk(new GoreDrinkerPerk());
        pm.registerPerk(new WarHorsePerk());
        // Expansion Gold - Human
        pm.registerPerk(new FightOrBeForgottenPerk());
        // Lucky Roll - Gold
        pm.registerPerk(new LuckyRollGoldPerk());
        // Nether Blade - Gold
        pm.registerPerk(new NetherBladePerk());
        // Long Bow - Gold - Human
        pm.registerPerk(new LongBowPerk());
        // Ninja Turtle - Gold - Human
        pm.registerPerk(new NinjaTurtlePerk());

        // ===== PRISMATIC PERKS =====
        // Both teams
        pm.registerPerk(new CantTouchThisPerk());
        pm.registerPerk(new GoliathPerk());
        pm.registerPerk(new ThunderstrikePerk());
        pm.registerPerk(new EarthquakePerk());
        // Human only
        pm.registerPerk(new CourageOfTheColossusPerk());
        pm.registerPerk(new EscapePlanStrongPerk());
        pm.registerPerk(new GiantSlayerPerk());
        pm.registerPerk(new NetheriteArsenalPerk());
        pm.registerPerk(new GuardianAngelPerk());
        pm.registerPerk(new TrapperPerk());
        pm.registerPerk(new CitadelPerk());
        pm.registerPerk(new HolyShieldPerk());
        pm.registerPerk(new TemporalShieldPerk());
        pm.registerPerk(new IronGuardianPerk());
        // DiamondEdgePerk removed
        pm.registerPerk(new ThornsEnchantPerk());
        pm.registerPerk(new RegenPotionsPerk());
        // Vampire only
        pm.registerPerk(new FinalFormPerk());
        pm.registerPerk(new DoubleTapPerk());
        pm.registerPerk(new BatFormPerk());
        pm.registerPerk(new SoulEaterPerk());
        pm.registerPerk(new SummonerPerk());
        pm.registerPerk(new VoidWalkerPerk());
        pm.registerPerk(new ReapersMarkPerk());
        // Expansion Prismatic - Both
        pm.registerPerk(new ChainLightningPerk());
        // DeathsGambitPerk removed
        pm.registerPerk(new PlagueDoctorPerk());
        // Expansion Prismatic - Human
        pm.registerPerk(new RadiantAuraPerk());
        pm.registerPerk(new TimeWarpPerk());
        pm.registerPerk(new BardPerk());
        pm.registerPerk(new PlantMasterPerk());
        // Expansion Prismatic - Both
        pm.registerPerk(new DecoyPerk());
        pm.registerPerk(new AnkhOfRebirthPerk());
        // Expansion Prismatic - Human
        pm.registerPerk(new DimensionalPocketPerk());
        // Expansion Prismatic - Vampire
        pm.registerPerk(new BloodMoonPerk());
        pm.registerPerk(new WraithWalkPerk());
        pm.registerPerk(new CurseOfDecayPerk());
        // Lucky Roll - Prismatic
        pm.registerPerk(new LuckyRollPrismaticPerk());
        // Galeforce - Prismatic
        pm.registerPerk(new GaleforcePerk());
    }
}

package com.vampirez;

import com.vampirez.perks.*;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class VampireZPlugin extends JavaPlugin {

    private GameManager gameManager;
    private ArenaManager arenaManager;
    private PerkListener perkListener;
    private VampireLeapListener leapListener;
    private StatAnvilManager statAnvilManager;

    public GameManager getGameManager() { return gameManager; }
    public ArenaManager getArenaManager() { return arenaManager; }

    @Override
    public void onEnable() {
        getLogger().info("VampireZ Plugin Enabled!");
        saveDefaultConfig();

        // 0. Initialize arena manager and load the arena world
        arenaManager = new ArenaManager(this);
        if (arenaManager.hasTemplate()) {
            arenaManager.loadArenaWorld();
        } else {
            getLogger().warning("No arena template found! Place your arena world in the 'arena-template' folder.");
        }

        // 1. Initialize managers
        EconomyManager economyManager = new EconomyManager(this);
        PerkManager perkManager = new PerkManager();
        GearManager gearManager = new GearManager();
        DayNightManager dayNightManager = new DayNightManager(this);
        ScoreboardManager scoreboardManager = new ScoreboardManager();

        // 2. Register all perks
        registerAllPerks(perkManager);

        // 3. Initialize game manager
        gameManager = new GameManager(this, economyManager, perkManager,
                gearManager, dayNightManager, scoreboardManager);
        gameManager.setArenaManager(arenaManager);
        dayNightManager.setGameManager(gameManager);

        // 3b. Initialize player state manager (inventory save/restore for join/leave)
        PlayerStateManager playerStateManager = new PlayerStateManager(this);
        gameManager.setPlayerStateManager(playerStateManager);

        // 4. Initialize GUIs and StatAnvil
        statAnvilManager = new StatAnvilManager(economyManager);
        PerkShopGUI perkShopGUI = new PerkShopGUI(perkManager, economyManager, gameManager, statAnvilManager);
        PerkSelectionGUI perkSelectionGUI = new PerkSelectionGUI(perkManager, gameManager, this);
        PerkTestGUI perkTestGUI = new PerkTestGUI(perkManager);
        gameManager.setPerkSelectionGUI(perkSelectionGUI);
        gameManager.setStatAnvilManager(statAnvilManager);

        // 5. Register commands
        getCommand("vampirez").setExecutor(new GameCommands(gameManager, perkShopGUI, perkTestGUI));

        // 6. Register event listeners
        getServer().getPluginManager().registerEvents(
                new GameListener(this, gameManager, gearManager, economyManager, perkManager, perkShopGUI), this);
        perkListener = new PerkListener(this, gameManager, perkManager, statAnvilManager);
        getServer().getPluginManager().registerEvents(perkListener, this);
        perkListener.startTickTask();
        getServer().getPluginManager().registerEvents(statAnvilManager, this);

        leapListener = new VampireLeapListener(this, gameManager);
        getServer().getPluginManager().registerEvents(leapListener, this);

        getServer().getPluginManager().registerEvents(perkShopGUI, this);
        getServer().getPluginManager().registerEvents(perkSelectionGUI, this);
        getServer().getPluginManager().registerEvents(perkTestGUI, this);

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
    }

    private void registerAllPerks(PerkManager pm) {
        // ===== SILVER PERKS =====
        // Both teams
        pm.registerPerk(new BluntForcePerk());
        pm.registerPerk(new DeftPerk());
        pm.registerPerk(new HeavyHitterPerk());
        pm.registerPerk(new GoredrinkPerk());
        pm.registerPerk(new EscapePlanWeakPerk());
        pm.registerPerk(new VitalityPerk());
        pm.registerPerk(new ToughSkinPerk());
        pm.registerPerk(new SwiftStrikesPerk());
        // Human only
        pm.registerPerk(new FirstAidKitPerk());
        pm.registerPerk(new BuffBuddiesPerk());
        pm.registerPerk(new SteadyAimPerk());
        // Vampire only
        pm.registerPerk(new HomeguardPerk());
        pm.registerPerk(new DiveBomberPerk());
        // New Silver - Human
        pm.registerPerk(new IronWallPerk());
        pm.registerPerk(new ThornsPerk());
        pm.registerPerk(new ArchersQuiverPerk());
        pm.registerPerk(new DeflectPerk());
        pm.registerPerk(new FortifyPerk());
        pm.registerPerk(new GuardiansOathPerk());
        pm.registerPerk(new WolfPackPerk());
        pm.registerPerk(new SharpnessBoostPerk());
        pm.registerPerk(new ProtectionBoostPerk());
        pm.registerPerk(new ArrowSupplyPerk());
        pm.registerPerk(new HealingPotionsPerk());
        pm.registerPerk(new FireAspectPerk());
        pm.registerPerk(new SpeedPotionsPerk());
        // New Silver - Both
        pm.registerPerk(new DashPerk());
        pm.registerPerk(new LightweightPerk());
        pm.registerPerk(new SecondWindPerk());
        pm.registerPerk(new AdrenalineRushPerk());
        pm.registerPerk(new RipostePerk());
        pm.registerPerk(new FlameArrowsPerk());
        // New Silver - Vampire
        pm.registerPerk(new ScavengerPerk());
        pm.registerPerk(new BackstabPerk());
        pm.registerPerk(new BloodlustPerk());
        pm.registerPerk(new PoisonFangPerk());
        pm.registerPerk(new PackHunterPerk());
        pm.registerPerk(new BoneArmorPerk());
        pm.registerPerk(new LeechSwarmPerk());
        pm.registerPerk(new UndeadHordePerk());
        // Expansion Silver - Both
        pm.registerPerk(new MomentumPerk());
        pm.registerPerk(new GravityWellPerk());
        // Expansion Silver - Human
        pm.registerPerk(new RallyCryPerk());
        pm.registerPerk(new NaturalLeaderPerk());
        pm.registerPerk(new CookerPerk());
        pm.registerPerk(new MedicPerk());
        // Expansion Silver - Vampire
        pm.registerPerk(new BloodScentPerk());
        pm.registerPerk(new FeralChargePerk());
        pm.registerPerk(new InfectiousBitePerk());
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
        pm.registerPerk(new HeavyweightPerk());
        pm.registerPerk(new PorcupinePerk());
        pm.registerPerk(new WarDrumsPerk());
        pm.registerPerk(new FortuneTellerPerk());
        // Lucky Roll - Silver
        pm.registerPerk(new LuckyRollSilverPerk());

        // ===== GOLD PERKS =====
        // Both teams
        pm.registerPerk(new CelestialBodyPerk());
        pm.registerPerk(new ExecutionerPerk());
        pm.registerPerk(new GetExcitedPerk());
        pm.registerPerk(new ItsCriticalPerk());
        // Human only
        pm.registerPerk(new DawnbringersResolvePerk());
        pm.registerPerk(new AllForYouPerk());
        pm.registerPerk(new ArmoredUpPerk());
        pm.registerPerk(new PhoenixDownPerk());
        pm.registerPerk(new CrossbowExpertPerk());
        pm.registerPerk(new MirrorShieldPerk());
        pm.registerPerk(new GoldenGuardPerk());
        pm.registerPerk(new BarricadePerk());
        pm.registerPerk(new PowerShotPerk());
        pm.registerPerk(new BlastShieldPerk());
        pm.registerPerk(new StrengthPotionsPerk());
        pm.registerPerk(new GoldenFeastPerk());
        pm.registerPerk(new KnockbackPerk());
        pm.registerPerk(new ChainArmorPerk());
        pm.registerPerk(new PoisonQuiverPerk());
        // Vampire only
        pm.registerPerk(new BluntForceGoldPerk());
        pm.registerPerk(new ShadowStrikePerk());
        pm.registerPerk(new FrostBitePerk());
        pm.registerPerk(new PhantomStepPerk());
        pm.registerPerk(new BloodPricePerk());
        pm.registerPerk(new TetherPerk());
        pm.registerPerk(new SkeletonArchersPerk());
        pm.registerPerk(new HarmingPotionsPerk());
        // Both teams (Gold)
        pm.registerPerk(new BerserkerPerk());
        pm.registerPerk(new SmokeBombPerk());
        // EnderPearlSupplyPerk removed
        pm.registerPerk(new IronRationsPerk());
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
        pm.registerPerk(new MartyrPerk());
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
        pm.registerPerk(new GlassCannonPerk());
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
        pm.registerPerk(new MarksmanPerk());
        pm.registerPerk(new CitadelPerk());
        pm.registerPerk(new HolyShieldPerk());
        pm.registerPerk(new TemporalShieldPerk());
        pm.registerPerk(new IronGuardianPerk());
        // DiamondEdgePerk removed
        pm.registerPerk(new ThornsEnchantPerk());
        pm.registerPerk(new RegenPotionsPerk());
        // Vampire only
        pm.registerPerk(new ErosionPerk());
        pm.registerPerk(new FinalFormPerk());
        pm.registerPerk(new FirebrandPerk());
        pm.registerPerk(new DoubleTapPerk());
        pm.registerPerk(new BatFormPerk());
        pm.registerPerk(new SoulEaterPerk());
        pm.registerPerk(new SummonerPerk());
        pm.registerPerk(new VoidWalkerPerk());
        pm.registerPerk(new ReapersMarkPerk());
        pm.registerPerk(new WitherGuardPerk());
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

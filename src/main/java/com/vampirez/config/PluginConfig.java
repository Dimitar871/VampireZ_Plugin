package com.vampirez.config;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Typed plugin configuration. Mirrors the legacy {@code config.yml} key layout exactly so that
 * existing server installations continue to load without any manual edits. Field names are
 * mapped to kebab-case (e.g. {@code minPlayers} → {@code min-players}) by the loader.
 */
@Configuration
public class PluginConfig {

    public GameSection game = new GameSection();
    public EconomySection economy = new EconomySection();
    public PerksSection perks = new PerksSection();
    public ArenaSection arena = new ArenaSection();
    public DayNightSection dayNight = new DayNightSection();
    public SpawnsSection spawns = new SpawnsSection();
    public MessagesSection messages = new MessagesSection();
    public TimingsSection timings = new TimingsSection();

    @Configuration
    public static class GameSection {
        @Comment("Minimum players required to start a game")
        public int minPlayers = 10;
        @Comment("Game duration in seconds (300-3600 recommended)")
        public int gameDurationSeconds = 1500;
        @Comment("Fraction of players assigned to vampires at game start (0.1-0.9)")
        public double vampireRatio = 0.3;
        @Comment("Ticks before a dead vampire respawns (20 ticks = 1s)")
        public int vampireRespawnDelayTicks = 80;
        @Comment("Auto-start countdown when min players reached, in seconds")
        public int lobbyCountdownSeconds = 30;
        @Comment("Countdown shown when /vz start runs, in seconds")
        public int startCountdownSeconds = 15;
    }

    @Configuration
    public static class EconomySection {
        @Comment("Gold awarded passively to every player on each tick interval")
        public int passiveIncomeAmount = 2;
        @Comment("How often passive income is paid out (ticks; 200 = 10s)")
        public int passiveIncomeIntervalTicks = 200;
        @Comment("Gold awarded to the killer for a kill")
        public int killReward = 15;
        @Comment("Gold awarded to each assister for an assist")
        public int assistReward = 5;
        @Comment("Window (ms) during which damage to a victim counts as an assist")
        public long assistTimeWindowMs = 10000L;
    }

    @Configuration
    public static class PerksSection {
        @Comment("Maximum perks a single player can hold at once")
        public int maxPerksPerPlayer = 10;
        public int silverCost = 50;
        public int goldCost = 150;
        public int prismaticCost = 400;
        @Comment("Number of random perk options shown per shop purchase")
        public int optionsPerPurchase = 3;
        @Comment("Perk IDs to hide from random rolls, the shop, and selection menus")
        public List<String> disabledPerks = new ArrayList<>();
    }

    @Configuration
    public static class ArenaSection {
        @Comment("Base name of the arena world; instance worlds are <name>_<n>")
        public String worldName = "vampirez_arena";
        @Comment("Folder under plugins/VampireZ holding the arena template files")
        public String templateFolder = "arena-template";
    }

    @Configuration
    public static class DayNightSection {
        public boolean enabled = true;
        public int dayDurationTicks = 7200;
        public int nightDurationTicks = 4800;
    }

    @Configuration
    public static class SpawnsSection {
        public SpawnPoint lobby = new SpawnPoint();
        public SpawnPoint human = new SpawnPoint();
        public SpawnPoint vampire = new SpawnPoint();
    }

    @Configuration
    public static class SpawnPoint {
        public String world = "";
        public double x = 0.0;
        public double y = 0.0;
        public double z = 0.0;
        public float yaw = 0.0f;
        public float pitch = 0.0f;
    }

    @Configuration
    public static class TimingsSection {
        @Comment("Window (ms) a player stays \"in combat\" after dealing or taking PvP damage")
        public long combatTagMs = 7_000L;
        @Comment("Vampire Leap (ghast tear) cooldown in seconds")
        public int vampireLeapCooldownSeconds = 8;
        @Comment("Game time remaining (seconds) when vampires receive the Blood Compass")
        public int bloodCompassUnlockAtRemainingSeconds = 600;
        @Comment("Elapsed seconds at which the free Silver perk roll fires (default: 5 minutes)")
        public int freeSilverPerkAtSeconds = 300;
        @Comment("Elapsed seconds at which the free Gold perk roll fires (default: 10 minutes)")
        public int freeGoldPerkAtSeconds = 600;
        @Comment("Elapsed seconds at which the free Prismatic perk roll fires (default: 15 minutes)")
        public int freePrismaticPerkAtSeconds = 900;
    }

    @Configuration
    public static class MessagesSection {
        public String prefix = "&8[&4VampireZ&8] ";
        public String gameStart = "&cThe hunt begins!";
        public String vampiresWin = "&4The Vampires have won! All humans have been slain!";
        public String humansWin = "&bThe Humans have survived! Dawn breaks and the vampires retreat!";
        public String humanDeath = "&c{player} has fallen! They rise again as a Vampire!";
        public String nightFall = "&4&lNight has fallen! Vampires grow stronger...";
        public String dayBreak = "&e&lThe sun rises! Vampires are weakened...";
    }
}

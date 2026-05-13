package com.vampirez.discord;

import com.vampirez.GameManager;
import com.vampirez.GameState;
import com.vampirez.VampireZPlugin;
import com.vampirez.config.PluginConfig;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

/**
 * Owns the two repeating tasks that push live data to Discord:
 *   - presence text (every {@code presenceUpdateSeconds})
 *   - status embed   (every {@code statusEmbedUpdateSeconds} during ACTIVE games)
 *
 * Both run on the main thread (cheap reads from {@link VampireZAPI}); they hand
 * serialized payloads to {@link DiscordBot} which queues to JDA's executor.
 */
public class DiscordStatusUpdater {

    private final VampireZPlugin plugin;
    private final DiscordBot bot;
    private final GameManager game;
    private final DiscordEmbedFactory embeds;

    private BukkitTask presenceTask;
    private BukkitTask embedTask;
    private GameState lastState = null;

    public DiscordStatusUpdater(VampireZPlugin plugin, DiscordBot bot, GameManager game, DiscordEmbedFactory embeds) {
        this.plugin = plugin;
        this.bot = bot;
        this.game = game;
        this.embeds = embeds;
    }

    public void start() {
        PluginConfig.DiscordSection cfg = plugin.getPluginConfig().discord;
        long presencePeriod = Math.max(15, cfg.presenceUpdateSeconds) * 20L;
        long embedPeriod    = Math.max(5,  cfg.statusEmbedUpdateSeconds) * 20L;

        presenceTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updatePresence,
                presencePeriod, presencePeriod);
        embedTask    = Bukkit.getScheduler().runTaskTimer(plugin, this::updateEmbedIfNeeded,
                embedPeriod, embedPeriod);
    }

    public void stop() {
        if (presenceTask != null) { presenceTask.cancel(); presenceTask = null; }
        if (embedTask != null)    { embedTask.cancel();    embedTask = null;    }
    }

    /** Called by event listener on state-changing events for an immediate refresh. */
    public void kick() {
        if (!bot.isReady()) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            updatePresence();
            updateEmbedIfNeeded();
        });
    }

    // ===== presence =====

    private void updatePresence() {
        if (!bot.isReady()) return;
        bot.setPresence(presenceText());
    }

    String presenceText() {
        return presenceText(game.getState(),
                game.getJoinedPlayers().size(),
                game.getMinPlayers(),
                game.getHumanTeam().size(),
                game.getVampireTeam().size(),
                game.getRemainingSeconds());
    }

    /** Pure helper for testing. */
    static String presenceText(GameState state, int joined, int minPlayers,
                                int humans, int vampires, int remainingSeconds) {
        return switch (state) {
            case LOBBY    -> "VampireZ: lobby " + joined + "/" + minPlayers;
            case STARTING -> "VampireZ: starting…";
            case ACTIVE   -> "In game: " + humans + "H vs " + vampires + "V";
            case ENDING   -> "VampireZ: round ending";
        };
    }

    // ===== embed =====

    private void updateEmbedIfNeeded() {
        if (!bot.isReady()) return;
        GameState state = game.getState();
        // Only push embed updates during ACTIVE, OR on state transitions.
        boolean stateChanged = (state != lastState);
        lastState = state;
        if (state != GameState.ACTIVE && !stateChanged) return;

        bot.sendOrEditStatusEmbed(embeds.liveStatus(
                state,
                game.getRemainingSeconds(),
                game.getJoinedPlayers().size(),
                game.getHumanTeam().size(),
                game.getVampireTeam().size(),
                game.getDayNightManager().isNight(),
                game.getMinPlayers()));
    }
}

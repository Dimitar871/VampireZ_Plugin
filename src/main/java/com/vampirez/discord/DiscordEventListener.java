package com.vampirez.discord;

import com.vampirez.GameManager;
import com.vampirez.VampireZPlugin;
import com.vampirez.api.event.DayPhaseChangeEvent;
import com.vampirez.api.event.PlayerConvertedEvent;
import com.vampirez.api.event.VampireZGameEndEvent;
import com.vampirez.api.event.VampireZGameStartEvent;
import com.vampirez.config.PluginConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Bukkit listener that translates plugin events into Discord postings via {@link DiscordBot}
 * and triggers immediate refreshes through {@link DiscordStatusUpdater#kick()}.
 *
 * <p>All handlers run at MONITOR priority — observe-only, never alter game state.
 * Player names are resolved on the main thread before being passed to async paths.
 */
public class DiscordEventListener implements Listener {

    private final VampireZPlugin plugin;
    private final DiscordBot bot;
    private final DiscordStatusUpdater updater;
    private final DiscordEmbedFactory embeds;
    private final GameManager game;

    public DiscordEventListener(VampireZPlugin plugin, DiscordBot bot,
                                DiscordStatusUpdater updater, DiscordEmbedFactory embeds,
                                GameManager game) {
        this.plugin = plugin;
        this.bot = bot;
        this.updater = updater;
        this.embeds = embeds;
        this.game = game;
    }

    private PluginConfig.DiscordSection cfg() { return plugin.getPluginConfig().discord; }

    // ===== Game lifecycle =====

    @EventHandler(priority = EventPriority.MONITOR)
    public void onGameStart(VampireZGameStartEvent event) {
        bot.postAnnouncement(embeds.gameStart(
                resolveNames(event.getHumans()),
                resolveNames(event.getVampires()),
                event.isForced()));
        updater.kick();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onGameEnd(VampireZGameEndEvent event) {
        bot.postAnnouncement(embeds.gameEnd(event.getWinner(), event.getDurationPlayedSeconds()));
        updater.kick();
    }

    // ===== In-game events =====

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConverted(PlayerConvertedEvent event) {
        if (cfg().announceConversions) {
            String name = nameOf(event.getPlayer());
            bot.postAnnouncement(embeds.conversion(name,
                    game.getHumanTeam().size(),
                    game.getVampireTeam().size()));
        }
        updater.kick();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDayPhase(DayPhaseChangeEvent event) {
        if (cfg().announceDayNight) {
            bot.postAnnouncement(embeds.dayPhase(event.isNowNight()));
        }
        updater.kick();
    }

    // ===== Lobby presence updates =====
    // (Bukkit join/quit so the lobby player count in presence/embed reflects new joiners promptly)

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        updater.kick();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Bukkit's quit fires before player leaves online list — defer one tick so counts are right.
        Bukkit.getScheduler().runTask(plugin, updater::kick);
    }

    // ===== helpers (main-thread only) =====

    private List<String> resolveNames(Set<UUID> uuids) {
        List<String> out = new ArrayList<>(uuids.size());
        for (UUID uuid : uuids) out.add(nameOf(uuid));
        return out;
    }

    private static String nameOf(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) return online.getName();
        String offline = Bukkit.getOfflinePlayer(uuid).getName();
        return offline != null ? offline : uuid.toString().substring(0, 8);
    }
}

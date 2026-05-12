package com.vampirez;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Sound;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;

import java.time.Duration;
import java.util.Collection;
import java.util.function.Supplier;

/**
 * Pure broadcast/I/O helper. Builds Components from typed config messages and pushes them
 * (with sounds, titles, fireworks) to the joined-online audience. No game-state mutation;
 * holds no per-game state of its own.
 */
public class GameAnnouncer {

    private final VampireZPlugin plugin;
    private final Supplier<Collection<Player>> audience;

    public GameAnnouncer(VampireZPlugin plugin, Supplier<Collection<Player>> audience) {
        this.plugin = plugin;
        this.audience = audience;
    }

    private Component prefix() {
        return MM.legacy(plugin.getPluginConfig().messages.prefix);
    }

    /** Send a prefixed message to every joined-online player. */
    public void broadcast(Component msg) {
        Component pfx = prefix();
        for (Player p : audience.get()) {
            p.sendMessage(pfx.append(msg));
        }
    }

    /** Game-start announcement: configurable message + optional "PvP enabled" title when vampires released. */
    public void broadcastGameStart(boolean vampiresReleased) {
        Component startMsg = MM.legacy(plugin.getPluginConfig().messages.gameStart);
        Component pfx = prefix();
        for (Player player : audience.get()) {
            player.sendMessage(pfx.append(startMsg));
            if (vampiresReleased) {
                player.sendMessage(MM.parse("<red><bold>PvP is now ENABLED!</bold> <yellow>The hunt begins!"));
                player.showTitle(Title.title(
                        MM.parse("<red><bold>PvP ENABLED!"),
                        MM.parse("<yellow>The hunt begins!"),
                        Title.Times.times(Duration.ofMillis(250), Duration.ofMillis(1500), Duration.ofMillis(500))));
            }
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.0f);
        }
    }

    /** Conversion announcement: "{victim} has fallen! They rise again as a Vampire!" */
    public void broadcastConversion(Player victim) {
        Component msg = MM.legacy(plugin.getPluginConfig().messages.humanDeath
                .replace("{player}", victim.getName()));
        broadcast(msg);
    }

    /** End-of-game winner announcement + per-player firework. */
    public void broadcastWinner(boolean humansWin) {
        Component winMsg = humansWin
                ? MM.legacy(plugin.getPluginConfig().messages.humansWin)
                : MM.legacy(plugin.getPluginConfig().messages.vampiresWin);
        Component pfx = prefix();
        for (Player player : audience.get()) {
            player.sendMessage(pfx.append(winMsg));
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
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
    }
}

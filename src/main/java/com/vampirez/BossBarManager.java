package com.vampirez;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

/**
 * Renders the active-game boss bar at the top of every joined player's screen.
 * Shows time remaining + day/night phase + team counts.
 *
 * <p>One shared {@link BossBar} instance — Adventure's protocol handles per-player viewers
 * via {@link Player#showBossBar} / {@link Player#hideBossBar}, so we never juggle per-player
 * state ourselves.
 */
public class BossBarManager {

    private final BossBar bar;
    private boolean visible = false;

    public BossBarManager() {
        this.bar = BossBar.bossBar(
                Component.text("VampireZ").color(NamedTextColor.GOLD),
                1.0f,
                BossBar.Color.RED,
                BossBar.Overlay.PROGRESS);
    }

    /** Show the bar to a player. Idempotent. */
    public void show(Player player) {
        player.showBossBar(bar);
    }

    /** Hide the bar from a player. Idempotent. */
    public void hide(Player player) {
        player.hideBossBar(bar);
    }

    /**
     * Refresh the bar's title + progress + color based on current game state.
     *
     * @param remainingSeconds   game timer (seconds left)
     * @param totalSeconds       configured game duration
     * @param isNight            day/night phase
     * @param humansAlive        count of human-team members
     * @param vampireCount       count of vampire-team members
     */
    public void update(int remainingSeconds, int totalSeconds, boolean isNight,
                       int humansAlive, int vampireCount) {
        int minutes = remainingSeconds / 60;
        int seconds = remainingSeconds % 60;
        String time = String.format("%d:%02d", minutes, seconds);

        NamedTextColor timeColor = remainingSeconds > 300
                ? NamedTextColor.GREEN
                : (remainingSeconds > 60 ? NamedTextColor.YELLOW : NamedTextColor.RED);

        Component title = Component.text("⏰ ").color(NamedTextColor.GOLD)
                .append(Component.text(time).color(timeColor))
                .append(Component.text("   ").color(NamedTextColor.DARK_GRAY))
                .append(Component.text(isNight ? "🌙 Night" : "☀ Day").color(isNight ? NamedTextColor.DARK_PURPLE : NamedTextColor.YELLOW))
                .append(Component.text("   ").color(NamedTextColor.DARK_GRAY))
                .append(Component.text("Humans: ").color(NamedTextColor.AQUA))
                .append(Component.text(String.valueOf(humansAlive)).color(NamedTextColor.WHITE))
                .append(Component.text("   ").color(NamedTextColor.DARK_GRAY))
                .append(Component.text("Vampires: ").color(NamedTextColor.RED))
                .append(Component.text(String.valueOf(vampireCount)).color(NamedTextColor.WHITE));

        bar.name(title);

        float progress = totalSeconds <= 0 ? 0f : Math.max(0f, Math.min(1f, (float) remainingSeconds / totalSeconds));
        bar.progress(progress);

        // Tint the bar to match phase: red for night (vampires strong), yellow for day (vampires weak).
        bar.color(isNight ? BossBar.Color.RED : BossBar.Color.YELLOW);
    }

    /** Whether the bar is currently showing (game in ACTIVE state). */
    public boolean isVisible() { return visible; }
    public void setVisible(boolean v) { this.visible = v; }
}

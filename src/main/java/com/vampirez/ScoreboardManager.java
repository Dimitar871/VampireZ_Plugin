package com.vampirez;

import fr.mrmicky.fastboard.adventure.FastBoard;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;

public class ScoreboardManager {

    private Scoreboard teamScoreboard;
    private final Map<UUID, FastBoard> boards = new HashMap<>();

    private static final Component TITLE = MM.parse("<dark_red><bold>VAMPIREZ");
    private static final Component SEPARATOR = MM.parse("<dark_gray>━━━━━━━━━━━━━━━━━");

    public void setupLobbyScoreboard() {
        teamScoreboard = Bukkit.getScoreboardManager().getNewScoreboard();

        Team humanTeam = teamScoreboard.registerNewTeam("humans");
        humanTeam.color(NamedTextColor.BLUE);
        humanTeam.setAllowFriendlyFire(false);

        Team vampTeam = teamScoreboard.registerNewTeam("vampires");
        vampTeam.color(NamedTextColor.RED);
        vampTeam.setAllowFriendlyFire(false);
    }

    public void updateLobbyScoreboard(int playerCount, int minPlayers) {
        updateLobbyScoreboard(playerCount, minPlayers, -1);
    }

    public void updateLobbyScoreboard(int playerCount, int minPlayers, int autoStartCountdown) {
        if (teamScoreboard == null) setupLobbyScoreboard();

        for (FastBoard board : boards.values()) {
            if (!board.isDeleted()) {
                List<Component> lines = new ArrayList<>();
                lines.add(SEPARATOR);
                lines.add(Component.empty());
                lines.add(MM.parse("<gray>       \u25B6 LOBBY \u25C0"));
                lines.add(Component.empty());
                lines.add(MM.parse("<white>  Players: <green>" + playerCount + "<gray>/<white>100"));
                lines.add(MM.parse("<white>  Min to start: <yellow>" + minPlayers));
                lines.add(Component.empty());
                if (autoStartCountdown >= 0) {
                    lines.add(MM.parse("<green>  Starting in: <yellow><bold>" + autoStartCountdown + "s"));
                } else if (playerCount >= minPlayers) {
                    lines.add(MM.parse("<green>  \u2714 Ready to start!"));
                } else {
                    lines.add(MM.parse("<yellow>  Waiting for players..."));
                }
                lines.add(Component.empty());
                lines.add(SEPARATOR);
                board.updateLines(lines);
            }
        }
    }

    public void showLobbyScoreboard(Player player) {
        if (teamScoreboard == null) setupLobbyScoreboard();

        // Set the Bukkit scoreboard for team colors
        player.setScoreboard(teamScoreboard);

        // Create FastBoard for sidebar
        UUID uuid = player.getUniqueId();
        FastBoard existing = boards.get(uuid);
        if (existing != null && !existing.isDeleted()) {
            existing.delete();
        }

        FastBoard board = new FastBoard(player);
        board.updateTitle(TITLE);
        boards.put(uuid, board);
    }

    public void createGameScoreboard(Player player, GameManager gameManager, EconomyManager economyManager, PerkManager perkManager, DayNightManager dayNightManager) {
        if (teamScoreboard == null) setupLobbyScoreboard();

        // Ensure teams are set up on the shared scoreboard
        syncTeams(gameManager);

        // Set the shared Bukkit scoreboard for team name colors + friendly fire
        player.setScoreboard(teamScoreboard);

        // Create FastBoard for sidebar
        UUID uuid = player.getUniqueId();
        FastBoard existing = boards.get(uuid);
        if (existing != null && !existing.isDeleted()) {
            existing.delete();
        }

        FastBoard board = new FastBoard(player);
        board.updateTitle(TITLE);
        boards.put(uuid, board);

        updateGameScoreboard(player, gameManager, economyManager, perkManager, dayNightManager);
    }

    public void updateGameScoreboard(Player player, GameManager gameManager, EconomyManager economyManager, PerkManager perkManager, DayNightManager dayNightManager) {
        FastBoard board = boards.get(player.getUniqueId());
        if (board == null || board.isDeleted()) return;

        UUID uuid = player.getUniqueId();
        int humansAlive = gameManager.getHumanTeam().size();
        int vampires = gameManager.getVampireTeam().size();
        int remainingSeconds = gameManager.getRemainingSeconds();
        int minutes = remainingSeconds / 60;
        int seconds = remainingSeconds % 60;
        String timeStr = String.format("%02d:%02d", minutes, seconds);

        String timeColor = remainingSeconds > 300 ? "<green>" : remainingSeconds > 60 ? "<yellow>" : "<red>";

        // Calculate next perk countdown — skip already-fired milestones, use wall-clock elapsed
        int elapsed = gameManager.getActiveElapsedSeconds();
        Component nextPerkLine = null;
        for (int milestone : new int[] { 300, 600, 900 }) {
            if (gameManager.hasFiredTimedMilestone(milestone)) continue;
            if (elapsed < milestone) {
                int untilNext = milestone - elapsed;
                nextPerkLine = MM.parse("<white>  Next Perk: <green>" + formatTime(untilNext) + " <gray>(?)");
                break;
            }
        }

        List<Perk> perks = perkManager.getPlayerPerks(uuid);

        // Build lines
        List<Component> lines = new ArrayList<>();
        lines.add(SEPARATOR);

        // Perks section
        for (Perk perk : perks) {
            lines.add(Component.text(" ✦ " + perk.getDisplayName(), perk.getTier().getTextColor()));
        }

        lines.add(Component.empty());
        lines.add(MM.parse("<green>  Perks: <white>" + perkManager.getPlayerPerkCount(uuid) + "/" + perkManager.getMaxPerks()));
        lines.add(MM.parse("<gold>  Gold: <white>" + economyManager.getGold(uuid)));
        lines.add(MM.parse("<white>  Phase: " + (dayNightManager.isNight() ? "<dark_purple>☾ Night" : "<yellow>☀ Day")));

        lines.add(Component.empty());
        if (nextPerkLine != null) {
            lines.add(nextPerkLine);
        }
        lines.add(MM.parse("<white>  Time Left: " + timeColor + timeStr));
        lines.add(SEPARATOR);
        lines.add(MM.parse("<blue>  Humans: <white>" + humansAlive));
        lines.add(MM.parse("<red>  Vampires: <white>" + vampires));
        lines.add(SEPARATOR);

        board.updateLines(lines);
    }

    public void updateAllGameScoreboards(GameManager gameManager, EconomyManager economyManager, PerkManager perkManager, DayNightManager dayNightManager) {
        for (Player player : gameManager.getJoinedOnlinePlayers()) {
            if (boards.containsKey(player.getUniqueId())) {
                updateGameScoreboard(player, gameManager, economyManager, perkManager, dayNightManager);
            }
        }
    }

    public void updateScoreboardTeams(Player player, GameManager gameManager) {
        syncTeams(gameManager);
    }

    public void updateTeamChangeOnAllScoreboards(Player player, GameManager gameManager) {
        syncTeams(gameManager);
    }

    private void syncTeams(GameManager gameManager) {
        if (teamScoreboard == null) return;

        Team humanTeam = teamScoreboard.getTeam("humans");
        Team vampTeam = teamScoreboard.getTeam("vampires");
        if (humanTeam == null || vampTeam == null) return;

        for (UUID uuid : gameManager.getHumanTeam()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                vampTeam.removeEntry(p.getName());
                humanTeam.addEntry(p.getName());
            }
        }
        for (UUID uuid : gameManager.getVampireTeam()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                humanTeam.removeEntry(p.getName());
                vampTeam.addEntry(p.getName());
            }
        }
    }

    private String formatTime(int totalSeconds) {
        int min = totalSeconds / 60;
        int sec = totalSeconds % 60;
        return String.format("%d:%02d", min, sec);
    }

    public void removePlayer(UUID uuid) {
        FastBoard board = boards.remove(uuid);
        if (board != null && !board.isDeleted()) {
            board.delete();
        }
    }

    public void resetAll() {
        for (FastBoard board : boards.values()) {
            if (!board.isDeleted()) {
                board.delete();
            }
        }
        boards.clear();
    }
}

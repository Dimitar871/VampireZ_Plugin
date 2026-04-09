package com.vampirez;

import fr.mrmicky.fastboard.FastBoard;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;

public class ScoreboardManager {

    private Scoreboard teamScoreboard;
    private final Map<UUID, FastBoard> boards = new HashMap<>();

    private static final String TITLE = ChatColor.DARK_RED + "" + ChatColor.BOLD + "VAMPIREZ";
    private static final String SEPARATOR = ChatColor.DARK_GRAY + "━━━━━━━━━━━━━━━━━";

    public void setupLobbyScoreboard() {
        teamScoreboard = Bukkit.getScoreboardManager().getNewScoreboard();

        Team humanTeam = teamScoreboard.registerNewTeam("humans");
        humanTeam.setColor(ChatColor.BLUE);
        humanTeam.setAllowFriendlyFire(false);
        humanTeam.setPrefix(ChatColor.BLUE + "");

        Team vampTeam = teamScoreboard.registerNewTeam("vampires");
        vampTeam.setColor(ChatColor.RED);
        vampTeam.setAllowFriendlyFire(false);
        vampTeam.setPrefix(ChatColor.RED + "");
    }

    public void updateLobbyScoreboard(int playerCount, int minPlayers) {
        if (teamScoreboard == null) setupLobbyScoreboard();

        for (FastBoard board : boards.values()) {
            if (!board.isDeleted()) {
                List<String> lines = new ArrayList<>();
                lines.add(SEPARATOR);
                lines.add("");
                lines.add(ChatColor.GRAY + "       \u25B6 LOBBY \u25C0");
                lines.add("");
                lines.add(ChatColor.WHITE + "  Players: " + ChatColor.GREEN + playerCount + ChatColor.GRAY + "/" + ChatColor.WHITE + "100");
                lines.add(ChatColor.WHITE + "  Min to start: " + ChatColor.YELLOW + minPlayers);
                lines.add("");
                if (playerCount < minPlayers) {
                    lines.add(ChatColor.YELLOW + "  Waiting for players...");
                } else {
                    lines.add(ChatColor.GREEN + "  \u2714 Ready to start!");
                }
                lines.add("");
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

        ChatColor timeColor;
        if (remainingSeconds > 300) timeColor = ChatColor.GREEN;
        else if (remainingSeconds > 60) timeColor = ChatColor.YELLOW;
        else timeColor = ChatColor.RED;

        // Calculate next perk countdown
        int gameDuration = gameManager.getGameDurationSeconds();
        int elapsed = gameDuration - remainingSeconds;
        String nextPerkLine = null;
        if (elapsed < 300) {
            int untilNext = 300 - elapsed;
            nextPerkLine = ChatColor.WHITE + "  Next Perk: " + ChatColor.GREEN + formatTime(untilNext) + " " + ChatColor.GRAY + "(?)";
        } else if (elapsed < 600) {
            int untilNext = 600 - elapsed;
            nextPerkLine = ChatColor.WHITE + "  Next Perk: " + ChatColor.GREEN + formatTime(untilNext) + " " + ChatColor.GRAY + "(?)";
        } else if (elapsed < 900) {
            int untilNext = 900 - elapsed;
            nextPerkLine = ChatColor.WHITE + "  Next Perk: " + ChatColor.GREEN + formatTime(untilNext) + " " + ChatColor.GRAY + "(?)";
        }

        List<Perk> perks = perkManager.getPlayerPerks(uuid);

        // Build lines
        List<String> lines = new ArrayList<>();
        lines.add(SEPARATOR);

        // Perks section
        for (Perk perk : perks) {
            lines.add(perk.getTier().getColor() + " \u2726 " + perk.getDisplayName());
        }

        lines.add("");
        lines.add(ChatColor.GREEN + "  Perks: " + ChatColor.WHITE + perkManager.getPlayerPerkCount(uuid) + "/" + perkManager.getMaxPerks());
        lines.add(ChatColor.GOLD + "  Gold: " + ChatColor.WHITE + economyManager.getGold(uuid));

        String phaseIcon = dayNightManager.isNight() ? "\u263E" : "\u2600";
        String phaseText = dayNightManager.isNight()
                ? (ChatColor.DARK_PURPLE + phaseIcon + " Night")
                : (ChatColor.YELLOW + phaseIcon + " Day");
        lines.add(ChatColor.WHITE + "  Phase: " + phaseText);

        lines.add("");
        if (nextPerkLine != null) {
            lines.add(nextPerkLine);
        }
        lines.add(ChatColor.WHITE + "  Time Left: " + timeColor + timeStr);
        lines.add(SEPARATOR);
        lines.add(ChatColor.BLUE + "  Humans: " + ChatColor.WHITE + humansAlive);
        lines.add(ChatColor.RED + "  Vampires: " + ChatColor.WHITE + vampires);
        lines.add(SEPARATOR);

        board.updateLines(lines);
    }

    public void updateAllGameScoreboards(GameManager gameManager, EconomyManager economyManager, PerkManager perkManager, DayNightManager dayNightManager) {
        for (Player player : Bukkit.getOnlinePlayers()) {
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

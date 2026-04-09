package com.vampirez;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class GameCommands implements CommandExecutor {

    private final GameManager gameManager;
    private final PerkShopGUI perkShopGUI;
    private final PerkTestGUI perkTestGUI;

    public GameCommands(GameManager gameManager, PerkShopGUI perkShopGUI, PerkTestGUI perkTestGUI) {
        this.gameManager = gameManager;
        this.perkShopGUI = perkShopGUI;
        this.perkTestGUI = perkTestGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help" -> sendHelp(player);
            case "start" -> handleStart(player, false);
            case "forcestart" -> handleStart(player, true);
            case "stop" -> handleStop(player);
            case "shop" -> handleShop(player);
            case "perks" -> handlePerks(player);
            case "gold" -> handleGold(player);
            case "status" -> handleStatus(player);
            case "setlobby" -> handleSetSpawn(player, "lobby");
            case "sethumanspawn" -> handleSetSpawn(player, "human");
            case "setvampspawn" -> handleSetSpawn(player, "vampire");
            case "test" -> handleTest(player);
            case "arena" -> handleArena(player);
            case "reload" -> handleReload(player);
            default -> {
                player.sendMessage(ChatColor.RED + "Unknown subcommand. Use /vz help");
            }
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.DARK_RED + "=== VampireZ Commands ===");
        player.sendMessage(ChatColor.GOLD + "/vz shop" + ChatColor.GRAY + " - Open the perk shop");
        player.sendMessage(ChatColor.GOLD + "/vz perks" + ChatColor.GRAY + " - List your active perks");
        player.sendMessage(ChatColor.GOLD + "/vz gold" + ChatColor.GRAY + " - Show your gold");
        player.sendMessage(ChatColor.GOLD + "/vz status" + ChatColor.GRAY + " - Show game status");
        if (player.hasPermission("vampirez.admin")) {
            player.sendMessage(ChatColor.RED + "/vz start" + ChatColor.GRAY + " - Start the game");
            player.sendMessage(ChatColor.RED + "/vz forcestart" + ChatColor.GRAY + " - Force start");
            player.sendMessage(ChatColor.RED + "/vz stop" + ChatColor.GRAY + " - Stop the game");
            player.sendMessage(ChatColor.RED + "/vz setlobby" + ChatColor.GRAY + " - Set lobby spawn");
            player.sendMessage(ChatColor.RED + "/vz sethumanspawn" + ChatColor.GRAY + " - Set human spawn");
            player.sendMessage(ChatColor.RED + "/vz setvampspawn" + ChatColor.GRAY + " - Set vampire spawn");
            player.sendMessage(ChatColor.RED + "/vz test" + ChatColor.GRAY + " - Open perk test menu");
            player.sendMessage(ChatColor.RED + "/vz reload" + ChatColor.GRAY + " - Reload config");
        }
    }

    private void handleStart(Player player, boolean force) {
        if (!player.hasPermission("vampirez.admin")) {
            player.sendMessage(ChatColor.RED + "No permission!");
            return;
        }
        if (gameManager.getState() != GameState.LOBBY) {
            player.sendMessage(ChatColor.RED + "A game is already running!");
            return;
        }
        if (!gameManager.hasSpawnsSet()) {
            player.sendMessage(ChatColor.RED + "Set human and vampire spawns first!");
            return;
        }
        if (!force && !gameManager.canStart()) {
            player.sendMessage(ChatColor.RED + "Not enough players! Need " + gameManager.getMinPlayers() + ". Use /vz forcestart to override.");
            return;
        }
        gameManager.startGame(force);
        player.sendMessage(ChatColor.GREEN + "Game starting!");
    }

    private void handleStop(Player player) {
        if (!player.hasPermission("vampirez.admin")) {
            player.sendMessage(ChatColor.RED + "No permission!");
            return;
        }
        if (gameManager.getState() == GameState.LOBBY) {
            player.sendMessage(ChatColor.RED + "No game is running!");
            return;
        }
        gameManager.stopGame();
        player.sendMessage(ChatColor.GREEN + "Game stopped!");
    }

    private void handleShop(Player player) {
        if (gameManager.getState() != GameState.ACTIVE) {
            player.sendMessage(ChatColor.RED + "The shop is only available during an active game!");
            return;
        }
        if (!gameManager.isInGame(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You are not in the game!");
            return;
        }
        PerkTeam team = gameManager.isHuman(player.getUniqueId()) ? PerkTeam.HUMAN : PerkTeam.VAMPIRE;
        perkShopGUI.openTierSelection(player, team);
    }

    private void handlePerks(Player player) {
        List<Perk> perks = gameManager.getPerkManager().getPlayerPerks(player.getUniqueId());
        if (perks.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "You have no active perks.");
            return;
        }
        player.sendMessage(ChatColor.GOLD + "=== Your Perks ===");
        for (Perk perk : perks) {
            player.sendMessage(perk.getTier().getColor() + " - " + perk.getDisplayName()
                    + ChatColor.GRAY + " (" + perk.getTier().getDisplayName() + ")");
        }
    }

    private void handleGold(Player player) {
        int gold = gameManager.getEconomyManager().getGold(player.getUniqueId());
        player.sendMessage(ChatColor.GOLD + "Gold: " + ChatColor.WHITE + gold);
    }

    private void handleStatus(Player player) {
        GameState state = gameManager.getState();
        player.sendMessage(ChatColor.DARK_RED + "=== VampireZ Status ===");
        player.sendMessage(ChatColor.GRAY + "State: " + ChatColor.WHITE + state.name());

        if (state == GameState.ACTIVE) {
            int mins = gameManager.getRemainingSeconds() / 60;
            int secs = gameManager.getRemainingSeconds() % 60;
            player.sendMessage(ChatColor.GRAY + "Time Left: " + ChatColor.WHITE + String.format("%02d:%02d", mins, secs));
            player.sendMessage(ChatColor.BLUE + "Humans: " + ChatColor.WHITE + gameManager.getHumanTeam().size());
            player.sendMessage(ChatColor.RED + "Vampires: " + ChatColor.WHITE + gameManager.getVampireTeam().size());

            if (gameManager.isHuman(player.getUniqueId())) {
                player.sendMessage(ChatColor.GRAY + "Team: " + ChatColor.BLUE + "Human");
            } else if (gameManager.isVampire(player.getUniqueId())) {
                player.sendMessage(ChatColor.GRAY + "Team: " + ChatColor.RED + "Vampire");
            } else {
                player.sendMessage(ChatColor.GRAY + "Team: " + ChatColor.GRAY + "Spectator");
            }
        }
    }

    private void handleSetSpawn(Player player, String type) {
        if (!player.hasPermission("vampirez.admin")) {
            player.sendMessage(ChatColor.RED + "No permission!");
            return;
        }

        switch (type) {
            case "lobby" -> {
                gameManager.setLobbySpawn(player.getLocation());
                player.sendMessage(ChatColor.GREEN + "Lobby spawn set!");
            }
            case "human" -> {
                gameManager.setHumanSpawn(player.getLocation());
                player.sendMessage(ChatColor.GREEN + "Human spawn set!");
            }
            case "vampire" -> {
                gameManager.setVampireSpawn(player.getLocation());
                player.sendMessage(ChatColor.GREEN + "Vampire spawn set!");
            }
        }
    }

    private void handleArena(Player player) {
        if (!player.hasPermission("vampirez.admin")) {
            player.sendMessage(ChatColor.RED + "No permission!");
            return;
        }
        ArenaManager arenaManager = gameManager.getArenaManager();
        if (arenaManager == null || arenaManager.getArenaWorld() == null) {
            player.sendMessage(ChatColor.RED + "Arena world is not loaded!");
            return;
        }
        org.bukkit.World arenaWorld = arenaManager.getArenaWorld();
        player.teleport(arenaWorld.getSpawnLocation());
        player.sendMessage(ChatColor.GREEN + "Teleported to arena world: " + arenaWorld.getName());
    }

    private void handleTest(Player player) {
        if (!player.hasPermission("vampirez.admin")) {
            player.sendMessage(ChatColor.RED + "No permission!");
            return;
        }
        perkTestGUI.openTestMenu(player, 0);
    }

    private void handleReload(Player player) {
        if (!player.hasPermission("vampirez.admin")) {
            player.sendMessage(ChatColor.RED + "No permission!");
            return;
        }
        // Reload is not safe during active game
        if (gameManager.getState() != GameState.LOBBY) {
            player.sendMessage(ChatColor.RED + "Cannot reload during an active game!");
            return;
        }
        player.sendMessage(ChatColor.GREEN + "Config reloaded! (Restart server for full effect)");
    }
}

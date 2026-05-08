package com.vampirez;

import com.vampirez.api.VampireZAPI;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GameCommands implements CommandExecutor, TabCompleter {

    private final GameManager gameManager;
    private final PerkShopGUI perkShopGUI;
    private final PerkTestGUI perkTestGUI;
    private final VampireZAPI api;
    private LeaderboardGUI leaderboardGUI;

    public GameCommands(GameManager gameManager, PerkShopGUI perkShopGUI, PerkTestGUI perkTestGUI, VampireZAPI api) {
        this.gameManager = gameManager;
        this.perkShopGUI = perkShopGUI;
        this.perkTestGUI = perkTestGUI;
        this.api = api;
    }

    public void setLeaderboardGUI(LeaderboardGUI gui) { this.leaderboardGUI = gui; }

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
            case "join" -> gameManager.joinGame(player);
            case "leave" -> gameManager.leaveGame(player);
            case "start" -> handleStart(player, false);
            case "forcestart" -> handleStart(player, true);
            case "stop" -> handleStop(player);
            case "shop" -> handleShop(player);
            case "perks" -> handlePerks(player, args);
            case "gold" -> handleGold(player);
            case "status" -> handleStatus(player);
            case "setlobby" -> handleSetSpawn(player, "lobby");
            case "sethumanspawn" -> handleSetSpawn(player, "human");
            case "setvampspawn" -> handleSetSpawn(player, "vampire");
            case "test" -> handleTest(player);
            case "tools" -> handleTools(player);
            case "debugdmg" -> handleDebugDmg(player);
            case "announce" -> handleAnnounce(player);
            case "arena" -> handleArena(player);
            case "reload" -> handleReload(player);
            case "giveperk" -> handleGivePerk(player, args);
            case "removeperk" -> handleRemovePerk(player, args);
            case "forceconvert" -> handleForceConvert(player, args);
            case "settime" -> handleSetTime(player, args);
            case "setphase" -> handleSetPhase(player, args);
            case "setgold" -> handleSetGold(player, args);
            case "addgold" -> handleAddGold(player, args);
            case "apitest" -> handleApiTest(player);
            case "leaderboard", "lb" -> handleLeaderboard(player);
            case "disableperk" -> handleDisablePerk(player, args);
            case "enableperk" -> handleEnablePerk(player, args);
            default -> {
                player.sendMessage(ChatColor.RED + "Unknown subcommand. Use /vz help");
            }
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.DARK_RED + "=== VampireZ Commands ===");
        player.sendMessage(ChatColor.GOLD + "/vz join" + ChatColor.GRAY + " - Join the VampireZ game");
        player.sendMessage(ChatColor.GOLD + "/vz leave" + ChatColor.GRAY + " - Leave and restore inventory");
        player.sendMessage(ChatColor.GOLD + "/vz shop" + ChatColor.GRAY + " - Open the perk shop");
        player.sendMessage(ChatColor.GOLD + "/vz perks [player]" + ChatColor.GRAY + " - List your perks (admins: any player's perks)");
        player.sendMessage(ChatColor.GOLD + "/vz gold" + ChatColor.GRAY + " - Show your gold");
        player.sendMessage(ChatColor.GOLD + "/vz status" + ChatColor.GRAY + " - Show game status");
        player.sendMessage(ChatColor.GOLD + "/vz leaderboard" + ChatColor.GRAY + " - Open the player leaderboard");
        if (player.hasPermission("vampirez.admin")) {
            player.sendMessage(ChatColor.RED + "/vz start" + ChatColor.GRAY + " - Start the game");
            player.sendMessage(ChatColor.RED + "/vz forcestart" + ChatColor.GRAY + " - Force start");
            player.sendMessage(ChatColor.RED + "/vz stop" + ChatColor.GRAY + " - Stop the game");
            player.sendMessage(ChatColor.RED + "/vz setlobby" + ChatColor.GRAY + " - Set lobby spawn");
            player.sendMessage(ChatColor.RED + "/vz sethumanspawn" + ChatColor.GRAY + " - Set human spawn");
            player.sendMessage(ChatColor.RED + "/vz setvampspawn" + ChatColor.GRAY + " - Set vampire spawn");
            player.sendMessage(ChatColor.RED + "/vz announce" + ChatColor.GRAY + " - Announce event to server");
            player.sendMessage(ChatColor.RED + "/vz test" + ChatColor.GRAY + " - Open perk test menu");
            player.sendMessage(ChatColor.RED + "/vz tools" + ChatColor.GRAY + " - Get debug-dummy books");
            player.sendMessage(ChatColor.RED + "/vz debugdmg" + ChatColor.GRAY + " - Toggle per-hit damage readout");
            player.sendMessage(ChatColor.RED + "/vz reload" + ChatColor.GRAY + " - Reload config");
            player.sendMessage(ChatColor.RED + "/vz giveperk <player> <perkId>" + ChatColor.GRAY + " - Grant a perk");
            player.sendMessage(ChatColor.RED + "/vz removeperk <player> <perkId>" + ChatColor.GRAY + " - Remove a perk");
            player.sendMessage(ChatColor.RED + "/vz forceconvert <player>" + ChatColor.GRAY + " - Force-convert a human");
            player.sendMessage(ChatColor.RED + "/vz settime <seconds>" + ChatColor.GRAY + " - Override game timer");
            player.sendMessage(ChatColor.RED + "/vz setphase day|night" + ChatColor.GRAY + " - Force day/night");
            player.sendMessage(ChatColor.RED + "/vz setgold <player> <amount>" + ChatColor.GRAY + " - Set gold");
            player.sendMessage(ChatColor.RED + "/vz addgold <player> <amount>" + ChatColor.GRAY + " - Add gold");
            player.sendMessage(ChatColor.RED + "/vz apitest" + ChatColor.GRAY + " - Run API self-test");
            player.sendMessage(ChatColor.RED + "/vz disableperk <perkId>" + ChatColor.GRAY + " - Disable a perk (saves to config)");
            player.sendMessage(ChatColor.RED + "/vz enableperk <perkId>" + ChatColor.GRAY + " - Enable a disabled perk (saves to config)");
        }
    }

    // ===== API-backed admin commands =====

    private boolean checkAdmin(Player player) {
        if (!player.hasPermission("vampirez.admin")) {
            player.sendMessage(ChatColor.RED + "No permission!");
            return false;
        }
        return true;
    }

    private Player resolvePlayer(Player sender, String name) {
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + name);
            return null;
        }
        return target;
    }

    private void handleGivePerk(Player player, String[] args) {
        if (!checkAdmin(player)) return;
        if (args.length < 3) { player.sendMessage(ChatColor.RED + "Usage: /vz giveperk <player> <perkId>"); return; }
        Player target = resolvePlayer(player, args[1]);
        if (target == null) return;
        String perkId = args[2];
        if (!api.getAvailablePerkIds().contains(perkId)) {
            player.sendMessage(ChatColor.RED + "Unknown perk id: " + perkId);
            return;
        }
        boolean ok = api.givePerk(target.getUniqueId(), perkId);
        player.sendMessage(ok
                ? ChatColor.GREEN + "Gave perk " + perkId + " to " + target.getName()
                : ChatColor.RED + "Failed to give perk (max reached, cancelled, or unknown id)");
    }

    private void handleRemovePerk(Player player, String[] args) {
        if (!checkAdmin(player)) return;
        if (args.length < 3) { player.sendMessage(ChatColor.RED + "Usage: /vz removeperk <player> <perkId>"); return; }
        Player target = resolvePlayer(player, args[1]);
        if (target == null) return;
        String perkId = args[2];
        boolean ok = api.removePerk(target.getUniqueId(), perkId);
        player.sendMessage(ok
                ? ChatColor.GREEN + "Removed perk " + perkId + " from " + target.getName()
                : ChatColor.RED + "Player did not have that perk");
    }

    private void handleForceConvert(Player player, String[] args) {
        if (!checkAdmin(player)) return;
        if (args.length < 2) { player.sendMessage(ChatColor.RED + "Usage: /vz forceconvert <player>"); return; }
        Player target = resolvePlayer(player, args[1]);
        if (target == null) return;
        boolean ok = api.forceConvert(target.getUniqueId());
        player.sendMessage(ok
                ? ChatColor.GREEN + "Converted " + target.getName() + " to vampire"
                : ChatColor.RED + target.getName() + " is not a human");
    }

    private void handleSetTime(Player player, String[] args) {
        if (!checkAdmin(player)) return;
        if (args.length < 2) { player.sendMessage(ChatColor.RED + "Usage: /vz settime <seconds>"); return; }
        if (api.getState() != GameState.ACTIVE) { player.sendMessage(ChatColor.RED + "Game is not active!"); return; }
        try {
            int seconds = Integer.parseInt(args[1]);
            api.setRemainingSeconds(seconds);
            player.sendMessage(ChatColor.GREEN + "Game time set to " + api.getRemainingSeconds() + "s");
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Not a number: " + args[1]);
        }
    }

    private void handleSetPhase(Player player, String[] args) {
        if (!checkAdmin(player)) return;
        if (args.length < 2) { player.sendMessage(ChatColor.RED + "Usage: /vz setphase day|night"); return; }
        switch (args[1].toLowerCase()) {
            case "day" -> { api.forceDay(); player.sendMessage(ChatColor.GREEN + "Forced day phase"); }
            case "night" -> { api.forceNight(); player.sendMessage(ChatColor.GREEN + "Forced night phase"); }
            default -> player.sendMessage(ChatColor.RED + "Phase must be 'day' or 'night'");
        }
    }

    private void handleSetGold(Player player, String[] args) {
        if (!checkAdmin(player)) return;
        if (args.length < 3) { player.sendMessage(ChatColor.RED + "Usage: /vz setgold <player> <amount>"); return; }
        Player target = resolvePlayer(player, args[1]);
        if (target == null) return;
        try {
            int amount = Integer.parseInt(args[2]);
            api.setGold(target.getUniqueId(), amount);
            player.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s gold to " + api.getGold(target.getUniqueId()));
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Not a number: " + args[2]);
        }
    }

    private void handleAddGold(Player player, String[] args) {
        if (!checkAdmin(player)) return;
        if (args.length < 3) { player.sendMessage(ChatColor.RED + "Usage: /vz addgold <player> <amount>"); return; }
        Player target = resolvePlayer(player, args[1]);
        if (target == null) return;
        try {
            int amount = Integer.parseInt(args[2]);
            api.addGold(target.getUniqueId(), amount);
            player.sendMessage(ChatColor.GREEN + "Added " + amount + " gold to " + target.getName() + " (total: " + api.getGold(target.getUniqueId()) + ")");
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Not a number: " + args[2]);
        }
    }

    private void handleApiTest(Player player) {
        if (!checkAdmin(player)) return;
        player.sendMessage(ChatColor.GOLD + "=== VampireZAPI self-test ===");

        Object reg = Bukkit.getServicesManager().getRegistration(VampireZAPI.class);
        boolean svcRegistered = reg != null;
        player.sendMessage(ChatColor.GRAY + "ServicesManager registration: " + (svcRegistered ? ChatColor.GREEN + "OK" : ChatColor.RED + "MISSING"));

        boolean staticOk = VampireZPlugin.getAPI() != null;
        player.sendMessage(ChatColor.GRAY + "Static accessor: " + (staticOk ? ChatColor.GREEN + "OK" : ChatColor.RED + "NULL"));

        player.sendMessage(ChatColor.GRAY + "State: " + ChatColor.WHITE + api.getState());
        player.sendMessage(ChatColor.GRAY + "Available perks: " + ChatColor.WHITE + api.getAvailablePerkIds().size());
        player.sendMessage(ChatColor.GRAY + "Day/night enabled: " + ChatColor.WHITE + api.isDayNightEnabled() + ", isNight: " + api.isNight());

        UUID uuid = player.getUniqueId();
        if (api.getState() == GameState.ACTIVE && api.isInGame(uuid)) {
            boolean hadDeft = api.hasPerk(uuid, "deft");
            if (!hadDeft) {
                boolean given = api.givePerk(uuid, "deft");
                boolean nowHas = api.hasPerk(uuid, "deft");
                player.sendMessage(ChatColor.GRAY + "givePerk(deft): " + (given && nowHas ? ChatColor.GREEN + "OK" : ChatColor.RED + "FAILED"));
                if (given) api.removePerk(uuid, "deft");
            } else {
                player.sendMessage(ChatColor.GRAY + "Skipped givePerk test (already have deft)");
            }
        } else {
            player.sendMessage(ChatColor.GRAY + "Skipped givePerk test (game not active or sender not playing)");
        }

        getLogger().info("[VampireZ] /vz apitest run by " + player.getName() + " — services=" + svcRegistered + " static=" + staticOk);
    }

    private java.util.logging.Logger getLogger() {
        return Bukkit.getLogger();
    }

    // ===== Tab completion =====

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("vampirez.admin")) return List.of();

        if (args.length == 1) {
            return filter(args[0], List.of(
                    "help", "join", "leave", "shop", "perks", "gold", "status",
                    "start", "forcestart", "stop", "setlobby", "sethumanspawn", "setvampspawn",
                    "test", "tools", "debugdmg", "announce", "arena", "reload",
                    "giveperk", "removeperk", "forceconvert", "settime", "setphase",
                    "setgold", "addgold", "apitest", "leaderboard", "lb",
                    "disableperk", "enableperk"
            ));
        }

        String sub = args[0].toLowerCase();

        if (args.length == 2) {
            switch (sub) {
                case "giveperk", "removeperk", "forceconvert", "setgold", "addgold" -> {
                    return filter(args[1], onlinePlayerNames());
                }
                case "perks" -> {
                    if (sender.hasPermission("vampirez.admin")) return filter(args[1], onlinePlayerNames());
                    return List.of();
                }
                case "settime" -> { return filter(args[1], List.of("60", "300", "600", "900", "1500")); }
                case "setphase" -> { return filter(args[1], List.of("day", "night")); }
                case "disableperk" -> {
                    // suggest only currently enabled perks
                    List<String> enabled = api.getAvailablePerkIds().stream()
                            .filter(id -> !gameManager.getPerkManager().isDisabled(id))
                            .collect(java.util.stream.Collectors.toList());
                    return filter(args[1], enabled);
                }
                case "enableperk" -> {
                    // suggest only currently disabled perks
                    List<String> disabled = api.getAvailablePerkIds().stream()
                            .filter(id -> gameManager.getPerkManager().isDisabled(id))
                            .collect(java.util.stream.Collectors.toList());
                    return filter(args[1], disabled);
                }
            }
        }

        if (args.length == 3) {
            switch (sub) {
                case "giveperk" -> { return filter(args[2], new ArrayList<>(api.getAvailablePerkIds())); }
                case "removeperk" -> {
                    Player target = Bukkit.getPlayerExact(args[1]);
                    if (target == null) return List.of();
                    return filter(args[2], api.getPlayerPerkIds(target.getUniqueId()));
                }
                case "setgold", "addgold" -> { return filter(args[2], List.of("100", "500", "1000")); }
            }
        }

        return List.of();
    }

    private List<String> onlinePlayerNames() {
        List<String> names = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
        return names;
    }

    private List<String> filter(String prefix, List<String> options) {
        String lower = prefix.toLowerCase();
        List<String> out = new ArrayList<>();
        for (String s : options) {
            if (s.toLowerCase().startsWith(lower)) out.add(s);
        }
        return out;
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

    private void handlePerks(Player player, String[] args) {
        UUID targetUuid = player.getUniqueId();
        String headerName = "Your";

        if (args.length >= 2) {
            if (!player.hasPermission("vampirez.admin")) {
                player.sendMessage(ChatColor.RED + "Only admins can check other players' perks.");
                return;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                player.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
                return;
            }
            targetUuid = target.getUniqueId();
            headerName = target.getName() + "'s";
        }

        List<Perk> perks = gameManager.getPerkManager().getPlayerPerks(targetUuid);
        if (perks.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + headerName + " has no active perks.");
            return;
        }
        player.sendMessage(ChatColor.GOLD + "=== " + headerName + " Perks ===");
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

    private void handleAnnounce(Player player) {
        if (!player.hasPermission("vampirez.admin")) {
            player.sendMessage(ChatColor.RED + "No permission!");
            return;
        }
        if (gameManager.getState() != GameState.LOBBY) {
            player.sendMessage(ChatColor.RED + "Can only announce during lobby phase!");
            return;
        }

        // Build clickable chat message
        TextComponent separator = new TextComponent("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        separator.setColor(net.md_5.bungee.api.ChatColor.DARK_RED);

        TextComponent header = new TextComponent("  VAMPIREZ EVENT STARTING!");
        header.setColor(net.md_5.bungee.api.ChatColor.DARK_RED);
        header.setBold(true);

        TextComponent desc = new TextComponent("  Humans vs Vampires - Survive 25 minutes!");
        desc.setColor(net.md_5.bungee.api.ChatColor.GOLD);

        TextComponent joinButton = new TextComponent("  >>> CLICK HERE TO JOIN <<<");
        joinButton.setColor(net.md_5.bungee.api.ChatColor.GREEN);
        joinButton.setBold(true);
        joinButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/vampirez join"));
        joinButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new Text(net.md_5.bungee.api.ChatColor.YELLOW + "Click to join VampireZ!")));

        TextComponent orType = new TextComponent("  Or type: /vz join");
        orType.setColor(net.md_5.bungee.api.ChatColor.GRAY);
        orType.setItalic(true);

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.spigot().sendMessage(separator);
            p.sendMessage("");
            p.spigot().sendMessage(header);
            p.spigot().sendMessage(desc);
            p.sendMessage("");
            p.spigot().sendMessage(joinButton);
            p.spigot().sendMessage(orType);
            p.sendMessage("");
            p.spigot().sendMessage(separator);
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.0f);
        }

        player.sendMessage(ChatColor.GREEN + "Event announced to all players!");
    }

    private void handleArena(Player player) {
        if (!player.hasPermission("vampirez.admin")) {
            player.sendMessage(ChatColor.RED + "No permission!");
            return;
        }
        if (gameManager.getLobbySpawn() != null) {
            player.teleport(gameManager.getLobbySpawn());
        } else {
            // First time setup — lobby not set yet, teleport to arena world spawn
            ArenaManager arenaManager = gameManager.getArenaManager();
            if (arenaManager == null || arenaManager.getArenaWorld() == null) {
                player.sendMessage(ChatColor.RED + "Arena world is not loaded!");
                return;
            }
            player.teleport(arenaManager.getArenaWorld().getSpawnLocation());
        }
        player.setGameMode(org.bukkit.GameMode.ADVENTURE);
        player.sendMessage(ChatColor.GREEN + "Teleported to the arena.");
        player.sendMessage(ChatColor.GRAY + "Use /vz setlobby, /vz sethumanspawn, /vz setvampspawn to configure.");
    }

    private void handleTest(Player player) {
        if (!player.hasPermission("vampirez.admin")) {
            player.sendMessage(ChatColor.RED + "No permission!");
            return;
        }
        perkTestGUI.openTestMenu(player, 0);
    }

    private void handleTools(Player player) {
        if (!player.hasPermission("vampirez.admin")) {
            player.sendMessage(ChatColor.RED + "No permission!");
            return;
        }
        int added = DebugBookManager.giveBooks(player);
        if (added == 0) {
            player.sendMessage(ChatColor.YELLOW + "You already have all debug books.");
        } else {
            player.sendMessage(ChatColor.GREEN + "Added " + added + " debug book(s) to your inventory.");
        }
    }

    private void handleDebugDmg(Player player) {
        if (!player.hasPermission("vampirez.admin")) {
            player.sendMessage(ChatColor.RED + "No permission!");
            return;
        }
        boolean enabled = DebugBookManager.toggleDamageDebug(player);
        if (enabled) {
            player.sendMessage(ChatColor.GREEN + "Damage readout ON. Every hit you deal will print damage + target HP in your action bar.");
        } else {
            player.sendMessage(ChatColor.YELLOW + "Damage readout OFF.");
        }
    }

    private void handleReload(Player player) {
        if (!player.hasPermission("vampirez.admin")) {
            player.sendMessage(ChatColor.RED + "No permission!");
            return;
        }
        if (gameManager.getState() != GameState.LOBBY) {
            player.sendMessage(ChatColor.RED + "Cannot reload during an active game!");
            return;
        }
        gameManager.reloadAllConfig();
        player.sendMessage(ChatColor.GREEN + "Config reloaded! Updated: game settings, economy, day/night, disabled perks, perk limits.");
    }

    private void handleLeaderboard(Player player) {
        if (leaderboardGUI == null) {
            player.sendMessage(ChatColor.RED + "Leaderboard is not available.");
            return;
        }
        leaderboardGUI.open(player);
    }

    private void handleDisablePerk(Player player, String[] args) {
        if (!checkAdmin(player)) return;
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /vz disableperk <perkId>");
            return;
        }
        String perkId = args[1].toLowerCase();
        if (gameManager.getPerkManager().getPerkById(perkId) == null) {
            player.sendMessage(ChatColor.RED + "Unknown perk: " + perkId);
            return;
        }
        if (gameManager.getPerkManager().isDisabled(perkId)) {
            player.sendMessage(ChatColor.YELLOW + perkId + " is already disabled.");
            return;
        }
        // Update config and apply immediately
        List<String> list = new ArrayList<>(gameManager.getPlugin().getConfig().getStringList("perks.disabled-perks"));
        list.add(perkId);
        gameManager.getPlugin().getConfig().set("perks.disabled-perks", list);
        gameManager.getPlugin().saveConfig();
        gameManager.getPerkManager().setDisabledPerks(list);
        player.sendMessage(ChatColor.RED + "⊘ " + ChatColor.BOLD + perkId + ChatColor.RESET + ChatColor.RED + " disabled. Will not appear in perk selections.");
    }

    private void handleEnablePerk(Player player, String[] args) {
        if (!checkAdmin(player)) return;
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /vz enableperk <perkId>");
            return;
        }
        String perkId = args[1].toLowerCase();
        if (!gameManager.getPerkManager().isDisabled(perkId)) {
            player.sendMessage(ChatColor.YELLOW + perkId + " is not currently disabled.");
            return;
        }
        List<String> list = new ArrayList<>(gameManager.getPlugin().getConfig().getStringList("perks.disabled-perks"));
        list.remove(perkId);
        gameManager.getPlugin().getConfig().set("perks.disabled-perks", list);
        gameManager.getPlugin().saveConfig();
        gameManager.getPerkManager().setDisabledPerks(list);
        player.sendMessage(ChatColor.GREEN + "✔ " + ChatColor.BOLD + perkId + ChatColor.RESET + ChatColor.GREEN + " enabled and back in the perk pool.");
    }
}

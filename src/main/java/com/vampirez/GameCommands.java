package com.vampirez;

import com.vampirez.api.VampireZAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
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
    private final LeaderboardGUI leaderboardGUI;

    public GameCommands(GameManager gameManager, PerkShopGUI perkShopGUI, PerkTestGUI perkTestGUI,
                        VampireZAPI api, LeaderboardGUI leaderboardGUI) {
        this.gameManager = gameManager;
        this.perkShopGUI = perkShopGUI;
        this.perkTestGUI = perkTestGUI;
        this.api = api;
        this.leaderboardGUI = leaderboardGUI;
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
            default -> player.sendMessage(MM.parse("<red>Unknown subcommand. Use /vz help"));
        }
        return true;
    }

    void sendHelp(Player player) {
        player.sendMessage(MM.parse("<dark_red>=== VampireZ Commands ==="));
        player.sendMessage(MM.parse("<gold>/vz join<gray> - Join the VampireZ game"));
        player.sendMessage(MM.parse("<gold>/vz leave<gray> - Leave and restore inventory"));
        player.sendMessage(MM.parse("<gold>/vz shop<gray> - Open the perk shop"));
        player.sendMessage(MM.parse("<gold>/vz perks [player]<gray> - List your perks (admins: any player's perks)"));
        player.sendMessage(MM.parse("<gold>/vz gold<gray> - Show your gold"));
        player.sendMessage(MM.parse("<gold>/vz status<gray> - Show game status"));
        player.sendMessage(MM.parse("<gold>/vz leaderboard<gray> - Open the player leaderboard"));
        if (player.hasPermission("vampirez.admin")) {
            player.sendMessage(MM.parse("<red>/vz start<gray> - Start the game"));
            player.sendMessage(MM.parse("<red>/vz forcestart<gray> - Force start"));
            player.sendMessage(MM.parse("<red>/vz stop<gray> - Stop the game"));
            player.sendMessage(MM.parse("<red>/vz setlobby<gray> - Set lobby spawn"));
            player.sendMessage(MM.parse("<red>/vz sethumanspawn<gray> - Set human spawn"));
            player.sendMessage(MM.parse("<red>/vz setvampspawn<gray> - Set vampire spawn"));
            player.sendMessage(MM.parse("<red>/vz announce<gray> - Announce event to server"));
            player.sendMessage(MM.parse("<red>/vz test<gray> - Open perk test menu"));
            player.sendMessage(MM.parse("<red>/vz tools<gray> - Get debug-dummy books"));
            player.sendMessage(MM.parse("<red>/vz debugdmg<gray> - Toggle per-hit damage readout"));
            player.sendMessage(MM.parse("<red>/vz reload<gray> - Reload config"));
            player.sendMessage(MM.parse("<red>/vz giveperk <player> <perkId><gray> - Grant a perk"));
            player.sendMessage(MM.parse("<red>/vz removeperk <player> <perkId><gray> - Remove a perk"));
            player.sendMessage(MM.parse("<red>/vz forceconvert <player><gray> - Force-convert a human"));
            player.sendMessage(MM.parse("<red>/vz settime <seconds><gray> - Override game timer"));
            player.sendMessage(MM.parse("<red>/vz setphase day|night<gray> - Force day/night"));
            player.sendMessage(MM.parse("<red>/vz setgold <player> <amount><gray> - Set gold"));
            player.sendMessage(MM.parse("<red>/vz addgold <player> <amount><gray> - Add gold"));
            player.sendMessage(MM.parse("<red>/vz apitest<gray> - Run API self-test"));
            player.sendMessage(MM.parse("<red>/vz disableperk <perkId><gray> - Disable a perk (saves to config)"));
            player.sendMessage(MM.parse("<red>/vz enableperk <perkId><gray> - Enable a disabled perk (saves to config)"));
        }
    }

    // ===== API-backed admin commands =====

    private boolean checkAdmin(Player player) {
        if (!player.hasPermission("vampirez.admin")) {
            player.sendMessage(MM.parse("<red>No permission!"));
            return false;
        }
        return true;
    }

    private Player resolvePlayer(Player sender, String name) {
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            sender.sendMessage(MM.parse("<red>Player not found: " + name));
            return null;
        }
        return target;
    }

    void handleGivePerk(Player player, String[] args) {
        if (!checkAdmin(player)) return;
        if (args.length < 3) { player.sendMessage(MM.parse("<red>Usage: /vz giveperk <player> <perkId>")); return; }
        Player target = resolvePlayer(player, args[1]);
        if (target == null) return;
        String perkId = args[2];
        if (!api.getAvailablePerkIds().contains(perkId)) {
            player.sendMessage(MM.parse("<red>Unknown perk id: " + perkId));
            return;
        }
        boolean ok = api.givePerk(target.getUniqueId(), perkId);
        player.sendMessage(ok
                ? MM.parse("<green>Gave perk " + perkId + " to " + target.getName())
                : MM.parse("<red>Failed to give perk (max reached, cancelled, or unknown id)"));
    }

    void handleRemovePerk(Player player, String[] args) {
        if (!checkAdmin(player)) return;
        if (args.length < 3) { player.sendMessage(MM.parse("<red>Usage: /vz removeperk <player> <perkId>")); return; }
        Player target = resolvePlayer(player, args[1]);
        if (target == null) return;
        String perkId = args[2];
        boolean ok = api.removePerk(target.getUniqueId(), perkId);
        player.sendMessage(ok
                ? MM.parse("<green>Removed perk " + perkId + " from " + target.getName())
                : MM.parse("<red>Player did not have that perk"));
    }

    void handleForceConvert(Player player, String[] args) {
        if (!checkAdmin(player)) return;
        if (args.length < 2) { player.sendMessage(MM.parse("<red>Usage: /vz forceconvert <player>")); return; }
        Player target = resolvePlayer(player, args[1]);
        if (target == null) return;
        boolean ok = api.forceConvert(target.getUniqueId());
        player.sendMessage(ok
                ? MM.parse("<green>Converted " + target.getName() + " to vampire")
                : MM.parse("<red>" + target.getName() + " is not a human"));
    }

    void handleSetTime(Player player, String[] args) {
        if (!checkAdmin(player)) return;
        if (args.length < 2) { player.sendMessage(MM.parse("<red>Usage: /vz settime <seconds>")); return; }
        if (api.getState() != GameState.ACTIVE) { player.sendMessage(MM.parse("<red>Game is not active!")); return; }
        try {
            int seconds = Integer.parseInt(args[1]);
            api.setRemainingSeconds(seconds);
            player.sendMessage(MM.parse("<green>Game time set to " + api.getRemainingSeconds() + "s"));
        } catch (NumberFormatException e) {
            player.sendMessage(MM.parse("<red>Not a number: " + args[1]));
        }
    }

    void handleSetPhase(Player player, String[] args) {
        if (!checkAdmin(player)) return;
        if (args.length < 2) { player.sendMessage(MM.parse("<red>Usage: /vz setphase day|night")); return; }
        switch (args[1].toLowerCase()) {
            case "day" -> { api.forceDay(); player.sendMessage(MM.parse("<green>Forced day phase")); }
            case "night" -> { api.forceNight(); player.sendMessage(MM.parse("<green>Forced night phase")); }
            default -> player.sendMessage(MM.parse("<red>Phase must be 'day' or 'night'"));
        }
    }

    void handleSetGold(Player player, String[] args) {
        if (!checkAdmin(player)) return;
        if (args.length < 3) { player.sendMessage(MM.parse("<red>Usage: /vz setgold <player> <amount>")); return; }
        Player target = resolvePlayer(player, args[1]);
        if (target == null) return;
        try {
            int amount = Integer.parseInt(args[2]);
            api.setGold(target.getUniqueId(), amount);
            player.sendMessage(MM.parse("<green>Set " + target.getName() + "'s gold to " + api.getGold(target.getUniqueId())));
        } catch (NumberFormatException e) {
            player.sendMessage(MM.parse("<red>Not a number: " + args[2]));
        }
    }

    void handleAddGold(Player player, String[] args) {
        if (!checkAdmin(player)) return;
        if (args.length < 3) { player.sendMessage(MM.parse("<red>Usage: /vz addgold <player> <amount>")); return; }
        Player target = resolvePlayer(player, args[1]);
        if (target == null) return;
        try {
            int amount = Integer.parseInt(args[2]);
            api.addGold(target.getUniqueId(), amount);
            player.sendMessage(MM.parse("<green>Added " + amount + " gold to " + target.getName() + " (total: " + api.getGold(target.getUniqueId()) + ")"));
        } catch (NumberFormatException e) {
            player.sendMessage(MM.parse("<red>Not a number: " + args[2]));
        }
    }

    void handleApiTest(Player player) {
        if (!checkAdmin(player)) return;
        player.sendMessage(MM.parse("<gold>=== VampireZAPI self-test ==="));

        Object reg = Bukkit.getServicesManager().getRegistration(VampireZAPI.class);
        boolean svcRegistered = reg != null;
        player.sendMessage(MM.parse("<gray>ServicesManager registration: " + (svcRegistered ? "<green>OK" : "<red>MISSING")));

        boolean staticOk = VampireZPlugin.getAPI() != null;
        player.sendMessage(MM.parse("<gray>Static accessor: " + (staticOk ? "<green>OK" : "<red>NULL")));

        player.sendMessage(MM.parse("<gray>State: <white>" + api.getState()));
        player.sendMessage(MM.parse("<gray>Available perks: <white>" + api.getAvailablePerkIds().size()));
        player.sendMessage(MM.parse("<gray>Day/night enabled: <white>" + api.isDayNightEnabled() + ", isNight: " + api.isNight()));

        UUID uuid = player.getUniqueId();
        if (api.getState() == GameState.ACTIVE && api.isInGame(uuid)) {
            boolean hadDeft = api.hasPerk(uuid, "deft");
            if (!hadDeft) {
                boolean given = api.givePerk(uuid, "deft");
                boolean nowHas = api.hasPerk(uuid, "deft");
                player.sendMessage(MM.parse("<gray>givePerk(deft): " + (given && nowHas ? "<green>OK" : "<red>FAILED")));
                if (given) api.removePerk(uuid, "deft");
            } else {
                player.sendMessage(MM.parse("<gray>Skipped givePerk test (already have deft)"));
            }
        } else {
            player.sendMessage(MM.parse("<gray>Skipped givePerk test (game not active or sender not playing)"));
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

    void handleStart(Player player, boolean force) {
        if (!player.hasPermission("vampirez.admin")) {
            player.sendMessage(MM.parse("<red>No permission!"));
            return;
        }
        if (gameManager.getState() != GameState.LOBBY) {
            player.sendMessage(MM.parse("<red>A game is already running!"));
            return;
        }
        if (!gameManager.hasSpawnsSet()) {
            player.sendMessage(MM.parse("<red>Set human and vampire spawns first!"));
            return;
        }
        if (!force && !gameManager.canStart()) {
            player.sendMessage(MM.parse("<red>Not enough players! Need " + gameManager.getMinPlayers() + ". Use /vz forcestart to override."));
            return;
        }
        gameManager.startGame(force);
        player.sendMessage(MM.parse("<green>Game starting!"));
    }

    void handleStop(Player player) {
        if (!player.hasPermission("vampirez.admin")) {
            player.sendMessage(MM.parse("<red>No permission!"));
            return;
        }
        if (gameManager.getState() == GameState.LOBBY) {
            player.sendMessage(MM.parse("<red>No game is running!"));
            return;
        }
        gameManager.stopGame();
        player.sendMessage(MM.parse("<green>Game stopped!"));
    }

    void handleShop(Player player) {
        if (gameManager.getState() != GameState.ACTIVE) {
            player.sendMessage(MM.parse("<red>The shop is only available during an active game!"));
            return;
        }
        if (!gameManager.isInGame(player.getUniqueId())) {
            player.sendMessage(MM.parse("<red>You are not in the game!"));
            return;
        }
        PerkTeam team = gameManager.isHuman(player.getUniqueId()) ? PerkTeam.HUMAN : PerkTeam.VAMPIRE;
        perkShopGUI.openTierSelection(player, team);
    }

    void handlePerks(Player player, String[] args) {
        UUID targetUuid = player.getUniqueId();
        String headerName = "Your";

        if (args.length >= 2) {
            if (!player.hasPermission("vampirez.admin")) {
                player.sendMessage(MM.parse("<red>Only admins can check other players' perks."));
                return;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                player.sendMessage(MM.parse("<red>Player not found: " + args[1]));
                return;
            }
            targetUuid = target.getUniqueId();
            headerName = target.getName() + "'s";
        }

        List<Perk> perks = gameManager.getPerkManager().getPlayerPerks(targetUuid);
        if (perks.isEmpty()) {
            player.sendMessage(MM.parse("<gray>" + headerName + " has no active perks."));
            return;
        }
        player.sendMessage(MM.parse("<gold>=== " + headerName + " Perks ==="));
        for (Perk perk : perks) {
            player.sendMessage(Component.empty()
                    .append(Component.text(" - " + perk.getDisplayName()).color(perk.getTier().getTextColor()))
                    .append(Component.text(" (" + perk.getTier().getDisplayName() + ")").color(NamedTextColor.GRAY)));
        }
    }

    void handleGold(Player player) {
        int gold = gameManager.getEconomyManager().getGold(player.getUniqueId());
        player.sendMessage(MM.parse("<gold>Gold: <white>" + gold));
    }

    void handleStatus(Player player) {
        GameState state = gameManager.getState();
        player.sendMessage(MM.parse("<dark_red>=== VampireZ Status ==="));
        player.sendMessage(MM.parse("<gray>State: <white>" + state.name()));

        if (state == GameState.ACTIVE) {
            int mins = gameManager.getRemainingSeconds() / 60;
            int secs = gameManager.getRemainingSeconds() % 60;
            player.sendMessage(MM.parse("<gray>Time Left: <white>" + String.format("%02d:%02d", mins, secs)));
            player.sendMessage(MM.parse("<blue>Humans: <white>" + gameManager.getHumanTeam().size()));
            player.sendMessage(MM.parse("<red>Vampires: <white>" + gameManager.getVampireTeam().size()));

            if (gameManager.isHuman(player.getUniqueId())) {
                player.sendMessage(MM.parse("<gray>Team: <blue>Human"));
            } else if (gameManager.isVampire(player.getUniqueId())) {
                player.sendMessage(MM.parse("<gray>Team: <red>Vampire"));
            } else {
                player.sendMessage(MM.parse("<gray>Team: Spectator"));
            }
        }
    }

    void handleSetSpawn(Player player, String type) {
        if (!player.hasPermission("vampirez.admin")) {
            player.sendMessage(MM.parse("<red>No permission!"));
            return;
        }

        switch (type) {
            case "lobby" -> {
                gameManager.setLobbySpawn(player.getLocation());
                player.sendMessage(MM.parse("<green>Lobby spawn set!"));
            }
            case "human" -> {
                gameManager.setHumanSpawn(player.getLocation());
                player.sendMessage(MM.parse("<green>Human spawn set!"));
            }
            case "vampire" -> {
                gameManager.setVampireSpawn(player.getLocation());
                player.sendMessage(MM.parse("<green>Vampire spawn set!"));
            }
        }
    }

    void handleAnnounce(Player player) {
        if (!player.hasPermission("vampirez.admin")) {
            player.sendMessage(MM.parse("<red>No permission!"));
            return;
        }
        if (gameManager.getState() != GameState.LOBBY) {
            player.sendMessage(MM.parse("<red>Can only announce during lobby phase!"));
            return;
        }

        Component separator = MM.parse("<dark_red>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Component header    = MM.parse("<dark_red><bold>  VAMPIREZ EVENT STARTING!");
        Component desc      = MM.parse("<gold>  Humans vs Vampires - Survive 25 minutes!");
        Component joinBtn   = Component.text("  >>> CLICK HERE TO JOIN <<<")
                .color(NamedTextColor.GREEN)
                .decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/vampirez join"))
                .hoverEvent(HoverEvent.showText(MM.parse("<yellow>Click to join VampireZ!")));
        Component orType    = MM.parse("<gray><italic>  Or type: /vz join");

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(separator);
            p.sendMessage(Component.empty());
            p.sendMessage(header);
            p.sendMessage(desc);
            p.sendMessage(Component.empty());
            p.sendMessage(joinBtn);
            p.sendMessage(orType);
            p.sendMessage(Component.empty());
            p.sendMessage(separator);
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.0f);
        }

        player.sendMessage(MM.parse("<green>Event announced to all players!"));
    }

    void handleArena(Player player) {
        if (!player.hasPermission("vampirez.admin")) {
            player.sendMessage(MM.parse("<red>No permission!"));
            return;
        }
        if (gameManager.getLobbySpawn() != null) {
            player.teleport(gameManager.getLobbySpawn());
        } else {
            // First time setup — lobby not set yet, teleport to arena world spawn
            ArenaManager arenaManager = gameManager.getArenaManager();
            if (arenaManager == null || arenaManager.getArenaWorld() == null) {
                player.sendMessage(MM.parse("<red>Arena world is not loaded!"));
                return;
            }
            player.teleport(arenaManager.getArenaWorld().getSpawnLocation());
        }
        player.setGameMode(org.bukkit.GameMode.ADVENTURE);
        player.sendMessage(MM.parse("<green>Teleported to the arena."));
        player.sendMessage(MM.parse("<gray>Use /vz setlobby, /vz sethumanspawn, /vz setvampspawn to configure."));
    }

    void handleTest(Player player) {
        if (!player.hasPermission("vampirez.admin")) {
            player.sendMessage(MM.parse("<red>No permission!"));
            return;
        }
        perkTestGUI.openTestMenu(player, 0);
    }

    void handleTools(Player player) {
        if (!player.hasPermission("vampirez.admin")) {
            player.sendMessage(MM.parse("<red>No permission!"));
            return;
        }
        int added = DebugBookManager.giveBooks(player);
        if (added == 0) {
            player.sendMessage(MM.parse("<yellow>You already have all debug books."));
        } else {
            player.sendMessage(MM.parse("<green>Added " + added + " debug book(s) to your inventory."));
        }
    }

    void handleDebugDmg(Player player) {
        if (!player.hasPermission("vampirez.admin")) {
            player.sendMessage(MM.parse("<red>No permission!"));
            return;
        }
        boolean enabled = DebugBookManager.toggleDamageDebug(player);
        if (enabled) {
            player.sendMessage(MM.parse("<green>Damage readout ON. Every hit you deal will print damage + target HP in your action bar."));
        } else {
            player.sendMessage(MM.parse("<yellow>Damage readout OFF."));
        }
    }

    void handleReload(Player player) {
        if (!player.hasPermission("vampirez.admin")) {
            player.sendMessage(MM.parse("<red>No permission!"));
            return;
        }
        if (gameManager.getState() != GameState.LOBBY) {
            player.sendMessage(MM.parse("<red>Cannot reload during an active game!"));
            return;
        }
        gameManager.reloadAllConfig();
        player.sendMessage(MM.parse("<green>Config reloaded! Updated: game settings, economy, day/night, disabled perks, perk limits."));
    }

    void handleLeaderboard(Player player) {
        if (leaderboardGUI == null) {
            player.sendMessage(MM.parse("<red>Leaderboard is not available."));
            return;
        }
        leaderboardGUI.open(player);
    }

    void handleDisablePerk(Player player, String[] args) {
        if (!checkAdmin(player)) return;
        if (args.length < 2) {
            player.sendMessage(MM.parse("<red>Usage: /vz disableperk <perkId>"));
            return;
        }
        String perkId = args[1].toLowerCase();
        if (gameManager.getPerkManager().getPerkById(perkId) == null) {
            player.sendMessage(MM.parse("<red>Unknown perk: " + perkId));
            return;
        }
        if (gameManager.getPerkManager().isDisabled(perkId)) {
            player.sendMessage(MM.parse("<yellow>" + perkId + " is already disabled."));
            return;
        }
        // Update typed config and apply immediately
        List<String> list = gameManager.getPlugin().getPluginConfig().perks.disabledPerks;
        list.add(perkId);
        gameManager.getPlugin().savePluginConfig();
        gameManager.getPerkManager().setDisabledPerks(list);
        player.sendMessage(MM.parse("<red>⊘ <bold>" + perkId + "</bold> disabled. Will not appear in perk selections."));
    }

    void handleEnablePerk(Player player, String[] args) {
        if (!checkAdmin(player)) return;
        if (args.length < 2) {
            player.sendMessage(MM.parse("<red>Usage: /vz enableperk <perkId>"));
            return;
        }
        String perkId = args[1].toLowerCase();
        if (!gameManager.getPerkManager().isDisabled(perkId)) {
            player.sendMessage(MM.parse("<yellow>" + perkId + " is not currently disabled."));
            return;
        }
        List<String> list = gameManager.getPlugin().getPluginConfig().perks.disabledPerks;
        list.remove(perkId);
        gameManager.getPlugin().savePluginConfig();
        gameManager.getPerkManager().setDisabledPerks(list);
        player.sendMessage(MM.parse("<green>✔ <bold>" + perkId + "</bold> enabled and back in the perk pool."));
    }
}

package com.vampirez;

import com.vampirez.api.VampireZAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.paper.util.sender.PaperSimpleSenderMapper;
import org.incendo.cloud.paper.util.sender.PlayerSource;
import org.incendo.cloud.paper.util.sender.Source;
import org.incendo.cloud.parser.standard.IntegerParser;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Cloud-command-framework routing layer for {@code /vz}. Owns the command tree, permission
 * gates, argument parsing, and tab-completion suggestions. All handler bodies remain in
 * {@link GameCommands} — this class only dispatches.
 *
 * <p>Registered through Paper's Brigadier command API, so clients see typed argument hints
 * ({@code <player>}, {@code <number>}) and per-subcommand suggestions natively. Every
 * subcommand is player-only; cloud rejects other senders before a handler runs.
 */
public class VampireZCloudCommands {

    private final VampireZPlugin plugin;
    private final GameCommands handlers;
    private final GameManager gameManager;
    private final VampireZAPI api;
    private PaperCommandManager<Source> manager;

    public VampireZCloudCommands(VampireZPlugin plugin, GameCommands handlers, GameManager gameManager, VampireZAPI api) {
        this.plugin = plugin;
        this.handlers = handlers;
        this.gameManager = gameManager;
        this.api = api;
    }

    public void register() {
        manager = PaperCommandManager.builder(PaperSimpleSenderMapper.simpleSenderMapper())
                .executionCoordinator(ExecutionCoordinator.simpleCoordinator())
                .buildOnEnable(plugin);

        // ===== Player commands (no permission required) =====
        leaf("help",         handlers::sendHelp);
        leaf("join",         gameManager::joinGame);
        leaf("leave",        gameManager::leaveGame);
        leaf("shop",         handlers::handleShop);
        leaf("gold",         handlers::handleGold);
        leaf("status",       handlers::handleStatus);
        leaf("leaderboard",  handlers::handleLeaderboard);
        leaf("lb",           handlers::handleLeaderboard);
        leaf("perkstats",    handlers::handlePerkStats);
        leaf("spectate",     gameManager::toggleSpectate);

        // /vz perks [player]
        manager.command(playerRoot("perks")
                .optional("player", StringParser.stringParser(), playerSuggestions())
                .handler(ctx -> {
                    String tgt = ctx.<String>optional("player").orElse(null);
                    handlers.handlePerks(ctx.sender().source(), tgt == null ? new String[]{"perks"} : new String[]{"perks", tgt});
                }));

        // ===== Admin commands (vampirez.admin) =====
        admin("start",         p -> handlers.handleStart(p, false));
        admin("forcestart",    p -> handlers.handleStart(p, true));
        admin("stop",          handlers::handleStop);
        admin("setlobby",      p -> handlers.handleSetSpawn(p, "lobby"));
        admin("sethumanspawn", p -> handlers.handleSetSpawn(p, "human"));
        admin("setvampspawn",  p -> handlers.handleSetSpawn(p, "vampire"));
        admin("test",          handlers::handleTest);
        admin("tools",         handlers::handleTools);
        admin("debugdmg",      handlers::handleDebugDmg);
        admin("announce",      handlers::handleAnnounce);
        admin("arena",         handlers::handleArena);
        admin("reload",        handlers::handleReload);
        admin("apitest",       handlers::handleApiTest);

        // /vz giveperk <player> <perkId>
        manager.command(adminRoot("giveperk")
                .required("player", StringParser.stringParser(), playerSuggestions())
                .required("perkId", StringParser.stringParser(), perkIdSuggestions())
                .handler(ctx -> handlers.handleGivePerk(ctx.sender().source(),
                        new String[]{"giveperk", ctx.get("player"), ctx.get("perkId")})));

        // /vz removeperk <player> <perkId>
        manager.command(adminRoot("removeperk")
                .required("player", StringParser.stringParser(), playerSuggestions())
                .required("perkId", StringParser.stringParser(), perkIdSuggestions())
                .handler(ctx -> handlers.handleRemovePerk(ctx.sender().source(),
                        new String[]{"removeperk", ctx.get("player"), ctx.get("perkId")})));

        // /vz forceconvert <player>
        manager.command(adminRoot("forceconvert")
                .required("player", StringParser.stringParser(), playerSuggestions())
                .handler(ctx -> handlers.handleForceConvert(ctx.sender().source(),
                        new String[]{"forceconvert", ctx.get("player")})));

        // /vz settime <seconds>
        manager.command(adminRoot("settime")
                .required("seconds", IntegerParser.integerParser(0))
                .handler(ctx -> handlers.handleSetTime(ctx.sender().source(),
                        new String[]{"settime", String.valueOf(ctx.<Integer>get("seconds"))})));

        // /vz setphase day|night
        manager.command(adminRoot("setphase")
                .required("phase", StringParser.stringParser(),
                        SuggestionProvider.suggestingStrings("day", "night"))
                .handler(ctx -> handlers.handleSetPhase(ctx.sender().source(),
                        new String[]{"setphase", ctx.get("phase")})));

        // /vz setgold <player> <amount>
        manager.command(adminRoot("setgold")
                .required("player", StringParser.stringParser(), playerSuggestions())
                .required("amount", IntegerParser.integerParser(0))
                .handler(ctx -> handlers.handleSetGold(ctx.sender().source(),
                        new String[]{"setgold", ctx.get("player"), String.valueOf(ctx.<Integer>get("amount"))})));

        // /vz addgold <player> <amount>
        manager.command(adminRoot("addgold")
                .required("player", StringParser.stringParser(), playerSuggestions())
                .required("amount", IntegerParser.integerParser())
                .handler(ctx -> handlers.handleAddGold(ctx.sender().source(),
                        new String[]{"addgold", ctx.get("player"), String.valueOf(ctx.<Integer>get("amount"))})));

        // /vz disableperk <perkId> — suggest only currently enabled perks
        manager.command(adminRoot("disableperk")
                .required("perkId", StringParser.stringParser(), enabledPerkSuggestions())
                .handler(ctx -> handlers.handleDisablePerk(ctx.sender().source(),
                        new String[]{"disableperk", ctx.get("perkId")})));

        // /vz enableperk <perkId> — suggest only currently disabled perks
        manager.command(adminRoot("enableperk")
                .required("perkId", StringParser.stringParser(), disabledPerkSuggestions())
                .handler(ctx -> handlers.handleEnablePerk(ctx.sender().source(),
                        new String[]{"enableperk", ctx.get("perkId")})));

        // Bare /vz → help
        manager.command(root().senderType(PlayerSource.class)
                .handler(ctx -> handlers.sendHelp(ctx.sender().source())));
    }

    private Command.Builder<Source> root() {
        return manager.commandBuilder("vz", "vampirez");
    }

    private Command.Builder<PlayerSource> playerRoot(String literal) {
        return root().literal(literal).senderType(PlayerSource.class);
    }

    private Command.Builder<PlayerSource> adminRoot(String literal) {
        return playerRoot(literal).permission("vampirez.admin");
    }

    /** Player-only zero-arg subcommand. */
    private void leaf(String name, Consumer<Player> handler) {
        manager.command(playerRoot(name).handler(ctx -> handler.accept(ctx.sender().source())));
    }

    /** Admin-gated zero-arg subcommand. */
    private void admin(String name, Consumer<Player> handler) {
        manager.command(adminRoot(name).handler(ctx -> handler.accept(ctx.sender().source())));
    }

    private SuggestionProvider<Source> playerSuggestions() {
        return (ctx, input) -> {
            List<Suggestion> sugs = Bukkit.getOnlinePlayers().stream()
                    .map(p -> Suggestion.suggestion(p.getName()))
                    .collect(Collectors.toList());
            return CompletableFuture.completedFuture(sugs);
        };
    }

    private SuggestionProvider<Source> perkIdSuggestions() {
        return (ctx, input) -> {
            List<Suggestion> sugs = api.getAvailablePerkIds().stream()
                    .map(Suggestion::suggestion)
                    .collect(Collectors.toList());
            return CompletableFuture.completedFuture(sugs);
        };
    }

    private SuggestionProvider<Source> enabledPerkSuggestions() {
        return (ctx, input) -> {
            List<Suggestion> sugs = api.getAvailablePerkIds().stream()
                    .filter(id -> !gameManager.getPerkManager().isDisabled(id))
                    .map(Suggestion::suggestion)
                    .collect(Collectors.toList());
            return CompletableFuture.completedFuture(sugs);
        };
    }

    private SuggestionProvider<Source> disabledPerkSuggestions() {
        return (ctx, input) -> {
            List<Suggestion> sugs = api.getAvailablePerkIds().stream()
                    .filter(id -> gameManager.getPerkManager().isDisabled(id))
                    .map(Suggestion::suggestion)
                    .collect(Collectors.toList());
            return CompletableFuture.completedFuture(sugs);
        };
    }
}

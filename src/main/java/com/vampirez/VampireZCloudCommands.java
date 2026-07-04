package com.vampirez;

import com.vampirez.api.VampireZAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import org.incendo.cloud.parser.standard.IntegerParser;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Cloud-command-framework routing layer for {@code /vz}. Owns the command tree, permission
 * gates, argument parsing, and tab-completion suggestions. All handler bodies remain in
 * {@link GameCommands} — this class only dispatches.
 *
 * <p>Adds Brigadier integration so 1.13+ clients see typed argument hints
 * ({@code <player>}, {@code <number>}) and per-subcommand suggestions natively.
 */
public class VampireZCloudCommands {

    private final VampireZPlugin plugin;
    private final GameCommands handlers;
    private final GameManager gameManager;
    private final VampireZAPI api;
    private LegacyPaperCommandManager<CommandSender> manager;

    public VampireZCloudCommands(VampireZPlugin plugin, GameCommands handlers, GameManager gameManager, VampireZAPI api) {
        this.plugin = plugin;
        this.handlers = handlers;
        this.gameManager = gameManager;
        this.api = api;
    }

    public void register() {
        manager = LegacyPaperCommandManager.createNative(plugin, ExecutionCoordinator.simpleCoordinator());
        // Best-effort Brigadier registration. Cloud 2.0-beta's capability check fails on
        // recent Paper builds; commands still dispatch via the legacy Bukkit path (with Cloud
        // tab-completion and permission gating), just without client-side argument type hints.
        try { manager.registerLegacyPaperBrigadier(); } catch (Throwable ignored) {}
        try { manager.registerAsynchronousCompletions(); } catch (Throwable ignored) {}

        // ===== Player commands (no permission required) =====
        leaf("help",         ctx -> handlers.sendHelp(player(ctx)));
        leaf("join",         ctx -> gameManager.joinGame(player(ctx)));
        leaf("leave",        ctx -> gameManager.leaveGame(player(ctx)));
        leaf("shop",         ctx -> handlers.handleShop(player(ctx)));
        leaf("gold",         ctx -> handlers.handleGold(player(ctx)));
        leaf("status",       ctx -> handlers.handleStatus(player(ctx)));
        leaf("leaderboard",  ctx -> handlers.handleLeaderboard(player(ctx)));
        leaf("lb",           ctx -> handlers.handleLeaderboard(player(ctx)));
        leaf("perkstats",    ctx -> handlers.handlePerkStats(player(ctx)));

        // /vz perks [player]
        manager.command(manager.commandBuilder("vz")
                .literal("perks")
                .optional("player", StringParser.stringParser(), playerSuggestions())
                .handler(ctx -> {
                    Player p = player(ctx);
                    String tgt = ctx.<String>optional("player").orElse(null);
                    handlers.handlePerks(p, tgt == null ? new String[]{"perks"} : new String[]{"perks", tgt});
                }));

        // ===== Admin commands (vampirez.admin) =====
        admin("start",        ctx -> handlers.handleStart(player(ctx), false));
        admin("forcestart",   ctx -> handlers.handleStart(player(ctx), true));
        admin("stop",         ctx -> handlers.handleStop(player(ctx)));
        admin("setlobby",     ctx -> handlers.handleSetSpawn(player(ctx), "lobby"));
        admin("sethumanspawn",ctx -> handlers.handleSetSpawn(player(ctx), "human"));
        admin("setvampspawn", ctx -> handlers.handleSetSpawn(player(ctx), "vampire"));
        admin("test",         ctx -> handlers.handleTest(player(ctx)));
        admin("tools",        ctx -> handlers.handleTools(player(ctx)));
        admin("debugdmg",     ctx -> handlers.handleDebugDmg(player(ctx)));
        admin("announce",     ctx -> handlers.handleAnnounce(player(ctx)));
        admin("arena",        ctx -> handlers.handleArena(player(ctx)));
        admin("reload",       ctx -> handlers.handleReload(player(ctx)));
        admin("apitest",      ctx -> handlers.handleApiTest(player(ctx)));

        // /vz giveperk <player> <perkId>
        manager.command(manager.commandBuilder("vz")
                .literal("giveperk")
                .permission("vampirez.admin")
                .required("player", StringParser.stringParser(), playerSuggestions())
                .required("perkId", StringParser.stringParser(), perkIdSuggestions())
                .handler(ctx -> {
                    handlers.handleGivePerk(player(ctx),
                            new String[]{"giveperk", ctx.get("player"), ctx.get("perkId")});
                }));

        // /vz removeperk <player> <perkId>
        manager.command(manager.commandBuilder("vz")
                .literal("removeperk")
                .permission("vampirez.admin")
                .required("player", StringParser.stringParser(), playerSuggestions())
                .required("perkId", StringParser.stringParser(), perkIdSuggestions())
                .handler(ctx -> {
                    handlers.handleRemovePerk(player(ctx),
                            new String[]{"removeperk", ctx.get("player"), ctx.get("perkId")});
                }));

        // /vz forceconvert <player>
        manager.command(manager.commandBuilder("vz")
                .literal("forceconvert")
                .permission("vampirez.admin")
                .required("player", StringParser.stringParser(), playerSuggestions())
                .handler(ctx -> {
                    handlers.handleForceConvert(player(ctx),
                            new String[]{"forceconvert", ctx.get("player")});
                }));

        // /vz settime <seconds>
        manager.command(manager.commandBuilder("vz")
                .literal("settime")
                .permission("vampirez.admin")
                .required("seconds", IntegerParser.integerParser(0))
                .handler(ctx -> {
                    handlers.handleSetTime(player(ctx),
                            new String[]{"settime", String.valueOf(ctx.<Integer>get("seconds"))});
                }));

        // /vz setphase day|night
        manager.command(manager.commandBuilder("vz")
                .literal("setphase")
                .permission("vampirez.admin")
                .required("phase", StringParser.stringParser(),
                        SuggestionProvider.suggestingStrings("day", "night"))
                .handler(ctx -> {
                    handlers.handleSetPhase(player(ctx),
                            new String[]{"setphase", ctx.get("phase")});
                }));

        // /vz setgold <player> <amount>
        manager.command(manager.commandBuilder("vz")
                .literal("setgold")
                .permission("vampirez.admin")
                .required("player", StringParser.stringParser(), playerSuggestions())
                .required("amount", IntegerParser.integerParser(0))
                .handler(ctx -> {
                    handlers.handleSetGold(player(ctx),
                            new String[]{"setgold", ctx.get("player"), String.valueOf(ctx.<Integer>get("amount"))});
                }));

        // /vz addgold <player> <amount>
        manager.command(manager.commandBuilder("vz")
                .literal("addgold")
                .permission("vampirez.admin")
                .required("player", StringParser.stringParser(), playerSuggestions())
                .required("amount", IntegerParser.integerParser())
                .handler(ctx -> {
                    handlers.handleAddGold(player(ctx),
                            new String[]{"addgold", ctx.get("player"), String.valueOf(ctx.<Integer>get("amount"))});
                }));

        // /vz disableperk <perkId> — suggest only currently enabled perks
        manager.command(manager.commandBuilder("vz")
                .literal("disableperk")
                .permission("vampirez.admin")
                .required("perkId", StringParser.stringParser(), enabledPerkSuggestions())
                .handler(ctx -> {
                    handlers.handleDisablePerk(player(ctx),
                            new String[]{"disableperk", ctx.get("perkId")});
                }));

        // /vz enableperk <perkId> — suggest only currently disabled perks
        manager.command(manager.commandBuilder("vz")
                .literal("enableperk")
                .permission("vampirez.admin")
                .required("perkId", StringParser.stringParser(), disabledPerkSuggestions())
                .handler(ctx -> {
                    handlers.handleEnablePerk(player(ctx),
                            new String[]{"enableperk", ctx.get("perkId")});
                }));

        // Bare /vz → help
        manager.command(manager.commandBuilder("vz")
                .handler(ctx -> handlers.sendHelp(player(ctx))));
    }

    /** Player-only zero-arg subcommand. */
    private void leaf(String name, java.util.function.Consumer<org.incendo.cloud.context.CommandContext<CommandSender>> handler) {
        manager.command(manager.commandBuilder("vz")
                .literal(name)
                .handler(ctx -> handler.accept(ctx)));
    }

    /** Admin-gated zero-arg subcommand. */
    private void admin(String name, java.util.function.Consumer<org.incendo.cloud.context.CommandContext<CommandSender>> handler) {
        manager.command(manager.commandBuilder("vz")
                .literal(name)
                .permission("vampirez.admin")
                .handler(ctx -> handler.accept(ctx)));
    }

    private static Player player(org.incendo.cloud.context.CommandContext<CommandSender> ctx) {
        CommandSender s = ctx.sender();
        if (!(s instanceof Player p)) {
            s.sendMessage("Only players can use this command.");
            return null;
        }
        return p;
    }

    private SuggestionProvider<CommandSender> playerSuggestions() {
        return (ctx, input) -> {
            List<Suggestion> sugs = Bukkit.getOnlinePlayers().stream()
                    .map(p -> Suggestion.suggestion(p.getName()))
                    .collect(Collectors.toList());
            return CompletableFuture.completedFuture(sugs);
        };
    }

    private SuggestionProvider<CommandSender> perkIdSuggestions() {
        return (ctx, input) -> {
            List<Suggestion> sugs = api.getAvailablePerkIds().stream()
                    .map(Suggestion::suggestion)
                    .collect(Collectors.toList());
            return CompletableFuture.completedFuture(sugs);
        };
    }

    private SuggestionProvider<CommandSender> enabledPerkSuggestions() {
        return (ctx, input) -> {
            List<Suggestion> sugs = api.getAvailablePerkIds().stream()
                    .filter(id -> !gameManager.getPerkManager().isDisabled(id))
                    .map(Suggestion::suggestion)
                    .collect(Collectors.toList());
            return CompletableFuture.completedFuture(sugs);
        };
    }

    private SuggestionProvider<CommandSender> disabledPerkSuggestions() {
        return (ctx, input) -> {
            List<Suggestion> sugs = api.getAvailablePerkIds().stream()
                    .filter(id -> gameManager.getPerkManager().isDisabled(id))
                    .map(Suggestion::suggestion)
                    .collect(Collectors.toList());
            return CompletableFuture.completedFuture(sugs);
        };
    }
}

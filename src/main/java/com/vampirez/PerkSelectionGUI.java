package com.vampirez;

import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Free / conversion / timed perk selection screen, built on triumph-gui.
 * Handles per-slot rerolls, a 15-second auto-pick countdown, and a max-attempts
 * close-and-reopen loop that auto-assigns a random perk if the player keeps closing the GUI.
 *
 * <p>State lives in a {@link SelectionState} closure attached to the {@link Gui} via
 * {@link Gui#setCloseGuiAction}; no plugin-level map is needed.
 */
public class PerkSelectionGUI {

    private static final int SELECTION_TIME_SECONDS = 15;
    private static final int MAX_REOPEN_ATTEMPTS = 3;

    private final PerkManager perkManager;
    private final GameManager gameManager;
    private final VampireZPlugin plugin;
    private final Random rng = new Random();

    /**
     * Selections currently awaiting a pick, keyed by player. Lets the game-end path
     * force-close every open selection (cancelAllOpen) instead of relying on each
     * countdown/click handler to notice the state change on its own.
     */
    private final Map<UUID, SelectionState> activeStates = new HashMap<>();

    public PerkSelectionGUI(PerkManager perkManager, GameManager gameManager, VampireZPlugin plugin) {
        this.perkManager = perkManager;
        this.gameManager = gameManager;
        this.plugin = plugin;
    }

    private enum Mode { FREE, TIMED, CONVERSION }

    private static class SelectionState {
        Mode mode;
        PerkTier perkTier;
        PerkTeam playerTeam;
        List<Perk> offeredPerks;
        int remainingConversionPicks;
        Component title;
        boolean selected;
        boolean rerolling;
        int reopenAttempts;
        boolean[] rerolled = new boolean[3];
        BukkitTask countdownTask;
    }

    // ===== Public entry points =====

    public void openFreeSelection(Player player, PerkTeam team) {
        List<Perk> options = perkManager.getRandomPerks(PerkTier.SILVER, team, 3, player.getUniqueId());
        if (options.isEmpty()) return;

        SelectionState state = new SelectionState();
        state.mode = Mode.FREE;
        state.perkTier = PerkTier.SILVER;
        state.playerTeam = team;
        state.offeredPerks = new ArrayList<>(options);
        state.title = Component.text("Choose Your Starting Perk").color(NamedTextColor.GREEN);
        openSelectionGui(player, state);
    }

    public void openTimedSelection(Player player, PerkTier tier, PerkTeam team) {
        List<Perk> options = perkManager.getRandomPerks(tier, team, 3, player.getUniqueId());
        if (options.isEmpty()) return;

        SelectionState state = new SelectionState();
        state.mode = Mode.TIMED;
        state.perkTier = tier;
        state.playerTeam = team;
        state.offeredPerks = new ArrayList<>(options);
        state.title = Component.text("Free ").color(NamedTextColor.GREEN)
                .append(Component.text(tier.getDisplayName()).color(tier.getTextColor()))
                .append(Component.text(" Perk!").color(NamedTextColor.GREEN));
        openSelectionGui(player, state);
    }

    public void openConversionSelection(Player player, PerkTeam newTeam, int picksRemaining) {
        List<Perk> options = perkManager.getRandomPerks(PerkTier.SILVER, newTeam, 3, player.getUniqueId());
        if (options.isEmpty()) return;

        SelectionState state = new SelectionState();
        state.mode = Mode.CONVERSION;
        state.perkTier = PerkTier.SILVER;
        state.playerTeam = newTeam;
        state.offeredPerks = new ArrayList<>(options);
        state.remainingConversionPicks = picksRemaining;
        state.title = Component.text("Choose Replacement Perk").color(NamedTextColor.DARK_PURPLE);
        openSelectionGui(player, state);
    }

    // ===== GUI building =====

    private void openSelectionGui(Player player, SelectionState state) {
        activeStates.put(player.getUniqueId(), state);
        Gui gui = buildGui(player, state);
        gui.open(player);
        startCountdown(player, state);
    }

    /**
     * Force-closes every open selection GUI without assigning perks. Called when the
     * game ends (endGame / stopGame / resetToLobby) so no countdown or click handler
     * can add a perk after the perk lists have been wiped (BUG_PERK_PERSISTENCE
     * Layer 2 — the per-handler state guards remain as Layer 1).
     */
    public void cancelAllOpen() {
        for (Map.Entry<UUID, SelectionState> entry : new HashMap<>(activeStates).entrySet()) {
            SelectionState state = entry.getValue();
            state.selected = true; // stops handleClose from reopening and countdown from assigning
            cancelCountdown(state);
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p != null && p.isOnline()) {
                p.closeInventory();
            }
        }
        activeStates.clear();
    }

    /** Marks a selection finished and forgets it (unless a chained pick already replaced it). */
    private void concludeState(UUID uuid, SelectionState state) {
        state.selected = true;
        cancelCountdown(state);
        if (activeStates.get(uuid) == state) {
            activeStates.remove(uuid);
        }
    }

    private Gui buildGui(Player player, SelectionState state) {
        Gui gui = Gui.gui()
                .title(state.title)
                .rows(3)
                .disableAllInteractions()
                .create();

        int[] perkSlots = {11, 13, 15};
        int[] rerollSlots = {20, 22, 24};

        for (int i = 0; i < state.offeredPerks.size() && i < 3; i++) {
            final int idx = i;
            Perk perk = state.offeredPerks.get(i);

            // Perk choice
            gui.setItem(perkSlots[i], new GuiItem(perkChoiceIcon(perk, state),
                    event -> selectPerk(player, state, idx)));

            // Reroll button (or "used" marker)
            if (!state.rerolled[i]) {
                gui.setItem(rerollSlots[i], new GuiItem(rerollIcon(),
                        event -> rerollSlot(player, state, idx, gui)));
            } else {
                gui.setItem(rerollSlots[i], new GuiItem(rerolledIcon()));
            }
        }

        // Auto-reopen + max-attempts handling
        gui.setCloseGuiAction(event -> handleClose(player, state));

        return gui;
    }

    private ItemStack perkChoiceIcon(Perk perk, SelectionState state) {
        ItemStack item = perk.createDisplayItem();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
            lore.add(Component.empty());
            if (state.mode == Mode.FREE || state.mode == Mode.TIMED) {
                lore.add(Component.text("FREE!").color(NamedTextColor.GREEN)
                        .decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
            } else {
                lore.add(Component.text("Replacement perk (" + state.remainingConversionPicks + " remaining)")
                        .color(NamedTextColor.DARK_PURPLE).decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack rerollIcon() {
        ItemStack item = new ItemStack(Material.SUNFLOWER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Reroll").color(NamedTextColor.YELLOW)
                    .decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Replace this perk with").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("a different random one.").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("One use only!").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack rerolledIcon() {
        ItemStack item = new ItemStack(Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Already Rerolled").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }

    // ===== Click handlers =====

    private void selectPerk(Player player, SelectionState state, int idx) {
        if (state.selected) return;
        // Game-state guard: if the game has already ended (e.g. last human just converted
        // while this GUI was still open), don't add the perk — it would leak into the lobby
        // and the next game.
        if (gameManager.getState() != GameState.ACTIVE && gameManager.getState() != GameState.STARTING) {
            concludeState(player.getUniqueId(), state);
            player.closeInventory();
            return;
        }
        Perk picked = state.offeredPerks.get(idx);
        UUID uuid = player.getUniqueId();

        perkManager.addPerkToPlayer(uuid, picked, sourceFor(state));
        player.sendMessage(Component.empty()
                .append(MM.parse("<green>Perk acquired: "))
                .append(Component.text(picked.getDisplayName()).color(picked.getTier().getTextColor())));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);

        finalizeSelection(player, state);
    }

    private void rerollSlot(Player player, SelectionState state, int idx, Gui gui) {
        UUID uuid = player.getUniqueId();
        Set<String> excludeIds = new HashSet<>();
        for (Perk p : perkManager.getPlayerPerks(uuid)) excludeIds.add(p.getId());
        for (int i = 0; i < state.offeredPerks.size(); i++) excludeIds.add(state.offeredPerks.get(i).getId());

        List<Perk> replacement = perkManager.getRandomPerks(state.perkTier, state.playerTeam, 1, uuid);
        replacement.removeIf(p -> excludeIds.contains(p.getId()));
        if (replacement.isEmpty()) {
            // Try again without excluding the other shown perks (just exclude the current one).
            replacement = perkManager.getRandomPerks(state.perkTier, state.playerTeam, 1, uuid);
            replacement.removeIf(p -> p.getId().equals(state.offeredPerks.get(idx).getId()));
        }

        if (replacement.isEmpty()) {
            player.sendMessage(MM.parse("<red>No other perks available to reroll into!"));
            return;
        }

        state.offeredPerks.set(idx, replacement.get(0));
        state.rerolled[idx] = true;
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);

        // Rebuild + reopen — flag prevents close handler from triggering reopen logic
        state.rerolling = true;
        Gui refreshed = buildGui(player, state);
        refreshed.open(player);
        state.rerolling = false;
    }

    private void handleClose(Player player, SelectionState state) {
        if (state.selected || state.rerolling) return;

        if (state.reopenAttempts < MAX_REOPEN_ATTEMPTS - 1) {
            state.reopenAttempts++;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;
                if (gameManager.getState() != GameState.ACTIVE && gameManager.getState() != GameState.STARTING) return;
                buildGui(player, state).open(player);
            }, 20L);
        } else {
            // Out of patience — auto-assign a random perk.
            concludeState(player.getUniqueId(), state);
            // Game-state guard: same leak as selectPerk/countdown — never assign post-game.
            if (gameManager.getState() != GameState.ACTIVE && gameManager.getState() != GameState.STARTING) {
                return;
            }
            Perk randomPerk = state.offeredPerks.get(rng.nextInt(state.offeredPerks.size()));
            perkManager.addPerkToPlayer(player.getUniqueId(), randomPerk, sourceFor(state));
            player.sendMessage(Component.empty()
                    .append(MM.parse("<yellow>Perk auto-assigned: "))
                    .append(Component.text(randomPerk.getDisplayName()).color(randomPerk.getTier().getTextColor())));
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            chainNextConversionPick(player, state);
        }
    }

    // ===== Countdown =====

    private void startCountdown(Player player, SelectionState state) {
        UUID uuid = player.getUniqueId();
        final int[] secondsLeft = {SELECTION_TIME_SECONDS};

        state.countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (state.selected) { cancelCountdown(state); return; }
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline()) { concludeState(uuid, state); return; }

            secondsLeft[0]--;
            if (secondsLeft[0] <= 5 && secondsLeft[0] > 0) {
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f + (5 - secondsLeft[0]) * 0.1f);
                p.sendActionBar(MM.parse("<red>⚠ <yellow>Auto-selecting in <red><bold>"
                        + secondsLeft[0] + "</bold><yellow>s <red>⚠"));
            }
            if (secondsLeft[0] <= 0) {
                // Game-state guard: if the game ended while the GUI was open, don't
                // auto-assign — would leak the perk into lobby and next game.
                if (gameManager.getState() != GameState.ACTIVE && gameManager.getState() != GameState.STARTING) {
                    concludeState(uuid, state);
                    p.closeInventory();
                    return;
                }
                Perk randomPerk = state.offeredPerks.get(rng.nextInt(state.offeredPerks.size()));
                perkManager.addPerkToPlayer(uuid, randomPerk, sourceFor(state));
                p.sendMessage(Component.empty()
                        .append(MM.parse("<yellow>Perk auto-assigned: "))
                        .append(Component.text(randomPerk.getDisplayName()).color(randomPerk.getTier().getTextColor())));
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);

                concludeState(uuid, state);
                p.closeInventory();
                chainNextConversionPick(p, state);
            }
        }, 20L, 20L);
    }

    private void cancelCountdown(SelectionState state) {
        if (state.countdownTask != null) {
            state.countdownTask.cancel();
            state.countdownTask = null;
        }
    }

    // ===== Lifecycle helpers =====

    private void finalizeSelection(Player player, SelectionState state) {
        concludeState(player.getUniqueId(), state);
        player.closeInventory();
        chainNextConversionPick(player, state);
    }

    private void chainNextConversionPick(Player player, SelectionState state) {
        if (state.mode == Mode.CONVERSION && state.remainingConversionPicks > 1) {
            int remaining = state.remainingConversionPicks - 1;
            PerkTeam team = state.playerTeam;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) openConversionSelection(player, team, remaining);
            }, 20L);
        }
    }

    private static com.vampirez.api.event.PlayerPerkGainedEvent.Source sourceFor(SelectionState s) {
        return switch (s.mode) {
            case TIMED -> com.vampirez.api.event.PlayerPerkGainedEvent.Source.TIMED;
            case FREE -> com.vampirez.api.event.PlayerPerkGainedEvent.Source.FREE;
            case CONVERSION -> com.vampirez.api.event.PlayerPerkGainedEvent.Source.CONVERSION;
        };
    }
}

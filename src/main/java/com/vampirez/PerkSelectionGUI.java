package com.vampirez;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.*;

public class PerkSelectionGUI implements Listener {

    private static final String FREE_GUI_TITLE = ChatColor.GREEN + "Choose Your Starting Perk";
    private static final String CONVERSION_GUI_TITLE = ChatColor.DARK_PURPLE + "Choose Replacement Perk";
    private static final String TIMED_GUI_TITLE_PREFIX = ChatColor.GREEN + "Free ";

    private final PerkManager perkManager;
    private final GameManager gameManager;
    private final JavaPlugin plugin;

    private final Map<UUID, SelectionState> selectionStates = new HashMap<>();

    public PerkSelectionGUI(PerkManager perkManager, GameManager gameManager, JavaPlugin plugin) {
        this.perkManager = perkManager;
        this.gameManager = gameManager;
        this.plugin = plugin;
    }

    private static final int SELECTION_TIME_SECONDS = 15;

    private static class SelectionState {
        List<Perk> offeredPerks;
        boolean isFreeSelection;
        int remainingConversionPicks;
        PerkTeam playerTeam;
        PerkTier perkTier;
        String guiTitle;
        boolean selected = false;
        int reopenAttempts = 0;
        boolean[] rerolled = {false, false, false}; // track per-slot rerolls
        boolean rerolling = false; // true while a reroll is reopening the inventory
        BukkitTask countdownTask;
    }

    private static final int MAX_REOPEN_ATTEMPTS = 3;

    public void openFreeSelection(Player player, PerkTeam team) {
        UUID uuid = player.getUniqueId();

        List<Perk> options = perkManager.getRandomPerks(PerkTier.SILVER, team, 3, uuid);
        if (options.isEmpty()) return;

        SelectionState state = new SelectionState();
        state.offeredPerks = options;
        state.isFreeSelection = true;
        state.playerTeam = team;
        state.perkTier = PerkTier.SILVER;
        state.guiTitle = FREE_GUI_TITLE;
        selectionStates.put(uuid, state);

        Inventory inv = buildSelectionInventory(state, FREE_GUI_TITLE);
        player.openInventory(inv);
        startCountdown(player, state);
    }

    public void openTimedSelection(Player player, PerkTier tier, PerkTeam team) {
        UUID uuid = player.getUniqueId();

        List<Perk> options = perkManager.getRandomPerks(tier, team, 3, uuid);
        if (options.isEmpty()) return;

        String title = TIMED_GUI_TITLE_PREFIX + tier.getColor() + tier.getDisplayName() + ChatColor.GREEN + " Perk!";

        SelectionState state = new SelectionState();
        state.offeredPerks = options;
        state.isFreeSelection = true;
        state.playerTeam = team;
        state.perkTier = tier;
        state.guiTitle = title;
        selectionStates.put(uuid, state);

        Inventory inv = buildSelectionInventory(state, title);
        player.openInventory(inv);
        startCountdown(player, state);
    }

    public void openConversionSelection(Player player, PerkTeam newTeam, int picksRemaining) {
        UUID uuid = player.getUniqueId();

        List<Perk> options = perkManager.getRandomPerks(PerkTier.SILVER, newTeam, 3, uuid);
        if (options.isEmpty()) return;

        SelectionState state = new SelectionState();
        state.offeredPerks = options;
        state.isFreeSelection = false;
        state.remainingConversionPicks = picksRemaining;
        state.playerTeam = newTeam;
        state.perkTier = PerkTier.SILVER;
        state.guiTitle = CONVERSION_GUI_TITLE;
        selectionStates.put(uuid, state);

        Inventory inv = buildSelectionInventory(state, CONVERSION_GUI_TITLE);
        player.openInventory(inv);
        startCountdown(player, state);
    }

    private void startCountdown(Player player, SelectionState state) {
        UUID uuid = player.getUniqueId();
        final int[] secondsLeft = {SELECTION_TIME_SECONDS};

        state.countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (state.selected || !selectionStates.containsKey(uuid)) {
                if (state.countdownTask != null) state.countdownTask.cancel();
                return;
            }
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline()) {
                if (state.countdownTask != null) state.countdownTask.cancel();
                selectionStates.remove(uuid);
                return;
            }

            secondsLeft[0]--;

            // Ticking countdown at 5 seconds and below
            if (secondsLeft[0] <= 5 && secondsLeft[0] > 0) {
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f + (5 - secondsLeft[0]) * 0.1f);
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        new TextComponent(ChatColor.RED + "\u26a0 " + ChatColor.YELLOW + "Auto-selecting in " +
                                ChatColor.RED + "" + ChatColor.BOLD + secondsLeft[0] +
                                ChatColor.RESET + ChatColor.YELLOW + "s " + ChatColor.RED + "\u26a0"));
            }

            if (secondsLeft[0] <= 0) {
                // Auto-assign random perk
                Perk randomPerk = state.offeredPerks.get(new Random().nextInt(state.offeredPerks.size()));
                perkManager.addPerkToPlayer(uuid, randomPerk);
                p.sendMessage(ChatColor.YELLOW + "Perk auto-assigned: " + randomPerk.getTier().getColor() + randomPerk.getDisplayName());
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);

                state.selected = true;
                p.closeInventory();

                // Handle conversion chain
                if (!state.isFreeSelection && state.remainingConversionPicks > 1) {
                    int remaining = state.remainingConversionPicks - 1;
                    PerkTeam team = state.playerTeam;
                    selectionStates.remove(uuid);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (p.isOnline()) {
                            openConversionSelection(p, team, remaining);
                        }
                    }, 20L);
                } else {
                    selectionStates.remove(uuid);
                }

                if (state.countdownTask != null) state.countdownTask.cancel();
            }
        }, 20L, 20L);
    }

    private Inventory buildSelectionInventory(SelectionState state, String title) {
        Inventory inv = Bukkit.createInventory(null, 27, title);
        int[] perkSlots = {11, 13, 15};
        int[] rerollSlots = {20, 22, 24};

        for (int i = 0; i < state.offeredPerks.size() && i < 3; i++) {
            ItemStack display = state.offeredPerks.get(i).createDisplayItem();
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add("");
                if (state.isFreeSelection) {
                    lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "FREE!");
                } else {
                    lore.add(ChatColor.DARK_PURPLE + "Replacement perk (" + state.remainingConversionPicks + " remaining)");
                }
                meta.setLore(lore);
                display.setItemMeta(meta);
            }
            inv.setItem(perkSlots[i], display);

            // Reroll button below each perk
            if (!state.rerolled[i]) {
                ItemStack rerollBtn = new ItemStack(Material.SUNFLOWER);
                ItemMeta rMeta = rerollBtn.getItemMeta();
                if (rMeta != null) {
                    rMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Reroll");
                    rMeta.setLore(Arrays.asList(
                            ChatColor.GRAY + "Replace this perk with",
                            ChatColor.GRAY + "a different random one.",
                            ChatColor.RED + "One use only!"));
                    rerollBtn.setItemMeta(rMeta);
                }
                inv.setItem(rerollSlots[i], rerollBtn);
            } else {
                // Already rerolled - show disabled indicator
                ItemStack used = new ItemStack(Material.GRAY_DYE);
                ItemMeta uMeta = used.getItemMeta();
                if (uMeta != null) {
                    uMeta.setDisplayName(ChatColor.GRAY + "Already Rerolled");
                    used.setItemMeta(uMeta);
                }
                inv.setItem(rerollSlots[i], used);
            }
        }

        return inv;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = event.getView().getTitle();
        if (!title.equals(FREE_GUI_TITLE) && !title.equals(CONVERSION_GUI_TITLE) && !title.startsWith(TIMED_GUI_TITLE_PREFIX)) return;

        event.setCancelled(true);

        UUID uuid = player.getUniqueId();
        SelectionState state = selectionStates.get(uuid);
        if (state == null) return;

        int slot = event.getSlot();

        // Check if it's a reroll button click
        int rerollIndex = switch (slot) {
            case 20 -> 0;
            case 22 -> 1;
            case 24 -> 2;
            default -> -1;
        };

        if (rerollIndex >= 0 && rerollIndex < state.offeredPerks.size() && !state.rerolled[rerollIndex]) {
            // Reroll this specific perk
            // Collect IDs to exclude: player's owned perks + the other offered perks
            Set<String> excludeIds = new HashSet<>();
            for (Perk p : perkManager.getPlayerPerks(uuid)) {
                excludeIds.add(p.getId());
            }
            for (int i = 0; i < state.offeredPerks.size(); i++) {
                if (i != rerollIndex) excludeIds.add(state.offeredPerks.get(i).getId());
            }
            excludeIds.add(state.offeredPerks.get(rerollIndex).getId()); // exclude current too

            // Get 1 new random perk of the same tier/team
            List<Perk> replacement = perkManager.getRandomPerks(state.perkTier, state.playerTeam, 1, uuid);
            // Filter out the ones already shown
            replacement.removeIf(p -> excludeIds.contains(p.getId()));

            if (replacement.isEmpty()) {
                // Try without excluding shown perks (no alternatives available)
                replacement = perkManager.getRandomPerks(state.perkTier, state.playerTeam, 1, uuid);
                replacement.removeIf(p -> p.getId().equals(state.offeredPerks.get(rerollIndex).getId()));
            }

            if (!replacement.isEmpty()) {
                state.offeredPerks.set(rerollIndex, replacement.get(0));
                state.rerolled[rerollIndex] = true;
                state.rerolling = true; // prevent close handler from counting this as a manual close
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);

                // Rebuild the inventory in-place
                Inventory newInv = buildSelectionInventory(state, state.guiTitle);
                player.openInventory(newInv);
                state.rerolling = false;
            } else {
                player.sendMessage(ChatColor.RED + "No other perks available to reroll into!");
            }
            return;
        }

        // Check if it's a perk selection click
        int perkIndex = switch (slot) {
            case 11 -> 0;
            case 13 -> 1;
            case 15 -> 2;
            default -> -1;
        };

        if (perkIndex < 0 || perkIndex >= state.offeredPerks.size()) return;

        Perk selectedPerk = state.offeredPerks.get(perkIndex);

        // Add perk (free, no gold cost)
        perkManager.addPerkToPlayer(uuid, selectedPerk);
        player.sendMessage(ChatColor.GREEN + "Perk acquired: " + selectedPerk.getTier().getColor() + selectedPerk.getDisplayName());
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);

        state.selected = true;
        if (state.countdownTask != null) { state.countdownTask.cancel(); state.countdownTask = null; }
        player.closeInventory();

        // If conversion picks remaining, open next
        if (!state.isFreeSelection && state.remainingConversionPicks > 1) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    openConversionSelection(player, state.playerTeam, state.remainingConversionPicks - 1);
                }
            }, 20L);
        } else {
            selectionStates.remove(uuid);
        }
    }

    private void reopenFromState(Player player, SelectionState state, String title) {
        Inventory inv = buildSelectionInventory(state, title);
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        String title = event.getView().getTitle();
        if (!title.equals(FREE_GUI_TITLE) && !title.equals(CONVERSION_GUI_TITLE) && !title.startsWith(TIMED_GUI_TITLE_PREFIX)) return;

        UUID uuid = player.getUniqueId();
        SelectionState state = selectionStates.get(uuid);
        if (state == null) return;

        // If a perk was selected or a reroll is in progress, don't reopen
        if (state.selected || state.rerolling) return;

        // Force reopen with the same perks if no selection was made (limit retries to prevent loops)
        if (state.offeredPerks != null && !state.offeredPerks.isEmpty() && state.reopenAttempts < MAX_REOPEN_ATTEMPTS) {
            state.reopenAttempts++;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (selectionStates.containsKey(uuid) && player.isOnline()
                        && (gameManager.getState() == GameState.ACTIVE || gameManager.getState() == GameState.STARTING)) {
                    reopenFromState(player, state, state.guiTitle);
                }
            }, 20L);
        } else if (state.reopenAttempts >= MAX_REOPEN_ATTEMPTS) {
            // Auto-assign a random perk from the offered options
            if (state.countdownTask != null) { state.countdownTask.cancel(); state.countdownTask = null; }
            Perk randomPerk = state.offeredPerks.get(new Random().nextInt(state.offeredPerks.size()));
            perkManager.addPerkToPlayer(uuid, randomPerk);
            player.sendMessage(ChatColor.YELLOW + "Perk auto-assigned: " + randomPerk.getTier().getColor() + randomPerk.getDisplayName());
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);

            // If conversion picks remaining, continue the chain
            if (!state.isFreeSelection && state.remainingConversionPicks > 1) {
                int remaining = state.remainingConversionPicks - 1;
                PerkTeam team = state.playerTeam;
                selectionStates.remove(uuid);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        openConversionSelection(player, team, remaining);
                    }
                }, 20L);
            } else {
                selectionStates.remove(uuid);
            }
        }
    }
}

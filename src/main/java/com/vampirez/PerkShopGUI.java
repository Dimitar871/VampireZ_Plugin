package com.vampirez;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class PerkShopGUI implements Listener {

    private static final String TIER_GUI_TITLE = ChatColor.GOLD + "Perk Shop";
    private static final String OPTION_GUI_TITLE = ChatColor.GOLD + "Choose a Perk";

    private final PerkManager perkManager;
    private final EconomyManager economyManager;
    private final GameManager gameManager;
    private final StatAnvilManager statAnvilManager;

    private final Map<UUID, ShopState> shopStates = new HashMap<>();

    public PerkShopGUI(PerkManager perkManager, EconomyManager economyManager, GameManager gameManager, StatAnvilManager statAnvilManager) {
        this.perkManager = perkManager;
        this.economyManager = economyManager;
        this.gameManager = gameManager;
        this.statAnvilManager = statAnvilManager;
    }

    private static class ShopState {
        PerkTier selectedTier;
        PerkTeam playerTeam;
        List<Perk> offeredPerks;
    }

    public void openTierSelection(Player player, PerkTeam team) {
        UUID uuid = player.getUniqueId();
        int perkCount = perkManager.getPlayerPerkCount(uuid);
        int gold = economyManager.getGold(uuid);

        Inventory inv = Bukkit.createInventory(null, 54, TIER_GUI_TITLE);

        // Row 1-3: Tier selection (slots 0-26)
        // Silver - slot 11
        inv.setItem(11, createTierItem(PerkTier.SILVER, gold, perkCount));
        // Gold - slot 13
        inv.setItem(13, createTierItem(PerkTier.GOLD, gold, perkCount));
        // Prismatic - slot 15
        inv.setItem(15, createTierItem(PerkTier.PRISMATIC, gold, perkCount));

        // Row 4: Separator (slots 27-35)
        ItemStack separator = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta sepMeta = separator.getItemMeta();
        if (sepMeta != null) {
            sepMeta.setDisplayName(ChatColor.GREEN + "");
            separator.setItemMeta(sepMeta);
        }
        for (int i = 27; i <= 35; i++) {
            inv.setItem(i, separator);
        }
        // Center label at slot 31
        ItemStack label = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta labelMeta = label.getItemMeta();
        if (labelMeta != null) {
            labelMeta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Your Active Perks");
            label.setItemMeta(labelMeta);
        }
        inv.setItem(31, label);

        // Repair Armor button at slot 29
        ItemStack repairItem = new ItemStack(Material.ANVIL);
        ItemMeta repairMeta = repairItem.getItemMeta();
        if (repairMeta != null) {
            repairMeta.setDisplayName(ChatColor.AQUA + "Repair Armor");
            repairMeta.setLore(java.util.Arrays.asList(
                ChatColor.YELLOW + "Cost: " + ChatColor.GREEN + "25 gold",
                ChatColor.GRAY + "Fully repairs all armor pieces"
            ));
            repairItem.setItemMeta(repairMeta);
        }
        inv.setItem(29, repairItem);

        // Stat Anvil button at slot 33
        ItemStack anvilItem = new ItemStack(Material.DAMAGED_ANVIL);
        ItemMeta anvilMeta = anvilItem.getItemMeta();
        if (anvilMeta != null) {
            anvilMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Stat Anvil");
            List<String> anvilLore = new ArrayList<>();
            anvilLore.add(ChatColor.YELLOW + "Cost: " + ChatColor.GREEN + StatAnvilManager.ANVIL_COST + " gold each");
            anvilLore.add(ChatColor.GRAY + "Buy permanent stat boosts!");
            anvilLore.add(ChatColor.GRAY + "(Does not use perk slots)");
            // Show owned buffs
            List<String> buffSummary = statAnvilManager.getBuffSummary(uuid);
            if (!buffSummary.isEmpty()) {
                anvilLore.add("");
                anvilLore.add(ChatColor.YELLOW + "Your buffs:");
                anvilLore.addAll(buffSummary);
            }
            anvilMeta.setLore(anvilLore);
            anvilItem.setItemMeta(anvilMeta);
        }
        inv.setItem(33, anvilItem);

        // Row 5-6 (slots 36-53): Owned perks with stats
        List<Perk> owned = perkManager.getPlayerPerks(uuid);
        int slot = 36;
        for (Perk perk : owned) {
            if (slot > 53) break;
            inv.setItem(slot, createOwnedPerkItem(perk, uuid));
            slot++;
        }

        ShopState state = new ShopState();
        state.playerTeam = team;
        shopStates.put(uuid, state);

        player.openInventory(inv);
    }

    private ItemStack createOwnedPerkItem(Perk perk, UUID playerUUID) {
        ItemStack item = new ItemStack(perk.getIcon());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(perk.getTier().getColor() + perk.getDisplayName());
            List<String> lore = new ArrayList<>();

            // Perk description
            for (String line : perk.getDescription()) {
                lore.add(ChatColor.GRAY + line);
            }

            lore.add("");
            lore.add(ChatColor.YELLOW + "--- Stats ---");

            // Stats
            Map<String, String> statLabels = perk.getStatLabels();
            Map<String, Double> stats = perk.getPlayerStats(playerUUID);

            if (statLabels.isEmpty()) {
                lore.add(ChatColor.GREEN + "Active");
            } else {
                for (Map.Entry<String, String> entry : statLabels.entrySet()) {
                    double value = stats.getOrDefault(entry.getKey(), 0.0);
                    String formatted;
                    if (value == Math.floor(value)) {
                        formatted = String.valueOf((int) value);
                    } else {
                        formatted = String.format("%.1f", value);
                    }
                    lore.add(ChatColor.GRAY + entry.getValue() + ": " + ChatColor.WHITE + formatted);
                }
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createTierItem(PerkTier tier, int playerGold, int perkCount) {
        boolean canAfford = playerGold >= tier.getCost();
        boolean maxPerks = perkCount >= perkManager.getMaxPerks();

        Material mat;
        if (maxPerks || !canAfford) {
            mat = Material.GRAY_STAINED_GLASS_PANE;
        } else {
            mat = switch (tier) {
                case SILVER -> Material.IRON_INGOT;
                case GOLD -> Material.GOLD_INGOT;
                case PRISMATIC -> Material.DIAMOND;
            };
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(tier.getColor() + tier.getDisplayName() + " Perk");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.YELLOW + "Cost: " + ChatColor.GREEN + tier.getCost() + " gold");
            lore.add(ChatColor.GRAY + "Your Gold: " + ChatColor.WHITE + playerGold);
            lore.add("");
            if (maxPerks) {
                lore.add(ChatColor.RED + "Max perks reached! (" + perkCount + "/" + perkManager.getMaxPerks() + ")");
            } else if (!canAfford) {
                lore.add(ChatColor.RED + "Not enough gold!");
            } else {
                lore.add(ChatColor.GREEN + "Click to browse " + tier.getDisplayName() + " perks!");
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void openPerkOptions(Player player, PerkTier tier) {
        UUID uuid = player.getUniqueId();
        ShopState state = shopStates.get(uuid);
        if (state == null) return;

        state.selectedTier = tier;
        state.offeredPerks = perkManager.getRandomPerks(tier, state.playerTeam, 3, uuid);

        if (state.offeredPerks.isEmpty()) {
            player.sendMessage(ChatColor.RED + "No perks available in this tier!");
            player.closeInventory();
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 27, OPTION_GUI_TITLE);

        // Place perk options at slots 11, 13, 15
        int[] slots = {11, 13, 15};
        for (int i = 0; i < state.offeredPerks.size() && i < 3; i++) {
            inv.setItem(slots[i], state.offeredPerks.get(i).createDisplayItem());
        }

        // Cancel button at slot 22
        ItemStack cancel = new ItemStack(Material.BARRIER);
        ItemMeta cancelMeta = cancel.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.setDisplayName(ChatColor.RED + "Cancel");
            cancel.setItemMeta(cancelMeta);
        }
        inv.setItem(22, cancel);

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = event.getView().getTitle();

        if (title.equals(TIER_GUI_TITLE)) {
            event.setCancelled(true);
            handleTierClick(player, event.getSlot());
        } else if (title.equals(OPTION_GUI_TITLE)) {
            event.setCancelled(true);
            handleOptionClick(player, event.getSlot());
        }
    }

    private void handleTierClick(Player player, int slot) {
        UUID uuid = player.getUniqueId();
        int gold = economyManager.getGold(uuid);
        int perkCount = perkManager.getPlayerPerkCount(uuid);

        // Stat Anvil button
        if (slot == 33) {
            player.closeInventory();
            statAnvilManager.openAnvilGUI(player);
            return;
        }

        // Repair armor button
        if (slot == 29) {
            if (!economyManager.removeGold(uuid, 25)) {
                player.sendMessage(ChatColor.RED + "Not enough gold! Need 25, you have " + gold);
                return;
            }
            for (ItemStack armor : player.getInventory().getArmorContents()) {
                if (armor != null && armor.getType() != Material.AIR) {
                    armor.setDurability((short) 0);
                }
            }
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
            player.sendMessage(ChatColor.GREEN + "Armor repaired!");
            player.closeInventory();
            return;
        }

        if (perkCount >= perkManager.getMaxPerks()) {
            player.sendMessage(ChatColor.RED + "You already have the maximum number of perks!");
            player.closeInventory();
            return;
        }

        PerkTier tier = switch (slot) {
            case 11 -> PerkTier.SILVER;
            case 13 -> PerkTier.GOLD;
            case 15 -> PerkTier.PRISMATIC;
            default -> null;
        };

        if (tier == null) return;

        if (gold < tier.getCost()) {
            player.sendMessage(ChatColor.RED + "Not enough gold! Need " + tier.getCost() + ", you have " + gold);
            return;
        }

        openPerkOptions(player, tier);
    }

    private void handleOptionClick(Player player, int slot) {
        UUID uuid = player.getUniqueId();
        ShopState state = shopStates.get(uuid);
        if (state == null) return;

        // Cancel button
        if (slot == 22) {
            player.closeInventory();
            shopStates.remove(uuid);
            return;
        }

        int perkIndex = switch (slot) {
            case 11 -> 0;
            case 13 -> 1;
            case 15 -> 2;
            default -> -1;
        };

        if (perkIndex < 0 || perkIndex >= state.offeredPerks.size()) return;

        Perk selectedPerk = state.offeredPerks.get(perkIndex);

        // Verify player can still afford it
        if (!economyManager.removeGold(uuid, state.selectedTier.getCost())) {
            player.sendMessage(ChatColor.RED + "Not enough gold!");
            player.closeInventory();
            shopStates.remove(uuid);
            return;
        }

        // Add perk
        if (perkManager.addPerkToPlayer(uuid, selectedPerk)) {
            player.sendMessage(ChatColor.GREEN + "Perk acquired: " + selectedPerk.getTier().getColor() + selectedPerk.getDisplayName());
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        } else {
            // Refund if can't add (shouldn't happen but safety)
            economyManager.addGold(uuid, state.selectedTier.getCost());
            player.sendMessage(ChatColor.RED + "Could not add perk!");
        }

        player.closeInventory();
        shopStates.remove(uuid);
    }
}

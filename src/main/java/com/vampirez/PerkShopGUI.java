package com.vampirez;

import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Two-screen perk shop using triumph-gui:
 *   1. Tier selection (Silver / Gold / Prismatic) + Repair Armor + Stat Anvil + owned perks
 *   2. Random perk options (3 per roll) + cancel
 *
 * <p>Click handlers are bound directly to {@link GuiItem}s, eliminating string-title routing,
 * magic slot numbers, and the per-player {@code shopStates} map (state lives in the click
 * lambda's closure now).
 */
public class PerkShopGUI {

    private final PerkManager perkManager;
    private final EconomyManager economyManager;
    private final GameManager gameManager;
    private final StatAnvilManager statAnvilManager;

    public PerkShopGUI(PerkManager perkManager, EconomyManager economyManager, GameManager gameManager, StatAnvilManager statAnvilManager) {
        this.perkManager = perkManager;
        this.economyManager = economyManager;
        this.gameManager = gameManager;
        this.statAnvilManager = statAnvilManager;
    }

    public void openTierSelection(Player player, PerkTeam team) {
        UUID uuid = player.getUniqueId();
        int gold = economyManager.getGold(uuid);
        int perkCount = perkManager.getPlayerPerkCount(uuid);

        Gui gui = Gui.gui()
                .title(Component.text("Perk Shop").color(NamedTextColor.GOLD))
                .rows(6)
                .disableAllInteractions()
                .create();

        // Row 1-3: Tier buttons at slots 11, 13, 15
        gui.setItem(11, tierItem(PerkTier.SILVER, gold, perkCount, player, team));
        gui.setItem(13, tierItem(PerkTier.GOLD, gold, perkCount, player, team));
        gui.setItem(15, tierItem(PerkTier.PRISMATIC, gold, perkCount, player, team));

        // Row 4 (slots 27-35): Separator + label, with Repair (29), label (31), Stat Anvil (33)
        ItemStack pane = paneItem();
        for (int i = 27; i <= 35; i++) gui.setItem(i, new GuiItem(pane));
        gui.setItem(29, repairItem(player));
        gui.setItem(31, labelItem());
        gui.setItem(33, statAnvilItem(player));

        // Row 5-6 (slots 36-53): owned perks display
        int slot = 36;
        for (Perk perk : perkManager.getPlayerPerks(uuid)) {
            if (slot > 53) break;
            gui.setItem(slot, new GuiItem(ownedPerkItem(perk, uuid)));
            slot++;
        }

        gui.open(player);
    }

    private GuiItem tierItem(PerkTier tier, int gold, int perkCount, Player player, PerkTeam team) {
        boolean canAfford = gold >= tier.getCost();
        boolean maxed = perkCount >= perkManager.getMaxPerks();

        Material mat = (maxed || !canAfford) ? Material.GRAY_STAINED_GLASS_PANE : switch (tier) {
            case SILVER -> Material.IRON_INGOT;
            case GOLD -> Material.GOLD_INGOT;
            case PRISMATIC -> Material.DIAMOND;
        };

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(tier.getDisplayName() + " Perk")
                    .color(tier.getTextColor()).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty().decoration(TextDecoration.ITALIC, false)
                    .append(Component.text("Cost: ").color(NamedTextColor.YELLOW))
                    .append(Component.text(tier.getCost() + " gold").color(NamedTextColor.GREEN)));
            lore.add(Component.empty().decoration(TextDecoration.ITALIC, false)
                    .append(Component.text("Your Gold: ").color(NamedTextColor.GRAY))
                    .append(Component.text(String.valueOf(gold)).color(NamedTextColor.WHITE)));
            lore.add(Component.empty());
            if (maxed) {
                lore.add(Component.text("Max perks reached! (" + perkCount + "/" + perkManager.getMaxPerks() + ")")
                        .color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
            } else if (!canAfford) {
                lore.add(Component.text("Not enough gold!").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
            } else {
                lore.add(Component.text("Click to browse " + tier.getDisplayName() + " perks!")
                        .color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }

        return new GuiItem(item, event -> {
            if (maxed) {
                player.sendMessage(MM.parse("<red>You already have the maximum number of perks!"));
                player.closeInventory();
                return;
            }
            if (!canAfford) {
                player.sendMessage(MM.parse("<red>Not enough gold! Need " + tier.getCost() + ", you have " + gold));
                return;
            }
            openPerkOptions(player, tier, team);
        });
    }

    private GuiItem repairItem(Player player) {
        ItemStack item = new ItemStack(Material.ANVIL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Repair Armor").color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.empty().decoration(TextDecoration.ITALIC, false)
                            .append(Component.text("Cost: ").color(NamedTextColor.YELLOW))
                            .append(Component.text("25 gold").color(NamedTextColor.GREEN)),
                    Component.text("Fully repairs all armor pieces").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return new GuiItem(item, event -> {
            UUID uuid = player.getUniqueId();
            if (!economyManager.removeGold(uuid, 25)) {
                player.sendMessage(MM.parse("<red>Not enough gold! Need 25, you have " + economyManager.getGold(uuid)));
                return;
            }
            for (ItemStack armor : player.getInventory().getArmorContents()) {
                if (armor != null && armor.getType() != Material.AIR) armor.setDurability((short) 0);
            }
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
            player.sendMessage(MM.parse("<green>Armor repaired!"));
            player.closeInventory();
        });
    }

    private GuiItem statAnvilItem(Player player) {
        UUID uuid = player.getUniqueId();
        ItemStack item = new ItemStack(Material.DAMAGED_ANVIL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Stat Anvil").color(NamedTextColor.LIGHT_PURPLE)
                    .decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty().decoration(TextDecoration.ITALIC, false)
                    .append(Component.text("Cost: ").color(NamedTextColor.YELLOW))
                    .append(Component.text(StatAnvilManager.ANVIL_COST + " gold each").color(NamedTextColor.GREEN)));
            lore.add(Component.text("Buy permanent stat boosts!").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("(Does not use perk slots)").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            List<Component> buffs = statAnvilManager.getBuffSummary(uuid);
            if (!buffs.isEmpty()) {
                lore.add(Component.empty());
                lore.add(Component.text("Your buffs:").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
                lore.addAll(buffs);
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return new GuiItem(item, event -> {
            player.closeInventory();
            statAnvilManager.openAnvilGUI(player);
        });
    }

    private GuiItem labelItem() {
        ItemStack item = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Your Active Perks").color(NamedTextColor.GREEN)
                    .decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return new GuiItem(item);
    }

    private ItemStack paneItem() {
        ItemStack pane = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty().decoration(TextDecoration.ITALIC, false));
            pane.setItemMeta(meta);
        }
        return pane;
    }

    private ItemStack ownedPerkItem(Perk perk, UUID playerUUID) {
        ItemStack item = new ItemStack(perk.getIcon());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(perk.getDisplayName())
                    .color(perk.getTier().getTextColor()).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            for (String line : perk.getDescription()) {
                lore.add(Component.text(line).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            }
            lore.add(Component.empty());
            lore.add(Component.text("--- Stats ---").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            Map<String, String> labels = perk.getStatLabels();
            Map<String, Double> stats = perk.getPlayerStats(playerUUID);
            if (labels.isEmpty()) {
                lore.add(Component.text("Active").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
            } else {
                for (Map.Entry<String, String> e : labels.entrySet()) {
                    double v = stats.getOrDefault(e.getKey(), 0.0);
                    String formatted = (v == Math.floor(v)) ? String.valueOf((int) v) : String.format("%.1f", v);
                    lore.add(Component.empty().decoration(TextDecoration.ITALIC, false)
                            .append(Component.text(e.getValue() + ": ").color(NamedTextColor.GRAY))
                            .append(Component.text(formatted).color(NamedTextColor.WHITE)));
                }
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void openPerkOptions(Player player, PerkTier tier, PerkTeam team) {
        UUID uuid = player.getUniqueId();
        List<Perk> options = perkManager.getRandomPerks(tier, team, 3, uuid);
        if (options.isEmpty()) {
            player.sendMessage(MM.parse("<red>No perks available in this tier!"));
            player.closeInventory();
            return;
        }

        Gui gui = Gui.gui()
                .title(Component.text("Choose a Perk").color(NamedTextColor.GOLD))
                .rows(3)
                .disableAllInteractions()
                .create();

        int[] slots = {11, 13, 15};
        for (int i = 0; i < options.size() && i < 3; i++) {
            Perk option = options.get(i);
            gui.setItem(slots[i], new GuiItem(option.createDisplayItem(), event -> buyPerk(player, option, tier)));
        }

        // Cancel button
        ItemStack cancel = new ItemStack(Material.BARRIER);
        ItemMeta cm = cancel.getItemMeta();
        if (cm != null) {
            cm.displayName(Component.text("Cancel").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
            cancel.setItemMeta(cm);
        }
        gui.setItem(22, new GuiItem(cancel, event -> player.closeInventory()));

        gui.open(player);
    }

    private void buyPerk(Player player, Perk perk, PerkTier tier) {
        UUID uuid = player.getUniqueId();
        if (!economyManager.removeGold(uuid, tier.getCost())) {
            player.sendMessage(MM.parse("<red>Not enough gold!"));
            player.closeInventory();
            return;
        }
        if (perkManager.addPerkToPlayer(uuid, perk, com.vampirez.api.event.PlayerPerkGainedEvent.Source.SHOP)) {
            player.sendMessage(Component.empty()
                    .append(MM.parse("<green>Perk acquired: "))
                    .append(Component.text(perk.getDisplayName()).color(perk.getTier().getTextColor())));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        } else {
            economyManager.addGold(uuid, tier.getCost()); // refund
            player.sendMessage(MM.parse("<red>Could not add perk!"));
        }
        player.closeInventory();
    }
}

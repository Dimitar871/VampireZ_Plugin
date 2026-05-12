package com.vampirez;

import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Admin perk-test browser. Built on triumph-gui's {@link PaginatedGui} so pagination + click
 * handling is library-managed; no string-title routing, no magic slot numbers.
 *
 * <p>Each page shows up to 45 perks (rows 0-4); the bottom row holds prev / clear-all / next /
 * info buttons. Clicking a perk toggles ownership for the viewing player.
 */
public class PerkTestGUI {

    private final PerkManager perkManager;

    public PerkTestGUI(PerkManager perkManager) {
        this.perkManager = perkManager;
    }

    public void openTestMenu(Player player, int page) {
        UUID uuid = player.getUniqueId();
        List<Perk> allPerks = new ArrayList<>(perkManager.getAllPerks());
        Set<String> ownedIds = new HashSet<>();
        for (Perk p : perkManager.getPlayerPerks(uuid)) ownedIds.add(p.getId());

        PaginatedGui gui = dev.triumphteam.gui.guis.Gui.paginated()
                .title(Component.text("Perk Test Menu").color(NamedTextColor.RED))
                .rows(6)
                .pageSize(45)
                .disableAllInteractions()
                .create();

        for (Perk perk : allPerks) {
            gui.addItem(buildPerkItem(perk, ownedIds, player, gui));
        }

        // Bottom row controls (slot indices 45-53 in the chest, but triumph-gui uses absolute slots)
        gui.setItem(46, prevPageItem(gui, player));
        gui.setItem(49, clearAllItem(player, gui));
        gui.setItem(51, nextPageItem(gui, player));
        gui.setItem(53, infoItem(uuid));

        // Set initial page (clamp to valid range)
        gui.open(player, Math.max(1, page + 1));
    }

    private GuiItem buildPerkItem(Perk perk, Set<String> ownedIds, Player player, PaginatedGui gui) {
        boolean isDisabled = perkManager.isDisabled(perk.getId());
        boolean owned = ownedIds.contains(perk.getId());
        ItemStack display = perk.createDisplayItem();
        if (isDisabled) display.setType(Material.BARRIER);

        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
            lore.add(teamLine(perk));
            if (isDisabled) {
                lore.add(Component.empty());
                lore.add(Component.text("⊘ DISABLED").color(NamedTextColor.RED).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Remove from disabled-perks in config.yml to enable").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            } else if (owned) {
                lore.add(Component.empty());
                lore.add(Component.text("OWNED - Click to remove").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
                meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            } else {
                lore.add(Component.empty());
                lore.add(Component.text("Click to add this perk").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(lore);
            display.setItemMeta(meta);
        }

        return new GuiItem(display, event -> {
            if (isDisabled) return;
            UUID uuid = player.getUniqueId();
            if (owned) {
                perkManager.removePerk(uuid, perk);
                player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(20.0);
                player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));
                perkManager.reapplyPerks(uuid);
                player.sendMessage(Component.empty()
                        .append(MM.parse("<red>Removed: "))
                        .append(Component.text(perk.getDisplayName()).color(perk.getTier().getTextColor())));
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            } else {
                if (perkManager.getPlayerPerkCount(uuid) >= perkManager.getMaxPerks()) {
                    player.sendMessage(MM.parse("<red>Max perks reached! Clear some first."));
                    return;
                }
                perkManager.addPerkToPlayer(uuid, perk);
                player.sendMessage(Component.empty()
                        .append(MM.parse("<green>Added: "))
                        .append(Component.text(perk.getDisplayName()).color(perk.getTier().getTextColor())));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            }
            // Refresh by re-opening on the same page (triumph-gui uses 1-indexed pages).
            int page = gui.getCurrentPageNum() - 1;
            openTestMenu(player, page);
        });
    }

    private GuiItem clearAllItem(Player player, PaginatedGui gui) {
        ItemStack item = new ItemStack(Material.TNT);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Clear All Perks").color(NamedTextColor.RED).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Removes all your active perks").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("and resets your stats").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return new GuiItem(item, event -> {
            UUID uuid = player.getUniqueId();
            perkManager.removeAllPerks(uuid);
            player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(20.0);
            player.setHealth(20.0);
            player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));
            player.sendMessage(MM.parse("<green>All perks cleared!"));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            openTestMenu(player, gui.getCurrentPageNum() - 1);
        });
    }

    private GuiItem prevPageItem(PaginatedGui gui, Player player) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Previous Page").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return new GuiItem(item, event -> gui.previous());
    }

    private GuiItem nextPageItem(PaginatedGui gui, Player player) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Next Page").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return new GuiItem(item, event -> gui.next());
    }

    private GuiItem infoItem(UUID uuid) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            int count = perkManager.getPlayerPerkCount(uuid);
            meta.displayName(Component.empty().decoration(TextDecoration.ITALIC, false)
                    .append(Component.text("Your Perks: ").color(NamedTextColor.GOLD))
                    .append(Component.text(count + "/" + perkManager.getMaxPerks()).color(NamedTextColor.WHITE)));
            List<Component> lore = new ArrayList<>();
            for (Perk perk : perkManager.getPlayerPerks(uuid)) {
                lore.add(Component.text(" - " + perk.getDisplayName())
                        .color(perk.getTier().getTextColor())
                        .decoration(TextDecoration.ITALIC, false));
            }
            if (lore.isEmpty()) lore.add(Component.text("No perks selected").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return new GuiItem(item);
    }

    private static Component teamLine(Perk perk) {
        NamedTextColor color = switch (perk.getTeam()) {
            case HUMAN -> NamedTextColor.BLUE;
            case VAMPIRE -> NamedTextColor.RED;
            default -> NamedTextColor.GREEN;
        };
        String label = switch (perk.getTeam()) {
            case HUMAN -> "Human";
            case VAMPIRE -> "Vampire";
            default -> "Both Teams";
        };
        return Component.empty().decoration(TextDecoration.ITALIC, false)
                .append(Component.text("Team: ").color(NamedTextColor.YELLOW))
                .append(Component.text(label).color(color));
    }
}

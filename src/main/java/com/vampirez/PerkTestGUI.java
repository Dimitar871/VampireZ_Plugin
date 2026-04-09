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

public class PerkTestGUI implements Listener {

    private static final String GUI_TITLE = ChatColor.RED + "Perk Test Menu";
    private static final String PAGE_PREFIX = GUI_TITLE + " - Page ";

    private final PerkManager perkManager;

    // Store the perk list in display order for slot mapping
    private final Map<UUID, Integer> playerPages = new HashMap<>();

    public PerkTestGUI(PerkManager perkManager) {
        this.perkManager = perkManager;
    }

    public void openTestMenu(Player player, int page) {
        List<Perk> allPerks = new ArrayList<>(perkManager.getAllPerks());
        int perksPerPage = 45; // 5 rows of 9, bottom row for controls
        int totalPages = (int) Math.ceil((double) allPerks.size() / perksPerPage);
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        playerPages.put(player.getUniqueId(), page);

        String title = allPerks.size() <= perksPerPage ? GUI_TITLE : PAGE_PREFIX + (page + 1);
        Inventory inv = Bukkit.createInventory(null, 54, title);

        // Fill perk slots (rows 0-4, slots 0-44)
        int startIndex = page * perksPerPage;
        Set<String> ownedIds = new HashSet<>();
        for (Perk p : perkManager.getPlayerPerks(player.getUniqueId())) {
            ownedIds.add(p.getId());
        }

        for (int i = 0; i < perksPerPage && (startIndex + i) < allPerks.size(); i++) {
            Perk perk = allPerks.get(startIndex + i);
            ItemStack display = perk.createDisplayItem();
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

                // Show team
                String teamStr;
                if (perk.getTeam() == PerkTeam.HUMAN) {
                    teamStr = ChatColor.BLUE + "Human";
                } else if (perk.getTeam() == PerkTeam.VAMPIRE) {
                    teamStr = ChatColor.RED + "Vampire";
                } else {
                    teamStr = ChatColor.GREEN + "Both Teams";
                }
                lore.add(ChatColor.YELLOW + "Team: " + teamStr);

                if (ownedIds.contains(perk.getId())) {
                    lore.add("");
                    lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "OWNED - Click to remove");
                    // Add enchant glow
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                    meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                } else {
                    lore.add("");
                    lore.add(ChatColor.YELLOW + "Click to add this perk");
                }
                meta.setLore(lore);
                display.setItemMeta(meta);
            }
            inv.setItem(i, display);
        }

        // Bottom row controls (row 5, slots 45-53)

        // Clear All Perks button - slot 49 (center of bottom row)
        ItemStack clearAll = new ItemStack(Material.TNT);
        ItemMeta clearMeta = clearAll.getItemMeta();
        if (clearMeta != null) {
            clearMeta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Clear All Perks");
            clearMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Removes all your active perks",
                ChatColor.GRAY + "and resets your stats"
            ));
            clearAll.setItemMeta(clearMeta);
        }
        inv.setItem(49, clearAll);

        // Perk count info - slot 53
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            int count = perkManager.getPlayerPerkCount(player.getUniqueId());
            infoMeta.setDisplayName(ChatColor.GOLD + "Your Perks: " + count + "/" + perkManager.getMaxPerks());
            List<String> infoLore = new ArrayList<>();
            for (Perk perk : perkManager.getPlayerPerks(player.getUniqueId())) {
                infoLore.add(perk.getTier().getColor() + " - " + perk.getDisplayName());
            }
            if (infoLore.isEmpty()) {
                infoLore.add(ChatColor.GRAY + "No perks selected");
            }
            infoMeta.setLore(infoLore);
            info.setItemMeta(infoMeta);
        }
        inv.setItem(53, info);

        // Previous page - slot 45
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prev.getItemMeta();
            if (prevMeta != null) {
                prevMeta.setDisplayName(ChatColor.YELLOW + "Previous Page");
                prev.setItemMeta(prevMeta);
            }
            inv.setItem(45, prev);
        }

        // Next page - slot 51 (was 53 but info is there)
        if (page < totalPages - 1) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = next.getItemMeta();
            if (nextMeta != null) {
                nextMeta.setDisplayName(ChatColor.YELLOW + "Next Page");
                next.setItemMeta(nextMeta);
            }
            inv.setItem(51, next);
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = event.getView().getTitle();
        if (!title.startsWith(GUI_TITLE) && !title.startsWith(PAGE_PREFIX)) return;

        event.setCancelled(true);

        int slot = event.getSlot();
        UUID uuid = player.getUniqueId();

        // Bottom row controls
        if (slot == 49) {
            // Clear All Perks
            perkManager.removeAllPerks(uuid);
            // Reset max health
            player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(20.0);
            player.setHealth(20.0);
            // Clear all potion effects
            player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));
            player.sendMessage(ChatColor.GREEN + "All perks cleared!");
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            // Reopen menu to refresh
            int page = playerPages.getOrDefault(uuid, 0);
            Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("VampireZ"), () -> {
                if (player.isOnline()) openTestMenu(player, page);
            }, 1L);
            return;
        }

        if (slot == 45) {
            // Previous page
            int page = playerPages.getOrDefault(uuid, 0);
            openTestMenu(player, page - 1);
            return;
        }

        if (slot == 51) {
            // Next page
            int page = playerPages.getOrDefault(uuid, 0);
            openTestMenu(player, page + 1);
            return;
        }

        // Perk slots (0-44)
        if (slot < 0 || slot > 44) return;

        List<Perk> allPerks = new ArrayList<>(perkManager.getAllPerks());
        int page = playerPages.getOrDefault(uuid, 0);
        int perkIndex = page * 45 + slot;
        if (perkIndex >= allPerks.size()) return;

        Perk clickedPerk = allPerks.get(perkIndex);

        // Check if already owned — if so, remove it
        boolean owned = perkManager.getPlayerPerks(uuid).stream()
                .anyMatch(p -> p.getId().equals(clickedPerk.getId()));

        if (owned) {
            perkManager.removePerk(uuid, clickedPerk);
            // Reset max health if it was a health-modifying perk, then reapply remaining
            player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(20.0);
            player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));
            perkManager.reapplyPerks(uuid);
            player.sendMessage(ChatColor.RED + "Removed: " + clickedPerk.getTier().getColor() + clickedPerk.getDisplayName());
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
        } else {
            if (perkManager.getPlayerPerkCount(uuid) >= perkManager.getMaxPerks()) {
                player.sendMessage(ChatColor.RED + "Max perks reached! Clear some first.");
                return;
            }
            perkManager.addPerkToPlayer(uuid, clickedPerk);
            player.sendMessage(ChatColor.GREEN + "Added: " + clickedPerk.getTier().getColor() + clickedPerk.getDisplayName());
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        }

        // Refresh the GUI
        Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("VampireZ"), () -> {
            if (player.isOnline()) openTestMenu(player, page);
        }, 1L);
    }
}

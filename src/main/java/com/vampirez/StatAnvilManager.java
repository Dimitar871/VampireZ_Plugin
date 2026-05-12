package com.vampirez;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class StatAnvilManager implements Listener {

    public static final int ANVIL_COST = 75;
    private static final Component ANVIL_GUI_TITLE = MM.parse("<dark_purple>Stat Anvil");

    private final EconomyManager economyManager;

    // Per-player stat buff counts
    private final Map<UUID, Map<StatBuff, Integer>> playerBuffs = new HashMap<>();

    // Offered buffs for current GUI session
    private final Map<UUID, List<StatBuff>> offeredBuffs = new HashMap<>();

    public enum StatBuff {
        HEARTS("+2 Hearts", Material.RED_DYE, "+2 max hearts (4 HP)"),
        DAMAGE("+10% Damage", Material.IRON_SWORD, "+10% melee damage dealt"),
        SPEED("+10% Speed", Material.SUGAR, "+10% movement speed"),
        ATTACK_SPEED("+15% Attack Speed", Material.GOLDEN_SWORD, "+15% attack speed"),
        LIFESTEAL("+10% Lifesteal", Material.GHAST_TEAR, "Heal 10% of damage dealt");

        public final String displayName;
        public final Material icon;
        public final String description;

        StatBuff(String displayName, Material icon, String description) {
            this.displayName = displayName;
            this.icon = icon;
            this.description = description;
        }
    }

    public StatAnvilManager(EconomyManager economyManager) {
        this.economyManager = economyManager;
    }

    public void openAnvilGUI(Player player) {
        UUID uuid = player.getUniqueId();
        int gold = economyManager.getGold(uuid);

        // Pick 3 random buffs
        List<StatBuff> pool = new ArrayList<>(Arrays.asList(StatBuff.values()));
        Collections.shuffle(pool);
        List<StatBuff> options = pool.subList(0, Math.min(3, pool.size()));
        offeredBuffs.put(uuid, new ArrayList<>(options));

        Inventory inv = Bukkit.createInventory(null, 27, ANVIL_GUI_TITLE);

        // Place buff options at slots 11, 13, 15
        int[] slots = {11, 13, 15};
        for (int i = 0; i < options.size(); i++) {
            StatBuff buff = options.get(i);
            int owned = getBuffCount(uuid, buff);
            inv.setItem(slots[i], createBuffItem(buff, owned, gold));
        }

        // Cancel button at slot 22
        ItemStack cancel = new ItemStack(Material.BARRIER);
        ItemMeta cancelMeta = cancel.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.displayName(Component.text("Cancel").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
            cancel.setItemMeta(cancelMeta);
        }
        inv.setItem(22, cancel);

        // Info item at slot 4
        ItemStack info = new ItemStack(Material.ANVIL);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.displayName(Component.text("Stat Anvil").color(NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
            infoMeta.lore(List.of(
                    Component.text("Purchase permanent stat boosts!").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.empty().decoration(TextDecoration.ITALIC, false)
                            .append(Component.text("Cost: ").color(NamedTextColor.YELLOW))
                            .append(Component.text(ANVIL_COST + " gold each").color(NamedTextColor.GREEN)),
                    Component.text("Does not use perk slots.").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
            ));
            info.setItemMeta(infoMeta);
        }
        inv.setItem(4, info);

        player.openInventory(inv);
    }

    private ItemStack createBuffItem(StatBuff buff, int ownedCount, int playerGold) {
        boolean canAfford = playerGold >= ANVIL_COST;

        ItemStack item = new ItemStack(canAfford ? buff.icon : Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(buff.displayName).color(NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(buff.description).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            lore.add(Component.empty().decoration(TextDecoration.ITALIC, false)
                    .append(Component.text("Cost: ").color(NamedTextColor.YELLOW))
                    .append(Component.text(ANVIL_COST + " gold").color(NamedTextColor.GREEN)));
            lore.add(Component.empty().decoration(TextDecoration.ITALIC, false)
                    .append(Component.text("Owned: ").color(NamedTextColor.GRAY))
                    .append(Component.text(String.valueOf(ownedCount)).color(NamedTextColor.WHITE)));
            lore.add(Component.empty());
            lore.add(canAfford
                    ? Component.text("Click to purchase!").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)
                    : Component.text("Not enough gold!").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().title().equals(ANVIL_GUI_TITLE)) return;

        event.setCancelled(true);

        UUID uuid = player.getUniqueId();
        List<StatBuff> options = offeredBuffs.get(uuid);
        if (options == null) return;

        // Cancel button
        if (event.getSlot() == 22) {
            player.closeInventory();
            offeredBuffs.remove(uuid);
            return;
        }

        int buffIndex = switch (event.getSlot()) {
            case 11 -> 0;
            case 13 -> 1;
            case 15 -> 2;
            default -> -1;
        };

        if (buffIndex < 0 || buffIndex >= options.size()) return;

        StatBuff selected = options.get(buffIndex);

        if (!economyManager.removeGold(uuid, ANVIL_COST)) {
            player.sendMessage(MM.parse("<red>Not enough gold! Need " + ANVIL_COST + "."));
            return;
        }

        // Apply buff
        addBuff(uuid, selected);
        applyBuffToPlayer(player, selected);

        player.sendMessage(MM.parse("<light_purple>Stat Anvil: <white>" + selected.displayName + "<green> acquired!"));
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.2f);
        player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.5);

        player.closeInventory();
        offeredBuffs.remove(uuid);
    }

    private void addBuff(UUID uuid, StatBuff buff) {
        playerBuffs.computeIfAbsent(uuid, k -> new EnumMap<>(StatBuff.class))
                .merge(buff, 1, Integer::sum);
    }

    private void applyBuffToPlayer(Player player, StatBuff buff) {
        switch (buff) {
            case HEARTS -> {
                double currentMax = player.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
                player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(currentMax + 4.0); // +2 hearts
                player.setHealth(Math.min(player.getHealth() + 4.0, currentMax + 4.0));
                // Track total applied HP for reapplyBuffs
                UUID uuid = player.getUniqueId();
                appliedHealth.merge(uuid, 4.0, Double::sum);
            }
            case SPEED -> {
                double currentSpeed = player.getAttribute(Attribute.MOVEMENT_SPEED).getBaseValue();
                player.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(currentSpeed * 1.10);
            }
            case ATTACK_SPEED -> {
                double currentAS = player.getAttribute(Attribute.ATTACK_SPEED).getBaseValue();
                player.getAttribute(Attribute.ATTACK_SPEED).setBaseValue(currentAS * 1.15);
            }
            case DAMAGE, LIFESTEAL -> {
                // These are handled dynamically in getDamageMult / getLifestealPercent
            }
        }
    }

    /**
     * Re-apply all attribute-based buffs (called on respawn/conversion).
     * Uses tracked totals to apply exact amounts on top of current base,
     * preventing double-stacking when called multiple times.
     */
    public void reapplyBuffs(Player player) {
        UUID uuid = player.getUniqueId();
        Map<StatBuff, Integer> buffs = playerBuffs.get(uuid);
        if (buffs == null) return;

        // For HEARTS: add exactly (count * 4.0) on top of the current base
        // First subtract any previously applied stat anvil HP, then add the correct amount
        int heartsCount = buffs.getOrDefault(StatBuff.HEARTS, 0);
        if (heartsCount > 0) {
            double previouslyApplied = appliedHealth.getOrDefault(uuid, 0.0);
            double currentBase = player.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
            double withoutAnvil = currentBase - previouslyApplied;
            double newBonus = heartsCount * 4.0;
            player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(withoutAnvil + newBonus);
            player.setHealth(Math.min(player.getHealth(), withoutAnvil + newBonus));
            appliedHealth.put(uuid, newBonus);
        }

        // For SPEED: apply multiplicative boost on vanilla base
        int speedCount = buffs.getOrDefault(StatBuff.SPEED, 0);
        if (speedCount > 0) {
            double base = 0.1; // vanilla walk speed
            for (int i = 0; i < speedCount; i++) {
                base *= 1.10;
            }
            player.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(base);
        }

        // For ATTACK_SPEED: apply multiplicative boost on vanilla base
        int asCount = buffs.getOrDefault(StatBuff.ATTACK_SPEED, 0);
        if (asCount > 0) {
            double base = 4.0; // vanilla attack speed
            for (int i = 0; i < asCount; i++) {
                base *= 1.15;
            }
            player.getAttribute(Attribute.ATTACK_SPEED).setBaseValue(base);
        }
    }

    // Track how much HP was added by stat anvil so we can subtract it before reapplying
    private final Map<UUID, Double> appliedHealth = new HashMap<>();

    public int getBuffCount(UUID uuid, StatBuff buff) {
        Map<StatBuff, Integer> buffs = playerBuffs.get(uuid);
        if (buffs == null) return 0;
        return buffs.getOrDefault(buff, 0);
    }

    /**
     * Get total damage multiplier from Damage buffs. e.g. 2 stacks = 1.20 (20% bonus)
     */
    public double getDamageMultiplier(UUID uuid) {
        int stacks = getBuffCount(uuid, StatBuff.DAMAGE);
        return 1.0 + (stacks * 0.10);
    }

    /**
     * Get total lifesteal percent from Lifesteal buffs. e.g. 2 stacks = 0.20 (20%)
     */
    public double getLifestealPercent(UUID uuid) {
        int stacks = getBuffCount(uuid, StatBuff.LIFESTEAL);
        return stacks * 0.10;
    }

    /**
     * Get summary of player's buffs for display.
     */
    public List<Component> getBuffSummary(UUID uuid) {
        Map<StatBuff, Integer> buffs = playerBuffs.get(uuid);
        if (buffs == null || buffs.isEmpty()) return Collections.emptyList();

        List<Component> lines = new ArrayList<>();
        for (Map.Entry<StatBuff, Integer> entry : buffs.entrySet()) {
            if (entry.getValue() > 0) {
                lines.add(Component.text(entry.getKey().displayName + " x" + entry.getValue())
                        .color(NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
            }
        }
        return lines;
    }

    public void resetAll() {
        playerBuffs.clear();
        offeredBuffs.clear();
    }

    public void resetPlayer(UUID uuid) {
        playerBuffs.remove(uuid);
        offeredBuffs.remove(uuid);
    }
}

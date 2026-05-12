package com.vampirez.engine.action;

import com.vampirez.MM;
import com.vampirez.engine.HookContext;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.util.List;
import java.util.Map;

/**
 * Adds an item (or stack) to the owner's inventory. Supports display name, lore,
 * enchantments, and potion type for SPLASH_POTION / TIPPED_ARROW / POTION items.
 *
 * Each enchantment in the list is a Map with keys "name" and "level".
 */
public class GiveItemAction implements Action {

    private final Material material;
    private final int amount;
    private final String displayName;
    private final List<String> lore;
    private final List<Map<?, ?>> enchants;
    private final PotionType potionType;

    public GiveItemAction(Material material, int amount, String displayName, List<String> lore,
                          List<Map<?, ?>> enchants, PotionType potionType) {
        this.material = material;
        this.amount = amount;
        this.displayName = displayName;
        this.lore = lore;
        this.enchants = enchants;
        this.potionType = potionType;
    }

    @Override
    public void run(HookContext ctx) {
        Player owner = ctx.owner;
        if (owner == null || material == null) return;

        ItemStack item = new ItemStack(material, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (displayName != null) {
                meta.displayName(MM.legacy(displayName));
            }
            if (lore != null && !lore.isEmpty()) {
                meta.lore(lore.stream().map(MM::legacy).toList());
            }
            if (enchants != null) {
                for (Map<?, ?> ench : enchants) {
                    Object nameRaw = ench.get("name");
                    Object levelRaw = ench.get("level");
                    if (nameRaw == null) continue;
                    String name = String.valueOf(nameRaw).toLowerCase();
                    NamespacedKey key = NamespacedKey.fromString(name.contains(":") ? name : "minecraft:" + name);
                    Enchantment enchantment = key == null ? null : Registry.ENCHANTMENT.get(key);
                    if (enchantment == null) continue;
                    int level = levelRaw instanceof Number ? ((Number) levelRaw).intValue() : 1;
                    meta.addEnchant(enchantment, level, true);
                }
            }
            if (meta instanceof PotionMeta pm && potionType != null) {
                pm.setBasePotionType(potionType);
            }
            item.setItemMeta(meta);
        }
        owner.getInventory().addItem(item);
    }
}

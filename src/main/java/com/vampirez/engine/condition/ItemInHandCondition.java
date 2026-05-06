package com.vampirez.engine.condition;

import com.vampirez.engine.HookContext;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * True if the owner is holding an item matching the given material (and optionally a name
 * substring). Used by right-click ability perks to gate their on_interact triggers.
 */
public class ItemInHandCondition implements Condition {

    private final Material material;
    private final String nameContains;

    public ItemInHandCondition(Material material, String nameContains) {
        this.material = material;
        this.nameContains = nameContains;
    }

    @Override
    public boolean test(HookContext ctx) {
        Player owner = ctx.owner;
        if (owner == null) return false;
        ItemStack item = owner.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) return false;
        if (material != null && item.getType() != material) return false;
        if (nameContains != null) {
            ItemMeta meta = item.getItemMeta();
            if (meta == null || !meta.hasDisplayName()) return false;
            if (!meta.getDisplayName().contains(nameContains)) return false;
        }
        return true;
    }
}

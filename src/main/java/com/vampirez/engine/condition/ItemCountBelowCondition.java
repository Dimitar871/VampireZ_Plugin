package com.vampirez.engine.condition;

import com.vampirez.engine.HookContext;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * True if the owner's inventory has fewer than {@code threshold} items of the given material.
 * Used by item-supply perks to gate their regen.
 */
public class ItemCountBelowCondition implements Condition {

    private final Material material;
    private final int threshold;

    public ItemCountBelowCondition(Material material, int threshold) {
        this.material = material;
        this.threshold = threshold;
    }

    @Override
    public boolean test(HookContext ctx) {
        Player owner = ctx.owner;
        if (owner == null || material == null) return false;
        int count = 0;
        for (ItemStack item : owner.getInventory().getContents()) {
            if (item != null && item.getType() == material) count += item.getAmount();
        }
        return count < threshold;
    }
}

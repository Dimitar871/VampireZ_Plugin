package com.vampirez.engine.condition;

import com.vampirez.ItemNames;
import com.vampirez.engine.HookContext;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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
            String name = ItemNames.plain(item);
            if (name.isEmpty() || !name.contains(nameContains)) return false;
        }
        return true;
    }
}

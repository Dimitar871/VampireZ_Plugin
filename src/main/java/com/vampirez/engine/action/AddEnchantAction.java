package com.vampirez.engine.action;

import com.vampirez.engine.HookContext;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Adds an enchantment to one or more items in the owner's inventory on apply.
 *
 * Target selection (exactly one must be set):
 *   - {@code materialName}   — exact Material match (e.g. "IRON_SWORD"), enchants the FIRST match
 *   - {@code materialContains} — Material name substring (e.g. "SWORD"), enchants the FIRST match
 *   - {@code targetArmor}     — true to enchant all four armor slots
 *
 * No corresponding remove action: the original Java perks left enchanted gear in place when
 * the perk was removed, and we mirror that.
 */
public class AddEnchantAction implements Action {

    private final String materialName;
    private final String materialContains;
    private final boolean targetArmor;
    private final Enchantment enchantment;
    private final int level;

    public AddEnchantAction(String materialName, String materialContains, boolean targetArmor,
                            Enchantment enchantment, int level) {
        this.materialName = materialName;
        this.materialContains = materialContains;
        this.targetArmor = targetArmor;
        this.enchantment = enchantment;
        this.level = level;
    }

    @Override
    public void run(HookContext ctx) {
        Player owner = ctx.owner;
        if (owner == null || enchantment == null) return;

        if (targetArmor) {
            ItemStack[] armor = owner.getInventory().getArmorContents();
            boolean changed = false;
            for (ItemStack piece : armor) {
                if (piece != null && piece.getType() != Material.AIR
                        && piece.getEnchantmentLevel(enchantment) < level) {
                    piece.addUnsafeEnchantment(enchantment, level);
                    changed = true;
                }
            }
            if (changed) owner.getInventory().setArmorContents(armor);
            return;
        }

        for (ItemStack item : owner.getInventory().getContents()) {
            if (item == null || item.getType() == Material.AIR) continue;
            String name = item.getType().name();
            boolean match = (materialName != null && name.equals(materialName))
                    || (materialContains != null && name.contains(materialContains));
            if (match) {
                if (item.getEnchantmentLevel(enchantment) < level) {
                    item.addUnsafeEnchantment(enchantment, level);
                }
                return;
            }
        }
    }
}

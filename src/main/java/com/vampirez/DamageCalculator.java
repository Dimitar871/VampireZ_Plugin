package com.vampirez;

import org.bukkit.Material;

/**
 * Pure functions for the custom PvP damage formula. GameListener gathers the
 * inputs (weapon, cooldown, armor points, Protection levels) and this class does
 * the math, so the formula itself is unit-testable without a Bukkit server.
 *
 * <p>Formula: team base damage (+ weapon material and Sharpness bonuses), scaled
 * by the vanilla attack-cooldown charge, then reduced by 30% of the vanilla
 * armor reduction and by 4% per Protection level.
 */
public final class DamageCalculator {

    public static final double HUMAN_BASE_DAMAGE = 5.0;
    public static final double VAMPIRE_BASE_DAMAGE = 4.0;
    public static final double DIAMOND_WEAPON_BONUS = 0.5;
    public static final double NETHERITE_WEAPON_BONUS = 1.0;
    public static final double SHARPNESS_BONUS_PER_LEVEL = 0.5;
    /** Only this fraction of the vanilla armor reduction applies. */
    public static final double ARMOR_REDUCTION_FACTOR = 0.3;
    /** Vanilla cap: armor can reduce at most 80% of damage. */
    public static final double MAX_ARMOR_REDUCTION = 0.8;
    public static final double PROTECTION_REDUCTION_PER_LEVEL = 0.04;
    public static final double MAX_PROTECTION_REDUCTION = 0.80;

    private DamageCalculator() {}

    /**
     * The custom formula only applies to swords and axes in the main hand.
     * endsWith("_AXE") deliberately excludes pickaxes — the old contains("AXE")
     * let a map-loot pickaxe hit with full formula damage.
     */
    public static boolean isMeleeWeapon(Material material) {
        String name = material.name();
        return name.contains("SWORD") || name.endsWith("_AXE");
    }

    /**
     * Computes the final PvP damage.
     *
     * @param attackerIsHuman      humans hit for 5.0 HP base, vampires for 4.0
     * @param weapon               main-hand item (diamond/netherite tier adds a bonus)
     * @param sharpnessLevel       Sharpness level on the weapon (0 if none)
     * @param attackCooldown       vanilla swing charge, 0.0–1.0
     * @param victimArmorPoints    victim's armor attribute value (may include stored
     *                             armor for Phantom Step / Bat Form)
     * @param totalProtectionLevels sum of Protection levels across the victim's armor
     */
    public static double computeDamage(boolean attackerIsHuman,
                                       Material weapon,
                                       int sharpnessLevel,
                                       double attackCooldown,
                                       double victimArmorPoints,
                                       int totalProtectionLevels) {
        double baseDamage = attackerIsHuman ? HUMAN_BASE_DAMAGE : VAMPIRE_BASE_DAMAGE;

        String weaponName = weapon.name();
        if (weaponName.contains("DIAMOND")) {
            baseDamage += DIAMOND_WEAPON_BONUS;
        } else if (weaponName.contains("NETHERITE")) {
            baseDamage += NETHERITE_WEAPON_BONUS;
        }

        baseDamage += sharpnessLevel * SHARPNESS_BONUS_PER_LEVEL;
        baseDamage *= attackCooldown;

        // Vanilla formula: armor reduces damage by armorPoints / 25 (capped at 80%),
        // but we only apply 30% of that reduction.
        double armorReduction = Math.min(victimArmorPoints / 25.0, MAX_ARMOR_REDUCTION);
        double afterArmor = baseDamage * (1.0 - armorReduction * ARMOR_REDUCTION_FACTOR);

        // Each Protection level reduces damage by 4% (capped at 80%).
        double protReduction = Math.min(totalProtectionLevels * PROTECTION_REDUCTION_PER_LEVEL,
                MAX_PROTECTION_REDUCTION);
        return afterArmor * (1.0 - protReduction);
    }
}

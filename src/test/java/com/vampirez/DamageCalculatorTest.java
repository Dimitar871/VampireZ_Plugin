package com.vampirez;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the custom PvP damage formula. These numbers are the documented game
 * balance (WIKI → Combat System); if a change here is intentional, update the
 * WIKI alongside the constant.
 */
class DamageCalculatorTest {

    private static final double EPS = 1e-9;

    // ===== weapon gate =====

    @Test
    void swordsAndAxesAreWeapons() {
        assertTrue(DamageCalculator.isMeleeWeapon(Material.IRON_SWORD));
        assertTrue(DamageCalculator.isMeleeWeapon(Material.STONE_SWORD));
        assertTrue(DamageCalculator.isMeleeWeapon(Material.NETHERITE_AXE));
        assertTrue(DamageCalculator.isMeleeWeapon(Material.WOODEN_AXE));
    }

    @Test
    void nonWeaponsAreNot() {
        assertFalse(DamageCalculator.isMeleeWeapon(Material.BOW));
        assertFalse(DamageCalculator.isMeleeWeapon(Material.STICK));
        assertFalse(DamageCalculator.isMeleeWeapon(Material.AIR));
        // Pickaxes matched via the "AXE" substring until v2.5.1 and hit with full
        // formula damage — deliberately excluded now (balance decision).
        assertFalse(DamageCalculator.isMeleeWeapon(Material.IRON_PICKAXE));
        assertFalse(DamageCalculator.isMeleeWeapon(Material.NETHERITE_PICKAXE));
    }

    // ===== base damage =====

    @Test
    void humanBaseDamageIs5() {
        assertEquals(5.0, damage(true, Material.IRON_SWORD, 0, 1.0, 0, 0), EPS);
    }

    @Test
    void vampireBaseDamageIs4() {
        assertEquals(4.0, damage(false, Material.STONE_SWORD, 0, 1.0, 0, 0), EPS);
    }

    // ===== weapon material bonus =====

    @Test
    void diamondWeaponAddsHalfHp() {
        assertEquals(5.5, damage(true, Material.DIAMOND_SWORD, 0, 1.0, 0, 0), EPS);
    }

    @Test
    void netheriteWeaponAddsOneHp() {
        assertEquals(6.0, damage(true, Material.NETHERITE_SWORD, 0, 1.0, 0, 0), EPS);
    }

    // ===== sharpness =====

    @Test
    void sharpnessAddsHalfHpPerLevel() {
        assertEquals(5.5, damage(true, Material.IRON_SWORD, 1, 1.0, 0, 0), EPS);
        assertEquals(6.0, damage(true, Material.IRON_SWORD, 2, 1.0, 0, 0), EPS);
    }

    // ===== attack cooldown =====

    @Test
    void halfChargedSwingDealsHalfDamage() {
        assertEquals(2.5, damage(true, Material.IRON_SWORD, 0, 0.5, 0, 0), EPS);
    }

    @Test
    void zeroChargeDealsZero() {
        assertEquals(0.0, damage(true, Material.IRON_SWORD, 0, 0.0, 0, 0), EPS);
    }

    // ===== armor =====

    @Test
    void armorOnlyApplies30PercentOfVanillaReduction() {
        // Full iron armor = 15 armor points → vanilla reduction 15/25 = 0.6
        // applied at 30% → ×(1 − 0.6×0.3) = ×0.82
        assertEquals(5.0 * 0.82, damage(true, Material.IRON_SWORD, 0, 1.0, 15, 0), EPS);
    }

    @Test
    void armorReductionIsCappedAt80Percent() {
        // 25+ armor points would exceed the vanilla 0.8 cap → ×(1 − 0.8×0.3) = ×0.76
        double capped = damage(true, Material.IRON_SWORD, 0, 1.0, 30, 0);
        assertEquals(5.0 * 0.76, capped, EPS);
        // More armor beyond the cap changes nothing
        assertEquals(capped, damage(true, Material.IRON_SWORD, 0, 1.0, 100, 0), EPS);
    }

    // ===== protection =====

    @Test
    void protectionReduces4PercentPerLevel() {
        // Protection I on all 4 pieces = 4 levels → 16% reduction
        assertEquals(5.0 * 0.84, damage(true, Material.IRON_SWORD, 0, 1.0, 0, 4), EPS);
    }

    @Test
    void protectionIsCappedAt80Percent() {
        double capped = damage(true, Material.IRON_SWORD, 0, 1.0, 0, 20);
        assertEquals(5.0 * 0.20, capped, EPS);
        assertEquals(capped, damage(true, Material.IRON_SWORD, 0, 1.0, 0, 50), EPS);
    }

    // ===== realistic combined scenario =====

    @Test
    void humanVsFullIronProt1VampireScenario() {
        // Human, iron sword Sharpness I, full swing vs full iron (15 pts) + Prot I ×4:
        // (5.0 + 0.5) × (1 − 0.6×0.3) × (1 − 0.16) = 5.5 × 0.82 × 0.84
        assertEquals(5.5 * 0.82 * 0.84, damage(true, Material.IRON_SWORD, 1, 1.0, 15, 4), EPS);
    }

    @Test
    void formulaNeverGoesNegative() {
        assertTrue(damage(false, Material.WOODEN_SWORD, 0, 0.1, 100, 50) >= 0);
    }

    private static double damage(boolean human, Material weapon, int sharpness,
                                 double cooldown, double armor, int protection) {
        return DamageCalculator.computeDamage(human, weapon, sharpness, cooldown, armor, protection);
    }
}

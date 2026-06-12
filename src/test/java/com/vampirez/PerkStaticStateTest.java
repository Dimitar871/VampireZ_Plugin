package com.vampirez;

import com.vampirez.perks.BlackCleaverPerk;
import com.vampirez.perks.CurseOfDecayPerk;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards against the bug class behind v2.3.1 (perks persisting across games):
 * perk state held in static collections is NOT cleared by remove(player) — that
 * only runs for online perk owners. Any perk keeping static Map/Set/List state
 * must override clearGlobalState(), which PerkManager.resetAll() calls for every
 * registered perk at game end.
 */
class PerkStaticStateTest {

    /**
     * Perk classes allowed to keep static collections WITHOUT a clearGlobalState
     * override — immutable lookup constants only. Add a class here only if its
     * static collections are never mutated after class initialization.
     */
    private static final Set<String> CONSTANT_ONLY_ALLOWLIST = Set.of(
            "PlagueCarrierPerk" // NEGATIVE_EFFECTS — fixed lookup table of potion types
    );

    @Test
    void everyPerkWithStaticCollectionsOverridesClearGlobalState() throws Exception {
        List<String> violations = new ArrayList<>();

        for (Class<?> perkClass : loadPerkClasses()) {
            if (CONSTANT_ONLY_ALLOWLIST.contains(perkClass.getSimpleName())) continue;

            boolean hasStaticCollection = false;
            for (Field field : perkClass.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) continue;
                Class<?> type = field.getType();
                if (Map.class.isAssignableFrom(type) || Collection.class.isAssignableFrom(type)) {
                    hasStaticCollection = true;
                    break;
                }
            }
            if (!hasStaticCollection) continue;

            Class<?> declaring = perkClass.getMethod("clearGlobalState").getDeclaringClass();
            if (declaring == Perk.class) {
                violations.add(perkClass.getSimpleName());
            }
        }

        assertTrue(violations.isEmpty(),
                "These perk classes keep static Map/Set/List state but do not override "
                + "clearGlobalState(), so the state leaks into the next game: " + violations
                + ". Either override clearGlobalState() to clear the collections, or — if they "
                + "are immutable constants — add the class to CONSTANT_ONLY_ALLOWLIST.");
    }

    @Test
    void blackCleaverClearGlobalStateEmptiesSharedMaps() {
        UUID victim = UUID.randomUUID();
        BlackCleaverPerk.cleavedTargets.put(victim, new int[]{3});
        BlackCleaverPerk.cleavedExpiry.put(victim, System.currentTimeMillis() + 60_000);

        new BlackCleaverPerk().clearGlobalState();

        assertTrue(BlackCleaverPerk.cleavedTargets.isEmpty(), "cleaver stacks must not survive a game reset");
        assertTrue(BlackCleaverPerk.cleavedExpiry.isEmpty(), "cleaver expiries must not survive a game reset");
        assertEquals(0, BlackCleaverPerk.getStacks(victim));
    }

    @Test
    void curseOfDecayClearGlobalStateEmptiesCurseMap() {
        UUID victim = UUID.randomUUID();
        CurseOfDecayPerk.cursedPlayers.put(victim, System.currentTimeMillis());

        new CurseOfDecayPerk().clearGlobalState();

        assertTrue(CurseOfDecayPerk.cursedPlayers.isEmpty(), "curses must not survive a game reset");
    }

    /**
     * Loads every class in com.vampirez.perks WITHOUT running static initializers
     * (some touch Bukkit registries that don't exist in unit tests).
     */
    private static List<Class<?>> loadPerkClasses() throws Exception {
        ClassLoader loader = PerkStaticStateTest.class.getClassLoader();
        URL packageUrl = loader.getResource("com/vampirez/perks");
        assertNotNull(packageUrl, "perks package not found on test classpath");

        File dir = new File(packageUrl.toURI());
        File[] classFiles = dir.listFiles((d, name) -> name.endsWith(".class") && !name.contains("$"));
        assertNotNull(classFiles);
        assertTrue(classFiles.length > 50, "expected the full perk package, found only " + classFiles.length);

        List<Class<?>> classes = new ArrayList<>();
        for (File file : classFiles) {
            String className = "com.vampirez.perks." + file.getName().replace(".class", "");
            Class<?> c = Class.forName(className, false, loader);
            if (Perk.class.isAssignableFrom(c) && !Modifier.isAbstract(c.getModifiers())) {
                classes.add(c);
            }
        }
        return classes;
    }
}

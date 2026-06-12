package com.vampirez;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Doc-drift guard: WIKI.md is the single source of truth for the perk catalogue,
 * and this test keeps it honest. It cross-checks three artifacts as text:
 *
 *   1. Java perks registered in VampireZPlugin.registerAllPerks (pm.registerPerk calls)
 *   2. YAML perks defined in perks.yml
 *   3. The WIKI.md catalogue (intro perk count, tier headers, table rows)
 *
 * History: before this test, README said 166 AND 145, WIKI said 144 with tier
 * headers summing to 146, and the code actually registered 167 — including one
 * perk (Diamond Edge) documented in the WIKI but unobtainable in game.
 * If this test fails, fix the WIKI (or the registration), not the test.
 */
class PerkCatalogueSyncTest {

    private enum Tier { SILVER, GOLD, PRISMATIC }

    private static final Path PLUGIN_SRC = Paths.get("src/main/java/com/vampirez/VampireZPlugin.java");
    private static final Path PERKS_DIR = Paths.get("src/main/java/com/vampirez/perks");
    private static final Path PERKS_YML = Paths.get("src/main/resources/perks.yml");
    private static final Path WIKI = Paths.get("WIKI.md");

    /** display name → tier, for every perk that is actually obtainable in game. */
    private static final Map<String, Tier> live = new HashMap<>();
    /** display name → tier, for every row in the WIKI catalogue tables. */
    private static final Map<String, Tier> wikiRows = new HashMap<>();
    /** tier → count claimed in the WIKI "### X Tier (N perks · …)" headers. */
    private static final Map<Tier, Integer> wikiHeaderCounts = new EnumMap<>(Tier.class);
    private static int wikiIntroCount = -1;

    @BeforeAll
    static void parseEverything() throws IOException {
        parseLivePerks();
        parseWiki();
    }

    // ===== assertions =====

    @Test
    void everyLivePerkHasExactlyOneWikiRow() {
        List<String> missing = live.keySet().stream()
                .filter(name -> !wikiRows.containsKey(name))
                .sorted().collect(Collectors.toList());
        assertTrue(missing.isEmpty(),
                "Perks registered in code but missing from the WIKI catalogue: " + missing
                + ". Add a row to the matching tier table in WIKI.md.");
    }

    @Test
    void everyWikiRowIsALivePerk() {
        List<String> ghosts = wikiRows.keySet().stream()
                .filter(name -> !live.containsKey(name))
                .sorted().collect(Collectors.toList());
        assertTrue(ghosts.isEmpty(),
                "WIKI catalogue lists perks that are NOT registered in code (unobtainable): " + ghosts
                + ". Remove the row or register the perk. (This is how Diamond Edge slipped through.)");
    }

    @Test
    void wikiRowsAreInTheCorrectTierSection() {
        List<String> misplaced = new ArrayList<>();
        for (Map.Entry<String, Tier> e : wikiRows.entrySet()) {
            Tier actual = live.get(e.getKey());
            if (actual != null && actual != e.getValue()) {
                misplaced.add(e.getKey() + " (wiki: " + e.getValue() + ", code: " + actual + ")");
            }
        }
        assertTrue(misplaced.isEmpty(), "WIKI rows in the wrong tier section: " + misplaced);
    }

    @Test
    void wikiTierHeaderCountsMatchActualRows() {
        for (Tier tier : Tier.values()) {
            long rows = wikiRows.values().stream().filter(t -> t == tier).count();
            assertEquals(wikiHeaderCounts.get(tier), (int) rows,
                    "WIKI '" + tier + " Tier (N perks …)' header disagrees with the number of rows "
                    + "in that section — update the header.");
        }
    }

    @Test
    void wikiIntroCountMatchesLivePerkTotal() {
        assertEquals(live.size(), wikiIntroCount,
                "WIKI intro says 'full " + wikiIntroCount + "-perk catalogue' but the code registers "
                + live.size() + " perks — update the intro line.");
    }

    // ===== parsing =====

    private static void parseLivePerks() throws IOException {
        // 1. Class names registered in VampireZPlugin
        String pluginSrc = Files.readString(PLUGIN_SRC, StandardCharsets.UTF_8);
        Matcher reg = Pattern.compile("pm\\.registerPerk\\(new (\\w+)\\(").matcher(pluginSrc);
        List<String> registeredClasses = new ArrayList<>();
        while (reg.find()) {
            registeredClasses.add(reg.group(1));
        }
        assertTrue(registeredClasses.size() > 100, "expected 100+ registered Java perks, found "
                + registeredClasses.size() + " — did registerAllPerks move?");

        // 2. Class → (display name, tier) from each perk source file's super(...) call
        Pattern superCall = Pattern.compile(
                "super\\(\\s*\"[^\"]+\",\\s*\"([^\"]+)\",\\s*PerkTier\\.(\\w+)", Pattern.DOTALL);
        for (String className : registeredClasses) {
            Path src = PERKS_DIR.resolve(className + ".java");
            assertTrue(Files.exists(src), "Registered perk class has no source file: " + className);
            Matcher m = superCall.matcher(Files.readString(src, StandardCharsets.UTF_8));
            assertTrue(m.find(), "Could not parse super(id, name, tier, …) in " + className);
            putLive(m.group(1), Tier.valueOf(m.group(2)), className);
        }

        // 3. YAML perks
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(PERKS_YML.toFile());
        for (String key : yaml.getKeys(false)) {
            String name = yaml.getString(key + ".name");
            String tier = yaml.getString(key + ".tier");
            assertTrue(name != null && tier != null, "perks.yml entry '" + key + "' missing name/tier");
            putLive(name, Tier.valueOf(tier.toUpperCase()), "perks.yml:" + key);
        }
    }

    private static void putLive(String name, Tier tier, String source) {
        Tier previous = live.put(name.trim(), tier);
        assertTrue(previous == null,
                "Duplicate live perk display name '" + name + "' (second source: " + source + "). "
                + "Two obtainable perks must not share a display name — the WIKI can't tell them apart.");
    }

    private static void parseWiki() throws IOException {
        Pattern intro = Pattern.compile("full (\\d+)-perk catalogue");
        Pattern header = Pattern.compile("^### (Silver|Gold|Prismatic) Tier \\((\\d+) perks");
        Pattern row = Pattern.compile("^\\| <img src=\"images/perks/[^|]+\\| [^|]+ \\| ([^|]+) \\|");

        Tier section = null;
        for (String line : Files.readAllLines(WIKI, StandardCharsets.UTF_8)) {
            Matcher mi = intro.matcher(line);
            if (wikiIntroCount < 0 && mi.find()) {
                wikiIntroCount = Integer.parseInt(mi.group(1));
            }
            Matcher mh = header.matcher(line);
            if (mh.find()) {
                section = Tier.valueOf(mh.group(1).toUpperCase());
                wikiHeaderCounts.put(section, Integer.parseInt(mh.group(2)));
                continue;
            }
            if (line.startsWith("### ") || line.startsWith("## ")) {
                section = null; // left the catalogue tier tables
                continue;
            }
            Matcher mr = row.matcher(line);
            if (section != null && mr.find()) {
                String name = mr.group(1).trim();
                Tier previous = wikiRows.put(name, section);
                assertTrue(previous == null, "Duplicate WIKI catalogue row: " + name);
            }
        }

        assertTrue(wikiIntroCount > 0, "Could not find 'full N-perk catalogue' in the WIKI intro");
        assertEquals(3, wikiHeaderCounts.size(), "Expected Silver/Gold/Prismatic tier headers in WIKI");
        assertFalse(wikiRows.isEmpty(), "Found no catalogue rows in WIKI.md — did the table format change?");
    }
}

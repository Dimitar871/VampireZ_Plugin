package com.vampirez;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * On-disk rules for arena worlds. Paper 26.1+ stores every world under
 * {@code <level>/dimensions/<namespace>/<key>}; a world keyed {@code vampirez:arena_N}
 * is named {@code vampirez_arena_N}.
 */
public final class ArenaPaths {

    public static final String NAMESPACE = "vampirez";
    private static final String KEY_PREFIX = "arena_";
    private static final String WORLD_NAME_PREFIX = NAMESPACE + "_" + KEY_PREFIX;

    private ArenaPaths() {}

    public static Path arenaRoot(Path levelDirectory) {
        return levelDirectory.resolve("dimensions").resolve(NAMESPACE);
    }

    public static String keyValue(int index) {
        return KEY_PREFIX + index;
    }

    public static boolean isArenaWorldName(String worldName) {
        return worldName.startsWith(WORLD_NAME_PREFIX);
    }

    /** Pre-26.1 templates are standalone worlds with their own level.dat. */
    public static boolean isLegacyTemplate(Path template) {
        return Files.exists(template.resolve("level.dat"));
    }

    public static boolean hasRegionData(Path template) {
        return Files.isDirectory(template.resolve("region"));
    }
}

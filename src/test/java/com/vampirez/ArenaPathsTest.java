package com.vampirez;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ArenaPathsTest {

    @TempDir
    Path dir;

    @Test
    void legacyTemplateHasLevelDat() throws IOException {
        Files.createDirectories(dir.resolve("region"));
        Files.createFile(dir.resolve("level.dat"));
        assertTrue(ArenaPaths.isLegacyTemplate(dir));
        assertTrue(ArenaPaths.hasRegionData(dir));
    }

    @Test
    void dimensionTemplateIsNotLegacy() throws IOException {
        Files.createDirectories(dir.resolve("region"));
        Files.createDirectories(dir.resolve("data/paper"));
        assertFalse(ArenaPaths.isLegacyTemplate(dir));
        assertTrue(ArenaPaths.hasRegionData(dir));
    }

    @Test
    void emptyFolderHasNoRegionData() {
        assertFalse(ArenaPaths.hasRegionData(dir));
    }

    @Test
    void arenaRootIsPluginNamespaceUnderDimensions() {
        assertEquals(dir.resolve("dimensions").resolve("vampirez"), ArenaPaths.arenaRoot(dir));
    }

    @Test
    void keyValueIsNumbered() {
        assertEquals("arena_3", ArenaPaths.keyValue(3));
    }

    @Test
    void recognisesArenaWorldNames() {
        assertTrue(ArenaPaths.isArenaWorldName("vampirez_arena_2"));
        assertFalse(ArenaPaths.isArenaWorldName("world"));
        assertFalse(ArenaPaths.isArenaWorldName("vampirez_lobby"));
    }
}

package com.vampirez;

import com.vampirez.config.PluginConfig;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Difficulty;
import org.bukkit.GameRules;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Arena world lifecycle on Paper 26.1+. Each game copies the template into
 * {@code <level>/dimensions/vampirez/arena_N} and loads it by key; the previous
 * arena is unloaded and deleted. A pre-26.1 template is upgraded once (with a backup).
 */
public class ArenaManager {

    private static final Logger log = LoggerFactory.getLogger(ArenaManager.class);
    private static final String UPGRADE_WORLD = "vampirez_template_upgrade";
    private static final int BAKE_RADIUS_CHUNKS = 16;

    private final VampireZPlugin plugin;
    private final Path templateDir;
    private final Path arenaRoot;
    private int worldCounter = 0;
    private NamespacedKey currentKey;

    public ArenaManager(VampireZPlugin plugin) {
        this.plugin = plugin;
        PluginConfig.ArenaSection cfg = plugin.getPluginConfig().arena;
        this.templateDir = plugin.getServer().getWorldContainer().toPath().resolve(cfg.templateFolder);
        this.arenaRoot = ArenaPaths.arenaRoot(plugin.getServer().getLevelDirectory());
    }

    public boolean hasTemplate() {
        return ArenaPaths.hasRegionData(templateDir);
    }

    public World getArenaWorld() {
        if (currentKey == null)
            return null;
        return Bukkit.getWorld(currentKey);
    }

    public World loadArenaWorld() {
        if (!hasTemplate()) {
            log.warn("Arena template not found at: {}", templateDir.toAbsolutePath());
            return null;
        }

        if (ArenaPaths.isLegacyTemplate(templateDir))
            upgradeLegacyTemplate();

        cleanupOldArenas();

        worldCounter++;
        NamespacedKey key = new NamespacedKey(plugin, ArenaPaths.keyValue(worldCounter));
        Path arenaDir = arenaRoot.resolve(key.getKey());
        log.info("Copying arena template to '{}'...", key);
        copyDirectory(templateDir, arenaDir);

        World world = Bukkit.createWorld(WorldCreator.ofKey(key));
        if (world == null)
            throw new IllegalStateException("Paper returned null creating arena world " + key);

        world.setGameRule(GameRules.RANDOM_TICK_SPEED, 0);
        world.setGameRule(GameRules.SPAWN_MOBS, false);
        world.setGameRule(GameRules.MOB_GRIEFING, false);
        world.setGameRule(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, 0);
        world.setGameRule(GameRules.ADVANCE_TIME, false);
        world.setGameRule(GameRules.ADVANCE_WEATHER, false);
        world.setGameRule(GameRules.SHOW_ADVANCEMENT_MESSAGES, false);
        world.setGameRule(GameRules.KEEP_INVENTORY, true);
        world.setDifficulty(Difficulty.HARD);
        world.setAutoSave(false);

        currentKey = key;
        log.info("Arena world '{}' loaded.", key);
        return world;
    }

    /**
     * Unloads the current arena, waits for Paper's chunk system to release it,
     * loads a fresh copy, then deletes the old folder in the background.
     */
    public void resetArena(Runnable callback) {
        World arenaWorld = getArenaWorld();
        Path oldDir = arenaWorld == null ? null : arenaWorld.getWorldPath();

        if (arenaWorld != null) {
            World fallback = Bukkit.getWorlds().get(0);
            for (Player player : arenaWorld.getPlayers())
                player.teleport(fallback.getSpawnLocation());

            for (Entity entity : arenaWorld.getEntities()) {
                if (!(entity instanceof Player))
                    entity.remove();
            }

            for (Chunk chunk : arenaWorld.getLoadedChunks())
                chunk.unload(false);

            Bukkit.unloadWorld(arenaWorld, false);
            log.info("Old arena world '{}' unloaded.", currentKey);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            World newWorld = loadArenaWorld();

            if (newWorld != null && callback != null)
                callback.run();

            if (oldDir != null)
                Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> deleteQuietly(oldDir), 100L);
        }, 60L);
    }

    /**
     * Converts a pre-26.1 template (standalone world with level.dat) into a 26.x
     * dimension folder. Paper only runs its legacy import for worlds loaded by name
     * from the world container, so the template is copied there, loaded, its chunks
     * are forced through conversion, and the migrated dimension folder becomes the
     * new template. The original is kept as {@code <template>-pre-26-backup}.
     * {@code new WorldCreator(String)} is {@code @ApiStatus.Obsolete}, not deprecated.
     */
    private void upgradeLegacyTemplate() {
        Path backup = templateDir.resolveSibling(templateDir.getFileName() + "-pre-26-backup");
        if (Files.exists(backup))
            throw new IllegalStateException("Legacy arena template found but backup already exists at "
                    + backup.toAbsolutePath() + " — move one of them away and restart.");

        log.info("Upgrading legacy arena template to the Paper 26.x format (one time)...");
        Path legacyCopy = plugin.getServer().getWorldContainer().toPath().resolve(UPGRADE_WORLD);
        deleteQuietly(legacyCopy);
        copyDirectory(templateDir, legacyCopy);

        World world = Bukkit.createWorld(new WorldCreator(UPGRADE_WORLD));
        if (world == null)
            throw new IllegalStateException("Paper failed to import the legacy arena template");

        int spawnX = world.getSpawnLocation().getBlockX() >> 4;
        int spawnZ = world.getSpawnLocation().getBlockZ() >> 4;
        for (int cx = spawnX - BAKE_RADIUS_CHUNKS; cx <= spawnX + BAKE_RADIUS_CHUNKS; cx++) {
            for (int cz = spawnZ - BAKE_RADIUS_CHUNKS; cz <= spawnZ + BAKE_RADIUS_CHUNKS; cz++)
                world.getChunkAt(cx, cz);
        }

        world.save();
        Path migrated = world.getWorldPath();
        if (!Bukkit.unloadWorld(world, true))
            throw new IllegalStateException("Could not unload template upgrade world " + UPGRADE_WORLD);

        Path staged = templateDir.resolveSibling(templateDir.getFileName() + ".upgrading");
        deleteQuietly(staged);
        copyDirectory(migrated, staged);

        try {
            Files.move(templateDir, backup);
            Files.move(staged, templateDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Swapping upgraded arena template failed", e);
        }

        deleteQuietly(migrated);
        deleteQuietly(legacyCopy);
        log.info("Arena template upgraded. Original kept at {}", backup.toAbsolutePath());
    }

    private void cleanupOldArenas() {
        if (!Files.isDirectory(arenaRoot))
            return;

        try (Stream<Path> dirs = Files.list(arenaRoot)) {
            for (Path dir : (Iterable<Path>) dirs::iterator) {
                NamespacedKey key = new NamespacedKey(plugin, dir.getFileName().toString());
                if (Bukkit.getWorld(key) != null)
                    continue;

                deleteQuietly(dir);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Listing " + arenaRoot + " failed", e);
        }
    }

    private static void copyDirectory(Path source, Path target) {
        try (Stream<Path> paths = Files.walk(source)) {
            for (Path path : (Iterable<Path>) paths::iterator) {
                Path dest = target.resolve(source.relativize(path).toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(dest);
                    continue;
                }

                String name = path.getFileName().toString();
                if (name.equals("session.lock") || name.equals("uid.dat"))
                    continue;

                Files.copy(path, dest, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Copying " + source + " to " + target + " failed", e);
        }
    }

    /** Best effort: Windows may still hold region files for a few ticks after unload. */
    private static void deleteQuietly(Path dir) {
        if (!Files.exists(dir))
            return;

        try (Stream<Path> paths = Files.walk(dir)) {
            for (Path path : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator)
                Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Could not fully delete {} ({}); it will be retried on the next reset.", dir, e.getMessage());
        }
    }
}

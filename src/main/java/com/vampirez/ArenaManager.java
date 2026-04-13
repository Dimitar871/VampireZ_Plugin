package com.vampirez;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Manages the arena world lifecycle using unique world names per game.
 * Each game gets a fresh copy from the template (e.g., vampirez_arena_1, vampirez_arena_2).
 * Old worlds are cleaned up on next reset or server start.
 */
public class ArenaManager {

    private final JavaPlugin plugin;
    private final String baseWorldName;
    private final File templateDir;
    private final File serverDir;
    private int worldCounter = 0;
    private String currentWorldName;

    public ArenaManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.baseWorldName = plugin.getConfig().getString("arena.world-name", "vampirez_arena");
        String templatePath = plugin.getConfig().getString("arena.template-folder", "arena-template");
        this.serverDir = plugin.getServer().getWorldContainer();
        this.templateDir = new File(serverDir, templatePath);
    }

    /**
     * Checks if the template directory exists and has region data.
     */
    public boolean hasTemplate() {
        return templateDir.exists() && new File(templateDir, "region").exists();
    }

    /**
     * Gets the current arena world name.
     */
    public String getArenaWorldName() {
        return currentWorldName != null ? currentWorldName : baseWorldName;
    }

    /**
     * Gets the currently loaded arena world, or null if not loaded.
     */
    public World getArenaWorld() {
        if (currentWorldName == null) return null;
        return Bukkit.getWorld(currentWorldName);
    }

    /**
     * Loads a fresh arena world from template with a unique name.
     * Returns the loaded World, or null on failure.
     */
    public World loadArenaWorld() {
        if (!hasTemplate()) {
            plugin.getLogger().warning("Arena template not found at: " + templateDir.getAbsolutePath());
            return null;
        }

        // Clean up any old arena folders from previous runs
        cleanupOldArenas();

        // Generate unique world name
        worldCounter++;
        currentWorldName = baseWorldName + "_" + worldCounter;
        File arenaDir = new File(serverDir, currentWorldName);

        // Copy from template
        plugin.getLogger().info("Copying arena from template to '" + currentWorldName + "'...");
        if (!copyDirectory(templateDir, arenaDir)) {
            plugin.getLogger().severe("Failed to copy arena template!");
            return null;
        }
        new File(arenaDir, "session.lock").delete();
        new File(arenaDir, "uid.dat").delete();
        plugin.getLogger().info("Arena copied successfully.");

        // Load the world
        WorldCreator creator = new WorldCreator(currentWorldName);
        World world = Bukkit.createWorld(creator);
        if (world != null) {
            world.setKeepSpawnInMemory(false);
            world.setGameRule(org.bukkit.GameRule.RANDOM_TICK_SPEED, 0);
            world.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING, false);
            world.setGameRule(org.bukkit.GameRule.MOB_GRIEFING, false);
            world.setGameRule(org.bukkit.GameRule.DO_FIRE_TICK, false);
            world.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, false);
            world.setGameRule(org.bukkit.GameRule.DO_WEATHER_CYCLE, false);
            world.setGameRule(org.bukkit.GameRule.ANNOUNCE_ADVANCEMENTS, false);
            world.setGameRule(org.bukkit.GameRule.KEEP_INVENTORY, true);
            world.setDifficulty(org.bukkit.Difficulty.HARD);

            // Check if the template needs to be "baked" in Paper's format.
            // On first ever load, force-save so Paper writes its converted chunk data,
            // then copy the saved world back as the template for future resets.
            File bakeMarker = new File(templateDir, ".paper-baked");
            if (!bakeMarker.exists()) {
                plugin.getLogger().info("First load: baking template in Paper format...");
                // Force load chunks around spawn so Paper converts them
                int spawnX = world.getSpawnLocation().getBlockX() >> 4;
                int spawnZ = world.getSpawnLocation().getBlockZ() >> 4;
                int radius = 16; // ~256 block radius
                for (int cx = spawnX - radius; cx <= spawnX + radius; cx++) {
                    for (int cz = spawnZ - radius; cz <= spawnZ + radius; cz++) {
                        world.getChunkAt(cx, cz); // forces load + conversion
                    }
                }
                // Save the converted data to disk
                world.save();
                plugin.getLogger().info("World saved with Paper-converted data.");

                // Copy the saved world back as the new template
                final File bakedDir = new File(serverDir, currentWorldName);
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    // Delete old template region/entities/poi and replace
                    deleteDirectory(new File(templateDir, "region"));
                    deleteDirectory(new File(templateDir, "entities"));
                    deleteDirectory(new File(templateDir, "poi"));
                    copyDirectory(new File(bakedDir, "region"), new File(templateDir, "region"));
                    File arenaEntities = new File(bakedDir, "entities");
                    if (arenaEntities.exists()) {
                        copyDirectory(arenaEntities, new File(templateDir, "entities"));
                    }
                    File arenaPoi = new File(bakedDir, "poi");
                    if (arenaPoi.exists()) {
                        copyDirectory(arenaPoi, new File(templateDir, "poi"));
                    }
                    // Also copy level.dat
                    try {
                        Files.copy(new File(bakedDir, "level.dat").toPath(),
                                new File(templateDir, "level.dat").toPath(),
                                StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        plugin.getLogger().warning("Could not copy level.dat to template");
                    }
                    // Create marker so we don't bake again
                    try {
                        bakeMarker.createNewFile();
                    } catch (IOException e) {
                        plugin.getLogger().warning("Could not create bake marker");
                    }
                    plugin.getLogger().info("Template baked in Paper format. Future resets will use this.");
                });
            }

            world.setAutoSave(false);
            plugin.getLogger().info("Arena world '" + currentWorldName + "' loaded.");
        }
        return world;
    }

    /**
     * Resets the arena: unloads current world, waits for Paper's chunk system to fully
     * release, then loads a fresh copy with a new name.
     * Old world folder is cleaned up in the background.
     */
    public void resetArena(Runnable callback) {
        String oldWorldName = currentWorldName;
        World arenaWorld = getArenaWorld();

        // Unload the old world
        if (arenaWorld != null) {
            World fallback = Bukkit.getWorlds().get(0);
            for (org.bukkit.entity.Player player : arenaWorld.getPlayers()) {
                player.teleport(fallback.getSpawnLocation());
            }

            // Remove all non-player entities
            for (org.bukkit.entity.Entity entity : arenaWorld.getEntities()) {
                if (!(entity instanceof org.bukkit.entity.Player)) {
                    entity.remove();
                }
            }

            // Force-unload every loaded chunk so Paper releases them from cache
            for (org.bukkit.Chunk chunk : arenaWorld.getLoadedChunks()) {
                chunk.unload(false); // false = don't save
            }

            Bukkit.unloadWorld(arenaWorld, false);
            plugin.getLogger().info("Old arena world '" + oldWorldName + "' unloaded.");
        }

        // Wait 3 seconds for Paper's chunk system to fully release all cached data,
        // then copy template and load the new world
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            World newWorld = loadArenaWorld();

            if (newWorld != null && callback != null) {
                callback.run();
            }

            // Clean up old world folder in background
            if (oldWorldName != null) {
                Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                    File oldDir = new File(serverDir, oldWorldName);
                    if (oldDir.exists()) {
                        deleteDirectory(oldDir);
                        if (oldDir.exists()) {
                            plugin.getLogger().warning("Could not fully delete old arena '" + oldWorldName + "', will retry on next reset.");
                        } else {
                            plugin.getLogger().info("Cleaned up old arena folder: " + oldWorldName);
                        }
                    }
                }, 100L);
            }
        }, 60L); // 3 second delay
    }

    /**
     * Deletes any leftover arena folders from previous runs (e.g., after a crash).
     */
    private void cleanupOldArenas() {
        File[] files = serverDir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory() && f.getName().startsWith(baseWorldName + "_")) {
                // Don't delete the currently loaded one
                if (f.getName().equals(currentWorldName)) continue;
                // Make sure it's not a loaded world
                if (Bukkit.getWorld(f.getName()) != null) continue;
                deleteDirectory(f);
                if (!f.exists()) {
                    plugin.getLogger().info("Cleaned up old arena folder: " + f.getName());
                }
            }
        }
    }

    // ===== File utilities =====

    private boolean copyDirectory(File source, File target) {
        try {
            Path sourcePath = source.toPath();
            Path targetPath = target.toPath();
            Files.walkFileTree(sourcePath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    Path targetDir = targetPath.resolve(sourcePath.relativize(dir));
                    Files.createDirectories(targetDir);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String name = file.getFileName().toString();
                    if (name.equals("session.lock") || name.equals("uid.dat")) {
                        return FileVisitResult.CONTINUE;
                    }
                    Files.copy(file, targetPath.resolve(sourcePath.relativize(file)),
                            StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }
            });
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to copy directory", e);
            return false;
        }
    }

    private void deleteDirectory(File dir) {
        try {
            Files.walkFileTree(dir.toPath(), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    try {
                        Files.delete(file);
                    } catch (IOException e) {
                        // File may be locked on Windows, skip it
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
                    try {
                        Files.delete(d);
                    } catch (IOException e) {
                        // Directory may not be empty if files were locked
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete directory: " + dir.getAbsolutePath(), e);
        }
    }
}

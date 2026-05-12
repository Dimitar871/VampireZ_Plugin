package com.vampirez;

import com.vampirez.config.PluginConfig;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.function.Supplier;

/**
 * Owns the three game spawn points (lobby, human, vampire). Reads them from typed config on
 * boot, persists changes back, and resolves world names against the current arena instance.
 */
public class SpawnManager {

    private final VampireZPlugin plugin;
    private final Supplier<ArenaManager> arenaSupplier;

    private Location lobbySpawn;
    private Location humanSpawn;
    private Location vampireSpawn;

    public SpawnManager(VampireZPlugin plugin, Supplier<ArenaManager> arenaSupplier) {
        this.plugin = plugin;
        this.arenaSupplier = arenaSupplier;
    }

    public void loadSpawns() {
        PluginConfig.SpawnsSection spawns = plugin.getPluginConfig().spawns;
        lobbySpawn = loadLocation(spawns.lobby);
        humanSpawn = loadLocation(spawns.human);
        vampireSpawn = loadLocation(spawns.vampire);
    }

    private Location loadLocation(PluginConfig.SpawnPoint sp) {
        if (sp == null || sp.world == null || sp.world.isEmpty()) return null;
        World world = Bukkit.getWorld(sp.world);
        ArenaManager arena = arenaSupplier.get();
        if (world == null && arena != null) {
            String arenaBase = plugin.getPluginConfig().arena.worldName;
            if (sp.world.startsWith(arenaBase)) {
                world = arena.getArenaWorld();
            }
        }
        if (world == null) return null;
        return new Location(world, sp.x, sp.y, sp.z, sp.yaw, sp.pitch);
    }

    /** Persist a spawn point ("lobby" / "human" / "vampire") to the typed config and disk. */
    public void saveSpawn(String which, Location loc) {
        PluginConfig.SpawnsSection spawns = plugin.getPluginConfig().spawns;
        PluginConfig.SpawnPoint sp = switch (which) {
            case "lobby" -> spawns.lobby;
            case "human" -> spawns.human;
            case "vampire" -> spawns.vampire;
            default -> throw new IllegalArgumentException("Unknown spawn key: " + which);
        };
        sp.world = loc.getWorld().getName();
        sp.x = loc.getX();
        sp.y = loc.getY();
        sp.z = loc.getZ();
        sp.yaw = loc.getYaw();
        sp.pitch = loc.getPitch();
        plugin.savePluginConfig();
    }

    public boolean hasSpawnsSet() {
        return humanSpawn != null && vampireSpawn != null;
    }

    public Location getLobbySpawn() { return lobbySpawn; }
    public Location getHumanSpawn() { return humanSpawn; }
    public Location getVampireSpawn() { return vampireSpawn; }

    public void setLobbySpawn(Location loc) {
        this.lobbySpawn = loc;
        saveSpawn("lobby", loc);
    }

    public void setHumanSpawn(Location loc) {
        this.humanSpawn = loc;
        saveSpawn("human", loc);
    }

    public void setVampireSpawn(Location loc) {
        this.vampireSpawn = loc;
        saveSpawn("vampire", loc);
    }

    /** Null-safe teleport helpers. Return false if no spawn is configured. */
    public boolean teleportToLobby(Player p) {
        if (lobbySpawn == null) return false;
        p.teleport(lobbySpawn);
        return true;
    }

    public boolean teleportToHuman(Player p) {
        if (humanSpawn == null) return false;
        p.teleport(humanSpawn);
        return true;
    }

    public boolean teleportToVampire(Player p) {
        if (vampireSpawn == null) return false;
        p.teleport(vampireSpawn);
        return true;
    }
}

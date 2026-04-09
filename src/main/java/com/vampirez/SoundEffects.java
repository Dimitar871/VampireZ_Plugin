package com.vampirez;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class SoundEffects {

    public static void playVampireDeathCackle(Player deadPlayer, JavaPlugin plugin) {
        Location loc = deadPlayer.getLocation();

        // Layer 1 — Witch cackle: "ha-ha-haa" at descending pitches
        loc.getWorld().playSound(loc, Sound.ENTITY_WITCH_CELEBRATE, 0.8f, 1.8f);

        Bukkit.getScheduler().runTaskLater(plugin, () ->
                loc.getWorld().playSound(loc, Sound.ENTITY_WITCH_CELEBRATE, 0.7f, 1.4f), 3L);

        Bukkit.getScheduler().runTaskLater(plugin, () ->
                loc.getWorld().playSound(loc, Sound.ENTITY_WITCH_CELEBRATE, 0.5f, 0.9f), 7L);

        // Layer 2 — Vex ambient: eerie ghostly undertone
        loc.getWorld().playSound(loc, Sound.ENTITY_VEX_AMBIENT, 0.4f, 0.7f);

        // Layer 3 — Soul escape: subtle whoosh
        Bukkit.getScheduler().runTaskLater(plugin, () ->
                loc.getWorld().playSound(loc, Sound.PARTICLE_SOUL_ESCAPE, 0.3f, 0.8f), 5L);

        // Layer 4 — Evoker cackle echo: trailing off
        Bukkit.getScheduler().runTaskLater(plugin, () ->
                loc.getWorld().playSound(loc, Sound.ENTITY_EVOKER_CELEBRATE, 0.3f, 1.2f), 10L);

        Bukkit.getScheduler().runTaskLater(plugin, () ->
                loc.getWorld().playSound(loc, Sound.ENTITY_EVOKER_CELEBRATE, 0.15f, 0.8f), 16L);
    }
}

package com.vampirez.fx;

import com.vampirez.VampireZPlugin;
import org.bukkit.Bukkit;

/**
 * Static accessor for the shared {@link EffectKit} + {@link SoundKit} instances.
 * Resolves the plugin once on first use and caches the references.
 *
 * <p>Use from perk classes: {@code VFX.fx().shockwave(loc, color, 5, 20);} and
 * {@code VFX.sound().playExplosion(loc);}.
 */
public final class VFX {

    private static volatile EffectKit fx;
    private static volatile SoundKit sound;

    private VFX() { /* static */ }

    public static EffectKit fx() {
        EffectKit local = fx;
        if (local != null) return local;
        return resolve().effects();
    }

    public static SoundKit sound() {
        SoundKit local = sound;
        if (local != null) return local;
        return resolve().sounds();
    }

    private static synchronized VampireZPlugin resolve() {
        VampireZPlugin plugin = (VampireZPlugin) Bukkit.getPluginManager().getPlugin("VampireZ");
        if (plugin == null) throw new IllegalStateException("VampireZ plugin not loaded");
        fx = plugin.effects();
        sound = plugin.sounds();
        return plugin;
    }

    /** Reset cached refs on plugin reload (called from VampireZPlugin#onDisable). */
    public static void invalidate() {
        fx = null;
        sound = null;
    }
}

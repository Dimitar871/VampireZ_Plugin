package com.vampirez.fx;

import com.vampirez.VampireZPlugin;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.AtomEffect;
import de.slikey.effectlib.effect.CircleEffect;
import de.slikey.effectlib.effect.HelixEffect;
import de.slikey.effectlib.effect.LineEffect;
import de.slikey.effectlib.effect.SphereEffect;
import de.slikey.effectlib.effect.VortexEffect;
import de.slikey.effectlib.util.DynamicLocation;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

/**
 * High-level wrapper around EffectLib. Each method takes simple primitives (location, color,
 * size, duration) and returns nothing — fire-and-forget. Effects auto-dispose after their
 * configured duration via EffectLib's built-in iteration counter.
 *
 * <p>One {@link EffectManager} is shared across the plugin (cheaper than re-creating per call).
 * Bukkit-thread only — call from main thread.
 */
public class EffectKit {

    private final EffectManager manager;

    public EffectKit(VampireZPlugin plugin) {
        this.manager = new EffectManager(plugin);
    }

    /** Cleanup on plugin disable to cancel any in-flight effects. */
    public void shutdown() {
        manager.dispose();
    }

    // ===== Single-burst effects =====

    /**
     * Expanding ground shockwave — a flat circle that grows outward with redstone particles.
     * Useful for: Earthquake, Holy Shield trigger, AoE perks.
     */
    public void shockwave(Location center, Color color, double radius, int durationTicks) {
        SphereEffect e = new SphereEffect(manager);
        e.setLocation(center.clone().add(0, 0.1, 0));
        e.particle = Particle.DUST;
        e.color = color;
        e.radius = 0.5;
        e.particles = 60;
        e.iterations = Math.max(1, durationTicks / 2);
        e.period = 2;
        e.radiusIncrease = (radius - 0.5) / Math.max(1, e.iterations);
        e.start();
    }

    /**
     * Persistent rune circle on the ground — flat ring of colored particles around a point.
     * Useful for: Citadel zones, Consecrated Ground, Blood Beacon.
     */
    public void runeCircle(Location center, Color color, double radius, int durationTicks) {
        CircleEffect e = new CircleEffect(manager);
        e.setLocation(center.clone().add(0, 0.05, 0));
        e.particle = Particle.DUST;
        e.color = color;
        e.radius = (float) radius;
        e.particles = 40;
        e.iterations = Math.max(1, durationTicks / 2);
        e.period = 2;
        e.start();
    }

    /**
     * Vertical column of swirling particles (vortex). Useful for: Wraith Walk, Bat Form,
     * teleport / dimensional shift abilities.
     */
    public void vortex(Location center, Color color, int durationTicks) {
        VortexEffect e = new VortexEffect(manager);
        e.setLocation(center);
        e.particle = Particle.DUST;
        e.color = color;
        e.radius = 1.5f;
        e.iterations = Math.max(1, durationTicks);
        e.period = 1;
        e.start();
    }

    /**
     * Beam connecting two points — for laser/missile/chain-lightning style effects.
     */
    public void beam(Location from, Location to, Color color) {
        LineEffect e = new LineEffect(manager);
        e.setLocation(from);
        e.setTargetLocation(to);
        e.particle = Particle.DUST;
        e.color = color;
        e.particles = 40;
        e.iterations = 5;
        e.period = 1;
        e.start();
    }

    /**
     * Atom-style orbit around a player — three intersecting electron rings + nucleus.
     * Useful for: buff applied (Final Form, Plague Doctor), mana visual.
     */
    public void atomAround(Player player, Color color, int durationTicks) {
        AtomEffect e = new AtomEffect(manager);
        e.setEntity(player);
        e.setDynamicOrigin(new DynamicLocation(player));
        e.particle = Particle.DUST;
        e.color = color;
        e.radius = 1.5f;
        e.iterations = Math.max(1, durationTicks / 2);
        e.period = 2;
        e.start();
    }

    /**
     * Helix — spinning vertical strand of particles. Useful for: Time Warp rewind,
     * Ankh of Rebirth resurrection visual.
     */
    public void helix(Location at, Color color, int durationTicks) {
        HelixEffect e = new HelixEffect(manager);
        e.setLocation(at);
        e.particle = Particle.DUST;
        e.color = color;
        e.radius = 1.0f;
        e.particles = 40;
        e.iterations = Math.max(1, durationTicks);
        e.period = 1;
        e.start();
    }

    /**
     * Quick blood spray — burst of dark-red dust at a location, no animation.
     * Useful for: hit-marker style feedback on conversion / kill / lifesteal.
     */
    public void bloodSpray(Location at) {
        if (at.getWorld() == null) return;
        at.getWorld().spawnParticle(Particle.DUST, at, 30, 0.4, 0.4, 0.4, 0,
                new Particle.DustOptions(Color.fromRGB(120, 0, 0), 1.5f));
        at.getWorld().spawnParticle(Particle.LARGE_SMOKE, at, 8, 0.3, 0.3, 0.3, 0.02);
    }

    /**
     * Lightning-strike visual + sound — instantly placed, no actual damage.
     * Useful for: Thunderstrike, Chain Lightning, dramatic ability triggers.
     */
    public void lightningFlash(Location at) {
        if (at.getWorld() == null) return;
        at.getWorld().strikeLightningEffect(at);
    }

    /**
     * Magic-missile trail — line of particles between two points with a slight arc feel.
     * Pairs well with playSpellCast() at the source.
     */
    public void magicMissile(Location from, Location to, Color color) {
        beam(from, to, color);
        if (from.getWorld() != null) {
            from.getWorld().spawnParticle(Particle.END_ROD, from, 20, 0.2, 0.2, 0.2, 0.05);
            from.getWorld().spawnParticle(Particle.WITCH, from, 10, 0.3, 0.3, 0.3, 0.0);
        }
    }
}

package com.vampirez.engine.action;

import com.vampirez.engine.HookContext;
import com.vampirez.engine.condition.Target;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;

/**
 * Spawns a cloud of particles around a target's location with optional Y offset and XYZ spread.
 *
 * Special handling:
 *   - {@link Particle#DUST} requires DustOptions (color + size)
 *   - {@link Particle#BLOCK} requires BlockData (which Material to mimic)
 */
public class SpawnParticleAction implements Action {

    private final Target target;
    private final Particle particle;
    private final int count;
    private final double offsetY;
    private final double spreadX, spreadY, spreadZ;
    private final double extra;
    private final Color dustColor;        // null when not used
    private final float dustSize;
    private final Material blockMaterial; // null when not used

    public SpawnParticleAction(Target target, Particle particle, int count, double offsetY,
                               double spreadX, double spreadY, double spreadZ, double extra,
                               Color dustColor, float dustSize, Material blockMaterial) {
        this.target = target;
        this.particle = particle;
        this.count = count;
        this.offsetY = offsetY;
        this.spreadX = spreadX;
        this.spreadY = spreadY;
        this.spreadZ = spreadZ;
        this.extra = extra;
        this.dustColor = dustColor;
        this.dustSize = dustSize;
        this.blockMaterial = blockMaterial;
    }

    @Override
    public void run(HookContext ctx) {
        LivingEntity entity = target.resolveLivingEntity(ctx);
        if (entity == null || particle == null) return;
        var loc = entity.getLocation().add(0, offsetY, 0);

        if (particle == Particle.DUST && dustColor != null) {
            entity.getWorld().spawnParticle(particle, loc, count, spreadX, spreadY, spreadZ, extra,
                    new Particle.DustOptions(dustColor, dustSize));
        } else if (particle == Particle.BLOCK && blockMaterial != null) {
            entity.getWorld().spawnParticle(particle, loc, count, spreadX, spreadY, spreadZ, extra,
                    blockMaterial.createBlockData());
        } else {
            entity.getWorld().spawnParticle(particle, loc, count, spreadX, spreadY, spreadZ, extra);
        }
    }
}

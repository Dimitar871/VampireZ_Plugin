package com.vampirez.engine.action;

import com.vampirez.engine.HookContext;
import com.vampirez.engine.condition.Target;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;

/**
 * Plays a sound at the target's current location. Audible to nearby players via the world.
 */
public class PlaySoundAction implements Action {

    private final Target target;
    private final Sound sound;
    private final float volume;
    private final float pitch;

    public PlaySoundAction(Target target, Sound sound, float volume, float pitch) {
        this.target = target;
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
    }

    @Override
    public void run(HookContext ctx) {
        LivingEntity entity = target.resolveLivingEntity(ctx);
        if (entity == null || sound == null) return;
        entity.getWorld().playSound(entity.getLocation(), sound, volume, pitch);
    }
}

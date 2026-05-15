package com.vampirez.fx;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Vanilla-only sound presets that layer + sequence existing {@link Sound} entries to feel
 * richer than a single one-shot. No resource pack required, nothing to host.
 *
 * <p>Each preset plays at a {@link Location} (heard by everyone in range) or at a
 * {@link Player} (heard only by that player). Pick the audience that matches the cue:
 * AoE abilities → location; personal feedback (heal, buff applied) → player.
 */
public class SoundKit {

    private final Plugin plugin;

    public SoundKit(Plugin plugin) { this.plugin = plugin; }

    // ===== Spell / cast =====

    /** Crisp magical "cast" — high arpeggio + portal travel layer. ~600ms. */
    public void playSpellCast(Location at) {
        if (at.getWorld() == null) return;
        at.getWorld().playSound(at, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 0.8f, 1.6f);
        at.getWorld().playSound(at, Sound.BLOCK_BEACON_ACTIVATE,    SoundCategory.PLAYERS, 0.4f, 2.0f);
        delayed(3, () -> at.getWorld().playSound(at, Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.PLAYERS, 0.6f, 1.8f));
    }

    /** Heavy explosive boom — for shockwaves, AoE damage abilities. ~1s tail. */
    public void playExplosion(Location at) {
        if (at.getWorld() == null) return;
        at.getWorld().playSound(at, Sound.ENTITY_GENERIC_EXPLODE,    SoundCategory.PLAYERS, 1.0f, 1.0f);
        at.getWorld().playSound(at, Sound.BLOCK_ANVIL_LAND,          SoundCategory.PLAYERS, 0.6f, 0.5f);
        delayed(4, () -> at.getWorld().playSound(at, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 0.5f, 1.4f));
    }

    /** Personal heal / buff cue — bell + level-up shimmer. Player-only. */
    public void playBuff(Player player) {
        Location loc = player.getLocation();
        player.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP,        SoundCategory.PLAYERS, 0.7f, 1.4f);
        player.playSound(loc, Sound.BLOCK_BELL_RESONATE,          SoundCategory.PLAYERS, 0.5f, 1.6f);
    }

    /** Personal de-buff cue — wither hurt + glass break. Player-only. */
    public void playDebuff(Player player) {
        Location loc = player.getLocation();
        player.playSound(loc, Sound.ENTITY_WITHER_HURT,           SoundCategory.PLAYERS, 0.5f, 1.5f);
        player.playSound(loc, Sound.BLOCK_GLASS_BREAK,            SoundCategory.PLAYERS, 0.4f, 0.8f);
    }

    /** Vampire conversion / death rattle — wither break + ravager roar. AoE. */
    public void playDeathRattle(Location at) {
        if (at.getWorld() == null) return;
        at.getWorld().playSound(at, Sound.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.PLAYERS, 0.7f, 0.6f);
        at.getWorld().playSound(at, Sound.ENTITY_RAVAGER_ROAR,       SoundCategory.PLAYERS, 0.6f, 0.8f);
        delayed(3, () -> at.getWorld().playSound(at, Sound.AMBIENT_CAVE, SoundCategory.PLAYERS, 0.4f, 0.6f));
    }

    /** Teleport / dimensional shift — enderman + chorus + portal. AoE. */
    public void playPortal(Location at) {
        if (at.getWorld() == null) return;
        at.getWorld().playSound(at, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 0.9f, 1.0f);
        at.getWorld().playSound(at, Sound.BLOCK_PORTAL_TRIGGER,     SoundCategory.PLAYERS, 0.4f, 1.5f);
        delayed(2, () -> at.getWorld().playSound(at, Sound.ITEM_CHORUS_FRUIT_TELEPORT, SoundCategory.PLAYERS, 0.6f, 1.2f));
    }

    /** Quick "pickup / acquire" sound — for perk buy, gold pickup, item award. Player-only. */
    public void playPickup(Player player) {
        Location loc = player.getLocation();
        player.playSound(loc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.7f, 1.6f);
        player.playSound(loc, Sound.UI_BUTTON_CLICK,              SoundCategory.PLAYERS, 0.5f, 2.0f);
    }

    // ===== helpers =====

    private void delayed(long ticks, Runnable r) {
        new BukkitRunnable() {
            @Override public void run() { r.run(); }
        }.runTaskLater(plugin, ticks);
    }
}

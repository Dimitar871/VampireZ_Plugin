package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SpiderClimbPerk extends Perk {

    public SpiderClimbPerk() {
        super("spider_climb", "Spider Climb", PerkTier.GOLD, PerkTeam.VAMPIRE,
                Material.COBWEB,
                "Near walls while sneaking: Levitation I to climb.",
                "Release sneak to detach. No fall damage.");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {
        player.removePotionEffect(PotionEffectType.LEVITATION);
    }

    @Override
    public void onTick(Player player) {
        if (player.isSneaking() && isNearWall(player)) {
            // Apply levitation to climb
            player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 30, 0, false, false, true), true);
            player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 0.5, 0),
                    3, 0.2, 0.2, 0.2, 0, new Particle.DustOptions(org.bukkit.Color.GRAY, 0.8f));
        } else {
            player.removePotionEffect(PotionEffectType.LEVITATION);
        }
    }

    private boolean isNearWall(Player player) {
        Location loc = player.getLocation();
        // Check 4 cardinal directions at both feet and eye level
        int[][] offsets = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] offset : offsets) {
            Block feetBlock = loc.getWorld().getBlockAt(
                    loc.getBlockX() + offset[0], loc.getBlockY(), loc.getBlockZ() + offset[1]);
            Block eyeBlock = loc.getWorld().getBlockAt(
                    loc.getBlockX() + offset[0], (int) (loc.getY() + player.getEyeHeight()), loc.getBlockZ() + offset[1]);
            if (feetBlock.getType().isSolid() || eyeBlock.getType().isSolid()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean negatesFallDamage() {
        return true;
    }
}

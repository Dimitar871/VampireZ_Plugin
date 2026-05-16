package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import com.vampirez.fx.VFX;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;

public class AllForYouPerk extends Perk {

    public AllForYouPerk() {
        super("all_for_you", "All For You", PerkTier.GOLD, PerkTeam.HUMAN,
                Material.SPLASH_POTION, "Splash healing potions heal 50% more");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {}

    @Override
    public void onHealthRegain(Player player, EntityRegainHealthEvent event) {
        if (event.getRegainReason() != EntityRegainHealthEvent.RegainReason.MAGIC) return;
        event.setAmount(event.getAmount() * 1.5);

        Location loc = player.getLocation().add(0, 1.0, 0);
        player.getWorld().spawnParticle(Particle.HEART, loc.clone().add(0, 1.2, 0), 10, 0.5, 0.5, 0.5, 0);
        player.getWorld().spawnParticle(Particle.DUST, loc, 30, 0.6, 0.8, 0.6, 0,
                new Particle.DustOptions(Color.fromRGB(255, 105, 180), 1.4f));
        player.getWorld().spawnParticle(Particle.END_ROD, loc, 6, 0.4, 0.6, 0.4, 0.02);
        VFX.fx().atomAround(player, Color.fromRGB(255, 182, 193), 14);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.5f, 1.6f);
    }
}

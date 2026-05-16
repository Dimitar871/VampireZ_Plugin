package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;

public class FirstAidKitPerk extends Perk {

    public FirstAidKitPerk() {
        super("first_aid_kit", "First-Aid Kit", PerkTier.SILVER, PerkTeam.HUMAN,
                Material.GOLDEN_APPLE, "+30% healing received");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {}

    @Override
    public void onHealthRegain(Player player, EntityRegainHealthEvent event) {
        double bonus = event.getAmount() * 0.3;
        event.setAmount(event.getAmount() + bonus);

        Location loc = player.getLocation().add(0, 1.0, 0);
        player.getWorld().spawnParticle(Particle.HEART, loc.clone().add(0, 1, 0), 3, 0.3, 0.2, 0.3, 0);
        player.getWorld().spawnParticle(Particle.DUST, loc, 12, 0.4, 0.6, 0.4, 0,
                new Particle.DustOptions(Color.fromRGB(140, 255, 140), 1.1f));
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 0.3f, 1.8f);
    }
}

package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;
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
        if (event.getRegainReason() == EntityRegainHealthEvent.RegainReason.MAGIC) {
            event.setAmount(event.getAmount() * 1.5);
        }
    }
}

package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;
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
        event.setAmount(event.getAmount() * 1.3);
    }
}

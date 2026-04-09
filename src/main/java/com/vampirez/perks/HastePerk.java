package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class HastePerk extends Perk {

    public HastePerk() {
        super("haste", "Haste", PerkTier.SILVER, PerkTeam.BOTH,
                Material.SUGAR,
                "Passive: reduces cooldown of all",
                "activatable abilities by 30%.");
    }

    @Override
    public void apply(Player player) {
        // Purely passive — detected by other perks via Perk.getEffectiveCooldown()
    }

    @Override
    public void remove(Player player) {
        // Nothing to remove
    }
}

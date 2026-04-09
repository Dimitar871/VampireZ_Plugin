package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class ToughSkinPerk extends Perk {

    public ToughSkinPerk() {
        super("tough_skin", "Tough Skin", PerkTier.SILVER, PerkTeam.BOTH,
                Material.IRON_INGOT,
                "-10% damage taken");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {}

    @Override
    public void onDamageTaken(Player victim, Entity attacker, EntityDamageByEntityEvent event) {
        event.setDamage(event.getDamage() * 0.90);
        victim.playSound(victim.getLocation(), Sound.BLOCK_STONE_HIT, 0.6f, 1.2f);
    }
}

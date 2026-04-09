package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.LinkedHashMap;
import java.util.Map;

public class SoulChainPerk extends Perk {

    public SoulChainPerk() {
        super("soul_chain", "Soul Chain", PerkTier.PRISMATIC, PerkTeam.VAMPIRE,
                Material.SOUL_LANTERN,
                "On kill: summon zombie ally",
                "with stone sword + leather armor (10s)");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {}

    @Override
    public void onKill(Player killer, Player victim) {
        incrementStat(killer.getUniqueId(), "summons");

        Zombie zombie = (Zombie) killer.getWorld().spawnEntity(
                victim.getLocation(), EntityType.ZOMBIE);
        zombie.setCustomName(ChatColor.RED + killer.getName() + "'s Undead");
        zombie.setCustomNameVisible(true);
        zombie.setBaby(false);

        zombie.getEquipment().setItemInMainHand(new ItemStack(Material.STONE_SWORD));
        zombie.getEquipment().setHelmet(new ItemStack(Material.LEATHER_HELMET));
        zombie.getEquipment().setChestplate(new ItemStack(Material.LEATHER_CHESTPLATE));
        zombie.getEquipment().setLeggings(new ItemStack(Material.LEATHER_LEGGINGS));
        zombie.getEquipment().setBoots(new ItemStack(Material.LEATHER_BOOTS));
        zombie.getEquipment().setItemInMainHandDropChance(0);
        zombie.getEquipment().setHelmetDropChance(0);
        zombie.getEquipment().setChestplateDropChance(0);
        zombie.getEquipment().setLeggingsDropChance(0);
        zombie.getEquipment().setBootsDropChance(0);
        zombie.setMetadata("vampirez_team", new FixedMetadataValue(getPlugin(), "VAMPIRE"));

        killer.getWorld().spawnParticle(Particle.SOUL, victim.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.05);
        killer.playSound(killer.getLocation(), Sound.BLOCK_SOUL_SAND_BREAK, 1.0f, 0.5f);
        killer.sendMessage(ChatColor.DARK_AQUA + "Soul Chain! Zombie ally summoned!");

        Bukkit.getScheduler().runTaskLater(getPlugin(), () -> {
            if (!zombie.isDead()) {
                zombie.getWorld().spawnParticle(Particle.SOUL, zombie.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.02);
                zombie.remove();
            }
        }, 200L);
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("summons", "Summons");
        return labels;
    }
}

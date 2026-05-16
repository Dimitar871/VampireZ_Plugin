package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import com.vampirez.MM;
import com.vampirez.fx.VFX;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RegenPotionsPerk extends Perk {

    private final Map<UUID, Long> lastRegen = new HashMap<>();
    private static final long REGEN_INTERVAL_MS = 120000;

    public RegenPotionsPerk() {
        super("regen_potions", "Regeneration Potions", PerkTier.PRISMATIC, PerkTeam.HUMAN,
                Material.SPLASH_POTION,
                "Start with 3 Splash Regeneration potions",
                "Regenerate 1 every 2 minutes");
    }

    private ItemStack createPotion() {
        ItemStack potion = new ItemStack(Material.SPLASH_POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        if (meta != null) {
            meta.setBasePotionData(new PotionData(PotionType.WATER));
            meta.addCustomEffect(new PotionEffect(PotionEffectType.REGENERATION, 600, 0), true); // 30 seconds
            meta.displayName(Component.text("Splash Potion of Regeneration").color(NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
            meta.setColor(org.bukkit.Color.fromRGB(205, 92, 171));
            potion.setItemMeta(meta);
        }
        return potion;
    }

    @Override
    public void apply(Player player) {
        for (int i = 0; i < 3; i++) {
            player.getInventory().addItem(createPotion());
        }
    }

    @Override
    public void remove(Player player) {
        lastRegen.remove(player.getUniqueId());
    }

    @Override
    public void onTick(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = lastRegen.get(uuid);
        if (last == null) { lastRegen.put(uuid, now); return; }
        if ((now - last) >= REGEN_INTERVAL_MS) {
            lastRegen.put(uuid, now);
            player.getInventory().addItem(createPotion());
            player.sendMessage(MM.parse("<light_purple>A regeneration potion has regenerated!"));

            Color pink = Color.fromRGB(205, 92, 171);
            Location loc = player.getLocation().add(0, 1.0, 0);
            player.getWorld().spawnParticle(Particle.DUST, loc, 25, 0.5, 0.7, 0.5, 0,
                    new Particle.DustOptions(pink, 1.3f));
            player.getWorld().spawnParticle(Particle.WITCH, loc, 10, 0.4, 0.6, 0.4, 0);
            VFX.fx().atomAround(player, pink, 16);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.6f, 1.3f);
            player.playSound(player.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, SoundCategory.PLAYERS, 0.5f, 1.5f);
        }
    }
}

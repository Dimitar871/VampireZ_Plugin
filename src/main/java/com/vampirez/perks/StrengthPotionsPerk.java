package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import com.vampirez.MM;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StrengthPotionsPerk extends Perk {

    private final Map<UUID, Long> lastRegen = new HashMap<>();
    private static final long REGEN_INTERVAL_MS = 120000;

    public StrengthPotionsPerk() {
        super("strength_potions", "Strength Potions", PerkTier.GOLD, PerkTeam.HUMAN,
                Material.SPLASH_POTION,
                "Start with 3 Splash Strength potions",
                "Regenerate 1 every 2 minutes");
    }

    private ItemStack createPotion() {
        ItemStack potion = new ItemStack(Material.SPLASH_POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        if (meta != null) {
            meta.setBasePotionType(PotionType.WATER);
            meta.addCustomEffect(new PotionEffect(PotionEffectType.STRENGTH, 600, 0), true); // 30 seconds
            meta.displayName(Component.text("Splash Potion of Strength").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
            meta.setColor(org.bukkit.Color.fromRGB(147, 36, 35));
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
            player.sendMessage(MM.parse("<light_purple>A strength potion has regenerated!"));
        }
    }
}

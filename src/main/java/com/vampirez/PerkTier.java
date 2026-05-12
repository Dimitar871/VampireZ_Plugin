package com.vampirez;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;

public enum PerkTier {
    SILVER(50, ChatColor.WHITE, NamedTextColor.WHITE, "Silver"),
    GOLD(150, ChatColor.GOLD, NamedTextColor.GOLD, "Gold"),
    PRISMATIC(400, ChatColor.LIGHT_PURPLE, NamedTextColor.LIGHT_PURPLE, "Prismatic");

    private final int cost;
    private final ChatColor color;           // kept for FastBoard string lines
    private final NamedTextColor textColor;  // Adventure API
    private final String displayName;

    PerkTier(int cost, ChatColor color, NamedTextColor textColor, String displayName) {
        this.cost = cost;
        this.color = color;
        this.textColor = textColor;
        this.displayName = displayName;
    }

    public int getCost() { return cost; }
    public ChatColor getColor() { return color; }
    public NamedTextColor getTextColor() { return textColor; }
    public String getDisplayName() { return displayName; }
}

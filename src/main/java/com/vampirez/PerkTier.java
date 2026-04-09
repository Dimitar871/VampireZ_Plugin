package com.vampirez;

import org.bukkit.ChatColor;

public enum PerkTier {
    SILVER(50, ChatColor.WHITE, "Silver"),
    GOLD(150, ChatColor.GOLD, "Gold"),
    PRISMATIC(400, ChatColor.LIGHT_PURPLE, "Prismatic");

    private final int cost;
    private final ChatColor color;
    private final String displayName;

    PerkTier(int cost, ChatColor color, String displayName) {
        this.cost = cost;
        this.color = color;
        this.displayName = displayName;
    }

    public int getCost() { return cost; }
    public ChatColor getColor() { return color; }
    public String getDisplayName() { return displayName; }
}

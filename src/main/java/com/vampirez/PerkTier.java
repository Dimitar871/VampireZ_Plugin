package com.vampirez;

import net.kyori.adventure.text.format.NamedTextColor;

public enum PerkTier {
    SILVER(50, NamedTextColor.WHITE, "Silver"),
    GOLD(150, NamedTextColor.GOLD, "Gold"),
    PRISMATIC(400, NamedTextColor.LIGHT_PURPLE, "Prismatic");

    private final int cost;
    private final NamedTextColor textColor;
    private final String displayName;

    PerkTier(int cost, NamedTextColor textColor, String displayName) {
        this.cost = cost;
        this.textColor = textColor;
        this.displayName = displayName;
    }

    public int getCost() { return cost; }
    public NamedTextColor getTextColor() { return textColor; }
    public String getDisplayName() { return displayName; }
}

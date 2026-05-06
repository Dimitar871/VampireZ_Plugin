package com.vampirez.engine;

import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Mirror of a single YAML perk entry.
 */
public class PerkConfig {
    public final String id;
    public final String displayName;
    public final PerkTier tier;
    public final PerkTeam team;
    public final Material icon;
    public final String[] description;
    public final Map<HookType, List<TriggerEntry>> triggers;

    public PerkConfig(String id,
                      String displayName,
                      PerkTier tier,
                      PerkTeam team,
                      Material icon,
                      String[] description,
                      Map<HookType, List<TriggerEntry>> triggers) {
        this.id = id;
        this.displayName = displayName;
        this.tier = tier;
        this.team = team;
        this.icon = icon;
        this.description = description;
        this.triggers = triggers != null ? triggers : new EnumMap<>(HookType.class);
    }
}

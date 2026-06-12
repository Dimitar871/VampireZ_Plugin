package com.vampirez.engine;

import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import com.vampirez.engine.action.Action;
import com.vampirez.engine.action.AddAttributeModifierAction;
import com.vampirez.engine.action.AddDamageAction;
import com.vampirez.engine.action.AddEnchantAction;
import com.vampirez.engine.action.AoeHealAction;
import com.vampirez.engine.action.ApplyPotionAction;
import com.vampirez.engine.action.ApplyPotionAuraAction;
import com.vampirez.engine.action.CancelEventAction;
import com.vampirez.engine.action.DashAction;
import com.vampirez.engine.action.GiveItemAction;
import com.vampirez.engine.action.MultiplyDamageAction;
import com.vampirez.engine.action.PlaySoundAction;
import com.vampirez.engine.action.RemoveAttributeModifierAction;
import com.vampirez.engine.action.SetDamageAction;
import com.vampirez.engine.action.SetFireAction;
import com.vampirez.engine.action.SpawnMobAction;
import com.vampirez.engine.action.SpawnParticleAction;
import com.vampirez.engine.condition.AllyWithinRangeCondition;
import com.vampirez.engine.condition.Condition;
import com.vampirez.engine.condition.CooldownCondition;
import com.vampirez.engine.condition.FromBehindCondition;
import com.vampirez.engine.condition.HpBelowAbsoluteCondition;
import com.vampirez.engine.condition.HpBelowPercentCondition;
import com.vampirez.engine.condition.IsRightClickCondition;
import com.vampirez.engine.condition.IsSneakingCondition;
import com.vampirez.engine.condition.IsSprintingCondition;
import com.vampirez.engine.condition.ItemCountBelowCondition;
import com.vampirez.engine.condition.ItemInHandCondition;
import com.vampirez.engine.condition.Target;
import com.vampirez.engine.condition.VictimIsPlayerCondition;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Reads perks.yml and produces a list of {@link DataDrivenPerk} instances ready to register.
 * Unknown action/condition types are logged and skipped — the rest of the perk still loads.
 */
public class PerkConfigLoader {

    private final Logger logger;

    public PerkConfigLoader(Logger logger) {
        this.logger = logger;
    }

    public List<DataDrivenPerk> loadAll(File yamlFile) {
        List<DataDrivenPerk> result = new ArrayList<>();
        if (!yamlFile.exists()) {
            logger.warning("perks.yml not found at " + yamlFile.getAbsolutePath() + " — no data-driven perks loaded.");
            return result;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(yamlFile);
        for (String id : yaml.getKeys(false)) {
            ConfigurationSection section = yaml.getConfigurationSection(id);
            if (section == null) continue;
            try {
                result.add(buildPerk(id, section));
            } catch (Exception e) {
                logger.warning("Failed to load perk '" + id + "' from perks.yml: " + e.getMessage());
            }
        }
        return result;
    }

    private DataDrivenPerk buildPerk(String id, ConfigurationSection section) {
        String displayName = section.getString("name", id);
        PerkTier tier = PerkTier.valueOf(section.getString("tier", "SILVER").toUpperCase());
        PerkTeam team = PerkTeam.valueOf(section.getString("team", "BOTH").toUpperCase());
        Material icon = Material.valueOf(section.getString("icon", "STONE").toUpperCase());
        List<String> descList = section.getStringList("description");
        String[] description = descList.toArray(new String[0]);

        Map<HookType, List<TriggerEntry>> triggers = new EnumMap<>(HookType.class);

        // Top-level `attributes:` shortcut: each entry expands into an APPLY trigger that adds
        // the modifier and a REMOVE trigger that removes it. Saves repeating the key in two places.
        List<Map<?, ?>> attrSpecs = section.getMapList("attributes");
        if (!attrSpecs.isEmpty()) {
            List<TriggerEntry> applyEntries = triggers.computeIfAbsent(HookType.APPLY, k -> new ArrayList<>());
            List<TriggerEntry> removeEntries = triggers.computeIfAbsent(HookType.REMOVE, k -> new ArrayList<>());
            for (Map<?, ?> attrSpec : attrSpecs) {
                AttributeShortcut s = parseAttributeShortcut(id, attrSpec);
                if (s == null) continue;
                applyEntries.add(new TriggerEntry(List.of(),
                        List.of(new AddAttributeModifierAction(s.key, s.attribute, s.operation, s.value, s.slotGroup))));
                removeEntries.add(new TriggerEntry(List.of(),
                        List.of(new RemoveAttributeModifierAction(s.key, s.attribute))));
            }
        }

        ConfigurationSection hooksSection = section.getConfigurationSection("hooks");
        if (hooksSection != null) {
            for (String hookKey : hooksSection.getKeys(false)) {
                HookType hook;
                try {
                    hook = HookType.fromYamlKey(hookKey);
                } catch (IllegalArgumentException ex) {
                    logger.warning("Unknown hook '" + hookKey + "' in perk '" + id + "' — skipped.");
                    continue;
                }
                List<TriggerEntry> entries = triggers.computeIfAbsent(hook, k -> new ArrayList<>());
                List<Map<?, ?>> entryMaps = hooksSection.getMapList(hookKey);
                for (Map<?, ?> entryMap : entryMaps) {
                    entries.add(parseTriggerEntry(id, hook, entryMap));
                }
            }
        }

        PerkConfig config = new PerkConfig(id, displayName, tier, team, icon, description, triggers);
        return new DataDrivenPerk(config);
    }

    private static class AttributeShortcut {
        NamespacedKey key;
        Attribute attribute;
        AttributeModifier.Operation operation;
        double value;
        EquipmentSlotGroup slotGroup;
    }

    private AttributeShortcut parseAttributeShortcut(String perkId, Map<?, ?> spec) {
        try {
            AttributeShortcut s = new AttributeShortcut();
            String keyStr = String.valueOf(spec.get("key"));
            s.key = NamespacedKey.fromString(keyStr);
            if (s.key == null) {
                logger.warning("Invalid attribute key '" + keyStr + "' in perk '" + perkId + "' — skipped.");
                return null;
            }
            // Registry lookup (Attribute.valueOf is removal-deprecated); accepts
            // both bare names (MAX_HEALTH) and namespaced (minecraft:max_health).
            String attrName = String.valueOf(spec.get("attribute"));
            NamespacedKey attrKey = attrName.contains(":")
                    ? NamespacedKey.fromString(attrName.toLowerCase())
                    : NamespacedKey.minecraft(attrName.toLowerCase());
            s.attribute = attrKey == null ? null : Registry.ATTRIBUTE.get(attrKey);
            if (s.attribute == null) {
                logger.warning("Unknown attribute '" + attrName + "' in perk '" + perkId + "' — skipped.");
                return null;
            }
            s.operation = AttributeModifier.Operation.valueOf(String.valueOf(spec.get("operation")).toUpperCase());
            s.value = asDouble(spec.get("value"), 0.0);
            Object slotRaw = spec.get("slot_group");
            s.slotGroup = slotRaw == null ? EquipmentSlotGroup.ANY
                    : EquipmentSlotGroup.getByName(String.valueOf(slotRaw).toLowerCase());
            if (s.slotGroup == null) s.slotGroup = EquipmentSlotGroup.ANY;
            return s;
        } catch (IllegalArgumentException ex) {
            logger.warning("Bad attribute spec in perk '" + perkId + "': " + ex.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private TriggerEntry parseTriggerEntry(String perkId, HookType hook, Map<?, ?> entryMap) {
        List<Condition> conditions = new ArrayList<>();
        List<Action> actions = new ArrayList<>();

        Object condList = entryMap.get("conditions");
        if (condList instanceof List<?>) {
            for (Object raw : (List<?>) condList) {
                if (raw instanceof Map<?, ?>) {
                    Condition c = parseCondition(perkId, (Map<String, Object>) raw);
                    if (c != null) conditions.add(c);
                }
            }
        }

        Object actList = entryMap.get("actions");
        if (actList instanceof List<?>) {
            for (Object raw : (List<?>) actList) {
                if (raw instanceof Map<?, ?>) {
                    Action a = parseAction(perkId, (Map<String, Object>) raw);
                    if (a != null) actions.add(a);
                }
            }
        }

        return new TriggerEntry(conditions, actions);
    }

    private Condition parseCondition(String perkId, Map<String, Object> map) {
        String type = String.valueOf(map.get("type"));
        switch (type) {
            case "victim_is_player":
                return new VictimIsPlayerCondition();
            case "from_behind":
                return new FromBehindCondition();
            case "is_sprinting":
                return new IsSprintingCondition(Target.fromYaml(asString(map.get("target"))));
            case "is_sneaking":
                return new IsSneakingCondition(Target.fromYaml(asString(map.get("target"))));
            case "hp_below_percent":
                return new HpBelowPercentCondition(
                        Target.fromYaml(asString(map.get("target"))),
                        asDouble(map.get("threshold"), 0.5));
            case "hp_below_absolute":
                return new HpBelowAbsoluteCondition(
                        Target.fromYaml(asString(map.get("target"))),
                        asDouble(map.get("threshold"), 1.0));
            case "cooldown":
                return new CooldownCondition(asLong(map.get("ms"), 1000L));
            case "ally_within_range":
                return new AllyWithinRangeCondition(asDouble(map.get("radius"), 10.0));
            case "is_right_click":
                return new IsRightClickCondition();
            case "item_count_below": {
                Material mat = parseMaterial(asString(map.get("material")));
                if (mat == null) {
                    logger.warning("item_count_below in perk '" + perkId + "' missing/invalid 'material' — skipped.");
                    return null;
                }
                return new ItemCountBelowCondition(mat, (int) asLong(map.get("threshold"), 1L));
            }
            case "item_in_hand": {
                Material mat = parseMaterial(asString(map.get("material")));
                String nameContains = asString(map.get("name_contains"));
                if (mat == null && nameContains == null) {
                    logger.warning("item_in_hand in perk '" + perkId + "' needs material and/or name_contains — skipped.");
                    return null;
                }
                return new ItemInHandCondition(mat, nameContains);
            }
            default:
                logger.warning("Unknown condition type '" + type + "' in perk '" + perkId + "' — skipped.");
                return null;
        }
    }

    private Action parseAction(String perkId, Map<String, Object> map) {
        String type = String.valueOf(map.get("type"));
        switch (type) {
            case "multiply_damage":
                return new MultiplyDamageAction(asDouble(map.get("factor"), 1.0));
            case "set_damage":
                return new SetDamageAction(asDouble(map.get("value"), 0.0));
            case "add_attribute_modifier": {
                AttributeShortcut s = parseAttributeShortcut(perkId, map);
                if (s == null) return null;
                return new AddAttributeModifierAction(s.key, s.attribute, s.operation, s.value, s.slotGroup);
            }
            case "remove_attribute_modifier": {
                AttributeShortcut s = parseAttributeShortcut(perkId, map);
                if (s == null) return null;
                return new RemoveAttributeModifierAction(s.key, s.attribute);
            }
            case "add_enchant": {
                String matName = asString(map.get("material"));
                String matContains = asString(map.get("material_contains"));
                Object slotRaw = map.get("slot");
                boolean targetArmor = slotRaw != null && "armor".equalsIgnoreCase(String.valueOf(slotRaw));
                String enchantStr = asString(map.get("enchantment"));
                if (enchantStr == null) {
                    logger.warning("add_enchant in perk '" + perkId + "' missing 'enchantment' — skipped.");
                    return null;
                }
                NamespacedKey enchKey = NamespacedKey.fromString(enchantStr.toLowerCase().contains(":")
                        ? enchantStr.toLowerCase()
                        : "minecraft:" + enchantStr.toLowerCase());
                Enchantment enchantment = enchKey == null ? null : Registry.ENCHANTMENT.get(enchKey);
                if (enchantment == null) {
                    logger.warning("Unknown enchantment '" + enchantStr + "' in perk '" + perkId + "' — skipped.");
                    return null;
                }
                int level = (int) asLong(map.get("level"), 1L);
                if (matName == null && matContains == null && !targetArmor) {
                    logger.warning("add_enchant in perk '" + perkId
                            + "' must set one of: material, material_contains, slot:armor — skipped.");
                    return null;
                }
                return new AddEnchantAction(matName, matContains, targetArmor, enchantment, level);
            }
            case "apply_potion": {
                String effStr = asString(map.get("effect"));
                if (effStr == null) {
                    logger.warning("apply_potion in perk '" + perkId + "' missing 'effect' — skipped.");
                    return null;
                }
                NamespacedKey effKey = NamespacedKey.fromString(effStr.toLowerCase().contains(":")
                        ? effStr.toLowerCase() : "minecraft:" + effStr.toLowerCase());
                PotionEffectType effType = effKey == null ? null : Registry.EFFECT.get(effKey);
                if (effType == null) {
                    logger.warning("Unknown potion effect '" + effStr + "' in perk '" + perkId + "' — skipped.");
                    return null;
                }
                int duration = (int) asLong(map.get("duration_ticks"), 60L);
                int amplifier = (int) asLong(map.get("amplifier"), 0L);
                boolean ambient = asBool(map.get("ambient"), false);
                boolean particles = asBool(map.get("particles"), true);
                boolean stack = asBool(map.get("stack_duration"), false);
                Target tgt = parseTargetWithDefault(map.get("target"), Target.VICTIM);
                return new ApplyPotionAction(tgt, effType, duration, amplifier, ambient, particles, stack);
            }
            case "aoe_heal": {
                double radius = asDouble(map.get("radius"), 8.0);
                boolean alliesOnly = asBool(map.get("allies_only"), true);
                boolean includeSelf = asBool(map.get("include_self"), false);
                double amount = asDouble(map.get("amount"), 4.0);
                return new AoeHealAction(radius, alliesOnly, includeSelf, amount);
            }
            case "apply_potion_aura": {
                String effStr = asString(map.get("effect"));
                if (effStr == null) {
                    logger.warning("apply_potion_aura in perk '" + perkId + "' missing 'effect' — skipped.");
                    return null;
                }
                NamespacedKey effKey = NamespacedKey.fromString(effStr.toLowerCase().contains(":")
                        ? effStr.toLowerCase() : "minecraft:" + effStr.toLowerCase());
                PotionEffectType effType = effKey == null ? null : Registry.EFFECT.get(effKey);
                if (effType == null) {
                    logger.warning("Unknown potion effect '" + effStr + "' in perk '" + perkId + "' — skipped.");
                    return null;
                }
                double radius = asDouble(map.get("radius"), 10.0);
                boolean alliesOnly = asBool(map.get("allies_only"), true);
                boolean includeSelf = asBool(map.get("include_self"), false);
                int duration = (int) asLong(map.get("duration_ticks"), 60L);
                int amplifier = (int) asLong(map.get("amplifier"), 0L);
                boolean ambient = asBool(map.get("ambient"), false);
                boolean particles = asBool(map.get("particles"), true);
                return new ApplyPotionAuraAction(radius, alliesOnly, includeSelf,
                        effType, duration, amplifier, ambient, particles);
            }
            case "play_sound": {
                String sndStr = asString(map.get("sound"));
                if (sndStr == null) {
                    logger.warning("play_sound in perk '" + perkId + "' missing 'sound' — skipped.");
                    return null;
                }
                NamespacedKey sndKey = NamespacedKey.fromString(sndStr.toLowerCase().contains(":")
                        ? sndStr.toLowerCase() : "minecraft:" + sndStr.toLowerCase().replace('_', '.'));
                Sound sound = sndKey == null ? null : Registry.SOUNDS.get(sndKey);
                if (sound == null) {
                    // Fallback: try the enum-style name (e.g. ENTITY_SPIDER_AMBIENT)
                    try { sound = Sound.valueOf(sndStr.toUpperCase()); }
                    catch (IllegalArgumentException ignored) {}
                }
                if (sound == null) {
                    logger.warning("Unknown sound '" + sndStr + "' in perk '" + perkId + "' — skipped.");
                    return null;
                }
                float volume = (float) asDouble(map.get("volume"), 1.0);
                float pitch = (float) asDouble(map.get("pitch"), 1.0);
                Target tgt = parseTargetWithDefault(map.get("target"), Target.ATTACKER);
                return new PlaySoundAction(tgt, sound, volume, pitch);
            }
            case "spawn_particle": {
                String partStr = asString(map.get("particle"));
                Particle particle = null;
                if (partStr != null) {
                    NamespacedKey partKey = NamespacedKey.fromString(partStr.toLowerCase().contains(":")
                            ? partStr.toLowerCase() : "minecraft:" + partStr.toLowerCase());
                    particle = partKey == null ? null : Registry.PARTICLE_TYPE.get(partKey);
                    if (particle == null) {
                        try { particle = Particle.valueOf(partStr.toUpperCase()); }
                        catch (IllegalArgumentException ignored) {}
                    }
                }
                if (particle == null) {
                    logger.warning("Unknown particle '" + partStr + "' in perk '" + perkId + "' — skipped.");
                    return null;
                }
                int count = (int) asLong(map.get("count"), 10L);
                double offsetY = asDouble(map.get("offset_y"), 1.0);
                double spreadX = asDouble(map.get("spread_x"), 0.3);
                double spreadY = asDouble(map.get("spread_y"), 0.3);
                double spreadZ = asDouble(map.get("spread_z"), 0.3);
                double extra = asDouble(map.get("extra"), 0.0);
                Color dustColor = parseDustColor(asString(map.get("dust_color")));
                float dustSize = (float) asDouble(map.get("dust_size"), 1.0);
                Material blockMaterial = null;
                String blockStr = asString(map.get("block"));
                if (blockStr != null) {
                    try { blockMaterial = Material.valueOf(blockStr.toUpperCase()); }
                    catch (IllegalArgumentException ignored) {}
                }
                Target tgt = parseTargetWithDefault(map.get("target"), Target.VICTIM);
                return new SpawnParticleAction(tgt, particle, count, offsetY,
                        spreadX, spreadY, spreadZ, extra, dustColor, dustSize, blockMaterial);
            }
            case "add_damage":
                return new AddDamageAction(asDouble(map.get("amount"), 0.0));
            case "set_fire": {
                Target tgt = parseTargetWithDefault(map.get("target"), Target.VICTIM);
                int duration = (int) asLong(map.get("duration_ticks"), 60L);
                return new SetFireAction(tgt, duration);
            }
            case "spawn_mob": {
                EntityType et;
                try {
                    et = EntityType.valueOf(asString(map.get("entity_type")).toUpperCase());
                } catch (IllegalArgumentException | NullPointerException ex) {
                    logger.warning("spawn_mob in perk '" + perkId + "' missing/invalid 'entity_type' — skipped.");
                    return null;
                }
                int count = (int) asLong(map.get("count"), 1L);
                long lifetime = asLong(map.get("lifetime_ticks"), 0L);
                String customName = asString(map.get("custom_name"));
                NamedTextColor nameColor = parseNamedTextColor(asString(map.get("name_color")));
                String team = asString(map.get("team"));
                boolean noBaby = asBool(map.get("no_baby"), false);
                boolean angry = asBool(map.get("angry"), false);
                boolean tamed = asBool(map.get("tamed"), false);
                Material mainHand = parseMaterial(asString(map.get("equipment_main_hand")));
                Material helmet = parseMaterial(asString(map.get("equipment_helmet")));
                double spreadX = asDouble(map.get("spread_x"), 1.0);
                return new SpawnMobAction(et, count, lifetime, customName, nameColor, team,
                        noBaby, angry, tamed, mainHand, helmet, spreadX);
            }
            case "give_item": {
                Material mat = parseMaterial(asString(map.get("material")));
                if (mat == null) {
                    logger.warning("give_item in perk '" + perkId + "' missing/invalid 'material' — skipped.");
                    return null;
                }
                int amount = (int) asLong(map.get("amount"), 1L);
                String displayName = asString(map.get("display_name"));
                Object loreRaw = map.get("lore");
                List<String> lore = null;
                if (loreRaw instanceof List<?>) {
                    lore = new ArrayList<>();
                    for (Object o : (List<?>) loreRaw) lore.add(String.valueOf(o));
                }
                Object enchRaw = map.get("enchantments");
                List<Map<?, ?>> enchants = null;
                if (enchRaw instanceof List<?>) {
                    enchants = new ArrayList<>();
                    for (Object o : (List<?>) enchRaw) {
                        if (o instanceof Map<?, ?>) enchants.add((Map<?, ?>) o);
                    }
                }
                PotionType potionType = null;
                String potionStr = asString(map.get("potion_type"));
                if (potionStr != null) {
                    try { potionType = PotionType.valueOf(potionStr.toUpperCase()); }
                    catch (IllegalArgumentException ex) {
                        logger.warning("Unknown potion_type '" + potionStr + "' in perk '" + perkId + "' — skipped.");
                    }
                }
                return new GiveItemAction(mat, amount, displayName, lore, enchants, potionType);
            }
            case "dash":
                return new DashAction(
                        asDouble(map.get("magnitude"), 1.5),
                        asDouble(map.get("lift"), 0.15));
            case "cancel_event":
                return new CancelEventAction();
            default:
                logger.warning("Unknown action type '" + type + "' in perk '" + perkId + "' — skipped.");
                return null;
        }
    }

    private static String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static double asDouble(Object o, double def) {
        if (o instanceof Number) return ((Number) o).doubleValue();
        if (o instanceof String) {
            try { return Double.parseDouble((String) o); } catch (NumberFormatException ignored) {}
        }
        return def;
    }

    private static long asLong(Object o, long def) {
        if (o instanceof Number) return ((Number) o).longValue();
        if (o instanceof String) {
            try { return Long.parseLong((String) o); } catch (NumberFormatException ignored) {}
        }
        return def;
    }

    private static boolean asBool(Object o, boolean def) {
        if (o instanceof Boolean) return (Boolean) o;
        if (o instanceof String) return Boolean.parseBoolean((String) o);
        return def;
    }

    /**
     * Returns the target the user explicitly set in YAML, or {@code defaultIfMissing} if the
     * "target" key was absent. Distinguishes "target: owner" (use OWNER) from "no target set"
     * (use the hook-appropriate default like VICTIM for potion-on-hit).
     */
    private static Target parseTargetWithDefault(Object raw, Target defaultIfMissing) {
        if (raw == null) return defaultIfMissing;
        try { return Target.valueOf(String.valueOf(raw).toUpperCase()); }
        catch (IllegalArgumentException ex) { return defaultIfMissing; }
    }

    private static Material parseMaterial(String s) {
        if (s == null) return null;
        try { return Material.valueOf(s.toUpperCase()); }
        catch (IllegalArgumentException ex) { return null; }
    }

    private static NamedTextColor parseNamedTextColor(String s) {
        if (s == null) return null;
        NamedTextColor color = NamedTextColor.NAMES.value(s.toLowerCase());
        return color;
    }

    /** Parses "r,g,b" (each 0-255) into a Color, or null if not parseable. */
    private static Color parseDustColor(String s) {
        if (s == null) return null;
        String[] parts = s.split(",");
        if (parts.length != 3) return null;
        try {
            int r = Integer.parseInt(parts[0].trim());
            int g = Integer.parseInt(parts[1].trim());
            int b = Integer.parseInt(parts[2].trim());
            return Color.fromRGB(r, g, b);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}

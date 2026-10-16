package com.sellgui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/** Puts the item's texture in front of its name using sprite text components (1.21.9+). */
final class ItemIcons {

    private static final String[] SHAPE_SUFFIXES = {
            "_stairs", "_slab", "_wall", "_fence_gate", "_fence", "_pressure_plate", "_button"
    };
    private static final String[] WOODS = {
            "oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry",
            "pale_oak", "bamboo", "crimson", "warped"
    };

    private final boolean enabled;
    private final Map<Material, String> overrides = new EnumMap<>(Material.class);

    ItemIcons(SellGUI plugin) {
        plugin.saveResource("icons.yml", false);
        YamlConfiguration y = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "icons.yml"));

        boolean on = y.getBoolean("enabled", true);
        if (on && !supported()) {
            plugin.getLogger().warning("Item icons need Paper 1.21.9+; icons are disabled.");
            on = false;
        }
        enabled = on;

        ConfigurationSection section = y.getConfigurationSection("overrides");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                Material mat = Material.matchMaterial(key);
                String path = section.getString(key);
                if (mat != null && path != null && !path.isBlank()) {
                    overrides.put(mat, path.trim().toLowerCase(Locale.ROOT));
                }
            }
        }
    }

    private static boolean supported() {
        try {
            Class.forName("net.kyori.adventure.text.object.ObjectContents");
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    /** Returns "[icon] name", or just the name when icons are off. */
    Component label(Material mat, Component name) {
        if (!enabled) return name;
        return Component.empty()
                .append(MiniMessage.miniMessage().deserialize("<sprite:" + path(mat) + ">"))
                .append(Component.text(" "))
                .append(name);
    }

    private String path(Material mat) {
        String custom = overrides.get(mat);
        if (custom != null) return custom;
        return guess(mat);
    }

    private static String guess(Material mat) {
        String name = mat.name().toLowerCase(Locale.ROOT);
        if (!mat.isBlock()) {
            return "item/" + name;
        }
        // flat items that also exist as blocks
        if (name.endsWith("_door") || name.endsWith("_sign") || name.endsWith("_hanging_sign")) {
            return "item/" + name;
        }
        if (name.endsWith("_wood") && !name.startsWith("stripped_")) {
            return "block/" + name.replace("_wood", "_log");
        }
        if (name.endsWith("_wood")) {
            return "block/" + name.replace("_wood", "_log");
        }
        if (name.endsWith("_hyphae")) {
            return "block/" + name.replace("_hyphae", "_stem");
        }
        for (String suffix : SHAPE_SUFFIXES) {
            if (name.endsWith(suffix)) {
                return "block/" + baseTexture(name.substring(0, name.length() - suffix.length()));
            }
        }
        return "block/" + name;
    }

    /** Stairs, slabs, fences ... use the texture of the full block they are made from. */
    private static String baseTexture(String base) {
        for (String wood : WOODS) {
            if (base.equals(wood)) return base + "_planks";
        }
        if (base.endsWith("brick") || base.endsWith("tile")) return base + "s";
        return base;
    }
}

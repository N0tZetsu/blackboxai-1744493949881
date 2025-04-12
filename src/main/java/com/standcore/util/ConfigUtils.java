package com.standcore.util;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import com.standcore.StandCore;

import java.util.HashMap;
import java.util.Map;

public class ConfigUtils {
    private static StandCore plugin;
    private static Map<String, FileConfiguration> configs;
    
    public static void init(StandCore instance) {
        plugin = instance;
        configs = new HashMap<>();
        loadConfigs();
    }
    
    /**
     * Loads all configuration files
     */
    public static void loadConfigs() {
        // Load main config
        configs.put("config", FileUtils.saveDefaultResource("config.yml"));
        
        // Load ranks config
        configs.put("ranks", FileUtils.saveDefaultResource("ranks.yml"));
        
        // Load messages config
        configs.put("messages", FileUtils.saveDefaultResource("messages.yml"));
    }
    
    /**
     * Reloads all configuration files
     */
    public static void reloadConfigs() {
        configs.clear();
        loadConfigs();
    }
    
    /**
     * Gets a configuration file
     * @param name The name of the configuration (config, ranks, messages)
     * @return The configuration file
     */
    public static FileConfiguration getConfig(String name) {
        return configs.get(name);
    }
    
    /**
     * Gets the plugin prefix from messages.yml
     * @return The formatted prefix
     */
    public static String getPrefix() {
        return color(configs.get("messages").getString("prefix", "&8[&bStandCore&8]"));
    }
    
    /**
     * Gets a message from messages.yml with prefix
     * @param path The path to the message
     * @param replacements The replacements to make (key=value pairs)
     * @return The formatted message
     */
    public static String getMessage(String path, String... replacements) {
        String message = configs.get("messages").getString(path);
        if (message == null) {
            return "Message not found: " + path;
        }
        
        message = message.replace("%prefix%", getPrefix());
        
        // Apply replacements
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace("%" + replacements[i] + "%", replacements[i + 1]);
            }
        }
        
        return color(message);
    }
    
    /**
     * Gets a configuration section
     * @param config The configuration name
     * @param path The path to the section
     * @return The configuration section or null if not found
     */
    public static ConfigurationSection getSection(String config, String path) {
        return configs.get(config).getConfigurationSection(path);
    }
    
    /**
     * Checks if a configuration section exists
     * @param config The configuration name
     * @param path The path to check
     * @return true if the section exists
     */
    public static boolean hasSection(String config, String path) {
        return configs.get(config).isConfigurationSection(path);
    }
    
    /**
     * Gets a string from a configuration
     * @param config The configuration name
     * @param path The path to the string
     * @param def The default value
     * @return The string value
     */
    public static String getString(String config, String path, String def) {
        return configs.get(config).getString(path, def);
    }
    
    /**
     * Gets an integer from a configuration
     * @param config The configuration name
     * @param path The path to the integer
     * @param def The default value
     * @return The integer value
     */
    public static int getInt(String config, String path, int def) {
        return configs.get(config).getInt(path, def);
    }
    
    /**
     * Gets a boolean from a configuration
     * @param config The configuration name
     * @param path The path to the boolean
     * @param def The default value
     * @return The boolean value
     */
    public static boolean getBoolean(String config, String path, boolean def) {
        return configs.get(config).getBoolean(path, def);
    }
    
    /**
     * Translates color codes in a string
     * @param text The text to colorize
     * @return The colorized text
     */
    public static String color(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }
    
    /**
     * Formats a list of strings with color codes
     * @param list The list to format
     * @return The formatted list
     */
    public static java.util.List<String> colorList(java.util.List<String> list) {
        list.replaceAll(ConfigUtils::color);
        return list;
    }
    
    /**
     * Gets a duration in milliseconds from a string format (1d, 1w, 1mo, etc.)
     * @param duration The duration string
     * @return The duration in milliseconds or -1 if permanent
     * @throws IllegalArgumentException if the format is invalid
     */
    public static long parseDuration(String duration) {
        if (duration.equalsIgnoreCase("permanent") || duration.equalsIgnoreCase("perm")) {
            return -1;
        }
        
        try {
            String value = duration.substring(0, duration.length() - 1);
            char unit = Character.toLowerCase(duration.charAt(duration.length() - 1));
            long time = Long.parseLong(value);
            
            switch (unit) {
                case 's': return time * 1000;
                case 'm': return time * 60 * 1000;
                case 'h': return time * 60 * 60 * 1000;
                case 'd': return time * 24 * 60 * 60 * 1000;
                case 'w': return time * 7 * 24 * 60 * 60 * 1000;
                default: throw new IllegalArgumentException("Invalid duration unit: " + unit);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid duration format: " + duration);
        }
    }
    
    /**
     * Formats a duration from milliseconds to a readable string
     * @param duration The duration in milliseconds
     * @return The formatted duration
     */
    public static String formatDuration(long duration) {
        if (duration == -1) {
            return "permanent";
        }
        
        long seconds = duration / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long weeks = days / 7;
        
        if (weeks > 0) {
            return weeks + "w";
        } else if (days > 0) {
            return days + "d";
        } else if (hours > 0) {
            return hours + "h";
        } else if (minutes > 0) {
            return minutes + "m";
        } else {
            return seconds + "s";
        }
    }
}

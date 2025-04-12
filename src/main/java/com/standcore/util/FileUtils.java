package com.standcore.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import com.standcore.StandCore;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

public class FileUtils {
    private static StandCore plugin;
    
    public static void init(StandCore instance) {
        plugin = instance;
    }
    
    /**
     * Creates a directory if it doesn't exist
     * @param path The path relative to plugin data folder
     * @return true if directory was created or already exists
     */
    public static boolean createDirectory(String path) {
        File directory = new File(plugin.getDataFolder(), path);
        if (!directory.exists()) {
            try {
                Files.createDirectories(directory.toPath());
                plugin.getLogger().info("Created directory: " + path);
                return true;
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create directory: " + path, e);
                return false;
            }
        }
        return true;
    }
    
    /**
     * Saves a resource from the jar if it doesn't exist
     * @param resourcePath The path of the resource in the jar
     * @return The configuration file
     */
    public static FileConfiguration saveDefaultResource(String resourcePath) {
        File file = new File(plugin.getDataFolder(), resourcePath);
        if (!file.exists()) {
            plugin.saveResource(resourcePath, false);
            plugin.getLogger().info("Created default configuration: " + resourcePath);
        }
        return YamlConfiguration.loadConfiguration(file);
    }
    
    /**
     * Loads a YAML configuration file
     * @param path The path relative to plugin data folder
     * @return The configuration file or null if it doesn't exist
     */
    public static FileConfiguration loadConfig(String path) {
        File file = new File(plugin.getDataFolder(), path);
        if (!file.exists()) {
            plugin.getLogger().warning("Configuration file not found: " + path);
            return null;
        }
        return YamlConfiguration.loadConfiguration(file);
    }
    
    /**
     * Saves a YAML configuration file
     * @param config The configuration to save
     * @param path The path relative to plugin data folder
     * @return true if save was successful
     */
    public static boolean saveConfig(FileConfiguration config, String path) {
        try {
            File file = new File(plugin.getDataFolder(), path);
            config.save(file);
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save configuration: " + path, e);
            return false;
        }
    }
    
    /**
     * Creates a new YAML file with the given content
     * @param path The path relative to plugin data folder
     * @param defaultContent The default content if file doesn't exist
     * @return The configuration file
     */
    public static FileConfiguration createYamlFile(String path, String defaultContent) {
        File file = new File(plugin.getDataFolder(), path);
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                Files.write(file.toPath(), defaultContent.getBytes());
                plugin.getLogger().info("Created new file: " + path);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create file: " + path, e);
                return null;
            }
        }
        return YamlConfiguration.loadConfiguration(file);
    }
    
    /**
     * Checks if a file exists
     * @param path The path relative to plugin data folder
     * @return true if file exists
     */
    public static boolean fileExists(String path) {
        return new File(plugin.getDataFolder(), path).exists();
    }
    
    /**
     * Gets the absolute path for a relative path
     * @param path The path relative to plugin data folder
     * @return The absolute path
     */
    public static Path getAbsolutePath(String path) {
        return new File(plugin.getDataFolder(), path).toPath();
    }
    
    /**
     * Deletes a file or directory
     * @param path The path relative to plugin data folder
     * @return true if deletion was successful
     */
    public static boolean delete(String path) {
        File file = new File(plugin.getDataFolder(), path);
        try {
            if (file.isDirectory()) {
                Files.walk(file.toPath())
                    .sorted((p1, p2) -> -p1.compareTo(p2))
                    .map(Path::toFile)
                    .forEach(File::delete);
            }
            return file.delete();
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete: " + path, e);
            return false;
        }
    }
}

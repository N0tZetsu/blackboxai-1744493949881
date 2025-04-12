package com.standcore.managers;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import com.standcore.StandCore;
import com.standcore.util.ConfigUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StaffModeManager {
    private final StandCore plugin;
    private final Map<UUID, Boolean> staffMode;
    private final Map<UUID, Boolean> vanished;
    private final Map<UUID, ItemStack[]> savedInventories;
    private final Map<UUID, ItemStack[]> savedArmor;
    private final Map<UUID, GameMode> savedGameModes;
    private final Set<UUID> frozenPlayers;
    private final Map<UUID, Location> lastLocations;
    
    public StaffModeManager(StandCore plugin) {
        this.plugin = plugin;
        this.staffMode = new ConcurrentHashMap<>();
        this.vanished = new ConcurrentHashMap<>();
        this.savedInventories = new ConcurrentHashMap<>();
        this.savedArmor = new ConcurrentHashMap<>();
        this.savedGameModes = new ConcurrentHashMap<>();
        this.frozenPlayers = new HashSet<>();
        this.lastLocations = new ConcurrentHashMap<>();
    }
    
    /**
     * Toggles staff mode for a player
     * @param player The player
     * @return true if enabled, false if disabled
     */
    public boolean toggleStaffMode(Player player) {
        if (isInStaffMode(player)) {
            disableStaffMode(player);
            return false;
        } else {
            enableStaffMode(player);
            return true;
        }
    }
    
    /**
     * Enables staff mode for a player
     * @param player The player
     */
    public void enableStaffMode(Player player) {
        // Save current inventory and armor
        savedInventories.put(player.getUniqueId(), player.getInventory().getContents());
        savedArmor.put(player.getUniqueId(), player.getInventory().getArmorContents());
        savedGameModes.put(player.getUniqueId(), player.getGameMode());
        
        // Clear inventory
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        
        // Set gamemode
        player.setGameMode(GameMode.CREATIVE);
        
        // Give staff tools
        giveStaffTools(player);
        
        // Enable staff mode
        staffMode.put(player.getUniqueId(), true);
        
        // Send message
        player.sendMessage(ConfigUtils.getMessage("staff.mode-enabled"));
        
        // Enable vanish if configured
        if (ConfigUtils.getBoolean("config", "settings.staff-mode.join-with-vanish", false)) {
            setVanished(player, true);
        }
    }
    
    /**
     * Disables staff mode for a player
     * @param player The player
     */
    public void disableStaffMode(Player player) {
        // Disable vanish
        if (isVanished(player)) {
            setVanished(player, false);
        }
        
        // Restore inventory and armor
        if (savedInventories.containsKey(player.getUniqueId())) {
            player.getInventory().setContents(savedInventories.remove(player.getUniqueId()));
        }
        if (savedArmor.containsKey(player.getUniqueId())) {
            player.getInventory().setArmorContents(savedArmor.remove(player.getUniqueId()));
        }
        
        // Restore gamemode
        if (savedGameModes.containsKey(player.getUniqueId())) {
            player.setGameMode(savedGameModes.remove(player.getUniqueId()));
        }
        
        // Disable staff mode
        staffMode.remove(player.getUniqueId());
        
        // Send message
        player.sendMessage(ConfigUtils.getMessage("staff.mode-disabled"));
    }
    
    /**
     * Gives staff tools to a player
     * @param player The player
     */
    private void giveStaffTools(Player player) {
        // Compass for teleportation
        ItemStack compass = createTool(Material.COMPASS, 
            ConfigUtils.getMessage("staff.tools.teleport"));
        player.getInventory().setItem(0, compass);
        
        // Book for inspection
        ItemStack book = createTool(Material.BOOK, 
            ConfigUtils.getMessage("staff.tools.inspect"));
        player.getInventory().setItem(1, book);
        
        // Blaze rod for freezing
        ItemStack blazeRod = createTool(Material.BLAZE_ROD, 
            ConfigUtils.getMessage("staff.tools.freeze"));
        player.getInventory().setItem(2, blazeRod);
        
        // Barrier for vanish
        ItemStack barrier = createTool(Material.BARRIER, 
            ConfigUtils.getMessage("staff.tools.vanish"));
        player.getInventory().setItem(8, barrier);
    }
    
    /**
     * Creates a staff tool item
     * @param material The material
     * @param name The name
     * @return The created item
     */
    private ItemStack createTool(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Toggles vanish for a player
     * @param player The player
     * @return true if enabled, false if disabled
     */
    public boolean toggleVanish(Player player) {
        return setVanished(player, !isVanished(player));
    }
    
    /**
     * Sets vanish state for a player
     * @param player The player
     * @param state The state
     * @return The new state
     */
    public boolean setVanished(Player player, boolean state) {
        if (state) {
            vanished.put(player.getUniqueId(), true);
            
            // Hide from all players
            for (Player online : plugin.getServer().getOnlinePlayers()) {
                if (!online.hasPermission("standcore.staff.vanish.see")) {
                    online.hidePlayer(plugin, player);
                }
            }
            
            player.sendMessage(ConfigUtils.getMessage("staff.vanish-enabled"));
        } else {
            vanished.remove(player.getUniqueId());
            
            // Show to all players
            for (Player online : plugin.getServer().getOnlinePlayers()) {
                online.showPlayer(plugin, player);
            }
            
            player.sendMessage(ConfigUtils.getMessage("staff.vanish-disabled"));
        }
        
        return state;
    }
    
    /**
     * Toggles freeze state for a player
     * @param target The target player
     * @param staff The staff member
     * @return true if frozen, false if unfrozen
     */
    public boolean toggleFreeze(Player target, Player staff) {
        if (isFrozen(target)) {
            frozenPlayers.remove(target.getUniqueId());
            
            target.sendMessage(ConfigUtils.getMessage("staff.you-are-unfrozen",
                "staff", staff.getName()
            ));
            
            staff.sendMessage(ConfigUtils.getMessage("staff.player-unfrozen",
                "player", target.getName()
            ));
            
            return false;
        } else {
            frozenPlayers.add(target.getUniqueId());
            
            target.sendMessage(ConfigUtils.getMessage("staff.you-are-frozen",
                "staff", staff.getName()
            ));
            
            staff.sendMessage(ConfigUtils.getMessage("staff.player-frozen",
                "player", target.getName()
            ));
            
            return true;
        }
    }
    
    /**
     * Checks if a player is in staff mode
     * @param player The player
     * @return true if in staff mode
     */
    public boolean isInStaffMode(Player player) {
        return staffMode.getOrDefault(player.getUniqueId(), false);
    }
    
    /**
     * Checks if a player is vanished
     * @param player The player
     * @return true if vanished
     */
    public boolean isVanished(Player player) {
        return vanished.getOrDefault(player.getUniqueId(), false);
    }
    
    /**
     * Checks if a player is frozen
     * @param player The player
     * @return true if frozen
     */
    public boolean isFrozen(Player player) {
        return frozenPlayers.contains(player.getUniqueId());
    }
    
    /**
     * Cleans up data for a player
     * @param player The player
     */
    public void cleanup(Player player) {
        UUID uuid = player.getUniqueId();
        staffMode.remove(uuid);
        vanished.remove(uuid);
        savedInventories.remove(uuid);
        savedArmor.remove(uuid);
        savedGameModes.remove(uuid);
        frozenPlayers.remove(uuid);
        lastLocations.remove(uuid);
    }
    
    /**
     * Sets the last location for a player
     * @param player The player
     * @param location The location
     */
    public void setLastLocation(Player player, Location location) {
        lastLocations.put(player.getUniqueId(), location);
    }
    
    /**
     * Gets the last location for a player
     * @param player The player
     * @return The location or null if not found
     */
    public Location getLastLocation(Player player) {
        return lastLocations.get(player.getUniqueId());
    }
}

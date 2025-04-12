package com.standcore.listeners;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import com.standcore.StandCore;
import com.standcore.util.ConfigUtils;

public class PlayerMoveListener implements Listener {
    private final StandCore plugin;
    
    public PlayerMoveListener(StandCore plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();
        
        // Check if player is frozen
        if (plugin.getStaffModeManager().isFrozen(player)) {
            // Allow head movement but prevent position changes
            if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
                // Cancel movement
                event.setTo(from);
                
                // Send frozen message if configured
                if (ConfigUtils.getBoolean("config", "settings.staff-mode.show-frozen-message", true)) {
                    player.sendMessage(ConfigUtils.getMessage("staff.you-are-frozen"));
                }
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        
        // Check if player is frozen
        if (plugin.getStaffModeManager().isFrozen(player)) {
            // Allow teleportation only if initiated by a staff member
            if (event.getCause() != PlayerTeleportEvent.TeleportCause.COMMAND && 
                event.getCause() != PlayerTeleportEvent.TeleportCause.PLUGIN) {
                event.setCancelled(true);
                player.sendMessage(ConfigUtils.getMessage("staff.you-are-frozen"));
            }
        }
        
        // Handle staff mode teleportation
        if (plugin.getStaffModeManager().isInStaffMode(player)) {
            // Store last location before teleport if configured
            if (ConfigUtils.getBoolean("config", "settings.staff-mode.store-last-location", true)) {
                plugin.getStaffModeManager().setLastLocation(player, event.getFrom());
            }
        }
    }
}

package com.standcore.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import com.standcore.StandCore;
import com.standcore.util.ConfigUtils;

public class PlayerInteractListener implements Listener {
    private final StandCore plugin;
    
    public PlayerInteractListener(StandCore plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        // Check if player is in staff mode
        if (!plugin.getStaffModeManager().isInStaffMode(player)) {
            return;
        }
        
        // Get item in hand
        ItemStack item = event.getItem();
        if (item == null) return;
        
        // Handle staff tools
        if (event.getAction() == Action.RIGHT_CLICK_AIR || 
            event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            
            String itemName = item.getItemMeta().getDisplayName();
            
            if (itemName.equals(ConfigUtils.getMessage("staff.tools.teleport"))) {
                // Teleport tool
                handleTeleportTool(player, event);
            } else if (itemName.equals(ConfigUtils.getMessage("staff.tools.inspect"))) {
                // Inspection tool
                handleInspectionTool(player, event);
            } else if (itemName.equals(ConfigUtils.getMessage("staff.tools.vanish"))) {
                // Vanish tool
                handleVanishTool(player);
            }
            
            // Cancel interaction with blocks while in staff mode
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        
        // Check if player is in staff mode
        if (!plugin.getStaffModeManager().isInStaffMode(player)) {
            return;
        }
        
        // Get item in hand
        ItemStack item = player.getItemInHand();
        if (item == null) return;
        
        // Check if clicked entity is a player
        if (!(event.getRightClicked() instanceof Player)) {
            return;
        }
        
        Player target = (Player) event.getRightClicked();
        String itemName = item.getItemMeta().getDisplayName();
        
        if (itemName.equals(ConfigUtils.getMessage("staff.tools.freeze"))) {
            // Freeze tool
            handleFreezeTool(player, target);
        } else if (itemName.equals(ConfigUtils.getMessage("staff.tools.inspect"))) {
            // Inspection tool on player
            handlePlayerInspection(player, target);
        }
        
        // Cancel interaction
        event.setCancelled(true);
    }
    
    /**
     * Handles the teleport tool
     */
    private void handleTeleportTool(Player player, PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            // Teleport to clicked block
            player.teleport(event.getClickedBlock().getLocation().add(0.5, 1, 0.5));
            player.sendMessage(ConfigUtils.getMessage("staff.teleported"));
        }
    }
    
    /**
     * Handles the inspection tool
     */
    private void handleInspectionTool(Player player, PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            // Show block information
            org.bukkit.block.Block block = event.getClickedBlock();
            player.sendMessage(ConfigUtils.getMessage("staff.inspect.block-info",
                "type", block.getType().name(),
                "data", String.valueOf(block.getData()),
                "location", String.format("%.2f, %.2f, %.2f", 
                    block.getLocation().getX(),
                    block.getLocation().getY(),
                    block.getLocation().getZ()
                )
            ));
        }
    }
    
    /**
     * Handles the vanish tool
     */
    private void handleVanishTool(Player player) {
        plugin.getStaffModeManager().toggleVanish(player);
    }
    
    /**
     * Handles the freeze tool
     */
    private void handleFreezeTool(Player player, Player target) {
        // Check permissions
        if (!player.hasPermission("standcore.staff.freeze")) {
            player.sendMessage(ConfigUtils.getMessage("general.no-permission"));
            return;
        }
        
        // Check if trying to freeze self
        if (target == player) {
            player.sendMessage(ConfigUtils.getMessage("general.cannot-target-self"));
            return;
        }
        
        // Check if target has higher rank
        String playerRank = plugin.getPermissionsManager().getPlayerRank(player);
        String targetRank = plugin.getPermissionsManager().getPlayerRank(target);
        
        int playerWeight = plugin.getPermissionsManager().getRank(playerRank).getWeight();
        int targetWeight = plugin.getPermissionsManager().getRank(targetRank).getWeight();
        
        if (targetWeight >= playerWeight && !player.hasPermission("standcore.staff.freeze.override")) {
            player.sendMessage(ConfigUtils.getMessage("general.cannot-target-higher-rank"));
            return;
        }
        
        // Toggle freeze
        plugin.getStaffModeManager().toggleFreeze(target, player);
    }
    
    /**
     * Handles player inspection
     */
    private void handlePlayerInspection(Player player, Player target) {
        // Show player information
        player.sendMessage(ConfigUtils.getMessage("staff.inspect.player-info",
            "name", target.getName(),
            "uuid", target.getUniqueId().toString(),
            "health", String.format("%.1f/%.1f", target.getHealth(), target.getMaxHealth()),
            "food", String.valueOf(target.getFoodLevel()),
            "gamemode", target.getGameMode().name(),
            "location", String.format("%.2f, %.2f, %.2f", 
                target.getLocation().getX(),
                target.getLocation().getY(),
                target.getLocation().getZ()
            ),
            "world", target.getWorld().getName(),
            "ip", target.getAddress().getAddress().getHostAddress()
        ));
        
        // Show rank information
        String rank = plugin.getPermissionsManager().getPlayerRank(target);
        player.sendMessage(ConfigUtils.getMessage("staff.inspect.player-rank",
            "rank", plugin.getPermissionsManager().getRank(rank).getName(),
            "prefix", plugin.getPermissionsManager().getPrefix(target)
        ));
        
        // Show active sanctions
        if (plugin.getSanctionsManager().isMuted(target)) {
            player.sendMessage(ConfigUtils.getMessage("staff.inspect.player-muted",
                "staff", plugin.getSanctionsManager().getActiveMute(target).getStaff(),
                "reason", plugin.getSanctionsManager().getActiveMute(target).getReason(),
                "remaining", ConfigUtils.formatDuration(
                    plugin.getSanctionsManager().getActiveMute(target).getRemaining()
                )
            ));
        }
    }
}

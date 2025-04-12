package com.standcore.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.standcore.StandCore;
import com.standcore.util.ConfigUtils;

public class VanishCommand implements CommandExecutor {
    private final StandCore plugin;
    
    public VanishCommand(StandCore plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if sender is a player
        if (!(sender instanceof Player)) {
            sender.sendMessage(ConfigUtils.getMessage("general.player-only"));
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check permission
        if (!player.hasPermission("standcore.staff.vanish")) {
            player.sendMessage(ConfigUtils.getMessage("general.no-permission"));
            return true;
        }
        
        // Check if in staff mode
        if (!plugin.getStaffModeManager().isInStaffMode(player)) {
            player.sendMessage(ConfigUtils.getMessage("staff.not-in-staff-mode"));
            return true;
        }
        
        // Toggle vanish
        boolean vanished = plugin.getStaffModeManager().toggleVanish(player);
        
        // Broadcast to other staff members
        if (vanished) {
            plugin.getServer().broadcast(
                ConfigUtils.getMessage("staff.player-vanished",
                    "player", player.getName()
                ),
                "standcore.staff.vanish.see"
            );
            
            // Update player's display name to show vanished status
            if (ConfigUtils.getBoolean("config", "settings.staff-mode.show-vanished-tag", true)) {
                String prefix = plugin.getPermissionsManager().getPrefix(player);
                player.setDisplayName(ConfigUtils.color(prefix + "&7[V] " + player.getName()));
                player.setPlayerListName(player.getDisplayName());
            }
        } else {
            plugin.getServer().broadcast(
                ConfigUtils.getMessage("staff.player-unvanished",
                    "player", player.getName()
                ),
                "standcore.staff.vanish.see"
            );
            
            // Restore original display name
            String prefix = plugin.getPermissionsManager().getPrefix(player);
            player.setDisplayName(ConfigUtils.color(prefix + player.getName()));
            player.setPlayerListName(player.getDisplayName());
        }
        
        // Update vanish status for all players
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (vanished) {
                if (!online.hasPermission("standcore.staff.vanish.see")) {
                    online.hidePlayer(plugin, player);
                }
            } else {
                online.showPlayer(plugin, player);
            }
        }
        
        return true;
    }
}

package com.standcore.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.standcore.StandCore;
import com.standcore.util.ConfigUtils;

public class StaffCommand implements CommandExecutor {
    private final StandCore plugin;
    
    public StaffCommand(StandCore plugin) {
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
        if (!player.hasPermission("standcore.staff")) {
            player.sendMessage(ConfigUtils.getMessage("general.no-permission"));
            return true;
        }
        
        // Toggle staff mode
        boolean enabled = plugin.getStaffModeManager().toggleStaffMode(player);
        
        // Broadcast to other staff members
        if (enabled) {
            plugin.getServer().broadcast(
                ConfigUtils.getMessage("staff.entered-staff-mode",
                    "player", player.getName()
                ),
                "standcore.staff"
            );
        } else {
            plugin.getServer().broadcast(
                ConfigUtils.getMessage("staff.left-staff-mode",
                    "player", player.getName()
                ),
                "standcore.staff"
            );
        }
        
        return true;
    }
}

package com.standcore.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.standcore.StandCore;
import com.standcore.util.ConfigUtils;

public class UnfreezeCommand implements CommandExecutor {
    private final StandCore plugin;
    
    public UnfreezeCommand(StandCore plugin) {
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
        if (!player.hasPermission("standcore.staff.freeze")) {
            player.sendMessage(ConfigUtils.getMessage("general.no-permission"));
            return true;
        }
        
        // Check if in staff mode
        if (!plugin.getStaffModeManager().isInStaffMode(player)) {
            player.sendMessage(ConfigUtils.getMessage("staff.not-in-staff-mode"));
            return true;
        }
        
        // Check arguments
        if (args.length != 1) {
            player.sendMessage(ConfigUtils.getMessage("general.invalid-args",
                "usage", "/unfreeze <player>"
            ));
            return true;
        }
        
        // Get target player
        Player target = plugin.getServer().getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(ConfigUtils.getMessage("general.player-not-found"));
            return true;
        }
        
        // Check if not frozen
        if (!plugin.getStaffModeManager().isFrozen(target)) {
            player.sendMessage(ConfigUtils.getMessage("staff.not-frozen",
                "player", target.getName()
            ));
            return true;
        }
        
        // Unfreeze player
        plugin.getStaffModeManager().toggleFreeze(target, player);
        
        // Broadcast to staff
        plugin.getServer().broadcast(
            ConfigUtils.getMessage("staff.unfreeze-broadcast",
                "staff", player.getName(),
                "player", target.getName()
            ),
            "standcore.staff"
        );
        
        return true;
    }
}

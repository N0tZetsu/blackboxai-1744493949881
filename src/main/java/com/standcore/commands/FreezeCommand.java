package com.standcore.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.standcore.StandCore;
import com.standcore.util.ConfigUtils;

public class FreezeCommand implements CommandExecutor {
    private final StandCore plugin;
    
    public FreezeCommand(StandCore plugin) {
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
                "usage", "/freeze <player>"
            ));
            return true;
        }
        
        // Get target player
        Player target = plugin.getServer().getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(ConfigUtils.getMessage("general.player-not-found"));
            return true;
        }
        
        // Check if trying to freeze self
        if (target == player) {
            player.sendMessage(ConfigUtils.getMessage("general.cannot-target-self"));
            return true;
        }
        
        // Check if target has higher rank
        String playerRank = plugin.getPermissionsManager().getPlayerRank(player);
        String targetRank = plugin.getPermissionsManager().getPlayerRank(target);
        
        int playerWeight = plugin.getPermissionsManager().getRank(playerRank).getWeight();
        int targetWeight = plugin.getPermissionsManager().getRank(targetRank).getWeight();
        
        if (targetWeight >= playerWeight && !player.hasPermission("standcore.staff.freeze.override")) {
            player.sendMessage(ConfigUtils.getMessage("general.cannot-target-higher-rank"));
            return true;
        }
        
        // Check if already frozen
        if (plugin.getStaffModeManager().isFrozen(target)) {
            player.sendMessage(ConfigUtils.getMessage("staff.already-frozen",
                "player", target.getName()
            ));
            return true;
        }
        
        // Freeze player
        plugin.getStaffModeManager().toggleFreeze(target, player);
        
        // Broadcast to staff
        plugin.getServer().broadcast(
            ConfigUtils.getMessage("staff.freeze-broadcast",
                "staff", player.getName(),
                "player", target.getName()
            ),
            "standcore.staff"
        );
        
        return true;
    }
}

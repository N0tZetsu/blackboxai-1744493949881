package com.standcore.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.standcore.StandCore;
import com.standcore.util.ConfigUtils;

public class KickCommand implements CommandExecutor {
    private final StandCore plugin;
    
    public KickCommand(StandCore plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check permission
        if (!sender.hasPermission("standcore.kick")) {
            sender.sendMessage(ConfigUtils.getMessage("general.no-permission"));
            return true;
        }
        
        // Check arguments
        if (args.length < 2) {
            sender.sendMessage(ConfigUtils.getMessage("general.invalid-args",
                "usage", "/kick <player> <reason>"
            ));
            return true;
        }
        
        // Get target player
        Player target = plugin.getServer().getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ConfigUtils.getMessage("general.player-not-found"));
            return true;
        }
        
        // Get reason
        String reason = String.join(" ", args).substring(args[0].length() + 1);
        
        // Check if sender is a player and target has higher rank
        if (sender instanceof Player) {
            Player player = (Player) sender;
            
            String playerRank = plugin.getPermissionsManager().getPlayerRank(player);
            String targetRank = plugin.getPermissionsManager().getPlayerRank(target);
            
            int playerWeight = plugin.getPermissionsManager().getRank(playerRank).getWeight();
            int targetWeight = plugin.getPermissionsManager().getRank(targetRank).getWeight();
            
            if (targetWeight >= playerWeight && !player.hasPermission("standcore.kick.override")) {
                player.sendMessage(ConfigUtils.getMessage("general.cannot-target-higher-rank"));
                return true;
            }
            
            // Check if trying to kick self
            if (target == player && !player.hasPermission("standcore.kick.self")) {
                player.sendMessage(ConfigUtils.getMessage("general.cannot-target-self"));
                return true;
            }
        }
        
        // Apply kick
        if (sender instanceof Player) {
            plugin.getSanctionsManager().kick(target, (Player) sender, reason);
        } else {
            // Create a fake "CONSOLE" player for the kick
            Player console = new org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer(
                plugin.getServer(),
                new com.mojang.authlib.GameProfile(
                    java.util.UUID.randomUUID(),
                    "CONSOLE"
                )
            ) {};
            plugin.getSanctionsManager().kick(target, console, reason);
        }
        
        // Send confirmation message
        sender.sendMessage(ConfigUtils.getMessage("sanctions.kick.success",
            "player", target.getName(),
            "reason", reason
        ));
        
        // If target was in staff mode, disable it
        if (plugin.getStaffModeManager().isInStaffMode(target)) {
            plugin.getStaffModeManager().disableStaffMode(target);
        }
        
        // If target was frozen, unfreeze them
        if (plugin.getStaffModeManager().isFrozen(target)) {
            if (sender instanceof Player) {
                plugin.getStaffModeManager().toggleFreeze(target, (Player) sender);
            } else {
                plugin.getStaffModeManager().toggleFreeze(target, null);
            }
        }
        
        return true;
    }
}

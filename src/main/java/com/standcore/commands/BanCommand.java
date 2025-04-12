package com.standcore.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.standcore.StandCore;
import com.standcore.util.ConfigUtils;

public class BanCommand implements CommandExecutor {
    private final StandCore plugin;
    
    public BanCommand(StandCore plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check permission
        if (!sender.hasPermission("standcore.ban")) {
            sender.sendMessage(ConfigUtils.getMessage("general.no-permission"));
            return true;
        }
        
        // Check arguments
        if (args.length < 2) {
            sender.sendMessage(ConfigUtils.getMessage("general.invalid-args",
                "usage", "/ban <player> [duration] <reason>"
            ));
            return true;
        }
        
        String targetName = args[0];
        String durationStr = "permanent";
        String reason;
        
        // Parse duration and reason
        if (args.length == 2) {
            // No duration specified, permanent ban
            reason = args[1];
        } else {
            try {
                // Try to parse the duration
                ConfigUtils.parseDuration(args[1]);
                durationStr = args[1];
                reason = String.join(" ", args).substring(targetName.length() + durationStr.length() + 2);
            } catch (IllegalArgumentException e) {
                // If duration parsing fails, assume it's part of the reason
                reason = String.join(" ", args).substring(targetName.length() + 1);
            }
        }
        
        // Check if sender is a player and target has higher rank
        if (sender instanceof Player) {
            Player player = (Player) sender;
            Player target = plugin.getServer().getPlayer(targetName);
            
            if (target != null) {
                String playerRank = plugin.getPermissionsManager().getPlayerRank(player);
                String targetRank = plugin.getPermissionsManager().getPlayerRank(target);
                
                int playerWeight = plugin.getPermissionsManager().getRank(playerRank).getWeight();
                int targetWeight = plugin.getPermissionsManager().getRank(targetRank).getWeight();
                
                if (targetWeight >= playerWeight && !player.hasPermission("standcore.ban.override")) {
                    player.sendMessage(ConfigUtils.getMessage("general.cannot-target-higher-rank"));
                    return true;
                }
            }
        }
        
        // Parse duration
        long duration;
        try {
            duration = durationStr.equalsIgnoreCase("permanent") ? 
                -1 : ConfigUtils.parseDuration(durationStr);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ConfigUtils.getMessage("general.invalid-duration"));
            return true;
        }
        
        // Apply ban
        if (sender instanceof Player) {
            plugin.getSanctionsManager().ban(targetName, (Player) sender, reason, duration);
        } else {
            // Create a fake "CONSOLE" player for the ban
            Player console = new org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer(
                plugin.getServer(),
                new com.mojang.authlib.GameProfile(
                    java.util.UUID.randomUUID(),
                    "CONSOLE"
                )
            ) {};
            plugin.getSanctionsManager().ban(targetName, console, reason, duration);
        }
        
        // Send confirmation message
        sender.sendMessage(ConfigUtils.getMessage("sanctions.ban.success",
            "player", targetName,
            "reason", reason,
            "duration", ConfigUtils.formatDuration(duration)
        ));
        
        return true;
    }
}

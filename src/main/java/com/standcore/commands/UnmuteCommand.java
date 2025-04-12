package com.standcore.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.standcore.StandCore;
import com.standcore.util.ConfigUtils;

public class UnmuteCommand implements CommandExecutor {
    private final StandCore plugin;
    
    public UnmuteCommand(StandCore plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check permission
        if (!sender.hasPermission("standcore.unmute")) {
            sender.sendMessage(ConfigUtils.getMessage("general.no-permission"));
            return true;
        }
        
        // Check arguments
        if (args.length != 1) {
            sender.sendMessage(ConfigUtils.getMessage("general.invalid-args",
                "usage", "/unmute <player>"
            ));
            return true;
        }
        
        // Get target player
        Player target = plugin.getServer().getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ConfigUtils.getMessage("general.player-not-found"));
            return true;
        }
        
        // Check if not muted
        if (!plugin.getSanctionsManager().isMuted(target)) {
            sender.sendMessage(ConfigUtils.getMessage("sanctions.unmute.not-muted",
                "player", target.getName()
            ));
            return true;
        }
        
        // Check if sender is a player and target has higher rank
        if (sender instanceof Player) {
            Player player = (Player) sender;
            
            String playerRank = plugin.getPermissionsManager().getPlayerRank(player);
            String targetRank = plugin.getPermissionsManager().getPlayerRank(target);
            
            int playerWeight = plugin.getPermissionsManager().getRank(playerRank).getWeight();
            int targetWeight = plugin.getPermissionsManager().getRank(targetRank).getWeight();
            
            if (targetWeight >= playerWeight && !player.hasPermission("standcore.unmute.override")) {
                player.sendMessage(ConfigUtils.getMessage("general.cannot-target-higher-rank"));
                return true;
            }
            
            // Check if trying to unmute self
            if (target == player && !player.hasPermission("standcore.unmute.self")) {
                player.sendMessage(ConfigUtils.getMessage("general.cannot-target-self"));
                return true;
            }
        }
        
        // Remove mute
        if (sender instanceof Player) {
            plugin.getSanctionsManager().unmute(target, (Player) sender);
        } else {
            // Create a fake "CONSOLE" player for the unmute
            Player console = new org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer(
                plugin.getServer(),
                new com.mojang.authlib.GameProfile(
                    java.util.UUID.randomUUID(),
                    "CONSOLE"
                )
            ) {};
            plugin.getSanctionsManager().unmute(target, console);
        }
        
        // Send confirmation message
        sender.sendMessage(ConfigUtils.getMessage("sanctions.unmute.success",
            "player", target.getName()
        ));
        
        return true;
    }
}

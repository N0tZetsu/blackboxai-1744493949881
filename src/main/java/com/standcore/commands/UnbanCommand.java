package com.standcore.commands;

import org.bukkit.BanList;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.standcore.StandCore;
import com.standcore.util.ConfigUtils;

public class UnbanCommand implements CommandExecutor {
    private final StandCore plugin;
    
    public UnbanCommand(StandCore plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check permission
        if (!sender.hasPermission("standcore.unban")) {
            sender.sendMessage(ConfigUtils.getMessage("general.no-permission"));
            return true;
        }
        
        // Check arguments
        if (args.length != 1) {
            sender.sendMessage(ConfigUtils.getMessage("general.invalid-args",
                "usage", "/unban <player>"
            ));
            return true;
        }
        
        String targetName = args[0];
        
        // Check if player is banned
        if (!plugin.getServer().getBanList(BanList.Type.NAME).isBanned(targetName)) {
            sender.sendMessage(ConfigUtils.getMessage("sanctions.unban.not-banned",
                "player", targetName
            ));
            return true;
        }
        
        // Remove ban
        if (sender instanceof Player) {
            plugin.getSanctionsManager().unban(targetName, (Player) sender);
        } else {
            // Create a fake "CONSOLE" player for the unban
            Player console = new org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer(
                plugin.getServer(),
                new com.mojang.authlib.GameProfile(
                    java.util.UUID.randomUUID(),
                    "CONSOLE"
                )
            ) {};
            plugin.getSanctionsManager().unban(targetName, console);
        }
        
        // Send confirmation message
        sender.sendMessage(ConfigUtils.getMessage("sanctions.unban.success",
            "player", targetName
        ));
        
        // Broadcast if enabled
        if (ConfigUtils.getBoolean("config", "settings.sanctions.broadcast-unbans", true)) {
            String staffName = sender instanceof Player ? 
                sender.getName() : "CONSOLE";
            
            plugin.getServer().broadcast(
                ConfigUtils.getMessage("sanctions.unban.broadcast",
                    "player", targetName,
                    "staff", staffName
                ),
                "standcore.sanctions.see"
            );
        }
        
        // Log to console
        plugin.getLogger().info(sender.getName() + " unbanned " + targetName);
        
        return true;
    }
}

package com.standcore.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.standcore.StandCore;
import com.standcore.util.ConfigUtils;

public class WarnCommand implements CommandExecutor {
    private final StandCore plugin;
    
    public WarnCommand(StandCore plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check permission
        if (!sender.hasPermission("standcore.warn")) {
            sender.sendMessage(ConfigUtils.getMessage("general.no-permission"));
            return true;
        }
        
        // Check arguments
        if (args.length < 2) {
            sender.sendMessage(ConfigUtils.getMessage("general.invalid-args",
                "usage", "/warn <player> <reason>"
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
            
            if (targetWeight >= playerWeight && !player.hasPermission("standcore.warn.override")) {
                player.sendMessage(ConfigUtils.getMessage("general.cannot-target-higher-rank"));
                return true;
            }
            
            // Check if trying to warn self
            if (target == player && !player.hasPermission("standcore.warn.self")) {
                player.sendMessage(ConfigUtils.getMessage("general.cannot-target-self"));
                return true;
            }
        }
        
        // Apply warning
        if (sender instanceof Player) {
            plugin.getSanctionsManager().warn(target, (Player) sender, reason);
        } else {
            // Create a fake "CONSOLE" player for the warning
            Player console = new org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer(
                plugin.getServer(),
                new com.mojang.authlib.GameProfile(
                    java.util.UUID.randomUUID(),
                    "CONSOLE"
                )
            ) {};
            plugin.getSanctionsManager().warn(target, console, reason);
        }
        
        // Send confirmation message
        sender.sendMessage(ConfigUtils.getMessage("sanctions.warn.success",
            "player", target.getName(),
            "reason", reason
        ));
        
        // Check for auto-punishments if configured
        int warnCount = plugin.getSanctionsManager().getSanctionHistory(target.getName()).stream()
            .filter(s -> s.getType() == SanctionsManager.SanctionType.WARN)
            .count();
        
        // Get auto-punishment configuration
        if (ConfigUtils.getBoolean("config", "settings.sanctions.auto-punish.enabled", false)) {
            int muteThreshold = ConfigUtils.getInt("config", 
                "settings.sanctions.auto-punish.warn-count-for-mute", -1);
            int kickThreshold = ConfigUtils.getInt("config", 
                "settings.sanctions.auto-punish.warn-count-for-kick", -1);
            int banThreshold = ConfigUtils.getInt("config", 
                "settings.sanctions.auto-punish.warn-count-for-ban", -1);
            
            String autoPunishReason = ConfigUtils.getMessage("sanctions.auto-punish.reason",
                "count", String.valueOf(warnCount)
            );
            
            if (banThreshold > 0 && warnCount >= banThreshold) {
                // Auto-ban
                if (sender instanceof Player) {
                    plugin.getSanctionsManager().ban(target.getName(), (Player) sender, 
                        autoPunishReason, -1);
                } else {
                    plugin.getSanctionsManager().ban(target.getName(), console, 
                        autoPunishReason, -1);
                }
            } else if (kickThreshold > 0 && warnCount >= kickThreshold) {
                // Auto-kick
                if (sender instanceof Player) {
                    plugin.getSanctionsManager().kick(target, (Player) sender, 
                        autoPunishReason);
                } else {
                    plugin.getSanctionsManager().kick(target, console, 
                        autoPunishReason);
                }
            } else if (muteThreshold > 0 && warnCount >= muteThreshold) {
                // Auto-mute
                long muteDuration = ConfigUtils.parseDuration(
                    ConfigUtils.getString("config", 
                        "settings.sanctions.auto-punish.mute-duration", "1h")
                );
                
                if (sender instanceof Player) {
                    plugin.getSanctionsManager().mute(target, (Player) sender, 
                        autoPunishReason, muteDuration);
                } else {
                    plugin.getSanctionsManager().mute(target, console, 
                        autoPunishReason, muteDuration);
                }
            }
        }
        
        return true;
    }
}

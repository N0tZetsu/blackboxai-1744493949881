package com.standcore.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.standcore.StandCore;
import com.standcore.util.ConfigUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class AdminChatCommand implements CommandExecutor {
    private final StandCore plugin;
    private final Set<UUID> toggledPlayers;
    
    public AdminChatCommand(StandCore plugin) {
        this.plugin = plugin;
        this.toggledPlayers = new HashSet<>();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if sender is a player
        if (!(sender instanceof Player)) {
            if (args.length > 0) {
                // Allow console to send messages to admin chat
                String message = String.join(" ", args);
                broadcastAdminMessage("CONSOLE", message);
            } else {
                sender.sendMessage(ConfigUtils.getMessage("general.console-usage",
                    "usage", "/ac <message>"
                ));
            }
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check permission
        if (!player.hasPermission("standcore.adminchat")) {
            player.sendMessage(ConfigUtils.getMessage("general.no-permission"));
            return true;
        }
        
        // If no args, toggle admin chat mode
        if (args.length == 0) {
            toggleAdminChat(player);
            return true;
        }
        
        // Send message to admin chat
        String message = String.join(" ", args);
        
        // Check for direct message syntax
        if (message.startsWith("!")) {
            message = message.substring(1);
            Player target = null;
            
            // Find the target player name
            String[] parts = message.split(" ");
            if (parts.length >= 2) {
                target = plugin.getServer().getPlayer(parts[0]);
                message = message.substring(parts[0].length() + 1);
            }
            
            if (target != null && target.hasPermission("standcore.adminchat.see")) {
                // Send direct message
                sendDirectMessage(player, target, message);
            } else {
                player.sendMessage(ConfigUtils.getMessage("adminchat.invalid-target"));
            }
        } else {
            // Regular admin chat message
            broadcastAdminMessage(player.getName(), message);
        }
        
        return true;
    }
    
    /**
     * Toggles admin chat mode for a player
     * @param player The player
     */
    private void toggleAdminChat(Player player) {
        if (toggledPlayers.contains(player.getUniqueId())) {
            toggledPlayers.remove(player.getUniqueId());
            player.sendMessage(ConfigUtils.getMessage("adminchat.toggled-off"));
        } else {
            toggledPlayers.add(player.getUniqueId());
            player.sendMessage(ConfigUtils.getMessage("adminchat.toggled-on"));
        }
    }
    
    /**
     * Broadcasts a message to all players with admin chat permission
     * @param sender The sender's name
     * @param message The message
     */
    private void broadcastAdminMessage(String sender, String message) {
        String format = ConfigUtils.getMessage("adminchat.format",
            "player", sender,
            "message", message
        );
        
        plugin.getServer().broadcast(format, "standcore.adminchat.see");
        
        // Log to console
        plugin.getLogger().info("[AdminChat] " + sender + ": " + message);
    }
    
    /**
     * Sends a direct message to a specific staff member
     * @param sender The sender
     * @param target The target
     * @param message The message
     */
    private void sendDirectMessage(Player sender, Player target, String message) {
        String format = ConfigUtils.getMessage("adminchat.direct-message",
            "player", sender.getName(),
            "target", target.getName(),
            "message", message
        );
        
        // Send to sender and target
        sender.sendMessage(format);
        target.sendMessage(format);
        
        // Log to console
        plugin.getLogger().info("[AdminChat] " + sender.getName() + " -> " + 
            target.getName() + ": " + message);
        
        // Notify other staff members if configured
        if (ConfigUtils.getBoolean("config", "settings.adminchat.broadcast-direct-messages", true)) {
            String spyFormat = ConfigUtils.getMessage("adminchat.direct-message-spy",
                "player", sender.getName(),
                "target", target.getName(),
                "message", message
            );
            
            plugin.getServer().getOnlinePlayers().stream()
                .filter(p -> p != sender && p != target)
                .filter(p -> p.hasPermission("standcore.adminchat.see"))
                .forEach(p -> p.sendMessage(spyFormat));
        }
    }
    
    /**
     * Checks if a player has admin chat toggled on
     * @param player The player
     * @return true if toggled on
     */
    public boolean hasAdminChatToggled(Player player) {
        return toggledPlayers.contains(player.getUniqueId());
    }
    
    /**
     * Removes a player from the toggled set
     * @param player The player
     */
    public void removePlayer(Player player) {
        toggledPlayers.remove(player.getUniqueId());
    }
}

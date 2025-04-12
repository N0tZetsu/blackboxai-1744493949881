package com.standcore.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import com.standcore.StandCore;
import com.standcore.commands.AdminChatCommand;
import com.standcore.managers.SanctionsManager.Sanction;
import com.standcore.util.ConfigUtils;

public class ChatListener implements Listener {
    private final StandCore plugin;
    
    public ChatListener(StandCore plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        
        // Check if player is muted
        if (plugin.getSanctionsManager().isMuted(player)) {
            Sanction mute = plugin.getSanctionsManager().getActiveMute(player);
            player.sendMessage(ConfigUtils.getMessage("sanctions.mute.message",
                "staff", mute.getStaff(),
                "reason", mute.getReason(),
                "duration", ConfigUtils.formatDuration(mute.getRemaining())
            ));
            event.setCancelled(true);
            return;
        }
        
        // Check if player has admin chat toggled
        AdminChatCommand adminChat = (AdminChatCommand) plugin.getCommand("adminchat").getExecutor();
        if (adminChat.hasAdminChatToggled(player)) {
            // Redirect message to admin chat
            event.setCancelled(true);
            String message = event.getMessage();
            
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
                    String format = ConfigUtils.getMessage("adminchat.direct-message",
                        "player", player.getName(),
                        "target", target.getName(),
                        "message", message
                    );
                    
                    // Send to sender and target
                    player.sendMessage(format);
                    target.sendMessage(format);
                    
                    // Notify other staff members if configured
                    if (ConfigUtils.getBoolean("config", 
                        "settings.adminchat.broadcast-direct-messages", true)) {
                        String spyFormat = ConfigUtils.getMessage("adminchat.direct-message-spy",
                            "player", player.getName(),
                            "target", target.getName(),
                            "message", message
                        );
                        
                        plugin.getServer().getOnlinePlayers().stream()
                            .filter(p -> p != player && p != target)
                            .filter(p -> p.hasPermission("standcore.adminchat.see"))
                            .forEach(p -> p.sendMessage(spyFormat));
                    }
                } else {
                    player.sendMessage(ConfigUtils.getMessage("adminchat.invalid-target"));
                }
            } else {
                // Regular admin chat message
                String format = ConfigUtils.getMessage("adminchat.format",
                    "player", player.getName(),
                    "message", message
                );
                
                plugin.getServer().broadcast(format, "standcore.adminchat.see");
                
                // Log to console
                plugin.getLogger().info("[AdminChat] " + player.getName() + ": " + message);
            }
            return;
        }
        
        // Format regular chat message
        String format = ConfigUtils.getString("ranks", "settings.format.chat",
            "%prefix% %player%: %message%");
        
        format = format.replace("%prefix%", plugin.getPermissionsManager().getPrefix(player))
                      .replace("%player%", "%1$s")
                      .replace("%message%", "%2$s");
        
        event.setFormat(ConfigUtils.color(format));
    }
}

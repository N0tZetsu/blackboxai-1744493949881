package com.standcore.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import com.standcore.StandCore;
import com.standcore.util.ConfigUtils;

public class PlayerJoinListener implements Listener {
    private final StandCore plugin;
    
    public PlayerJoinListener(StandCore plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Set up permissions
        String defaultRank = ConfigUtils.getString("ranks", "settings.default-rank", "default");
        plugin.getPermissionsManager().setupPermissions(player, defaultRank);
        
        // Check for active grants
        plugin.getGrantsManager().getActiveGrants(player.getUniqueId()).stream()
            .filter(grant -> !grant.isExpired())
            .findFirst()
            .ifPresent(grant -> {
                // Apply the grant's rank
                plugin.getPermissionsManager().setupPermissions(player, grant.getRank());
                
                // If temporary, show remaining time
                if (!grant.isPermanent()) {
                    player.sendMessage(ConfigUtils.getMessage("grants.temporary-remaining",
                        "rank", plugin.getPermissionsManager().getRank(grant.getRank()).getName(),
                        "time", ConfigUtils.formatDuration(grant.getRemaining())
                    ));
                }
            });
        
        // Check if player should join with staff mode
        if (player.hasPermission("standcore.staff") && 
            ConfigUtils.getBoolean("config", "settings.staff-mode.join-with-staff-mode", false)) {
            plugin.getStaffModeManager().enableStaffMode(player);
        }
        
        // Hide vanished players
        plugin.getServer().getOnlinePlayers().forEach(online -> {
            if (plugin.getStaffModeManager().isVanished(online) && 
                !player.hasPermission("standcore.staff.vanish.see")) {
                player.hidePlayer(plugin, online);
            }
        });
        
        // Check if player is muted
        if (plugin.getSanctionsManager().isMuted(player)) {
            player.sendMessage(ConfigUtils.getMessage("sanctions.mute.message",
                "staff", plugin.getSanctionsManager().getActiveMute(player).getStaff(),
                "reason", plugin.getSanctionsManager().getActiveMute(player).getReason(),
                "duration", ConfigUtils.formatDuration(
                    plugin.getSanctionsManager().getActiveMute(player).getRemaining()
                )
            ));
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Clean up staff mode data
        if (plugin.getStaffModeManager().isInStaffMode(player)) {
            plugin.getStaffModeManager().disableStaffMode(player);
        }
        
        // Clean up permissions
        plugin.getPermissionsManager().clearPermissions(player);
        
        // If player was frozen, notify staff
        if (plugin.getStaffModeManager().isFrozen(player)) {
            plugin.getServer().broadcast(
                ConfigUtils.getMessage("staff.frozen-player-left",
                    "player", player.getName()
                ),
                "standcore.staff"
            );
        }
    }
}

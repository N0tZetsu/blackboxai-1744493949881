package com.standcore.managers;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import com.standcore.StandCore;
import com.standcore.managers.GrantsManager.Grant;
import com.standcore.util.ConfigUtils;

import java.util.List;

public class PlaceholderManager extends PlaceholderExpansion {
    private final StandCore plugin;
    
    public PlaceholderManager(StandCore plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getIdentifier() {
        return "standcore";
    }
    
    @Override
    public String getAuthor() {
        return "StandCore";
    }
    
    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) return "";
        
        switch (identifier.toLowerCase()) {
            case "rank":
                return plugin.getPermissionsManager().getRank(
                    plugin.getPermissionsManager().getPlayerRank(player)
                ).getName();
                
            case "prefix":
                return plugin.getPermissionsManager().getPrefix(player);
                
            case "grant_remaining":
                List<Grant> grants = plugin.getGrantsManager().getActiveGrants(player.getUniqueId());
                if (grants.isEmpty()) return "None";
                Grant latestGrant = grants.get(grants.size() - 1);
                if (latestGrant.isPermanent()) return "Permanent";
                return ConfigUtils.formatDuration(latestGrant.getRemaining());
                
            case "staff_mode":
                return plugin.getStaffModeManager().isInStaffMode(player) ? "Yes" : "No";
                
            case "vanished":
                return plugin.getStaffModeManager().isVanished(player) ? "Yes" : "No";
                
            case "frozen":
                return plugin.getStaffModeManager().isFrozen(player) ? "Yes" : "No";
                
            case "muted":
                return plugin.getSanctionsManager().isMuted(player) ? "Yes" : "No";
                
            case "mute_remaining":
                if (!plugin.getSanctionsManager().isMuted(player)) return "Not muted";
                SanctionsManager.Sanction mute = plugin.getSanctionsManager().getActiveMute(player);
                if (mute.isPermanent()) return "Permanent";
                return ConfigUtils.formatDuration(mute.getRemaining());
                
            case "mute_reason":
                if (!plugin.getSanctionsManager().isMuted(player)) return "Not muted";
                return plugin.getSanctionsManager().getActiveMute(player).getReason();
                
            case "grants_count":
                return String.valueOf(
                    plugin.getGrantsManager().getGrantHistory(player.getUniqueId()).size()
                );
                
            case "sanctions_count":
                return String.valueOf(
                    plugin.getSanctionsManager().getSanctionHistory(player.getName()).size()
                );
                
            case "warns_count":
                return String.valueOf(
                    plugin.getSanctionsManager().getSanctionHistory(player.getName()).stream()
                        .filter(s -> s.getType() == SanctionsManager.SanctionType.WARN)
                        .count()
                );
                
            case "rank_weight":
                return String.valueOf(
                    plugin.getPermissionsManager().getRank(
                        plugin.getPermissionsManager().getPlayerRank(player)
                    ).getWeight()
                );
                
            case "rank_display":
                String rankName = plugin.getPermissionsManager().getPlayerRank(player);
                PermissionsManager.RankData rank = plugin.getPermissionsManager().getRank(rankName);
                return rank.getPrefix() + rank.getName();
        }
        
        return null;
    }
    
    /**
     * Registers this expansion with PlaceholderAPI
     */
    public void register() {
        if (this.register()) {
            plugin.getLogger().info("Registered PlaceholderAPI expansion!");
        }
    }
    
    /**
     * Unregisters this expansion
     */
    public void unregister() {
        this.unregister();
    }
}

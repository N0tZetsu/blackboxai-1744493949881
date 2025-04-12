package com.standcore.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import com.standcore.StandCore;
import com.standcore.util.ConfigUtils;
import com.standcore.util.FileUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GrantsManager {
    private final StandCore plugin;
    private final Map<UUID, List<Grant>> activeGrants;
    private final SimpleDateFormat dateFormat;
    
    public GrantsManager(StandCore plugin) {
        this.plugin = plugin;
        this.activeGrants = new ConcurrentHashMap<>();
        this.dateFormat = new SimpleDateFormat(
            ConfigUtils.getString("config", "settings.date-format", "dd/MM/yyyy HH:mm:ss")
        );
        
        // Start expiration checker
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::checkExpirations, 20L * 60, 20L * 60);
    }
    
    /**
     * Grants a rank to a player
     * @param target The target player
     * @param rank The rank name
     * @param duration Duration in milliseconds (-1 for permanent)
     * @param granter The player granting the rank
     * @return true if grant was successful
     */
    public boolean grantRank(Player target, String rank, long duration, Player granter) {
        // Validate rank exists
        if (!plugin.getPermissionsManager().rankExists(rank)) {
            return false;
        }
        
        // Create grant
        long expiration = duration == -1 ? -1 : System.currentTimeMillis() + duration;
        Grant grant = new Grant(
            UUID.randomUUID(),
            rank,
            granter.getName(),
            granter.getUniqueId(),
            System.currentTimeMillis(),
            expiration
        );
        
        // Add to active grants
        List<Grant> playerGrants = activeGrants.computeIfAbsent(
            target.getUniqueId(), 
            k -> new ArrayList<>()
        );
        playerGrants.add(grant);
        
        // Save to history
        saveGrant(target.getUniqueId(), grant);
        
        // Apply permissions
        plugin.getPermissionsManager().setupPermissions(target, rank);
        
        // Send messages
        target.sendMessage(ConfigUtils.getMessage("grants.received-rank",
            "rank", plugin.getPermissionsManager().getRank(rank).getName(),
            "duration", formatDuration(duration)
        ));
        
        granter.sendMessage(ConfigUtils.getMessage("grants.granted-rank",
            "player", target.getName(),
            "rank", plugin.getPermissionsManager().getRank(rank).getName(),
            "duration", formatDuration(duration)
        ));
        
        return true;
    }
    
    /**
     * Gets a player's grant history
     * @param uuid The player's UUID
     * @return List of grants
     */
    public List<Grant> getGrantHistory(UUID uuid) {
        File file = new File(plugin.getDataFolder(), "data/grants/" + uuid + ".yml");
        if (!file.exists()) {
            return new ArrayList<>();
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<Grant> grants = new ArrayList<>();
        
        ConfigurationSection grantsSection = config.getConfigurationSection("grants");
        if (grantsSection != null) {
            for (String key : grantsSection.getKeys(false)) {
                ConfigurationSection grantSection = grantsSection.getConfigurationSection(key);
                if (grantSection == null) continue;
                
                grants.add(new Grant(
                    UUID.fromString(key),
                    grantSection.getString("rank"),
                    grantSection.getString("granter"),
                    UUID.fromString(grantSection.getString("granter-uuid")),
                    grantSection.getLong("granted"),
                    grantSection.getLong("expiration")
                ));
            }
        }
        
        // Sort by date (newest first)
        grants.sort((g1, g2) -> Long.compare(g2.getGranted(), g1.getGranted()));
        return grants;
    }
    
    /**
     * Gets a player's active grants
     * @param uuid The player's UUID
     * @return List of active grants
     */
    public List<Grant> getActiveGrants(UUID uuid) {
        return activeGrants.getOrDefault(uuid, new ArrayList<>());
    }
    
    /**
     * Saves a grant to the player's history
     * @param uuid The player's UUID
     * @param grant The grant to save
     */
    private void saveGrant(UUID uuid, Grant grant) {
        File file = new File(plugin.getDataFolder(), "data/grants/" + uuid + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        
        ConfigurationSection grantSection = config.createSection("grants." + grant.getId().toString());
        grantSection.set("rank", grant.getRank());
        grantSection.set("granter", grant.getGranter());
        grantSection.set("granter-uuid", grant.getGranterUUID().toString());
        grantSection.set("granted", grant.getGranted());
        grantSection.set("expiration", grant.getExpiration());
        
        try {
            config.save(file);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save grant history for " + uuid);
            e.printStackTrace();
        }
    }
    
    /**
     * Checks for expired grants
     */
    private void checkExpirations() {
        long now = System.currentTimeMillis();
        
        for (Map.Entry<UUID, List<Grant>> entry : activeGrants.entrySet()) {
            UUID uuid = entry.getKey();
            List<Grant> grants = entry.getValue();
            
            // Remove expired grants
            grants.removeIf(grant -> {
                if (grant.getExpiration() != -1 && grant.getExpiration() <= now) {
                    Player player = plugin.getServer().getPlayer(uuid);
                    if (player != null) {
                        // Reset to default rank
                        String defaultRank = ConfigUtils.getString("ranks", "settings.default-rank", "default");
                        plugin.getPermissionsManager().setupPermissions(player, defaultRank);
                        
                        player.sendMessage(ConfigUtils.getMessage("grants.expired",
                            "rank", plugin.getPermissionsManager().getRank(grant.getRank()).getName()
                        ));
                    }
                    return true;
                }
                return false;
            });
            
            // Remove entry if no grants left
            if (grants.isEmpty()) {
                activeGrants.remove(uuid);
            }
        }
    }
    
    /**
     * Formats a duration for display
     * @param duration Duration in milliseconds
     * @return Formatted duration string
     */
    private String formatDuration(long duration) {
        return duration == -1 ? 
            ConfigUtils.getMessage("grants.permanent") : 
            ConfigUtils.getMessage("grants.temporary", "time", ConfigUtils.formatDuration(duration));
    }
    
    /**
     * Class to hold grant data
     */
    public static class Grant {
        private final UUID id;
        private final String rank;
        private final String granter;
        private final UUID granterUUID;
        private final long granted;
        private final long expiration;
        
        public Grant(UUID id, String rank, String granter, UUID granterUUID, long granted, long expiration) {
            this.id = id;
            this.rank = rank;
            this.granter = granter;
            this.granterUUID = granterUUID;
            this.granted = granted;
            this.expiration = expiration;
        }
        
        public UUID getId() { return id; }
        public String getRank() { return rank; }
        public String getGranter() { return granter; }
        public UUID getGranterUUID() { return granterUUID; }
        public long getGranted() { return granted; }
        public long getExpiration() { return expiration; }
        
        public boolean isExpired() {
            return expiration != -1 && expiration <= System.currentTimeMillis();
        }
        
        public boolean isPermanent() {
            return expiration == -1;
        }
        
        public long getRemaining() {
            return expiration == -1 ? -1 : Math.max(0, expiration - System.currentTimeMillis());
        }
    }
}

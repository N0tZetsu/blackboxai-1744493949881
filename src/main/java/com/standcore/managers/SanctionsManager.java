package com.standcore.managers;

import org.bukkit.BanList;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import com.standcore.StandCore;
import com.standcore.util.ConfigUtils;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SanctionsManager {
    private final StandCore plugin;
    private final Map<UUID, Sanction> activeMutes;
    private final Map<UUID, List<Sanction>> sanctionHistory;
    
    public SanctionsManager(StandCore plugin) {
        this.plugin = plugin;
        this.activeMutes = new ConcurrentHashMap<>();
        this.sanctionHistory = new ConcurrentHashMap<>();
        
        // Start mute expiration checker
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, 
            this::checkMuteExpirations, 20L * 60, 20L * 60);
        
        // Load active mutes
        loadActiveMutes();
    }
    
    /**
     * Bans a player
     * @param target The target player name or UUID
     * @param staff The staff member
     * @param reason The reason
     * @param duration Duration in milliseconds (-1 for permanent)
     * @return true if successful
     */
    public boolean ban(String target, Player staff, String reason, long duration) {
        Date expiry = duration == -1 ? null : new Date(System.currentTimeMillis() + duration);
        
        // Apply ban
        plugin.getServer().getBanList(BanList.Type.NAME).addBan(
            target, 
            formatBanScreen(staff.getName(), reason, duration),
            expiry, 
            staff.getName()
        );
        
        // Kick if online
        Player targetPlayer = plugin.getServer().getPlayer(target);
        if (targetPlayer != null) {
            targetPlayer.kickPlayer(formatBanScreen(staff.getName(), reason, duration));
        }
        
        // Save to history
        Sanction sanction = new Sanction(
            UUID.randomUUID(),
            SanctionType.BAN,
            target,
            staff.getName(),
            staff.getUniqueId(),
            reason,
            System.currentTimeMillis(),
            duration == -1 ? -1 : System.currentTimeMillis() + duration
        );
        saveSanction(target, sanction);
        
        // Broadcast if enabled
        if (ConfigUtils.getBoolean("config", "settings.sanctions.broadcast-bans", true)) {
            plugin.getServer().broadcastMessage(ConfigUtils.getMessage("sanctions.ban.broadcast",
                "player", target,
                "staff", staff.getName(),
                "reason", reason
            ));
        }
        
        return true;
    }
    
    /**
     * Mutes a player
     * @param target The target player
     * @param staff The staff member
     * @param reason The reason
     * @param duration Duration in milliseconds (-1 for permanent)
     * @return true if successful
     */
    public boolean mute(Player target, Player staff, String reason, long duration) {
        // Create mute
        Sanction mute = new Sanction(
            UUID.randomUUID(),
            SanctionType.MUTE,
            target.getName(),
            staff.getName(),
            staff.getUniqueId(),
            reason,
            System.currentTimeMillis(),
            duration == -1 ? -1 : System.currentTimeMillis() + duration
        );
        
        // Add to active mutes
        activeMutes.put(target.getUniqueId(), mute);
        
        // Save to history
        saveSanction(target.getName(), mute);
        
        // Notify target
        target.sendMessage(ConfigUtils.getMessage("sanctions.mute.message",
            "staff", staff.getName(),
            "reason", reason,
            "duration", formatDuration(duration)
        ));
        
        // Broadcast if enabled
        if (ConfigUtils.getBoolean("config", "settings.sanctions.broadcast-mutes", true)) {
            plugin.getServer().broadcastMessage(ConfigUtils.getMessage("sanctions.mute.broadcast",
                "player", target.getName(),
                "staff", staff.getName(),
                "reason", reason
            ));
        }
        
        return true;
    }
    
    /**
     * Kicks a player
     * @param target The target player
     * @param staff The staff member
     * @param reason The reason
     * @return true if successful
     */
    public boolean kick(Player target, Player staff, String reason) {
        // Create kick record
        Sanction kick = new Sanction(
            UUID.randomUUID(),
            SanctionType.KICK,
            target.getName(),
            staff.getName(),
            staff.getUniqueId(),
            reason,
            System.currentTimeMillis(),
            -1
        );
        
        // Save to history
        saveSanction(target.getName(), kick);
        
        // Kick player
        target.kickPlayer(ConfigUtils.getMessage("sanctions.kick.screen",
            "staff", staff.getName(),
            "reason", reason
        ));
        
        // Broadcast if enabled
        if (ConfigUtils.getBoolean("config", "settings.sanctions.broadcast-kicks", true)) {
            plugin.getServer().broadcastMessage(ConfigUtils.getMessage("sanctions.kick.broadcast",
                "player", target.getName(),
                "staff", staff.getName(),
                "reason", reason
            ));
        }
        
        return true;
    }
    
    /**
     * Warns a player
     * @param target The target player
     * @param staff The staff member
     * @param reason The reason
     * @return true if successful
     */
    public boolean warn(Player target, Player staff, String reason) {
        // Create warning record
        Sanction warning = new Sanction(
            UUID.randomUUID(),
            SanctionType.WARN,
            target.getName(),
            staff.getName(),
            staff.getUniqueId(),
            reason,
            System.currentTimeMillis(),
            -1
        );
        
        // Save to history
        saveSanction(target.getName(), warning);
        
        // Notify target
        target.sendMessage(ConfigUtils.getMessage("sanctions.warn.message",
            "staff", staff.getName(),
            "reason", reason
        ));
        
        // Broadcast if enabled
        if (ConfigUtils.getBoolean("config", "settings.sanctions.broadcast-warns", false)) {
            plugin.getServer().broadcastMessage(ConfigUtils.getMessage("sanctions.warn.broadcast",
                "player", target.getName(),
                "staff", staff.getName(),
                "reason", reason
            ));
        }
        
        return true;
    }
    
    /**
     * Unbans a player
     * @param target The target player name
     * @param staff The staff member
     * @return true if successful
     */
    public boolean unban(String target, Player staff) {
        if (!plugin.getServer().getBanList(BanList.Type.NAME).isBanned(target)) {
            staff.sendMessage(ConfigUtils.getMessage("sanctions.unban.not-banned",
                "player", target
            ));
            return false;
        }
        
        // Remove ban
        plugin.getServer().getBanList(BanList.Type.NAME).pardon(target);
        
        // Broadcast if enabled
        plugin.getServer().broadcastMessage(ConfigUtils.getMessage("sanctions.unban.broadcast",
            "player", target,
            "staff", staff.getName()
        ));
        
        return true;
    }
    
    /**
     * Unmutes a player
     * @param target The target player
     * @param staff The staff member
     * @return true if successful
     */
    public boolean unmute(Player target, Player staff) {
        if (!isMuted(target)) {
            staff.sendMessage(ConfigUtils.getMessage("sanctions.unmute.not-muted",
                "player", target.getName()
            ));
            return false;
        }
        
        // Remove mute
        activeMutes.remove(target.getUniqueId());
        
        // Notify target
        target.sendMessage(ConfigUtils.getMessage("sanctions.unmute.message",
            "staff", staff.getName()
        ));
        
        // Broadcast if enabled
        plugin.getServer().broadcastMessage(ConfigUtils.getMessage("sanctions.unmute.broadcast",
            "player", target.getName(),
            "staff", staff.getName()
        ));
        
        return true;
    }
    
    /**
     * Checks if a player is muted
     * @param player The player
     * @return true if muted
     */
    public boolean isMuted(Player player) {
        Sanction mute = activeMutes.get(player.getUniqueId());
        if (mute == null) return false;
        
        // Check if expired
        if (mute.getExpiration() != -1 && mute.getExpiration() <= System.currentTimeMillis()) {
            activeMutes.remove(player.getUniqueId());
            return false;
        }
        
        return true;
    }
    
    /**
     * Gets a player's active mute
     * @param player The player
     * @return The mute or null if not muted
     */
    public Sanction getActiveMute(Player player) {
        return activeMutes.get(player.getUniqueId());
    }
    
    /**
     * Gets a player's sanction history
     * @param playerName The player name
     * @return List of sanctions
     */
    public List<Sanction> getSanctionHistory(String playerName) {
        File file = new File(plugin.getDataFolder(), "data/sanctions/" + playerName + ".yml");
        if (!file.exists()) {
            return new ArrayList<>();
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<Sanction> sanctions = new ArrayList<>();
        
        ConfigurationSection sanctionsSection = config.getConfigurationSection("sanctions");
        if (sanctionsSection != null) {
            for (String key : sanctionsSection.getKeys(false)) {
                ConfigurationSection sanctionSection = sanctionsSection.getConfigurationSection(key);
                if (sanctionSection == null) continue;
                
                sanctions.add(new Sanction(
                    UUID.fromString(key),
                    SanctionType.valueOf(sanctionSection.getString("type")),
                    sanctionSection.getString("target"),
                    sanctionSection.getString("staff"),
                    UUID.fromString(sanctionSection.getString("staff-uuid")),
                    sanctionSection.getString("reason"),
                    sanctionSection.getLong("timestamp"),
                    sanctionSection.getLong("expiration")
                ));
            }
        }
        
        // Sort by date (newest first)
        sanctions.sort((s1, s2) -> Long.compare(s2.getTimestamp(), s1.getTimestamp()));
        return sanctions;
    }
    
    /**
     * Saves a sanction to history
     * @param playerName The player name
     * @param sanction The sanction
     */
    private void saveSanction(String playerName, Sanction sanction) {
        File file = new File(plugin.getDataFolder(), "data/sanctions/" + playerName + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        
        ConfigurationSection sanctionSection = config.createSection(
            "sanctions." + sanction.getId().toString()
        );
        sanctionSection.set("type", sanction.getType().name());
        sanctionSection.set("target", sanction.getTarget());
        sanctionSection.set("staff", sanction.getStaff());
        sanctionSection.set("staff-uuid", sanction.getStaffUUID().toString());
        sanctionSection.set("reason", sanction.getReason());
        sanctionSection.set("timestamp", sanction.getTimestamp());
        sanctionSection.set("expiration", sanction.getExpiration());
        
        try {
            config.save(file);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save sanction history for " + playerName);
            e.printStackTrace();
        }
    }
    
    /**
     * Loads active mutes from files
     */
    private void loadActiveMutes() {
        File sanctionsDir = new File(plugin.getDataFolder(), "data/sanctions");
        if (!sanctionsDir.exists()) return;
        
        for (File file : sanctionsDir.listFiles()) {
            if (!file.getName().endsWith(".yml")) continue;
            
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection sanctionsSection = config.getConfigurationSection("sanctions");
            if (sanctionsSection == null) continue;
            
            for (String key : sanctionsSection.getKeys(false)) {
                ConfigurationSection sanctionSection = sanctionsSection.getConfigurationSection(key);
                if (sanctionSection == null) continue;
                
                if (sanctionSection.getString("type").equals("MUTE")) {
                    long expiration = sanctionSection.getLong("expiration");
                    if (expiration == -1 || expiration > System.currentTimeMillis()) {
                        String target = sanctionSection.getString("target");
                        Player player = plugin.getServer().getPlayer(target);
                        if (player != null) {
                            activeMutes.put(player.getUniqueId(), new Sanction(
                                UUID.fromString(key),
                                SanctionType.MUTE,
                                target,
                                sanctionSection.getString("staff"),
                                UUID.fromString(sanctionSection.getString("staff-uuid")),
                                sanctionSection.getString("reason"),
                                sanctionSection.getLong("timestamp"),
                                expiration
                            ));
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Checks for expired mutes
     */
    private void checkMuteExpirations() {
        long now = System.currentTimeMillis();
        
        activeMutes.entrySet().removeIf(entry -> {
            Sanction mute = entry.getValue();
            if (mute.getExpiration() != -1 && mute.getExpiration() <= now) {
                Player player = plugin.getServer().getPlayer(entry.getKey());
                if (player != null) {
                    player.sendMessage(ConfigUtils.getMessage("sanctions.mute.expired"));
                }
                return true;
            }
            return false;
        });
    }
    
    /**
     * Formats the ban screen
     * @param staff The staff member
     * @param reason The reason
     * @param duration The duration
     * @return The formatted screen
     */
    private String formatBanScreen(String staff, String reason, long duration) {
        return ConfigUtils.getMessage("sanctions.ban.screen",
            "staff", staff,
            "reason", reason,
            "duration", formatDuration(duration),
            "expires", duration == -1 ? "Never" : 
                new Date(System.currentTimeMillis() + duration).toString()
        );
    }
    
    /**
     * Formats a duration for display
     * @param duration Duration in milliseconds
     * @return Formatted duration string
     */
    private String formatDuration(long duration) {
        return duration == -1 ? "permanent" : ConfigUtils.formatDuration(duration);
    }
    
    /**
     * Sanction types
     */
    public enum SanctionType {
        BAN,
        MUTE,
        KICK,
        WARN
    }
    
    /**
     * Class to hold sanction data
     */
    public static class Sanction {
        private final UUID id;
        private final SanctionType type;
        private final String target;
        private final String staff;
        private final UUID staffUUID;
        private final String reason;
        private final long timestamp;
        private final long expiration;
        
        public Sanction(UUID id, SanctionType type, String target, String staff, 
                       UUID staffUUID, String reason, long timestamp, long expiration) {
            this.id = id;
            this.type = type;
            this.target = target;
            this.staff = staff;
            this.staffUUID = staffUUID;
            this.reason = reason;
            this.timestamp = timestamp;
            this.expiration = expiration;
        }
        
        public UUID getId() { return id; }
        public SanctionType getType() { return type; }
        public String getTarget() { return target; }
        public String getStaff() { return staff; }
        public UUID getStaffUUID() { return staffUUID; }
        public String getReason() { return reason; }
        public long getTimestamp() { return timestamp; }
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

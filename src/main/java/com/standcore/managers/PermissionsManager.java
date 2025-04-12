package com.standcore.managers;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import com.standcore.StandCore;
import com.standcore.util.ConfigUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PermissionsManager {
    private final StandCore plugin;
    private final Map<UUID, PermissionAttachment> attachments;
    private final Map<String, RankData> ranks;
    private final Map<UUID, String> playerRanks;
    private final Scoreboard scoreboard;
    
    public PermissionsManager(StandCore plugin) {
        this.plugin = plugin;
        this.attachments = new ConcurrentHashMap<>();
        this.ranks = new HashMap<>();
        this.playerRanks = new ConcurrentHashMap<>();
        this.scoreboard = plugin.getServer().getScoreboardManager().getMainScoreboard();
        
        loadRanks();
    }
    
    /**
     * Loads all ranks from ranks.yml
     */
    public void loadRanks() {
        ranks.clear();
        ConfigurationSection ranksSection = ConfigUtils.getSection("ranks", "ranks");
        
        if (ranksSection == null) {
            plugin.getLogger().warning("No ranks found in ranks.yml!");
            return;
        }
        
        // Load each rank
        for (String rankName : ranksSection.getKeys(false)) {
            ConfigurationSection rankSection = ranksSection.getConfigurationSection(rankName);
            if (rankSection == null) continue;
            
            String name = rankSection.getString("name", rankName);
            String prefix = ConfigUtils.color(rankSection.getString("prefix", "&7"));
            int weight = rankSection.getInt("weight", 0);
            List<String> permissions = rankSection.getStringList("permissions");
            List<String> inheritance = rankSection.getStringList("inheritance");
            
            RankData rank = new RankData(rankName, name, prefix, weight, permissions, inheritance);
            ranks.put(rankName.toLowerCase(), rank);
            
            // Create or update team for this rank
            String teamName = "z" + String.format("%03d", weight) + rankName;
            if (teamName.length() > 16) teamName = teamName.substring(0, 16);
            
            Team team = scoreboard.getTeam(teamName);
            if (team == null) {
                team = scoreboard.registerNewTeam(teamName);
            }
            team.setPrefix(prefix);
        }
    }
    
    /**
     * Sets up permissions for a player
     * @param player The player
     * @param rankName The rank name
     */
    public void setupPermissions(Player player, String rankName) {
        // Remove existing permissions
        clearPermissions(player);
        
        RankData rank = ranks.get(rankName.toLowerCase());
        if (rank == null) {
            rank = ranks.get(ConfigUtils.getString("ranks", "settings.default-rank", "default"));
            if (rank == null) {
                plugin.getLogger().warning("Default rank not found for player: " + player.getName());
                return;
            }
        }
        
        // Create new attachment
        PermissionAttachment attachment = player.addAttachment(plugin);
        attachments.put(player.getUniqueId(), attachment);
        
        // Apply permissions from rank and its inheritance
        Set<String> appliedPermissions = new HashSet<>();
        applyRankPermissions(rank, attachment, appliedPermissions);
        
        // Update player's rank
        playerRanks.put(player.getUniqueId(), rankName.toLowerCase());
        
        // Update player's team
        updatePlayerTeam(player, rank);
    }
    
    /**
     * Applies permissions from a rank and its inheritance
     * @param rank The rank
     * @param attachment The permission attachment
     * @param appliedPermissions Set of already applied permissions
     */
    private void applyRankPermissions(RankData rank, PermissionAttachment attachment, Set<String> appliedPermissions) {
        // Apply inherited permissions first
        for (String inheritedRankName : rank.getInheritance()) {
            RankData inheritedRank = ranks.get(inheritedRankName.toLowerCase());
            if (inheritedRank != null) {
                applyRankPermissions(inheritedRank, attachment, appliedPermissions);
            }
        }
        
        // Apply rank's permissions
        for (String permission : rank.getPermissions()) {
            if (!appliedPermissions.contains(permission)) {
                if (permission.endsWith(".*")) {
                    String basePermission = permission.substring(0, permission.length() - 2);
                    attachment.setPermission(basePermission + ".*", true);
                } else {
                    attachment.setPermission(permission, true);
                }
                appliedPermissions.add(permission);
            }
        }
    }
    
    /**
     * Updates a player's team based on their rank
     * @param player The player
     * @param rank The rank
     */
    private void updatePlayerTeam(Player player, RankData rank) {
        String teamName = "z" + String.format("%03d", rank.getWeight()) + rank.getId();
        if (teamName.length() > 16) teamName = teamName.substring(0, 16);
        
        // Remove from old team
        Team currentTeam = scoreboard.getEntryTeam(player.getName());
        if (currentTeam != null) {
            currentTeam.removeEntry(player.getName());
        }
        
        // Add to new team
        Team newTeam = scoreboard.getTeam(teamName);
        if (newTeam != null) {
            newTeam.addEntry(player.getName());
        }
    }
    
    /**
     * Clears permissions for a player
     * @param player The player
     */
    public void clearPermissions(Player player) {
        PermissionAttachment attachment = attachments.remove(player.getUniqueId());
        if (attachment != null) {
            player.removeAttachment(attachment);
        }
        playerRanks.remove(player.getUniqueId());
        
        Team team = scoreboard.getEntryTeam(player.getName());
        if (team != null) {
            team.removeEntry(player.getName());
        }
    }
    
    /**
     * Gets a player's rank
     * @param player The player
     * @return The rank name or default rank
     */
    public String getPlayerRank(Player player) {
        return playerRanks.getOrDefault(player.getUniqueId(), 
            ConfigUtils.getString("ranks", "settings.default-rank", "default"));
    }
    
    /**
     * Gets a rank's data
     * @param rankName The rank name
     * @return The rank data or null if not found
     */
    public RankData getRank(String rankName) {
        return ranks.get(rankName.toLowerCase());
    }
    
    /**
     * Gets all available ranks
     * @return Map of rank name to rank data
     */
    public Map<String, RankData> getRanks() {
        return Collections.unmodifiableMap(ranks);
    }
    
    /**
     * Checks if a rank exists
     * @param rankName The rank name
     * @return true if the rank exists
     */
    public boolean rankExists(String rankName) {
        return ranks.containsKey(rankName.toLowerCase());
    }
    
    /**
     * Gets a player's prefix
     * @param player The player
     * @return The prefix or empty string if no rank
     */
    public String getPrefix(Player player) {
        String rankName = getPlayerRank(player);
        RankData rank = ranks.get(rankName);
        return rank != null ? rank.getPrefix() : "";
    }
    
    /**
     * Class to hold rank data
     */
    public static class RankData {
        private final String id;
        private final String name;
        private final String prefix;
        private final int weight;
        private final List<String> permissions;
        private final List<String> inheritance;
        
        public RankData(String id, String name, String prefix, int weight, 
                       List<String> permissions, List<String> inheritance) {
            this.id = id;
            this.name = name;
            this.prefix = prefix;
            this.weight = weight;
            this.permissions = permissions;
            this.inheritance = inheritance;
        }
        
        public String getId() { return id; }
        public String getName() { return name; }
        public String getPrefix() { return prefix; }
        public int getWeight() { return weight; }
        public List<String> getPermissions() { return permissions; }
        public List<String> getInheritance() { return inheritance; }
    }
}

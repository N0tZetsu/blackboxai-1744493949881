package com.standcore.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import com.standcore.StandCore;
import com.standcore.gui.RankSelectionGUI;
import com.standcore.gui.RankSelectionGUI.GuiData;
import com.standcore.gui.RankSelectionGUI.GuiType;
import com.standcore.util.ConfigUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InventoryClickListener implements Listener {
    private final StandCore plugin;
    private final RankSelectionGUI rankGui;
    private final Map<UUID, Long> customDurationRequests;
    
    public InventoryClickListener(StandCore plugin) {
        this.plugin = plugin;
        this.rankGui = new RankSelectionGUI(plugin);
        this.customDurationRequests = new HashMap<>();
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        
        // Check if player has an open GUI
        GuiData data = rankGui.getGuiData(player);
        if (data == null) return;
        
        event.setCancelled(true);
        
        // Get clicked item
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        
        // Handle based on GUI type
        switch (data.getType()) {
            case RANK_SELECTION:
                handleRankSelection(player, clicked, data);
                break;
                
            case DURATION_SELECTION:
                handleDurationSelection(player, clicked, data);
                break;
                
            case CONFIRMATION:
                handleConfirmation(player, clicked, data);
                break;
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        
        // Check if player has an open GUI
        GuiData data = rankGui.getGuiData(player);
        if (data == null) return;
        
        // If waiting for custom duration input, don't remove GUI data
        if (customDurationRequests.containsKey(player.getUniqueId())) {
            return;
        }
        
        // Remove GUI data
        rankGui.removeGuiData(player);
    }
    
    /**
     * Handles rank selection GUI clicks
     */
    private void handleRankSelection(Player player, ItemStack clicked, GuiData data) {
        String displayName = clicked.getItemMeta().getDisplayName();
        
        // Find clicked rank
        for (String rankId : plugin.getPermissionsManager().getRanks().keySet()) {
            String rankName = ConfigUtils.color(ConfigUtils.getSection("ranks", 
                "ranks." + rankId + ".icon").getString("name"));
            
            if (displayName.equals(rankName)) {
                // Open duration selection
                rankGui.openDurationSelection(player, 
                    plugin.getServer().getPlayer(data.getTargetUUID()), rankId);
                return;
            }
        }
    }
    
    /**
     * Handles duration selection GUI clicks
     */
    private void handleDurationSelection(Player player, ItemStack clicked, GuiData data) {
        String displayName = clicked.getItemMeta().getDisplayName();
        
        // Check for back button
        if (displayName.equals(ConfigUtils.color(
            ConfigUtils.getString("config", "gui.back-button.name", "&cGo Back")
        ))) {
            rankGui.openRankSelection(player, 
                plugin.getServer().getPlayer(data.getTargetUUID()));
            return;
        }
        
        // Check for custom duration
        if (displayName.equals(ConfigUtils.color("&6Custom Duration"))) {
            player.closeInventory();
            player.sendMessage(ConfigUtils.getMessage("grants.gui.custom-duration"));
            customDurationRequests.put(player.getUniqueId(), System.currentTimeMillis());
            return;
        }
        
        // Handle preset duration
        String duration = displayName.substring(2); // Remove color code
        if (duration.equalsIgnoreCase("Permanent")) {
            duration = "permanent";
        }
        
        // Open confirmation
        rankGui.openConfirmation(player, 
            plugin.getServer().getPlayer(data.getTargetUUID()),
            data.getSelectedRank(), duration);
    }
    
    /**
     * Handles confirmation GUI clicks
     */
    private void handleConfirmation(Player player, ItemStack clicked, GuiData data) {
        String displayName = clicked.getItemMeta().getDisplayName();
        
        if (displayName.equals(ConfigUtils.color("&aConfirm"))) {
            // Get target player
            Player target = plugin.getServer().getPlayer(data.getTargetUUID());
            if (target == null) {
                player.sendMessage(ConfigUtils.getMessage("general.player-not-found"));
                player.closeInventory();
                return;
            }
            
            // Parse duration
            long duration;
            try {
                duration = data.getSelectedDuration().equalsIgnoreCase("permanent") ? 
                    -1 : ConfigUtils.parseDuration(data.getSelectedDuration());
            } catch (IllegalArgumentException e) {
                player.sendMessage(ConfigUtils.getMessage("general.invalid-duration"));
                player.closeInventory();
                return;
            }
            
            // Apply grant
            plugin.getGrantsManager().grantRank(target, data.getSelectedRank(), 
                duration, player);
            
            player.closeInventory();
        } else if (displayName.equals(ConfigUtils.color("&cCancel"))) {
            player.closeInventory();
        }
    }
    
    /**
     * Handles custom duration chat input
     * @param player The player
     * @param message The message
     * @return true if handled
     */
    public boolean handleCustomDuration(Player player, String message) {
        if (!customDurationRequests.containsKey(player.getUniqueId())) {
            return false;
        }
        
        // Remove request
        customDurationRequests.remove(player.getUniqueId());
        
        // Get GUI data
        GuiData data = rankGui.getGuiData(player);
        if (data == null) return false;
        
        // Validate duration
        try {
            ConfigUtils.parseDuration(message);
        } catch (IllegalArgumentException e) {
            player.sendMessage(ConfigUtils.getMessage("general.invalid-duration"));
            rankGui.openDurationSelection(player, 
                plugin.getServer().getPlayer(data.getTargetUUID()),
                data.getSelectedRank());
            return true;
        }
        
        // Open confirmation
        rankGui.openConfirmation(player, 
            plugin.getServer().getPlayer(data.getTargetUUID()),
            data.getSelectedRank(), message);
        
        return true;
    }
    
    /**
     * Checks if a player is waiting for custom duration input
     * @param player The player
     * @return true if waiting
     */
    public boolean isWaitingForDuration(Player player) {
        return customDurationRequests.containsKey(player.getUniqueId());
    }
}

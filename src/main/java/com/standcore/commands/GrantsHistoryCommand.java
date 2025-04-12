package com.standcore.commands;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.standcore.StandCore;
import com.standcore.managers.GrantsManager.Grant;
import com.standcore.util.ConfigUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class GrantsHistoryCommand implements CommandExecutor {
    private final StandCore plugin;
    private final SimpleDateFormat dateFormat;
    
    public GrantsHistoryCommand(StandCore plugin) {
        this.plugin = plugin;
        this.dateFormat = new SimpleDateFormat(
            ConfigUtils.getString("config", "settings.date-format", "dd/MM/yyyy HH:mm:ss")
        );
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check permission
        if (!sender.hasPermission("standcore.grantshistory")) {
            sender.sendMessage(ConfigUtils.getMessage("general.no-permission"));
            return true;
        }
        
        // Check arguments
        if (args.length != 1) {
            sender.sendMessage(ConfigUtils.getMessage("general.invalid-args",
                "usage", "/grantshistory <player>"
            ));
            return true;
        }
        
        // Get target player
        OfflinePlayer target = plugin.getServer().getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(ConfigUtils.getMessage("general.player-not-found"));
            return true;
        }
        
        // Get grant history
        List<Grant> history = plugin.getGrantsManager().getGrantHistory(target.getUniqueId());
        
        if (history.isEmpty()) {
            sender.sendMessage(ConfigUtils.getMessage("grants.history-empty",
                "player", target.getName()
            ));
            return true;
        }
        
        // Send header
        sender.sendMessage(ConfigUtils.getMessage("grants.history-header",
            "player", target.getName()
        ));
        
        // If sender is a player and has permission, create a book
        if (sender instanceof Player && sender.hasPermission("standcore.grantshistory.book")) {
            sendBookHistory((Player) sender, target.getName(), history);
        } else {
            // Otherwise send chat messages
            sendChatHistory(sender, history);
        }
        
        return true;
    }
    
    /**
     * Sends grant history in chat format
     * @param sender The command sender
     * @param history The grant history
     */
    private void sendChatHistory(CommandSender sender, List<Grant> history) {
        for (Grant grant : history) {
            String rankName = plugin.getPermissionsManager().getRank(grant.getRank()).getName();
            String duration = grant.isPermanent() ? 
                ConfigUtils.getMessage("grants.permanent") :
                ConfigUtils.formatDuration(grant.getExpiration() - grant.getGranted());
            
            sender.sendMessage(ConfigUtils.getMessage("grants.history-entry",
                "rank", rankName,
                "granter", grant.getGranter(),
                "duration", duration,
                "date", dateFormat.format(new Date(grant.getGranted()))
            ));
        }
    }
    
    /**
     * Sends grant history in book format
     * @param player The player
     * @param targetName The target player name
     * @param history The grant history
     */
    private void sendBookHistory(Player player, String targetName, List<Grant> history) {
        org.bukkit.inventory.ItemStack book = new org.bukkit.inventory.ItemStack(
            org.bukkit.Material.WRITTEN_BOOK
        );
        
        org.bukkit.inventory.meta.BookMeta meta = 
            (org.bukkit.inventory.meta.BookMeta) book.getItemMeta();
        
        meta.setTitle(ConfigUtils.color("&bGrant History"));
        meta.setAuthor("StandCore");
        
        StringBuilder content = new StringBuilder();
        content.append(ConfigUtils.color("&8=== &bGrant History &8===\n"));
        content.append(ConfigUtils.color("&7Player: &f" + targetName + "\n\n"));
        
        for (Grant grant : history) {
            String rankName = plugin.getPermissionsManager().getRank(grant.getRank()).getName();
            String duration = grant.isPermanent() ? 
                "permanent" : ConfigUtils.formatDuration(grant.getExpiration() - grant.getGranted());
            
            content.append(ConfigUtils.color("&7Rank: &f" + rankName + "\n"));
            content.append(ConfigUtils.color("&7By: &f" + grant.getGranter() + "\n"));
            content.append(ConfigUtils.color("&7Duration: &f" + duration + "\n"));
            content.append(ConfigUtils.color("&7Date: &f" + 
                dateFormat.format(new Date(grant.getGranted())) + "\n\n"));
            
            // Split into pages if needed (max 256 chars per page)
            if (content.length() > 256) {
                meta.addPage(content.toString());
                content = new StringBuilder();
            }
        }
        
        // Add remaining content as last page
        if (content.length() > 0) {
            meta.addPage(content.toString());
        }
        
        book.setItemMeta(meta);
        player.openBook(book);
    }
}

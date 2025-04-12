package com.standcore.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.standcore.StandCore;
import com.standcore.gui.RankSelectionGUI;
import com.standcore.util.ConfigUtils;

public class GrantCommand implements CommandExecutor {
    private final StandCore plugin;
    private final RankSelectionGUI gui;
    
    public GrantCommand(StandCore plugin) {
        this.plugin = plugin;
        this.gui = new RankSelectionGUI(plugin);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if sender is a player
        if (!(sender instanceof Player)) {
            sender.sendMessage(ConfigUtils.getMessage("general.player-only"));
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check permission
        if (!player.hasPermission("standcore.grant")) {
            player.sendMessage(ConfigUtils.getMessage("general.no-permission"));
            return true;
        }
        
        // Check arguments
        if (args.length != 1) {
            player.sendMessage(ConfigUtils.getMessage("general.invalid-args",
                "usage", "/grant <player>"
            ));
            return true;
        }
        
        // Get target player
        Player target = plugin.getServer().getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(ConfigUtils.getMessage("general.player-not-found"));
            return true;
        }
        
        // Check if trying to grant to self
        if (target == player && !player.hasPermission("standcore.grant.self")) {
            player.sendMessage(ConfigUtils.getMessage("general.cannot-target-self"));
            return true;
        }
        
        // Check if target has higher rank
        String playerRank = plugin.getPermissionsManager().getPlayerRank(player);
        String targetRank = plugin.getPermissionsManager().getPlayerRank(target);
        
        int playerWeight = plugin.getPermissionsManager().getRank(playerRank).getWeight();
        int targetWeight = plugin.getPermissionsManager().getRank(targetRank).getWeight();
        
        if (targetWeight >= playerWeight && !player.hasPermission("standcore.grant.override")) {
            player.sendMessage(ConfigUtils.getMessage("general.cannot-target-higher-rank"));
            return true;
        }
        
        // Open rank selection GUI
        gui.openRankSelection(player, target);
        return true;
    }
}

package com.standcore.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.inventory.InventoryType;
import com.standcore.StandCore;
import com.standcore.util.ConfigUtils;

public class InvSeeCommand implements CommandExecutor {
    private final StandCore plugin;
    
    public InvSeeCommand(StandCore plugin) {
        this.plugin = plugin;
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
        if (!player.hasPermission("standcore.staff.invsee")) {
            player.sendMessage(ConfigUtils.getMessage("general.no-permission"));
            return true;
        }
        
        // Check if in staff mode
        if (!plugin.getStaffModeManager().isInStaffMode(player)) {
            player.sendMessage(ConfigUtils.getMessage("staff.not-in-staff-mode"));
            return true;
        }
        
        // Check arguments
        if (args.length < 1 || args.length > 2) {
            player.sendMessage(ConfigUtils.getMessage("general.invalid-args",
                "usage", "/invsee <player> [armor]"
            ));
            return true;
        }
        
        // Get target player
        Player target = plugin.getServer().getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(ConfigUtils.getMessage("general.player-not-found"));
            return true;
        }
        
        // Check if trying to view own inventory
        if (target == player) {
            player.sendMessage(ConfigUtils.getMessage("general.cannot-target-self"));
            return true;
        }
        
        // Check if target has higher rank
        String playerRank = plugin.getPermissionsManager().getPlayerRank(player);
        String targetRank = plugin.getPermissionsManager().getPlayerRank(target);
        
        int playerWeight = plugin.getPermissionsManager().getRank(playerRank).getWeight();
        int targetWeight = plugin.getPermissionsManager().getRank(targetRank).getWeight();
        
        if (targetWeight >= playerWeight && !player.hasPermission("standcore.staff.invsee.override")) {
            player.sendMessage(ConfigUtils.getMessage("general.cannot-target-higher-rank"));
            return true;
        }
        
        // Check if viewing armor
        if (args.length == 2 && args[1].equalsIgnoreCase("armor")) {
            openArmorInventory(player, target);
        } else {
            openMainInventory(player, target);
        }
        
        // Log to staff
        plugin.getServer().broadcast(
            ConfigUtils.getMessage("staff.invsee-broadcast",
                "staff", player.getName(),
                "player", target.getName()
            ),
            "standcore.staff"
        );
        
        return true;
    }
    
    /**
     * Opens a player's main inventory
     * @param staff The staff member
     * @param target The target player
     */
    private void openMainInventory(Player staff, Player target) {
        Inventory inv = plugin.getServer().createInventory(
            null,
            45,
            ConfigUtils.color("&8" + target.getName() + "'s Inventory")
        );
        
        // Copy inventory contents
        ItemStack[] contents = target.getInventory().getContents();
        for (int i = 0; i < contents.length && i < 36; i++) {
            if (contents[i] != null) {
                inv.setItem(i, contents[i].clone());
            }
        }
        
        // Add armor items at the bottom
        ItemStack[] armor = target.getInventory().getArmorContents();
        for (int i = 0; i < armor.length; i++) {
            if (armor[i] != null) {
                inv.setItem(36 + i, armor[i].clone());
            }
        }
        
        // Add offhand item
        ItemStack offhand = target.getInventory().getItemInOffHand();
        if (offhand != null && offhand.getType() != org.bukkit.Material.AIR) {
            inv.setItem(40, offhand.clone());
        }
        
        // Open inventory
        staff.openInventory(inv);
        staff.sendMessage(ConfigUtils.getMessage("staff.invsee-opened",
            "player", target.getName()
        ));
    }
    
    /**
     * Opens a player's armor inventory
     * @param staff The staff member
     * @param target The target player
     */
    private void openArmorInventory(Player staff, Player target) {
        Inventory inv = plugin.getServer().createInventory(
            null,
            InventoryType.HOPPER,
            ConfigUtils.color("&8" + target.getName() + "'s Armor")
        );
        
        // Copy armor contents
        ItemStack[] armor = target.getInventory().getArmorContents();
        for (int i = 0; i < armor.length; i++) {
            if (armor[i] != null) {
                inv.setItem(i, armor[i].clone());
            }
        }
        
        // Add offhand item
        ItemStack offhand = target.getInventory().getItemInOffHand();
        if (offhand != null && offhand.getType() != org.bukkit.Material.AIR) {
            inv.setItem(4, offhand.clone());
        }
        
        // Open inventory
        staff.openInventory(inv);
        staff.sendMessage(ConfigUtils.getMessage("staff.invsee-armor-opened",
            "player", target.getName()
        ));
    }
}

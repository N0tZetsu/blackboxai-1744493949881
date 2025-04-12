package com.standcore.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import com.standcore.StandCore;
import com.standcore.managers.PermissionsManager.RankData;
import com.standcore.util.ConfigUtils;

import java.util.*;

public class RankSelectionGUI {
    private final StandCore plugin;
    private final Map<UUID, GuiData> openGuis;
    
    public RankSelectionGUI(StandCore plugin) {
        this.plugin = plugin;
        this.openGuis = new HashMap<>();
    }
    
    /**
     * Opens the rank selection GUI for a player
     * @param staff The staff member
     * @param target The target player
     */
    public void openRankSelection(Player staff, Player target) {
        int size = 54; // 6 rows
        Inventory inv = Bukkit.createInventory(null, size, 
            ConfigUtils.getString("config", "settings.grants.gui.title", "&8Grant Menu")
        );
        
        // Get all ranks sorted by weight
        List<RankData> ranks = new ArrayList<>(plugin.getPermissionsManager().getRanks().values());
        ranks.sort((r1, r2) -> Integer.compare(r2.getWeight(), r1.getWeight()));
        
        // Add rank items
        int slot = 10;
        for (RankData rank : ranks) {
            // Skip ranks that are higher than the staff's rank
            RankData staffRank = plugin.getPermissionsManager().getRank(
                plugin.getPermissionsManager().getPlayerRank(staff)
            );
            if (rank.getWeight() >= staffRank.getWeight()) continue;
            
            // Create rank item
            ConfigurationSection iconSection = ConfigUtils.getSection("ranks", 
                "ranks." + rank.getId() + ".icon");
            
            Material material = Material.valueOf(
                iconSection.getString("material", "STONE")
            );
            
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            
            meta.setDisplayName(ConfigUtils.color(iconSection.getString("name")));
            
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ConfigUtils.color("&7Weight: &f" + rank.getWeight()));
            lore.add(ConfigUtils.color("&7Prefix: " + rank.getPrefix()));
            lore.add("");
            
            // Add configured lore
            List<String> configLore = iconSection.getStringList("lore");
            if (!configLore.isEmpty()) {
                lore.addAll(ConfigUtils.colorList(configLore));
                lore.add("");
            }
            
            lore.add(ConfigUtils.color("&eClick to select"));
            meta.setLore(lore);
            
            if (iconSection.getBoolean("glow", false)) {
                meta.addEnchant(
                    org.bukkit.enchantments.Enchantment.DURABILITY, 1, true
                );
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }
            
            item.setItemMeta(meta);
            inv.setItem(slot, item);
            
            // Update slot
            slot++;
            if ((slot % 9) == 8) slot += 3;
            if (slot >= size) break;
        }
        
        // Fill empty slots
        if (ConfigUtils.getBoolean("config", "gui.fill-empty-slots", true)) {
            ItemStack filler = new ItemStack(Material.valueOf(
                ConfigUtils.getString("config", "gui.fill-material", "BLACK_STAINED_GLASS_PANE")
            ));
            ItemMeta fillerMeta = filler.getItemMeta();
            fillerMeta.setDisplayName(" ");
            filler.setItemMeta(fillerMeta);
            
            for (int i = 0; i < size; i++) {
                if (inv.getItem(i) == null) {
                    inv.setItem(i, filler);
                }
            }
        }
        
        // Store GUI data
        openGuis.put(staff.getUniqueId(), new GuiData(GuiType.RANK_SELECTION, target.getUniqueId()));
        
        // Open inventory
        staff.openInventory(inv);
    }
    
    /**
     * Opens the duration selection GUI
     * @param staff The staff member
     * @param target The target player
     * @param rank The selected rank
     */
    public void openDurationSelection(Player staff, Player target, String rank) {
        int size = 27; // 3 rows
        Inventory inv = Bukkit.createInventory(null, size,
            ConfigUtils.getString("config", "settings.grants.gui.duration-title", "&8Select Duration")
        );
        
        // Add duration options
        List<String> durations = ConfigUtils.getConfig("config")
            .getStringList("settings.grants.default-durations");
        
        int slot = 10;
        for (String duration : durations) {
            Material material = duration.equalsIgnoreCase("permanent") ? 
                Material.DIAMOND : Material.CLOCK;
            
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            
            String display = duration.equalsIgnoreCase("permanent") ? 
                "&6Permanent" : "&6" + duration;
            meta.setDisplayName(ConfigUtils.color(display));
            
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ConfigUtils.color("&7Click to select this duration"));
            meta.setLore(lore);
            
            item.setItemMeta(meta);
            inv.setItem(slot++, item);
        }
        
        // Add custom duration option
        if (ConfigUtils.getBoolean("config", "settings.grants.custom-duration-allowed", true)) {
            ItemStack custom = new ItemStack(Material.PAPER);
            ItemMeta customMeta = custom.getItemMeta();
            customMeta.setDisplayName(ConfigUtils.color("&6Custom Duration"));
            
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ConfigUtils.color("&7Click to enter a custom duration"));
            lore.add(ConfigUtils.color("&7Format: 1d, 1w, 1mo, etc."));
            customMeta.setLore(lore);
            
            custom.setItemMeta(customMeta);
            inv.setItem(16, custom);
        }
        
        // Add back button
        ItemStack back = new ItemStack(Material.valueOf(
            ConfigUtils.getString("config", "gui.back-button.material", "ARROW")
        ));
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName(ConfigUtils.color(
            ConfigUtils.getString("config", "gui.back-button.name", "&cGo Back")
        ));
        back.setItemMeta(backMeta);
        inv.setItem(18, back);
        
        // Fill empty slots
        if (ConfigUtils.getBoolean("config", "gui.fill-empty-slots", true)) {
            ItemStack filler = new ItemStack(Material.valueOf(
                ConfigUtils.getString("config", "gui.fill-material", "BLACK_STAINED_GLASS_PANE")
            ));
            ItemMeta fillerMeta = filler.getItemMeta();
            fillerMeta.setDisplayName(" ");
            filler.setItemMeta(fillerMeta);
            
            for (int i = 0; i < size; i++) {
                if (inv.getItem(i) == null) {
                    inv.setItem(i, filler);
                }
            }
        }
        
        // Store GUI data
        GuiData data = new GuiData(GuiType.DURATION_SELECTION, target.getUniqueId());
        data.setSelectedRank(rank);
        openGuis.put(staff.getUniqueId(), data);
        
        // Open inventory
        staff.openInventory(inv);
    }
    
    /**
     * Opens the confirmation GUI
     * @param staff The staff member
     * @param target The target player
     * @param rank The selected rank
     * @param duration The selected duration
     */
    public void openConfirmation(Player staff, Player target, String rank, String duration) {
        int size = 27; // 3 rows
        Inventory inv = Bukkit.createInventory(null, size,
            ConfigUtils.getString("config", "settings.grants.gui.confirm-title", "&8Confirm Grant")
        );
        
        // Add info item
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName(ConfigUtils.color("&6Grant Information"));
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ConfigUtils.color("&7Player: &f" + target.getName()));
        lore.add(ConfigUtils.color("&7Rank: &f" + plugin.getPermissionsManager().getRank(rank).getName()));
        lore.add(ConfigUtils.color("&7Duration: &f" + duration));
        lore.add("");
        lore.add(ConfigUtils.color("&eClick a button below to confirm or cancel"));
        infoMeta.setLore(lore);
        
        info.setItemMeta(infoMeta);
        inv.setItem(13, info);
        
        // Add confirm button
        ItemStack confirm = new ItemStack(Material.LIME_WOOL);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.setDisplayName(ConfigUtils.color("&aConfirm"));
        confirm.setItemMeta(confirmMeta);
        inv.setItem(11, confirm);
        
        // Add cancel button
        ItemStack cancel = new ItemStack(Material.RED_WOOL);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.setDisplayName(ConfigUtils.color("&cCancel"));
        cancel.setItemMeta(cancelMeta);
        inv.setItem(15, cancel);
        
        // Fill empty slots
        if (ConfigUtils.getBoolean("config", "gui.fill-empty-slots", true)) {
            ItemStack filler = new ItemStack(Material.valueOf(
                ConfigUtils.getString("config", "gui.fill-material", "BLACK_STAINED_GLASS_PANE")
            ));
            ItemMeta fillerMeta = filler.getItemMeta();
            fillerMeta.setDisplayName(" ");
            filler.setItemMeta(fillerMeta);
            
            for (int i = 0; i < size; i++) {
                if (inv.getItem(i) == null) {
                    inv.setItem(i, filler);
                }
            }
        }
        
        // Store GUI data
        GuiData data = new GuiData(GuiType.CONFIRMATION, target.getUniqueId());
        data.setSelectedRank(rank);
        data.setSelectedDuration(duration);
        openGuis.put(staff.getUniqueId(), data);
        
        // Open inventory
        staff.openInventory(inv);
    }
    
    /**
     * Gets the GUI data for a player
     * @param player The player
     * @return The GUI data or null if not found
     */
    public GuiData getGuiData(Player player) {
        return openGuis.get(player.getUniqueId());
    }
    
    /**
     * Removes GUI data for a player
     * @param player The player
     */
    public void removeGuiData(Player player) {
        openGuis.remove(player.getUniqueId());
    }
    
    /**
     * GUI types
     */
    public enum GuiType {
        RANK_SELECTION,
        DURATION_SELECTION,
        CONFIRMATION
    }
    
    /**
     * Class to hold GUI data
     */
    public static class GuiData {
        private final GuiType type;
        private final UUID targetUUID;
        private String selectedRank;
        private String selectedDuration;
        
        public GuiData(GuiType type, UUID targetUUID) {
            this.type = type;
            this.targetUUID = targetUUID;
        }
        
        public GuiType getType() { return type; }
        public UUID getTargetUUID() { return targetUUID; }
        public String getSelectedRank() { return selectedRank; }
        public String getSelectedDuration() { return selectedDuration; }
        
        public void setSelectedRank(String rank) { this.selectedRank = rank; }
        public void setSelectedDuration(String duration) { this.selectedDuration = duration; }
    }
}

package com.standcore;

import org.bukkit.plugin.java.JavaPlugin;
import com.standcore.managers.*;
import com.standcore.commands.*;
import com.standcore.listeners.*;
import com.standcore.util.ConfigUtils;
import com.standcore.util.FileUtils;

public class StandCore extends JavaPlugin {
    private static StandCore instance;
    private GrantsManager grantsManager;
    private SanctionsManager sanctionsManager;
    private StaffModeManager staffModeManager;
    private PermissionsManager permissionsManager;
    private PlaceholderManager placeholderManager;

    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize utilities
        FileUtils.init(this);
        ConfigUtils.init(this);
        
        // Create default directories
        FileUtils.createDirectory("data/grants");
        FileUtils.createDirectory("data/sanctions");
        
        // Initialize managers
        initializeManagers();
        
        // Register commands
        registerCommands();
        
        // Register listeners
        registerListeners();
        
        // Setup PlaceholderAPI integration if available
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderManager.register();
            getLogger().info("PlaceholderAPI integration enabled!");
        }
        
        getLogger().info("StandCore has been enabled!");
    }

    @Override
    public void onDisable() {
        if (placeholderManager != null) {
            placeholderManager.unregister();
        }
        getLogger().info("StandCore has been disabled!");
    }

    private void initializeManagers() {
        grantsManager = new GrantsManager(this);
        sanctionsManager = new SanctionsManager(this);
        staffModeManager = new StaffModeManager(this);
        permissionsManager = new PermissionsManager(this);
        placeholderManager = new PlaceholderManager(this);
    }

    private void registerCommands() {
        // Staff commands
        AdminChatCommand adminChatCmd = new AdminChatCommand(this);
        getCommand("adminchat").setExecutor(adminChatCmd);
        getCommand("ac").setExecutor(adminChatCmd);
        
        getCommand("staff").setExecutor(new StaffCommand(this));
        getCommand("vanish").setExecutor(new VanishCommand(this));
        getCommand("v").setExecutor(new VanishCommand(this));
        getCommand("freeze").setExecutor(new FreezeCommand(this));
        getCommand("unfreeze").setExecutor(new UnfreezeCommand(this));
        getCommand("invsee").setExecutor(new InvSeeCommand(this));
        
        // Grant commands
        getCommand("grant").setExecutor(new GrantCommand(this));
        getCommand("grantshistory").setExecutor(new GrantsHistoryCommand(this));
        getCommand("grantshist").setExecutor(new GrantsHistoryCommand(this));
        getCommand("gh").setExecutor(new GrantsHistoryCommand(this));
        
        // Punishment commands
        getCommand("ban").setExecutor(new BanCommand(this));
        getCommand("unban").setExecutor(new UnbanCommand(this));
        getCommand("mute").setExecutor(new MuteCommand(this));
        getCommand("unmute").setExecutor(new UnmuteCommand(this));
        getCommand("kick").setExecutor(new KickCommand(this));
        getCommand("warn").setExecutor(new WarnCommand(this));
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new InventoryClickListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerMoveListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
    }

    public static StandCore getInstance() {
        return instance;
    }

    public GrantsManager getGrantsManager() {
        return grantsManager;
    }

    public SanctionsManager getSanctionsManager() {
        return sanctionsManager;
    }

    public StaffModeManager getStaffModeManager() {
        return staffModeManager;
    }

    public PermissionsManager getPermissionsManager() {
        return permissionsManager;
    }

    public PlaceholderManager getPlaceholderManager() {
        return placeholderManager;
    }
}

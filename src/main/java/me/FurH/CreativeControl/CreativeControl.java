/*
 * Copyright (C) 2011-2013 FurmigaHumana.  All rights reserved.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation,  version 3.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package me.FurH.CreativeControl;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import de.diddiz.LogBlock.Consumer;
import de.diddiz.LogBlock.LogBlock;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import me.FurH.Core.CorePlugin;
import me.FurH.Core.cache.CoreHashSet;
import me.FurH.Core.exceptions.CoreException;
import me.FurH.Core.internals.InternalManager;
import me.FurH.Core.updater.CoreUpdater;
import me.FurH.CreativeControl.blacklist.CreativeBlackList;
import me.FurH.CreativeControl.commands.CreativeCommands;
import me.FurH.CreativeControl.configuration.CreativeMainConfig;
import me.FurH.CreativeControl.configuration.CreativeMessages;
import me.FurH.CreativeControl.configuration.CreativeWorldConfig;
import me.FurH.CreativeControl.configuration.CreativeWorldNodes;
import me.FurH.CreativeControl.data.CreativeDataUpdater;
import me.FurH.CreativeControl.data.CreativePlayerData;
import me.FurH.CreativeControl.data.friend.CreativePlayerFriends;
import me.FurH.CreativeControl.database.CreativeSQLDatabase;
import me.FurH.CreativeControl.database.extra.CreativeSQLUpdater;
import me.FurH.CreativeControl.integration.AuthMe;
import me.FurH.CreativeControl.integration.CreativeHideInventory;
import me.FurH.CreativeControl.integration.MobArena;
import me.FurH.CreativeControl.integration.SurvivalGames;
import me.FurH.CreativeControl.integration.worldedit.CreativeEditSessionFactory;
import me.FurH.CreativeControl.listener.*;
import me.FurH.CreativeControl.manager.CreativeBlockManager;
import me.FurH.CreativeControl.metrics.CreativeMetrics;
import me.FurH.CreativeControl.metrics.CreativeMetrics.Graph;
import me.FurH.CreativeControl.permissions.CreativePermissions;
import me.FurH.CreativeControl.region.CreativeRegion;
import me.FurH.CreativeControl.region.CreativeRegionManager;
import me.FurH.CreativeControl.selection.CreativeBlocksSelection;
import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

/**
 *
 * @author FurmigaHumana
 */
public class CreativeControl extends CorePlugin {

    public CreativeControl() {
        super("&8[&3CreativeControl&8]&7:&f");
    }

    /* classes */
    public static CreativeControl plugin;
    private static CreativeSQLDatabase database;
    private static CreativeBlocksSelection selector;
    private static CreativeRegionManager regioner;
    private static CreativeBlockManager manager;
    private static CreativePlayerData data;
    private static CreativePlayerFriends friends;
    private static CreativeMainConfig mainconfig;
    private static CreativeMessages messages;
    private static Consumer lbconsumer = null;
    private static CreativeWorldConfig worldconfig;
    private static boolean prismEnabled;
    private static CoreProtectAPI coreprotect;
    private static CreativeBlackList blacklist;
    
    private static CreativePermissions permissions;

    public WeakHashMap<Player, Location> right = new WeakHashMap<>();
    public WeakHashMap<Player, Location> left = new WeakHashMap<>();

    public Map<String, CoreHashSet<UUID>> limits = new HashMap<>();
    public Map<String, Integer> mods = new HashMap<>();

    public CoreUpdater updater;

    @Override
    public void onEnable() {
        
        long localStart = System.currentTimeMillis();
        
        plugin = this;
        
        updater = new CoreUpdater(this, "http://dev.bukkit.org/server-mods/creativecontrol/");

        messages = new CreativeMessages(this);
        messages.load();

        getCommunicator().setTag(messages.prefix_tag);
        blacklist = new CreativeBlackList();
        
        log("[TAG] Initializing configurations...");
        mainconfig = new CreativeMainConfig(this);
        mainconfig.load();

        worldconfig = new CreativeWorldConfig(this);
        worldconfig.setSingleConfig(mainconfig.config_single);
        
        if (!mainconfig.config_single) {
            for (World w : getServer().getWorlds()) { worldconfig.load(w); }
        } else {
            worldconfig.load(getServer().getWorlds().get(0));
        }

        mainconfig.updateConfig();

        getCommunicator().setDebug(mainconfig.com_debugcons);
        getCommunicator().setQuiet(mainconfig.com_quiet);

        log("[TAG] Loading Modules...");
        selector = new CreativeBlocksSelection();
        regioner = new CreativeRegionManager();
        manager = new CreativeBlockManager();
        friends = new CreativePlayerFriends();
        data = new CreativePlayerData();
        permissions = new CreativePermissions();

        database = new CreativeSQLDatabase(this, mainconfig.database_prefix, mainconfig.database_type, mainconfig.database_host, mainconfig.database_port, mainconfig.database_table, mainconfig.database_user, mainconfig.database_pass);

        database.setupQueue(mainconfig.queue_speed, mainconfig.queue_threadds);
        
        database.setAllowMainThread(true);
        
        try {
            database.setAutoCommit(false);
        } catch (CoreException ex) {
            error(ex);
        }
        
        try {
            database.connect();
        } catch (CoreException ex) {
            error(ex);
        }

        database.load();
        
        try {
            database.commit();
        } catch (CoreException ex) {
            ex.printStackTrace();
        }

        log("[TAG] Registring Events...");
        PluginManager pm = getServer().getPluginManager();

        pm.registerEvents(new CreativeBlockListener(), this);
        pm.registerEvents(new CreativeEntityListener(), this);
        pm.registerEvents(new CreativePlayerListener(), this);
        pm.registerEvents(new CreativeWorldListener(), this);

        if (mainconfig.events_move) {
            pm.registerEvents(new CreativeMoveListener(), this);
        }

        if (mainconfig.events_misc) {
            pm.registerEvents(new CreativeMiscListener(), this);
        }

        if (mainconfig.data_hide && pm.getPlugin("AuthMe") != null) {
            
            InternalManager.setup(true);
            
            pm.registerEvents(new CreativeHideInventory(), this);
            pm.registerEvents(new AuthMe(), this);
        }

        loadIntegrations();
                
        CommandExecutor cc = new CreativeCommands();
        getCommand("creativecontrol").setExecutor(cc);

        setupWorldEdit();
        setupLoggers();
        
        permissions.setup();

        Bukkit.getScheduler().runTaskAsynchronously(this, new Runnable() {
            @Override
            public void run() {
                log("[TAG] Cached {0} protections", manager.preCache());
                log("[TAG] Loaded {0} regions", regioner.loadRegions());
                log("[TAG] {0} blocks protected", manager.getTotal());
            }
        });

        if (mainconfig.updater_enabled) {
            updater.setup();
        }

        startMetrics();
        
        try {
            if (database.isUpdateAvailable()) {
                log("[TAG] Database update required!");
                
                if (database.getCurrentVersion() >= 2) {
                    CreativeDataUpdater invUpdater = new CreativeDataUpdater(this);
                    invUpdater.run();
                } else {
                    Bukkit.getScheduler().runTaskAsynchronously(this, new CreativeSQLUpdater(this));
                }
            }
        } catch (CoreException ex) {
            error(ex);
        }
        
        logEnable(Math.abs(System.currentTimeMillis() - localStart));
    }

    @Override
    public void onDisable() {
        
        long localStart = System.currentTimeMillis();

        try {
            database.disconnect(false);
        } catch (CoreException ex) {
            error(ex);
        }

        HandlerList.unregisterAll(this);
        getServer().getScheduler().cancelTasks(this);
        
        clear();
        right.clear();
        left.clear();
        mods.clear();
        data.clear();
        friends.clear();
        limits.clear();
        
        messages.unload();
        mainconfig.unload();
        worldconfig.unload();

        worldconfig.clear();
        
        plugin = null;
        database = null;
        selector = null;
        regioner = null;
        manager = null;
        data = null;
        friends = null;
        mainconfig = null;
        messages = null;
        lbconsumer = null;
        worldconfig = null;
        prismEnabled = false;
        coreprotect = null;
        blacklist = null;
        permissions = null;
        updater = null;
        
        
        logDisable(Math.abs(System.currentTimeMillis() - localStart));
    }
    
    public void reload(CommandSender sender) {

        String ssql  = mainconfig.database_type;
        boolean move = mainconfig.events_move;
        boolean misc = mainconfig.events_misc;

        clear();
        right.clear();
        left.clear();
        mods.clear();
        data.clear();
        friends.clear();
        limits.clear();
        
        messages.unload();
        mainconfig.unload();
        worldconfig.unload();
        
        messages.load();
        mainconfig.load();

        worldconfig.clear();

        if (!mainconfig.config_single) {
            for (World w : getServer().getWorlds()) { worldconfig.load(w); }
        } else {
            worldconfig.load(getServer().getWorlds().get(0));
        }
        
        loadIntegrations();
        
        String  newssql = mainconfig.database_type;
        boolean newmove = mainconfig.events_move;
        boolean newmisc = mainconfig.events_misc;

        if (!ssql.equals(newssql)) {
            
            try {
                database.disconnect(false);
            } catch (CoreException ex) {
                error(ex);
            }
            
            try {
                database.connect();
            } catch (CoreException ex) {
                error(ex);
            }

            database.load();

            msg(sender, "[TAG] Database Type: &4{0}&7 Defined.", database.getDatabaseEngine());
        }
        
        PluginManager pm = getServer().getPluginManager();
        if (move != newmove) {
            if (newmove) {
                pm.registerEvents(new CreativeMoveListener(), this);
                msg(sender, "[TAG] CreativeMoveListener registred, Listener enabled.");
            } else {
                HandlerList.unregisterAll(new CreativeMoveListener());
                msg(sender, "[TAG] CreativeMoveListener unregistered, Listener disabled.");
            }
        }

        if (misc != newmisc) {
            if (newmisc) {
                pm.registerEvents(new CreativeMiscListener(), this);
                msg(sender, "[TAG] CreativeMiscListener registred, Listener enabled.");
            } else {
                HandlerList.unregisterAll(new CreativeMoveListener());
                msg(sender, "[TAG] CreativeMiscListener unregistered, Listener disabled.");
            }
        }
    }
    
    public void loadIntegrations() {
        PluginManager pm = getServer().getPluginManager();
        Plugin p = pm.getPlugin("MobArena");
        if (p != null) {
            if (p.isEnabled()) {
                log("[TAG] MobArena support enabled!");
                pm.registerEvents(new MobArena(), this);
            }
        }

        try {
            Class.forName("org.mcsg.survivalgames.api.PlayerJoinArenaEvent");
            p = pm.getPlugin("SurvivalGames");
            if (p != null) {
                if (p.isEnabled()) {
                    log("[TAG] SurvivalGames support enabled!");
                    pm.registerEvents(new SurvivalGames(), this);
                }
            }
        } catch (Exception ex) { }

        p = pm.getPlugin("Multiverse-Inventories");
        if (p != null) {
            if (p.isEnabled()) {
                if (mainconfig.data_inventory || mainconfig.data_status) {
                    if (mainconfig.config_conflict) {
                        mainconfig.data_inventory = false;
                        mainconfig.data_status = false;
                        int anoy = 5;
                        while (anoy > 0) {
                            log("[TAG] ***************************************************");
                            log("[TAG] Multiverse-Inventories Detected!!");
                            log("[TAG] Per-GameMode inventories will be disabled by this plugin");
                            log("[TAG] Use the multiverse inventories manager!");
                            log("[TAG] ***************************************************");   
                            anoy--;
                        }
                    } else {
                        log("[TAG] ***************************************************");
                        log("[TAG] Multiverse-Inventories Detected!!");
                        log("[TAG] Per-GameMode inventories may be buggy!");
                        log("[TAG] Use the multiverse inventories manager!");
                        log("[TAG] ***************************************************");   
                    }
                }
            }
        }
    }

    @Override
    public boolean hasPerm(CommandSender sender, String node) {
        return ((sender instanceof Player)) ? permissions.hasPerm((Player)sender, "CreativeControl."+node) : true;
    }
    
    private void clear() {
        CoreHashSet<UUID> entity = new CoreHashSet<>();

        for (String key : limits.keySet()) {
            entity.addAll(limits.get(key));
        }

        for (World w : getServer().getWorlds()) {
            for (Entity x : w.getEntities()) {
                if (entity.contains(x.getUniqueId())) {
                    x.remove();
                }
            }
        }
        
        entity.clear();
        limits.clear();
    }
    
    public void clear(Player player) {
        CoreHashSet<UUID> entity = limits.get(player.getName());

        if (entity == null) {
            return;
        }

        for (World w : Bukkit.getWorlds()) {
            for (Entity x : w.getEntities()) {
                if (entity.contains(x.getUniqueId())) {
                    x.remove();
                }
            }
        }

        entity.clear();
        limits.remove(player.getName());
    }
    
    public static CreativeBlackList getBlackList() {
        return blacklist;
    }
    
    public static CreativePermissions getPermissions2() {
        return permissions;
    }
    
    public static CreativeWorldConfig getWorldConfig() {
        return worldconfig;
    }

    public static CreativeWorldNodes getWorldNodes(World world) {
        return worldconfig.get(world);
    }

    public static CreativeControl getPlugin() { 
        return plugin; 
    }

    public static CreativeBlocksSelection getSelector() { 
        return selector; 
    }
    
    public static CreativePlayerFriends getFriends() {
        return friends;
    }

    public static CreativeSQLDatabase getDb() { 
        return database; 
    }
    
    public static CreativeRegionManager getRegioner() { 
        return regioner; 
    }
    
    public static CreativeMainConfig getMainConfig() {
        return mainconfig;
    }

    public static CreativeBlockManager getManager() { 
        return manager; 
    }
    
    public static CreativePlayerData getPlayerData() { 
        return data; 
    }

    public static CreativeMessages getMessages() {
        return messages;
    }
    
    public static Consumer getLogBlock() { 
        return lbconsumer; 
    }

    public static CoreProtectAPI getCoreProtect() {
        return coreprotect;
    }
    
    public static boolean getPrism() {
        return prismEnabled;
    }
    
    public void setupLoggers() {
        
        Plugin logblock = Bukkit.getPluginManager().getPlugin("LogBlock");
        if (logblock != null) {
            log("[TAG] LogBlock hooked as logging plugin");
            lbconsumer = ((LogBlock)logblock).getConsumer();
        }

        Plugin prism = Bukkit.getPluginManager().getPlugin("Prism");
        if (prism != null) {
            log("[TAG] Prism hooked as logging plugin");
            prismEnabled = true;
        }
        
        Plugin corep = Bukkit.getPluginManager().getPlugin("CoreProtect");
        if (corep != null) {
            log("[TAG] CoreProtect hooked as logging plugin");            
            coreprotect = ((CoreProtect)corep).getAPI();
        }
    }

    public void setupWorldEdit() {
        Plugin we = Bukkit.getPluginManager().getPlugin("WorldEdit");
        if (we != null && we.isEnabled()) {
            CreativeEditSessionFactory.setup();
        }
    }
    
    public WorldEditPlugin getWorldEdit() {
        PluginManager pm = getServer().getPluginManager();
        Plugin wex = pm.getPlugin("WorldEdit");
        return (WorldEditPlugin) wex;
    }

    private int survival = 0;
    private int creative = 0;
    private int useMove = 0;
    private int useMisc = 0;
    private int OwnBlock = 0;
    private int NoDrop = 0;
    
    private void startMetrics() {
        try {
            CreativeMetrics metrics = new CreativeMetrics(this);

            Graph dbType = metrics.createGraph("Database Type");
            dbType.addPlotter(new CreativeMetrics.Plotter(database.getDatabaseEngine().toString()) {
                @Override
                public int getValue() {
                    return 1;
                }
            });

            for (CreativeRegion CR : regioner.getAreas()) {
                if (CR.gamemode == GameMode.CREATIVE) {
                    creative++;
                } else {
                    survival++;
                }
            }
            
            Graph reg = metrics.createGraph("Regions");
            reg.addPlotter(new CreativeMetrics.Plotter("Regions") {
                    @Override
                    public int getValue() {
                            return creative+survival;
                    }
            });
            
            Graph reg1 = metrics.createGraph("Regions Type");
            reg1.addPlotter(new CreativeMetrics.Plotter("Creative") {
                    @Override
                    public int getValue() {
                            return creative;
                    }
            });
            
            reg1.addPlotter(new CreativeMetrics.Plotter("Survival") {

                    @Override
                    public int getValue() {
                            return survival;
                    }

            });
            
            if (mainconfig.events_move) {
                useMove++;
            }
            
            if (mainconfig.events_misc) {
                useMisc++;
            }
            
            Graph extra = metrics.createGraph("Extra Events");
            extra.addPlotter(new CreativeMetrics.Plotter("Move Event") {
                    @Override
                    public int getValue() {
                            return useMove;
                    }
            });
            
            extra.addPlotter(new CreativeMetrics.Plotter("Misc Protection") {
                    @Override
                    public int getValue() {
                            return useMisc;
                    }
            });
            
            for (World world : getServer().getWorlds()) {
                if (worldconfig.get(world).block_ownblock) {
                    OwnBlock++;
                } else
                if (worldconfig.get(world).block_nodrop) {
                    NoDrop++;
                }
            }
            
            Graph ptype = metrics.createGraph("Protection Type");
             ptype.addPlotter(new CreativeMetrics.Plotter("OwnBlocks") {
                    @Override
                    public int getValue() {
                            return OwnBlock;
                    }
            });
            
             ptype.addPlotter(new CreativeMetrics.Plotter("NoDrop") {
                    @Override
                    public int getValue() {
                            return NoDrop;
                    }
            });
            
            metrics.start();
        } catch (IOException e) {
        }
    }
}
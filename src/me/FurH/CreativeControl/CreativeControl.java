/*
 * Copyright (C) 2011-2012 FurmigaHumana.  All rights reserved.
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
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import me.FurH.CreativeControl.cache.CreativeBlockCache;
import me.FurH.CreativeControl.commands.CreativeCommands;
import me.FurH.CreativeControl.configuration.CreativeMainConfig;
import me.FurH.CreativeControl.configuration.CreativeMessages;
import me.FurH.CreativeControl.configuration.CreativeWorldConfig;
import me.FurH.CreativeControl.data.conversor.CreativePlayerConversor;
import me.FurH.CreativeControl.data.CreativePlayerData;
import me.FurH.CreativeControl.data.friend.CreativePlayerFriends;
import me.FurH.CreativeControl.database.CreativeBlockMatcher;
import me.FurH.CreativeControl.database.CreativeBlockManager;
import me.FurH.CreativeControl.database.CreativeSQLDatabase;
import me.FurH.CreativeControl.database.extra.CreativeSQLUpdater;
import me.FurH.CreativeControl.integration.AuthMe;
import me.FurH.CreativeControl.integration.worldedit.CreativeWorldEditHook;
import me.FurH.CreativeControl.integration.xAuth;
import me.FurH.CreativeControl.listener.*;
import me.FurH.CreativeControl.metrics.CreativeMetrics;
import me.FurH.CreativeControl.metrics.CreativeMetrics.Graph;
import me.FurH.CreativeControl.region.CreativeRegion;
import me.FurH.CreativeControl.region.CreativeRegion.gmType;
import me.FurH.CreativeControl.region.CreativeRegionCreator;
import me.FurH.CreativeControl.selection.CreativeBlocksSelection;
import me.FurH.CreativeControl.util.CreativeCommunicator;
import me.FurH.CreativeControl.util.CreativeUtil;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author FurmigaHumana
 */
public class CreativeControl extends JavaPlugin {
    public static final Logger logger = Logger.getLogger("minecraft");
    public static String tag = "[CreativeControl]: ";
    public static Permission permission = null;

    /*
     * Classes
     */
    private static CreativeControl plugin;
    private static CreativeBlockCache cache;
    private static CreativeCommunicator communicator;
    private static CreativeSQLDatabase database;
    private static CreativeBlocksSelection selector;
    private static CreativeRegionCreator regioner;
    private static CreativeRegion regions;
    private static CreativeBlockManager manager;
    private static CreativeBlockMatcher matcher;
    private static CreativePlayerData data;
    private static CreativeWorldEditHook worldedit;
    private static CreativePlayerFriends friends;
    private static CreativeMainConfig mainconfig;
    private static CreativeMessages messages;
    private static Consumer lbconsumer = null;

    public WeakHashMap<Player, Location> right = new WeakHashMap<Player, Location>();
    public WeakHashMap<Player, Location> left = new WeakHashMap<Player, Location>();
    
    public WeakHashMap<String, String> mods = new WeakHashMap<String, String>();
    
    public List<UUID> entity = new ArrayList<UUID>();
    public Map<String, Integer> limits = new HashMap<String, Integer>();
    public Player player = null;
    
    private Runnable update;
    
    public String currentversion;
    public String newversion;
    
    public boolean hasUpdate;

    @Override
    public void onEnable() {
        plugin = this;
        communicator = new CreativeCommunicator();
        messages = new CreativeMessages();
        messages.load();

        communicator.log("[TAG] Initializing configurations...");
        mainconfig = new CreativeMainConfig();
        mainconfig.load();

        if (!mainconfig.config_single) {
            for (World w : getServer().getWorlds()) { CreativeWorldConfig.load(w); }
        } else {
            CreativeWorldConfig.load(getServer().getWorlds().get(0));
        }

        communicator.log("[TAG] Loading Modules...");
        cache = new CreativeBlockCache();
        selector = new CreativeBlocksSelection();
        regioner = new CreativeRegionCreator();
        regions = new CreativeRegion(this);
        manager = new CreativeBlockManager();
        friends = new CreativePlayerFriends();
        matcher = new CreativeBlockMatcher();
        data = new CreativePlayerData();
        worldedit = new CreativeWorldEditHook();
        database = new CreativeSQLDatabase(this, true);
        
        getRegioner().loadRegions();

        communicator.log("[TAG] Registring Events...");
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

        CommandExecutor cc = new CreativeCommands();
        getCommand("creativecontrol").setExecutor(cc);
        
        setupPermission();
        setupLogBlock();

        communicator.log("[TAG] Cached {0} protections", manager.preCache());
        communicator.log("[TAG] Loaded {0} regions", regions.get().size());
        
        PluginDescriptionFile version = getDescription();
        currentversion = "v"+version.getVersion();
        logger.info("[CreativeControl] CreativeControl " + currentversion + " Enabled");

        if (mainconfig.updater_enabled) {
            updateThread();
        }

        startMetrics();

        CreativePlayerConversor.loadup();
        CreativeSQLUpdater updater = new CreativeSQLUpdater(null);
        if (!updater.lock) {
            updater.loadup();
        }
    }
    
    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);

        clear();
        right.clear();
        left.clear();
        mods.clear();
        cache.clear();
        data.clear();
        friends.clear();
        database.clear();
        entity.clear();
        limits.clear();
        
        database.close();
        logger.info("[CreativeControl] CreativeControl " + currentversion + " Disabled");
        getServer().getScheduler().cancelTasks(this);
    }
    
    public void reload(CommandSender sender) {
        boolean ssql = mainconfig.database_mysql;
        boolean move = mainconfig.events_move;
        boolean misc = mainconfig.events_misc;
        
        clear();
        right.clear();
        left.clear();
        mods.clear();
        cache.clear();
        data.clear();
        friends.clear();
        database.clear();
        entity.clear();
        limits.clear();
        
        messages.load();
        mainconfig.load();

        CreativeWorldConfig.clear();
        
        if (!mainconfig.config_single) {
            for (World w : getServer().getWorlds()) { CreativeWorldConfig.load(w); }
        } else {
            CreativeWorldConfig.load(getServer().getWorlds().get(0));
        }
        
        boolean newssql = mainconfig.database_mysql;
        boolean newmove = mainconfig.events_move;
        boolean newmisc = mainconfig.events_misc;

        if (ssql != newssql) {
            new CreativeSQLDatabase(this, false);
            database = new CreativeSQLDatabase(this, true);
            communicator.msg(sender, "[TAG] Database Type: &4{0}&7 Defined.", newssql ? "MySQL" : "SQLite");
        }
        
        PluginManager pm = getServer().getPluginManager();
        if (move != newmove) {
            if (newmove) {
                pm.registerEvents(new CreativeMoveListener(), this);
                communicator.msg(sender, "[TAG] CreativeMoveListener registred, Listener enabled.");
            } else {
                HandlerList.unregisterAll(new CreativeMoveListener());
                communicator.msg(sender, "[TAG] CreativeMoveListener unregistered, Listener disabled.");
            }
        }

        if (misc != newmisc) {
            if (newmisc) {
                pm.registerEvents(new CreativeMiscListener(), this);
                communicator.msg(sender, "[TAG] CreativeMiscListener registred, Listener enabled.");
            } else {
                HandlerList.unregisterAll(new CreativeMoveListener());
                communicator.msg(sender, "[TAG] CreativeMiscListener unregistered, Listener disabled.");
            }
        }
    }
    
    private void clear() {
        for (World w : getServer().getWorlds()) {
            for (Entity x : w.getEntities()) {
                if (entity.contains(x.getUniqueId())) {
                    x.remove();
                }
            }
        }
    }
    
    public static CreativeControl getPlugin() { 
        return plugin; 
    }
    
    public static CreativeBlockCache getCache() { 
        return cache; 
    }
    
    public static CreativeCommunicator getCommunicator() { 
        return communicator; 
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
    
    public static CreativeRegionCreator getRegioner() { 
        return regioner; 
    }
    
    public static CreativeMainConfig getMainConfig() {
        return mainconfig;
    }
    
    public static CreativeRegion getRegions() { 
        return regions; 
    }
    
    public static CreativeBlockManager getManager() { 
        return manager; 
    }
    
    public static CreativeBlockMatcher getMatcher() { 
        return matcher; 
    }
    
    public static CreativePlayerData getPlayerData() { 
        return data; 
    }
    
    public static CreativeWorldEditHook getWorldEditHook() { 
        return worldedit; 
    }
    
    public static CreativeMessages getMessages() {
        return messages;
    }
    
    public static Consumer getConsumer() { 
        return lbconsumer; 
    }
    
    public void setupLogBlock() {
        PluginManager pm = getServer().getPluginManager();
        Plugin x = pm.getPlugin("LogBlock");
        if (x != null) {
            communicator.log("[TAG] LogBlock hooked as logging plugin");
            lbconsumer = ((LogBlock)x).getConsumer();
        }
    }
    
    public WorldEditPlugin getWorldEdit() {
        PluginManager pm = getServer().getPluginManager();
        Plugin wex = pm.getPlugin("WorldEdit");
        return (WorldEditPlugin) wex;
    }

    private boolean setupPermission() {
        PluginManager pm = getServer().getPluginManager();
        if (pm.getPlugin("Vault") != null) {
            RegisteredServiceProvider permissionProvider = getServer().getServicesManager().getRegistration(Permission.class);
            if (permissionProvider != null) {
                permission = (Permission)permissionProvider.getProvider();
            }
            communicator.log("[TAG] Vault Hooked as Permission plugin");
            return permission != null;
        }
        permission = null;
        communicator.log("[TAG] Vault Plugin Not Found");
        communicator.log("[TAG] Defaulting to SuperPerms");
        return false;
    }

    public boolean hasPerm(CommandSender sender, String perm) {
        if ((perm == null) || (perm.equals(""))) {
            return true;
        } else {
            if (!(sender instanceof Player)) {
                return true;
            } else {
                Player player = (Player) sender;
                if (player.isOp()) {
                    if (mainconfig.perm_ophas) {
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    if (permission != null) {
                        if (permission.has(player, "CreativeControl."+perm)) {
                            return true;
                        } else {
                            return false;
                        }
                    } else {
                        if (player.hasPermission("CreativeControl."+perm)) {
                            return true;
                        } else {
                            return false;
                        }
                    }
                }
            }
        }
    }

    public boolean isLoggedIn(Player player) {
        PluginManager pm = getServer().getPluginManager();
        
        if (pm.getPlugin("AuthMe") != null && !AuthMe.isLoggedInComplete(player)) {
            return false;
        }

        if (pm.getPlugin("xAuth") != null && !xAuth.isLoggedIn(player)) {
            return false;
        }
        
        return true;
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
            dbType.addPlotter(new CreativeMetrics.Plotter(database.type.toString()) {
                @Override
                public int getValue() {
                    return 1;
                }
            });

            for (CreativeRegion CR : regions.get()) {
                if (CR.getType() == gmType.CREATIVE) {
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
                if (CreativeWorldConfig.get(world).block_ownblock) {
                    OwnBlock++;
                } else
                if (CreativeWorldConfig.get(world).block_nodrop) {
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
    
    public void updateThread() {
        if (update == null) {
            update = new Runnable() {
                @Override
                public void run() {
                    newversion = getVersion(currentversion);
                    int nv = CreativeUtil.toInteger(newversion);
                    int od = CreativeUtil.toInteger(currentversion);
                    
                    if (od < nv) {
                        communicator.log("New Version Found: {0} (You have: {1})", newversion, currentversion);
                        communicator.log("Visit: http://bit.ly/creativecontrol/");
                        hasUpdate = true;
                    }
                }
            };
        }
        getServer().getScheduler().scheduleAsyncRepeatingTask(this, update, 100, 21600 * 20);
    }
    
    public String getVersion(String current) {
        try {	
            URL url = new URL("http://dev.bukkit.org/server-mods/creativecontrol/files.rss");
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(url.openConnection().getInputStream());
            doc.getDocumentElement().normalize();
            NodeList nodes = doc.getElementsByTagName("item");
            Node firstNode = nodes.item(0);
            if (firstNode.getNodeType() == 1) {
                Element firstElement = (Element)firstNode;
                NodeList firstElementTagName = firstElement.getElementsByTagName("title");
                Element firstNameElement = (Element) firstElementTagName.item(0);
                NodeList firstNodes = firstNameElement.getChildNodes();
                return firstNodes.item(0).getNodeValue();
            }
        } catch (Exception e) {
            return current;
        }
        return current;
    }
}
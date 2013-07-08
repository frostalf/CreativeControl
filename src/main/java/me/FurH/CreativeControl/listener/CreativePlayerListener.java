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

package me.FurH.CreativeControl.listener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import me.FurH.Core.cache.CoreLRUCache;
import me.FurH.Core.location.LocationUtils;
import me.FurH.Core.player.PlayerUtils;
import me.FurH.Core.util.Communicator;
import me.FurH.CreativeControl.CreativeControl;
import me.FurH.CreativeControl.blacklist.CreativeBlackList;
import me.FurH.CreativeControl.configuration.CreativeMainConfig;
import me.FurH.CreativeControl.configuration.CreativeMessages;
import me.FurH.CreativeControl.configuration.CreativeWorldNodes;
import me.FurH.CreativeControl.data.CreativePlayerData;
import me.FurH.CreativeControl.data.friend.CreativePlayerFriends;
import me.FurH.CreativeControl.database.CreativeSQLDatabase;
import me.FurH.CreativeControl.manager.CreativeBlockData;
import me.FurH.CreativeControl.manager.CreativeBlockManager;
import me.FurH.CreativeControl.region.CreativeRegion;
import me.FurH.CreativeControl.region.CreativeRegionManager;
import me.FurH.CreativeControl.stack.CreativeItemStack;
import me.FurH.CreativeControl.util.CreativeUtil;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.*;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

/**
 *
 * @author FurmigaHumana
 */
public class CreativePlayerListener implements Listener {

    public static HashSet<String> changed = new HashSet<>();
    private HashSet<String> dontdrop = new HashSet<>();

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerGameModeChange(PlayerGameModeChangeEvent e) {
        if (e.isCancelled()) { return; }

        final Player player = e.getPlayer();
        final GameMode newgm = e.getNewGameMode();
        final GameMode oldgm = player.getGameMode();

        CreativeMainConfig          config      = CreativeControl.getMainConfig();
        final CreativeControl       plugin      = CreativeControl.getPlugin();
        final CreativePlayerData    data        = CreativeControl.getPlayerData();
        CreativeRegionManager       manager     = CreativeControl.getRegioner();
        CreativeRegion              region      = manager.getRegion(player.getLocation());
        Communicator                com         = plugin.getCommunicator();
        CreativeMessages            messages    = CreativeControl.getMessages();
        CreativeWorldNodes          wconfig     = CreativeControl.getWorldConfig().get(player.getWorld());
        CreativeSQLDatabase         db          = CreativeControl.getDb();
        
        if (config.data_glitch) {
            if (!newgm.equals(GameMode.CREATIVE) && !player.isOnGround()) {
                changed.add(player.getName());
            } else {
                changed.remove(player.getName());
            }
        }

        if (config.data_inventory) {
            if (!plugin.hasPerm(player, "Data.Status")) {

                InventoryView view = player.getOpenInventory();
                view.close();

                data.process(player, newgm, oldgm);
            }
        }

        if (config.perm_enabled && !plugin.hasPerm(player, "Permission.Change")) {
            Permission permissions = CreativeControl.getPermissions2().getVault();

            if (permissions != null) {
                
                if (newgm.equals(GameMode.CREATIVE)) {

                    if (config.perm_keep) {
                        
                        permissions.playerAddGroup(player, config.perm_creative);
                        
                    } else {
                        
                        String[] groups = permissions.getPlayerGroups(player);

                        db.saveOldGroups(player, permissions.getPlayerGroups(player));

                        for (String group : groups) {
                            permissions.playerRemoveGroup(player, group);
                        }
                        
                        permissions.playerAddGroup(player, config.perm_creative);
                    }
                    
                } else {

                    if (config.perm_keep) {

                        permissions.playerRemoveGroup(player, config.perm_creative);

                    } else {

                        String[] current = permissions.getPlayerGroups(player);
                        String[] groups = db.getOldGroup(player);

                        Arrays.sort(groups, Collections.reverseOrder());

                        if (groups != null) {

                            for (String group : current) {
                                permissions.playerRemoveGroup(player, group);
                            }

                            for (String old : groups) {
                                permissions.playerAddGroup(player, old);
                            }

                        }
                    }
                }
            } else {
                com.log("The permissions function only works if Vault is installed!");
            }
        }

        if (region != null) {
            if (!newgm.equals(region.gamemode) && !newgm.equals(wconfig.world_gamemode)) {
                if (!plugin.hasPerm(player, "Region.Change")) {
                    com.msg(player, messages.region_cant_change);
                    e.setCancelled(true);
                }
            }
        }
    }

    /*
     * Player Command Module
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerCommand(PlayerCommandPreprocessEvent e) {
        if (e.isCancelled()) { return; }

        Player p = e.getPlayer();
        World world = p.getWorld();

        /*
         * Command Black List
         */
        CreativeControl         plugin      = CreativeControl.getPlugin();
        CreativeWorldNodes      config      = CreativeControl.getWorldNodes(world);
        Communicator            com         = plugin.getCommunicator();
        CreativeMessages        messages    = CreativeControl.getMessages();

        if (config.world_exclude) {
            return;
        }
        
        String cmd = e.getMessage().toLowerCase();
        if (cmd.contains(" ")) {
            cmd = cmd.split(" ")[0];
        }
        
        if (p.getGameMode().equals(GameMode.CREATIVE)) {

            if (config.black_cmds.isEmpty()) {
                return;
            }
            
            if (config.black_cmds.contains(cmd)) {
                if (!plugin.hasPerm(p, "BlackList.Commands") && !plugin.hasPerm(p, "BlackList.Commands."+cmd)) {
                    com.msg(p, messages.blacklist_commands, p.getGameMode().toString().toLowerCase());
                    e.setCancelled(true);
                }
            }
            
        } else {
            
            if (config.black_s_cmds.isEmpty()) {
                return;
            }

            if (config.black_s_cmds.contains(cmd)) {
                if (!plugin.hasPerm(p, "BlackList.SurvivalCommands") && !plugin.hasPerm(p, "BlackList.SurvivalCommands."+cmd)) {
                    com.msg(p, messages.blacklist_commands, p.getGameMode().toString().toLowerCase());
                    e.setCancelled(true);
                }
            }
        }
    }
    
    /*
     * Player Death Module
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerDeath(PlayerDeathEvent e) {

        Player p = e.getEntity();
        World world = p.getWorld();
        
        /*
         * Clear drops on creative death
         */
        CreativeWorldNodes      config      = CreativeControl.getWorldNodes(world);
        CreativeControl         plugin      = CreativeControl.getPlugin();
        
        if (config.world_exclude) {
            return;
        }

        if ((p.getGameMode().equals(GameMode.CREATIVE)) && (config.prevent_drops)) {
            if (!plugin.hasPerm(p, "Preventions.ClearDrops")) {
                e.getDrops().clear();
            }
        }
    }
    
    /*
     * Player Enchant Item Module
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEnchantItemEvent(EnchantItemEvent e) {
        if (e.isCancelled()) { return; }
                
        Player p = e.getEnchanter();
        World world = p.getWorld();

        /*
         * Prevent Creative Player Enchant Items
         */
        CreativeWorldNodes      config      = CreativeControl.getWorldNodes(world);
        CreativeControl         plugin      = CreativeControl.getPlugin();
        
        if (config.world_exclude) {
            return;
        }

        if (p.getGameMode().equals(GameMode.CREATIVE)) {
            if ((!plugin.hasPerm(p, "Preventions.Enchantments")) && (config.prevent_enchant)) {
                e.setCancelled(true);
            }
        }
    }

    public void onInventoryBlackList(Player p, ItemStack item, InventoryCreativeEvent e) {

        if (p == null || item == null || e == null) {
            return;
        }
        
        CreativeItemStack stack = new CreativeItemStack(item.getTypeId(), item.getData().getData());

        CreativeWorldNodes      config      = CreativeControl.getWorldNodes(p.getWorld());
        CreativeControl         plugin      = CreativeControl.getPlugin();
        CreativeBlackList       blacklist   = CreativeControl.getBlackList();

        if (config.world_exclude) {
            return;
        }

        if (!p.getGameMode().equals(GameMode.CREATIVE)) {
            return;
        }

        if (!plugin.hasPerm(p, "BlackList.Inventory")) {
            if (blacklist.isBlackListed(config.black_inventory, stack)) {
                e.setCancelled(true); return;
            } else
            if (blacklist.isBlackListed(config.black_place, stack)) {
                e.setCancelled(true); return;
            } else
            if (blacklist.isBlackListed(config.black_use, stack)) {
                e.setCancelled(true); return;
            }
        }

        if (!plugin.hasPerm(p, "Preventions.StackLimit")) {
            if (config.prevent_stacklimit > 0 && config.prevent_stacklimit < item.getAmount()) {
                item.setAmount(config.prevent_stacklimit);
            }
        }

    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryOpen(InventoryOpenEvent e) {
        if (e.isCancelled()) { return; }
        
        HumanEntity entity = e.getPlayer();
        if (!(entity instanceof Player)) {
            return;
        }

        Player p = (Player)entity;
        World world = p.getWorld();

        CreativeMessages        messages    = CreativeControl.getMessages();
        CreativeWorldNodes      config      = CreativeControl.getWorldNodes(world);
        CreativeControl         plugin      = CreativeControl.getPlugin();
        Communicator            com         = plugin.getCommunicator();

        if (config.world_exclude) {
            return;
        }

        if (p.getGameMode().equals(GameMode.CREATIVE)) {
            if (config.prevent_invinteract) {
                if (!plugin.hasPerm(p, "Preventions.InventoryOpen")) {
                    com.msg(p, messages.mainode_restricted);
                    p.closeInventory();
                    e.setCancelled(true);
                }
            }
        }
    }

    /*
     * Inventory Click Module
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryClick(InventoryCreativeEvent e) {
        if (e.isCancelled()) { return; }
        
        if (!(e.getWhoClicked() instanceof Player)) {
            return;
        }

        Player p = (Player)e.getWhoClicked();
        World world = p.getWorld();

        CreativeWorldNodes      config      = CreativeControl.getWorldNodes(world);
        CreativeControl         plugin      = CreativeControl.getPlugin();

        if (config.world_exclude) {
            return;
        }

        if (p.getGameMode().equals(GameMode.CREATIVE)) {
            
            if (config.prevent_invinteract) {
                if (e.getInventory().getType() == InventoryType.PLAYER) {
                    if (e.getSlotType() != SlotType.QUICKBAR) {
                        if (e.getSlotType() == SlotType.ARMOR) {
                            if (!plugin.hasPerm(p, "Preventions.InventoryArmor")) {
                                e.setCancelled(true); return;
                            }
                        } else if (!plugin.hasPerm(p, "Preventions.InventoryInteract")) {
                            e.setCancelled(true); return;
                        }
                    }
                }
            }
            
            onInventoryBlackList(p, e.getCurrentItem(), e);
            onInventoryBlackList(p, e.getCursor(), e);
            
        }
    }
 
    /*
     * Player Kick Module
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerKick(PlayerKickEvent e) {
        cleanup(e.getPlayer());
    }
    
    /*
     * Player Quit Module
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerQuit(PlayerQuitEvent e) {
        cleanup(e.getPlayer());
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerTeleport(PlayerTeleportEvent e) {
        if (e.isCancelled()) { return; }

        CreativeWorldNodes config = CreativeControl.getWorldNodes(e.getTo().getWorld());

        if (config.world_exclude) {
            return;
        }

        processRegion(e.getPlayer(), e.getTo());
    }
    
    public static void processRegion(Player p, Location to) {

        CreativeMessages        messages    = CreativeControl.getMessages();
        CreativeControl         plugin      = CreativeControl.getPlugin();
        Communicator            com         = plugin.getCommunicator();
        CreativeRegion          region      = CreativeControl.getRegioner().getRegion(to);
        
        if (region == null) {
            return;
        }

        if (region.gamemode != null) {
            if (!plugin.hasPerm(p, "Region.Keep")) {
                if (!p.getGameMode().equals(region.gamemode)) {
                    com.msg(p, messages.region_welcome, region.gamemode.toString().toLowerCase());
                    p.setGameMode(region.gamemode);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlayerLogin(PlayerLoginEvent e) {
        if (e.getResult() == Result.ALLOWED) {

            final String player = e.getPlayer().getName();
            dontdrop.add(player);

            Bukkit.getScheduler().runTaskLater(CreativeControl.getPlugin(), new Runnable() {
                @Override
                public void run() {
                    dontdrop.remove(player);
                }
            }, 100L);
        }
    }

    /*
     * Player Join Module
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerJoin(PlayerJoinEvent e) {
        
        final Player p = e.getPlayer();

        final CreativeControl       plugin   = CreativeControl.getPlugin();

        if (CreativeControl.getMainConfig().data_teleport) {
            PlayerUtils.toSafeLocation(p);
        }
        
        if (CreativeControl.getMainConfig().data_survival) {
            p.setGameMode(GameMode.SURVIVAL);
        }
        
        if (plugin.updater.isUpdateAvailable()) {
            if (plugin.hasPerm(p, "Updater.Broadcast")) {
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        plugin.updater.announce(p);
                    }
                }, 40L);
            }
        }
        
    }

    /*
     * Player Changed World Module
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent e) {
        onPlayerWorldChange(e.getPlayer(), true);
    }

    public static boolean onPlayerWorldChange(Player p, boolean blocks) {

        CreativeWorldNodes      config      = CreativeControl.getWorldNodes(p.getWorld());
        CreativeControl         plugin      = CreativeControl.getPlugin();
        Communicator            com         = plugin.getCommunicator();
        CreativeMessages        messages    = CreativeControl.getMessages();
        
        if (config.world_changegm) {
            if (!p.getGameMode().equals(config.world_gamemode)) {

                if (plugin.hasPerm(p, "World.Keep")) {
                    return false;
                }

                if (plugin.hasPerm(p, "World.Keep."+p.getWorld().getName())) {
                    return false;
                }

                if (CreativeControl.getMainConfig().data_teleport) {
                    PlayerUtils.toSafeLocation(p);
                }

                com.msg(p, messages.region_unallowed, p.getGameMode().toString().toLowerCase());
                p.setGameMode(config.world_gamemode);
                
                return true;
            }
        }
        
        return false;
    }

    /*
     * Player Pickup Item Module
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerPickupItem(PlayerPickupItemEvent e) {
        if (e.isCancelled()) { return; }
                
        Player p = e.getPlayer();
        World world = p.getWorld();
        
        /*
        * Item Pickup prevent
        */
        CreativeWorldNodes      config      = CreativeControl.getWorldNodes(world);
        CreativeControl         plugin      = CreativeControl.getPlugin();
        
        if (config.world_exclude) {
            return;
        }

        if (p.getGameMode().equals(GameMode.CREATIVE)) {
            if (config.prevent_pickup) {
                if (!plugin.hasPerm(p, "Preventions.Pickup")) {
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlayerDropItemOhNoes(PlayerDropItemEvent e) {
        if (dontdrop.contains(e.getPlayer().getName())) {
            e.setCancelled(true);
        }
    }

    /*
     * Player Drops Item Module
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerDropItem(PlayerDropItemEvent e) {
        if (e.isCancelled()) { return; }
                
        Player p = e.getPlayer();
        World world = p.getWorld();

        /*
        * Item drop prevent
        */
        CreativeWorldNodes      config      = CreativeControl.getWorldNodes(world);
        CreativeControl         plugin      = CreativeControl.getPlugin();
        Communicator            com         = plugin.getCommunicator();
        CreativeMessages        messages    = CreativeControl.getMessages();
                    
        if (config.world_exclude) {
            return;
        }
        
        if (dontdrop.contains(p.getName())) {
            e.setCancelled(true); return;
        }
        
        if (p.getGameMode().equals(GameMode.CREATIVE)) {
            if (config.prevent_drop) {
                if (!plugin.hasPerm(p, "Preventions.ItemDrop")) {
                    com.msg(p, messages.mainode_restricted);
                    e.getItemDrop().remove();
                }
            }
        }
    }
    
    /*
     * Chicken Egg Throw Module
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerEggThrowEvent(PlayerEggThrowEvent e) {

        Player p = e.getPlayer();
        World world = p.getWorld();

        CreativeWorldNodes      config      = CreativeControl.getWorldNodes(world);
        CreativeControl         plugin      = CreativeControl.getPlugin();
        Communicator            com         = plugin.getCommunicator();
        CreativeMessages        messages    = CreativeControl.getMessages();

        if (config.world_exclude) {
            return;
        }

        if (p.getGameMode().equals(GameMode.CREATIVE) && config.prevent_eggs) {
            if (!plugin.hasPerm(p, "Preventions.Eggs")) {
                com.msg(p, messages.mainode_restricted);
                e.setHatching(false); e.setNumHatches((byte)0);
            }
        }
    }

    /*
     * Player interact section
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPlayerInteract(PlayerInteractEvent e) {
        
        Player p = e.getPlayer();
        Block i = e.getClickedBlock();
        World world = p.getWorld();
        
        CreativeMessages      messages = CreativeControl.getMessages();
        CreativeControl       plugin   = CreativeControl.getPlugin();
        Communicator          com      = plugin.getCommunicator();
        CreativeWorldNodes    config   = CreativeControl.getWorldNodes(world);
        CreativeMainConfig    main     = CreativeControl.getMainConfig();
        CreativeBlackList     blacklist= CreativeControl.getBlackList();
                
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (processEconomySign(p, i)) {
                e.setCancelled(true);
                return;
            }
        }

        if (main.selection_tool == p.getItemInHand().getTypeId()) {
            if (plugin.hasPerm(p, "Utily.Selection")) {
                if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    Location right = e.getClickedBlock().getLocation();
                    plugin.right.put(p, right);
                    com.msg(p, messages.selection_second, right.getBlockX(), right.getBlockY(), right.getBlockZ());
                    e.setCancelled(true);
                    return;
                } else
                if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
                    Location left = e.getClickedBlock().getLocation();
                    plugin.left.put(p, left);
                    com.msg(p, messages.selection_first, left.getBlockX(), left.getBlockY(), left.getBlockZ());
                    e.setCancelled(true);
                    return;
                }
            }
        }
        
        if (plugin.mods.containsKey(p.getName())) {
            int id = plugin.mods.get(p.getName());
            if (id == 0) {
                if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    if (plugin.hasPerm(p, "Utily.Tool.info")) {
                        info(p, i);
                        e.setCancelled(true);
                        return;
                    }
                } else
                if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
                    if (plugin.hasPerm(p, "Utily.Tool.add")) {
                        add(p, i);
                        e.setCancelled(true);
                        return;
                    }
                }
            } else 
            if (id == 1) {
                if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    if (plugin.hasPerm(p, "Utily.Tool.info")) {
                        info(p, i);
                        e.setCancelled(true);
                        return;
                    }
                } else
                if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
                    if (plugin.hasPerm(p, "Utily.Tool.del")) {
                        del(p, i);
                        e.setCancelled(true);
                        return;
                    }
                }
            }
        }

        if (config.world_exclude) {
            return;
        }

        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (p.getGameMode().equals(GameMode.CREATIVE)) {
                
                CreativeItemStack itemStack = new CreativeItemStack(i.getTypeId(), i.getData());
                
                if (blacklist.isBlackListed(config.black_interact, itemStack)) {
                    if (!plugin.hasPerm(p, "BlackList.ItemInteract."+i.getTypeId())) {
                        com.msg(p, messages.mainode_restricted);
                        e.setCancelled(true);
                        return;
                    }
                }
            }
        }
        
        if ((e.getAction() == Action.LEFT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_BLOCK) && (e.getMaterial() == Material.MINECART || e.getMaterial() == Material.BOAT)) {
            if (p.getGameMode().equals(GameMode.CREATIVE)) {
                if (!CreativeEntityListener.waiting.contains(p)) {
                    CreativeEntityListener.waiting.add(p);
                }
            }
        }
        
        if ((e.getAction() == Action.RIGHT_CLICK_AIR) || (e.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            if (p.getGameMode().equals(GameMode.CREATIVE)) {

                if (e.getItem() != null) {
                    CreativeItemStack itemStack = new CreativeItemStack(e.getItem().getTypeId(), e.getItem().getData().getData());

                    if (blacklist.isBlackListed(config.black_use, itemStack)) {
                        if (!plugin.hasPerm(p, "BlackList.ItemUse."+e.getItem().getTypeId())) {
                            com.msg(p, messages.mainode_restricted);
                            e.setCancelled(true);
                            return;
                        }
                    }
                }
                
                if (p.getItemInHand() != null) {

                    CreativeItemStack itemStack = new CreativeItemStack(p.getItemInHand().getTypeId(), p.getItemInHand().getData().getData());

                    if (blacklist.isBlackListed(config.black_use, itemStack)) {
                        if (!plugin.hasPerm(p, "BlackList.ItemUse."+p.getItemInHand().getTypeId())) {
                            com.msg(p, messages.mainode_restricted);
                            e.setCancelled(true);
                            return;
                        }
                    }
                    
                    if (config.prevent_eggs) {
                        if ((p.getItemInHand().getType() == Material.MONSTER_EGG) || (p.getItemInHand().getType() == Material.MONSTER_EGGS)) {
                            if (!plugin.hasPerm(p, "Preventions.Eggs")) {
                                com.msg(p, messages.mainode_restricted);
                                e.setCancelled(true);
                                return;
                            }
                        }
                    }
                    
                    if (config.prevent_potion) {
                        if (p.getItemInHand().getTypeId() == 373) {
                            if (!plugin.hasPerm(p, "Preventions.PotionSplash")) {
                                com.msg(p, messages.mainode_restricted);
                                e.setCancelled(true);
                            }
                        }
                    }
                }
            }
        }
    }
    
    public static boolean processEconomySign(Player p, Block block) {

        CreativeControl     plugin      = CreativeControl.getPlugin();
        Communicator        com         = plugin.getCommunicator();
        CreativeMessages    messages    = CreativeControl.getMessages();

        if (p.getGameMode().equals(GameMode.CREATIVE)) {
            if (block != null) {
                if (block.getType() == Material.WALL_SIGN || block.getType() == Material.SIGN_POST) {
                    Sign sign = (Sign)block.getState();
                    if (CreativeUtil.isBlackListedSign(sign)) {
                        if (!plugin.hasPerm(p, "BlackList.EconomySigns")) {
                            com.msg(p, messages.mainode_restricted);
                            return true;
                        }
                    }
                }
            }
        }
        
        return false;
    }

    /*
     * Block info Module
     */
    /*
     * Print informations about the block
     */
    public void info(Player p, Block b) {
        if (!is(p, b)) { return; }

        CreativeWorldNodes nodes = CreativeControl.getWorldNodes(b.getWorld());
        
        CreativeBlockManager manager = CreativeControl.getManager();
        
        CoreLRUCache<String, CreativeBlockData> cache = manager.getCache();

        CreativeBlockData data1 = manager.getFullData(b.getLocation());        
        CreativeBlockData data2 = null;
        
        if (nodes.block_ownblock) {
            data2 = cache.get(LocationUtils.locationToString(b.getLocation()));
        }

        boolean insql = data1 != null;
        boolean incache = data2 != null;

        if (nodes.block_nodrop) {
            incache = cache.containsKey(LocationUtils.locationToString(b.getLocation()));
        }

        CreativeMessages messages = CreativeControl.getMessages();
        CreativeControl plugin = CreativeControl.getPlugin();
        Communicator com = plugin.getCommunicator();
        
        if (!insql && !incache) {
            com.msg(p, messages.blockmanager_unprotected);
            plugin.mods.remove(p.getName());
            return;
        }

        String owner = null;
        String allowed = null;
        int type = 0;
        String date = null;
        
        if (insql) {
            owner = data1.owner;
            allowed = new ArrayList<>(data1.allowed).toString();
            type = data1.type;
            date = data1.date;
        }

        if (incache) {
            if (data2.owner != null) {
                owner = data2.owner;
            }
            
            if (data2.allowed != null) {
                allowed = new ArrayList<>(data2.allowed).toString();
            }
            
            type = data2.type;
            date = Long.toString(System.currentTimeMillis());
        }

        Location loc = b.getLocation();
        com.msg(p, "&4Owner&8:&7 {0}", owner);
        com.msg(p, "&4Data&8:&7 W&8: &4{0}&7 X&8:&4{1}&7 Y&8:&4{2}&7 Z&8:&4{3}&7 T&8:&4{4}&8/&4{5}", loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), loc.getBlock().getTypeId(), type);
        if (!"".equals(allowed) && allowed != null && !"null".equals(allowed) && !allowed.isEmpty() && !"[]".equals(allowed)) {
            com.msg(p, "&7Permissions&8: &4{0}", allowed.replaceAll(" ,", " &a,&7").replaceAll("\\[", "").replaceAll("\\]", ""));
        }
        com.msg(p, "&7Status: &4{0}&7 &4{1}", 
                (incache ? "&4In Cache &7e" : ""), 
                (insql ? "&4SQL Database" : "&4On Queue&8 [Memory]"));
        com.msg(p, "&7Created in: &4{0}", CreativeUtil.getDate(Long.parseLong(date)));
        
        plugin.mods.remove(p.getName());
    }
    
    /*
     * Add a block to the database
     */
    public void add(Player p, Block b) {
        if (!is(p, b)) { return; }
        
        CreativeBlockManager manager = CreativeControl.getManager();
        CreativeMessages messages = CreativeControl.getMessages();
        CreativeControl plugin = CreativeControl.getPlugin();
        CreativeWorldNodes config = CreativeControl.getWorldNodes(b.getWorld());
        Communicator com = plugin.getCommunicator();

        if (config.block_ownblock) {
            CreativeBlockData data = manager.isprotected(b, true);
            if (data != null) {
                com.msg(p, messages.blockmanager_belongs, data.owner);
            } else {
                com.msg(p, messages.blockmanager_protected);
                manager.protect(p, b);
            }
        } else
        if (config.block_nodrop) {
            if (manager.isprotected(b, false) != null) {
                com.msg(p, messages.blockmanager_already);
            } else {
                com.msg(p, messages.blockmanager_protected);
                manager.protect(p, b);
            }
        }

        plugin.mods.remove(p.getName());
    }
    
    /*
     * Remove a protection from the block
     */
    public void del(Player p, Block b) {
        if (!is(p, b)) { return; }
        
        CreativeBlockManager manager = CreativeControl.getManager();
        CreativeMessages messages = CreativeControl.getMessages();
        CreativeControl plugin = CreativeControl.getPlugin();
        CreativeWorldNodes config = CreativeControl.getWorldNodes(b.getWorld());
        Communicator com = plugin.getCommunicator();

        if (config.block_ownblock) {
            CreativeBlockData data = manager.isprotected(b, true);
            if (data != null) {
                if (!data.owner.equalsIgnoreCase(p.getName())) {
                    com.msg(p, messages.blockmanager_belongs, data.owner);
                } else {
                    com.msg(p, messages.blockmanager_removed);
                    manager.unprotect(b);
                }
            } else {
                com.msg(p, messages.blockmanager_unprotected);
            }
        } else
        if (config.block_nodrop) {
            if (manager.isprotected(b, true) != null) {
                com.msg(p, messages.blockmanager_unprotected);
            } else {
                com.msg(p, messages.blockmanager_removed);
                manager.unprotect(b);
            }
        }

        plugin.mods.remove(p.getName());
    }
    
    private boolean is(Player p, Block b) {
        CreativeBlockManager manager = CreativeControl.getManager();
        CreativeMessages messages = CreativeControl.getMessages();
        CreativeControl plugin = CreativeControl.getPlugin();
        CreativeWorldNodes config = CreativeControl.getWorldNodes(b.getWorld());
        
        Communicator com = plugin.getCommunicator();
        
        if (config.world_exclude) {
            com.msg(p, messages.blockmanager_worldexcluded);
            plugin.mods.remove(p.getName());
            return false;
        }
        
        if (!manager.isprotectable(b.getWorld(), b.getTypeId())) {
            com.msg(p, messages.blockmanager_excluded);
            plugin.mods.remove(p.getName());
            return false;
        }
        
        return true;
    }

    private void cleanup(Player p) {
        CreativeControl plugin = CreativeControl.getPlugin();
        plugin.clear(p);
        plugin.right.remove(p);
        plugin.left.remove(p);
        plugin.mods.remove(p.getName());
        CreativePlayerFriends friend = CreativeControl.getFriends();
        friend.uncache(p);
        CreativePlayerData data = CreativeControl.getPlayerData();
        data.clear(p.getName());
    }
}
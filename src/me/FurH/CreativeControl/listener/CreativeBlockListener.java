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

package me.FurH.CreativeControl.listener;

import de.diddiz.LogBlock.Consumer;
import me.FurH.CreativeControl.CreativeControl;
import me.FurH.CreativeControl.configuration.CreativeMainConfig;
import me.FurH.CreativeControl.configuration.CreativeMessages;
import me.FurH.CreativeControl.configuration.CreativeWorldConfig;
import me.FurH.CreativeControl.configuration.CreativeWorldNodes;
import me.FurH.CreativeControl.manager.CreativeBlockData;
import me.FurH.CreativeControl.manager.CreativeBlockManager;
import me.FurH.CreativeControl.monitor.CreativePerformance;
import me.FurH.CreativeControl.monitor.CreativePerformance.Event;
import me.FurH.CreativeControl.util.CreativeCommunicator;
import me.FurH.CreativeControl.util.CreativeUtil;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.material.MaterialData;
import org.bukkit.material.PistonBaseMaterial;

/**
 *
 * @author FurmigaHumana
 */
public class CreativeBlockListener implements Listener {
    
    /*
     * Block Place Module
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        double start = System.currentTimeMillis();
        
        Player p = e.getPlayer();
        Block b = e.getBlockPlaced();
        World world = p.getWorld();
        
        CreativeCommunicator    com        = CreativeControl.getCommunicator();
        CreativeMessages        messages   = CreativeControl.getMessages();
        CreativeControl         plugin     = CreativeControl.getPlugin();
        CreativeWorldNodes config = CreativeWorldConfig.get(world);
        
        /*
         * Excluded Worlds
         */
        if (config.world_exclude) { return; }
        
        if (p.getGameMode().equals(GameMode.CREATIVE)) {
            if (e.isCancelled()) {
                if (e.getBlockAgainst().getType() == Material.WALL_SIGN || e.getBlockAgainst().getType() == Material.SIGN_POST) {
                    Sign sign = (Sign)e.getBlockAgainst().getState();
                    if (CreativeUtil.isEconomySign(sign)) {
                        if (!plugin.hasPerm(p, "BlackList.EconomySigns")) {
                            com.msg(p, messages.player_cantdo);
                            e.setCancelled(true);
                            return;
                        }
                    }
                }
            }
        }
        
        if (e.isCancelled()) { return; }

        /*
         * Gamemode Handler
         */
        CreativeMainConfig      main       = CreativeControl.getMainConfig();
        if (!main.events_move) {
            if (config.world_changegm) {
                if (p.getGameMode().equals(GameMode.CREATIVE)) {
                    if ((!config.world_creative) && (!plugin.hasPerm(p, "World.Keep"))) {
                        com.msg(p, messages.blocks_nocreative);
                        p.setGameMode(GameMode.SURVIVAL);
                        e.setCancelled(true);
                        return;
                    }
                } else 
                if (p.getGameMode().equals(GameMode.SURVIVAL)) {
                    if ((config.world_creative) && (!plugin.hasPerm(p, "World.Keep"))) {
                        com.msg(p, messages.blocks_nosurvival);
                        p.setGameMode(GameMode.CREATIVE);
                        e.setCancelled(true);
                        return;
                    }
                }
            }
        }
        
        if (p.getGameMode().equals(GameMode.CREATIVE)) {
            /*
             * Block Place BlackList
             */
            if ((config.black_place != null) && (config.black_place.contains(b.getTypeId()))) {
                if (!plugin.hasPerm(p, "BlackList.BlockPlace." + b.getTypeId())) {
                    String blockName = b.getType().toString().toLowerCase().replace("_", " ");
                    com.msg(p, messages.blocks_cantplace, blockName);
                    e.setCancelled(true);
                    return;
                }
            }
            
            /*
             * Anti Whiter Creation
             */
            if ((config.prevent_wither) && (!plugin.hasPerm(p, "Preventions.Wither"))) {
                if (e.getBlockPlaced().getType() == Material.SKULL) {
                    if ((world.getBlockAt(b.getX(), b.getY() - 1, b.getZ()).getType() == Material.SOUL_SAND) &&
                            (world.getBlockAt(b.getX(), b.getY() - 2, b.getZ()).getType() == Material.SOUL_SAND) &&
                            (world.getBlockAt(b.getX() + 1, b.getY() - 1, b.getZ()).getType() == Material.SOUL_SAND) &&
                            (world.getBlockAt(b.getX() - 1, b.getY() - 1, b.getZ()).getType() == Material.SOUL_SAND) ||
                            (world.getBlockAt(b.getX(), b.getY() - 1, b.getZ() - 1).getType() == Material.SOUL_SAND) &&
                            (world.getBlockAt(b.getX(), b.getY() - 1, b.getZ() + 1).getType() == Material.SOUL_SAND)) {
                        com.msg(p, messages.blocks_wither);
                        e.setCancelled(true);
                        return;
                    }
                }
            }
            
            /*
             * Anti SnowGolem Creation
             */
            if ((config.prevent_snowgolem) && (!plugin.hasPerm(p, "Preventions.SnowGolem")) && 
                    ((b.getType() == Material.PUMPKIN) || (b.getType() == Material.JACK_O_LANTERN)) &&
                    (world.getBlockAt(b.getX(), b.getY() - 1, b.getZ()).getType() == Material.SNOW_BLOCK) &&
                    (world.getBlockAt(b.getX(), b.getY() - 2, b.getZ()).getType() == Material.SNOW_BLOCK)) {
                com.msg(p, messages.blocks_snowgolem);
                e.setCancelled(true);
                return;
            }
            
            /*
             * Anti IronGolem Creation
             */
            if ((config.prevent_irongolem) && (!plugin.hasPerm(p, "Preventions.IronGolem")) && 
                    ((b.getType() == Material.PUMPKIN) || (b.getType() == Material.JACK_O_LANTERN)) && 
                    (world.getBlockAt(b.getX(), b.getY() - 1, b.getZ()).getType() == Material.IRON_BLOCK) &&
                    (world.getBlockAt(b.getX(), b.getY() - 2, b.getZ()).getType() == Material.IRON_BLOCK) &&
                    (((world.getBlockAt(b.getX() + 1, b.getY() - 1, b.getZ()).getType() == Material.IRON_BLOCK) &&
                    (world.getBlockAt(b.getX() - 1, b.getY() - 1, b.getZ()).getType() == Material.IRON_BLOCK)) ||
                    ((world.getBlockAt(b.getX(), b.getY() - 1, b.getZ() + 1).getType() == Material.IRON_BLOCK) &&
                    (world.getBlockAt(b.getX(), b.getY() - 1, b.getZ() - 1).getType() == Material.IRON_BLOCK)))) {
                com.msg(p, messages.blocks_irongolem);
                e.setCancelled(true);
                return;
            }
        }
        
        CreativeBlockManager    manager    = CreativeControl.getManager();

        Block r = e.getBlockReplacedState().getBlock();
        Block ba = e.getBlockAgainst();
        /*
         * NoDrop Save Section
         */
        if (config.block_nodrop) {
            if (config.misc_liquid) {
                if (r.getType() != Material.AIR) {
                    manager.delBlock(r);
                }
            } 
            if (p.getGameMode().equals(GameMode.CREATIVE)) {
                if (!plugin.hasPerm(p, "NoDrop.DontSave")) {
                    manager.addBlock(p, b, true);
                }
            }
        } else
        /*
         * OwnBlock Section
         */
        if (config.block_ownblock) {
            if (config.misc_liquid) {
                if (r.getType() != Material.AIR) {
                    String[] data = manager.getBlock(b);
                    if (data != null) {
                        if (manager.isAllowed(p, data)) {
                            manager.delBlock(b);
                        } else {
                            com.msg(p, messages.blocks_pertence, data[0]);
                            e.setCancelled(true);
                            return;
                        }
                    }
                }
            }

            if (config.block_against) {
                String[] data = manager.getBlock(ba);
                if (data != null) {
                    if (!manager.isAllowed(p, data)) {
                        com.msg(p, messages.blocks_pertence, data[0]);
                        e.setCancelled(true);
                        return;
                    }
                }
            }

            if (p.getGameMode().equals(GameMode.CREATIVE)) {
                if (!plugin.hasPerm(p, "OwnBlock.DontSave")) {
                    manager.addBlock(p.getName(), b, false);
                }
            }
        }
        CreativePerformance.update(Event.BlockPlaceEvent, (System.currentTimeMillis() - start));
    }

    
    /*
     * Block Break Module
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        double start = System.currentTimeMillis();
        Player p = e.getPlayer();
        Block b = e.getBlock();
        World world = p.getWorld();

        CreativeControl         plugin     = CreativeControl.getPlugin();
        CreativeCommunicator    com        = CreativeControl.getCommunicator();
        CreativeMessages        messages   = CreativeControl.getMessages();
        CreativeWorldNodes      config     = CreativeWorldConfig.get(world);
        CreativeBlockManager    manager    = CreativeControl.getManager();
        
        /*
         * Excluded Worlds
         */
        if (config.world_exclude) { return; }
        
        if (p.getGameMode().equals(GameMode.CREATIVE)) {
            if (e.isCancelled()) {
                if (b.getType() == Material.WALL_SIGN || b.getType() == Material.SIGN_POST) {
                    Sign sign = (Sign)b.getState();
                    if (CreativeUtil.isEconomySign(sign)) {
                        if (!plugin.hasPerm(p, "BlackList.EconomySigns")) {
                            com.msg(p, messages.player_cantdo);
                            e.setCancelled(true);
                            return;
                        }
                    }
                }
            }
        }
        
        if (e.isCancelled()) { return; }
        /*
         * Gamemode Handler
         */
        CreativeMainConfig      main       = CreativeControl.getMainConfig();
        if (!main.events_move) {
            if (config.world_changegm) {
                if (p.getGameMode().equals(GameMode.CREATIVE)) {
                    if ((!config.world_creative) && (!plugin.hasPerm(p, "World.Keep"))) {
                        com.msg(p, messages.blocks_nocreative);
                        p.setGameMode(GameMode.SURVIVAL);
                        e.setCancelled(true);
                        return;
                    }
                } else 
                if (p.getGameMode().equals(GameMode.SURVIVAL)) {
                    if ((config.world_creative) && (!plugin.hasPerm(p, "World.Keep"))) {
                        com.msg(p, messages.blocks_nosurvival);
                        p.setGameMode(GameMode.CREATIVE);
                        e.setCancelled(true);
                        return;
                    }
                }
            }
        }

        /*
         * Anti BedRock Breaking
         */
        if (p.getGameMode().equals(GameMode.CREATIVE)) {
            if ((config.prevent_bedrock) && (!plugin.hasPerm(p, "Preventions.BreakBedRock"))) {
                if (b.getType() == Material.BEDROCK) {
                    if (b.getY() < 1) {
                        com.msg(p, messages.blocks_bedrock);
                        e.setCancelled(true);
                        return;
                    }
                }
            }

            /*
             * Block Break BlackList
             */
            if ((config.black_break != null) && (config.black_break.contains(b.getTypeId()))) {
                if (!plugin.hasPerm(p, "BlackList.BlockBreak" + b.getTypeId())) {
                    String blockName = b.getType().toString().toLowerCase().replace("_", " ");
                    com.msg(p, messages.blocks_cantbreak, blockName);
                    e.setCancelled(true);
                    return;
                }
            }
        }
                
        if (config.block_ownblock) {
            for (CreativeBlockData bls : manager.getBlocks(b, config.block_attach)) {
                if (!manager.isAllowed(p, bls.getData())) {
                    com.msg(p, messages.blocks_pertence, bls.getData()[0]);
                    e.setCancelled(true);
                    break;
                } else {
                    process(config, e, bls.getBlock(), p);
                }
            }
        } else
        if (config.block_nodrop) {
            for (CreativeBlockData bls : manager.getBlocks(b, config.block_attach)) {
                process(config, e, bls.getBlock(), p);
            }
        }
        CreativePerformance.update(Event.BlockBreakEvent, (System.currentTimeMillis() - start));
    }
    
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        if (e.isCancelled()) { return; }
        
        double start = System.currentTimeMillis();

        World world = e.getBlock().getWorld();
        CreativeBlockManager    manager    = CreativeControl.getManager();
        CreativeWorldNodes config = CreativeWorldConfig.get(world);
        
        if (config.block_pistons) {
            for (Block b : e.getBlocks()) {
                if (b.getType() == Material.AIR) { return; }
                if (manager.isProtected(b)) {
                    e.setCancelled(true);
                    break;
                }
            }
        }
        CreativePerformance.update(Event.BlockPistonExtendEvent, (System.currentTimeMillis() - start));
    }
    
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPistonRetract(BlockPistonRetractEvent e) {
        if (e.isCancelled()) { return; }
        
        double start = System.currentTimeMillis();
        
        Block b = e.getBlock();
        World world = b.getWorld();

        if (b.getType() == Material.AIR) { return; }
        if (!e.isSticky()) { return; }
        
        CreativeWorldNodes config = CreativeWorldConfig.get(world);
        
        if (config.block_pistons) {
            BlockFace direction = null;
            MaterialData data = b.getState().getData();

            if (data instanceof PistonBaseMaterial) {
                direction = ((PistonBaseMaterial) data).getFacing();
            }
            
            if (direction == null) { return; }
            Block moved = b.getRelative(direction, 2);
            CreativeBlockManager    manager    = CreativeControl.getManager();
            if (manager.isProtected(moved)) {
                e.setCancelled(true);
            }
        }
        
        CreativePerformance.update(Event.BlockPistonRetractEvent, (System.currentTimeMillis() - start));
    }
    
    public void logBlock(Player p, Block b) {
        Consumer                consumer   = CreativeControl.getConsumer();
        if (consumer != null) {
            consumer.queueBlockBreak(p.getName(), b.getState());
        }
    }

    private void process(CreativeWorldNodes config, BlockBreakEvent e, Block b, Player p) {
        if (!e.isCancelled()) {
            CreativeCommunicator    com        = CreativeControl.getCommunicator();
            CreativeMessages        messages   = CreativeControl.getMessages();
            CreativeBlockManager    manager    = CreativeControl.getManager();
            if (config.block_creative) {
                if (p.getGameMode().equals(GameMode.CREATIVE)) {
                    manager.delBlock(b);
                    logBlock(p, b);
                    e.setExpToDrop(0);
                    b.setType(Material.AIR);
                } else {
                    com.msg(p, messages.blocks_creative);
                    e.setCancelled(true);
                }
            } else {
                manager.delBlock(b);
                logBlock(p, b);
                e.setExpToDrop(0);
                b.setType(Material.AIR);
                if (!p.getGameMode().equals(GameMode.CREATIVE)) {
                    com.msg(p, messages.blocks_nodrop);
                }
            }
        }
    }
}
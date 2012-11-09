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
import java.util.List;
import me.FurH.CreativeControl.CreativeControl;
import me.FurH.CreativeControl.configuration.CreativeMainConfig;
import me.FurH.CreativeControl.configuration.CreativeMessages;
import me.FurH.CreativeControl.configuration.CreativeWorldConfig;
import me.FurH.CreativeControl.configuration.CreativeWorldNodes;
import me.FurH.CreativeControl.database.CreativeBlockManager;
import me.FurH.CreativeControl.database.CreativeBlockMatcher;
import me.FurH.CreativeControl.util.CreativeCommunicator;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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
        
        if (config.prevent_economy) {
            if (e.getBlockAgainst().getType() == Material.WALL_SIGN || e.getBlockAgainst().getType() == Material.SIGN_POST) {
                com.msg(p, messages.player_cantdo);
                e.setCancelled(true);
                return;
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
                if (b.getType() == Material.SKULL && (world.getBlockAt(b.getX(), b.getY() - 1, b.getZ()).getType() == Material.SOUL_SAND) &&
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
                manager.addBlock(p.getName(), b);
            }
        } else
        /*
         * OwnBlock Section
         */
        if (config.block_ownblock) {
            if (config.misc_liquid) {
                if (r.getType() != Material.AIR) {
                    String[] data = manager.getBlock(r);
                    if (data != null) {
                        if (manager.isAllowed(p, data)) {
                            manager.delBlock(b, data);
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
                manager.addBlock(p.getName(), b);
            }
        }
    }

    
    /*
     * Block Break Module
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {        
        Player p = e.getPlayer();
        Block b = e.getBlock();
        World world = p.getWorld();

        CreativeControl         plugin     = CreativeControl.getPlugin();
        CreativeCommunicator    com        = CreativeControl.getCommunicator();
        CreativeMessages        messages   = CreativeControl.getMessages();
        CreativeWorldNodes config = CreativeWorldConfig.get(world);
        
        /*
         * Excluded Worlds
         */
        if (config.world_exclude) { return; }
        
        /* TODO: Find a way to check if is a economy sign! Some plugins uses breakevent to buy/sell in signs as well
        if (config.prevent_economy) {
            if (b.getType() == Material.WALL_SIGN || b.getType() == Material.SIGN_POST) {
                com.msg(p, messages.player_cantdo);
                e.setCancelled(true);
                return;
            }
        }
        */
        
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

        CreativeBlockManager    manager    = CreativeControl.getManager();
        if (config.block_ownblock) {
            if (b.getTypeId() == 64 || b.getTypeId() == 71) {
                String[] data = manager.getDoor2(b);
                if (data != null) {
                    if (!manager.isAllowed(p, data)) {
                        com.msg(p, messages.blocks_pertence, data[0]);
                        e.setCancelled(true);
                    } else {
                        process(e, data, b, p);
                    }
                }
            } else {
                String[] data = manager.getBlock(b);
                if (data != null) {
                    if (!manager.isAllowed(p, data)) {
                        com.msg(p, messages.blocks_pertence, data[0]);
                        e.setCancelled(true);
                    } else {
                        process(e, data, b, p);
                    }
                } else {
                    data = manager.getDoor3(b);
                    if (data != null) {
                        if (!manager.isAllowed(p, data)) {
                            com.msg(p, messages.blocks_pertence, data[0]);
                            e.setCancelled(true);
                        } else {
                            process(e, data, b, p);
                        }
                    } else {
                        List<Block> attach = CreativeBlockMatcher.getAttached(b);
                        if (attach != null && attach.size() > 0) {
                            for (Block ba1 : attach) {
                                data = manager.getBlock(ba1);
                                if (data != null) {
                                    if (!manager.isAllowed(p, data)) {
                                        com.msg(p, messages.blocks_pertence, data[0]);
                                        e.setCancelled(true);
                                    } else {
                                        process(e, data, ba1, p);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else
        if (config.block_nodrop) {
            if (b.getTypeId() == 64 || b.getTypeId() == 71) {
                String[] data = manager.getDoor2(b);
                if (data != null) {
                    process(e, data, b, p);
                }
            } else {
                String[] data = manager.getBlock(b);
                if (data != null) {
                    process(e, data, b, p);
                } else {
                    data = manager.getDoor3(b);
                    if (data != null) {
                        process(e, data, b, p);
                    } else {
                        List<Block> attach = CreativeBlockMatcher.getAttached(b);
                        if (attach != null && attach.size() > 0) {
                            for (Block ba1 : attach) {
                                data = manager.getBlock(ba1);
                                if (data != null) {
                                    process(e, data, ba1, p);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        if (e.isCancelled()) { return; }

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
    }
    
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPistonRetract(BlockPistonRetractEvent e) {
        if (e.isCancelled()) { return; }
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
    }
    
    public void logBlock(Player p, Block b) {
        Consumer                consumer   = CreativeControl.getConsumer();
        if (consumer != null) {
            consumer.queueBlockBreak(p.getName(), b.getState());
        }
    }

    private void process(BlockBreakEvent e, String[] data, Block b, Player p) {
        if (!e.isCancelled()) {
            CreativeCommunicator    com        = CreativeControl.getCommunicator();
            CreativeMessages        messages   = CreativeControl.getMessages();
            CreativeBlockManager    manager    = CreativeControl.getManager();
            CreativeWorldNodes config = CreativeWorldConfig.get(b.getWorld());
            if (config.block_creative) {
                if (p.getGameMode().equals(GameMode.CREATIVE)) {
                    manager.delBlock(b, data);
                    logBlock(p, b);
                    e.setExpToDrop(0);
                    b.setType(Material.AIR);
                } else {
                    com.msg(p, messages.blocks_creative);
                    e.setCancelled(true);
                }
            } else {
                manager.delBlock(b, data);
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
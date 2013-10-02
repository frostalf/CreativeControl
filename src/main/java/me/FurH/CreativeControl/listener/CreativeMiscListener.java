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

import java.util.Arrays;
import java.util.HashSet;
import me.FurH.Core.util.Communicator;
import me.FurH.CreativeControl.CreativeControl;
import me.FurH.CreativeControl.configuration.CreativeMessages;
import me.FurH.CreativeControl.configuration.CreativeWorldNodes;
import me.FurH.CreativeControl.manager.CreativeBlockData;
import me.FurH.CreativeControl.manager.CreativeBlockManager;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;

/**
 *
 * @author FurmigaHumana
 */
public class CreativeMiscListener implements Listener {
    
    /*
     * TNT Explosion Control
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onExplosionPrime(ExplosionPrimeEvent e) {
        if (e.isCancelled()) { return; }
        
        if ((e.getEntity() instanceof TNTPrimed)) {

            TNTPrimed tnt = (TNTPrimed)e.getEntity();
            Block b = tnt.getLocation().getBlock();

            CreativeWorldNodes config = CreativeControl.getWorldNodes(b.getWorld());

            if (config.world_exclude) {
                return;
            }
            
            CreativeBlockManager manager  = CreativeControl.getManager();

            if (config.misc_tnt) {
                if (manager.isprotected(b, true) != null) {
                    removeIgnition(b);
                    b.setType(Material.TNT);
                    e.setCancelled(true);
                }
            }
        }
    }
    
    private void removeIgnition(Block b) {
        HashSet<Integer> blocks = new HashSet<>(Arrays.asList(new Integer[]{10, 11, 27, 28, 51, 69, 70, 72, 73, 74, 75, 76, 77, 55, 331, 356}));
        
        int x = b.getX(); int y = b.getY(); int z = b.getZ(); int radius = 2;
        int minX = x - radius; int minY = y - radius; int minZ = z - radius;
        int maxX = x + radius; int maxY = y + radius; int maxZ = z + radius;

        for (int counterX = minX; counterX < maxX; counterX++) {
            for (int counterY = minY; counterY < maxY; counterY++) {
                for (int counterZ = minZ; counterZ < maxZ; counterZ++) {
                    Block block = b.getWorld().getBlockAt(counterX, counterY, counterZ);
                    if (blocks.contains(block.getTypeId())) {
                        CreativeBlockManager manager  = CreativeControl.getManager();
                        manager.unprotect(block);
                        block.breakNaturally();
                    }
                }
            }
        }
    }

    /*
     * Anti Creative Block Fire
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockIgnite(BlockIgniteEvent e) {
        if (e.isCancelled()) { return; }
        Block b = e.getBlock();

        CreativeWorldNodes config = CreativeControl.getWorldNodes(b.getWorld());
        
        if (config.world_exclude) {
            return;
        }
        
        if (config.misc_fire) {
            CreativeBlockManager manager  = CreativeControl.getManager();
            if (manager.isprotected(b, true) != null) {
                e.setCancelled(true);
            }
        }
    }
    
    /*
     * Anti Creative Block Fire
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockBurn(BlockBurnEvent e) {
        if (e.isCancelled()) { return; }
        
        Block b = e.getBlock();
        CreativeWorldNodes config = CreativeControl.getWorldNodes(b.getWorld());

        if (config.world_exclude) {
            return;
        }
        
        if (config.misc_fire) {
            CreativeBlockManager manager  = CreativeControl.getManager();
            if (manager.isprotected(b, true) != null) {
                removeFire(b);
                e.setCancelled(true);
            }
        }
    }
    
    private void removeFire(Block b) {
        int x = b.getX(); int y = b.getY(); int z = b.getZ(); int radius = 5;
        int minX = x - radius; int minY = y - radius; int minZ = z - radius;
        int maxX = x + radius; int maxY = y + radius; int maxZ = z + radius;

        for (int counterX = minX; counterX < maxX; counterX++) {
            for (int counterY = minY; counterY < maxY; counterY++) {
                for (int counterZ = minZ; counterZ < maxZ; counterZ++) {
                    Block block = b.getWorld().getBlockAt(counterX, counterY, counterZ);
                    if (block.getType() == Material.FIRE) {
                        block.setType(Material.AIR);
                    }
                }
            }
        }
    }
    
    /*
     * On block Fade
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockFade(BlockFadeEvent e) {
        if (e.isCancelled()) { return; }
        
        Block b = e.getBlock();
        Material type = b.getType();
        
        CreativeWorldNodes config = CreativeControl.getWorldNodes(b.getWorld());
        
        if (config.world_exclude) {
            return;
        }
        
        if (type == Material.ICE) {
            if (config.misc_ice) {
                CreativeBlockManager manager  = CreativeControl.getManager();
                if (manager.isprotected(b, true) != null) {
                    e.setCancelled(true);
                }
            }
        }
    }
    
    /*
     * On Block transform to other type
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockFromTo(BlockFromToEvent e) {
        if (e.isCancelled()) { return; }
        
        Block b = e.getBlock();
        CreativeWorldNodes config = CreativeControl.getWorldNodes(b.getWorld());
        
        if (config.world_exclude) {
            return;
        }
        
        if (config.misc_liquid) {
            CreativeBlockManager manager  = CreativeControl.getManager();
            if ((b.getType() == Material.WATER) || (b.getType() == Material.STATIONARY_WATER)) {
                if (manager.isprotected(b, true) != null) {
                    b.setType(Material.STATIONARY_WATER);
                    e.setCancelled(true);
                }
            }
            
            if ((b.getType() == Material.LAVA) || (b.getType() == Material.STATIONARY_LAVA)) {
                if (manager.isprotected(b, true) != null) {
                    b.setType(Material.STATIONARY_LAVA);
                    e.setCancelled(true);
                }
            }
        }
    }
    
    /*
     * On Lava/Water taked with a bukket
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBucketFill(PlayerBucketFillEvent e) {
        if (e.isCancelled()) { return; }
        
        Player p = e.getPlayer();
        Block b = e.getBlockClicked();
        
        CreativeWorldNodes config = CreativeControl.getWorldNodes(b.getWorld());
        
        if (config.world_exclude) {
            return;
        }
        
        if (config.misc_liquid) {
            CreativeBlockManager manager  = CreativeControl.getManager();
            if (config.block_ownblock) {
                CreativeBlockData data = manager.isprotected(b, true);
                if (data != null) {
                    if (manager.isAllowed(p, data)) {
                        manager.unprotect(b);
                    } else {
                        Communicator         com      = CreativeControl.plugin.getCommunicator();
                        CreativeMessages     messages = CreativeControl.getMessages();
                        com.msg(p, messages.blockmanager_belongs, data.owner);
                        e.setCancelled(true);
                    }
                }  
            } else
            if (config.block_nodrop) {
                manager.unprotect(b);
            }
        }
    }
    
    /*
     * On Water/Lava place by Bucket
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBucketEmpty(PlayerBucketEmptyEvent e) {
        if (e.isCancelled()) { return; }
        Player p = e.getPlayer();
        Material bucket = e.getBucket();
        
        Block bDown = e.getBlockClicked();
        Block b = e.getBlockClicked().getRelative(e.getBlockFace());

        CreativeWorldNodes config = CreativeControl.getWorldNodes(b.getWorld());
        
        if (config.world_exclude) { return; }
        
        if (p.getGameMode().equals(GameMode.CREATIVE)) {
            if (config.misc_liquid) {
                if ((bucket == Material.WATER_BUCKET) || (bucket == Material.LAVA_BUCKET) || (bucket == Material.BUCKET) || (bucket == Material.MILK_BUCKET)) {
                    CreativeBlockManager manager  = CreativeControl.getManager();
                    if (config.block_ownblock) {
                        CreativeBlockData data = manager.isprotected(bDown, true);
                        if (data != null) {
                            if (manager.isAllowed(p, data)) {
                                if (bucket == Material.WATER_BUCKET) {
                                    b.setType(Material.STATIONARY_WATER);
                                } else
                                if (bucket == Material.LAVA_BUCKET) {
                                    b.setType(Material.STATIONARY_LAVA);
                                }
                                manager.protect(p, b);
                            } else {
                                Communicator         com      = CreativeControl.plugin.getCommunicator();
                                CreativeMessages     messages = CreativeControl.getMessages();
                                com.msg(p, messages.blockmanager_belongs, data.owner);
                                e.setCancelled(true);
                            }
                        }
                    }
                    
                    if (config.block_nodrop) {
                        if (bucket == Material.WATER_BUCKET) {
                            b.setType(Material.STATIONARY_WATER);
                        } else
                        if (bucket == Material.LAVA_BUCKET) {
                            b.setType(Material.STATIONARY_LAVA);
                        }
                        manager.protect(p, b);
                    }
                }
            }
        }
    }
}

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

import java.util.Arrays;
import java.util.List;
import me.FurH.CreativeControl.CreativeControl;
import me.FurH.CreativeControl.configuration.CreativeMessages;
import me.FurH.CreativeControl.configuration.CreativeWorldConfig;
import me.FurH.CreativeControl.configuration.CreativeWorldNodes;
import me.FurH.CreativeControl.database.CreativeBlockManager;
import me.FurH.CreativeControl.util.CreativeCommunicator;
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
            CreativeWorldNodes config = CreativeWorldConfig.get(b.getWorld());
            
            if (config.world_exclude) { return; }
            
            CreativeBlockManager manager  = CreativeControl.getManager();
            if (config.misc_tnt) {
                if (manager.isProtected(b)) {
                    removeIgnition(b);
                    b.setType(Material.TNT);
                    e.setCancelled(true);
                }
            }
        }
    }
    
    private void removeIgnition(Block b) {
        List BLOCKS2 = Arrays.asList(new Integer[]{10, 11, 27, 28, 51, 69, 70, 72, 73, 74, 75, 76, 77, 55, 331, 356});
        int x = b.getX(); int y = b.getY(); int z = b.getZ(); int radius = 2;
        int minX = x - radius; int minY = y - radius; int minZ = z - radius;
        int maxX = x + radius; int maxY = y + radius; int maxZ = z + radius;

        for (int counterX = minX; counterX < maxX; counterX++) {
            for (int counterY = minY; counterY < maxY; counterY++) {
                for (int counterZ = minZ; counterZ < maxZ; counterZ++) {
                    Block block = b.getWorld().getBlockAt(counterX, counterY, counterZ);
                    if (BLOCKS2.contains(block.getTypeId())) {
                        CreativeBlockManager manager  = CreativeControl.getManager();
                        manager.delBlock(block);
                        block.setType(Material.AIR);
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

        CreativeWorldNodes config = CreativeWorldConfig.get(b.getWorld());
        
        if (config.world_exclude) { return; }
        
        if (config.misc_fire) {
            CreativeBlockManager manager  = CreativeControl.getManager();
            if (manager.isProtected(b)) {
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
        
        CreativeWorldNodes config = CreativeWorldConfig.get(b.getWorld());
        
        if (config.world_exclude) { return; }
        
        if (config.misc_fire) {
            CreativeBlockManager manager  = CreativeControl.getManager();
            if (manager.isProtected(b)) {
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
        
        CreativeWorldNodes config = CreativeWorldConfig.get(b.getWorld());
        
        if (config.world_exclude) { return; }
        
        if (type == Material.ICE) {
            if (config.misc_ice) {
                CreativeBlockManager manager  = CreativeControl.getManager();
                if (manager.isProtected(b)) {
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
        
        CreativeWorldNodes config = CreativeWorldConfig.get(b.getWorld());
        
        if (config.world_exclude) { return; }
        
        if (config.misc_liquid) {
            CreativeBlockManager manager  = CreativeControl.getManager();
            if ((b.getType() == Material.WATER) || (b.getType() == Material.STATIONARY_WATER)) {
                if (manager.isProtected(b)) {
                    b.setType(Material.STATIONARY_WATER);
                    e.setCancelled(true);
                }
            }
            
            if ((b.getType() == Material.LAVA) || (b.getType() == Material.STATIONARY_LAVA)) {
                if (manager.isProtected(b)) {
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
        
        CreativeWorldNodes config = CreativeWorldConfig.get(b.getWorld());
        
        if (config.world_exclude) { return; }
        
        if (config.misc_liquid) {
            CreativeBlockManager manager  = CreativeControl.getManager();
            if (config.block_ownblock) {
                String[] data = manager.getBlock(b);
                if (data != null) {
                    if (manager.isAllowed(p, data)) {
                        manager.delBlock(b, data);
                    } else {
                        CreativeCommunicator com      = CreativeControl.getCommunicator();
                        CreativeMessages     messages = CreativeControl.getMessages();
                        com.msg(p, messages.blocks_pertence, data[0]);
                        e.setCancelled(true);
                    }
                }  
            }
            if (config.block_nodrop) {
                manager.delBlock(b);
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

        CreativeWorldNodes config = CreativeWorldConfig.get(b.getWorld());
        
        if (config.world_exclude) { return; }
        
        if (p.getGameMode().equals(GameMode.CREATIVE)) {
            if (config.misc_liquid) {
                if ((bucket == Material.WATER_BUCKET) || (bucket == Material.LAVA_BUCKET) || (bucket == Material.BUCKET) || (bucket == Material.MILK_BUCKET)) {
                    CreativeBlockManager manager  = CreativeControl.getManager();
                    if (config.block_ownblock) {
                        String[] data = manager.getBlock(bDown);
                        if (data != null) {
                            if (manager.isAllowed(p, data)) {
                                if (bucket == Material.WATER_BUCKET) {
                                    b.setType(Material.STATIONARY_WATER);
                                } else
                                if (bucket == Material.LAVA_BUCKET) {
                                    b.setType(Material.STATIONARY_LAVA);
                                }
                                manager.addBlock(p.getName(), b);
                            } else {
                                CreativeCommunicator com      = CreativeControl.getCommunicator();
                                CreativeMessages     messages = CreativeControl.getMessages();
                                com.msg(p, messages.blocks_pertence, data[0]);
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
                        manager.addBlock(p.getName(), b);
                    }
                }
            }
        }
    }
}
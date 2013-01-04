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

package me.FurH.CreativeControl.manager;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import me.FurH.CreativeControl.CreativeControl;
import me.FurH.CreativeControl.cache.CreativeBlockCache;
import me.FurH.CreativeControl.configuration.CreativeMainConfig;
import me.FurH.CreativeControl.data.friend.CreativePlayerFriends;
import me.FurH.CreativeControl.database.CreativeSQLDatabase;
import me.FurH.CreativeControl.util.CreativeCommunicator;
import me.FurH.CreativeControl.util.CreativeUtil;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

/**
 *
 * @author FurmigaHumana
 */
public class CreativeOwnBlockManager {
    
    /*
     * return true if the block is protected
     */
    public static boolean isProtected(Block b) {
        return getBlock(b) != null;
    }

    /*
     * return the array representation of the protection
     */
    public static String[] getBlock(Block b) {
        return getBlock(b, false);
    }
    
    /*
     * return a simple protected door
     */
    public static String[] getDoor2(Block b) {
        String[] data = getBlock(b);
        
        if (data == null) {
            Block blockdown = b.getRelative(BlockFace.DOWN);
            if (blockdown.getTypeId() == 64 || blockdown.getTypeId() == 71) {
                data = getBlock(blockdown);
                if (data != null) {
                    return data;
                }
            }
        }
        
        return data;
    }
    
    public static String[] getDoor3(Block b) {
        Block blockup = b.getRelative(BlockFace.UP);
        String[] data = null;
                
        if (blockup.getTypeId() == 64 || blockup.getTypeId() == 71) {
            data = getBlock(blockup);
        }
        
        return data;
    }
    
    /*
     * return the array representation of the protection
     */
    public static String[] getBlock(Block b, boolean force) {
        if (!force && !CreativeControl.getManager().isProtectable(b.getWorld(), b.getTypeId())) {
            return null;
        }

        CreativeBlockCache   cache      = CreativeControl.getSlowCache();
        String location = CreativeUtil.getLocation(b.getLocation());
        CreativeSQLDatabase  db         = CreativeControl.getDb();
        String[] ret = cache.get(location);
        if (ret == null) {
            try {
                ResultSet rs = db.getQuery("SELECT owner, allowed FROM `"+db.prefix+"blocks` WHERE location = '" + location + "'");
                if (rs.next()) {
                    String owner = rs.getString("owner");
                    String allowed = rs.getString("allowed");
                    if (allowed != null || !"[]".equals(allowed) || !"".equals(allowed)) {
                        ret = new String[] { owner, allowed };
                    } else {
                        ret = new String[] { owner };
                    }
                }
            } catch (SQLException ex) {
                CreativeCommunicator com        = CreativeControl.getCommunicator();
                com.error("[TAG] Failed to get the block from the database, {0}", ex, ex.getMessage());
                if (!db.isOk()) { db.fix(); }
            }
        }
                
        return ret;
    }

    /*
     * return true if the player is allowed to modify that protection
     */
    public static boolean isAllowed(Player p, String[] data) {
        CreativeMainConfig   config     = CreativeControl.getMainConfig();
        CreativePlayerFriends friends = CreativeControl.getFriends();

        if (isOwner(p, data[0])) {
            return true;
        } else {
            if (data.length >= 1) {
                try {
                    if (isAllowed(p, data[1])) {
                        return true;
                    } else {
                        if (config.config_friend) {
                            HashSet<String> friend = friends.getFriends(data[0]);
                            if (friend.contains(p.getName().toLowerCase())) {
                                return true;
                            } else {
                                return false;
                            }
                        } else {
                            return false;
                        }
                    }
                } catch (Exception ex) {
                    if (config.config_friend) {
                        HashSet<String> friend = friends.getFriends(data[0]);
                        if (friend.contains(p.getName().toLowerCase())) {
                            return true;
                        } else {
                            return false;
                        }
                    } else {
                        return false;
                    }
                }
            } else {
                if (config.config_friend) {
                    HashSet<String> friend = friends.getFriends(data[0]);
                    if (friend.contains(p.getName().toLowerCase())) {
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        }
    }

    /*
     * return true if the player is the owner of that protection
     */
    public static boolean isOwner(Player p, String owner) {
        CreativeControl      plugin     = CreativeControl.getPlugin();
        if (plugin.hasPerm(p, "OwnBlock.Bypass")) {
            return true;
        } else {
            if (owner.equalsIgnoreCase(p.getName())) {
                return true;
            } else {
                return false;
            }
        }
    }
    
    /*
     * return true if the player is allowed to use that protection
     */
    private static boolean isAllowed(Player p, String allowed) {
        CreativeControl      plugin     = CreativeControl.getPlugin();
        if (plugin.hasPerm(p, "OwnBlock.Bypass")) {
            return true;
        } else {
            if (allowed != null && !"[]".equals(allowed) && !"".equals(allowed) && !"null".equals(allowed)) {
                if (CreativeUtil.toStringHashSet(allowed, ", ").contains(p.getName().toLowerCase())) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }

    public static void update(String location, String owner, String allowed) {
        CreativeBlockCache   cache      = CreativeControl.getSlowCache();
        CreativeSQLDatabase  db         = CreativeControl.getDb();
        if (allowed != null && !"".equals(allowed) && !"[]".equals(allowed)) {
            cache.replace(location, new String[] { owner, allowed });
            db.executeQuery("UPDATE `"+db.prefix+"blocks` SET `allowed` = '"+allowed+"' WHERE `location` = '"+location+"';");
        } else {
            cache.replace(location, new String[] { owner });
            db.executeQuery("UPDATE `"+db.prefix+"blocks` SET `allowed` = '"+null+"' WHERE `location` = '"+location+"';");
        }
    }
}
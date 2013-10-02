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

package me.FurH.CreativeControl.manager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import me.FurH.Core.cache.CoreLRUCache;
import me.FurH.Core.exceptions.CoreException;
import me.FurH.Core.list.CollectionUtils;
import me.FurH.Core.location.LocationUtils;
import me.FurH.Core.util.Communicator;
import me.FurH.CreativeControl.CreativeControl;
import me.FurH.CreativeControl.blacklist.CreativeBlackList;
import me.FurH.CreativeControl.configuration.CreativeMainConfig;
import me.FurH.CreativeControl.configuration.CreativeWorldNodes;
import me.FurH.CreativeControl.data.friend.CreativePlayerFriends;
import me.FurH.CreativeControl.database.CreativeSQLDatabase;
import me.FurH.CreativeControl.stack.CreativeItemStack;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 *
 * @author FurmigaHumana
 */
public class CreativeBlockManager {
    
    private static CoreLRUCache<String, CreativeBlockData> cache;

    public CreativeBlockManager() {
        cache = new CoreLRUCache<>(CreativeControl.getMainConfig().cache_capacity);
        cache.setSoftCache(true);
    }

    public CoreLRUCache<String, CreativeBlockData> getCache() {
        return cache;
    }

    public boolean isAllowed(Player p, CreativeBlockData data) {
        
        if (data == null) {
            return true;
        }
        
        if (data.owner != null && data.owner.equalsIgnoreCase(p.getName())) {
            return true;
        }
        
        if (CreativeControl.plugin.hasPerm(p, "OwnBlock.Bypass")) {
            return true;
        }
        
        if (data.allowed != null && data.allowed.contains(p.getName())) {
            return true;
        }
        
        CreativeMainConfig config = CreativeControl.getMainConfig();

        if (config.config_friend) {
            CreativePlayerFriends friends = CreativeControl.getFriends();
            return friends.getFriends(data.owner).contains(p.getName());
        }

        return false;
    }
    
    public void unprotect(Block b) {
        unprotect(b.getWorld(), b.getX(), b.getY(), b.getZ(), b.getTypeId());
    }
    
    public void unprotect(World world, int x, int y, int z, int type) {
        if (isprotectable(world, type)) {

            cache.remove(LocationUtils.locationToString(x, y, z, world.getName()));
            CreativeControl.getDb().unprotect(world.getName(), x, y, z);
        }
    }

    public void protect(Player p, Block b) {
        protect(p.getName(), b);
    }

    public void protect(String player, Block b) {
        protect(player, b.getWorld(), b.getX(), b.getY(), b.getZ(), b.getTypeId());
    }
    
    public void protect(String owner, World world, int x, int y, int z, int type) {
        if (isprotectable(world, type)) {

            CreativeBlockData data = new CreativeBlockData(owner, type, null);
            cache.put(LocationUtils.locationToString(x, y, z, world.getName()), data);

            CreativeControl.getDb().protect(owner, world.getName(), x, y, z, type);
        }
    }
    
    public int preCache() {
        
        CreativeSQLDatabase db = CreativeControl.getDb();
        Communicator com = CreativeControl.plugin.getCommunicator();
        CreativeMainConfig config = CreativeControl.getMainConfig();
        
        int worlds = Bukkit.getWorlds().size();
        int pass = 0;
        int count = 0;

        int each = (int) Math.floor(config.cache_precache / worlds);

        try {

            List<World> worldsx = new ArrayList<>(Bukkit.getWorlds());
            Collections.reverse(worldsx);

            for (World world : worldsx) {

                PreparedStatement ps = db.getQuery("SELECT * FROM `"+db.prefix+"blocks_"+world.getName() + "` ORDER BY 'time' DESC LIMIT "+each+";");
                ResultSet rs = ps.getResultSet();

                pass++;

                boolean nodrop = CreativeControl.getWorldNodes(world).block_nodrop;
                int ran = 0;

                while (rs.next()) {
                    CreativeBlockData data = null;

                    if (!nodrop) {
                        data = new CreativeBlockData(db.getPlayerName(rs.getInt("owner")), rs.getInt("type"), CollectionUtils.toStringHashSet(rs.getString("allowed"), ", "));
                    } else {
                        data = new CreativeBlockData(rs.getInt("type"));
                    }
                    
                    count++;
                    cache.put(LocationUtils.locationToString(rs.getInt("x"), rs.getInt("y"), rs.getInt("z"), world.getName()), data);
                    ran++;
                }
                
                ran++;
                
                if (ran < each) {
                    if ((worlds - pass) > 0) {
                        each = (int) Math.floor((config.cache_precache - pass) / (worlds - pass));
                    } else {
                        each = ((config.cache_precache - pass));
                    }
                }
                
                rs.close();
                ps.close();
            }
            
            worldsx.clear();
        } catch (Exception ex) {
            com.error(ex, "Failed to add protections to cache");
        }

        return count;
    }
    
    public int getTotal() {
        CreativeSQLDatabase db = CreativeControl.getDb();
        Communicator com = CreativeControl.plugin.getCommunicator();
        
        int total = 0;

        List<World> worlds = new ArrayList<>(Bukkit.getWorlds());

        for (World world : worlds) {
            try {
                total += db.getTableCount(db.prefix+"blocks_"+world.getName());
            } catch (CoreException ex) {
                com.error(ex, "Failed to count world tables size");
            }
        }
        
        return total;
    }

    public void update(CreativeBlockData data, Block block) {
        update(data, block.getWorld(), block.getX(), block.getY(), block.getZ());
    }
    
    public void update(CreativeBlockData data, World world, int x, int y, int z) {
        CreativeSQLDatabase  db         = CreativeControl.getDb();

        if (data == null) {
            return;
        }

        if (data.allowed == null || data.allowed.isEmpty()) {
            data.allowed = null;
        }

        db.update(data, world.getName(), x, y, z);
        
        cache.put(LocationUtils.locationToString(x, y, z, world.getName()), data);
    }
    
    public CreativeBlockData isprotected(Block block, boolean nodrop) {
        return isprotected(block.getWorld(), block.getX(), block.getY(), block.getZ(), block.getTypeId(), nodrop);
    }
    
    public CreativeBlockData isprotected(World world, int x, int y, int z, int type, boolean nodrop) {
        
        if (!isprotectable(world, type)) {
            return null;
        }
        
        String key = LocationUtils.locationToString(x, y, z, world.getName());
        CreativeBlockData data = null;

        if (cache.containsKey(key)) {
            data = cache.get(key);
            if (data != null) {
                return data;
            }
        }

        data = CreativeControl.getDb().isprotected(world.getName(), x, y, z, type, nodrop);

        if (data != null) {
            cache.put(key, data);
        }

        return data;
    }
    
    public CreativeBlockData getFullData(Location location) {
        return CreativeControl.getDb().getFullData(location);
    }
    
    public boolean isprotectable(World world, int typeId) {
        CreativeWorldNodes nodes = CreativeControl.getWorldNodes(world);

        CreativeBlackList       blacklist  = CreativeControl.getBlackList();
        CreativeItemStack       itemStack  = new CreativeItemStack(typeId, (byte) -1);

        if (nodes.block_invert) {
            return blacklist.isBlackListed(nodes.block_exclude, itemStack);
        } else {
            return !blacklist.isBlackListed(nodes.block_exclude, itemStack);
        }
    }

    public void clear() {
        cache.clear();
    }

    public double getTablesSize() {
        CreativeSQLDatabase db = CreativeControl.getDb();
        double ret = 0;

        for (World world : Bukkit.getWorlds()) {
            try {
                ret += db.getTableSize(db.prefix+"blocks_"+world.getName());
            } catch (CoreException ex) {
                CreativeControl.getPlugin().getCommunicator().error(ex, "Failed to get world tables size");
            }
        }

        return ret;
    }

    public double getTablesFree() {
        CreativeSQLDatabase db = CreativeControl.getDb();
        double ret = 0;

        for (World world : Bukkit.getWorlds()) {
            try {
                ret += db.getTableFree(db.prefix+"blocks_"+world.getName());
            } catch (CoreException ex) {
                CreativeControl.getPlugin().getCommunicator().error(ex, "Failed to get world tables free size");
            }
        }

        return ret;
    }
}
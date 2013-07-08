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

package me.FurH.CreativeControl.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import me.FurH.Core.CorePlugin;
import me.FurH.Core.cache.CoreSafeCache;
import me.FurH.Core.database.CoreSQLDatabase;
import me.FurH.Core.exceptions.CoreException;
import me.FurH.Core.list.CollectionUtils;
import me.FurH.Core.location.LocationUtils;
import me.FurH.Core.util.Communicator;
import me.FurH.CreativeControl.CreativeControl;
import me.FurH.CreativeControl.database.extra.CreativeSQLUpdater;
import me.FurH.CreativeControl.manager.CreativeBlockData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 *
 * @author FurmigaHumana
 */
public final class CreativeSQLDatabase extends CoreSQLDatabase {
    
    private static CoreSafeCache<String, Integer> owners = new CoreSafeCache<>();

    public CreativeSQLDatabase(CorePlugin plugin, String prefix, String engine, String database_host, String database_port, String database_table, String database_user, String database_pass) {
        super(plugin, prefix, engine, database_host, database_port, database_table, database_user, database_pass);
        super.setDatabaseVersion(3);
        this.prefix = prefix;
    }
    
    public String[] getOldGroup(Player player) {
        String[] ret = null;
        
        Communicator com = CreativeControl.plugin.getCommunicator();
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {

            ps = getQuery("SELECT groups FROM `"+prefix+"groups` WHERE player = '"+getPlayerId(player.getName()) + "' LIMIT 1;");
            rs = ps.getResultSet();

            if (rs.next()) {
                ret = CollectionUtils.toStringList(rs.getString("groups"), ", ").toArray(new String[0]);
            } else {
                setOldGroups(player);
            }

        } catch (Exception ex) {
            com.error(ex, "Failed to get old group data for the player: " + player.getName());
        } finally {
            closeQuietly(ps);
            closeQuietly(rs);
        }

        return ret;
    }
    
    public void saveOldGroups(Player player, String[] groups) {
        Communicator com = CreativeControl.plugin.getCommunicator();

        try {
            execute("UPDATE `"+prefix+"groups` SET groups = '"+Arrays.toString(groups)+"' WHERE player = '"+getPlayerId(player.getName())+"';");
        } catch (CoreException ex) {
            com.error(ex, "Failed to save '"+player.getName()+"' groups data");
        }
        
        try {
            commit();
        } catch (CoreException ex) { }
    }
    
    private void setOldGroups(Player player) {
        Communicator com = CreativeControl.plugin.getCommunicator();

        try {
            execute("INSERT INTO `"+prefix+"groups` (player, groups) VALUES ('"+getPlayerId(player.getName())+"', '');");
        } catch (CoreException ex) {
            com.error(ex, "Failed to save '"+player.getName()+"' groups data");
        }

        try {
            commit();
        } catch (CoreException ex) { }
    }

    public void protect(Player player, Block block) {
        protect(player.getName(), block);
    }

    public void update(CreativeBlockData data, Block block) {
        update(data, block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }
    
    public void update(CreativeBlockData data, String world, int x, int y, int z) {
        queue("UPDATE `"+prefix+"blocks_"+world+"` SET `allowed` = '"+data.allowed+"', `owner` = '"+getPlayerId(data.owner)+"' WHERE x = '" + x + "' AND z = '" + z + "' AND y = '" + y + "';");
    }
    
    public void protect(String player, Block block) {
        protect(player, block.getWorld().getName(), block.getX(), block.getY(), block.getZ(), block.getTypeId());
    }
    
    public void protect(String player, String world, int x, int y, int z, int type) {
        queue("INSERT INTO `"+prefix+"blocks_"+world+"` (owner, x, y, z, type, allowed, time) VALUES ('"+getPlayerId(player) + "', '" + x + "', '" + y + "', '" + z + "', '" + type + "', '" + null + "', '" + System.currentTimeMillis() + "');");
    }
    
    public void unprotect(Block block) {
        unprotect(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    public void unprotect(String world, int x, int y, int z) {
        queue("DELETE FROM `"+prefix+"blocks_"+world+"` WHERE x = '" + x + "' AND z = '" + z + "' AND y = '" + y + "';");
    }

    public CreativeBlockData getFullData(Location block) {
        
        Communicator com = CreativeControl.plugin.getCommunicator();
        CreativeBlockData data = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            ps = getQuery("SELECT * FROM `"+prefix+"blocks_"+block.getWorld().getName()+"` WHERE x = '" + block.getX() + "' AND z = '" + block.getZ() + "' AND y = '" + block.getY() + "';");

            rs = ps.getResultSet();

            if (rs.next()) {
                data = new CreativeBlockData(getPlayerName(rs.getInt("owner")), rs.getInt("type"), CollectionUtils.toStringHashSet(rs.getString("allowed"), ", "), Long.toString(rs.getLong("time")));
            } else if (CreativeSQLUpdater.lock) {
                ps = getQuery("SELECT * FROM `"+prefix+"blocks` WHERE location = "+LocationUtils.locationToString2(block)+"';");
                rs = ps.getResultSet();

                if (rs.next()) {
                    data = new CreativeBlockData(getPlayerName(rs.getInt("owner")), rs.getInt("type"), CollectionUtils.toStringHashSet(rs.getString("allowed"), ", "), rs.getString("time"));
                }
            }

        } catch (SQLException ex) {
            com.error(ex, "Failed to get block from database");
        } catch (CoreException ex) {
            com.error(ex, "Failed to get block from database");
        } finally {
            closeQuietly(rs);
        }
        
        return data;
    }
    
    public CreativeBlockData isprotected(Block block, boolean nodrop) {
        return null;
    }
    
    public CreativeBlockData isprotected(String world, int x, int y, int z, int type, boolean nodrop) {
        
        Communicator com = CreativeControl.plugin.getCommunicator();
        CreativeBlockData data = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            if (nodrop) {
                ps = getQuery("SELECT owner, type, allowed FROM `"+prefix+"blocks_"+world+"` WHERE x = '" + x + "' AND z = '" + z + "' AND y = '" + y + "';");
            } else {
                ps = getQuery("SELECT type FROM `"+prefix+"blocks_"+world+"` WHERE x = '" + x + "' AND z = '" + z + "' AND y = '" + y + "';");
            }

            rs = ps.getResultSet();
        
            if (rs.next()) {
                if (nodrop) {
                    data = new CreativeBlockData(getPlayerName(rs.getInt("owner")), rs.getInt("type"), CollectionUtils.toStringHashSet(rs.getString("allowed"), ", "));
                } else {
                    data = new CreativeBlockData(rs.getInt("type"));
                }
            } else if (CreativeSQLUpdater.lock) {
                ps = getQuery("SELECT owner, type, allowed FROM `"+prefix+"blocks` WHERE location = "+LocationUtils.locationToString2(world, x, y, z)+"';");
                rs = ps.getResultSet();

                if (rs.next()) {
                    data = new CreativeBlockData(getPlayerName(rs.getInt("owner")), rs.getInt("type"), CollectionUtils.toStringHashSet(rs.getString("allowed"), ", "));
                }
            }

        } catch (SQLException ex) {
            com.error(ex, "Failed to get block from database");
        } catch (CoreException ex) {
            com.error(ex, "Failed to get block from database");
        } finally {
            closeQuietly(rs);
        }

        if (data != null && data.type != 73 && data.type != 74 && data.type != type) {
            data = null;
        }
        
        return data;
    }
    
    public void load() {
        load(this.connection, this.getDatabaseEngine());
    }

    public void load(Connection connection, type type) {
        Communicator com = CreativeControl.getPlugin().getCommunicator();
        
        try {
            /* groups table */
            createTable(connection, "CREATE TABLE IF NOT EXISTS `"+prefix+"groups` (player INT, groups VARCHAR(255));", type);
        } catch (CoreException ex) {
            com.error(ex, "Failed to create `"+prefix+"groups` table");
        }
        
        try {
            /* player id table */
            createTable(connection, "CREATE TABLE IF NOT EXISTS `"+prefix+"players` ({auto}, player VARCHAR(255));", type);
        } catch (CoreException ex) {
            com.error(ex, "Failed to create `"+prefix+"players` table");
        }
        try {
            createIndex(connection, "CREATE INDEX `"+prefix+"names` ON `"+prefix+"players` (player);");
        } catch (CoreException ex) {
            com.error(ex, "Failed to create `"+prefix+"names` index");
        }
        try {
            /* players inventory */
            createTable(connection, "CREATE TABLE IF NOT EXISTS `"+prefix+"players_adventurer` ({auto}, player INT, health INT, foodlevel INT, exhaustion INT, saturation INT, experience INT, armor TEXT, inventory TEXT);", type);
        } catch (CoreException ex) {
            com.error(ex, "Failed to create `"+prefix+"players_adventurer` table");
        }
        try {
            createTable(connection, "CREATE TABLE IF NOT EXISTS `"+prefix+"players_survival` ({auto}, player INT, health INT, foodlevel INT, exhaustion INT, saturation INT, experience INT, armor TEXT, inventory TEXT);", type);
        } catch (CoreException ex) {
            com.error(ex, "Failed to create `"+prefix+"players_survival` table");
        }
        try {
            createTable(connection, "CREATE TABLE IF NOT EXISTS `"+prefix+"players_creative` ({auto}, player INT, armor TEXT, inventory TEXT);", type);
        } catch (CoreException ex) {
            com.error(ex, "Failed to create `"+prefix+"players_creative` table");
        }

        /* block data */
        for (World world : Bukkit.getWorlds()) {
            load(connection, world.getName(), type);
        }
        
        try {
            /* region data */
            createTable(connection, "CREATE TABLE IF NOT EXISTS `"+prefix+"regions` ({auto}, name VARCHAR(255), start VARCHAR(255), end VARCHAR(255), type VARCHAR(255));", type);
        } catch (CoreException ex) {
            com.error(ex, "Failed to create `"+prefix+"regions` table");
        }
        try {
            /* friends data */
            createTable(connection, "CREATE TABLE IF NOT EXISTS `"+prefix+"friends` ({auto}, player INT, friends TEXT);", type);
        } catch (CoreException ex) {
            com.error(ex, "[TAG] Failed to create `"+prefix+"friends` table");
        }
        try {
            /* internal data */
            createTable(connection, "CREATE TABLE IF NOT EXISTS `"+prefix+"internal` (version INT);", type);
        } catch (CoreException ex) {
            com.error(ex, "[TAG] Failed to create `"+prefix+"internal` table");
        }
    }

    public void load(Connection connection, String world, type type) {
        Communicator com = CreativeControl.getPlugin().getCommunicator();

        if (connection == null) {
            connection = this.connection;
        }
        
        if (type == null) {
            type = this.getDatabaseEngine();
        }

        try {
            createTable(connection, "CREATE TABLE IF NOT EXISTS `"+prefix+"blocks_"+world+"` (owner INT, x INT, y INT, z INT, type INT, allowed VARCHAR(255), time BIGINT);", type);
        } catch (CoreException ex) {
            com.error(ex, "Failed to create `"+prefix+"blocks_"+world+"` table");
        }
        
        try {
            /* create the index */
            createIndex(connection, "CREATE INDEX `"+prefix+"block_"+world+"` ON `"+prefix+"blocks_"+world+"` (x, z, y);");
            createIndex(connection, "CREATE INDEX `"+prefix+"type_"+world+"` ON `"+prefix+"blocks_"+world+"` (type);");
            createIndex(connection, "CREATE INDEX `"+prefix+"owner_"+world+"` ON `"+prefix+"blocks_"+world+"` (owner);");
        } catch (CoreException ex) {
            com.error(ex, "Failed to create `"+prefix+"blocks_"+world+"` index");
        }
    }
    
    public String getPlayerName(int id) {
        String ret = null;
        
        if (owners.containsValue(id)) {
            return owners.getKey(id);
        }
        
        Communicator com = CreativeControl.plugin.getCommunicator();

        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            ps = getQuery("SELECT player FROM `"+prefix+"players` WHERE id = '" + id + "' LIMIT 1;");
            rs = ps.getResultSet();
            
            if (rs.next()) {
                ret = rs.getString("player");
            }
        } catch (SQLException ex) {
            com.error(ex, "Failed to get the player data from the database");
        } catch (CoreException ex) {
            com.error(ex, "Failed to get the player data from the database");
        } finally {
            closeQuietly(rs);
        }

        owners.put(ret, id);
        return ret;
    }
    
    public int getPlayerId(String player) {
        int ret = -1;
        
        if (owners.containsKey(player)) {
            return owners.get(player);
        }

        Communicator com = CreativeControl.plugin.getCommunicator();
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            ps = getQuery("SELECT id FROM `"+prefix+"players` WHERE player = '" + player + "' LIMIT 1;");
            rs = ps.getResultSet();
            
            if (rs.next()) {
                ret = rs.getInt("id");
            }
        } catch (SQLException ex) {
            com.error(ex, "Failed to retrieve "+player+"'s id");
        } catch (CoreException ex) {
            com.error(ex, "Failed to retrieve "+player+"'s id");
        } finally {
            closeQuietly(rs);
        }
        
        if (ret == -1) {

            try {
                execute("INSERT INTO `"+prefix+"players` (player) VALUES ('"+player+"');");
            } catch (CoreException ex) {
                com.error(ex, "Failed to insert "+player+"'s id"); return -1;
            }

            return getPlayerId(player);
        }
        
        owners.put(player, ret);
        return ret;
    }
    
    public List<Integer> getAllPlayersId() {
        List<Integer> ret = new ArrayList<>();

        Communicator com = CreativeControl.plugin.getCommunicator();
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            ps = getQuery("SELECT id FROM `"+prefix+"players`;");
            rs = ps.getResultSet();

            while (rs.next()) {
                ret.add(rs.getInt("id"));
            }

        } catch (SQLException ex) {
            com.error(ex, "Failed to get player data from the database");
        } catch (CoreException ex) {
            com.error(ex, "Failed to get all players id");
        } finally {
            closeQuietly(rs);
        }

        return ret;
    }
}
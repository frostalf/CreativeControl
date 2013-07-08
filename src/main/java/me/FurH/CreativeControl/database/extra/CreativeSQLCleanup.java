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

package me.FurH.CreativeControl.database.extra;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import me.FurH.Core.exceptions.CoreException;
import me.FurH.Core.location.LocationUtils;
import me.FurH.Core.util.Communicator;
import me.FurH.CreativeControl.CreativeControl;
import me.FurH.CreativeControl.database.CreativeSQLDatabase;
import net.minecraft.server.v1_6_R1.WorldServer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_6_R1.CraftWorld;
import org.bukkit.entity.Player;

/**
 *
 * @author FurmigaHumana
 */
public class CreativeSQLCleanup implements Runnable {
    
    private CreativeControl plugin;
    public static boolean lock = false;
    private Player p;
    
    public CreativeSQLCleanup(CreativeControl plugin, Player player) {
        this.plugin = plugin;
        this.p = player;
    }
    
    @Override
    public void run() {
        if (lock) {
            System.out.println("Cleanup Locked");
            return;
        }
        
        lock = true;
        long start = System.currentTimeMillis();
        
        Communicator com = plugin.getCommunicator();
        com.msg(p, "&7Initializing... ");

        /* move blocks */
        for (World world : Bukkit.getWorlds()) {
             cleanup_blocks(world);
        }
        
        /* done */
        com.msg(p, "&7All tables cleaned in &4{0}&7 ms", (System.currentTimeMillis() - start));

        lock = false;
    }
    
    public void cleanup_blocks(World w) {
        Communicator com = plugin.getCommunicator();
        CreativeSQLDatabase db = CreativeControl.getDb();
        long blocks_start = System.currentTimeMillis();

        String table = db.prefix+"blocks_" + w.getName();

        /* move regions table */
        com.msg(p, "&7Cleaning table '&4"+table+"&7' ...");

        final WorldServer worldServer = ((CraftWorld)w).getHandle();

        double blocks_size = 0;
        try {
            blocks_size = db.getTableCount(table);
        } catch (CoreException ex) { }

        com.msg(p, "Table size: &4" + blocks_size);

        double blocks_process = 0;
        double blocks_done = 0;
        double blocks_last = 0;
        double blocks_removed = 0;

        HashSet<String> locations = new HashSet<>();
        
        while (true) {

            blocks_process = ((blocks_done / blocks_size) * 100.0D);

            int row = 0;

            if (blocks_process - blocks_last >= 5) {
                System.gc();
                com.msg(p, "&4{0}&7 of ~&4{1}&7 queries processed, &4{2}&7%", blocks_done, blocks_size, String.format("%d", (int) blocks_process));
                blocks_last = blocks_process;
            }

            try {
                PreparedStatement ps = db.getQuery("SELECT * FROM `"+table+"` LIMIT " + (int) blocks_done + ", " + 10000 + ";");
                ResultSet rs = ps.getResultSet();

                while (rs.next()) {
                    
                    boolean delete = false;
                    int x = rs.getInt("x");
                    int y = rs.getInt("y");
                    int z = rs.getInt("z");
                    int type = rs.getInt("type");
                    int id = worldServer.getTypeId(x, y, z);

                    if (type != id) {
                        com.msg(p, "&7Invalid block at X: &4" + x + "&7, Y: &4" + y + "&7, Z: &4" + z + "&7, I1: &4" + type + "&7, I2: &4" + id);
                        delete = true;
                    }
                    
                    String loc = LocationUtils.locationToString(x, y, z, w.getName());
                    
                    if (!locations.contains(loc)) {
                        locations.add(loc);
                    } else {
                        com.msg(p, "&7Duplicated block at X: &4" + x + "&7, Y: &4" + y + "&7, Z: &4" + z);
                        delete = true;
                    }

                    if (delete) {
                        db.execute("DELETE FROM `"+table+"` WHERE x = '" + x + "' AND z = '" + z + "' AND y = '" + y + "' AND time = '"+rs.getString("time")+"';");
                        blocks_removed++;
                    }

                    blocks_done++;
                    row++;
                }

                db.commit();

                rs.close();
                ps.close();

                if (row < 10000) {
                    break;
                }
                
            } catch (CoreException ex) {
                com.error(ex, "An error ocurried while cleaning the database");
                break;
            } catch (SQLException ex) {
                com.error(ex, "An error ocurried while cleaning the database");
                break;
            }
        }

        long survival_time = (System.currentTimeMillis() - blocks_start);
        com.msg(p, "&7Table '&4" + table + "&7' cleaned in &4{0}&7 ms", survival_time);
    }
}
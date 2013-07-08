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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import me.FurH.Core.exceptions.CoreException;
import me.FurH.Core.util.Communicator;
import me.FurH.CreativeControl.CreativeControl;
import me.FurH.CreativeControl.database.CreativeSQLDatabase;
import org.bukkit.entity.Player;

/**
 *
 * @author FurmigaHumana
 */
public class CreativeSQLUpdater implements Runnable {
    
    private HashSet<String> convert = new HashSet<>();
    private HashSet<String> tables = new HashSet<>();
    private CreativeControl plugin;
    public static boolean lock = false;
    private Player p;
    
    public CreativeSQLUpdater(CreativeControl plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (lock) {
            System.out.println("Updater Locked");
            return;
        }
        
        lock = true;
        long start = System.currentTimeMillis();

        Communicator com = plugin.getCommunicator();
        com.msg(p, "&7Initializing... ");
        
        CreativeSQLDatabase db = CreativeControl.getDb();

        List<String> thistables = new ArrayList<>();
        thistables.add(db.prefix + "players_survival");
        thistables.add(db.prefix + "players_creative");
        thistables.add(db.prefix + "players_adventurer");
        thistables.add(db.prefix + "friends");

        try {
            for (String table : thistables) {
                try {
                    PreparedStatement ps = db.getQuery("SELECT * FROM `"+table+"` LIMIT 1;");
                    ResultSet rs = ps.getResultSet();
                    
                    if (rs.next()) {
                        String player = rs.getString("player");
                    }
                } catch (Exception ex) {
                    db.execute("ALTER TABLE `"+table+"` RENAME TO `old_"+table+"`;");
                    convert.add(table);
                }
            }
        } catch (CoreException ex) {
            com.error(ex, "Failed to rename older tables");
        }

        try {
            db.commit();
        } catch (CoreException ex) {
            com.error(ex);
        }
        
        db.load();
        
        try {
            db.commit();
        } catch (CoreException ex) {
            com.error(ex);
        }
        
        /* update the players creative inventories table */
        update_players_creative_2();
        
        /* update the players survival inventories table */
        update_players_survival_2();
        
        /* update the players adventurer inventories table */
        update_players_adventurer_2();
        
        /* update the players friends list table */
        update_friends_2();
        
        /* update all blocks */
        update_blocks_2();
        
        try {
            db.incrementVersion(2);
        } catch (CoreException ex) {
            com.error(ex, "Failed to increment database version");
        }
        
        com.msg(p, "&7All data updated in &4{0}&7 ms", (System.currentTimeMillis() - start));

        lock = false;
    }
    
    public void update_blocks_2() {
        Communicator com = plugin.getCommunicator();
        CreativeSQLDatabase db = CreativeControl.getDb();
        long blocks_start = System.currentTimeMillis();
        
        String table = db.prefix + "blocks";
        
        /* move regions table */
        com.msg(p, "&7Updating table '&4"+table+"&7' ...");

        double blocks_size = 0;
        try {
            blocks_size = db.getTableCount(table);
        } catch (CoreException ex) { }

        com.msg(p, "&7Table size: &4" + blocks_size);

        double blocks_process = 0;
        double blocks_done = 0;
        double blocks_last = 0;

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
                    String[] location = rs.getString("location").split(":");
                    
                    String table1 = db.prefix+"blocks_"+location[0];
                    if (!tables.contains(table1)) {
                        if (!db.hasTable(table1)) {
                            db.load(db.connection, location[0], db.getDatabaseEngine());
                            db.commit();
                        }
                        tables.add(table1);
                    }
                    
                    PreparedStatement ps2 = db.prepare("INSERT INTO `"+db.prefix+"blocks_"+location[0]+"` (owner, x, y, z, type, allowed, time) VALUES (?, ?, ?, ?, ?, ?, ?);");

                    ps2.setInt(1, db.getPlayerId(rs.getString("owner")));                    
                    ps2.setInt(2, Integer.parseInt(location[1]));
                    ps2.setInt(3, Integer.parseInt(location[2]));
                    ps2.setInt(4, Integer.parseInt(location[3]));
                    ps2.setInt(5, rs.getInt("type"));
                    ps2.setString(6, rs.getString("allowed"));
                    ps2.setLong(7, rs.getLong("time"));
                    
                    ps2.execute();

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
                com.error(ex, "An error occurried while running the update method 'update_blocks_2'");
                break;
            } catch (SQLException ex) {
                com.error(ex, "An error occurried while running the update method 'update_blocks_2'");
                break;
            }
        }
        
        long blocks_time = (System.currentTimeMillis() - blocks_start);
        com.msg(p, "&7Table '&4" + table + "&7' updated in &4{0}&7 ms", blocks_time);
    }
 
    
    public void update_players_creative_2() {
        Communicator com = plugin.getCommunicator();
        CreativeSQLDatabase db = CreativeControl.getDb();
        long creative_start = System.currentTimeMillis();
        
        String table = db.prefix + "players_creative";
        if (!convert.contains(table)) {
            return;
        }
        
        /* move regions table */
        com.msg(p, "&7Updating table '&4"+table+"&7' ...");

        double creative_size = 0;
        try {
            creative_size = db.getTableCount(table);
        } catch (CoreException ex) { }

        com.msg(p, "&7Table size: &4" + creative_size);

        double creative_process = 0;
        double creative_done = 0;
        double creative_last = 0;

        while (true) {

            creative_process = ((creative_done / creative_size) * 100.0D);

            int row = 0;

            if (creative_process - creative_last >= 5) {
                System.gc();
                com.msg(p, "&4{0}&7 of ~&4{1}&7 queries processed, &4{2}&7%", creative_done, creative_size, String.format("%d", (int) creative_process));
                creative_last = creative_process;
            }

            try {
                PreparedStatement ps = db.getQuery("SELECT * FROM `old_"+table+"` LIMIT " + (int) creative_done + ", " + 10000 + ";");
                ResultSet rs = ps.getResultSet();

                while (rs.next()) {
                    PreparedStatement ps2 = db.prepare("INSERT INTO `"+table+"` (player, armor, inventory) VALUES (?, ?, ?);");

                    ps2.setInt(1, db.getPlayerId(rs.getString("player")));
                    ps2.setString(2, rs.getString("armor"));
                    ps2.setString(3, rs.getString("inventory"));
                    
                    ps2.execute();

                    creative_done++;
                    row++;
                }

                db.commit();

                rs.close();
                ps.close();

                if (row < 10000) {
                    break;
                }
            } catch (CoreException ex) {
                com.error(ex, "An error occurried while running the update method 'update_players_creative_2'");
                break;
            } catch (SQLException ex) {
                com.error(ex, "An error occurried while running the update method 'update_players_creative_2'");
                break;
            }
        }
        
        long creative_time = (System.currentTimeMillis() - creative_start);
        com.msg(p, "&7Table '&4" + table + "&7' updated in &4{0}&7 ms", creative_time);
    }
 
    public void update_players_survival_2() {
        Communicator com = plugin.getCommunicator();
        CreativeSQLDatabase db = CreativeControl.getDb();
        long survival_start = System.currentTimeMillis();
        
        String table = db.prefix + "players_survival";
        if (!convert.contains(table)) {
            return;
        }
        
        /* move regions table */
        com.msg(p, "&7Updating table '&4"+table+"&7' ...");

        double survival_size = 0;
        try {
            survival_size = db.getTableCount(table);
        } catch (CoreException ex) { }

        com.msg(p, "&7Table size: &4" + survival_size);

        double survival_process = 0;
        double survival_done = 0;
        double survival_last = 0;

        while (true) {

            survival_process = ((survival_done / survival_size) * 100.0D);

            int row = 0;

            if (survival_process - survival_last >= 5) {
                System.gc();
                com.msg(p, "&4{0}&7 of ~&4{1}&7 queries processed, &4{2}&7%", survival_done, survival_size, String.format("%d", (int) survival_process));
                survival_last = survival_process;
            }

            try {
                PreparedStatement ps = db.getQuery("SELECT * FROM `old_"+table+"` LIMIT " + (int) survival_done + ", " + 10000 + ";");
                ResultSet rs = ps.getResultSet();

                while (rs.next()) {
                    PreparedStatement ps2 = db.prepare("INSERT INTO `"+table+"` (player, health, foodlevel, exhaustion, saturation, experience, armor, inventory) VALUES (?, ?, ?, ?, ?, ?, ?, ?);");

                    ps2.setInt(1, db.getPlayerId(rs.getString("player")));
                    ps2.setInt(2, rs.getInt("health"));
                    ps2.setInt(3, rs.getInt("foodlevel"));
                    ps2.setInt(4, rs.getInt("exhaustion"));
                    ps2.setInt(5, rs.getInt("saturation"));
                    ps2.setInt(6, rs.getInt("experience"));
                    ps2.setString(7, rs.getString("armor"));
                    ps2.setString(8, rs.getString("inventory"));
                    
                    ps2.execute();

                    survival_done++;
                    row++;
                }

                db.commit();

                rs.close();
                ps.close();

                if (row < 10000) {
                    break;
                }
            } catch (CoreException ex) {
                com.error(ex, "An error occurried while running the update method 'update_players_survival_2'");
                break;
            } catch (SQLException ex) {
                com.error(ex, "An error occurried while running the update method 'update_players_survival_2'");
                break;
            }
        }
        
        long survival_time = (System.currentTimeMillis() - survival_start);
        com.msg(p, "&7Table '&4" + table + "&7' updated in &4{0}&7 ms", survival_time);
    }

    public void update_players_adventurer_2() {
        Communicator com = plugin.getCommunicator();
        CreativeSQLDatabase db = CreativeControl.getDb();
        long adventurer_start = System.currentTimeMillis();
        
        String table = db.prefix + "players_adventurer";
        if (!convert.contains(table)) {
            return;
        }
        
        /* move regions table */
        com.msg(p, "&7Updating table '&4"+table+"&7' ...");

        double adventurer_size = 0;
        try {
            adventurer_size = db.getTableCount(table);
        } catch (CoreException ex) { }

        com.msg(p, "&7Table size: &4" + adventurer_size);

        double adventurer_process = 0;
        double adventurer_done = 0;
        double adventurer_last = 0;

        while (true) {

            adventurer_process = ((adventurer_done / adventurer_size) * 100.0D);

            int row = 0;

            if (adventurer_process - adventurer_last >= 5) {
                System.gc();
                com.msg(p, "&4{0}&7 of ~&4{1}&7 queries processed, &4{2}&7%", adventurer_done, adventurer_size, String.format("%d", (int) adventurer_process));
                adventurer_last = adventurer_process;
            }

            try {
                PreparedStatement ps = db.getQuery("SELECT * FROM `old_"+table+"` LIMIT " + (int) adventurer_done + ", " + 10000 + ";");
                ResultSet rs = ps.getResultSet();

                while (rs.next()) {
                    PreparedStatement ps2 = db.prepare("INSERT INTO `"+table+"` (player, health, foodlevel, exhaustion, saturation, experience, armor, inventory) VALUES (?, ?, ?, ?, ?, ?, ?, ?);");

                    ps2.setInt(1, db.getPlayerId(rs.getString("player")));
                    ps2.setInt(2, rs.getInt("health"));
                    ps2.setInt(3, rs.getInt("foodlevel"));
                    ps2.setInt(4, rs.getInt("exhaustion"));
                    ps2.setInt(5, rs.getInt("saturation"));
                    ps2.setInt(6, rs.getInt("experience"));
                    ps2.setString(7, rs.getString("armor"));
                    ps2.setString(8, rs.getString("inventory"));
                    
                    ps2.execute();

                    adventurer_done++;
                    row++;
                }

                db.commit();

                rs.close();
                ps.close();

                if (row < 10000) {
                    break;
                }
            } catch (CoreException ex) {
                com.error(ex, "An error occurried while running the update method 'update_players_adventurer_2'");
                break;
            } catch (SQLException ex) {
                com.error(ex, "An error occurried while running the update method 'update_players_adventurer_2'");
                break;
            }
        }
        
        long adventurer_time = (System.currentTimeMillis() - adventurer_start);
        com.msg(p, "&7Table '&4" + table + "&7' updated in &4{0}&7 ms", adventurer_time);
    }
    
    public void update_friends_2() {
        Communicator com = plugin.getCommunicator();
        CreativeSQLDatabase db = CreativeControl.getDb();
        long friends_start = System.currentTimeMillis();
        
        String table = db.prefix + "friends";
        if (!convert.contains(table)) {
            return;
        }
        
        /* move regions table */
        com.msg(p, "&7Updating table '&4"+table+"&7' ...");

        double friends_size = 0;
        try {
            friends_size = db.getTableCount(table);
        } catch (CoreException ex) { }

        com.msg(p, "&7Table size: &4" + friends_size);

        double friends_process = 0;
        double friends_done = 0;
        double friends_last = 0;

        while (true) {

            friends_process = ((friends_done / friends_size) * 100.0D);

            int row = 0;

            if (friends_process - friends_last >= 5) {
                System.gc();
                com.msg(p, "&4{0}&7 of ~&4{1}&7 queries processed, &4{2}&7%", friends_done, friends_size, String.format("%d", (int) friends_process));
                friends_last = friends_process;
            }

            try {
                PreparedStatement ps = db.getQuery("SELECT * FROM `old_"+table+"` LIMIT " + (int) friends_done + ", " + 10000 + ";");
                ResultSet rs = ps.getResultSet();

                while (rs.next()) {
                    PreparedStatement ps2 = db.prepare("INSERT INTO `"+table+"` (player, friends) VALUES (?, ?);");

                    ps2.setInt(1, db.getPlayerId(rs.getString("player")));
                    ps2.setString(2, rs.getString("friends"));
                    
                    ps2.execute();

                    friends_done++;
                    row++;
                }

                db.commit();

                rs.close();
                ps.close();

                if (row < 10000) {
                    break;
                }
            } catch (CoreException ex) {
                com.error(ex, "An error occurried while running the update method 'update_friends_2'");
                break;
            } catch (SQLException ex) {
                com.error(ex, "An error occurried while running the update method 'update_friends_2'");
                break;
            }
        }
        
        long friends_time = (System.currentTimeMillis() - friends_start);
        com.msg(p, "&7Table '&4" + table + "&7' updated in &4{0}&7 ms", friends_time);
    }
}
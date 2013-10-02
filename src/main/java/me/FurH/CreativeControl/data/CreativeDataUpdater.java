package me.FurH.CreativeControl.data;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import me.FurH.Core.exceptions.CoreException;
import me.FurH.Core.inventory.InvUtils;
import me.FurH.Core.inventory.InventoryStack;
import me.FurH.Core.util.Communicator;
import me.FurH.CreativeControl.CreativeControl;
import me.FurH.CreativeControl.database.CreativeSQLDatabase;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 *
 * @author FurmigaHumana
 * All Rights Reserved unless otherwise explicitly stated.
 */
public class CreativeDataUpdater {

    public static boolean lock = false;
    private CreativeControl plugin;
    private Player p;
    
    public CreativeDataUpdater(CreativeControl plugin) {
        this.plugin = plugin;
    }
    
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

        db.load();

        try {
            db.commit();
        } catch (CoreException ex) {
            com.error(ex);
        }
        
        List<String> tables = new ArrayList<>();
        
        tables.add(db.prefix + "players_adventurer");
        tables.add(db.prefix + "players_survival");
        tables.add(db.prefix + "players_creative");

        /* update the players inventories tables */
        for (String table : tables) {
            update_players_table_3(table);
        }

        try {
            db.incrementVersion(3);
        } catch (CoreException ex) {
            com.error(ex, "Failed to increment the database version");
        }
        
        com.msg(p, "&7All data updated in &4{0}&7 ms", (System.currentTimeMillis() - start));

        lock = false;
    }
    
    public void update_players_table_3(String table) {
        Communicator com = plugin.getCommunicator();
        
        CreativePlayerData data = CreativeControl.getPlayerData();
        CreativeSQLDatabase db = CreativeControl.getDb();
        long adventurer_start = System.currentTimeMillis();

        /* move table */
        com.msg(p, "&7Updating table '&4"+table+"&7' ...");

        double table_size = 0;
        try {
            table_size = db.getTableCount(table);
        } catch (CoreException ex) { }

        com.msg(p, "&7Table size: &4" + table_size);

        double table_processed = 0;
        double table_done = 0;
        double table_last = 0;

        while (true) {

            table_processed = ((table_done / table_size) * 100.0D);

            int row = 0;

            if (table_processed - table_last >= 5) {
                System.gc();
                com.msg(p, "&4{0}&7 of ~&4{1}&7 queries processed, &4{2}&7%", table_done, table_size, String.format("%d", (int) table_processed));
                table_last = table_processed;
            }

            try {
                PreparedStatement ps = db.getQuery("SELECT * FROM `"+table+"` LIMIT " + (int) table_done + ", " + 10000 + ";");
                ResultSet rs = ps.getResultSet();

                while (rs.next()) {
                    
                    int id = rs.getInt("player");

                    ItemStack[] armor = toArrayStack(rs.getString("armor"));
                    ItemStack[] contents = toArrayStack(rs.getString("inventory"));
                    
                    db.execute("UPDATE `"+table+"` SET armor = '"+InventoryStack.getStringFromArray(armor)+"', inventory = '"+InventoryStack.getStringFromArray(contents)+"' WHERE player = '"+id+"';");

                    table_done++;
                    row++;
                }

                db.commit();

                rs.close();
                ps.close();

                if (row < 10000) {
                    break;
                }
            } catch (CoreException ex) {
                com.error(ex, "An error occurried while running the update method 'update_players_table_3'");
                break;
            } catch (SQLException ex) {
                com.error(ex, "An error occurried while running the update method 'update_players_table_3'");
                break;
            }
        }
        
        long table_time = (System.currentTimeMillis() - adventurer_start);
        com.msg(p, "&7Table '&4" + table + "&7' updated in &4{0}&7 ms", table_time);
    }
    
    public ItemStack[] toArrayStack(String string) {
        return InvUtils.toArrayStack(string);
    }
}
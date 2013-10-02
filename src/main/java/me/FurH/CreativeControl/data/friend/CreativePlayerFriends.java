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

package me.FurH.CreativeControl.data.friend;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import me.FurH.Core.cache.CoreSafeCache;
import me.FurH.Core.exceptions.CoreException;
import me.FurH.CreativeControl.CreativeControl;
import me.FurH.CreativeControl.database.CreativeSQLDatabase;
import me.FurH.CreativeControl.util.CreativeUtil;
import org.bukkit.entity.Player;

/**
 *
 * @author FurmigaHumana
 */
public class CreativePlayerFriends {
    
    private CoreSafeCache<String, HashSet<String>> hascache = new CoreSafeCache<>(true);
    
    public void uncache(Player p) {
        hascache.remove(p.getName().toLowerCase());
    }
    
    public void clear() {
        hascache.clear();
    }
    
    public void saveFriends(String player, HashSet<String> friends) {
        CreativeSQLDatabase db = CreativeControl.getDb();
        
        hascache.put(player, friends);

        List<String> newFriends = new ArrayList<>(friends);
        String query = "UPDATE `"+db.prefix+"friends` SET friends = '"+newFriends+"' WHERE player = '"+db.getPlayerId(player.toLowerCase())+"'";

        try {
            db.execute(query);
        } catch (CoreException ex) {
            CreativeControl.plugin.getCommunicator().error(ex, "Failed to save "+player+"'s friends to the database");
        }

        newFriends.clear();
    }
    
    public HashSet<String> getFriends(String player) {
        HashSet<String> friends = hascache.get(player);
        
        CreativeSQLDatabase db = CreativeControl.getDb();

        if (friends == null) {
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                ps = db.getQuery("SELECT * FROM `"+db.prefix+"friends` WHERE player = '" + db.getPlayerId(player.toLowerCase()) + "'");
                rs = ps.getResultSet();
                
                if (rs.next()) {
                    friends = CreativeUtil.toStringHashSet(rs.getString("friends"), ", ");
                } else {
                    friends = new HashSet<>();
                    db.execute("INSERT INTO `"+db.prefix+"friends` (player, friends) VALUES ('"+ db.getPlayerId(player.toLowerCase()) +"', '[]');");
                }

                hascache.put(player, friends);
            } catch (SQLException ex) {
                CreativeControl.plugin.getCommunicator().error(ex, "Failed to get the data from the database");
            } catch (CoreException ex) {
                CreativeControl.plugin.getCommunicator().error(ex, "Failed to get the data from the database");
            } finally {
                if (rs != null) {
                    try {
                        rs.close();
                    } catch (SQLException ex) { }
                }
            }
        }
        
        return friends;
    }
}

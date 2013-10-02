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

package me.FurH.CreativeControl.data;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import me.FurH.Core.cache.CoreSafeCache;
import me.FurH.Core.exceptions.CoreException;
import me.FurH.Core.file.FileUtils;
import me.FurH.Core.inventory.InventoryStack;
import me.FurH.CreativeControl.CreativeControl;
import me.FurH.CreativeControl.configuration.CreativeMainConfig;
import me.FurH.CreativeControl.database.CreativeSQLDatabase;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 *
 * @author FurmigaHumana
 */
public class CreativePlayerData {

    public CoreSafeCache<String, CreativePlayerCache> adventurer_cache = new CoreSafeCache<>(true);
    public CoreSafeCache<String, CreativePlayerCache> creative_cache = new CoreSafeCache<>(true);
    public CoreSafeCache<String, CreativePlayerCache> survival_cache = new CoreSafeCache<>(true);

    public void clear() {
        adventurer_cache.clear();
        creative_cache.clear();
        survival_cache.clear();
    }

    public void clear(String player) {
        adventurer_cache.remove(player);
        creative_cache.remove(player);
        survival_cache.remove(player);
    }
    
    public boolean process(Player player, GameMode newgm, GameMode oldgm) throws CoreException {

        if (save(player, oldgm)) {
            return restore(player, newgm);
        }

        return false;
    }
    
    public boolean save(Player p, GameMode gm) throws CoreException {
        CreativeSQLDatabase db = CreativeControl.getDb();
        if (gm.equals(GameMode.ADVENTURE)) {
            CreativePlayerCache cache = hasAdv(p.getName());
            
            if (cache == null) {
                cache = new CreativePlayerCache(); cache.name = p.getName().toLowerCase();

                cache = newCache(p, cache);
                adventurer_cache.put(cache.name, cache);

                String query = "INSERT INTO `"+db.prefix+"players_adventurer` (player, health, foodlevel, exhaustion, saturation, experience, armor, inventory) VALUES "
                        + "('"+db.getPlayerId(cache.name)+"', '"+cache.health+"', '"+cache.food+"', '"+cache.ex+"', '"+cache.sat+"', '" + cache.exp +"', '"+ toArrayString(cache.armor) +"', '"+ toArrayString(cache.items) +"');";

                try {
                    db.execute(query);
                } catch (CoreException ex) {
                    throw new CoreException(ex, "Failed to save " + p.getName() + " adventurer data");
                }

                return true;
            } else {
                cache = newCache(p, cache);
                
                adventurer_cache.remove(cache.name);
                adventurer_cache.put(cache.name, cache);
                
                String query = "UPDATE `"+db.prefix+"players_adventurer` SET health = '"+cache.health+"', foodlevel = '"+cache.food+"', exhaustion = '"+cache.ex+"', "
                        + "saturation = '"+cache.sat+"', experience = '"+cache.exp+"', armor = '"+toArrayString(cache.armor)+"', inventory = '"+ toArrayString(cache.items) +"' WHERE id = '"+cache.id+"'";

                try {
                    db.execute(query);
                } catch (CoreException ex) {
                    throw new CoreException(ex, "Failed to save " + p.getName() + " adventurer data");
                }
                
                return true;
            }
        } else 
        if (gm.equals(GameMode.CREATIVE)) {
            CreativePlayerCache cache = hasCre(p.getName());
            
            if (cache == null) {
                cache = new CreativePlayerCache(); cache.name = p.getName().toLowerCase();
                
                cache = newCache(p, cache);
                creative_cache.put(cache.name, cache);
                
                String query = "INSERT INTO `"+db.prefix+"players_creative` (player, armor, inventory) VALUES "
                        + "('"+db.getPlayerId(cache.name)+"', '"+ toArrayString(cache.armor) +"', '"+ toArrayString(cache.items) +"');";

                try {
                    db.execute(query);
                } catch (CoreException ex) {
                    throw new CoreException(ex, "Failed to save " + p.getName() + " creative data");
                }

                return true;
            } else {
                cache = newCache(p, cache);
                
                creative_cache.remove(cache.name);
                creative_cache.put(cache.name, cache);
                
                String query = "UPDATE `"+db.prefix+"players_creative` SET armor = '"+toArrayString(cache.armor)+"', inventory = '"+ toArrayString(cache.items) +"' WHERE id = '"+cache.id+"'";

                try {
                    db.execute(query);
                } catch (CoreException ex) {
                    throw new CoreException(ex, "Failed to save " + p.getName() + " creative data");
                }

                return true;
            }
        } else 
        if (gm.equals(GameMode.SURVIVAL)) {
            CreativePlayerCache cache = hasSur(p.getName());
            
            if (cache == null) {
                cache = new CreativePlayerCache(); cache.name = p.getName().toLowerCase();
                
                cache = newCache(p, cache);
                survival_cache.put(cache.name, cache);
                
                String query = "INSERT INTO `"+db.prefix+"players_survival` (player, health, foodlevel, exhaustion, saturation, experience, armor, inventory) VALUES "
                        + "('"+db.getPlayerId(cache.name)+"', '"+cache.health+"', '"+cache.food+"', '"+cache.ex+"', '"+cache.sat+"', '" + cache.exp +"', '"+ toArrayString(cache.armor) +"', '"+ toArrayString(cache.items) +"');";

                try {
                    db.execute(query);
                } catch (CoreException ex) {
                    throw new CoreException(ex, "Failed to save " + p.getName() + " survival data");
                }
                
                return true;
            } else {
                cache = newCache(p, cache);
                
                survival_cache.remove(cache.name);
                survival_cache.put(cache.name, cache);
                
                String query = "UPDATE `"+db.prefix+"players_survival` SET health = '"+cache.health+"', foodlevel = '"+cache.food+"', exhaustion = '"+cache.ex+"', "
                        + "saturation = '"+cache.sat+"', experience = '"+cache.exp+"', armor = '"+toArrayString(cache.armor)+"', inventory = '"+ toArrayString(cache.items) +"' WHERE id = '"+cache.id+"'";
                
                try {
                    db.execute(query);
                } catch (CoreException ex) {
                    throw new CoreException(ex, "Failed to save " + p.getName() + " survival data");
                }
                
                return true;
            }
        }
        return false;
    }
    
    public CreativePlayerCache newCache(Player p, CreativePlayerCache cache) {
        cache.armor = p.getInventory().getArmorContents();
        cache.health = p.getHealth();
        cache.food = p.getFoodLevel();
        cache.ex = p.getExhaustion();
        cache.exp = p.getTotalExperience();
        cache.sat = p.getSaturation();
        cache.items = p.getInventory().getContents();
        return cache;
    }

    public boolean restore(Player p, GameMode gm) throws CoreException {        

        if (gm.equals(GameMode.ADVENTURE)) {
            
            CreativePlayerCache cache = hasAdv(p.getName());
            return restore(p, cache);
            
        } else
        if (gm.equals(GameMode.CREATIVE)) {
            CreativePlayerCache cache = hasCre(p.getName());

            ItemStack[] armor = setCreativeArmor(p);
            if (armor != null && cache != null) {
                cache.armor = armor;
            }

            return restore(p, cache);
        } else
        if (gm.equals(GameMode.SURVIVAL)) {

            CreativePlayerCache cache = hasSur(p.getName());
            return restore(p, cache);

        }
        
        return false;
    }
    
    private ItemStack[] setCreativeArmor(Player p) {
        
        CreativeMainConfig config = CreativeControl.getMainConfig();

        if (config.armor_helmet != null && config.armor_helmet.getType() != Material.AIR) {
            p.getInventory().setHelmet(config.armor_helmet);
        }

        if (config.armor_chest != null && config.armor_chest.getType() != Material.AIR) {
            p.getInventory().setChestplate(config.armor_chest);
        }

        if (config.armor_leggs != null && config.armor_leggs.getType() != Material.AIR) {
            p.getInventory().setLeggings(config.armor_leggs);
        }

        if (config.armor_boots != null && config.armor_boots.getType() != Material.AIR) {
            p.getInventory().setBoots(config.armor_boots);
        }
        
        return p.getInventory().getArmorContents();
    }
    
    public CreativePlayerCache hasAdv(String player) throws CoreException {

        CreativePlayerCache cache = adventurer_cache.get(player.toLowerCase());
        CreativeSQLDatabase db = CreativeControl.getDb();

        if (cache == null) {

            PreparedStatement ps = null;
            ResultSet rs = null;

            try {

                ps = db.getRawQuery("SELECT * FROM `"+db.prefix+"players_adventurer` WHERE player = '" + db.getPlayerId(player.toLowerCase()) + "'");
                rs = ps.getResultSet();

                if (rs.next()) {
                    cache = new CreativePlayerCache();
                    cache.id = rs.getInt("id");
                    cache.name = rs.getString("player");
                    cache.health = rs.getDouble("health");
                    cache.food = rs.getInt("foodlevel");
                    cache.ex = rs.getShort("exhaustion");
                    cache.sat = rs.getShort("saturation");
                    cache.exp = rs.getInt("experience");
                    cache.armor = toArrayStack(rs.getString("armor"));
                    cache.items = toArrayStack(rs.getString("inventory"));
                    adventurer_cache.put(cache.name, cache);
                }
                
            } catch (Throwable ex) {
                throw new CoreException(ex, "Failed to get " + player + "'s adventurer data");
            } finally {
                FileUtils.closeQuietly(rs);
            }
        }
        
        return cache;
    }
    
    public CreativePlayerCache hasSur(String player) throws CoreException {
        
        CreativePlayerCache cache = survival_cache.get(player.toLowerCase());
        CreativeSQLDatabase db = CreativeControl.getDb();

        if (cache == null) {

            PreparedStatement ps = null;
            ResultSet rs = null;

            try {
                
                ps = db.getRawQuery("SELECT * FROM `"+db.prefix+"players_survival` WHERE player = '" + db.getPlayerId(player.toLowerCase()) + "'");
                rs = ps.getResultSet();

                if (rs.next()) {
                    cache = new CreativePlayerCache();
                    cache.id = rs.getInt("id");
                    cache.name = rs.getString("player");
                    cache.health = rs.getDouble("health");
                    cache.food = rs.getInt("foodlevel");
                    cache.ex = rs.getShort("exhaustion");
                    cache.sat = rs.getShort("saturation");
                    cache.exp = rs.getInt("experience");
                    cache.armor = toArrayStack(rs.getString("armor"));
                    cache.items = toArrayStack(rs.getString("inventory"));
                    survival_cache.put(cache.name, cache);
                }

            } catch (Throwable ex) {
                throw new CoreException(ex, "Failed to get " + player + "'s survival data");
            } finally {
                FileUtils.closeQuietly(rs);
            }
        }
        
        return cache;
    }
    
    public CreativePlayerCache hasCre(String player) throws CoreException {
        
        CreativePlayerCache cache = creative_cache.get(player.toLowerCase());
        CreativeSQLDatabase db = CreativeControl.getDb();

        if (cache == null) {

            PreparedStatement ps = null;
            ResultSet rs = null;

            try {

                ps = db.getRawQuery("SELECT * FROM `"+db.prefix+"players_creative` WHERE player = '" + db.getPlayerId(player.toLowerCase()) + "'");
                rs = ps.getResultSet();

                if (rs.next()) {
                    cache = new CreativePlayerCache();
                    cache.id = rs.getInt("id");
                    cache.name = rs.getString("player");
                    cache.armor = toArrayStack(rs.getString("armor"));
                    cache.items = toArrayStack(rs.getString("inventory"));
                    creative_cache.put(cache.name, cache);
                }

            } catch (Throwable ex) {
                throw new CoreException(ex, "Failed to get " + player + "'s creative data");
            } finally {
                FileUtils.closeQuietly(rs);
            }
        }

        return cache;
    }
    
    private boolean restore(Player p, CreativePlayerCache cache) {
        if (cache == null) { cache = new CreativePlayerCache(); }
        
        p.getInventory().setArmorContents(cache.armor);
        
        CreativeMainConfig    config   = CreativeControl.getMainConfig();
        if (config.data_status) {

            double health = cache.health;
            
            if (health <= 0) { health = 20; }
            if (health > 20) { health = 20; }

            p.setHealth(health);

            p.setFoodLevel(cache.food);
            p.setExhaustion(cache.ex);
            p.setSaturation(cache.sat);
        }

        p.getInventory().setContents(cache.items);
        
        p.updateInventory();
        
        return true;
    }

    private String toArrayString(ItemStack[] armor) {
        
        try {
            return InventoryStack.getStringFromArray(armor);
        } catch (CoreException ex) {
            CreativeControl.getPlugin().error(ex, "Failed to parse ItemStack array into a string");
        }
        
        return "";
    }

    public ItemStack[] toArrayStack(String string) {
        
        try {
            return InventoryStack.getArrayFromString(string);
        } catch (CoreException ex) {
            CreativeControl.getPlugin().error(ex, "Failed to parse '"+string+"' into an ItemStack array");
        }
        
        return new ItemStack[] { new ItemStack(Material.AIR, 1) };
    }
}
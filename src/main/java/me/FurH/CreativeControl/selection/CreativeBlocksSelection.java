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

package me.FurH.CreativeControl.selection;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.Selection;
import java.util.HashSet;
import me.FurH.Core.util.Communicator;
import me.FurH.CreativeControl.CreativeControl;
import me.FurH.CreativeControl.configuration.CreativeMainConfig;
import me.FurH.CreativeControl.configuration.CreativeMessages;
import me.FurH.CreativeControl.configuration.CreativeWorldNodes;
import me.FurH.CreativeControl.manager.CreativeBlockData;
import me.FurH.CreativeControl.manager.CreativeBlockManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 *
 * @author FurmigaHumana
 */
public class CreativeBlocksSelection {
    
    private long elapsedTime = 0;
    private Location min = null;
    private Location max = null;

    public enum Type {
        DELALL, DELPLAYER, DELTYPE, ADD, ALLOW, TRANSFER;
    };

    public boolean allBlocks(CommandSender sender, final String args, final Type type) {
        
        final CreativeControl      plugin   = CreativeControl.getPlugin();
        final CreativeBlockManager manager  = CreativeControl.getManager();
        final Communicator         com      = plugin.getCommunicator();
        final CreativeMessages     messages = CreativeControl.getMessages();
        final CreativeMainConfig   main     = CreativeControl.getMainConfig();
        
        if (!plugin.hasPerm(sender, "Commands.Use.others")) {
            if ((!args.equalsIgnoreCase(sender.getName()))) {
                com.msg(sender, "&4You dont have permission to use this command!");
                return true;
            }
        }
        
        int area = 0;
        if (!main.selection_usewe || getSelection((Player)sender) == null) {
            if (!plugin.left.containsKey((Player)sender) && !plugin.right.containsKey((Player)sender)) {
                com.msg(sender, "&4You must select the area first!");
                return true;
            }
        
            final CreativeSelection sel = new CreativeSelection(plugin.left.get((Player)sender), plugin.right.get((Player)sender));
            
            area = sel.getArea();
            min = sel.getStart();
            max = sel.getEnd();
        } else {
            Selection sel = getSelection((Player)sender);
            
            if (sel == null) {
                com.msg(sender, "&4You must select the area first!");
                return true;
            }
            
            area = sel.getArea();
            min = sel.getMinimumPoint();
            max = sel.getMaximumPoint();
        }

        com.msg(sender, "&4{0}&7 blocks selected!", area);
        com.msg(sender, "&7This may take a while...");

        final long startTimer = System.currentTimeMillis();
        final Player player = (Player) sender;
        final World w = min.getWorld();

        Thread t = new Thread() {
            @Override
            public void run() {
                for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
                    for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                        for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {

                            int id = w.getBlockTypeIdAt(x, y, z);
                            if (id == 0) { continue; }

                            CreativeWorldNodes wconfig = CreativeControl.getWorldNodes(w);

                            if (type == Type.DELALL) {
                                if (wconfig.block_ownblock) {
                                    CreativeBlockData data = manager.isprotected(w, x, y, z, id, true);
                                    if (data != null) {
                                        if (data.owner.equalsIgnoreCase(args)) {
                                            manager.unprotect(w, x, y, z, id);
                                        }
                                    }
                                } else
                                if (wconfig.block_nodrop) {
                                    if (plugin.hasPerm(player, "Commands.NoDrop")) {
                                        manager.unprotect(w, x, y, z, id);
                                    }
                                }
                            } else
                            if (type == Type.DELPLAYER) {
                                if (wconfig.block_ownblock) {
                                    if (args.equalsIgnoreCase(player.getName())) {
                                        delPlayer(args, w, x, y, z, id);
                                    } else if (plugin.hasPerm(player, "OwnBlock.DelPlayer")) {
                                        delPlayer(args, w, x, y, z, id);
                                    }
                                } else
                                if (wconfig.block_nodrop) {
                                    if (plugin.hasPerm(player, "Commands.NoDrop")) {
                                        delPlayer(args, w, x, y, z, id);
                                    }
                                }
                            } else
                            if (type == Type.DELTYPE) {
                                if (wconfig.block_ownblock) {
                                    CreativeBlockData data = manager.isprotected(w, x, y, z, id, true);
                                    if (data != null) {
                                        if (data.owner.equalsIgnoreCase(player.getName())) {
                                            delType(args, w, x, y, z, id);
                                        }
                                    }
                                } else
                                if (wconfig.block_nodrop) {
                                    if (plugin.hasPerm(player, "Commands.NoDrop")) {
                                        delType(args, w, x, y, z, id);
                                    }
                                }
                            } else
                            if (type == Type.ADD) {
                                CreativeBlockData data = manager.isprotected(w, x, y, z, id, true);
                                if (data == null) {
                                    manager.protect(args, w, x, y, z, id);
                                }
                            } else
                            if (type == Type.ALLOW) {
                                CreativeBlockData data = manager.isprotected(w, x, y, z, id, false);
                                if (data != null) {
                                    if (data.owner.equalsIgnoreCase(player.getName())) {
                                        String mod = args.toLowerCase();
                                        HashSet<String> als = new HashSet<>();

                                        if (data.allowed != null) {
                                            als = data.allowed;
                                        }

                                        if (mod.startsWith("-")) {
                                            mod = mod.substring(1);
                                            if (als.contains(mod)) {
                                                als.remove(mod);
                                            }
                                        } else {
                                            if (!als.contains(mod)) {
                                                als.add(mod);
                                            }
                                        }

                                        manager.update(data, w, x, y, z);
                                    }
                                }
                            } else
                            if (type == Type.TRANSFER) {
                                CreativeBlockData data = manager.isprotected(w, x, y, z, id, true);
                                if (data != null) {
                                    if (data.owner.equalsIgnoreCase(player.getName())) {
                                        data.owner = args;
                                        manager.update(data, w, x, y, z);
                                    }
                                }
                            }
                        }
                    }
                }

                elapsedTime = (System.currentTimeMillis() - startTimer);
                com.msg(player, "&7All blocks processed in &4{0}&7 ms", elapsedTime);
            }
        };
        t.setDaemon(true);
        t.setName("CreativeControl Selection Thread");
        t.setPriority(1);
        t.start();
        
        return true;
    }

    public void delPlayer(String args, World world, int x, int y, int z, int type) {
        CreativeBlockManager manager = CreativeControl.getManager();
        CreativeBlockData data = manager.isprotected(world, x, y, z, type, true);

        if (data != null) {
            if (data.owner.equalsIgnoreCase(args)) {
                manager.unprotect(world, x, y, z, type);
            }
        }
    }

    public void delType(String args, World world, int x, int y, int z, int id) {
        CreativeBlockManager manager = CreativeControl.getManager();
        try {
            int type = Integer.parseInt(args);
            if (id == type) {
                manager.unprotect(world, x, y, z, id);
            }
        } catch (Exception ex) {
            Communicator com        = CreativeControl.plugin.getCommunicator();
            com.error(ex, "[TAG] {0} is not a valid number!", args);
        }
    }
    
    public Selection getSelection(Player p) {
        CreativeControl plugin = CreativeControl.getPlugin();
        WorldEditPlugin we = plugin.getWorldEdit();
        
        if (we != null) {
            return plugin.getWorldEdit().getSelection(p);
        }
        
        return null;
    }
}
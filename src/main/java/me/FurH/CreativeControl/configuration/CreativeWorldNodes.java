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

package me.FurH.CreativeControl.configuration;

import java.util.HashSet;
import me.FurH.CreativeControl.stack.CreativeItemStack;
import org.bukkit.GameMode;

/**
 *
 * @author FurmigaHumana
 */
public class CreativeWorldNodes {
    public GameMode world_gamemode        = GameMode.SURVIVAL;
    public boolean  world_exclude         = false;
    public boolean  world_changegm        = false;
    
    public HashSet<String> black_cmds       = new HashSet<>();//Arrays.asList(new String[] { "/buy", "/sell", "/logout" });
    public HashSet<String> black_s_cmds     = new HashSet<>();
    
    public HashSet<CreativeItemStack> black_place     = new HashSet<>();//Arrays.asList(new Integer[] { 8, 9, 10, 11, 46, 51, 52, 79 });
    public HashSet<CreativeItemStack> black_break     = new HashSet<>();//Arrays.asList(new Integer[] { });
    public HashSet<CreativeItemStack> black_use       = new HashSet<>();//Arrays.asList(new Integer[] { 259, 326, 327, 384, 385 });
    public HashSet<CreativeItemStack> black_interact  = new HashSet<>();//Arrays.asList(new Integer[] { 23, 54, 58, 61, 62, 63, 68, 84, 116, 117, 130, 138, 145 });
    public HashSet<CreativeItemStack> black_inventory = new HashSet<>();//Arrays.asList(new Integer[] { });
    
    public HashSet<String> black_sign       = new HashSet<>();//Arrays.asList(new String[] { "buy", "sell", "trade" });
    public boolean black_sign_all        = false;
    
    public boolean misc_tnt              = false;
    public boolean misc_ice              = false;
    public boolean misc_liquid           = false;
    public boolean misc_fire             = false;
    
    public boolean block_water           = false;
    public boolean block_worledit        = false;
    public boolean block_ownblock        = false;
    public boolean block_nodrop          = true;
    public boolean block_explosion       = false;
    public boolean block_creative        = false;
    public boolean block_pistons         = false;
    public boolean block_physics         = false;
    public boolean block_against         = false;
    public boolean block_attach          = false;
    public boolean block_invert          = false;
    public HashSet<CreativeItemStack> block_exclude   = new HashSet<>();//Arrays.asList(new Integer[] { 0, 8, 9, 10, 11 });
    
    public boolean prevent_drop          = true;
    public boolean prevent_pickup        = true;
    public boolean prevent_pvp           = true;
    public boolean prevent_mobs          = false;
    public boolean prevent_eggs          = true;
    public boolean prevent_target        = false;
    public boolean prevent_mobsdrop      = true;
    public boolean prevent_irongolem     = true;
    public boolean prevent_snowgolem     = true;
    public boolean prevent_wither        = true;
    public boolean prevent_drops         = true;
    public boolean prevent_enchant       = true;
    public boolean prevent_mcstore       = true;
    public boolean prevent_bedrock       = true;
    public boolean prevent_invinteract   = true;
    public boolean prevent_bonemeal      = true;
    public boolean prevent_villager      = true;
    public boolean prevent_potion        = true;
    public boolean prevent_frame         = true;
    public boolean prevent_vehicle       = true;
    public int prevent_limitvechile      = -1;
    public int prevent_stacklimit        = 1;
    public boolean prevent_open          = false;
    public boolean prevent_fly           = false;
    public boolean prevent_creative      = false;
}
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

import me.FurH.Core.CorePlugin;
import me.FurH.Core.cache.CoreSafeCache;
import me.FurH.Core.configuration.Configuration;
import me.FurH.CreativeControl.CreativeControl;
import me.FurH.CreativeControl.blacklist.CreativeBlackList;
import org.bukkit.GameMode;
import org.bukkit.World;

/**
 *
 * @author FurmigaHumana
 */
public class CreativeWorldConfig extends Configuration {
    private CoreSafeCache<String, CreativeWorldNodes> config_cache = new CoreSafeCache<>();
    private CreativeWorldNodes nodes = new CreativeWorldNodes();

    public CreativeWorldConfig(CorePlugin plugin) {
        super(plugin);
    }

    public void clear() {
        config_cache.clear();
    }

    public CreativeWorldNodes get(World w) {

        CreativeMainConfig   main   = CreativeControl.getMainConfig();
        if (main.config_single) {
            return nodes;
        } else {
            CreativeWorldNodes n = config_cache.get(w.getName());
            if (n == null) { load(w); }
            return n;
        }
    }

    public void load(World w) {
        CreativeMainConfig   main   = CreativeControl.getMainConfig();
        
        CreativeBlackList blacklist = CreativeControl.getBlackList();
        CreativeWorldNodes x = new CreativeWorldNodes();

        String gamemode         = getString(w, "World.GameMode");
        if (gamemode.equalsIgnoreCase("CREATIVE")) {
            x.world_gamemode        = GameMode.CREATIVE;
        } else
        if (gamemode.equalsIgnoreCase("ADVENTURE")) {
            x.world_gamemode        = GameMode.ADVENTURE;
        } else {
            x.world_gamemode        = GameMode.SURVIVAL;
        }
        
        x.world_exclude         = getBoolean(w, "World.Exclude");
        x.world_changegm        = getBoolean(w, "World.ChangeGameMode");

        x.black_cmds            = getStringAsStringSet(w, "BlackList.Commands");
        x.black_s_cmds          = getStringAsStringSet(w, "BlackList.SurvivalCommands");
        
        x.black_place           = blacklist.buildHashSet(getStringAsStringSet(w, "BlackList.BlockPlace"));
        x.black_break           = blacklist.buildHashSet(getStringAsStringSet(w, "BlackList.BlockBreak"));
        x.black_use             = blacklist.buildHashSet(getStringAsStringSet(w, "BlackList.ItemUse"));
        x.black_interact        = blacklist.buildHashSet(getStringAsStringSet(w, "BlackList.ItemInteract"));
        x.black_inventory       = blacklist.buildHashSet(getStringAsStringSet(w, "BlackList.Inventory"));
        
        x.black_sign            = getStringAsStringSet(w, "BlackList.SignText");
        x.black_sign_all        = false;
        
        x.misc_tnt              = getBoolean(w, "MiscProtection.NoTNTExplosion");
        x.misc_ice              = getBoolean(w, "MiscProtection.IceMelt");
        x.misc_liquid           = getBoolean(w, "MiscProtection.LiquidControl");
        x.misc_fire             = getBoolean(w, "MiscProtection.Fire");

        x.block_worledit        = getBoolean(w, "BlockProtection.WorldEdit");
        x.block_ownblock        = getBoolean(w, "BlockProtection.OwnBlocks");
        x.block_nodrop          = getBoolean(w, "BlockProtection.NoDrop");
        
        if (x.block_ownblock && x.block_nodrop) {
            x.block_nodrop = false;
        }  

        x.block_water           = getBoolean(w, "BlockProtection.WaterFlow");
        x.block_explosion       = getBoolean(w, "BlockProtection.Explosions");
        x.block_creative        = getBoolean(w, "BlockProtection.CreativeOnly");
        x.block_pistons         = getBoolean(w, "BlockProtection.Pistons");
        x.block_physics         = getBoolean(w, "BlockProtection.Physics");
        x.block_against         = getBoolean(w, "BlockProtection.BlockAgainst");
        x.block_attach          = getBoolean(w, "BlockProtection.CheckAttached");
        x.block_invert          = getBoolean(w, "BlockProtection.inverted");
        x.block_exclude         = blacklist.buildHashSet(getStringAsStringSet(w, "BlockProtection.exclude"));

        x.prevent_drop          = getBoolean(w, "Preventions.ItemDrop");
        x.prevent_pickup        = getBoolean(w, "Preventions.ItemPickup");
        x.prevent_pvp           = getBoolean(w, "Preventions.PvP");
        x.prevent_mobs          = getBoolean(w, "Preventions.Mobs");
        x.prevent_eggs          = getBoolean(w, "Preventions.Eggs");
        x.prevent_target        = getBoolean(w, "Preventions.Target");
        x.prevent_mobsdrop      = getBoolean(w, "Preventions.MobsDrop");
        x.prevent_irongolem     = getBoolean(w, "Preventions.IronGolem");
        x.prevent_snowgolem     = getBoolean(w, "Preventions.SnowGolem");
        x.prevent_wither        = getBoolean(w, "Preventions.Wither");
        x.prevent_drops         = getBoolean(w, "Preventions.ClearDrops");
        x.prevent_enchant       = getBoolean(w, "Preventions.Enchantments");
        x.prevent_mcstore       = getBoolean(w, "Preventions.MineCartStorage");
        x.prevent_bedrock       = getBoolean(w, "Preventions.BreakBedRock");
        x.prevent_invinteract   = getBoolean(w, "Preventions.InvInteract");
        x.prevent_bonemeal      = getBoolean(w, "Preventions.Bonemeal");
        x.prevent_villager      = getBoolean(w, "Preventions.InteractVillagers");
        x.prevent_potion        = getBoolean(w, "Preventions.PotionSplash");
        x.prevent_frame         = getBoolean(w, "Preventions.ItemFrame");
        x.prevent_vehicle       = getBoolean(w, "Preventions.VehicleDrop");
        x.prevent_limitvechile  = getInteger(w, "Preventions.VehicleLimit");
        x.prevent_stacklimit    = getInteger(w, "Preventions.StackLimit");
        x.prevent_open          = getBoolean(w, "Preventions.InventoryOpen");
        x.prevent_fly           = getBoolean(w, "Preventions.RemoveFlyOnPvP");
        x.prevent_creative      = getBoolean(w, "Preventions.NoCreativeOnPvP");
        
        if (!main.config_single) {
            config_cache.put(w.getName(), x);
        } else {
            nodes = x;
        }
        
        updateConfig();
    }
}

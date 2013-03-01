/*
 * Copyright (C) 2011-2012 FurmigaHumana.  All rights reserved.
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
import org.bukkit.World;

/**
 *
 * @author FurmigaHumana
 */
public class CreativeWorldConfig extends Configuration {
    private CoreSafeCache<String, CreativeWorldNodes> cache = new CoreSafeCache<String, CreativeWorldNodes>(100);
    private CreativeWorldNodes nodes = new CreativeWorldNodes();

    public CreativeWorldConfig(CorePlugin plugin) {
        super(plugin);
    }

    public void clear() {
        cache.clear();
    }

    public CreativeWorldNodes get(World w) {
        CreativeMainConfig   main   = CreativeControl.getMainConfig();
        if (main.config_single) {
            return nodes;
        } else {
            CreativeWorldNodes n = cache.get(w.getName());
            if (n == null) { load(w); }
            return n;
        }
    }

    public void load(World w) {
        CreativeMainConfig   main   = CreativeControl.getMainConfig();
        
        CreativeWorldNodes x = new CreativeWorldNodes();

        x.world_creative        = getBoolean(w, "World.Creative");
        x.world_exclude         = getBoolean(w, "World.Exclude");
        x.world_changegm        = getBoolean(w, "World.ChangeGameMode");
        x.world_nodrop          = getBoolean(w, "World.GlobalNoDrop");

        x.black_cmds            = getStringSet(w, "BlackList.Commands");
        x.black_place           = getIntegerSet(w, "BlackList.BlockPlace");
        x.black_break           = getIntegerSet(w, "BlackList.BlockBreak");
        x.black_use             = getIntegerSet(w, "BlackList.ItemUse");
        x.black_interact        = getIntegerSet(w, "BlackList.ItemInteract");
        x.black_inventory       = getIntegerSet(w, "BlackList.Inventory");
        x.black_sign            = getStringSet(w, "BlackList.EconomySigns");
        x.black_sign_all        = false;
        
        x.misc_tnt              = getBoolean(w, "MiscProtection.NoTNTExplosion");
        x.misc_ice              = getBoolean(w, "MiscProtection.IceMelt");
        x.misc_liquid           = getBoolean(w, "MiscProtection.LiquidControl");
        x.misc_fire             = getBoolean(w, "MiscProtection.Fire");

        x.block_worledit        = getBoolean(w, "BlockProtection.WorldEdit");
        x.block_ownblock        = getBoolean(w, "BlockProtection.OwnBlocks");
        x.block_nodrop          = getBoolean(w, "BlockProtection.NoDrop");
        x.block_explosion       = getBoolean(w, "BlockProtection.Explosions");
        x.block_creative        = getBoolean(w, "BlockProtection.CreativeOnly");
        x.block_pistons         = getBoolean(w, "BlockProtection.Pistons");
        x.block_against         = getBoolean(w, "BlockProtection.BlockAgainst");
        x.block_attach          = getBoolean(w, "BlockProtection.CheckAttached");
        x.block_invert          = getBoolean(w, "BlockProtection.inverted");
        x.block_exclude         = getIntegerSet(w, "BlockProtection.exclude");

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

        if (!main.config_single) {
            cache.put(w.getName(), x);
        } else {
            nodes = x;
        }
    }
}
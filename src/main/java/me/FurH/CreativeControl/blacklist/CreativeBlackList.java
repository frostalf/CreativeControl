package me.FurH.CreativeControl.blacklist;

import java.util.HashSet;
import me.FurH.Core.util.Communicator;
import me.FurH.CreativeControl.CreativeControl;
import me.FurH.CreativeControl.stack.CreativeItemStack;

/**
 *
 * @author FurmigaHumana
 * All Rights Reserved unless otherwise explicitly stated.
 */
public class CreativeBlackList {

    private static CreativeItemStack fixed = new CreativeItemStack(-59941, -1);

    public boolean isBlackListed(HashSet<CreativeItemStack> source, CreativeItemStack check) {
        
        if (source.contains(fixed)) {
            return true;
        }
        
        if (source.contains(check)) {
            return true;
        }

        for (CreativeItemStack item : source) {
            if (item.equals(check)) {
                source.add(check); return true;
            }
        }

        return false;
    }
    
    public HashSet<CreativeItemStack> buildHashSet(HashSet<String> source) {
        HashSet<CreativeItemStack> ret = new HashSet<>();
        
        for (String string : source) {

            if (!isClearInteger(string)) {
                continue;
            }
            
            if (!string.contains(":")) {
                ret.add(new CreativeItemStack(Integer.parseInt(string), -1)); continue;
            }
            
            ret.add(new CreativeItemStack(Integer.parseInt(string.split(":")[0]), Integer.parseInt(string.split(":")[1])));
        }
        
        return ret;
    }
    
    private boolean isClearInteger(String string) {
        Communicator com = CreativeControl.getPlugin().getCommunicator();
        
        try {
            Integer.parseInt(string.replaceAll("[^0-9-.]", ""));
        } catch (Exception ex) {
            com.log("[TAG] &c"+string+" is not a valid item id!");
            return false;
        }
        
        return true;
    }
}

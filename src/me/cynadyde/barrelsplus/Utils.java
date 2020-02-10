package me.cynadyde.barrelsplus;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * The BarrelsPlus plugin's utility functions.
 */
class Utils {

    /**
     * Translates ampersands into color codes, then formats the string.
     */
    static String chatFormat(String message, Object... objs) {
        return String.format(ChatColor.translateAlternateColorCodes('&', message), objs);
    }

    /**
     * Tests if the given item stack is a barrel that has contents.
     * NOTE: this returns true for all barrels in this build.
     */
    static boolean isNonEmptyBarrel(ItemStack item) {
        return item != null && item.getType() == Material.BARREL; //&& item.hasItemMeta();
    }
}

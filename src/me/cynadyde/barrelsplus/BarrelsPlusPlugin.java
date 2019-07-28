package me.cynadyde.barrelsplus;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;


/**
 * Main class of the BarrelsPlus plugin.
 * @noinspection unused
 */
public class BarrelsPlusPlugin extends JavaPlugin implements Listener {

    /**
     * Translates ampersands into color codes, then formats the string.
     */
    private String formatted(String message, Object... objs) {
        return String.format(ChatColor.translateAlternateColorCodes('&', message), objs);
    }

    @Override
    public void onEnable() {

        getServer().getPluginManager().registerEvents(this, this);
    }

    /**
     * Keep contents inside of barrels when they are picked up.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {

        if (!event.isDropItems()) {
            return;
        }

        Block block = event.getBlock();

        // Make sure that the block being broken is a barrel...
        if (!block.getType().equals(Material.BARREL)) {
            return;
        }

        // Get the location and world...
        Location loc = event.getBlock().getLocation();
        World world = loc.getWorld();

        if (world == null) {
            return;
        }

        Barrel blockState = (Barrel) block.getState();

        // Create a list for the items that will be dropped...
        List<ItemStack> eventDrops = new ArrayList<>();

        // Get the contents of the barrel being broken...
        ItemStack[] blockContents = blockState.getSnapshotInventory().getContents();

        // Create a list to hold the names of the first five items...
        List<String> items = new ArrayList<>();

        // For each item in the block's inventory...
        blockContentsLoop:
        for (int i = 0; i < blockContents.length; i++) {

            ItemStack item = blockContents[i];

            if (item != null) {

                // If the item is a container...
                if (item.getType().equals(Material.BARREL)) {

                    BlockStateMeta itemMeta = (BlockStateMeta) item.getItemMeta();
                    if (itemMeta == null) {
                        continue;
                    }

                    // If the container has anything inside it...
                    Container itemState = (Container) itemMeta.getBlockState();

                    for (ItemStack nestedItem : itemState.getInventory().getContents()) {
                        if (nestedItem != null) {

                            // Separate it from the inventory into a separate drop...
                            blockContents[i] = null;
                            eventDrops.add(item);

                            continue blockContentsLoop;
                        }
                    }
                }
                // Add the name of the item to the list...
                if (items.size() < 5) {
                    String itemName = null;

                    // Check if there is a custom display name...
                    ItemMeta itemMeta = item.getItemMeta();
                    if (itemMeta != null) {
                        if (itemMeta.hasDisplayName()) {
                            itemName = itemMeta.getDisplayName();
                        }
                    }
                    // Create the name from the Material type...
                    if (itemName == null) {
                        itemName = item.getType().toString().replace('_', ' ').toLowerCase();
                        if (itemName.length() > 0) {
                            itemName = itemName.substring(0, 1).toUpperCase() + itemName.substring(1);
                        }
                    }
                    items.add(formatted("&r&f%s x%d", itemName, item.getAmount()));
                }

                // Or, if the list already has five items, just add the number that is remaining...
                else if (items.size() == 5) {
                    items.add(formatted("&f&oand %d more...", blockContents.length - 1));
                }
            }
        }
        // Create the barrel item that will be dropped...
        ItemStack barrelItem = new ItemStack(Material.BARREL, 1);


        // If the barrel block had a custom name, add it to the barrel item...
        String customName = ((Barrel) block.getState()).getCustomName();
        if (customName != null) {

            BlockStateMeta barrelMeta = (BlockStateMeta) barrelItem.getItemMeta();
            if (barrelMeta == null) {
                return;
            }
            barrelMeta.setDisplayName(customName);
            barrelItem.setItemMeta(barrelMeta);
        }

        // If the barrel block contained any items...
        if (items.size() > 0) {

            BlockStateMeta barrelMeta = (BlockStateMeta) barrelItem.getItemMeta();
            if (barrelMeta == null) {
                return;
            }
            // Set the barrel item's inventory...
            Barrel barrelState = (Barrel) barrelMeta.getBlockState();
            barrelState.getInventory().setContents(blockContents);
            barrelMeta.setBlockState(barrelState);

            // Set an informative lore...
            List<String> lore = barrelMeta.getLore();
            if (lore == null) {
                lore = new ArrayList<>();
            }
            lore.addAll(0, items);
            barrelMeta.setLore(lore);

            barrelItem.setItemMeta(barrelMeta);
        }

        // Add the barrel item to the list of items being dropped...
        eventDrops.add(barrelItem);

        // Cancel what would drop normally...
        blockState.getInventory().setContents(new ItemStack[0]);
        event.setDropItems(false);

        // Drop the barrel item and any separated items...
        for (ItemStack item : eventDrops) {
            world.dropItemNaturally(loc, item);
        }
    }
}

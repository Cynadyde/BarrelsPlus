package me.cynadyde.barrelsplus;

import org.apache.commons.lang.WordUtils;
import org.bukkit.*;
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
 */
public class BarrelsPlusPlugin extends JavaPlugin implements Listener {

    /**
     * Translates ampersands into color codes, then formats the string.
     */
    private String chatFormat(String message, Object... objs) {
        return String.format(ChatColor.translateAlternateColorCodes('&', message), objs);
    }

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    /**
     * Keep contents inside of barrels when they are picked up.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {

        // make sure the event is meant to drop items...
        if (event.isCancelled() || !event.isDropItems()) {
            return;
        }

        Block block = event.getBlock();
        if (block.getType() != Material.BARREL) {
            return;
        }

        Location loc = event.getBlock().getLocation();
        World world = loc.getWorld();
        if (world == null) {
            return;
        }

        Barrel blockState = (Barrel) block.getState();
        ItemStack[] blockContents = blockState.getSnapshotInventory().getContents();

        // create a list for the items that will be dropped for this event
        // and a list to hold the names of the first five items...
        List<ItemStack> eventDrops = new ArrayList<>();
        List<String> items = new ArrayList<>();

        blockContentsLoop:
        for (int i = 0; i < blockContents.length; i++) {

            ItemStack item = blockContents[i];

            if (item != null) {

                // if the item is a container...
                if (item.getItemMeta() instanceof BlockStateMeta) {
                    BlockStateMeta itemBlockMeta = (BlockStateMeta) item.getItemMeta();

                    if (itemBlockMeta.getBlockState() instanceof Container) {
                        Container itemContainerMeta = (Container) itemBlockMeta.getBlockState();

                        // if the container has anything inside it...
                        for (ItemStack nestedItem : itemContainerMeta.getInventory().getContents()) {
                            //noinspection ConstantConditions
                            if (nestedItem != null) {

                                // separate the container from the inventory into a separate drop...
                                blockContents[i] = null;
                                eventDrops.add(item);
                                continue blockContentsLoop;
                            }
                        }
                    }
                }
                // add the names of the first five items to the list...
                if (items.size() < 5) {
                    String itemName = null;

                    // check if there is a custom display name...
                    ItemMeta itemMeta = item.getItemMeta();
                    if (itemMeta != null) {
                        if (itemMeta.hasDisplayName()) {
                            itemName = itemMeta.getDisplayName();
                        }
                    }
                    // or, create the display name from its material type...
                    // sorry that it is not translated based on locale!
                    if (itemName == null) {
                        itemName = WordUtils.capitalizeFully(item.getType().toString().replace("_", " "));
                    }
                    items.add(chatFormat("&r&f%s x%d", itemName, item.getAmount()));
                }
            }
        }

        // if the list has five or more items, add the number that is remaining...
        if (items.size() >= 5) {
            items.add(chatFormat("&f&oand %d more...", blockContents.length - 1));
        }

        // if the barrel is empty and we are in creative mode, quit...
        if (items.isEmpty() && eventDrops.isEmpty()) {
            if (event.getPlayer().getGameMode() == GameMode.CREATIVE) {
                return;
            }
        }
        ItemStack barrelItem = new ItemStack(Material.BARREL, 1);

        // if the barrel block had a custom name, add it to the barrel item...
        String customName = ((Barrel) block.getState()).getCustomName();
        if (customName != null) {

            BlockStateMeta barrelMeta = (BlockStateMeta) barrelItem.getItemMeta();
            assert (barrelMeta != null);

            barrelMeta.setDisplayName(customName);
            barrelItem.setItemMeta(barrelMeta);
        }

        // if the barrel block contained any items...
        if (items.size() > 0) {

            BlockStateMeta barrelMeta = (BlockStateMeta) barrelItem.getItemMeta();
            assert (barrelMeta != null);

            // set the barrel item's inventory...
            Barrel barrelState = (Barrel) barrelMeta.getBlockState();
            barrelState.getInventory().setContents(blockContents);
            barrelMeta.setBlockState(barrelState);

            // set an informative lore...
            List<String> lore = barrelMeta.getLore();
            if (lore == null) {
                lore = new ArrayList<>();
            }
            lore.addAll(0, items);
            barrelMeta.setLore(lore);

            barrelItem.setItemMeta(barrelMeta);
        }

        // add the barrel item to the list of items being dropped...
        eventDrops.add(barrelItem);

        // cancel what would drop normally...
        blockState.getInventory().setContents(new ItemStack[0]);
        event.setDropItems(false);

        // drop the barrel item and any separated items...
        for (ItemStack item : eventDrops) {
            world.dropItemNaturally(loc, item);
        }
    }
}

package me.cynadyde.barrelsplus;

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


public class BarrelsPlusPlugin extends JavaPlugin implements Listener {

    private String formatted(String message, Object...objs) {
        return String.format(ChatColor.translateAlternateColorCodes('&', message), objs);
    }

    @Override
    public void onEnable() {

        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {

    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {

        Block block = event.getBlock();

        // Make sure that the block being broken is a barrel...
        if (!block.getType().equals(Material.BARREL)) {
            return;
        }

        getLogger().info("A barrel is being broken...");

        // Create a list for the items that will be dropped...
        List<ItemStack> droppedItems = new ArrayList<>();

        // Get the contents of the barrel being broken...
        ItemStack[] items = ((Barrel) (block.getState())).getSnapshotInventory().getContents();

        // If there are any other barrels with contents in this inventory, separate them out
        // At the same time, start building a list of item names for the lore

        List<String> description = new ArrayList<>();

        for (int i = 0; i < items.length; i++) {

            ItemStack item = items[i];

            if (item != null) {

                getLogger().info(String.format(" ... contains %s x%d", item.getType(), item.getAmount()));

                if (item.getType().equals(Material.BARREL)) {

                    BlockStateMeta itemMeta = (BlockStateMeta) item.getItemMeta();

                    if (itemMeta != null) {

                        // !!! assuming the block state can be cast to a container
                        ItemStack[] itemContents = ((Container) itemMeta.getBlockState()).getInventory().getContents();
                        boolean itemHasContents = false;

                        for (ItemStack itemContent : itemContents) {
                            if (itemContent != null) {
                                itemHasContents = true;
                                break;
                            }
                        }
                        if (itemHasContents) {
                            items[i] = null;
                            droppedItems.add(item);

                            getLogger().info(" ... ... this barrel contained items!");

                            continue;

                        }
                        else {
                            getLogger().info(" ... ... this barrel was empty!");
                        }
                    }
                }

                // Not sure why this isn't more elegant...
                if (description.size() < 5) {
                    String itemName = null;

                    ItemMeta itemMeta = item.getItemMeta();
                    if (itemMeta != null) {
                        if (itemMeta.hasDisplayName()) {
                            itemName = itemMeta.getDisplayName();
                        }
                    }
                    if (itemName == null) {
                        itemName = item.getType().toString().replace('_', ' ').toLowerCase();
                        if (itemName.length() > 0) {
                            itemName = itemName.substring(0, 1).toUpperCase()
                                    + itemName.substring(1, itemName.length());
                        }
                    }

                    description.add(formatted("&r&f%s x%d", itemName, item.getAmount()));
                }
                else if (description.size() == 5) {
                    description.add(formatted("&f&oand %d more...", items.length - 1));
                }
            }

        }

        // Create the barrel item that will be dropped...
        ItemStack barrelItem = new ItemStack(Material.BARREL, 1);

        // Add custom name to the barrel if it existed on the block...
        String customName = ((Barrel) block.getState()).getCustomName();

        if (customName != null) {
            BlockStateMeta barrelMeta = (BlockStateMeta) barrelItem.getItemMeta();

            // Get any NBT data that was already on the old barrel block...
            if (barrelMeta == null) {
                getLogger().warning("BlockStateMeta for new ItemStack of Material.BARREL was null");
                return;
            }
            barrelMeta.setDisplayName(customName);
            barrelItem.setItemMeta(barrelMeta);
        }

        // If the barrel item contains items...
        if (description.size() > 0) {

            BlockStateMeta barrelMeta = (BlockStateMeta) barrelItem.getItemMeta();

            if (barrelMeta == null) {
                getLogger().warning("BlockStateMeta for new ItemStack of Material.BARREL was null");
                return;
            }

            // Set the barrel item's inventory...
            Barrel barrelState = (Barrel) barrelMeta.getBlockState();
            barrelState.getInventory().setContents(items);
            barrelMeta.setBlockState(barrelState);

            getLogger().info("Here are the items in the barrel item-meta block-state:");

            for (ItemStack item : barrelState.getInventory().getContents()) {
                if (item != null) {
                    getLogger().info(String.format(" ... item: %s x%d", item.getType(), item.getAmount()));
                }
            }
            // Set an informative lore...
            List<String> lore = barrelMeta.getLore();
            if (lore == null) {
                lore = new ArrayList<>();
            }

            lore.addAll(0, description);
            barrelMeta.setLore(lore);

            barrelItem.setItemMeta(barrelMeta);
        }


        // Add the barrel item to the list of items being dropped...
        droppedItems.add(barrelItem);

        // Cancel what would drop normally...
        ((Barrel) event.getBlock().getState()).getInventory().setContents(new ItemStack[0]);
        event.setDropItems(false);

        // Drop the barrel item and any separated items...
        Location loc = event.getBlock().getLocation();
        World world = loc.getWorld();

        if (world == null) {
            getLogger().warning("World of BlockBreakEvent Block was null");
            return;
        }

        getLogger().info("Dropping items:");

        for (ItemStack item : droppedItems) {

            getLogger().info(String.format(" ... dropping item: %s", item));

            world.dropItemNaturally(loc, item);
        }

        getLogger().info("BreakBlockEvent finished!");
    }
}

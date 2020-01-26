package me.cynadyde.barrelsplus;

import org.apache.commons.lang.WordUtils;
import org.bukkit.*;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main class of the BarrelsPlus plugin.
 */
public class BarrelsPlusPlugin extends JavaPlugin implements Listener {

    /**
     * Holds bounding boxes where duplicate items may spawn as a result of the CraftBukkit bug.
     */
    private Map<BoundingBox, Long> poiConflicts = new HashMap<>();

    /**
     * Translates ampersands into color codes, then formats the string.
     */
    private static String chatFormat(String message, Object... objs) {
        return String.format(ChatColor.translateAlternateColorCodes('&', message), objs);
    }

    /**
     * Tests if the given item stack is a barrel that has contents.
     * NOTE: all barrels test true in this build.
     */
    private static boolean isNonEmptyBarrel(ItemStack item) {
        return item != null && item.getType() == Material.BARREL; // && item.hasItemMeta();
    }

    @Override
    public void onEnable() {

        getServer().getPluginManager().registerEvents(this, this);

        getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
            if (!poiConflicts.isEmpty()) {
                long curTime = System.currentTimeMillis();
                poiConflicts.entrySet().removeIf(e -> e.getValue() >= curTime);
            }
        }, 20L, 20L);
    }

    /**
     * Prevents barrels with contents from being placed into a furnace.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClickEvent(InventoryClickEvent event) {

        if (event.getInventory() instanceof FurnaceInventory) {
            switch (event.getAction()) {
                default: break;
                case PLACE_ONE:
                case PLACE_SOME:
                case PLACE_ALL:
                case SWAP_WITH_CURSOR:
                    if (event.getSlotType() == InventoryType.SlotType.FUEL) {
                        if (isNonEmptyBarrel(event.getCursor())) {
                            event.setCancelled(true);
                        }
                    }
                    break;
                case HOTBAR_SWAP:
                    if (event.getSlotType() == InventoryType.SlotType.FUEL) {
                        if (isNonEmptyBarrel(event.getWhoClicked()
                                .getInventory().getItem(event.getHotbarButton()))) {
                            event.setCancelled(true);
                        }
                    }
                    break;
                case MOVE_TO_OTHER_INVENTORY:
                    if (isNonEmptyBarrel(event.getCurrentItem())) {
                        event.setCancelled(true);
                    }
                    break;
            }
        }
    }

    /**
     * Prevents barrels with contents from being dragged into a furnace.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryDrag(InventoryDragEvent event) {

        if (event.getInventory() instanceof FurnaceInventory) {
            if (isNonEmptyBarrel(event.getOldCursor())) {
                if (event.getInventorySlots().contains(/*fuel slot index*/ 1)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    /**
     * Prevents barrels with contents from being sucked into a furnace.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {

        if (event.getDestination() instanceof FurnaceInventory) {
            if (isNonEmptyBarrel(event.getItem())) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Keep track of where a barrel place event was cancelled.
     * <p>
     * This is a workaround for a CraftBukkit bug where cancelling
     * a block place event for barrels with contents causes them to
     * drop a duplicate of their contents.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event) {

        if (event.isCancelled() && event.getBlockPlaced().getType() == Material.BARREL) {

            Vector blocKVec = event.getBlock().getLocation().toVector();
            double randomSpawnRadius = 0.5; // this radius seems to catch em all

            BoundingBox itemSpawnBounds = new BoundingBox(
                    blocKVec.getBlockX() - randomSpawnRadius,
                    blocKVec.getBlockY() - randomSpawnRadius,
                    blocKVec.getBlockZ() - randomSpawnRadius,
                    blocKVec.getBlockX() + 1 + randomSpawnRadius,
                    blocKVec.getBlockY() + 1 + randomSpawnRadius,
                    blocKVec.getBlockZ() + 1 + randomSpawnRadius
            );
            poiConflicts.put(itemSpawnBounds, System.currentTimeMillis() + /*expiration*/ 1000);
        }
    }

    /**
     * Intercepts and cancels spawned items that are the
     * result of the CraftBukkit bug.
     * <p>
     * This bug causes a cancelled block place event to
     * drop a duplicate of barrel contents.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onItemSpawn(ItemSpawnEvent event) {

        if (event.getEntityType() == EntityType.DROPPED_ITEM) {
            Vector spawnVec = event.getLocation().toVector();

            for (Map.Entry<BoundingBox, Long> e : poiConflicts.entrySet()) {
                if (e.getKey().contains(spawnVec)) {
                    event.setCancelled(true);
                    break;
                }
            }
        }
    }

    /**
     * Keeps contents inside of barrels when they are picked up.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {

        // make sure the event is meant to drop items...
        if (event.isCancelled() || !event.isDropItems()) {
            return;
        }

        if (event.getBlock().getType() != Material.BARREL) {
            return;
        }
        Block block = event.getBlock();
        Location loc = block.getLocation();
        World world = loc.getWorld();
        if (world == null) {
            return;
        }

        Barrel blockState = (Barrel) block.getState();
        ItemStack[] blockContents = blockState.getSnapshotInventory().getContents();
        int contentsCount = 0;

        // create a list for the items that will be dropped for this event
        // and a list to hold the names of the first five items...
        List<ItemStack> eventDrops = new ArrayList<>();
        List<String> previewItems = new ArrayList<>();

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

                                // separate the container into a separate event drop...
                                blockContents[i] = null;
                                eventDrops.add(item);
                                continue blockContentsLoop;
                            }
                        }
                    }
                }
                // add the names of the first five items to the list...
                if (previewItems.size() < 5) {
                    String itemName = null;

                    // check if there is a custom display name...
                    ItemMeta itemMeta = item.getItemMeta();
                    if (itemMeta != null) {
                        if (itemMeta.hasDisplayName()) {
                            itemName = itemMeta.getDisplayName();
                        }
                    }
                    // or, create the display name from its material type...
                    // FIXME see if we can translate based on server locale!
                    if (itemName == null) {
                        itemName = WordUtils.capitalizeFully(item.getType().toString().replace("_", " "));
                    }
                    previewItems.add(chatFormat("&r&f%s x%d", itemName, item.getAmount()));
                }
                contentsCount++;
            }
        }

        // if the list has five or more items, add the number that is remaining...
        if (previewItems.size() >= 5) {
            previewItems.add(chatFormat("&f&oand %d more...", contentsCount));
        }

        // if the barrel is empty and we are in creative mode, quit...
        if (contentsCount == 0 && eventDrops.isEmpty()) {
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
        if (contentsCount > 0) {

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
            lore.addAll(0, previewItems);
            barrelMeta.setLore(lore);

            barrelItem.setItemMeta(barrelMeta);
        }

        // add the barrel item to the list of items being dropped...
        eventDrops.add(barrelItem);

        // cancel what would drop normally...
        blockState.getInventory().setContents(new ItemStack[0]);
        event.setDropItems(false);

        // drop the barrel and any separated containers...
        for (ItemStack item : eventDrops) {
            world.dropItemNaturally(loc, item);
        }
    }
}

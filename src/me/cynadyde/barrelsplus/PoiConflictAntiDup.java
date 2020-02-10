package me.cynadyde.barrelsplus;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Barrel;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The plugin's event listener to prevent
 * the POI conflict item duplication glitch.
 * <p>
 * This bug causes a cancelled BlockPlaceEvent to
 * drop a duplicate of barrel contents.
 */
public class PoiConflictAntiDup implements Listener {

    /**
     * Holds bounding boxes where duplicate items may spawn as a result of the CraftBukkit bug.
     */
    private final Set<PoiConflict> poiConflicts = new HashSet<>();

    PoiConflictAntiDup(BarrelsPlusPlugin plugin) {

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
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

        if (event.isCancelled() && event.getBlockPlaced().getType() == Material.BARREL
                && event.getBlockPlaced().getState() instanceof Barrel) {

            //noinspection ConstantConditions - getContents() may contain null items
            List<ItemStack> duplicates = Arrays.stream(
                    ((Barrel) event.getBlockPlaced().getState()).getInventory().getContents()
            ).filter(i -> i != null && !i.getType().isAir()).collect(Collectors.toList());

            if (duplicates.size() > 0) {

                World world = event.getBlockPlaced().getWorld();
                Vector blocKVec = event.getBlock().getLocation().toVector();
                double randomSpawnRadius = 0.5; // this radius seems to catch them all

                BoundingBox itemSpawnBounds = new BoundingBox(
                        blocKVec.getBlockX() - randomSpawnRadius,
                        blocKVec.getBlockY() - randomSpawnRadius,
                        blocKVec.getBlockZ() - randomSpawnRadius,
                        blocKVec.getBlockX() + 1 + randomSpawnRadius,
                        blocKVec.getBlockY() + 1 + randomSpawnRadius,
                        blocKVec.getBlockZ() + 1 + randomSpawnRadius
                );
                for (ItemStack duplicate : duplicates) {
                    poiConflicts.add(new PoiConflict(world, itemSpawnBounds, duplicate));
                }
            }
        }
    }

    /**
     * Intercepts and cancels spawned items that are the
     * result of the POI conflict CraftBukkit bug.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onItemSpawn(ItemSpawnEvent event) {

        if (event.getEntityType() == EntityType.DROPPED_ITEM) {
            Vector spawnVec = event.getLocation().toVector();

            for (PoiConflict e : poiConflicts) {
                if (e.getWorld().equals(event.getEntity().getWorld())) {
                    if (e.getArea().contains(spawnVec)) {
                        if (e.getDuplicate().isSimilar(event.getEntity().getItemStack())) {

                            int remaining = e.getDuplicate().getAmount()
                                    - event.getEntity().getItemStack().getAmount();

                            if (remaining > 0) { e.getDuplicate().setAmount(remaining); }
                            else { poiConflicts.remove(e); }

                            event.setCancelled(true);
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Gets any POI conflict duplicate items currently
     * marked for removal.
     */
    @SuppressWarnings("unused")
    public Set<PoiConflict> getPoiConflicts() {
        return poiConflicts;
    }

    /**
     * An expectation that a given item stack will spawn in a given vicinity,
     * and that it is a glitch and should be immediately removed.
     */
    private static class PoiConflict {

        private final World world;
        private final BoundingBox area;
        private final ItemStack duplicate;

        PoiConflict(World world, BoundingBox area, ItemStack duplicate) {
            this.world = world;
            this.area = area;
            this.duplicate = duplicate.clone();
        }

        World getWorld() {
            return world;
        }

        BoundingBox getArea() {
            return area;
        }

        ItemStack getDuplicate() {
            return duplicate;
        }
    }
}

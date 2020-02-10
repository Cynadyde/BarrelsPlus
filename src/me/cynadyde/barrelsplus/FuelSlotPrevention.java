package me.cynadyde.barrelsplus;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.FurnaceInventory;

/**
 * The plugin's event handler to prevent
 * barrels from entering the fuel slot of a furnace.
 */
public class FuelSlotPrevention implements Listener {

    FuelSlotPrevention(BarrelsPlusPlugin plugin) {

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
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
                        if (Utils.isNonEmptyBarrel(event.getCursor())) {
                            event.setCancelled(true);
                        }
                    }
                    break;
                case HOTBAR_SWAP:
                    if (event.getSlotType() == InventoryType.SlotType.FUEL) {
                        if (Utils.isNonEmptyBarrel(event.getWhoClicked()
                                .getInventory().getItem(event.getHotbarButton()))) {
                            event.setCancelled(true);
                        }
                    }
                    break;
                case MOVE_TO_OTHER_INVENTORY:
                    if (Utils.isNonEmptyBarrel(event.getCurrentItem())) {
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
            if (Utils.isNonEmptyBarrel(event.getOldCursor())) {
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
            if (Utils.isNonEmptyBarrel(event.getItem())) {
                event.setCancelled(true);
            }
        }
    }
}

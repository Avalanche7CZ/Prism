package me.botsko.prism.listeners;

import java.util.Map;
import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.ActionFactory;
import me.botsko.prism.actionlibs.RecordingQueue;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryHolder;

public class PrismInventoryMoveItemEvent implements Listener {
   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onInventoryMoveItem(InventoryMoveItemEvent event) {
      InventoryHolder ih;
      Location containerLoc;
      BlockState eventChest;
      if (Prism.getIgnore().event("item-insert") && event.getDestination() != null) {
         ih = event.getDestination().getHolder();
         containerLoc = null;
         if (ih instanceof BlockState) {
            eventChest = (BlockState)ih;
            containerLoc = eventChest.getLocation();
         }

         if (containerLoc == null) {
            return;
         }

         if (event.getSource().getType().equals(InventoryType.HOPPER)) {
            RecordingQueue.addToQueue(ActionFactory.createItemStack("item-insert", event.getItem(), event.getItem().getAmount(), 0, (Map)null, containerLoc, "hopper"));
         }
      }

      if (Prism.getIgnore().event("item-remove") && event.getSource() != null) {
         ih = event.getSource().getHolder();
         containerLoc = null;
         if (ih instanceof BlockState) {
            eventChest = (BlockState)ih;
            containerLoc = eventChest.getLocation();
         }

         if (containerLoc == null) {
            return;
         }

         if (event.getDestination().getType().equals(InventoryType.HOPPER)) {
            RecordingQueue.addToQueue(ActionFactory.createItemStack("item-remove", event.getItem(), event.getItem().getAmount(), 0, (Map)null, containerLoc, "hopper"));
         }
      }

   }
}

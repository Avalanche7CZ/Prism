package me.botsko.prism.listeners;

import java.util.Iterator;
import java.util.Map;
import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.ActionFactory;
import me.botsko.prism.actionlibs.RecordingQueue;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class PrismInventoryEvents implements Listener {
   private final Prism plugin;

   public PrismInventoryEvents(Prism plugin) {
      this.plugin = plugin;
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onInventoryPickupItem(InventoryPickupItemEvent event) {
      if (this.plugin.getConfig().getBoolean("prism.track-hopper-item-events")) {
         if (Prism.getIgnore().event("item-pickup")) {
            if (event.getInventory().getType().equals(InventoryType.HOPPER)) {
               RecordingQueue.addToQueue(ActionFactory.createItemStack("item-pickup", event.getItem().getItemStack(), event.getItem().getItemStack().getAmount(), -1, (Map)null, event.getItem().getLocation(), "hopper"));
            }

         }
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onInventoryDrag(InventoryDragEvent event) {
      if (this.plugin.getConfig().getBoolean("prism.tracking.item-insert") || this.plugin.getConfig().getBoolean("prism.tracking.item-remove")) {
         InventoryHolder ih = event.getInventory().getHolder();
         Location containerLoc = null;
         if (ih instanceof BlockState) {
            BlockState eventChest = (BlockState)ih;
            containerLoc = eventChest.getLocation();
         }

         Player player = (Player)event.getWhoClicked();
         Map newItems = event.getNewItems();
         Iterator i$ = newItems.entrySet().iterator();

         while(i$.hasNext()) {
            Map.Entry entry = (Map.Entry)i$.next();
            this.recordInvAction(player, containerLoc, (ItemStack)entry.getValue(), (Integer)entry.getKey(), "item-insert");
         }

      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onInventoryClick(InventoryClickEvent event) {
      if (this.plugin.getConfig().getBoolean("prism.tracking.item-insert") || this.plugin.getConfig().getBoolean("prism.tracking.item-remove")) {
         Location containerLoc = null;
         Player player = (Player)event.getWhoClicked();
         ItemStack currentitem = event.getCurrentItem();
         ItemStack cursoritem = event.getCursor();
         if (event.getInventory().getHolder() instanceof BlockState) {
            BlockState b = (BlockState)event.getInventory().getHolder();
            containerLoc = b.getLocation();
         } else if (event.getInventory().getHolder() instanceof Entity) {
            Entity e = (Entity)event.getInventory().getHolder();
            containerLoc = e.getLocation();
         } else if (event.getInventory().getHolder() instanceof DoubleChest) {
            DoubleChest chest = (DoubleChest)event.getInventory().getHolder();
            containerLoc = chest.getLocation();
         }

         int defaultSize = event.getView().getType().getDefaultSize();
         if (event.getInventory().getHolder() instanceof DoubleChest) {
            defaultSize = event.getView().getType().getDefaultSize() * 2;
         }

         if (event.getSlot() == event.getRawSlot() && event.getRawSlot() <= defaultSize) {
            if (currentitem != null && !currentitem.getType().equals(Material.AIR) && cursoritem != null && !cursoritem.getType().equals(Material.AIR)) {
               this.recordInvAction(player, containerLoc, currentitem, event.getRawSlot(), "item-remove", event);
               this.recordInvAction(player, containerLoc, cursoritem, event.getRawSlot(), "item-insert", event);
            } else if (currentitem != null && !currentitem.getType().equals(Material.AIR)) {
               this.recordInvAction(player, containerLoc, currentitem, event.getRawSlot(), "item-remove", event);
            } else if (cursoritem != null && !cursoritem.getType().equals(Material.AIR)) {
               this.recordInvAction(player, containerLoc, cursoritem, event.getRawSlot(), "item-insert", event);
            }

         } else {
            if (event.isShiftClick() && cursoritem != null && cursoritem.getType().equals(Material.AIR)) {
               this.recordInvAction(player, containerLoc, currentitem, -1, "item-insert", event);
            }

         }
      }
   }

   protected void recordInvAction(Player player, Location containerLoc, ItemStack item, int slot, String actionType) {
      this.recordInvAction(player, containerLoc, item, slot, actionType, (InventoryClickEvent)null);
   }

   protected void recordInvAction(Player player, Location containerLoc, ItemStack item, int slot, String actionType, InventoryClickEvent event) {
      if (Prism.getIgnore().event(actionType, player)) {
         int officialQuantity = 0;
         if (item != null) {
            officialQuantity = item.getAmount();
            if (event != null && event.isRightClick()) {
               if (actionType.equals("item-remove")) {
                  officialQuantity -= (int)Math.floor((double)(item.getAmount() / 2));
               } else if (actionType.equals("item-insert")) {
                  officialQuantity = 1;
               }
            }
         }

         if (actionType != null && containerLoc != null && item != null && item.getTypeId() != 0 && officialQuantity > 0) {
            RecordingQueue.addToQueue(ActionFactory.createItemStack(actionType, item, officialQuantity, slot, (Map)null, containerLoc, player.getName()));
         }

      }
   }
}

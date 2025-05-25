package me.botsko.elixr;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class InventoryUtils {
   public static void updateInventory(Player p) {
      p.updateInventory();
   }

   public static boolean playerInvIsEmpty(Player p) {
      ItemStack[] var1;
      int var2 = (var1 = p.getInventory().getContents()).length;

      for(int var3 = 0; var3 < var2; ++var3) {
         ItemStack item = var1[var3];
         if (item != null) {
            return false;
         }
      }

      return true;
   }

   public static boolean playerArmorIsEmpty(Player p) {
      ItemStack[] var1;
      int var2 = (var1 = p.getInventory().getArmorContents()).length;

      for(int var3 = 0; var3 < var2; ++var3) {
         ItemStack item = var1[var3];
         if (item != null && !item.getType().equals(Material.AIR)) {
            return false;
         }
      }

      return true;
   }

   public static int inventoryHasItem(Inventory inv, int item_id, int sub_id) {
      int currentSlot = 0;
      ItemStack[] var4;
      int var5 = (var4 = inv.getContents()).length;

      for(int var6 = 0; var6 < var5; ++var6) {
         ItemStack item = var4[var6];
         if (item != null && item.getTypeId() == item_id && item.getDurability() == sub_id) {
            return currentSlot;
         }

         ++currentSlot;
      }

      return -1;
   }

   public static boolean moveItemToHand(PlayerInventory inv, int item_id, byte sub_id) {
      int slot = inventoryHasItem(inv, item_id, sub_id);
      if (slot > -1) {
         ItemStack item = inv.getItem(slot);
         inv.clear(slot);
         if (!playerHasEmptyHand(inv)) {
            inv.setItem(slot, inv.getItemInHand());
         }

         inv.setItemInHand(item);
         return true;
      } else {
         return false;
      }
   }

   public static boolean playerHasEmptyHand(PlayerInventory inv) {
      return inv.getItemInHand().getTypeId() == 0;
   }

   public static HashMap addItemToInventory(Inventory inv, ItemStack item) {
      return inv.addItem(new ItemStack[]{item});
   }

   public static boolean handItemToPlayer(PlayerInventory inv, ItemStack item) {
      if (inv.firstEmpty() == -1) {
         return false;
      } else {
         ItemStack originalItem = inv.getItemInHand().clone();
         if (!playerHasEmptyHand(inv)) {
            for(int i = 0; i <= inv.getSize(); ++i) {
               if (i != inv.getHeldItemSlot()) {
                  ItemStack current = inv.getItem(i);
                  if (current == null) {
                     inv.setItem(i, originalItem);
                     break;
                  }
               }
            }
         }

         inv.setItemInHand(item);
         return true;
      }
   }

   public static void subtractAmountFromPlayerInvSlot(PlayerInventory inv, int slot, int quant) {
      ItemStack itemAtSlot = inv.getItem(slot);
      if (itemAtSlot != null && quant <= 64) {
         itemAtSlot.setAmount(itemAtSlot.getAmount() - quant);
         if (itemAtSlot.getAmount() == 0) {
            inv.clear(slot);
         }
      }

   }

   public static void dropItemsByPlayer(HashMap leftovers, Player player) {
      if (!leftovers.isEmpty()) {
         Iterator var2 = leftovers.entrySet().iterator();

         while(var2.hasNext()) {
            Map.Entry entry = (Map.Entry)var2.next();
            player.getWorld().dropItemNaturally(player.getLocation(), (ItemStack)entry.getValue());
         }
      }

   }

   public static boolean isEmpty(Inventory in) {
      boolean ret = false;
      if (in == null) {
         return true;
      } else {
         ItemStack[] var2;
         int var3 = (var2 = in.getContents()).length;

         for(int var4 = 0; var4 < var3; ++var4) {
            ItemStack item = var2[var4];
            ret |= item != null;
         }

         return !ret;
      }
   }

   public static void movePlayerInventoryToContainer(PlayerInventory inv, Block target, HashMap filters) throws Exception {
      InventoryHolder container = (InventoryHolder)target.getState();
      if (!moveInventoryToInventory(inv, container.getInventory(), false, filters)) {
         throw new Exception("Target container is full.");
      }
   }

   public static void moveContainerInventoryToPlayer(PlayerInventory inv, Block target, HashMap filters) throws Exception {
      InventoryHolder container = (InventoryHolder)target.getState();
      moveInventoryToInventory(container.getInventory(), inv, false, filters);
   }

   public static boolean moveInventoryToInventory(Inventory from, Inventory to, boolean fullFlag, HashMap filters) {
      if (to.firstEmpty() != -1 && !fullFlag) {
         ItemStack[] var4;
         int var5 = (var4 = from.getContents()).length;

         for(int var6 = 0; var6 < var5; ++var6) {
            ItemStack item = var4[var6];
            if (to.firstEmpty() == -1) {
               return false;
            }

            if (item != null && to.firstEmpty() != -1) {
               boolean shouldTransfer = false;
               if (filters.size() > 0) {
                  Iterator var9 = filters.entrySet().iterator();

                  while(var9.hasNext()) {
                     Map.Entry entry = (Map.Entry)var9.next();
                     if ((Integer)entry.getKey() == item.getTypeId() && (Short)entry.getValue() == item.getDurability()) {
                        shouldTransfer = true;
                     }
                  }
               } else {
                  shouldTransfer = true;
               }

               if (shouldTransfer) {
                  HashMap leftovers = to.addItem(new ItemStack[]{item});
                  if (leftovers.size() == 0) {
                     from.removeItem(new ItemStack[]{item});
                  } else {
                     from.removeItem(new ItemStack[]{item});
                     from.addItem(new ItemStack[]{(ItemStack)leftovers.get(0)});
                  }
               }
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public static ItemStack[] sortItemStack(ItemStack[] stack, Player player) {
      return sortItemStack(stack, 0, stack.length, player);
   }

   public static ItemStack[] sortItemStack(ItemStack[] stack, int start, int end, Player player) {
      stack = stackItems(stack, start, end);
      recQuickSort(stack, start, end - 1);
      return stack;
   }

   private static ItemStack[] stackItems(ItemStack[] items, int start, int end) {
      for(int i = start; i < end; ++i) {
         ItemStack item = items[i];
         if (item != null && item.getAmount() > 0 && ItemUtils.canSafelyStack(item)) {
            int max_stack = item.getMaxStackSize();
            if (item.getAmount() < max_stack) {
               int needed = max_stack - item.getAmount();

               for(int j = i + 1; j < end; ++j) {
                  ItemStack item2 = items[j];
                  if (item2 != null && item2.getAmount() > 0 && ItemUtils.canSafelyStack(item) && item2.getTypeId() == item.getTypeId() && (!ItemUtils.dataValueUsedForSubitems(item.getTypeId()) || item.getDurability() == item2.getDurability())) {
                     if (item2.getAmount() > needed) {
                        item.setAmount(max_stack);
                        item2.setAmount(item2.getAmount() - needed);
                        break;
                     }

                     item.setAmount(item.getAmount() + item2.getAmount());
                     needed = max_stack - item.getAmount();
                     items[j].setTypeId(0);
                  }
               }
            }
         }
      }

      return items;
   }

   private static void swap(ItemStack[] list, int first, int second) {
      ItemStack temp = list[first];
      list[first] = list[second];
      list[second] = temp;
   }

   private static int partition(ItemStack[] list, int first, int last) {
      swap(list, first, (first + last) / 2);
      ItemStack pivot = list[first];
      int smallIndex = first;

      for(int index = first + 1; index <= last; ++index) {
         ComparableIS compElem = new ComparableIS(list[index]);
         if (compElem.compareTo(pivot) < 0) {
            ++smallIndex;
            swap(list, smallIndex, index);
         }
      }

      swap(list, first, smallIndex);
      return smallIndex;
   }

   private static void recQuickSort(ItemStack[] list, int first, int last) {
      if (first < last) {
         int pivotLocation = partition(list, first, last);
         recQuickSort(list, first, pivotLocation - 1);
         recQuickSort(list, pivotLocation + 1, last);
      }

   }

   private static class ComparableIS {
      private ItemStack item;

      public ComparableIS(ItemStack item) {
         this.item = item;
      }

      public int compareTo(ItemStack check) {
         if (this.item == null && check != null) {
            return -1;
         } else if (this.item != null && check == null) {
            return 1;
         } else if (this.item == null && check == null) {
            return 0;
         } else if (this.item.getTypeId() > check.getTypeId()) {
            return 1;
         } else if (this.item.getTypeId() < check.getTypeId()) {
            return -1;
         } else {
            if (this.item.getTypeId() == check.getTypeId()) {
               if (ItemUtils.dataValueUsedForSubitems(this.item.getTypeId())) {
                  if (this.item.getDurability() < check.getDurability()) {
                     return 1;
                  }

                  if (this.item.getDurability() > check.getDurability()) {
                     return -1;
                  }
               }

               if (this.item.getAmount() < check.getAmount()) {
                  return -1;
               }

               if (this.item.getAmount() > check.getAmount()) {
                  return 1;
               }
            }

            return 0;
         }
      }
   }
}

package me.botsko.prism.wands;

import com.helion3.prism.libs.elixr.InventoryUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public abstract class WandBase {
   protected boolean item_given = false;
   protected String wand_mode;
   protected int item_id = 0;
   protected byte item_subid = 0;
   protected ItemStack original_item;

   public void setItemWasGiven(boolean given) {
      this.item_given = given;
   }

   public boolean itemWasGiven() {
      return this.item_given;
   }

   public void setWandMode(String mode) {
      this.wand_mode = mode;
   }

   public String getWandMode() {
      return this.wand_mode;
   }

   public int getItemId() {
      return this.item_id;
   }

   public void setItemId(int item_id) {
      this.item_id = item_id;
   }

   public byte getItemSubId() {
      return this.item_subid;
   }

   public void setItemSubId(byte item_subid) {
      this.item_subid = item_subid;
   }

   public void setItemFromKey(String key) {
      if (key.contains(":")) {
         String[] toolKeys = key.split(":");
         this.item_id = Integer.parseInt(toolKeys[0]);
         this.item_subid = Byte.parseByte(toolKeys[1]);
      }

   }

   public void setOriginallyHeldItem(ItemStack item) {
      if (item.getTypeId() > 0) {
         this.original_item = item;
      }

   }

   public void disable(Player player) {
      PlayerInventory inv = player.getInventory();
      if (this.itemWasGiven()) {
         int itemSlot;
         if (inv.getItemInHand().getTypeId() == this.item_id && inv.getItemInHand().getDurability() == this.item_subid) {
            itemSlot = inv.getHeldItemSlot();
         } else {
            itemSlot = InventoryUtils.inventoryHasItem(inv, this.item_id, this.item_subid);
         }

         if (itemSlot > -1) {
            InventoryUtils.subtractAmountFromPlayerInvSlot(inv, itemSlot, 1);
            player.updateInventory();
         }
      }

      if (this.original_item != null) {
         InventoryUtils.moveItemToHand(inv, this.original_item.getTypeId(), (byte)this.original_item.getDurability());
      }

   }
}

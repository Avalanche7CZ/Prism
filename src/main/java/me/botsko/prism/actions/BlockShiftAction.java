package me.botsko.prism.actions;

import org.bukkit.Location;
import org.bukkit.block.Block;

public class BlockShiftAction extends GenericAction {
   protected BlockShiftActionData actionData;

   public void setBlock(Block from) {
      this.actionData = new BlockShiftActionData();
      if (from != null) {
         this.block_id = from.getTypeId();
         this.block_subid = from.getData();
         this.actionData.x = from.getX();
         this.actionData.y = from.getY();
         this.actionData.z = from.getZ();
         this.world_name = from.getWorld().getName();
      }

   }

   public void setToLocation(Location to) {
      if (to != null) {
         this.x = (double)to.getBlockX();
         this.y = (double)to.getBlockY();
         this.z = (double)to.getBlockZ();
      }

   }

   public void save() {
      this.data = this.gson.toJson((Object)this.actionData);
   }

   public void setData(String data) {
      this.data = data;
      if (data != null && data.startsWith("{")) {
         this.actionData = (BlockShiftActionData)this.gson.fromJson(data, BlockShiftActionData.class);
      }

   }

   public String getNiceName() {
      return this.materialAliases.getAlias(this.block_id, this.block_subid) + " from " + this.actionData.x + " " + this.actionData.z;
   }

   public class BlockShiftActionData {
      public int x;
      public int y;
      public int z;
   }
}

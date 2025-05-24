package me.botsko.prism.actions;

import org.bukkit.block.BlockState;

public class GrowAction extends BlockAction {
   public void setBlock(BlockState state) {
      if (state != null) {
         this.block_id = state.getTypeId();
         this.block_subid = state.getData().getData();
         this.world_name = state.getWorld().getName();
         this.x = (double)state.getLocation().getBlockX();
         this.y = (double)state.getLocation().getBlockY();
         this.z = (double)state.getLocation().getBlockZ();
      }

   }
}

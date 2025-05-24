package me.botsko.prism.events;

import org.bukkit.block.BlockState;

public class BlockStateChange {
   private final BlockState originalBlock;
   private final BlockState newBlock;

   public BlockStateChange(BlockState originalBlock, BlockState newBlock) {
      this.originalBlock = originalBlock;
      this.newBlock = newBlock;
   }

   public BlockState getOriginalBlock() {
      return this.originalBlock;
   }

   public BlockState getNewBlock() {
      return this.newBlock;
   }
}

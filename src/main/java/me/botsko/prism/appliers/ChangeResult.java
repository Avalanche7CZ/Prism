package me.botsko.prism.appliers;

import me.botsko.prism.events.BlockStateChange;

public class ChangeResult {
   protected final BlockStateChange blockStateChange;
   protected final ChangeResultType changeResultType;

   public ChangeResult(ChangeResultType changeResultType) {
      this(changeResultType, (BlockStateChange)null);
   }

   public ChangeResult(ChangeResultType changeResultType, BlockStateChange blockStateChange) {
      this.blockStateChange = blockStateChange;
      this.changeResultType = changeResultType;
   }

   public BlockStateChange getBlockStateChange() {
      return this.blockStateChange;
   }

   public ChangeResultType getType() {
      return this.changeResultType;
   }
}

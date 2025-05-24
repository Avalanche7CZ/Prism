package me.botsko.prism.actions;

import com.helion3.prism.libs.elixr.BlockUtils;
import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.appliers.ChangeResult;
import me.botsko.prism.appliers.ChangeResultType;
import me.botsko.prism.appliers.PrismProcessType;
import me.botsko.prism.commandlibs.Flag;
import me.botsko.prism.events.BlockStateChange;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class BlockChangeAction extends BlockAction {
   protected BlockAction.BlockActionData actionData;

   public String getNiceName() {
      String name = "";
      if (this.getType().getName().equals("block-fade")) {
         name = name + this.materialAliases.getAlias(this.old_block_id, this.old_block_subid);
      } else {
         name = name + this.materialAliases.getAlias(this.block_id, this.block_subid);
      }

      return name;
   }

   public ChangeResult applyRollback(Player player, QueryParameters parameters, boolean is_preview) {
      Block block = this.getWorld().getBlockAt(this.getLoc());
      return this.placeBlock(player, parameters, is_preview, this.getType().getName(), this.getOldBlockId(), this.getOldBlockSubId(), this.getBlockId(), this.getBlockSubId(), block, false);
   }

   public ChangeResult applyRestore(Player player, QueryParameters parameters, boolean is_preview) {
      Block block = this.getWorld().getBlockAt(this.getLoc());
      return this.placeBlock(player, parameters, is_preview, this.getType().getName(), this.getOldBlockId(), this.getOldBlockSubId(), this.getBlockId(), this.getBlockSubId(), block, false);
   }

   public ChangeResult applyUndo(Player player, QueryParameters parameters, boolean is_preview) {
      Block block = this.getWorld().getBlockAt(this.getLoc());
      return this.placeBlock(player, parameters, is_preview, this.getType().getName(), this.getOldBlockId(), this.getOldBlockSubId(), this.getBlockId(), this.getBlockSubId(), block, false);
   }

   public ChangeResult applyDeferred(Player player, QueryParameters parameters, boolean is_preview) {
      Block block = this.getWorld().getBlockAt(this.getLoc());
      return this.placeBlock(player, parameters, is_preview, this.getType().getName(), this.getOldBlockId(), this.getOldBlockSubId(), this.getBlockId(), this.getBlockSubId(), block, true);
   }

   protected ChangeResult placeBlock(Player player, QueryParameters parameters, boolean is_preview, String type, int old_id, int old_subid, int new_id, int new_subid, Block block, boolean is_deferred) {
      BlockAction b = new BlockAction();
      b.setActionType(type);
      b.setPlugin(this.plugin);
      b.setWorldName(this.getWorldName());
      b.setX(this.getX());
      b.setY(this.getY());
      b.setZ(this.getZ());
      if (parameters.getProcessType().equals(PrismProcessType.ROLLBACK)) {
         if (!BlockUtils.isAcceptableForBlockPlace(block.getType()) && !BlockUtils.areBlockIdsSameCoreItem(block.getTypeId(), new_id) && !is_preview && !parameters.hasFlag(Flag.OVERWRITE)) {
            return new ChangeResult(ChangeResultType.SKIPPED, (BlockStateChange)null);
         } else {
            b.setBlockId(old_id);
            b.setBlockSubId(old_subid);
            return b.placeBlock(player, parameters, is_preview, block, is_deferred);
         }
      } else if (parameters.getProcessType().equals(PrismProcessType.RESTORE)) {
         if (!BlockUtils.isAcceptableForBlockPlace(block.getType()) && !BlockUtils.areBlockIdsSameCoreItem(block.getTypeId(), old_id) && !is_preview && !parameters.hasFlag(Flag.OVERWRITE)) {
            return new ChangeResult(ChangeResultType.SKIPPED, (BlockStateChange)null);
         } else {
            b.setBlockId(new_id);
            b.setBlockSubId(new_subid);
            return b.placeBlock(player, parameters, is_preview, block, is_deferred);
         }
      } else if (parameters.getProcessType().equals(PrismProcessType.UNDO)) {
         b.setBlockId(old_id);
         b.setBlockSubId(old_subid);
         return b.placeBlock(player, parameters, is_preview, block, is_deferred);
      } else {
         return new ChangeResult(ChangeResultType.SKIPPED, (BlockStateChange)null);
      }
   }

   /** @deprecated */
   @Deprecated
   public class WorldEditActionData extends BlockAction.BlockActionData {
      public int originalBlock_id;
      public byte originalBlock_subid;
      public int newBlock_id;
      public byte newBlock_subid;

      public WorldEditActionData() {
         super();
      }
   }

   /** @deprecated */
   @Deprecated
   public class BlockChangeActionData extends BlockAction.BlockActionData {
      public int old_id;
      public byte old_subid;

      public BlockChangeActionData() {
         super();
      }
   }
}

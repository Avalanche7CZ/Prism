package me.botsko.prism.actions;

import com.helion3.prism.libs.elixr.BlockUtils;
import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.appliers.ChangeResult;
import me.botsko.prism.appliers.ChangeResultType;
import me.botsko.prism.events.BlockStateChange;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;

public class HangingItemAction extends GenericAction {
   protected HangingItemActionData actionData;

   public void setHanging(Hanging hanging) {
      this.actionData = new HangingItemActionData();
      if (hanging != null) {
         this.actionData.type = hanging.getType().name().toLowerCase();
         if (hanging.getAttachedFace() != null) {
            this.actionData.direction = hanging.getAttachedFace().name().toLowerCase();
         }

         this.world_name = hanging.getWorld().getName();
         this.x = (double)hanging.getLocation().getBlockX();
         this.y = (double)hanging.getLocation().getBlockY();
         this.z = (double)hanging.getLocation().getBlockZ();
      }

   }

   public void setData(String data) {
      this.data = data;
      if (data != null && data.startsWith("{")) {
         this.actionData = (HangingItemActionData)this.gson.fromJson(data, HangingItemActionData.class);
      }

   }

   public void save() {
      this.data = this.gson.toJson((Object)this.actionData);
   }

   public String getHangingType() {
      return this.actionData.type;
   }

   public BlockFace getDirection() {
      return this.actionData.direction != null ? BlockFace.valueOf(this.actionData.direction.toUpperCase()) : null;
   }

   public String getNiceName() {
      return this.actionData.type != null ? this.actionData.type : this.data.toLowerCase();
   }

   public ChangeResult applyRollback(Player player, QueryParameters parameters, boolean is_preview) {
      return this.hangItem(player, parameters, is_preview);
   }

   public ChangeResult applyRestore(Player player, QueryParameters parameters, boolean is_preview) {
      return this.hangItem(player, parameters, is_preview);
   }

   public ChangeResult hangItem(Player player, QueryParameters parameters, boolean is_preview) {
      BlockFace attachedFace = this.getDirection();
      Location loc = (new Location(this.getWorld(), this.getX(), this.getY(), this.getZ())).getBlock().getRelative(this.getDirection()).getLocation();
      if (BlockUtils.materialMeansBlockDetachment(loc.getBlock().getType())) {
         return new ChangeResult(ChangeResultType.SKIPPED, (BlockStateChange)null);
      } else {
         try {
            Hanging hangingItem;
            if (this.getHangingType().equals("item_frame")) {
               hangingItem = (Hanging)this.getWorld().spawn(loc, ItemFrame.class);
               hangingItem.setFacingDirection(attachedFace, true);
               return new ChangeResult(ChangeResultType.APPLIED, (BlockStateChange)null);
            }

            if (this.getHangingType().equals("painting")) {
               hangingItem = (Hanging)this.getWorld().spawn(loc, Painting.class);
               hangingItem.setFacingDirection(this.getDirection(), true);
               return new ChangeResult(ChangeResultType.APPLIED, (BlockStateChange)null);
            }
         } catch (IllegalArgumentException var7) {
         }

         return new ChangeResult(ChangeResultType.SKIPPED, (BlockStateChange)null);
      }
   }

   public class HangingItemActionData {
      public String type;
      public String direction;
   }
}

package me.botsko.prism.actions;

import com.helion3.prism.libs.elixr.TypeUtils;
import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.appliers.ChangeResult;
import me.botsko.prism.appliers.ChangeResultType;
import me.botsko.prism.events.BlockStateChange;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.material.Sign;

public class SignAction extends GenericAction {
   protected Block block;
   protected SignChangeActionData actionData;

   public void setBlock(Block block, String[] lines) {
      this.actionData = new SignChangeActionData();
      if (block != null) {
         this.actionData.sign_type = block.getType().name();
         Sign sign = (Sign)block.getState().getData();
         this.actionData.facing = sign.getFacing();
         this.block = block;
         this.world_name = block.getWorld().getName();
         this.x = (double)block.getX();
         this.y = (double)block.getY();
         this.z = (double)block.getZ();
      }

      if (lines != null) {
         this.actionData.lines = lines;
      }

   }

   public void setData(String data) {
      this.data = data;
      if (data != null && !this.data.isEmpty()) {
         this.actionData = (SignChangeActionData)this.gson.fromJson(data, SignChangeActionData.class);
      }

   }

   public void save() {
      this.data = this.gson.toJson((Object)this.actionData);
   }

   public String[] getLines() {
      return this.actionData.lines;
   }

   public Material getSignType() {
      if (this.actionData.sign_type != null) {
         Material m = Material.valueOf(this.actionData.sign_type);
         if (m != null) {
            return m;
         }
      }

      return Material.SIGN;
   }

   public BlockFace getFacing() {
      return this.actionData.facing;
   }

   public String getNiceName() {
      String name = "sign (";
      if (this.actionData.lines != null && this.actionData.lines.length > 0) {
         name = name + TypeUtils.join(this.actionData.lines, ", ");
      } else {
         name = name + "no text";
      }

      name = name + ")";
      return name;
   }

   public ChangeResult applyRestore(Player player, QueryParameters parameters, boolean is_preview) {
      Block block = this.getWorld().getBlockAt(this.getLoc());
      if (block.getType().equals(Material.AIR) || block.getType().equals(Material.SIGN_POST) || block.getType().equals(Material.SIGN) || block.getType().equals(Material.WALL_SIGN)) {
         if (block.getType().equals(Material.AIR)) {
            block.setType(this.getSignType());
         }

         if (block.getState().getData() instanceof Sign) {
            Sign s = (Sign)block.getState().getData();
            s.setFacingDirection(this.getFacing());
         }

         if (block.getState() instanceof org.bukkit.block.Sign) {
            String[] lines = this.getLines();
            org.bukkit.block.Sign sign = (org.bukkit.block.Sign)block.getState();
            int i = 0;
            if (lines != null && lines.length > 0) {
               String[] arr$ = lines;
               int len$ = lines.length;

               for(int i$ = 0; i$ < len$; ++i$) {
                  String line = arr$[i$];
                  sign.setLine(i, line);
                  ++i;
               }
            }

            sign.update();
            return new ChangeResult(ChangeResultType.APPLIED, (BlockStateChange)null);
         }
      }

      return new ChangeResult(ChangeResultType.SKIPPED, (BlockStateChange)null);
   }

   public class SignChangeActionData {
      public String[] lines;
      public String sign_type;
      public BlockFace facing;
   }
}

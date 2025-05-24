package me.botsko.prism.appliers;

import com.helion3.prism.libs.elixr.EntityUtils;
import java.util.ArrayList;
import java.util.List;
import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.commandlibs.Flag;
import me.botsko.prism.utils.BlockUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class Rollback extends Preview {
   public Rollback(Prism plugin, CommandSender sender, List results, QueryParameters parameters, ApplierCallback callback) {
      super(plugin, sender, results, parameters, callback);
   }

   public void preview() {
      this.is_preview = true;
      this.apply();
   }

   public void apply() {
      if (this.player != null) {
         ArrayList drained;
         if (this.plugin.getConfig().getBoolean("prism.appliers.remove-fire-on-burn-rollback") && this.parameters.getActionTypes().containsKey("block-burn") && !this.parameters.hasFlag(Flag.NO_EXT)) {
            drained = BlockUtils.extinguish(this.player.getLocation(), this.parameters.getRadius());
            if (drained != null && !drained.isEmpty()) {
               this.player.sendMessage(Prism.messenger.playerHeaderMsg("Extinguishing fire!" + ChatColor.GRAY + " Like a boss."));
            }
         }

         if (this.plugin.getConfig().getBoolean("prism.appliers.remove-drops-on-explode-rollback") && (this.parameters.getActionTypes().containsKey("tnt-explode") || this.parameters.getActionTypes().containsKey("creeper-explode")) && !this.parameters.hasFlag(Flag.NO_ITEMCLEAR)) {
            int removed = EntityUtils.removeNearbyItemDrops(this.player, this.parameters.getRadius());
            if (removed > 0) {
               this.player.sendMessage(Prism.messenger.playerHeaderMsg("Removed " + removed + " drops in affected area." + ChatColor.GRAY + " Like a boss."));
            }
         }

         drained = null;
         if (this.parameters.hasFlag(Flag.DRAIN)) {
            drained = BlockUtils.drain(this.player.getLocation(), this.parameters.getRadius());
         }

         if (this.parameters.hasFlag(Flag.DRAIN_LAVA)) {
            drained = BlockUtils.drainlava(this.player.getLocation(), this.parameters.getRadius());
         }

         if (this.parameters.hasFlag(Flag.DRAIN_WATER)) {
            drained = BlockUtils.drainwater(this.player.getLocation(), this.parameters.getRadius());
         }

         if (drained != null && drained.size() > 0) {
            this.player.sendMessage(Prism.messenger.playerHeaderMsg("Draining liquid!" + ChatColor.GRAY + " Like a boss."));
         }
      }

      super.apply();
   }
}

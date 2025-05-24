package me.botsko.prism.wands;

import java.util.Iterator;
import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.ActionsQuery;
import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.actionlibs.QueryResult;
import me.botsko.prism.appliers.PrismApplierCallback;
import me.botsko.prism.appliers.PrismProcessType;
import me.botsko.prism.appliers.Rollback;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class RollbackWand extends QueryWandBase implements Wand {
   protected boolean item_given = false;

   public RollbackWand(Prism plugin) {
      super(plugin);
   }

   public void playerLeftClick(Player player, Location loc) {
      if (loc != null) {
         this.rollback(player, loc);
      }

   }

   public void playerRightClick(Player player, Location loc) {
      if (loc != null) {
         this.rollback(player, loc);
      }

   }

   protected void rollback(Player player, Location loc) {
      Block block = loc.getBlock();
      this.plugin.eventTimer.recordTimedEvent("rollback wand used");

      QueryParameters params;
      try {
         params = this.parameters.clone();
      } catch (CloneNotSupportedException var9) {
         params = new QueryParameters();
         player.sendMessage(Prism.messenger.playerError("Error retrieving parameters. Checking with default parameters."));
      }

      params.setWorld(player.getWorld().getName());
      params.setSpecificBlockLocation(block.getLocation());
      params.setLimit(1);
      params.setProcessType(PrismProcessType.ROLLBACK);
      boolean timeDefault = false;
      Iterator i$ = params.getDefaultsUsed().iterator();

      while(i$.hasNext()) {
         String _default = (String)i$.next();
         if (_default.startsWith("t:")) {
            timeDefault = true;
         }
      }

      if (timeDefault) {
         params.setIgnoreTime(true);
      }

      ActionsQuery aq = new ActionsQuery(this.plugin);
      QueryResult results = aq.lookup(params, player);
      if (!results.getActionResults().isEmpty()) {
         Rollback rb = new Rollback(this.plugin, player, results.getActionResults(), params, new PrismApplierCallback());
         rb.apply();
      } else {
         String space_name = block.getType().equals(Material.AIR) ? "space" : block.getType().toString().replaceAll("_", " ").toLowerCase() + (block.getType().toString().endsWith("BLOCK") ? "" : " block");
         player.sendMessage(Prism.messenger.playerError("Nothing to rollback for this " + space_name + " found."));
      }

   }

   public void playerRightClick(Player player, Entity entity) {
   }

   public void setItemWasGiven(boolean given) {
      this.item_given = given;
   }

   public boolean itemWasGiven() {
      return this.item_given;
   }
}

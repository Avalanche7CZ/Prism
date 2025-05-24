package me.botsko.prism.wands;

import com.helion3.prism.libs.elixr.BlockUtils;
import java.util.ArrayList;
import java.util.Iterator;
import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.ActionMessage;
import me.botsko.prism.actionlibs.ActionsQuery;
import me.botsko.prism.actionlibs.MatchRule;
import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.actionlibs.QueryResult;
import me.botsko.prism.actions.Handler;
import me.botsko.prism.commandlibs.Flag;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class InspectorWand extends QueryWandBase implements Wand {
   public InspectorWand(Prism plugin) {
      super(plugin);
   }

   public void playerLeftClick(Player player, Location loc) {
      this.showLocationHistory(player, loc);
   }

   public void playerRightClick(Player player, Location loc) {
      this.showLocationHistory(player, loc);
   }

   protected void showLocationHistory(final Player player, final Location loc) {
      final Block block = loc.getBlock();
      this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, new Runnable() {
         public void run() {
            QueryParameters params;
            try {
               params = InspectorWand.this.parameters.clone();
            } catch (CloneNotSupportedException var10) {
               params = new QueryParameters();
               player.sendMessage(Prism.messenger.playerError("Error retrieving parameters. Checking with default parameters."));
            }

            params.setWorld(player.getWorld().getName());
            params.setSpecificBlockLocation(loc);
            Block sibling = BlockUtils.getSiblingForDoubleLengthBlock(block);
            if (sibling != null) {
               params.addSpecificBlockLocation(sibling.getLocation());
            }

            Iterator i$;
            String ignore;
            if (params.getActionTypes().size() == 0) {
               ArrayList ignoreActions = (ArrayList)InspectorWand.this.plugin.getConfig().getList("prism.wands.inspect.ignore-actions");
               if (ignoreActions != null && !ignoreActions.isEmpty()) {
                  i$ = ignoreActions.iterator();

                  while(i$.hasNext()) {
                     ignore = (String)i$.next();
                     params.addActionType(ignore, MatchRule.EXCLUDE);
                  }
               }
            }

            boolean timeDefault = false;
            i$ = params.getDefaultsUsed().iterator();

            while(i$.hasNext()) {
               ignore = (String)i$.next();
               if (ignore.startsWith("t:")) {
                  timeDefault = true;
               }
            }

            if (timeDefault) {
               params.setIgnoreTime(true);
            }

            ActionsQuery aq = new ActionsQuery(InspectorWand.this.plugin);
            QueryResult results = aq.lookup(params, player);
            String blockname;
            if (!results.getActionResults().isEmpty()) {
               blockname = Prism.getItems().getAlias(block.getTypeId(), block.getData());
               player.sendMessage(Prism.messenger.playerHeaderMsg(ChatColor.GOLD + "--- Inspecting " + blockname + " at " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() + " ---"));
               if (results.getActionResults().size() > 5) {
                  player.sendMessage(Prism.messenger.playerHeaderMsg("Showing " + results.getTotalResults() + " results. Page 1 of " + results.getTotal_pages()));
               }

               ActionMessage am;
               for(Iterator i$x = results.getPaginatedActionResults().iterator(); i$x.hasNext(); player.sendMessage(Prism.messenger.playerMsg(am.getMessage()))) {
                  Handler a = (Handler)i$x.next();
                  am = new ActionMessage(a);
                  if (InspectorWand.this.parameters.hasFlag(Flag.EXTENDED) || InspectorWand.this.plugin.getConfig().getBoolean("prism.messenger.always-show-extended")) {
                     am.showExtended();
                  }
               }
            } else {
               blockname = block.getType().equals(Material.AIR) ? "space" : block.getType().toString().replaceAll("_", " ").toLowerCase() + (block.getType().toString().endsWith("BLOCK") ? "" : " block");
               player.sendMessage(Prism.messenger.playerError("No history for this " + blockname + " found."));
            }

         }
      });
   }

   public void playerRightClick(Player player, Entity entity) {
   }
}

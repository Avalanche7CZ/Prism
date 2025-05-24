package me.botsko.prism.monitors;

import com.helion3.prism.libs.elixr.TypeUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.ActionsQuery;
import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.actionlibs.QueryResult;
import me.botsko.prism.utils.MiscUtils;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;

public class OreMonitor {
   private final int threshold_max = 100;
   private int threshold = 1;
   private final Prism plugin;
   protected Player player;
   protected Block block;

   public OreMonitor(Prism plugin) {
      this.plugin = plugin;
   }

   public void processAlertsFromBlock(final Player player, final Block block) {
      if (this.plugin.getConfig().getBoolean("prism.alerts.ores.enabled")) {
         if (player != null && player.getGameMode() != null && !player.getGameMode().equals(GameMode.CREATIVE)) {
            if (block != null && this.isWatched(block) && !this.plugin.alertedBlocks.containsKey(block.getLocation())) {
               this.threshold = 1;
               ArrayList matchingBlocks = new ArrayList();
               ArrayList foundores = this.findNeighborBlocks(block.getType(), block, matchingBlocks);
               if (!foundores.isEmpty()) {
                  BlockState state = block.getState();
                  block.setType(Material.AIR);
                  int light = block.getLightLevel();
                  light = light > 0 ? Math.round((float)((light & 255) * 100)) / 15 : 0;
                  block.setType(state.getType());
                  String count = foundores.size() + (foundores.size() >= 100 ? "+" : "");
                  final String msg = this.getOreColor(block) + player.getName() + " found " + count + " " + this.getOreNiceName(block) + " " + light + "% light";
                  this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, new Runnable() {
                     public void run() {
                        boolean wasplaced = false;
                        QueryParameters params = new QueryParameters();
                        params.setWorld(player.getWorld().getName());
                        params.addSpecificBlockLocation(block.getLocation());
                        params.addActionType("block-place");
                        ActionsQuery aq = new ActionsQuery(OreMonitor.this.plugin);
                        QueryResult results = aq.lookup(params, player);
                        if (!results.getActionResults().isEmpty()) {
                           wasplaced = true;
                        }

                        if (!wasplaced) {
                           OreMonitor.this.plugin.alertPlayers((Player)null, TypeUtils.colorize(msg));
                           if (OreMonitor.this.plugin.getConfig().getBoolean("prism.alerts.ores.log-to-console")) {
                              Prism.log(msg);
                           }

                           List commands = OreMonitor.this.plugin.getConfig().getStringList("prism.alerts.ores.log-commands");
                           MiscUtils.dispatchAlert(msg, commands);
                        }

                     }
                  });
               }
            }

         }
      }
   }

   protected String getOreColor(Block block) {
      return this.isWatched(block) ? (String)Prism.getAlertedOres().get("" + block.getTypeId()) : "&f";
   }

   protected String getOreNiceName(Block block) {
      return block.getType().toString().replace("_", " ").toLowerCase().replace("glowing", " ");
   }

   protected boolean isWatched(Block block) {
      return Prism.getAlertedOres().containsKey(block.getTypeId() + ":" + block.getData()) || Prism.getAlertedOres().containsKey("" + block.getTypeId());
   }

   private ArrayList findNeighborBlocks(Material type, Block currBlock, ArrayList matchingBlocks) {
      if (this.isWatched(currBlock)) {
         matchingBlocks.add(currBlock);
         Date date = new Date();
         this.plugin.alertedBlocks.put(currBlock.getLocation(), date.getTime());

         for(int x = -1; x <= 1; ++x) {
            for(int z = -1; z <= 1; ++z) {
               for(int y = -1; y <= 1; ++y) {
                  Block newblock = currBlock.getRelative(x, y, z);
                  if (newblock.getType() == type && !matchingBlocks.contains(newblock)) {
                     ++this.threshold;
                     if (this.threshold <= 100) {
                        this.findNeighborBlocks(type, newblock, matchingBlocks);
                     }
                  }
               }
            }
         }
      }

      return matchingBlocks;
   }
}

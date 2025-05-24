package me.botsko.prism.commands;

import com.helion3.prism.libs.elixr.ChunkUtils;
import java.util.ArrayList;
import java.util.List;
import me.botsko.prism.Prism;
import me.botsko.prism.commandlibs.CallInfo;
import me.botsko.prism.commandlibs.SubHandler;
import org.bukkit.Material;

public class ViewCommand implements SubHandler {
   private final Prism plugin;

   public ViewCommand(Prism plugin) {
      this.plugin = plugin;
   }

   public void handle(CallInfo call) {
      String playerName = call.getPlayer().getName();
      if (call.getArg(1).equals("chunk")) {
         ArrayList blocks;
         if (this.plugin.playerActiveViews.containsKey(playerName)) {
            blocks = (ArrayList)this.plugin.playerActiveViews.get(playerName);
            ChunkUtils.resetPreviewBoundaryBlocks(call.getPlayer(), blocks);
            call.getSender().sendMessage(Prism.messenger.playerHeaderMsg("Reset your current view."));
            this.plugin.playerActiveViews.remove(playerName);
         } else {
            blocks = ChunkUtils.getBoundingBlocksAtY(call.getPlayer().getLocation().getChunk(), call.getPlayer().getLocation().getBlockY());
            ChunkUtils.setPreviewBoundaryBlocks(call.getPlayer(), blocks, Material.GLOWSTONE);
            this.plugin.playerActiveViews.put(playerName, blocks);
            call.getSender().sendMessage(Prism.messenger.playerHeaderMsg("Showing current chunk boundaries."));
         }

      } else {
         call.getSender().sendMessage(Prism.messenger.playerError("Invalid view option. Use /prism ? for help."));
      }
   }

   public List handleComplete(CallInfo call) {
      return null;
   }
}

package me.botsko.prism.commands;

import com.helion3.prism.libs.elixr.TypeUtils;
import java.util.ArrayList;
import java.util.List;
import me.botsko.prism.Prism;
import me.botsko.prism.commandlibs.CallInfo;
import me.botsko.prism.commandlibs.SubHandler;
import me.botsko.prism.events.PrismBlocksExtinguishEvent;
import me.botsko.prism.utils.BlockUtils;

public class ExtinguishCommand implements SubHandler {
   private final Prism plugin;

   public ExtinguishCommand(Prism plugin) {
      this.plugin = plugin;
   }

   public void handle(CallInfo call) {
      int radius = this.plugin.getConfig().getInt("prism.ex.default-radius");
      if (call.getArgs().length == 2) {
         if (!TypeUtils.isNumeric(call.getArg(1))) {
            call.getPlayer().sendMessage(Prism.messenger.playerError("Radius must be a number. Or leave it off to use the default. Use /prism ? for help."));
            return;
         }

         int _tmp_radius = Integer.parseInt(call.getArg(1));
         if (_tmp_radius <= 0) {
            call.getPlayer().sendMessage(Prism.messenger.playerError("Radius must be greater than zero. Or leave it off to use the default. Use /prism ? for help."));
            return;
         }

         if (_tmp_radius > this.plugin.getConfig().getInt("prism.ex.max-radius")) {
            call.getPlayer().sendMessage(Prism.messenger.playerError("Radius exceeds max set in config."));
            return;
         }

         radius = _tmp_radius;
      }

      ArrayList blockStateChanges = BlockUtils.extinguish(call.getPlayer().getLocation(), radius);
      if (blockStateChanges != null && !blockStateChanges.isEmpty()) {
         call.getPlayer().sendMessage(Prism.messenger.playerHeaderMsg("Extinguished nearby fire! Cool!"));
         PrismBlocksExtinguishEvent event = new PrismBlocksExtinguishEvent(blockStateChanges, call.getPlayer(), radius);
         this.plugin.getServer().getPluginManager().callEvent(event);
      } else {
         call.getPlayer().sendMessage(Prism.messenger.playerError("No fires found within that radius to extinguish."));
      }

   }

   public List handleComplete(CallInfo call) {
      return null;
   }
}

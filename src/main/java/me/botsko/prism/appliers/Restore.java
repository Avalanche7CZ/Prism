package me.botsko.prism.appliers;

import java.util.List;
import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.QueryParameters;
import org.bukkit.command.CommandSender;

public class Restore extends Preview {
   public Restore(Prism plugin, CommandSender sender, List results, QueryParameters parameters, ApplierCallback callback) {
      super(plugin, sender, results, parameters, callback);
   }

   public void preview() {
      this.is_preview = true;
      this.apply();
   }
}

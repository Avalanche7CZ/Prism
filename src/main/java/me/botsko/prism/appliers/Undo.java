package me.botsko.prism.appliers;

import java.util.List;
import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.QueryParameters;
import org.bukkit.entity.Player;

public class Undo extends Preview {
   public Undo(Prism plugin, Player player, List results, QueryParameters parameters, ApplierCallback callback) {
      super(plugin, player, results, parameters, callback);
   }

   public void preview() {
      this.player.sendMessage(Prism.messenger.playerError("You can't preview an undo."));
   }
}

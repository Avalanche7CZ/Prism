package me.botsko.prism.wands;

import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.appliers.PrismProcessType;
import me.botsko.prism.commandlibs.PreprocessArgs;
import org.bukkit.entity.Player;

public abstract class QueryWandBase extends WandBase {
   protected QueryParameters parameters = new QueryParameters();
   protected final Prism plugin;

   public QueryWandBase(Prism plugin) {
      this.plugin = plugin;
   }

   public boolean setParameters(Player sender, String[] args, int argStart) {
      PrismProcessType processType = this instanceof RollbackWand ? PrismProcessType.ROLLBACK : (this instanceof RestoreWand ? PrismProcessType.RESTORE : (this instanceof InspectorWand ? PrismProcessType.LOOKUP : PrismProcessType.LOOKUP));
      QueryParameters params = PreprocessArgs.process(this.plugin, sender, args, processType, argStart, false, true);
      if (params == null) {
         return false;
      } else {
         params.resetMinMaxVectors();
         this.parameters = params;
         return true;
      }
   }

   public QueryParameters getParameters() {
      return this.parameters;
   }
}

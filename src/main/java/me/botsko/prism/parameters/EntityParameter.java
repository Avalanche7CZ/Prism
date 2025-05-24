package me.botsko.prism.parameters;

import java.util.regex.Pattern;
import me.botsko.prism.actionlibs.MatchRule;
import me.botsko.prism.actionlibs.QueryParameters;
import org.bukkit.command.CommandSender;

public class EntityParameter extends SimplePrismParameterHandler {
   public EntityParameter() {
      super("Entity", Pattern.compile("[~|!]?[\\w,]+"), "e");
   }

   public void process(QueryParameters query, String alias, String input, CommandSender sender) {
      MatchRule match = MatchRule.INCLUDE;
      if (input.startsWith("!")) {
         match = MatchRule.EXCLUDE;
      }

      String[] entityNames = input.split(",");
      if (entityNames.length > 0) {
         String[] arr$ = entityNames;
         int len$ = entityNames.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            String entityName = arr$[i$];
            query.addEntity(entityName.replace("!", ""), match);
         }
      }

   }
}

package me.botsko.prism.parameters;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import me.botsko.prism.actionlibs.MatchRule;
import me.botsko.prism.actionlibs.QueryParameters;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PlayerParameter extends SimplePrismParameterHandler {
   public PlayerParameter() {
      super("Player", Pattern.compile("[~|!]?[\\w,]+"), "p");
   }

   public void process(QueryParameters query, String alias, String input, CommandSender sender) {
      MatchRule match = MatchRule.INCLUDE;
      if (input.startsWith("!")) {
         match = MatchRule.EXCLUDE;
         input = input.replace("!", "");
      } else if (input.startsWith("~")) {
         match = MatchRule.PARTIAL;
         input = input.replace("~", "");
      }

      String[] playerNames = input.split(",");
      if (playerNames.length > 0) {
         String[] arr$ = playerNames;
         int len$ = playerNames.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            String playerName = arr$[i$];
            query.addPlayerName(playerName, match);
         }
      }

   }

   protected List tabComplete(String alias, String partialParameter, CommandSender sender) {
      String prefix = "";
      String partialName = partialParameter;
      if (partialParameter.startsWith("!") || partialParameter.startsWith("~")) {
         prefix = partialParameter.substring(0, 1);
         partialName = partialParameter.substring(1);
      }

      int end = partialName.lastIndexOf(44);
      if (end != -1) {
         prefix = prefix + partialName.substring(0, end) + ",";
         partialName = partialName.substring(end + 1);
      }

      partialName = partialName.toLowerCase();
      List completions = new ArrayList();
      Iterator i$ = Bukkit.getOnlinePlayers().iterator();

      while(i$.hasNext()) {
         Player player = (Player)i$.next();
         if (player.getName().toLowerCase().startsWith(partialName)) {
            completions.add(prefix + player.getName());
         }
      }

      return completions;
   }
}

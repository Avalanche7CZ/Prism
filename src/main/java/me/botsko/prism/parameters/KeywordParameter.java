package me.botsko.prism.parameters;

import java.util.regex.Pattern;
import me.botsko.prism.actionlibs.QueryParameters;
import org.bukkit.command.CommandSender;

public class KeywordParameter extends SimplePrismParameterHandler {
   public KeywordParameter() {
      super("Keyword", Pattern.compile("[^\\s]+"), "k");
   }

   public void process(QueryParameters query, String alias, String input, CommandSender sender) {
      query.setKeyword(input);
   }
}

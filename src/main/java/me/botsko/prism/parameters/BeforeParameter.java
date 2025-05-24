package me.botsko.prism.parameters;

import java.util.regex.Pattern;
import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.utils.DateUtil;
import org.bukkit.command.CommandSender;

public class BeforeParameter extends SimplePrismParameterHandler {
   public BeforeParameter() {
      super("Before", Pattern.compile("[\\w]+"), "before");
   }

   public void process(QueryParameters query, String alias, String input, CommandSender sender) {
      Long date = DateUtil.translateTimeStringToDate(input);
      if (date != null) {
         query.setBeforeTime(date);
      } else {
         throw new IllegalArgumentException("Date/time for 'before' parameter value not recognized. Try /pr ? for help");
      }
   }
}

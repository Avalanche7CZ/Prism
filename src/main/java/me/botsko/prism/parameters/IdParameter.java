package me.botsko.prism.parameters;

import com.helion3.prism.libs.elixr.TypeUtils;
import java.util.regex.Pattern;
import me.botsko.prism.actionlibs.QueryParameters;
import org.bukkit.command.CommandSender;

public class IdParameter extends SimplePrismParameterHandler {
   public IdParameter() {
      super("ID", Pattern.compile("[\\d,]+"), "id");
   }

   public void process(QueryParameters query, String alias, String input, CommandSender sender) {
      if (!TypeUtils.isNumeric(input)) {
         throw new IllegalArgumentException("ID must be a number. Use /prism ? for help.");
      } else {
         query.setId(Integer.parseInt(input));
      }
   }
}

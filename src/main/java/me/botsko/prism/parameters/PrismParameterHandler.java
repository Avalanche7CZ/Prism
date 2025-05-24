package me.botsko.prism.parameters;

import java.util.List;
import me.botsko.prism.actionlibs.QueryParameters;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permissible;

public interface PrismParameterHandler {
   String getName();

   String[] getHelp();

   boolean applicable(String var1, CommandSender var2);

   void process(QueryParameters var1, String var2, CommandSender var3);

   void defaultTo(QueryParameters var1, CommandSender var2);

   List tabComplete(String var1, CommandSender var2);

   boolean hasPermission(String var1, Permissible var2);
}

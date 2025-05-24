package me.botsko.prism.parameters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import me.botsko.prism.actionlibs.QueryParameters;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permissible;

public abstract class SimplePrismParameterHandler implements PrismParameterHandler {
   private final String name;
   private final Pattern inputMatcher;
   private final Set aliases;
   private String permission;

   public SimplePrismParameterHandler(String name, String... aliases) {
      this(name, (Pattern)null, aliases);
   }

   public SimplePrismParameterHandler(String name, Pattern inputMatcher, String... aliases) {
      this.name = name;
      this.inputMatcher = inputMatcher;
      this.aliases = new HashSet(Arrays.asList(aliases));
      if (this.aliases.isEmpty()) {
         this.aliases.add(this.name.toLowerCase());
      }

      this.permission = "prism.parameters." + name.toLowerCase();
   }

   public final String getName() {
      return this.name;
   }

   public String[] getHelp() {
      return new String[0];
   }

   public String getPermission() {
      return this.permission;
   }

   protected void setPermission(String permission) {
      this.permission = permission;
   }

   protected abstract void process(QueryParameters var1, String var2, String var3, CommandSender var4);

   public final void process(QueryParameters query, String parameter, CommandSender sender) {
      String[] split = parameter.split(":", 2);
      String alias = split[0];
      String input = split[1];
      if (this.inputMatcher != null && !this.inputMatcher.matcher(input).matches()) {
         throw new IllegalArgumentException("Invalid syntax for parameter " + input);
      } else {
         this.process(query, alias, input, sender);
      }
   }

   public final boolean applicable(String parameter, CommandSender sender) {
      String[] split = parameter.split(":", 2);
      if (split.length != 2) {
         return false;
      } else {
         String alias = split[0];
         return this.aliases.contains(alias);
      }
   }

   public void defaultTo(QueryParameters query, CommandSender sender) {
   }

   public final List tabComplete(String partialParameter, CommandSender sender) {
      String[] split = partialParameter.split(":", 2);
      String alias = split[0];
      String input = split[1];
      List completions = this.tabComplete(alias, input, sender);
      if (completions == null) {
         return Collections.emptyList();
      } else {
         List edited = new ArrayList(completions.size());
         Iterator i$ = completions.iterator();

         while(i$.hasNext()) {
            String completion = (String)i$.next();
            edited.add(alias + ":" + completion);
         }

         return edited;
      }
   }

   protected List tabComplete(String alias, String partialParameter, CommandSender sender) {
      return null;
   }

   public final boolean hasPermission(String parameter, Permissible permissible) {
      return permissible == null ? true : permissible.hasPermission(this.permission);
   }
}

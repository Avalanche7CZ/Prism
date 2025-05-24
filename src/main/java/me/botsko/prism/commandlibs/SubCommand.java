package me.botsko.prism.commandlibs;

import org.bukkit.entity.Player;

public final class SubCommand {
   private final String[] commandAliases;
   private String[] permissionNodes;
   private int minArgs;
   private boolean allow_console;
   private SubHandler handler;

   public SubCommand(String[] commandAliases, String[] permissionNodes) {
      this.minArgs = 0;
      this.allow_console = false;
      this.handler = null;
      this.commandAliases = commandAliases;
      this.permissionNodes = permissionNodes;
   }

   public SubCommand(String[] commandAliases, String[] permissionNodes, SubHandler handler) {
      this(commandAliases, permissionNodes);
      this.handler = handler;
   }

   public SubCommand allowConsole() {
      this.allow_console = true;
      return this;
   }

   public boolean isConsoleAllowed() {
      return this.allow_console;
   }

   public int getMinArgs() {
      return this.minArgs;
   }

   public SubCommand setMinArgs(int minArgs) {
      this.minArgs = minArgs;
      return this;
   }

   public SubHandler getHandler() {
      return this.handler;
   }

   public SubCommand setHandler(SubHandler handler) {
      this.handler = handler;
      return this;
   }

   public boolean playerHasPermission(Player player) {
      String[] arr$ = this.permissionNodes;
      int len$ = arr$.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         String node = arr$[i$];
         if (player.hasPermission(node)) {
            return true;
         }

         if (!node.contains("*")) {
            for(int index = node.lastIndexOf(46); index != -1; index = node.lastIndexOf(46)) {
               node = node.substring(0, index);
               if (player.hasPermission(node + ".*")) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   public void setPermNodes(String[] permissionNodes) {
      this.permissionNodes = permissionNodes;
   }

   public String[] getAliases() {
      return this.commandAliases;
   }
}

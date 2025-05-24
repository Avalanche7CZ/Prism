package me.botsko.prism.commands;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.ActionsQuery;
import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.actionlibs.QueryResult;
import me.botsko.prism.appliers.PrismApplierCallback;
import me.botsko.prism.appliers.PrismProcessType;
import me.botsko.prism.appliers.Restore;
import me.botsko.prism.commandlibs.CallInfo;
import me.botsko.prism.commandlibs.PreprocessArgs;
import me.botsko.prism.commandlibs.SubHandler;
import org.bukkit.entity.Player;

public class RestoreCommand implements SubHandler {
   private final Prism plugin;

   public RestoreCommand(Prism plugin) {
      this.plugin = plugin;
   }

   public void handle(final CallInfo call) {
      final QueryParameters parameters = PreprocessArgs.process(this.plugin, call.getSender(), call.getArgs(), PrismProcessType.RESTORE, 1, !this.plugin.getConfig().getBoolean("prism.queries.never-use-defaults"));
      if (parameters != null) {
         parameters.setProcessType(PrismProcessType.RESTORE);
         parameters.setStringFromRawArgs(call.getArgs(), 1);
         ArrayList defaultsUsed = parameters.getDefaultsUsed();
         String defaultsReminder = "";
         if (!defaultsUsed.isEmpty()) {
            defaultsReminder = defaultsReminder + " using defaults:";

            String d;
            for(Iterator i$ = defaultsUsed.iterator(); i$.hasNext(); defaultsReminder = defaultsReminder + " " + d) {
               d = (String)i$.next();
            }
         }

         call.getSender().sendMessage(Prism.messenger.playerSubduedHeaderMsg("Preparing results..." + defaultsReminder));
         this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, new Runnable() {
            public void run() {
               ActionsQuery aq = new ActionsQuery(RestoreCommand.this.plugin);
               final QueryResult results = aq.lookup(parameters, call.getSender());
               if (!results.getActionResults().isEmpty()) {
                  call.getSender().sendMessage(Prism.messenger.playerHeaderMsg("Restoring changes..."));
                  if (call.getSender() instanceof Player) {
                     Player player = (Player)call.getSender();
                     RestoreCommand.this.plugin.notifyNearby(player, parameters.getRadius(), player.getDisplayName() + " is re-applying block changes nearby. Just so you know.");
                  }

                  RestoreCommand.this.plugin.getServer().getScheduler().runTask(RestoreCommand.this.plugin, new Runnable() {
                     public void run() {
                        Restore rs = new Restore(RestoreCommand.this.plugin, call.getSender(), results.getActionResults(), parameters, new PrismApplierCallback());
                        rs.apply();
                     }
                  });
               } else {
                  call.getSender().sendMessage(Prism.messenger.playerError("Nothing found to restore. Try using /prism l (args) first."));
               }

            }
         });
      }
   }

   public List handleComplete(CallInfo call) {
      return PreprocessArgs.complete(call.getSender(), call.getArgs());
   }
}

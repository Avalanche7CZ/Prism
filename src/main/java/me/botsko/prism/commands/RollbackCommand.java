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
import me.botsko.prism.appliers.Rollback;
import me.botsko.prism.commandlibs.CallInfo;
import me.botsko.prism.commandlibs.PreprocessArgs;
import me.botsko.prism.commandlibs.SubHandler;

public class RollbackCommand implements SubHandler {
   private final Prism plugin;

   public RollbackCommand(Prism plugin) {
      this.plugin = plugin;
   }

   public void handle(final CallInfo call) {
      final QueryParameters parameters = PreprocessArgs.process(this.plugin, call.getSender(), call.getArgs(), PrismProcessType.ROLLBACK, 1, !this.plugin.getConfig().getBoolean("prism.queries.never-use-defaults"));
      if (parameters != null) {
         parameters.setProcessType(PrismProcessType.ROLLBACK);
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
               ActionsQuery aq = new ActionsQuery(RollbackCommand.this.plugin);
               final QueryResult results = aq.lookup(parameters, call.getSender());
               if (!results.getActionResults().isEmpty()) {
                  call.getSender().sendMessage(Prism.messenger.playerHeaderMsg("Beginning rollback..."));
                  RollbackCommand.this.plugin.getServer().getScheduler().runTask(RollbackCommand.this.plugin, new Runnable() {
                     public void run() {
                        Rollback rb = new Rollback(RollbackCommand.this.plugin, call.getSender(), results.getActionResults(), parameters, new PrismApplierCallback());
                        rb.apply();
                     }
                  });
               } else {
                  call.getSender().sendMessage(Prism.messenger.playerError("Nothing found to rollback. Try using /prism l (args) first."));
               }

            }
         });
      }
   }

   public List handleComplete(CallInfo call) {
      return PreprocessArgs.complete(call.getSender(), call.getArgs());
   }
}

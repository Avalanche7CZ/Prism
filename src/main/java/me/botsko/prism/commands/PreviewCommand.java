package me.botsko.prism.commands;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.ActionsQuery;
import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.actionlibs.QueryResult;
import me.botsko.prism.appliers.PreviewSession;
import me.botsko.prism.appliers.Previewable;
import me.botsko.prism.appliers.PrismApplierCallback;
import me.botsko.prism.appliers.PrismProcessType;
import me.botsko.prism.appliers.Restore;
import me.botsko.prism.appliers.Rollback;
import me.botsko.prism.commandlibs.CallInfo;
import me.botsko.prism.commandlibs.PreprocessArgs;
import me.botsko.prism.commandlibs.SubHandler;
import me.botsko.prism.utils.MiscUtils;

public class PreviewCommand implements SubHandler {
   private final Prism plugin;
   private final List secondaries;

   public PreviewCommand(Prism plugin) {
      this.plugin = plugin;
      this.secondaries = new ArrayList();
      this.secondaries.add("apply");
      this.secondaries.add("cancel");
      this.secondaries.add("rollback");
      this.secondaries.add("restore");
      this.secondaries.add("rb");
      this.secondaries.add("rs");
   }

   public void handle(final CallInfo call) {
      if (call.getArgs().length >= 2) {
         PreviewSession previewSession;
         if (call.getArg(1).equalsIgnoreCase("apply")) {
            if (this.plugin.playerActivePreviews.containsKey(call.getPlayer().getName())) {
               previewSession = (PreviewSession)this.plugin.playerActivePreviews.get(call.getPlayer().getName());
               previewSession.getPreviewer().apply_preview();
               this.plugin.playerActivePreviews.remove(call.getPlayer().getName());
            } else {
               call.getPlayer().sendMessage(Prism.messenger.playerError("You have no preview pending."));
            }

            return;
         }

         if (call.getArg(1).equalsIgnoreCase("cancel")) {
            if (this.plugin.playerActivePreviews.containsKey(call.getPlayer().getName())) {
               previewSession = (PreviewSession)this.plugin.playerActivePreviews.get(call.getPlayer().getName());
               previewSession.getPreviewer().cancel_preview();
               this.plugin.playerActivePreviews.remove(call.getPlayer().getName());
            } else {
               call.getPlayer().sendMessage(Prism.messenger.playerError("You have no preview pending."));
            }

            return;
         }

         if (this.plugin.playerActivePreviews.containsKey(call.getPlayer().getName())) {
            call.getPlayer().sendMessage(Prism.messenger.playerError("You have an existing preview pending. Please apply or cancel before moving on."));
            return;
         }

         if (call.getArg(1).equalsIgnoreCase("rollback") || call.getArg(1).equalsIgnoreCase("restore") || call.getArg(1).equalsIgnoreCase("rb") || call.getArg(1).equalsIgnoreCase("rs")) {
            final QueryParameters parameters = PreprocessArgs.process(this.plugin, call.getPlayer(), call.getArgs(), PrismProcessType.ROLLBACK, 2, !this.plugin.getConfig().getBoolean("prism.queries.never-use-defaults"));
            if (parameters == null) {
               return;
            } else {
               parameters.setStringFromRawArgs(call.getArgs(), 1);
               if (parameters.getActionTypes().containsKey("world-edit")) {
                  call.getPlayer().sendMessage(Prism.messenger.playerError("Prism does not support previews for WorldEdit rollbacks/restores yet."));
                  return;
               } else {
                  ArrayList defaultsUsed = parameters.getDefaultsUsed();
                  String defaultsReminder = "";
                  if (!defaultsUsed.isEmpty()) {
                     defaultsReminder = defaultsReminder + " using defaults:";

                     String d;
                     for(Iterator i$ = defaultsUsed.iterator(); i$.hasNext(); defaultsReminder = defaultsReminder + " " + d) {
                        d = (String)i$.next();
                     }
                  }

                  call.getPlayer().sendMessage(Prism.messenger.playerSubduedHeaderMsg("Preparing results..." + defaultsReminder));
                  this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, new Runnable() {
                     public void run() {
                        ActionsQuery aq = new ActionsQuery(PreviewCommand.this.plugin);
                        final QueryResult results = aq.lookup(parameters, call.getPlayer());
                        if (call.getArg(1).equalsIgnoreCase("rollback") || call.getArg(1).equalsIgnoreCase("rb")) {
                           parameters.setProcessType(PrismProcessType.ROLLBACK);
                           if (!results.getActionResults().isEmpty()) {
                              call.getPlayer().sendMessage(Prism.messenger.playerHeaderMsg("Beginning preview..."));
                              PreviewCommand.this.plugin.getServer().getScheduler().runTask(PreviewCommand.this.plugin, new Runnable() {
                                 public void run() {
                                    Previewable rs = new Rollback(PreviewCommand.this.plugin, call.getPlayer(), results.getActionResults(), parameters, new PrismApplierCallback());
                                    rs.preview();
                                 }
                              });
                           } else {
                              call.getPlayer().sendMessage(Prism.messenger.playerError("Nothing found to preview."));
                           }
                        }

                        if (call.getArg(1).equalsIgnoreCase("restore") || call.getArg(1).equalsIgnoreCase("rs")) {
                           parameters.setProcessType(PrismProcessType.RESTORE);
                           if (!results.getActionResults().isEmpty()) {
                              call.getPlayer().sendMessage(Prism.messenger.playerHeaderMsg("Beginning preview..."));
                              PreviewCommand.this.plugin.getServer().getScheduler().runTask(PreviewCommand.this.plugin, new Runnable() {
                                 public void run() {
                                    Previewable rs = new Restore(PreviewCommand.this.plugin, call.getPlayer(), results.getActionResults(), parameters, new PrismApplierCallback());
                                    rs.preview();
                                 }
                              });
                           } else {
                              call.getPlayer().sendMessage(Prism.messenger.playerError("Nothing found to preview."));
                           }
                        }

                     }
                  });
                  return;
               }
            }
         }

         call.getPlayer().sendMessage(Prism.messenger.playerError("Invalid command. Check /prism ? for help."));
      }

   }

   public List handleComplete(CallInfo call) {
      return call.getArgs().length == 2 ? MiscUtils.getStartingWith(call.getArg(1), this.secondaries) : PreprocessArgs.complete(call.getSender(), call.getArgs());
   }
}

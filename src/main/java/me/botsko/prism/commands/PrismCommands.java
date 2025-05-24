package me.botsko.prism.commands;

import java.util.List;
import me.botsko.prism.Prism;
import me.botsko.prism.commandlibs.CallInfo;
import me.botsko.prism.commandlibs.Executor;
import me.botsko.prism.commandlibs.SubHandler;

public class PrismCommands extends Executor {
   public PrismCommands(Prism prism) {
      super(prism, "subcommand", "prism");
      this.setupCommands();
   }

   private void setupCommands() {
      final Prism prism = (Prism)this.plugin;
      this.addSub(new String[]{"about", "default"}, "prism.help").allowConsole().setHandler(new AboutCommand(prism));
      this.addSub(new String[]{"lookup", "l"}, "prism.lookup").allowConsole().setMinArgs(1).setHandler(new LookupCommand(prism));
      this.addSub("near", "prism.lookup").setHandler(new NearCommand(prism));
      this.addSub(new String[]{"page", "pg"}, new String[]{"prism.lookup.paginate", "prism.lookup"}).allowConsole().setMinArgs(1).setHandler(new PageCommand(prism));
      this.addSub(new String[]{"wand", "w", "i", "inspect"}, new String[]{"prism.rollback", "prism.restore", "prism.lookup", "prism.wand.inspect", "prism.wand.profile", "prism.wand.rollback", "prism.wand.restore"}).setHandler(new WandCommand(prism));
      this.addSub(new String[]{"setmy"}, new String[]{"prism.setmy.wand"}).setHandler(new SetmyCommand(prism));
      this.addSub(new String[]{"resetmy"}, new String[]{"prism.setmy.wand"}).setHandler(new ResetmyCommand(prism));
      this.addSub("tp", "prism.tp").setMinArgs(1).setHandler(new TeleportCommand(prism));
      this.addSub("ex", "prism.extinguish").setHandler(new ExtinguishCommand(prism));
      this.addSub("drain", "prism.drain").setHandler(new DrainCommand(prism));
      this.addSub(new String[]{"preview", "pv"}, "prism.preview").setMinArgs(1).setHandler(new PreviewCommand(prism));
      this.addSub(new String[]{"report", "rp"}, "prism.report").allowConsole().setHandler(new ReportCommand(prism));
      this.addSub(new String[]{"rollback", "rb"}, "prism.rollback").allowConsole().setMinArgs(1).setHandler(new RollbackCommand(prism));
      this.addSub(new String[]{"restore", "rs"}, "prism.restore").allowConsole().setMinArgs(1).setHandler(new RestoreCommand(prism));
      this.addSub(new String[]{"delete", "purge"}, "prism.delete").allowConsole().setHandler(new DeleteCommand(prism));
      this.addSub("recorder", "prism.recorder").allowConsole().setHandler(new RecorderCommand(prism));
      this.addSub("undo", "prism.rollback").setHandler(new UndoCommand(prism));
      this.addSub(new String[]{"view", "v"}, "prism.view").setMinArgs(1).setHandler(new ViewCommand(prism));
      this.addSub(new String[]{"help", "?"}, "prism.help").allowConsole().setHandler(new HelpCommand());
      this.addSub("params", "prism.help").allowConsole().setHandler(new ParamsCommand());
      this.addSub("actions", "prism.help").allowConsole().setHandler(new ActionsCommand());
      this.addSub("flags", "prism.help").allowConsole().setHandler(new FlagsCommand());
      this.addSub("reload", "prism.reload").allowConsole().setHandler(new SubHandler() {
         public void handle(CallInfo call) {
            prism.reloadConfig();
            call.getSender().sendMessage(Prism.messenger.playerHeaderMsg("Configuration reloaded successfully."));
         }

         public List handleComplete(CallInfo call) {
            return null;
         }
      });
   }
}

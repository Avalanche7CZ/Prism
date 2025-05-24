package me.botsko.prism.actionlibs;

import me.botsko.prism.actions.Handler;
import org.bukkit.ChatColor;

public class ActionMessage {
   protected final Handler a;
   private boolean showExtended = false;
   private int index = 0;

   public ActionMessage(Handler a) {
      this.a = a;
   }

   public void showExtended() {
      this.showExtended = true;
   }

   public void setResultIndex(int index) {
      this.index = index;
   }

   public String getRawMessage() {
      StringBuilder msg = new StringBuilder();
      msg.append(!this.a.getType().doesCreateBlock() && !this.a.getType().getName().equals("item-insert") && !this.a.getType().getName().equals("sign-change") ? "-" : "+");
      msg.append(" #" + this.a.getId());
      msg.append(" " + this.a.getPlayerName());
      msg.append(" " + this.a.getType().getName());
      msg.append(" " + this.a.getBlockId() + ":" + this.a.getBlockSubId());
      if (this.a.getType().getHandler() != null) {
         if (!this.a.getNiceName().isEmpty()) {
            msg.append(" (" + this.a.getNiceName() + ")");
         }
      } else if (this.a.getType().getName().equals("lava-bucket")) {
         msg.append(" (lava)");
      } else if (this.a.getType().getName().equals("water-bucket")) {
         msg.append(" (water)");
      }

      if (this.a.getAggregateCount() > 1) {
         msg.append(" x" + this.a.getAggregateCount());
      }

      msg.append(" " + this.a.getDisplayDate());
      msg.append(" " + this.a.getDisplayTime().toLowerCase());
      msg.append(" - " + this.a.getWorldName() + " @ " + this.a.getX() + " " + this.a.getY() + " " + this.a.getZ());
      return msg.toString();
   }

   public String[] getMessage() {
      String[] msg = new String[1];
      if (this.showExtended) {
         msg = new String[2];
      }

      ChatColor highlight = ChatColor.DARK_AQUA;
      String line1 = "";
      line1 = line1 + this.getPosNegPrefix();
      if (this.index > 0) {
         line1 = line1 + ChatColor.GRAY + " [" + this.index + "] ";
      }

      line1 = line1 + highlight + this.a.getPlayerName();
      line1 = line1 + " " + ChatColor.WHITE + this.a.getType().getNiceDescription();
      if (this.a.getType().getHandler() != null) {
         if (!this.a.getNiceName().isEmpty()) {
            line1 = line1 + " " + highlight + this.a.getNiceName();
         }
      } else if (this.a.getType().getName().equals("lava-bucket")) {
         line1 = line1 + " " + highlight + "lava";
      } else if (this.a.getType().getName().equals("water-bucket")) {
         line1 = line1 + " " + highlight + "water";
      }

      if (this.showExtended) {
         line1 = line1 + " " + this.a.getBlockId() + ":" + this.a.getBlockSubId();
      }

      if (this.a.getAggregateCount() > 1) {
         line1 = line1 + ChatColor.GREEN + " x" + this.a.getAggregateCount();
      }

      if (!this.a.getTimeSince().isEmpty()) {
         line1 = line1 + ChatColor.WHITE + " " + this.a.getTimeSince();
      }

      line1 = line1 + " " + ChatColor.GRAY + "(a:" + this.a.getType().getShortName() + ")";
      String line2 = ChatColor.GRAY + " --";
      line2 = line2 + ChatColor.GRAY + " " + this.a.getId() + " - ";
      if (this.showExtended) {
         line2 = line2 + ChatColor.GRAY + this.a.getDisplayDate();
         line2 = line2 + " " + ChatColor.GRAY + this.a.getDisplayTime().toLowerCase();
         line2 = line2 + " - " + this.a.getWorldName() + " @ " + this.a.getX() + " " + this.a.getY() + " " + this.a.getZ() + " ";
      }

      msg[0] = line1;
      if (this.showExtended) {
         msg[1] = line2;
      }

      return msg;
   }

   protected String getPosNegPrefix() {
      return !this.a.getType().doesCreateBlock() && !this.a.getType().getName().equals("item-insert") && !this.a.getType().getName().equals("sign-change") ? ChatColor.RED + " - " + ChatColor.WHITE : ChatColor.GREEN + " + " + ChatColor.WHITE;
   }
}

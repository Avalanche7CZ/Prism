package me.botsko.prism.appliers;

import java.util.Date;
import org.bukkit.entity.Player;

public class PreviewSession {
   protected final Player player;
   protected final Previewable previewer;
   protected final long queryTime;

   public PreviewSession(Player player, Previewable previewer) {
      this.player = player;
      this.previewer = previewer;
      Date date = new Date();
      this.queryTime = date.getTime();
   }

   public Player getPlayer() {
      return this.player;
   }

   public Previewable getPreviewer() {
      return this.previewer;
   }

   public long getQueryTime() {
      return this.queryTime;
   }
}

package me.botsko.prism.actionlibs;

import java.util.concurrent.LinkedBlockingQueue;
import me.botsko.prism.actions.Handler;

public class RecordingQueue {
   private static final LinkedBlockingQueue queue = new LinkedBlockingQueue();

   public static int getQueueSize() {
      return queue.size();
   }

   public static void addToQueue(Handler a) {
      if (a != null) {
         if (!a.getPlayerName().trim().isEmpty()) {
            a.save();
            queue.add(a);
         }
      }
   }

   public static LinkedBlockingQueue getQueue() {
      return queue;
   }
}

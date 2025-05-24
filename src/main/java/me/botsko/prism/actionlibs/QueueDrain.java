package me.botsko.prism.actionlibs;

import me.botsko.prism.Prism;

public class QueueDrain {
   private final Prism plugin;

   public QueueDrain(Prism plugin) {
      this.plugin = plugin;
   }

   public void forceDrainQueue() {
      Prism.log("Forcing recorder queue to run a new batch before shutdown...");
      RecordingTask recorderTask = new RecordingTask(this.plugin);

      while(!RecordingQueue.getQueue().isEmpty()) {
         Prism.log("Starting drain batch...");
         Prism.log("Current queue size: " + RecordingQueue.getQueue().size());

         try {
            recorderTask.insertActionsIntoDatabase();
         } catch (Exception var3) {
            var3.printStackTrace();
            Prism.log("Stopping queue drain due to caught exception. Queue items lost: " + RecordingQueue.getQueue().size());
            break;
         }

         if (RecordingManager.failedDbConnectionCount > 0) {
            Prism.log("Stopping queue drain due to detected database error. Queue items lost: " + RecordingQueue.getQueue().size());
         }
      }

   }
}

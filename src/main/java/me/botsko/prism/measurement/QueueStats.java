package me.botsko.prism.measurement;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

public class QueueStats {
   protected final ConcurrentSkipListMap perRunRecordingCounts = new ConcurrentSkipListMap();

   public void addRunCount(int count) {
      Date date = new Date();
      long currentTime = date.getTime();
      if (this.perRunRecordingCounts.size() > 5) {
         int i = 0;
         Iterator i$ = this.perRunRecordingCounts.descendingMap().entrySet().iterator();

         while(i$.hasNext()) {
            Map.Entry entry = (Map.Entry)i$.next();
            if (i++ > 5) {
               this.perRunRecordingCounts.remove(entry.getKey());
            }
         }
      }

      this.perRunRecordingCounts.put(currentTime, count);
   }

   public ConcurrentSkipListMap getRecentRunCounts() {
      return this.perRunRecordingCounts;
   }
}

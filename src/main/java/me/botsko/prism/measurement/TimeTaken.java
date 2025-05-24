package me.botsko.prism.measurement;

import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import me.botsko.prism.Prism;

public class TimeTaken {
   protected final Prism plugin;
   protected final TreeMap eventsTimed = new TreeMap();

   public TimeTaken(Prism plugin) {
      this.plugin = plugin;
   }

   protected long getTimestamp() {
      Calendar lCDateTime = Calendar.getInstance();
      return lCDateTime.getTimeInMillis();
   }

   public void recordTimedEvent(String eventname) {
      if (this.plugin.getConfig().getBoolean("prism.debug")) {
         this.eventsTimed.put(this.getTimestamp(), eventname);
      }
   }

   protected void resetEventList() {
      this.eventsTimed.clear();
   }

   protected TreeMap getEventsTimedList() {
      return this.eventsTimed;
   }

   public void printTimeRecord() {
      if (this.plugin.getConfig().getBoolean("prism.debug")) {
         TreeMap timers = this.plugin.eventTimer.getEventsTimedList();
         if (timers.size() > 0) {
            long lastTime = 0L;
            long total = 0L;
            Prism.debug("-- Timer information for last action: --");

            Map.Entry entry;
            for(Iterator i$ = timers.entrySet().iterator(); i$.hasNext(); lastTime = (Long)entry.getKey()) {
               entry = (Map.Entry)i$.next();
               long diff = 0L;
               if (lastTime > 0L) {
                  diff = (Long)entry.getKey() - lastTime;
                  total += diff;
               }

               Prism.debug((String)entry.getValue() + " " + diff + "ms");
            }

            Prism.debug("Total time: " + total + "ms");
         }
      }

      this.plugin.eventTimer.resetEventList();
   }
}

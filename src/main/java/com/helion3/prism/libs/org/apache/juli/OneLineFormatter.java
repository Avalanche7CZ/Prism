package com.helion3.prism.libs.org.apache.juli;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class OneLineFormatter extends Formatter {
   private static final String LINE_SEP = System.getProperty("line.separator");
   private static final String ST_SEP;
   private static final String timeFormat = "dd-MMM-yyyy HH:mm:ss";
   private static final int globalCacheSize = 30;
   private static final int localCacheSize = 5;
   private static final DateFormatCache globalDateCache;
   private static final ThreadLocal localDateCache;

   public String format(LogRecord record) {
      StringBuilder sb = new StringBuilder();
      this.addTimestamp(sb, record.getMillis());
      sb.append(' ');
      sb.append(record.getLevel());
      sb.append(' ');
      sb.append('[');
      sb.append(Thread.currentThread().getName());
      sb.append(']');
      sb.append(' ');
      sb.append(record.getSourceClassName());
      sb.append('.');
      sb.append(record.getSourceMethodName());
      sb.append(' ');
      sb.append(this.formatMessage(record));
      if (record.getThrown() != null) {
         sb.append(ST_SEP);
         StringWriter sw = new StringWriter();
         PrintWriter pw = new PrintWriter(sw);
         record.getThrown().printStackTrace(pw);
         pw.close();
         sb.append(sw.getBuffer());
      }

      sb.append(LINE_SEP);
      return sb.toString();
   }

   protected void addTimestamp(StringBuilder buf, long timestamp) {
      buf.append(((DateFormatCache)localDateCache.get()).getFormat(timestamp));
      long frac = timestamp % 1000L;
      buf.append('.');
      if (frac < 100L) {
         if (frac < 10L) {
            buf.append('0');
            buf.append('0');
         } else {
            buf.append('0');
         }
      }

      buf.append(frac);
   }

   static {
      ST_SEP = LINE_SEP + " ";
      globalDateCache = new DateFormatCache(30, "dd-MMM-yyyy HH:mm:ss", (DateFormatCache)null);
      localDateCache = new ThreadLocal() {
         protected DateFormatCache initialValue() {
            return new DateFormatCache(5, "dd-MMM-yyyy HH:mm:ss", OneLineFormatter.globalDateCache);
         }
      };
   }
}

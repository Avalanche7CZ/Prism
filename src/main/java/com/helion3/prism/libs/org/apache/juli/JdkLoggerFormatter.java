package com.helion3.prism.libs.org.apache.juli;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class JdkLoggerFormatter extends Formatter {
   private static final String LINE_SEP = System.getProperty("line.separator");
   public static final int LOG_LEVEL_TRACE = 400;
   public static final int LOG_LEVEL_DEBUG = 500;
   public static final int LOG_LEVEL_INFO = 800;
   public static final int LOG_LEVEL_WARN = 900;
   public static final int LOG_LEVEL_ERROR = 1000;
   public static final int LOG_LEVEL_FATAL = 1000;

   public String format(LogRecord record) {
      Throwable t = record.getThrown();
      int level = record.getLevel().intValue();
      String name = record.getLoggerName();
      long time = record.getMillis();
      String message = this.formatMessage(record);
      if (name.indexOf(".") >= 0) {
         name = name.substring(name.lastIndexOf(".") + 1);
      }

      StringBuilder buf = new StringBuilder();
      buf.append(time);

      int i;
      for(i = 0; i < 8 - buf.length(); ++i) {
         buf.append(" ");
      }

      switch (level) {
         case 400:
            buf.append(" T ");
            break;
         case 500:
            buf.append(" D ");
            break;
         case 800:
            buf.append(" I ");
            break;
         case 900:
            buf.append(" W ");
            break;
         case 1000:
            buf.append(" E ");
            break;
         default:
            buf.append("   ");
      }

      buf.append(name);
      buf.append(" ");

      for(i = 0; i < 8 - buf.length(); ++i) {
         buf.append(" ");
      }

      buf.append(message);
      if (t != null) {
         buf.append(LINE_SEP);
         StringWriter sw = new StringWriter(1024);
         PrintWriter pw = new PrintWriter(sw);
         t.printStackTrace(pw);
         pw.close();
         buf.append(sw.toString());
      }

      buf.append(LINE_SEP);
      return buf.toString();
   }
}

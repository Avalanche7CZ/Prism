package com.helion3.prism.libs.org.apache.juli;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class VerbatimFormatter extends Formatter {
   private static final String LINE_SEP = System.getProperty("line.separator");

   public String format(LogRecord record) {
      StringBuilder sb = new StringBuilder(record.getMessage());
      sb.append(LINE_SEP);
      return sb.toString();
   }
}

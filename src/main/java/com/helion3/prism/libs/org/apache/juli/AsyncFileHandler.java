package com.helion3.prism.libs.org.apache.juli;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogRecord;

public class AsyncFileHandler extends FileHandler {
   public static final int OVERFLOW_DROP_LAST = 1;
   public static final int OVERFLOW_DROP_FIRST = 2;
   public static final int OVERFLOW_DROP_FLUSH = 3;
   public static final int OVERFLOW_DROP_CURRENT = 4;
   public static final int OVERFLOW_DROP_TYPE = Integer.parseInt(System.getProperty("com.helion3.prism.libs.org.apache.juli.AsyncOverflowDropType", "1"));
   public static final int DEFAULT_MAX_RECORDS = Integer.parseInt(System.getProperty("com.helion3.prism.libs.org.apache.juli.AsyncMaxRecordCount", "10000"));
   public static final int LOGGER_SLEEP_TIME = Integer.parseInt(System.getProperty("com.helion3.prism.libs.org.apache.juli.AsyncLoggerPollInterval", "1000"));
   protected static LinkedBlockingDeque queue;
   protected static LoggerThread logger;
   protected volatile boolean closed;

   public AsyncFileHandler() {
      this((String)null, (String)null, (String)null);
   }

   public AsyncFileHandler(String directory, String prefix, String suffix) {
      super(directory, prefix, suffix);
      this.closed = false;
      this.open();
   }

   public void close() {
      if (!this.closed) {
         this.closed = true;
         super.close();
      }
   }

   protected void open() {
      if (this.closed) {
         this.closed = false;
         super.open();
      }
   }

   public void publish(LogRecord record) {
      if (this.isLoggable(record)) {
         LogEntry entry = new LogEntry(record, this);
         boolean added = false;

         try {
            while(!added && !queue.offer(entry)) {
               switch (OVERFLOW_DROP_TYPE) {
                  case 1:
                     queue.pollLast();
                     break;
                  case 2:
                     queue.pollFirst();
                     break;
                  case 3:
                     added = queue.offer(entry, 1000L, TimeUnit.MILLISECONDS);
                     break;
                  case 4:
                     added = true;
               }
            }
         } catch (InterruptedException var5) {
            Thread.interrupted();
         }

      }
   }

   protected void publishInternal(LogRecord record) {
      super.publish(record);
   }

   static {
      queue = new LinkedBlockingDeque(DEFAULT_MAX_RECORDS);
      logger = new LoggerThread();
      logger.start();
   }

   protected static class LogEntry {
      private LogRecord record;
      private AsyncFileHandler handler;

      public LogEntry(LogRecord record, AsyncFileHandler handler) {
         this.record = record;
         this.handler = handler;
      }

      public boolean flush() {
         if (this.handler.closed) {
            return false;
         } else {
            this.handler.publishInternal(this.record);
            return true;
         }
      }
   }

   protected static class LoggerThread extends Thread {
      protected boolean run = true;

      public LoggerThread() {
         this.setDaemon(true);
         this.setName("AsyncFileHandlerWriter-" + System.identityHashCode(this));
      }

      public void run() {
         while(this.run) {
            try {
               LogEntry entry = (LogEntry)AsyncFileHandler.queue.poll((long)AsyncFileHandler.LOGGER_SLEEP_TIME, TimeUnit.MILLISECONDS);
               if (entry != null) {
                  entry.flush();
               }
            } catch (InterruptedException var2) {
               Thread.interrupted();
            } catch (Exception var3) {
               var3.printStackTrace();
            }
         }

      }
   }
}

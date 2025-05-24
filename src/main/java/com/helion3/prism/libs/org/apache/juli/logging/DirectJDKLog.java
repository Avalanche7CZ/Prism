package com.helion3.prism.libs.org.apache.juli.logging;

import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

class DirectJDKLog implements Log {
   public Logger logger;
   private static final String SIMPLE_FMT = "java.util.logging.SimpleFormatter";
   private static final String SIMPLE_CFG = "com.helion3.prism.libs.org.apache.juli.JdkLoggerConfig";
   private static final String FORMATTER = "com.helion3.prism.libs.org.apache.juli.formatter";

   public DirectJDKLog(String name) {
      this.logger = Logger.getLogger(name);
   }

   public final boolean isErrorEnabled() {
      return this.logger.isLoggable(Level.SEVERE);
   }

   public final boolean isWarnEnabled() {
      return this.logger.isLoggable(Level.WARNING);
   }

   public final boolean isInfoEnabled() {
      return this.logger.isLoggable(Level.INFO);
   }

   public final boolean isDebugEnabled() {
      return this.logger.isLoggable(Level.FINE);
   }

   public final boolean isFatalEnabled() {
      return this.logger.isLoggable(Level.SEVERE);
   }

   public final boolean isTraceEnabled() {
      return this.logger.isLoggable(Level.FINER);
   }

   public final void debug(Object message) {
      this.log(Level.FINE, String.valueOf(message), (Throwable)null);
   }

   public final void debug(Object message, Throwable t) {
      this.log(Level.FINE, String.valueOf(message), t);
   }

   public final void trace(Object message) {
      this.log(Level.FINER, String.valueOf(message), (Throwable)null);
   }

   public final void trace(Object message, Throwable t) {
      this.log(Level.FINER, String.valueOf(message), t);
   }

   public final void info(Object message) {
      this.log(Level.INFO, String.valueOf(message), (Throwable)null);
   }

   public final void info(Object message, Throwable t) {
      this.log(Level.INFO, String.valueOf(message), t);
   }

   public final void warn(Object message) {
      this.log(Level.WARNING, String.valueOf(message), (Throwable)null);
   }

   public final void warn(Object message, Throwable t) {
      this.log(Level.WARNING, String.valueOf(message), t);
   }

   public final void error(Object message) {
      this.log(Level.SEVERE, String.valueOf(message), (Throwable)null);
   }

   public final void error(Object message, Throwable t) {
      this.log(Level.SEVERE, String.valueOf(message), t);
   }

   public final void fatal(Object message) {
      this.log(Level.SEVERE, String.valueOf(message), (Throwable)null);
   }

   public final void fatal(Object message, Throwable t) {
      this.log(Level.SEVERE, String.valueOf(message), t);
   }

   private void log(Level level, String msg, Throwable ex) {
      if (this.logger.isLoggable(level)) {
         Throwable dummyException = new Throwable();
         StackTraceElement[] locations = dummyException.getStackTrace();
         String cname = "unknown";
         String method = "unknown";
         if (locations != null && locations.length > 2) {
            StackTraceElement caller = locations[2];
            cname = caller.getClassName();
            method = caller.getMethodName();
         }

         if (ex == null) {
            this.logger.logp(level, cname, method, msg);
         } else {
            this.logger.logp(level, cname, method, msg, ex);
         }
      }

   }

   static void release() {
   }

   static Log getInstance(String name) {
      return new DirectJDKLog(name);
   }

   static {
      if (System.getProperty("java.util.logging.config.class") == null && System.getProperty("java.util.logging.config.file") == null) {
         try {
            Class.forName("com.helion3.prism.libs.org.apache.juli.JdkLoggerConfig").newInstance();
         } catch (Throwable var4) {
         }

         try {
            Formatter fmt = (Formatter)Class.forName(System.getProperty("com.helion3.prism.libs.org.apache.juli.formatter", "java.util.logging.SimpleFormatter")).newInstance();
            Logger root = Logger.getLogger("");
            Handler[] handlers = root.getHandlers();

            for(int i = 0; i < handlers.length; ++i) {
               if (handlers[i] instanceof ConsoleHandler) {
                  handlers[i].setFormatter(fmt);
               }
            }
         } catch (Throwable var5) {
         }
      }

   }
}

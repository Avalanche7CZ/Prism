package com.helion3.prism.libs.org.apache.juli;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class ClassLoaderLogManager extends LogManager {
   public static final String DEBUG_PROPERTY = ClassLoaderLogManager.class.getName() + ".debug";
   protected final Map classLoaderLoggers = new WeakHashMap();
   protected ThreadLocal prefix = new ThreadLocal();
   protected volatile boolean useShutdownHook = true;

   public ClassLoaderLogManager() {
      try {
         Runtime.getRuntime().addShutdownHook(new Cleaner());
      } catch (IllegalStateException var2) {
      }

   }

   public boolean isUseShutdownHook() {
      return this.useShutdownHook;
   }

   public void setUseShutdownHook(boolean useShutdownHook) {
      this.useShutdownHook = useShutdownHook;
   }

   public synchronized boolean addLogger(final Logger logger) {
      String loggerName = logger.getName();
      ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
      ClassLoaderLogInfo info = this.getClassLoaderInfo(classLoader);
      if (info.loggers.containsKey(loggerName)) {
         return false;
      } else {
         info.loggers.put(loggerName, logger);
         final String levelString = this.getProperty(loggerName + ".level");
         if (levelString != null) {
            try {
               AccessController.doPrivileged(new PrivilegedAction() {
                  public Void run() {
                     logger.setLevel(Level.parse(levelString.trim()));
                     return null;
                  }
               });
            } catch (IllegalArgumentException var14) {
            }
         }

         int dotIndex = loggerName.lastIndexOf(46);
         if (dotIndex >= 0) {
            String parentName = loggerName.substring(0, dotIndex);
            Logger.getLogger(parentName);
         }

         LogNode node = info.rootNode.findNode(loggerName);
         node.logger = logger;
         Logger parentLogger = node.findParentLogger();
         if (parentLogger != null) {
            doSetParentLogger(logger, parentLogger);
         }

         node.setParentLogger(logger);
         String handlers = this.getProperty(loggerName + ".handlers");
         if (handlers != null) {
            logger.setUseParentHandlers(false);
            StringTokenizer tok = new StringTokenizer(handlers, ",");

            while(tok.hasMoreTokens()) {
               String handlerName = tok.nextToken().trim();
               Handler handler = null;

               for(ClassLoader current = classLoader; current != null; current = current.getParent()) {
                  info = (ClassLoaderLogInfo)this.classLoaderLoggers.get(current);
                  if (info != null) {
                     handler = (Handler)info.handlers.get(handlerName);
                     if (handler != null) {
                        break;
                     }
                  }
               }

               if (handler != null) {
                  logger.addHandler(handler);
               }
            }
         }

         String useParentHandlersString = this.getProperty(loggerName + ".useParentHandlers");
         if (Boolean.valueOf(useParentHandlersString)) {
            logger.setUseParentHandlers(true);
         }

         return true;
      }
   }

   public synchronized Logger getLogger(String name) {
      ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
      return (Logger)this.getClassLoaderInfo(classLoader).loggers.get(name);
   }

   public synchronized Enumeration getLoggerNames() {
      ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
      return Collections.enumeration(this.getClassLoaderInfo(classLoader).loggers.keySet());
   }

   public String getProperty(String name) {
      String prefix = (String)this.prefix.get();
      String result = null;
      if (prefix != null) {
         result = this.findProperty(prefix + name);
      }

      if (result == null) {
         result = this.findProperty(name);
      }

      if (result != null) {
         result = this.replace(result);
      }

      return result;
   }

   private String findProperty(String name) {
      ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
      ClassLoaderLogInfo info = this.getClassLoaderInfo(classLoader);
      String result = info.props.getProperty(name);
      if (result == null && info.props.isEmpty()) {
         for(ClassLoader current = classLoader.getParent(); current != null; current = current.getParent()) {
            info = (ClassLoaderLogInfo)this.classLoaderLoggers.get(current);
            if (info != null) {
               result = info.props.getProperty(name);
               if (result != null || !info.props.isEmpty()) {
                  break;
               }
            }
         }

         if (result == null) {
            result = super.getProperty(name);
         }
      }

      return result;
   }

   public void readConfiguration() throws IOException, SecurityException {
      this.checkAccess();
      this.readConfiguration(Thread.currentThread().getContextClassLoader());
   }

   public void readConfiguration(InputStream is) throws IOException, SecurityException {
      this.checkAccess();
      this.reset();
      this.readConfiguration(is, Thread.currentThread().getContextClassLoader());
   }

   public void reset() throws SecurityException {
      Thread thread = Thread.currentThread();
      if (!thread.getClass().getName().startsWith("java.util.logging.LogManager$")) {
         ClassLoader classLoader = thread.getContextClassLoader();
         ClassLoaderLogInfo clLogInfo = this.getClassLoaderInfo(classLoader);
         this.resetLoggers(clLogInfo);
         super.reset();
      }
   }

   public void shutdown() {
      Iterator i$ = this.classLoaderLoggers.values().iterator();

      while(i$.hasNext()) {
         ClassLoaderLogInfo clLogInfo = (ClassLoaderLogInfo)i$.next();
         this.resetLoggers(clLogInfo);
      }

   }

   private void resetLoggers(ClassLoaderLogInfo clLogInfo) {
      synchronized(clLogInfo) {
         Iterator i$ = clLogInfo.loggers.values().iterator();

         while(i$.hasNext()) {
            Logger logger = (Logger)i$.next();
            Handler[] handlers = logger.getHandlers();
            Handler[] arr$ = handlers;
            int len$ = handlers.length;

            for(int i$ = 0; i$ < len$; ++i$) {
               Handler handler = arr$[i$];
               logger.removeHandler(handler);
            }
         }

         i$ = clLogInfo.handlers.values().iterator();

         while(i$.hasNext()) {
            Handler handler = (Handler)i$.next();

            try {
               handler.close();
            } catch (Exception var11) {
            }
         }

         clLogInfo.handlers.clear();
      }
   }

   protected ClassLoaderLogInfo getClassLoaderInfo(final ClassLoader classLoader) {
      if (classLoader == null) {
         classLoader = ClassLoader.getSystemClassLoader();
      }

      ClassLoaderLogInfo info = (ClassLoaderLogInfo)this.classLoaderLoggers.get(classLoader);
      if (info == null) {
         AccessController.doPrivileged(new PrivilegedAction() {
            public Void run() {
               try {
                  ClassLoaderLogManager.this.readConfiguration(classLoader);
               } catch (IOException var2) {
               }

               return null;
            }
         });
         info = (ClassLoaderLogInfo)this.classLoaderLoggers.get(classLoader);
      }

      return info;
   }

   protected void readConfiguration(ClassLoader classLoader) throws IOException {
      InputStream is = null;

      ClassLoaderLogInfo info;
      try {
         if (classLoader instanceof URLClassLoader) {
            URL logConfig = ((URLClassLoader)classLoader).findResource("logging.properties");
            if (null != logConfig) {
               if (Boolean.getBoolean(DEBUG_PROPERTY)) {
                  System.err.println(this.getClass().getName() + ".readConfiguration(): " + "Found logging.properties at " + logConfig);
               }

               is = classLoader.getResourceAsStream("logging.properties");
            } else if (Boolean.getBoolean(DEBUG_PROPERTY)) {
               System.err.println(this.getClass().getName() + ".readConfiguration(): " + "Found no logging.properties");
            }
         }
      } catch (AccessControlException var9) {
         info = (ClassLoaderLogInfo)this.classLoaderLoggers.get(ClassLoader.getSystemClassLoader());
         if (info != null) {
            Logger log = (Logger)info.loggers.get("");
            if (log != null) {
               Permission perm = var9.getPermission();
               if (perm instanceof FilePermission && perm.getActions().equals("read")) {
                  log.warning("Reading " + perm.getName() + " is not permitted. See \"per context logging\" in the default catalina.policy file.");
               } else {
                  log.warning("Reading logging.properties is not permitted in some context. See \"per context logging\" in the default catalina.policy file.");
                  log.warning("Original error was: " + var9.getMessage());
               }
            }
         }
      }

      if (is == null && classLoader == ClassLoader.getSystemClassLoader()) {
         String configFileStr = System.getProperty("java.util.logging.config.file");
         if (configFileStr != null) {
            try {
               is = new FileInputStream(this.replace(configFileStr));
            } catch (IOException var8) {
            }
         }

         if (is == null) {
            File defaultFile = new File(new File(System.getProperty("java.home"), "lib"), "logging.properties");

            try {
               is = new FileInputStream(defaultFile);
            } catch (IOException var7) {
            }
         }
      }

      Logger localRootLogger = new RootLogger();
      if (is == null) {
         ClassLoader current = classLoader.getParent();

         ClassLoaderLogInfo info;
         for(info = null; current != null && info == null; current = current.getParent()) {
            info = this.getClassLoaderInfo(current);
         }

         if (info != null) {
            localRootLogger.setParent(info.rootNode.logger);
         }
      }

      info = new ClassLoaderLogInfo(new LogNode((LogNode)null, localRootLogger));
      this.classLoaderLoggers.put(classLoader, info);
      if (is != null) {
         this.readConfiguration((InputStream)is, classLoader);
      }

      this.addLogger(localRootLogger);
   }

   protected void readConfiguration(InputStream is, ClassLoader classLoader) throws IOException {
      ClassLoaderLogInfo info = (ClassLoaderLogInfo)this.classLoaderLoggers.get(classLoader);

      try {
         info.props.load(is);
      } catch (IOException var20) {
         System.err.println("Configuration error");
         var20.printStackTrace();
      } finally {
         try {
            is.close();
         } catch (IOException var18) {
         }

      }

      String rootHandlers = info.props.getProperty(".handlers");
      String handlers = info.props.getProperty("handlers");
      Logger localRootLogger = info.rootNode.logger;
      if (handlers != null) {
         StringTokenizer tok = new StringTokenizer(handlers, ",");

         while(tok.hasMoreTokens()) {
            String handlerName = tok.nextToken().trim();
            String handlerClassName = handlerName;
            String prefix = "";
            if (handlerName.length() > 0) {
               if (Character.isDigit(handlerName.charAt(0))) {
                  int pos = handlerName.indexOf(46);
                  if (pos >= 0) {
                     prefix = handlerName.substring(0, pos + 1);
                     handlerClassName = handlerName.substring(pos + 1);
                  }
               }

               try {
                  this.prefix.set(prefix);
                  Handler handler = (Handler)classLoader.loadClass(handlerClassName).newInstance();
                  this.prefix.set((Object)null);
                  info.handlers.put(handlerName, handler);
                  if (rootHandlers == null) {
                     localRootLogger.addHandler(handler);
                  }
               } catch (Exception var19) {
                  System.err.println("Handler error");
                  var19.printStackTrace();
               }
            }
         }
      }

   }

   protected static void doSetParentLogger(final Logger logger, final Logger parent) {
      AccessController.doPrivileged(new PrivilegedAction() {
         public Void run() {
            logger.setParent(parent);
            return null;
         }
      });
   }

   protected String replace(String str) {
      String result = str;
      int pos_start = str.indexOf("${");
      if (pos_start >= 0) {
         StringBuilder builder = new StringBuilder();

         int pos_end;
         for(pos_end = -1; pos_start >= 0; pos_start = str.indexOf("${", pos_end + 1)) {
            builder.append(str, pos_end + 1, pos_start);
            pos_end = str.indexOf(125, pos_start + 2);
            if (pos_end < 0) {
               pos_end = pos_start - 1;
               break;
            }

            String propName = str.substring(pos_start + 2, pos_end);
            String replacement = propName.length() > 0 ? System.getProperty(propName) : null;
            if (replacement != null) {
               builder.append(replacement);
            } else {
               builder.append(str, pos_start, pos_end + 1);
            }
         }

         builder.append(str, pos_end + 1, str.length());
         result = builder.toString();
      }

      return result;
   }

   protected static class RootLogger extends Logger {
      public RootLogger() {
         super("", (String)null);
      }
   }

   protected static final class ClassLoaderLogInfo {
      final LogNode rootNode;
      final Map loggers = new ConcurrentHashMap();
      final Map handlers = new HashMap();
      final Properties props = new Properties();

      ClassLoaderLogInfo(LogNode rootNode) {
         this.rootNode = rootNode;
      }
   }

   protected static final class LogNode {
      Logger logger;
      protected final Map children;
      protected final LogNode parent;

      LogNode(LogNode parent, Logger logger) {
         this.children = new HashMap();
         this.parent = parent;
         this.logger = logger;
      }

      LogNode(LogNode parent) {
         this(parent, (Logger)null);
      }

      LogNode findNode(String name) {
         LogNode currentNode = this;
         if (this.logger.getName().equals(name)) {
            return this;
         } else {
            LogNode childNode;
            for(; name != null; currentNode = childNode) {
               int dotIndex = name.indexOf(46);
               String nextName;
               if (dotIndex < 0) {
                  nextName = name;
                  name = null;
               } else {
                  nextName = name.substring(0, dotIndex);
                  name = name.substring(dotIndex + 1);
               }

               childNode = (LogNode)currentNode.children.get(nextName);
               if (childNode == null) {
                  childNode = new LogNode(currentNode);
                  currentNode.children.put(nextName, childNode);
               }
            }

            return currentNode;
         }
      }

      Logger findParentLogger() {
         Logger logger = null;

         for(LogNode node = this.parent; node != null && logger == null; node = node.parent) {
            logger = node.logger;
         }

         return logger;
      }

      void setParentLogger(Logger parent) {
         Iterator iter = this.children.values().iterator();

         while(iter.hasNext()) {
            LogNode childNode = (LogNode)iter.next();
            if (childNode.logger == null) {
               childNode.setParentLogger(parent);
            } else {
               ClassLoaderLogManager.doSetParentLogger(childNode.logger, parent);
            }
         }

      }
   }

   private final class Cleaner extends Thread {
      private Cleaner() {
      }

      public void run() {
         if (ClassLoaderLogManager.this.useShutdownHook) {
            ClassLoaderLogManager.this.shutdown();
         }

      }

      // $FF: synthetic method
      Cleaner(Object x1) {
         this();
      }
   }
}

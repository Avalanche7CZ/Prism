package com.helion3.prism.libs.org.apache.juli.logging;

import java.util.Properties;
import java.util.logging.LogManager;

public class LogFactory {
   public static final String FACTORY_PROPERTY = "org.apache.commons.logging.LogFactory";
   public static final String FACTORY_DEFAULT = "org.apache.commons.logging.impl.LogFactoryImpl";
   public static final String FACTORY_PROPERTIES = "commons-logging.properties";
   public static final String HASHTABLE_IMPLEMENTATION_PROPERTY = "org.apache.commons.logging.LogFactory.HashtableImpl";
   private static LogFactory singleton = new LogFactory();
   Properties logConfig = new Properties();

   private LogFactory() {
   }

   void setLogConfig(Properties p) {
      this.logConfig = p;
   }

   public Log getInstance(String name) throws LogConfigurationException {
      return DirectJDKLog.getInstance(name);
   }

   public void release() {
      DirectJDKLog.release();
   }

   public Object getAttribute(String name) {
      return this.logConfig.get(name);
   }

   public String[] getAttributeNames() {
      String[] result = new String[this.logConfig.size()];
      return (String[])this.logConfig.keySet().toArray(result);
   }

   public void removeAttribute(String name) {
      this.logConfig.remove(name);
   }

   public void setAttribute(String name, Object value) {
      this.logConfig.put(name, value);
   }

   public Log getInstance(Class clazz) throws LogConfigurationException {
      return this.getInstance(clazz.getName());
   }

   public static LogFactory getFactory() throws LogConfigurationException {
      return singleton;
   }

   public static Log getLog(Class clazz) throws LogConfigurationException {
      return getFactory().getInstance(clazz);
   }

   public static Log getLog(String name) throws LogConfigurationException {
      return getFactory().getInstance(name);
   }

   public static void release(ClassLoader classLoader) {
      if (!LogManager.getLogManager().getClass().getName().equals("java.util.logging.LogManager")) {
         LogManager.getLogManager().reset();
      }

   }

   public static void releaseAll() {
      singleton.release();
   }

   public static String objectId(Object o) {
      return o == null ? "null" : o.getClass().getName() + "@" + System.identityHashCode(o);
   }
}

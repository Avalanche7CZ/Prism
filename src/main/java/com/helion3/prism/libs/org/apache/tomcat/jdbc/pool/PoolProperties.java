package com.helion3.prism.libs.org.apache.tomcat.jdbc.pool;

import com.helion3.prism.libs.org.apache.juli.logging.Log;
import com.helion3.prism.libs.org.apache.juli.logging.LogFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

public class PoolProperties implements PoolConfiguration, Cloneable, Serializable {
   private static final long serialVersionUID = -8519283440854213745L;
   private static final Log log = LogFactory.getLog(PoolProperties.class);
   public static final int DEFAULT_MAX_ACTIVE = 100;
   protected static AtomicInteger poolCounter = new AtomicInteger(0);
   private volatile Properties dbProperties = new Properties();
   private volatile String url = null;
   private volatile String driverClassName = null;
   private volatile Boolean defaultAutoCommit = null;
   private volatile Boolean defaultReadOnly = null;
   private volatile int defaultTransactionIsolation = -1;
   private volatile String defaultCatalog = null;
   private volatile String connectionProperties;
   private volatile int initialSize = 10;
   private volatile int maxActive = 100;
   private volatile int maxIdle;
   private volatile int minIdle;
   private volatile int maxWait;
   private volatile String validationQuery;
   private volatile int validationQueryTimeout;
   private volatile String validatorClassName;
   private volatile Validator validator;
   private volatile boolean testOnBorrow;
   private volatile boolean testOnReturn;
   private volatile boolean testWhileIdle;
   private volatile int timeBetweenEvictionRunsMillis;
   private volatile int numTestsPerEvictionRun;
   private volatile int minEvictableIdleTimeMillis;
   private volatile boolean accessToUnderlyingConnectionAllowed;
   private volatile boolean removeAbandoned;
   private volatile int removeAbandonedTimeout;
   private volatile boolean logAbandoned;
   private volatile String name;
   private volatile String password;
   private volatile String username;
   private volatile long validationInterval;
   private volatile boolean jmxEnabled;
   private volatile String initSQL;
   private volatile boolean testOnConnect;
   private volatile String jdbcInterceptors;
   private volatile boolean fairQueue;
   private volatile boolean useEquals;
   private volatile int abandonWhenPercentageFull;
   private volatile long maxAge;
   private volatile boolean useLock;
   private volatile InterceptorDefinition[] interceptors;
   private volatile int suspectTimeout;
   private volatile Object dataSource;
   private volatile String dataSourceJNDI;
   private volatile boolean alternateUsernameAllowed;
   private volatile boolean commitOnReturn;
   private volatile boolean rollbackOnReturn;
   private volatile boolean useDisposableConnectionFacade;
   private volatile boolean logValidationErrors;
   private volatile boolean propagateInterruptState;
   private volatile boolean ignoreExceptionOnPreLoad;

   public PoolProperties() {
      this.maxIdle = this.maxActive;
      this.minIdle = this.initialSize;
      this.maxWait = 30000;
      this.validationQueryTimeout = -1;
      this.testOnBorrow = false;
      this.testOnReturn = false;
      this.testWhileIdle = false;
      this.timeBetweenEvictionRunsMillis = 5000;
      this.minEvictableIdleTimeMillis = 60000;
      this.accessToUnderlyingConnectionAllowed = true;
      this.removeAbandoned = false;
      this.removeAbandonedTimeout = 60;
      this.logAbandoned = false;
      this.name = "Tomcat Connection Pool[" + poolCounter.addAndGet(1) + "-" + System.identityHashCode(PoolProperties.class) + "]";
      this.validationInterval = 30000L;
      this.jmxEnabled = true;
      this.testOnConnect = false;
      this.jdbcInterceptors = null;
      this.fairQueue = true;
      this.useEquals = true;
      this.abandonWhenPercentageFull = 0;
      this.maxAge = 0L;
      this.useLock = false;
      this.interceptors = null;
      this.suspectTimeout = 0;
      this.dataSource = null;
      this.dataSourceJNDI = null;
      this.alternateUsernameAllowed = false;
      this.commitOnReturn = false;
      this.rollbackOnReturn = false;
      this.useDisposableConnectionFacade = true;
      this.logValidationErrors = false;
      this.propagateInterruptState = false;
      this.ignoreExceptionOnPreLoad = false;
   }

   public void setAbandonWhenPercentageFull(int percentage) {
      if (percentage < 0) {
         this.abandonWhenPercentageFull = 0;
      } else if (percentage > 100) {
         this.abandonWhenPercentageFull = 100;
      } else {
         this.abandonWhenPercentageFull = percentage;
      }

   }

   public int getAbandonWhenPercentageFull() {
      return this.abandonWhenPercentageFull;
   }

   public boolean isFairQueue() {
      return this.fairQueue;
   }

   public void setFairQueue(boolean fairQueue) {
      this.fairQueue = fairQueue;
   }

   public boolean isAccessToUnderlyingConnectionAllowed() {
      return this.accessToUnderlyingConnectionAllowed;
   }

   public String getConnectionProperties() {
      return this.connectionProperties;
   }

   public Properties getDbProperties() {
      return this.dbProperties;
   }

   public Boolean isDefaultAutoCommit() {
      return this.defaultAutoCommit;
   }

   public String getDefaultCatalog() {
      return this.defaultCatalog;
   }

   public Boolean isDefaultReadOnly() {
      return this.defaultReadOnly;
   }

   public int getDefaultTransactionIsolation() {
      return this.defaultTransactionIsolation;
   }

   public String getDriverClassName() {
      return this.driverClassName;
   }

   public int getInitialSize() {
      return this.initialSize;
   }

   public boolean isLogAbandoned() {
      return this.logAbandoned;
   }

   public int getMaxActive() {
      return this.maxActive;
   }

   public int getMaxIdle() {
      return this.maxIdle;
   }

   public int getMaxWait() {
      return this.maxWait;
   }

   public int getMinEvictableIdleTimeMillis() {
      return this.minEvictableIdleTimeMillis;
   }

   public int getMinIdle() {
      return this.minIdle;
   }

   public String getName() {
      return this.name;
   }

   public int getNumTestsPerEvictionRun() {
      return this.numTestsPerEvictionRun;
   }

   public String getPassword() {
      return this.password;
   }

   public String getPoolName() {
      return this.getName();
   }

   public boolean isRemoveAbandoned() {
      return this.removeAbandoned;
   }

   public int getRemoveAbandonedTimeout() {
      return this.removeAbandonedTimeout;
   }

   public boolean isTestOnBorrow() {
      return this.testOnBorrow;
   }

   public boolean isTestOnReturn() {
      return this.testOnReturn;
   }

   public boolean isTestWhileIdle() {
      return this.testWhileIdle;
   }

   public int getTimeBetweenEvictionRunsMillis() {
      return this.timeBetweenEvictionRunsMillis;
   }

   public String getUrl() {
      return this.url;
   }

   public String getUsername() {
      return this.username;
   }

   public String getValidationQuery() {
      return this.validationQuery;
   }

   public int getValidationQueryTimeout() {
      return this.validationQueryTimeout;
   }

   public void setValidationQueryTimeout(int validationQueryTimeout) {
      this.validationQueryTimeout = validationQueryTimeout;
   }

   public String getValidatorClassName() {
      return this.validatorClassName;
   }

   public Validator getValidator() {
      return this.validator;
   }

   public void setValidator(Validator validator) {
      this.validator = validator;
      if (validator != null) {
         this.validatorClassName = validator.getClass().getName();
      } else {
         this.validatorClassName = null;
      }

   }

   public long getValidationInterval() {
      return this.validationInterval;
   }

   public String getInitSQL() {
      return this.initSQL;
   }

   public boolean isTestOnConnect() {
      return this.testOnConnect;
   }

   public String getJdbcInterceptors() {
      return this.jdbcInterceptors;
   }

   public InterceptorDefinition[] getJdbcInterceptorsAsArray() {
      if (this.interceptors == null) {
         if (this.jdbcInterceptors == null) {
            this.interceptors = new InterceptorDefinition[0];
         } else {
            String[] interceptorValues = this.jdbcInterceptors.split(";");
            InterceptorDefinition[] definitions = new InterceptorDefinition[interceptorValues.length + 1];
            definitions[0] = new InterceptorDefinition(TrapException.class);

            for(int i = 0; i < interceptorValues.length; ++i) {
               int propIndex = interceptorValues[i].indexOf("(");
               int endIndex = interceptorValues[i].indexOf(")");
               if (propIndex >= 0 && endIndex >= 0 && endIndex > propIndex) {
                  String name = interceptorValues[i].substring(0, propIndex).trim();
                  definitions[i + 1] = new InterceptorDefinition(name);
                  String propsAsString = interceptorValues[i].substring(propIndex + 1, interceptorValues[i].length() - 1);
                  String[] props = propsAsString.split(",");

                  for(int j = 0; j < props.length; ++j) {
                     int pidx = props[j].indexOf("=");
                     String propName = props[j].substring(0, pidx).trim();
                     String propValue = props[j].substring(pidx + 1).trim();
                     definitions[i + 1].addProperty(new InterceptorProperty(propName, propValue));
                  }
               } else {
                  definitions[i + 1] = new InterceptorDefinition(interceptorValues[i].trim());
               }
            }

            this.interceptors = definitions;
         }
      }

      return this.interceptors;
   }

   public void setAccessToUnderlyingConnectionAllowed(boolean accessToUnderlyingConnectionAllowed) {
   }

   public void setConnectionProperties(String connectionProperties) {
      this.connectionProperties = connectionProperties;
      getProperties(connectionProperties, this.getDbProperties());
   }

   public void setDbProperties(Properties dbProperties) {
      this.dbProperties = dbProperties;
   }

   public void setDefaultAutoCommit(Boolean defaultAutoCommit) {
      this.defaultAutoCommit = defaultAutoCommit;
   }

   public void setDefaultCatalog(String defaultCatalog) {
      this.defaultCatalog = defaultCatalog;
   }

   public void setDefaultReadOnly(Boolean defaultReadOnly) {
      this.defaultReadOnly = defaultReadOnly;
   }

   public void setDefaultTransactionIsolation(int defaultTransactionIsolation) {
      this.defaultTransactionIsolation = defaultTransactionIsolation;
   }

   public void setDriverClassName(String driverClassName) {
      this.driverClassName = driverClassName;
   }

   public void setInitialSize(int initialSize) {
      this.initialSize = initialSize;
   }

   public void setLogAbandoned(boolean logAbandoned) {
      this.logAbandoned = logAbandoned;
   }

   public void setMaxActive(int maxActive) {
      this.maxActive = maxActive;
   }

   public void setMaxIdle(int maxIdle) {
      this.maxIdle = maxIdle;
   }

   public void setMaxWait(int maxWait) {
      this.maxWait = maxWait;
   }

   public void setMinEvictableIdleTimeMillis(int minEvictableIdleTimeMillis) {
      this.minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
   }

   public void setMinIdle(int minIdle) {
      this.minIdle = minIdle;
   }

   public void setName(String name) {
      this.name = name;
   }

   public void setNumTestsPerEvictionRun(int numTestsPerEvictionRun) {
      this.numTestsPerEvictionRun = numTestsPerEvictionRun;
   }

   public void setPassword(String password) {
      this.password = password;
   }

   public void setRemoveAbandoned(boolean removeAbandoned) {
      this.removeAbandoned = removeAbandoned;
   }

   public void setRemoveAbandonedTimeout(int removeAbandonedTimeout) {
      this.removeAbandonedTimeout = removeAbandonedTimeout;
   }

   public void setTestOnBorrow(boolean testOnBorrow) {
      this.testOnBorrow = testOnBorrow;
   }

   public void setTestWhileIdle(boolean testWhileIdle) {
      this.testWhileIdle = testWhileIdle;
   }

   public void setTestOnReturn(boolean testOnReturn) {
      this.testOnReturn = testOnReturn;
   }

   public void setTimeBetweenEvictionRunsMillis(int timeBetweenEvictionRunsMillis) {
      this.timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
   }

   public void setUrl(String url) {
      this.url = url;
   }

   public void setUsername(String username) {
      this.username = username;
   }

   public void setValidationInterval(long validationInterval) {
      this.validationInterval = validationInterval;
   }

   public void setValidationQuery(String validationQuery) {
      this.validationQuery = validationQuery;
   }

   public void setValidatorClassName(String className) {
      this.validatorClassName = className;
      this.validator = null;
      if (className != null) {
         try {
            Class validatorClass = Class.forName(className);
            this.validator = (Validator)validatorClass.newInstance();
         } catch (ClassNotFoundException var3) {
            log.warn("The class " + className + " cannot be found.", var3);
         } catch (ClassCastException var4) {
            log.warn("The class " + className + " does not implement the Validator interface.", var4);
         } catch (InstantiationException var5) {
            log.warn("An object of class " + className + " cannot be instantiated. Make sure that " + "it includes an implicit or explicit no-arg constructor.", var5);
         } catch (IllegalAccessException var6) {
            log.warn("The class " + className + " or its no-arg constructor are inaccessible.", var6);
         }

      }
   }

   public void setInitSQL(String initSQL) {
      this.initSQL = initSQL;
   }

   public void setTestOnConnect(boolean testOnConnect) {
      this.testOnConnect = testOnConnect;
   }

   public void setJdbcInterceptors(String jdbcInterceptors) {
      this.jdbcInterceptors = jdbcInterceptors;
      this.interceptors = null;
   }

   public String toString() {
      StringBuilder buf = new StringBuilder("ConnectionPool[");

      try {
         String[] fields = DataSourceFactory.ALL_PROPERTIES;
         String[] arr$ = fields;
         int len$ = fields.length;

         label40:
         for(int i$ = 0; i$ < len$; ++i$) {
            String field = arr$[i$];
            String[] prefix = new String[]{"get", "is"};
            int j = 0;

            Method m;
            while(true) {
               if (j >= prefix.length) {
                  continue label40;
               }

               String name = prefix[j] + field.substring(0, 1).toUpperCase(Locale.ENGLISH) + field.substring(1);
               m = null;

               try {
                  m = this.getClass().getMethod(name);
                  break;
               } catch (NoSuchMethodException var12) {
                  ++j;
               }
            }

            buf.append(field);
            buf.append("=");
            if ("password".equals(field)) {
               buf.append("********");
            } else {
               buf.append(m.invoke(this));
            }

            buf.append("; ");
         }
      } catch (Exception var13) {
         log.debug("toString() call failed", var13);
      }

      return buf.toString();
   }

   public static int getPoolCounter() {
      return poolCounter.get();
   }

   public boolean isJmxEnabled() {
      return this.jmxEnabled;
   }

   public void setJmxEnabled(boolean jmxEnabled) {
      this.jmxEnabled = jmxEnabled;
   }

   public Boolean getDefaultAutoCommit() {
      return this.defaultAutoCommit;
   }

   public Boolean getDefaultReadOnly() {
      return this.defaultReadOnly;
   }

   public int getSuspectTimeout() {
      return this.suspectTimeout;
   }

   public void setSuspectTimeout(int seconds) {
      this.suspectTimeout = seconds;
   }

   public boolean isPoolSweeperEnabled() {
      boolean timer = this.getTimeBetweenEvictionRunsMillis() > 0;
      boolean result = timer && this.isRemoveAbandoned() && this.getRemoveAbandonedTimeout() > 0;
      result = result || timer && this.getSuspectTimeout() > 0;
      result = result || timer && this.isTestWhileIdle() && this.getValidationQuery() != null;
      result = result || timer && this.getMinEvictableIdleTimeMillis() > 0;
      return result;
   }

   public boolean isUseEquals() {
      return this.useEquals;
   }

   public void setUseEquals(boolean useEquals) {
      this.useEquals = useEquals;
   }

   public long getMaxAge() {
      return this.maxAge;
   }

   public void setMaxAge(long maxAge) {
      this.maxAge = maxAge;
   }

   public boolean getUseLock() {
      return this.useLock;
   }

   public void setUseLock(boolean useLock) {
      this.useLock = useLock;
   }

   public void setDataSource(Object ds) {
      this.dataSource = ds;
   }

   public Object getDataSource() {
      return this.dataSource;
   }

   public void setDataSourceJNDI(String jndiDS) {
      this.dataSourceJNDI = jndiDS;
   }

   public String getDataSourceJNDI() {
      return this.dataSourceJNDI;
   }

   public static Properties getProperties(String propText, Properties props) {
      if (props == null) {
         props = new Properties();
      }

      if (propText != null) {
         try {
            props.load(new ByteArrayInputStream(propText.replace(';', '\n').getBytes()));
         } catch (IOException var3) {
            throw new RuntimeException(var3);
         }
      }

      return props;
   }

   public boolean isAlternateUsernameAllowed() {
      return this.alternateUsernameAllowed;
   }

   public void setAlternateUsernameAllowed(boolean alternateUsernameAllowed) {
      this.alternateUsernameAllowed = alternateUsernameAllowed;
   }

   public void setCommitOnReturn(boolean commitOnReturn) {
      this.commitOnReturn = commitOnReturn;
   }

   public boolean getCommitOnReturn() {
      return this.commitOnReturn;
   }

   public void setRollbackOnReturn(boolean rollbackOnReturn) {
      this.rollbackOnReturn = rollbackOnReturn;
   }

   public boolean getRollbackOnReturn() {
      return this.rollbackOnReturn;
   }

   public void setUseDisposableConnectionFacade(boolean useDisposableConnectionFacade) {
      this.useDisposableConnectionFacade = useDisposableConnectionFacade;
   }

   public boolean getUseDisposableConnectionFacade() {
      return this.useDisposableConnectionFacade;
   }

   public void setLogValidationErrors(boolean logValidationErrors) {
      this.logValidationErrors = logValidationErrors;
   }

   public boolean getLogValidationErrors() {
      return this.logValidationErrors;
   }

   public boolean getPropagateInterruptState() {
      return this.propagateInterruptState;
   }

   public void setPropagateInterruptState(boolean propagateInterruptState) {
      this.propagateInterruptState = propagateInterruptState;
   }

   public boolean isIgnoreExceptionOnPreLoad() {
      return this.ignoreExceptionOnPreLoad;
   }

   public void setIgnoreExceptionOnPreLoad(boolean ignoreExceptionOnPreLoad) {
      this.ignoreExceptionOnPreLoad = ignoreExceptionOnPreLoad;
   }

   protected Object clone() throws CloneNotSupportedException {
      return super.clone();
   }

   public static class InterceptorProperty {
      String name;
      String value;

      public InterceptorProperty(String name, String value) {
         assert name != null;

         this.name = name;
         this.value = value;
      }

      public String getName() {
         return this.name;
      }

      public String getValue() {
         return this.value;
      }

      public boolean getValueAsBoolean(boolean def) {
         if (this.value == null) {
            return def;
         } else if ("true".equals(this.value)) {
            return true;
         } else {
            return "false".equals(this.value) ? false : def;
         }
      }

      public int getValueAsInt(int def) {
         if (this.value == null) {
            return def;
         } else {
            try {
               int v = Integer.parseInt(this.value);
               return v;
            } catch (NumberFormatException var3) {
               return def;
            }
         }
      }

      public long getValueAsLong(long def) {
         if (this.value == null) {
            return def;
         } else {
            try {
               return Long.parseLong(this.value);
            } catch (NumberFormatException var4) {
               return def;
            }
         }
      }

      public byte getValueAsByte(byte def) {
         if (this.value == null) {
            return def;
         } else {
            try {
               return Byte.parseByte(this.value);
            } catch (NumberFormatException var3) {
               return def;
            }
         }
      }

      public short getValueAsShort(short def) {
         if (this.value == null) {
            return def;
         } else {
            try {
               return Short.parseShort(this.value);
            } catch (NumberFormatException var3) {
               return def;
            }
         }
      }

      public float getValueAsFloat(float def) {
         if (this.value == null) {
            return def;
         } else {
            try {
               return Float.parseFloat(this.value);
            } catch (NumberFormatException var3) {
               return def;
            }
         }
      }

      public double getValueAsDouble(double def) {
         if (this.value == null) {
            return def;
         } else {
            try {
               return Double.parseDouble(this.value);
            } catch (NumberFormatException var4) {
               return def;
            }
         }
      }

      public char getValueAschar(char def) {
         if (this.value == null) {
            return def;
         } else {
            try {
               return this.value.charAt(0);
            } catch (StringIndexOutOfBoundsException var3) {
               return def;
            }
         }
      }

      public int hashCode() {
         return this.name.hashCode();
      }

      public boolean equals(Object o) {
         if (o == this) {
            return true;
         } else if (o instanceof InterceptorProperty) {
            InterceptorProperty other = (InterceptorProperty)o;
            return other.name.equals(this.name);
         } else {
            return false;
         }
      }
   }

   public static class InterceptorDefinition {
      protected String className;
      protected Map properties;
      protected volatile Class clazz;

      public InterceptorDefinition(String className) {
         this.properties = new HashMap();
         this.clazz = null;
         this.className = className;
      }

      public InterceptorDefinition(Class cl) {
         this(cl.getName());
         this.clazz = cl;
      }

      public String getClassName() {
         return this.className;
      }

      public void addProperty(String name, String value) {
         InterceptorProperty p = new InterceptorProperty(name, value);
         this.addProperty(p);
      }

      public void addProperty(InterceptorProperty p) {
         this.properties.put(p.getName(), p);
      }

      public Map getProperties() {
         return this.properties;
      }

      public Class getInterceptorClass() throws ClassNotFoundException {
         if (this.clazz == null) {
            if (this.getClassName().indexOf(".") < 0) {
               if (PoolProperties.log.isDebugEnabled()) {
                  PoolProperties.log.debug("Loading interceptor class:org.apache.tomcat.jdbc.pool.interceptor." + this.getClassName());
               }

               this.clazz = Class.forName("com.helion3.prism.libs.org.apache.tomcat.jdbc.pool.interceptor." + this.getClassName(), true, this.getClass().getClassLoader());
            } else {
               if (PoolProperties.log.isDebugEnabled()) {
                  PoolProperties.log.debug("Loading interceptor class:" + this.getClassName());
               }

               this.clazz = Class.forName(this.getClassName(), true, this.getClass().getClassLoader());
            }
         }

         return this.clazz;
      }
   }
}

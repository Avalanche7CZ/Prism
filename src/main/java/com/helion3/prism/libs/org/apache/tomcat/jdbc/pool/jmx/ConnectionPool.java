package com.helion3.prism.libs.org.apache.tomcat.jdbc.pool.jmx;

import com.helion3.prism.libs.org.apache.juli.logging.Log;
import com.helion3.prism.libs.org.apache.juli.logging.LogFactory;
import com.helion3.prism.libs.org.apache.tomcat.jdbc.pool.PoolConfiguration;
import com.helion3.prism.libs.org.apache.tomcat.jdbc.pool.PoolProperties;
import com.helion3.prism.libs.org.apache.tomcat.jdbc.pool.PoolUtilities;
import com.helion3.prism.libs.org.apache.tomcat.jdbc.pool.Validator;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationListener;

public class ConnectionPool extends NotificationBroadcasterSupport implements ConnectionPoolMBean {
   private static final Log log = LogFactory.getLog(ConnectionPool.class);
   protected com.helion3.prism.libs.org.apache.tomcat.jdbc.pool.ConnectionPool pool = null;
   protected AtomicInteger sequence = new AtomicInteger(0);
   protected ConcurrentLinkedQueue listeners = new ConcurrentLinkedQueue();
   public static final String NOTIFY_INIT = "INIT FAILED";
   public static final String NOTIFY_CONNECT = "CONNECTION FAILED";
   public static final String NOTIFY_ABANDON = "CONNECTION ABANDONED";
   public static final String SLOW_QUERY_NOTIFICATION = "SLOW QUERY";
   public static final String FAILED_QUERY_NOTIFICATION = "FAILED QUERY";
   public static final String SUSPECT_ABANDONED_NOTIFICATION = "SUSPECT CONNETION ABANDONED";
   public static final String POOL_EMPTY = "POOL EMPTY";

   public ConnectionPool(com.helion3.prism.libs.org.apache.tomcat.jdbc.pool.ConnectionPool pool) {
      this.pool = pool;
   }

   public com.helion3.prism.libs.org.apache.tomcat.jdbc.pool.ConnectionPool getPool() {
      return this.pool;
   }

   public PoolConfiguration getPoolProperties() {
      return this.pool.getPoolProperties();
   }

   public MBeanNotificationInfo[] getNotificationInfo() {
      MBeanNotificationInfo[] pres = super.getNotificationInfo();
      MBeanNotificationInfo[] loc = getDefaultNotificationInfo();
      MBeanNotificationInfo[] aug = new MBeanNotificationInfo[pres.length + loc.length];
      if (pres.length > 0) {
         System.arraycopy(pres, 0, aug, 0, pres.length);
      }

      if (loc.length > 0) {
         System.arraycopy(loc, 0, aug, pres.length, loc.length);
      }

      return aug;
   }

   public static MBeanNotificationInfo[] getDefaultNotificationInfo() {
      String[] types = new String[]{"INIT FAILED", "CONNECTION FAILED", "CONNECTION ABANDONED", "SLOW QUERY", "FAILED QUERY", "SUSPECT CONNETION ABANDONED"};
      String name = Notification.class.getName();
      String description = "A connection pool error condition was met.";
      MBeanNotificationInfo info = new MBeanNotificationInfo(types, name, description);
      return new MBeanNotificationInfo[]{info};
   }

   public boolean notify(String type, String message) {
      try {
         Notification n = new Notification(type, this, (long)this.sequence.incrementAndGet(), System.currentTimeMillis(), "[" + type + "] " + message);
         this.sendNotification(n);
         Iterator i$ = this.listeners.iterator();

         while(i$.hasNext()) {
            NotificationListener listener = (NotificationListener)i$.next();
            listener.handleNotification(n, this);
         }

         return true;
      } catch (Exception var6) {
         if (log.isDebugEnabled()) {
            log.debug("Notify failed. Type=" + type + "; Message=" + message, var6);
         }

         return false;
      }
   }

   public void addListener(NotificationListener list) {
      this.listeners.add(list);
   }

   public boolean removeListener(NotificationListener list) {
      return this.listeners.remove(list);
   }

   public int getSize() {
      return this.pool.getSize();
   }

   public int getIdle() {
      return this.pool.getIdle();
   }

   public int getActive() {
      return this.pool.getActive();
   }

   public int getNumIdle() {
      return this.getIdle();
   }

   public int getNumActive() {
      return this.getActive();
   }

   public int getWaitCount() {
      return this.pool.getWaitCount();
   }

   public void checkIdle() {
      this.pool.checkIdle();
   }

   public void checkAbandoned() {
      this.pool.checkAbandoned();
   }

   public void testIdle() {
      this.pool.testAllIdle();
   }

   public String getConnectionProperties() {
      return this.getPoolProperties().getConnectionProperties();
   }

   public Properties getDbProperties() {
      return PoolUtilities.cloneWithoutPassword(this.getPoolProperties().getDbProperties());
   }

   public String getDefaultCatalog() {
      return this.getPoolProperties().getDefaultCatalog();
   }

   public int getDefaultTransactionIsolation() {
      return this.getPoolProperties().getDefaultTransactionIsolation();
   }

   public String getDriverClassName() {
      return this.getPoolProperties().getDriverClassName();
   }

   public int getInitialSize() {
      return this.getPoolProperties().getInitialSize();
   }

   public String getInitSQL() {
      return this.getPoolProperties().getInitSQL();
   }

   public String getJdbcInterceptors() {
      return this.getPoolProperties().getJdbcInterceptors();
   }

   public int getMaxActive() {
      return this.getPoolProperties().getMaxActive();
   }

   public int getMaxIdle() {
      return this.getPoolProperties().getMaxIdle();
   }

   public int getMaxWait() {
      return this.getPoolProperties().getMaxWait();
   }

   public int getMinEvictableIdleTimeMillis() {
      return this.getPoolProperties().getMinEvictableIdleTimeMillis();
   }

   public int getMinIdle() {
      return this.getPoolProperties().getMinIdle();
   }

   public long getMaxAge() {
      return this.getPoolProperties().getMaxAge();
   }

   public String getName() {
      return this.getPoolName();
   }

   public int getNumTestsPerEvictionRun() {
      return this.getPoolProperties().getNumTestsPerEvictionRun();
   }

   public String getPassword() {
      return "Password not available as DataSource/JMX operation.";
   }

   public int getRemoveAbandonedTimeout() {
      return this.getPoolProperties().getRemoveAbandonedTimeout();
   }

   public int getTimeBetweenEvictionRunsMillis() {
      return this.getPoolProperties().getTimeBetweenEvictionRunsMillis();
   }

   public String getUrl() {
      return this.getPoolProperties().getUrl();
   }

   public String getUsername() {
      return this.getPoolProperties().getUsername();
   }

   public long getValidationInterval() {
      return this.getPoolProperties().getValidationInterval();
   }

   public String getValidationQuery() {
      return this.getPoolProperties().getValidationQuery();
   }

   public int getValidationQueryTimeout() {
      return this.getPoolProperties().getValidationQueryTimeout();
   }

   public String getValidatorClassName() {
      return this.getPoolProperties().getValidatorClassName();
   }

   public Validator getValidator() {
      return this.getPoolProperties().getValidator();
   }

   public boolean isAccessToUnderlyingConnectionAllowed() {
      return this.getPoolProperties().isAccessToUnderlyingConnectionAllowed();
   }

   public Boolean isDefaultAutoCommit() {
      return this.getPoolProperties().isDefaultAutoCommit();
   }

   public Boolean isDefaultReadOnly() {
      return this.getPoolProperties().isDefaultReadOnly();
   }

   public boolean isLogAbandoned() {
      return this.getPoolProperties().isLogAbandoned();
   }

   public boolean isPoolSweeperEnabled() {
      return this.getPoolProperties().isPoolSweeperEnabled();
   }

   public boolean isRemoveAbandoned() {
      return this.getPoolProperties().isRemoveAbandoned();
   }

   public int getAbandonWhenPercentageFull() {
      return this.getPoolProperties().getAbandonWhenPercentageFull();
   }

   public boolean isTestOnBorrow() {
      return this.getPoolProperties().isTestOnBorrow();
   }

   public boolean isTestOnConnect() {
      return this.getPoolProperties().isTestOnConnect();
   }

   public boolean isTestOnReturn() {
      return this.getPoolProperties().isTestOnReturn();
   }

   public boolean isTestWhileIdle() {
      return this.getPoolProperties().isTestWhileIdle();
   }

   public Boolean getDefaultAutoCommit() {
      return this.getPoolProperties().getDefaultAutoCommit();
   }

   public Boolean getDefaultReadOnly() {
      return this.getPoolProperties().getDefaultReadOnly();
   }

   public PoolProperties.InterceptorDefinition[] getJdbcInterceptorsAsArray() {
      return this.getPoolProperties().getJdbcInterceptorsAsArray();
   }

   public boolean getUseLock() {
      return this.getPoolProperties().getUseLock();
   }

   public boolean isFairQueue() {
      return this.getPoolProperties().isFairQueue();
   }

   public boolean isJmxEnabled() {
      return this.getPoolProperties().isJmxEnabled();
   }

   public boolean isUseEquals() {
      return this.getPoolProperties().isUseEquals();
   }

   public void setAbandonWhenPercentageFull(int percentage) {
      this.getPoolProperties().setAbandonWhenPercentageFull(percentage);
   }

   public void setAccessToUnderlyingConnectionAllowed(boolean accessToUnderlyingConnectionAllowed) {
      this.getPoolProperties().setAccessToUnderlyingConnectionAllowed(accessToUnderlyingConnectionAllowed);
   }

   public void setDbProperties(Properties dbProperties) {
      this.getPoolProperties().setDbProperties(dbProperties);
   }

   public void setDefaultReadOnly(Boolean defaultReadOnly) {
      this.getPoolProperties().setDefaultReadOnly(defaultReadOnly);
   }

   public void setMaxAge(long maxAge) {
      this.getPoolProperties().setMaxAge(maxAge);
   }

   public void setName(String name) {
      this.getPoolProperties().setName(name);
   }

   public String getPoolName() {
      return this.getPoolProperties().getName();
   }

   public void setConnectionProperties(String connectionProperties) {
      this.getPoolProperties().setConnectionProperties(connectionProperties);
   }

   public void setDefaultAutoCommit(Boolean defaultAutoCommit) {
      this.getPoolProperties().setDefaultAutoCommit(defaultAutoCommit);
   }

   public void setDefaultCatalog(String defaultCatalog) {
      this.getPoolProperties().setDefaultCatalog(defaultCatalog);
   }

   public void setDefaultTransactionIsolation(int defaultTransactionIsolation) {
      this.getPoolProperties().setDefaultTransactionIsolation(defaultTransactionIsolation);
   }

   public void setDriverClassName(String driverClassName) {
      this.getPoolProperties().setDriverClassName(driverClassName);
   }

   public void setFairQueue(boolean fairQueue) {
      this.getPoolProperties().setFairQueue(fairQueue);
   }

   public void setInitialSize(int initialSize) {
      throw new UnsupportedOperationException();
   }

   public void setInitSQL(String initSQL) {
      this.getPoolProperties().setInitSQL(initSQL);
   }

   public void setJdbcInterceptors(String jdbcInterceptors) {
      throw new UnsupportedOperationException();
   }

   public void setJmxEnabled(boolean jmxEnabled) {
      throw new UnsupportedOperationException();
   }

   public void setLogAbandoned(boolean logAbandoned) {
      this.getPoolProperties().setLogAbandoned(logAbandoned);
   }

   public void setMaxActive(int maxActive) {
      this.getPoolProperties().setMaxActive(maxActive);
   }

   public void setMaxIdle(int maxIdle) {
      this.getPoolProperties().setMaxIdle(maxIdle);
   }

   public void setMaxWait(int maxWait) {
      this.getPoolProperties().setMaxWait(maxWait);
   }

   public void setMinEvictableIdleTimeMillis(int minEvictableIdleTimeMillis) {
      boolean wasEnabled = this.getPoolProperties().isPoolSweeperEnabled();
      this.getPoolProperties().setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
      boolean shouldBeEnabled = this.getPoolProperties().isPoolSweeperEnabled();
      if (!wasEnabled && shouldBeEnabled) {
         this.pool.initializePoolCleaner(this.getPoolProperties());
      }

   }

   public void setMinIdle(int minIdle) {
      this.getPoolProperties().setMinIdle(minIdle);
   }

   public void setNumTestsPerEvictionRun(int numTestsPerEvictionRun) {
      this.getPoolProperties().setNumTestsPerEvictionRun(numTestsPerEvictionRun);
   }

   public void setPassword(String password) {
      this.getPoolProperties().setPassword(password);
   }

   public void setRemoveAbandoned(boolean removeAbandoned) {
      boolean wasEnabled = this.getPoolProperties().isPoolSweeperEnabled();
      this.getPoolProperties().setRemoveAbandoned(removeAbandoned);
      boolean shouldBeEnabled = this.getPoolProperties().isPoolSweeperEnabled();
      if (!wasEnabled && shouldBeEnabled) {
         this.pool.initializePoolCleaner(this.getPoolProperties());
      }

   }

   public void setRemoveAbandonedTimeout(int removeAbandonedTimeout) {
      boolean wasEnabled = this.getPoolProperties().isPoolSweeperEnabled();
      this.getPoolProperties().setRemoveAbandonedTimeout(removeAbandonedTimeout);
      boolean shouldBeEnabled = this.getPoolProperties().isPoolSweeperEnabled();
      if (!wasEnabled && shouldBeEnabled) {
         this.pool.initializePoolCleaner(this.getPoolProperties());
      }

   }

   public void setTestOnBorrow(boolean testOnBorrow) {
      this.getPoolProperties().setTestOnBorrow(testOnBorrow);
   }

   public void setTestOnConnect(boolean testOnConnect) {
      this.getPoolProperties().setTestOnConnect(testOnConnect);
   }

   public void setTestOnReturn(boolean testOnReturn) {
      this.getPoolProperties().setTestOnReturn(testOnReturn);
   }

   public void setTestWhileIdle(boolean testWhileIdle) {
      boolean wasEnabled = this.getPoolProperties().isPoolSweeperEnabled();
      this.getPoolProperties().setTestWhileIdle(testWhileIdle);
      boolean shouldBeEnabled = this.getPoolProperties().isPoolSweeperEnabled();
      if (!wasEnabled && shouldBeEnabled) {
         this.pool.initializePoolCleaner(this.getPoolProperties());
      }

   }

   public void setTimeBetweenEvictionRunsMillis(int timeBetweenEvictionRunsMillis) {
      boolean wasEnabled = this.getPoolProperties().isPoolSweeperEnabled();
      this.getPoolProperties().setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
      boolean shouldBeEnabled = this.getPoolProperties().isPoolSweeperEnabled();
      if (!wasEnabled && shouldBeEnabled) {
         this.pool.initializePoolCleaner(this.getPoolProperties());
      }

   }

   public void setUrl(String url) {
      this.getPoolProperties().setUrl(url);
   }

   public void setUseEquals(boolean useEquals) {
      this.getPoolProperties().setUseEquals(useEquals);
   }

   public void setUseLock(boolean useLock) {
      this.getPoolProperties().setUseLock(useLock);
   }

   public void setUsername(String username) {
      this.getPoolProperties().setUsername(username);
   }

   public void setValidationInterval(long validationInterval) {
      this.getPoolProperties().setValidationInterval(validationInterval);
   }

   public void setValidationQuery(String validationQuery) {
      this.getPoolProperties().setValidationQuery(validationQuery);
   }

   public void setValidationQueryTimeout(int validationQueryTimeout) {
      this.getPoolProperties().setValidationQueryTimeout(validationQueryTimeout);
   }

   public void setValidatorClassName(String className) {
      this.getPoolProperties().setValidatorClassName(className);
   }

   public int getSuspectTimeout() {
      return this.getPoolProperties().getSuspectTimeout();
   }

   public void setSuspectTimeout(int seconds) {
      this.getPoolProperties().setSuspectTimeout(seconds);
   }

   public void setDataSource(Object ds) {
      this.getPoolProperties().setDataSource(ds);
   }

   public Object getDataSource() {
      return this.getPoolProperties().getDataSource();
   }

   public void setDataSourceJNDI(String jndiDS) {
      this.getPoolProperties().setDataSourceJNDI(jndiDS);
   }

   public String getDataSourceJNDI() {
      return this.getPoolProperties().getDataSourceJNDI();
   }

   public boolean isAlternateUsernameAllowed() {
      return this.getPoolProperties().isAlternateUsernameAllowed();
   }

   public void setAlternateUsernameAllowed(boolean alternateUsernameAllowed) {
      this.getPoolProperties().setAlternateUsernameAllowed(alternateUsernameAllowed);
   }

   public void setValidator(Validator validator) {
      this.getPoolProperties().setValidator(validator);
   }

   public void setCommitOnReturn(boolean commitOnReturn) {
      this.getPoolProperties().setCommitOnReturn(commitOnReturn);
   }

   public boolean getCommitOnReturn() {
      return this.getPoolProperties().getCommitOnReturn();
   }

   public void setRollbackOnReturn(boolean rollbackOnReturn) {
      this.getPoolProperties().setRollbackOnReturn(rollbackOnReturn);
   }

   public boolean getRollbackOnReturn() {
      return this.getPoolProperties().getRollbackOnReturn();
   }

   public void setUseDisposableConnectionFacade(boolean useDisposableConnectionFacade) {
      this.getPoolProperties().setUseDisposableConnectionFacade(useDisposableConnectionFacade);
   }

   public boolean getUseDisposableConnectionFacade() {
      return this.getPoolProperties().getUseDisposableConnectionFacade();
   }

   public void setLogValidationErrors(boolean logValidationErrors) {
      this.getPoolProperties().setLogValidationErrors(logValidationErrors);
   }

   public boolean getLogValidationErrors() {
      return this.getPoolProperties().getLogValidationErrors();
   }

   public boolean getPropagateInterruptState() {
      return this.getPoolProperties().getPropagateInterruptState();
   }

   public void setPropagateInterruptState(boolean propagateInterruptState) {
      this.getPoolProperties().setPropagateInterruptState(propagateInterruptState);
   }

   public boolean isIgnoreExceptionOnPreLoad() {
      return this.getPoolProperties().isIgnoreExceptionOnPreLoad();
   }

   public void setIgnoreExceptionOnPreLoad(boolean ignoreExceptionOnPreLoad) {
      this.getPoolProperties().setIgnoreExceptionOnPreLoad(ignoreExceptionOnPreLoad);
   }

   public void purge() {
      this.pool.purge();
   }

   public void purgeOnReturn() {
      this.pool.purgeOnReturn();
   }
}

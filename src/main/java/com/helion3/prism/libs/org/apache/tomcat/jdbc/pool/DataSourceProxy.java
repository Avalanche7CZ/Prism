package com.helion3.prism.libs.org.apache.tomcat.jdbc.pool;

import com.helion3.prism.libs.org.apache.juli.logging.Log;
import com.helion3.prism.libs.org.apache.juli.logging.LogFactory;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import javax.sql.XAConnection;

public class DataSourceProxy implements PoolConfiguration {
   private static final Log log = LogFactory.getLog(DataSourceProxy.class);
   protected volatile ConnectionPool pool;
   protected volatile PoolConfiguration poolProperties;

   public DataSourceProxy() {
      this(new PoolProperties());
   }

   public DataSourceProxy(PoolConfiguration poolProperties) {
      this.pool = null;
      this.poolProperties = null;
      if (poolProperties == null) {
         throw new NullPointerException("PoolConfiguration can not be null.");
      } else {
         this.poolProperties = poolProperties;
      }
   }

   public boolean isWrapperFor(Class iface) throws SQLException {
      return false;
   }

   public Object unwrap(Class iface) throws SQLException {
      return null;
   }

   public Connection getConnection(String username, String password) throws SQLException {
      if (this.getPoolProperties().isAlternateUsernameAllowed()) {
         return this.pool == null ? this.createPool().getConnection(username, password) : this.pool.getConnection(username, password);
      } else {
         return this.getConnection();
      }
   }

   public PoolConfiguration getPoolProperties() {
      return this.poolProperties;
   }

   public ConnectionPool createPool() throws SQLException {
      return this.pool != null ? this.pool : this.pCreatePool();
   }

   private synchronized ConnectionPool pCreatePool() throws SQLException {
      if (this.pool != null) {
         return this.pool;
      } else {
         this.pool = new ConnectionPool(this.poolProperties);
         return this.pool;
      }
   }

   public Connection getConnection() throws SQLException {
      return this.pool == null ? this.createPool().getConnection() : this.pool.getConnection();
   }

   public Future getConnectionAsync() throws SQLException {
      return this.pool == null ? this.createPool().getConnectionAsync() : this.pool.getConnectionAsync();
   }

   public XAConnection getXAConnection() throws SQLException {
      Connection con = this.getConnection();
      if (con instanceof XAConnection) {
         return (XAConnection)con;
      } else {
         try {
            con.close();
         } catch (Exception var3) {
         }

         throw new SQLException("Connection from pool does not implement javax.sql.XAConnection");
      }
   }

   public XAConnection getXAConnection(String username, String password) throws SQLException {
      Connection con = this.getConnection(username, password);
      if (con instanceof XAConnection) {
         return (XAConnection)con;
      } else {
         try {
            con.close();
         } catch (Exception var5) {
         }

         throw new SQLException("Connection from pool does not implement javax.sql.XAConnection");
      }
   }

   public javax.sql.PooledConnection getPooledConnection() throws SQLException {
      return (javax.sql.PooledConnection)this.getConnection();
   }

   public javax.sql.PooledConnection getPooledConnection(String username, String password) throws SQLException {
      return (javax.sql.PooledConnection)this.getConnection();
   }

   public ConnectionPool getPool() {
      return this.pool;
   }

   public void close() {
      this.close(false);
   }

   public void close(boolean all) {
      try {
         if (this.pool != null) {
            ConnectionPool p = this.pool;
            this.pool = null;
            if (p != null) {
               p.close(all);
            }
         }
      } catch (Exception var3) {
         log.warn("Error duing connection pool closure.", var3);
      }

   }

   public int getPoolSize() throws SQLException {
      ConnectionPool p = this.pool;
      return p == null ? 0 : p.getSize();
   }

   public String toString() {
      return super.toString() + "{" + this.getPoolProperties() + "}";
   }

   public String getPoolName() {
      return this.pool.getName();
   }

   public void setPoolProperties(PoolConfiguration poolProperties) {
      this.poolProperties = poolProperties;
   }

   public void setDriverClassName(String driverClassName) {
      this.poolProperties.setDriverClassName(driverClassName);
   }

   public void setInitialSize(int initialSize) {
      this.poolProperties.setInitialSize(initialSize);
   }

   public void setInitSQL(String initSQL) {
      this.poolProperties.setInitSQL(initSQL);
   }

   public void setLogAbandoned(boolean logAbandoned) {
      this.poolProperties.setLogAbandoned(logAbandoned);
   }

   public void setMaxActive(int maxActive) {
      this.poolProperties.setMaxActive(maxActive);
   }

   public void setMaxIdle(int maxIdle) {
      this.poolProperties.setMaxIdle(maxIdle);
   }

   public void setMaxWait(int maxWait) {
      this.poolProperties.setMaxWait(maxWait);
   }

   public void setMinEvictableIdleTimeMillis(int minEvictableIdleTimeMillis) {
      this.poolProperties.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
   }

   public void setMinIdle(int minIdle) {
      this.poolProperties.setMinIdle(minIdle);
   }

   public void setNumTestsPerEvictionRun(int numTestsPerEvictionRun) {
      this.poolProperties.setNumTestsPerEvictionRun(numTestsPerEvictionRun);
   }

   public void setPassword(String password) {
      this.poolProperties.setPassword(password);
      this.poolProperties.getDbProperties().setProperty("password", this.poolProperties.getPassword());
   }

   public void setRemoveAbandoned(boolean removeAbandoned) {
      this.poolProperties.setRemoveAbandoned(removeAbandoned);
   }

   public void setRemoveAbandonedTimeout(int removeAbandonedTimeout) {
      this.poolProperties.setRemoveAbandonedTimeout(removeAbandonedTimeout);
   }

   public void setTestOnBorrow(boolean testOnBorrow) {
      this.poolProperties.setTestOnBorrow(testOnBorrow);
   }

   public void setTestOnConnect(boolean testOnConnect) {
      this.poolProperties.setTestOnConnect(testOnConnect);
   }

   public void setTestOnReturn(boolean testOnReturn) {
      this.poolProperties.setTestOnReturn(testOnReturn);
   }

   public void setTestWhileIdle(boolean testWhileIdle) {
      this.poolProperties.setTestWhileIdle(testWhileIdle);
   }

   public void setTimeBetweenEvictionRunsMillis(int timeBetweenEvictionRunsMillis) {
      this.poolProperties.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
   }

   public void setUrl(String url) {
      this.poolProperties.setUrl(url);
   }

   public void setUsername(String username) {
      this.poolProperties.setUsername(username);
      this.poolProperties.getDbProperties().setProperty("user", this.getPoolProperties().getUsername());
   }

   public void setValidationInterval(long validationInterval) {
      this.poolProperties.setValidationInterval(validationInterval);
   }

   public void setValidationQuery(String validationQuery) {
      this.poolProperties.setValidationQuery(validationQuery);
   }

   public void setValidatorClassName(String className) {
      this.poolProperties.setValidatorClassName(className);
   }

   public void setValidationQueryTimeout(int validationQueryTimeout) {
      this.poolProperties.setValidationQueryTimeout(validationQueryTimeout);
   }

   public void setJdbcInterceptors(String interceptors) {
      this.getPoolProperties().setJdbcInterceptors(interceptors);
   }

   public void setJmxEnabled(boolean enabled) {
      this.getPoolProperties().setJmxEnabled(enabled);
   }

   public void setFairQueue(boolean fairQueue) {
      this.getPoolProperties().setFairQueue(fairQueue);
   }

   public void setUseLock(boolean useLock) {
      this.getPoolProperties().setUseLock(useLock);
   }

   public void setDefaultCatalog(String catalog) {
      this.getPoolProperties().setDefaultCatalog(catalog);
   }

   public void setDefaultAutoCommit(Boolean autocommit) {
      this.getPoolProperties().setDefaultAutoCommit(autocommit);
   }

   public void setDefaultTransactionIsolation(int defaultTransactionIsolation) {
      this.getPoolProperties().setDefaultTransactionIsolation(defaultTransactionIsolation);
   }

   public void setConnectionProperties(String properties) {
      try {
         Properties prop = DataSourceFactory.getProperties(properties);
         Iterator i = prop.keySet().iterator();

         while(i.hasNext()) {
            String key = (String)i.next();
            String value = prop.getProperty(key);
            this.getPoolProperties().getDbProperties().setProperty(key, value);
         }

      } catch (Exception var6) {
         log.error("Unable to parse connection properties.", var6);
         throw new RuntimeException(var6);
      }
   }

   public void setUseEquals(boolean useEquals) {
      this.getPoolProperties().setUseEquals(useEquals);
   }

   public Logger getParentLogger() throws SQLFeatureNotSupportedException {
      throw new SQLFeatureNotSupportedException();
   }

   public PrintWriter getLogWriter() throws SQLException {
      return null;
   }

   public void setLogWriter(PrintWriter out) throws SQLException {
   }

   public int getLoginTimeout() {
      return this.poolProperties == null ? 0 : this.poolProperties.getMaxWait() / 1000;
   }

   public void setLoginTimeout(int i) {
      if (this.poolProperties != null) {
         this.poolProperties.setMaxWait(1000 * i);
      }
   }

   public int getSuspectTimeout() {
      return this.getPoolProperties().getSuspectTimeout();
   }

   public void setSuspectTimeout(int seconds) {
      this.getPoolProperties().setSuspectTimeout(seconds);
   }

   public int getIdle() {
      try {
         return this.createPool().getIdle();
      } catch (SQLException var2) {
         throw new RuntimeException(var2);
      }
   }

   public int getNumIdle() {
      return this.getIdle();
   }

   public void checkAbandoned() {
      try {
         this.createPool().checkAbandoned();
      } catch (SQLException var2) {
         throw new RuntimeException(var2);
      }
   }

   public void checkIdle() {
      try {
         this.createPool().checkIdle();
      } catch (SQLException var2) {
         throw new RuntimeException(var2);
      }
   }

   public int getActive() {
      try {
         return this.createPool().getActive();
      } catch (SQLException var2) {
         throw new RuntimeException(var2);
      }
   }

   public int getNumActive() {
      return this.getActive();
   }

   public int getWaitCount() {
      try {
         return this.createPool().getWaitCount();
      } catch (SQLException var2) {
         throw new RuntimeException(var2);
      }
   }

   public int getSize() {
      try {
         return this.createPool().getSize();
      } catch (SQLException var2) {
         throw new RuntimeException(var2);
      }
   }

   public void testIdle() {
      try {
         this.createPool().testAllIdle();
      } catch (SQLException var2) {
         throw new RuntimeException(var2);
      }
   }

   public String getConnectionProperties() {
      return this.getPoolProperties().getConnectionProperties();
   }

   public Properties getDbProperties() {
      return this.getPoolProperties().getDbProperties();
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
      return this.getPoolProperties().getName();
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

   public void setValidator(Validator validator) {
      this.getPoolProperties().setValidator(validator);
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
      try {
         this.createPool().purge();
      } catch (SQLException var2) {
         log.error("Unable to purge pool.", var2);
      }

   }

   public void purgeOnReturn() {
      try {
         this.createPool().purgeOnReturn();
      } catch (SQLException var2) {
         log.error("Unable to purge pool.", var2);
      }

   }
}

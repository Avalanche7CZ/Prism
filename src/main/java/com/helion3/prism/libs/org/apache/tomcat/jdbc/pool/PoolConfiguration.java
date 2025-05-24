package com.helion3.prism.libs.org.apache.tomcat.jdbc.pool;

import java.util.Properties;

public interface PoolConfiguration {
   String PKG_PREFIX = "com.helion3.prism.libs.org.apache.tomcat.jdbc.pool.interceptor.";

   void setAbandonWhenPercentageFull(int var1);

   int getAbandonWhenPercentageFull();

   boolean isFairQueue();

   void setFairQueue(boolean var1);

   boolean isAccessToUnderlyingConnectionAllowed();

   void setAccessToUnderlyingConnectionAllowed(boolean var1);

   String getConnectionProperties();

   void setConnectionProperties(String var1);

   Properties getDbProperties();

   void setDbProperties(Properties var1);

   Boolean isDefaultAutoCommit();

   Boolean getDefaultAutoCommit();

   void setDefaultAutoCommit(Boolean var1);

   String getDefaultCatalog();

   void setDefaultCatalog(String var1);

   Boolean isDefaultReadOnly();

   Boolean getDefaultReadOnly();

   void setDefaultReadOnly(Boolean var1);

   int getDefaultTransactionIsolation();

   void setDefaultTransactionIsolation(int var1);

   String getDriverClassName();

   void setDriverClassName(String var1);

   int getInitialSize();

   void setInitialSize(int var1);

   boolean isLogAbandoned();

   void setLogAbandoned(boolean var1);

   int getMaxActive();

   void setMaxActive(int var1);

   int getMaxIdle();

   void setMaxIdle(int var1);

   int getMaxWait();

   void setMaxWait(int var1);

   int getMinEvictableIdleTimeMillis();

   void setMinEvictableIdleTimeMillis(int var1);

   int getMinIdle();

   void setMinIdle(int var1);

   String getName();

   void setName(String var1);

   int getNumTestsPerEvictionRun();

   void setNumTestsPerEvictionRun(int var1);

   String getPassword();

   void setPassword(String var1);

   String getPoolName();

   String getUsername();

   void setUsername(String var1);

   boolean isRemoveAbandoned();

   void setRemoveAbandoned(boolean var1);

   void setRemoveAbandonedTimeout(int var1);

   int getRemoveAbandonedTimeout();

   boolean isTestOnBorrow();

   void setTestOnBorrow(boolean var1);

   boolean isTestOnReturn();

   void setTestOnReturn(boolean var1);

   boolean isTestWhileIdle();

   void setTestWhileIdle(boolean var1);

   int getTimeBetweenEvictionRunsMillis();

   void setTimeBetweenEvictionRunsMillis(int var1);

   String getUrl();

   void setUrl(String var1);

   String getValidationQuery();

   void setValidationQuery(String var1);

   int getValidationQueryTimeout();

   void setValidationQueryTimeout(int var1);

   String getValidatorClassName();

   void setValidatorClassName(String var1);

   Validator getValidator();

   void setValidator(Validator var1);

   long getValidationInterval();

   void setValidationInterval(long var1);

   String getInitSQL();

   void setInitSQL(String var1);

   boolean isTestOnConnect();

   void setTestOnConnect(boolean var1);

   String getJdbcInterceptors();

   void setJdbcInterceptors(String var1);

   PoolProperties.InterceptorDefinition[] getJdbcInterceptorsAsArray();

   boolean isJmxEnabled();

   void setJmxEnabled(boolean var1);

   boolean isPoolSweeperEnabled();

   boolean isUseEquals();

   void setUseEquals(boolean var1);

   long getMaxAge();

   void setMaxAge(long var1);

   boolean getUseLock();

   void setUseLock(boolean var1);

   void setSuspectTimeout(int var1);

   int getSuspectTimeout();

   void setDataSource(Object var1);

   Object getDataSource();

   void setDataSourceJNDI(String var1);

   String getDataSourceJNDI();

   boolean isAlternateUsernameAllowed();

   void setAlternateUsernameAllowed(boolean var1);

   void setCommitOnReturn(boolean var1);

   boolean getCommitOnReturn();

   void setRollbackOnReturn(boolean var1);

   boolean getRollbackOnReturn();

   void setUseDisposableConnectionFacade(boolean var1);

   boolean getUseDisposableConnectionFacade();

   void setLogValidationErrors(boolean var1);

   boolean getLogValidationErrors();

   boolean getPropagateInterruptState();

   void setPropagateInterruptState(boolean var1);

   void setIgnoreExceptionOnPreLoad(boolean var1);

   boolean isIgnoreExceptionOnPreLoad();
}

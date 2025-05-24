package com.helion3.prism.libs.org.apache.tomcat.jdbc.pool;

import com.helion3.prism.libs.org.apache.juli.logging.Log;
import com.helion3.prism.libs.org.apache.juli.logging.LogFactory;
import com.helion3.prism.libs.org.apache.tomcat.jdbc.pool.interceptor.ConnectionState;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.XAConnection;

public class PooledConnection {
   private static final Log log = LogFactory.getLog(PooledConnection.class);
   public static final String PROP_USER = "user";
   public static final String PROP_PASSWORD = "password";
   public static final int VALIDATE_BORROW = 1;
   public static final int VALIDATE_RETURN = 2;
   public static final int VALIDATE_IDLE = 3;
   public static final int VALIDATE_INIT = 4;
   protected PoolConfiguration poolProperties;
   private volatile Connection connection;
   protected volatile XAConnection xaConnection;
   private String abandonTrace = null;
   private volatile long timestamp;
   private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(false);
   private volatile boolean discarded = false;
   private volatile long lastConnected = -1L;
   private volatile long lastValidated = System.currentTimeMillis();
   protected ConnectionPool parent;
   private HashMap attributes = new HashMap();
   private volatile long connectionVersion = 0L;
   private volatile JdbcInterceptor handler = null;
   private AtomicBoolean released = new AtomicBoolean(false);
   private volatile boolean suspect = false;
   private Driver driver = null;

   public PooledConnection(PoolConfiguration prop, ConnectionPool parent) {
      this.poolProperties = prop;
      this.parent = parent;
      this.connectionVersion = parent.getPoolVersion();
   }

   public long getConnectionVersion() {
      return this.connectionVersion;
   }

   public boolean checkUser(String username, String password) {
      if (!this.getPoolProperties().isAlternateUsernameAllowed()) {
         return true;
      } else {
         if (username == null) {
            username = this.poolProperties.getUsername();
         }

         if (password == null) {
            password = this.poolProperties.getPassword();
         }

         String storedUsr = (String)this.getAttributes().get("user");
         String storedPwd = (String)this.getAttributes().get("password");
         boolean result = username == null && storedUsr == null;
         result = result || username != null && username.equals(storedUsr);
         result = result && (password == null && storedPwd == null || password != null && password.equals(storedPwd));
         if (username == null) {
            this.getAttributes().remove("user");
         } else {
            this.getAttributes().put("user", username);
         }

         if (password == null) {
            this.getAttributes().remove("password");
         } else {
            this.getAttributes().put("password", password);
         }

         return result;
      }
   }

   public void connect() throws SQLException {
      if (this.released.get()) {
         throw new SQLException("A connection once released, can't be reestablished.");
      } else {
         if (this.connection != null) {
            try {
               this.disconnect(false);
            } catch (Exception var2) {
               log.debug("Unable to disconnect previous connection.", var2);
            }
         }

         if (this.poolProperties.getDataSource() == null && this.poolProperties.getDataSourceJNDI() != null) {
         }

         if (this.poolProperties.getDataSource() != null) {
            this.connectUsingDataSource();
         } else {
            this.connectUsingDriver();
         }

         if (this.poolProperties.getJdbcInterceptors() == null || this.poolProperties.getJdbcInterceptors().indexOf(ConnectionState.class.getName()) < 0 || this.poolProperties.getJdbcInterceptors().indexOf(ConnectionState.class.getSimpleName()) < 0) {
            if (this.poolProperties.getDefaultTransactionIsolation() != -1) {
               this.connection.setTransactionIsolation(this.poolProperties.getDefaultTransactionIsolation());
            }

            if (this.poolProperties.getDefaultReadOnly() != null) {
               this.connection.setReadOnly(this.poolProperties.getDefaultReadOnly());
            }

            if (this.poolProperties.getDefaultAutoCommit() != null) {
               this.connection.setAutoCommit(this.poolProperties.getDefaultAutoCommit());
            }

            if (this.poolProperties.getDefaultCatalog() != null) {
               this.connection.setCatalog(this.poolProperties.getDefaultCatalog());
            }
         }

         this.discarded = false;
         this.lastConnected = System.currentTimeMillis();
      }
   }

   protected void connectUsingDataSource() throws SQLException {
      String usr = null;
      String pwd = null;
      if (this.getAttributes().containsKey("user")) {
         usr = (String)this.getAttributes().get("user");
      } else {
         usr = this.poolProperties.getUsername();
         this.getAttributes().put("user", usr);
      }

      if (this.getAttributes().containsKey("password")) {
         pwd = (String)this.getAttributes().get("password");
      } else {
         pwd = this.poolProperties.getPassword();
         this.getAttributes().put("password", pwd);
      }

      if (this.poolProperties.getDataSource() instanceof javax.sql.XADataSource) {
         javax.sql.XADataSource xds = (javax.sql.XADataSource)this.poolProperties.getDataSource();
         if (usr != null && pwd != null) {
            this.xaConnection = xds.getXAConnection(usr, pwd);
            this.connection = this.xaConnection.getConnection();
         } else {
            this.xaConnection = xds.getXAConnection();
            this.connection = this.xaConnection.getConnection();
         }
      } else if (this.poolProperties.getDataSource() instanceof javax.sql.DataSource) {
         javax.sql.DataSource ds = (javax.sql.DataSource)this.poolProperties.getDataSource();
         if (usr != null && pwd != null) {
            this.connection = ds.getConnection(usr, pwd);
         } else {
            this.connection = ds.getConnection();
         }
      } else {
         if (!(this.poolProperties.getDataSource() instanceof ConnectionPoolDataSource)) {
            throw new SQLException("DataSource is of unknown class:" + (this.poolProperties.getDataSource() != null ? this.poolProperties.getDataSource().getClass() : "null"));
         }

         ConnectionPoolDataSource ds = (ConnectionPoolDataSource)this.poolProperties.getDataSource();
         if (usr != null && pwd != null) {
            this.connection = ds.getPooledConnection(usr, pwd).getConnection();
         } else {
            this.connection = ds.getPooledConnection().getConnection();
         }
      }

   }

   protected void connectUsingDriver() throws SQLException {
      SQLException ex;
      try {
         if (this.driver == null) {
            if (log.isDebugEnabled()) {
               log.debug("Instantiating driver using class: " + this.poolProperties.getDriverClassName() + " [url=" + this.poolProperties.getUrl() + "]");
            }

            this.driver = (Driver)Class.forName(this.poolProperties.getDriverClassName(), true, PooledConnection.class.getClassLoader()).newInstance();
         }
      } catch (Exception var8) {
         if (log.isDebugEnabled()) {
            log.debug("Unable to instantiate JDBC driver.", var8);
         }

         ex = new SQLException(var8.getMessage());
         ex.initCause(var8);
         throw ex;
      }

      String driverURL = this.poolProperties.getUrl();
      ex = null;
      String pwd = null;
      String usr;
      if (this.getAttributes().containsKey("user")) {
         usr = (String)this.getAttributes().get("user");
      } else {
         usr = this.poolProperties.getUsername();
         this.getAttributes().put("user", usr);
      }

      if (this.getAttributes().containsKey("password")) {
         pwd = (String)this.getAttributes().get("password");
      } else {
         pwd = this.poolProperties.getPassword();
         this.getAttributes().put("password", pwd);
      }

      Properties properties = PoolUtilities.clone(this.poolProperties.getDbProperties());
      if (usr != null) {
         properties.setProperty("user", usr);
      }

      if (pwd != null) {
         properties.setProperty("password", pwd);
      }

      try {
         this.connection = this.driver.connect(driverURL, properties);
      } catch (Exception var7) {
         if (log.isDebugEnabled()) {
            log.debug("Unable to connect to database.", var7);
         }

         if (this.parent.jmxPool != null) {
            this.parent.jmxPool.notify("CONNECTION FAILED", ConnectionPool.getStackTrace(var7));
         }

         if (var7 instanceof SQLException) {
            throw (SQLException)var7;
         }

         SQLException ex = new SQLException(var7.getMessage());
         ex.initCause(var7);
         throw ex;
      }

      if (this.connection == null) {
         throw new SQLException("Driver:" + this.driver + " returned null for URL:" + driverURL);
      }
   }

   public boolean isInitialized() {
      return this.connection != null;
   }

   public void reconnect() throws SQLException {
      this.disconnect(false);
      this.connect();
   }

   private void disconnect(boolean finalize) {
      if (!this.isDiscarded() || this.connection != null) {
         this.setDiscarded(true);
         if (this.connection != null) {
            try {
               this.parent.disconnectEvent(this, finalize);
               if (this.xaConnection == null) {
                  this.connection.close();
               } else {
                  this.xaConnection.close();
               }
            } catch (Exception var3) {
               if (log.isDebugEnabled()) {
                  log.debug("Unable to close underlying SQL connection", var3);
               }
            }
         }

         this.connection = null;
         this.xaConnection = null;
         this.lastConnected = -1L;
         if (finalize) {
            this.parent.finalize(this);
         }

      }
   }

   public long getAbandonTimeout() {
      return this.poolProperties.getRemoveAbandonedTimeout() <= 0 ? Long.MAX_VALUE : (long)(this.poolProperties.getRemoveAbandonedTimeout() * 1000);
   }

   private boolean doValidate(int action) {
      if (action == 1 && this.poolProperties.isTestOnBorrow()) {
         return true;
      } else if (action == 2 && this.poolProperties.isTestOnReturn()) {
         return true;
      } else if (action == 3 && this.poolProperties.isTestWhileIdle()) {
         return true;
      } else if (action == 4 && this.poolProperties.isTestOnConnect()) {
         return true;
      } else {
         return action == 4 && this.poolProperties.getInitSQL() != null;
      }
   }

   public boolean validate(int validateAction) {
      return this.validate(validateAction, (String)null);
   }

   public boolean validate(int validateAction, String sql) {
      if (this.isDiscarded()) {
         return false;
      } else if (!this.doValidate(validateAction)) {
         return true;
      } else {
         long now = System.currentTimeMillis();
         if (validateAction != 4 && this.poolProperties.getValidationInterval() > 0L && now - this.lastValidated < this.poolProperties.getValidationInterval()) {
            return true;
         } else if (this.poolProperties.getValidator() != null) {
            if (this.poolProperties.getValidator().validate(this.connection, validateAction)) {
               this.lastValidated = now;
               return true;
            } else {
               if (this.getPoolProperties().getLogValidationErrors()) {
                  log.error("Custom validation through " + this.poolProperties.getValidator() + " failed.");
               }

               return false;
            }
         } else {
            String query = sql;
            if (validateAction == 4 && this.poolProperties.getInitSQL() != null) {
               query = this.poolProperties.getInitSQL();
            }

            if (query == null) {
               query = this.poolProperties.getValidationQuery();
            }

            Statement stmt = null;

            try {
               stmt = this.connection.createStatement();
               int validationQueryTimeout = this.poolProperties.getValidationQueryTimeout();
               if (validationQueryTimeout > 0) {
                  stmt.setQueryTimeout(validationQueryTimeout);
               }

               stmt.execute(query);
               stmt.close();
               this.lastValidated = now;
               return true;
            } catch (Exception var10) {
               if (this.getPoolProperties().getLogValidationErrors()) {
                  log.warn("SQL Validation error", var10);
               } else if (log.isDebugEnabled()) {
                  log.debug("Unable to validate object:", var10);
               }

               if (stmt != null) {
                  try {
                     stmt.close();
                  } catch (Exception var9) {
                  }
               }

               return false;
            }
         }
      }
   }

   public long getReleaseTime() {
      return (long)this.poolProperties.getMinEvictableIdleTimeMillis();
   }

   public boolean release() {
      try {
         this.disconnect(true);
      } catch (Exception var2) {
         if (log.isDebugEnabled()) {
            log.debug("Unable to close SQL connection", var2);
         }
      }

      return this.released.compareAndSet(false, true);
   }

   public void setStackTrace(String trace) {
      this.abandonTrace = trace;
   }

   public String getStackTrace() {
      return this.abandonTrace;
   }

   public void setTimestamp(long timestamp) {
      this.timestamp = timestamp;
      this.setSuspect(false);
   }

   public boolean isSuspect() {
      return this.suspect;
   }

   public void setSuspect(boolean suspect) {
      this.suspect = suspect;
   }

   public void setDiscarded(boolean discarded) {
      if (this.discarded && !discarded) {
         throw new IllegalStateException("Unable to change the state once the connection has been discarded");
      } else {
         this.discarded = discarded;
      }
   }

   public void setLastValidated(long lastValidated) {
      this.lastValidated = lastValidated;
   }

   public void setPoolProperties(PoolConfiguration poolProperties) {
      this.poolProperties = poolProperties;
   }

   public long getTimestamp() {
      return this.timestamp;
   }

   public boolean isDiscarded() {
      return this.discarded;
   }

   public long getLastValidated() {
      return this.lastValidated;
   }

   public PoolConfiguration getPoolProperties() {
      return this.poolProperties;
   }

   public void lock() {
      if (this.poolProperties.getUseLock() || this.poolProperties.isPoolSweeperEnabled()) {
         this.lock.writeLock().lock();
      }

   }

   public void unlock() {
      if (this.poolProperties.getUseLock() || this.poolProperties.isPoolSweeperEnabled()) {
         this.lock.writeLock().unlock();
      }

   }

   public Connection getConnection() {
      return this.connection;
   }

   public XAConnection getXAConnection() {
      return this.xaConnection;
   }

   public long getLastConnected() {
      return this.lastConnected;
   }

   public JdbcInterceptor getHandler() {
      return this.handler;
   }

   public void setHandler(JdbcInterceptor handler) {
      if (this.handler != null && this.handler != handler) {
         for(JdbcInterceptor interceptor = this.handler; interceptor != null; interceptor = interceptor.getNext()) {
            interceptor.reset((ConnectionPool)null, (PooledConnection)null);
         }
      }

      this.handler = handler;
   }

   public String toString() {
      return "PooledConnection[" + (this.connection != null ? this.connection.toString() : "null") + "]";
   }

   public boolean isReleased() {
      return this.released.get();
   }

   public HashMap getAttributes() {
      return this.attributes;
   }
}

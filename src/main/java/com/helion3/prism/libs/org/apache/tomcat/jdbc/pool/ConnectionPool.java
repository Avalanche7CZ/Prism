package com.helion3.prism.libs.org.apache.tomcat.jdbc.pool;

import com.helion3.prism.libs.org.apache.juli.logging.Log;
import com.helion3.prism.libs.org.apache.juli.logging.LogFactory;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import javax.sql.XAConnection;

public class ConnectionPool {
   public static final String POOL_JMX_DOMAIN = "tomcat.jdbc";
   public static final String POOL_JMX_TYPE_PREFIX = "tomcat.jdbc:type=";
   private static final Log log = LogFactory.getLog(ConnectionPool.class);
   private AtomicInteger size = new AtomicInteger(0);
   private PoolConfiguration poolProperties;
   private BlockingQueue busy;
   private BlockingQueue idle;
   private volatile PoolCleaner poolCleaner;
   private volatile boolean closed = false;
   private Constructor proxyClassConstructor;
   private ThreadPoolExecutor cancellator;
   protected com.helion3.prism.libs.org.apache.tomcat.jdbc.pool.jmx.ConnectionPool jmxPool;
   private AtomicInteger waitcount;
   private AtomicLong poolVersion;
   private static volatile Timer poolCleanTimer = null;
   private static HashSet cleaners = new HashSet();

   public ConnectionPool(PoolConfiguration prop) throws SQLException {
      this.cancellator = new ThreadPoolExecutor(0, 1, 1000L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue());
      this.jmxPool = null;
      this.waitcount = new AtomicInteger(0);
      this.poolVersion = new AtomicLong(Long.MIN_VALUE);
      this.init(prop);
   }

   public Future getConnectionAsync() throws SQLException {
      try {
         PooledConnection pc = this.borrowConnection(0, (String)null, (String)null);
         if (pc != null) {
            return new ConnectionFuture(pc);
         }
      } catch (SQLException var2) {
         if (var2.getMessage().indexOf("NoWait") < 0) {
            throw var2;
         }
      }

      Future pcf;
      if (this.idle instanceof FairBlockingQueue) {
         pcf = ((FairBlockingQueue)this.idle).pollAsync();
         return new ConnectionFuture(pcf);
      } else if (this.idle instanceof MultiLockFairBlockingQueue) {
         pcf = ((MultiLockFairBlockingQueue)this.idle).pollAsync();
         return new ConnectionFuture(pcf);
      } else {
         throw new SQLException("Connection pool is misconfigured, doesn't support async retrieval. Set the 'fair' property to 'true'");
      }
   }

   public Connection getConnection() throws SQLException {
      PooledConnection con = this.borrowConnection(-1, (String)null, (String)null);
      return this.setupConnection(con);
   }

   public Connection getConnection(String username, String password) throws SQLException {
      PooledConnection con = this.borrowConnection(-1, username, password);
      return this.setupConnection(con);
   }

   public String getName() {
      return this.getPoolProperties().getPoolName();
   }

   public int getWaitCount() {
      return this.waitcount.get();
   }

   public PoolConfiguration getPoolProperties() {
      return this.poolProperties;
   }

   public int getSize() {
      return this.size.get();
   }

   public int getActive() {
      return this.busy.size();
   }

   public int getIdle() {
      return this.idle.size();
   }

   public boolean isClosed() {
      return this.closed;
   }

   protected Connection setupConnection(PooledConnection con) throws SQLException {
      JdbcInterceptor handler = con.getHandler();
      PoolProperties.InterceptorDefinition[] proxies;
      if (handler == null) {
         handler = new ProxyConnection(this, con, this.getPoolProperties().isUseEquals());
         proxies = this.getPoolProperties().getJdbcInterceptorsAsArray();

         for(int i = proxies.length - 1; i >= 0; --i) {
            try {
               JdbcInterceptor interceptor = (JdbcInterceptor)proxies[i].getInterceptorClass().newInstance();
               interceptor.setProperties(proxies[i].getProperties());
               interceptor.setNext((JdbcInterceptor)handler);
               interceptor.reset(this, con);
               handler = interceptor;
            } catch (Exception var8) {
               SQLException sx = new SQLException("Unable to instantiate interceptor chain.");
               sx.initCause(var8);
               throw sx;
            }
         }

         con.setHandler((JdbcInterceptor)handler);
      } else {
         for(JdbcInterceptor next = handler; next != null; next = ((JdbcInterceptor)next).getNext()) {
            ((JdbcInterceptor)next).reset(this, con);
         }
      }

      try {
         this.getProxyConstructor(con.getXAConnection() != null);
         proxies = null;
         Connection connection;
         if (this.getPoolProperties().getUseDisposableConnectionFacade()) {
            connection = (Connection)this.proxyClassConstructor.newInstance(new DisposableConnectionFacade((JdbcInterceptor)handler));
         } else {
            connection = (Connection)this.proxyClassConstructor.newInstance(handler);
         }

         return connection;
      } catch (Exception var7) {
         SQLException s = new SQLException();
         s.initCause(var7);
         throw s;
      }
   }

   public Constructor getProxyConstructor(boolean xa) throws NoSuchMethodException {
      if (this.proxyClassConstructor == null) {
         Class proxyClass = xa ? Proxy.getProxyClass(ConnectionPool.class.getClassLoader(), Connection.class, javax.sql.PooledConnection.class, XAConnection.class) : Proxy.getProxyClass(ConnectionPool.class.getClassLoader(), Connection.class, javax.sql.PooledConnection.class);
         this.proxyClassConstructor = proxyClass.getConstructor(InvocationHandler.class);
      }

      return this.proxyClassConstructor;
   }

   protected void close(boolean force) {
      if (!this.closed) {
         this.closed = true;
         if (this.poolCleaner != null) {
            this.poolCleaner.stopRunning();
         }

         BlockingQueue pool = this.idle.size() > 0 ? this.idle : (force ? this.busy : this.idle);

         while(pool.size() > 0) {
            try {
               for(PooledConnection con = (PooledConnection)pool.poll(1000L, TimeUnit.MILLISECONDS); con != null; con = (PooledConnection)pool.poll(1000L, TimeUnit.MILLISECONDS)) {
                  if (pool == this.idle) {
                     this.release(con);
                  } else {
                     this.abandon(con);
                  }

                  if (pool.size() <= 0) {
                     break;
                  }
               }
            } catch (InterruptedException var7) {
               if (this.getPoolProperties().getPropagateInterruptState()) {
                  Thread.currentThread().interrupt();
               }
            }

            if (pool.size() == 0 && force && pool != this.busy) {
               pool = this.busy;
            }
         }

         if (this.getPoolProperties().isJmxEnabled()) {
            this.jmxPool = null;
         }

         PoolProperties.InterceptorDefinition[] proxies = this.getPoolProperties().getJdbcInterceptorsAsArray();

         for(int i = 0; i < proxies.length; ++i) {
            try {
               JdbcInterceptor interceptor = (JdbcInterceptor)proxies[i].getInterceptorClass().newInstance();
               interceptor.setProperties(proxies[i].getProperties());
               interceptor.poolClosed(this);
            } catch (Exception var6) {
               log.debug("Unable to inform interceptor of pool closure.", var6);
            }
         }

      }
   }

   protected void init(PoolConfiguration properties) throws SQLException {
      this.poolProperties = properties;
      if (properties.getMaxActive() < 1) {
         log.warn("maxActive is smaller than 1, setting maxActive to: 100");
         properties.setMaxActive(100);
      }

      if (properties.getMaxActive() < properties.getInitialSize()) {
         log.warn("initialSize is larger than maxActive, setting initialSize to: " + properties.getMaxActive());
         properties.setInitialSize(properties.getMaxActive());
      }

      if (properties.getMinIdle() > properties.getMaxActive()) {
         log.warn("minIdle is larger than maxActive, setting minIdle to: " + properties.getMaxActive());
         properties.setMinIdle(properties.getMaxActive());
      }

      if (properties.getMaxIdle() > properties.getMaxActive()) {
         log.warn("maxIdle is larger than maxActive, setting maxIdle to: " + properties.getMaxActive());
         properties.setMaxIdle(properties.getMaxActive());
      }

      if (properties.getMaxIdle() < properties.getMinIdle()) {
         log.warn("maxIdle is smaller than minIdle, setting maxIdle to: " + properties.getMinIdle());
         properties.setMaxIdle(properties.getMinIdle());
      }

      this.busy = new ArrayBlockingQueue(properties.getMaxActive(), false);
      if (properties.isFairQueue()) {
         this.idle = new FairBlockingQueue();
      } else {
         this.idle = new ArrayBlockingQueue(properties.getMaxActive(), properties.isFairQueue());
      }

      this.initializePoolCleaner(properties);
      if (this.getPoolProperties().isJmxEnabled()) {
         this.createMBean();
      }

      PoolProperties.InterceptorDefinition[] proxies = this.getPoolProperties().getJdbcInterceptorsAsArray();

      for(int i = 0; i < proxies.length; ++i) {
         try {
            if (log.isDebugEnabled()) {
               log.debug("Creating interceptor instance of class:" + proxies[i].getInterceptorClass());
            }

            JdbcInterceptor interceptor = (JdbcInterceptor)proxies[i].getInterceptorClass().newInstance();
            interceptor.setProperties(proxies[i].getProperties());
            interceptor.poolStarted(this);
         } catch (Exception var21) {
            log.error("Unable to inform interceptor of pool start.", var21);
            if (this.jmxPool != null) {
               this.jmxPool.notify("INIT FAILED", getStackTrace(var21));
            }

            this.close(true);
            SQLException ex = new SQLException();
            ex.initCause(var21);
            throw ex;
         }
      }

      PooledConnection[] initialPool = new PooledConnection[this.poolProperties.getInitialSize()];
      boolean var15 = false;

      label278: {
         int i;
         label279: {
            try {
               var15 = true;

               for(i = 0; i < initialPool.length; ++i) {
                  initialPool[i] = this.borrowConnection(0, (String)null, (String)null);
               }

               var15 = false;
               break label279;
            } catch (SQLException var19) {
               log.error("Unable to create initial connections of pool.", var19);
               if (!this.poolProperties.isIgnoreExceptionOnPreLoad()) {
                  if (this.jmxPool != null) {
                     this.jmxPool.notify("INIT FAILED", getStackTrace(var19));
                  }

                  this.close(true);
                  throw var19;
               }

               var15 = false;
            } finally {
               if (var15) {
                  int i = 0;

                  while(true) {
                     if (i >= initialPool.length) {
                        ;
                     } else {
                        if (initialPool[i] != null) {
                           try {
                              this.returnConnection(initialPool[i]);
                           } catch (Exception var16) {
                           }
                        }

                        ++i;
                     }
                  }
               }
            }

            i = 0;

            while(true) {
               if (i >= initialPool.length) {
                  break label278;
               }

               if (initialPool[i] != null) {
                  try {
                     this.returnConnection(initialPool[i]);
                  } catch (Exception var17) {
                  }
               }

               ++i;
            }
         }

         for(i = 0; i < initialPool.length; ++i) {
            if (initialPool[i] != null) {
               try {
                  this.returnConnection(initialPool[i]);
               } catch (Exception var18) {
               }
            }
         }
      }

      this.closed = false;
   }

   public void initializePoolCleaner(PoolConfiguration properties) {
      if (properties.isPoolSweeperEnabled()) {
         this.poolCleaner = new PoolCleaner(this, (long)properties.getTimeBetweenEvictionRunsMillis());
         this.poolCleaner.start();
      }

   }

   protected void abandon(PooledConnection con) {
      if (con != null) {
         try {
            con.lock();
            String trace = con.getStackTrace();
            if (this.getPoolProperties().isLogAbandoned()) {
               log.warn("Connection has been abandoned " + con + ":" + trace);
            }

            if (this.jmxPool != null) {
               this.jmxPool.notify("CONNECTION ABANDONED", trace);
            }

            this.release(con);
         } finally {
            con.unlock();
         }

      }
   }

   protected void suspect(PooledConnection con) {
      if (con != null) {
         if (!con.isSuspect()) {
            try {
               con.lock();
               String trace = con.getStackTrace();
               if (this.getPoolProperties().isLogAbandoned()) {
                  log.warn("Connection has been marked suspect, possibly abandoned " + con + "[" + (System.currentTimeMillis() - con.getTimestamp()) + " ms.]:" + trace);
               }

               if (this.jmxPool != null) {
                  this.jmxPool.notify("SUSPECT CONNETION ABANDONED", trace);
               }

               con.setSuspect(true);
            } finally {
               con.unlock();
            }

         }
      }
   }

   protected void release(PooledConnection con) {
      if (con != null) {
         try {
            con.lock();
            if (con.release()) {
               this.size.addAndGet(-1);
               con.setHandler((JdbcInterceptor)null);
            }
         } finally {
            con.unlock();
         }

         if (this.waitcount.get() > 0) {
            this.idle.offer(this.create(true));
         }

      }
   }

   private PooledConnection borrowConnection(int wait, String username, String password) throws SQLException {
      if (this.isClosed()) {
         throw new SQLException("Connection pool closed.");
      } else {
         long now = System.currentTimeMillis();
         PooledConnection con = (PooledConnection)this.idle.poll();

         long maxWait;
         long timetowait;
         do {
            if (con != null) {
               PooledConnection result = this.borrowConnection(now, con, username, password);
               if (result != null) {
                  return result;
               }
            }

            if (this.size.get() < this.getPoolProperties().getMaxActive()) {
               if (this.size.addAndGet(1) <= this.getPoolProperties().getMaxActive()) {
                  return this.createConnection(now, con, username, password);
               }

               this.size.decrementAndGet();
            }

            maxWait = (long)wait;
            if (wait == -1) {
               maxWait = this.getPoolProperties().getMaxWait() <= 0 ? Long.MAX_VALUE : (long)this.getPoolProperties().getMaxWait();
            }

            timetowait = Math.max(0L, maxWait - (System.currentTimeMillis() - now));
            this.waitcount.incrementAndGet();

            try {
               con = (PooledConnection)this.idle.poll(timetowait, TimeUnit.MILLISECONDS);
            } catch (InterruptedException var17) {
               if (this.getPoolProperties().getPropagateInterruptState()) {
                  Thread.currentThread().interrupt();
               }

               SQLException sx = new SQLException("Pool wait interrupted.");
               sx.initCause(var17);
               throw sx;
            } finally {
               this.waitcount.decrementAndGet();
            }

            if (maxWait == 0L && con == null) {
               if (this.jmxPool != null) {
                  this.jmxPool.notify("POOL EMPTY", "Pool empty - no wait.");
               }

               throw new PoolExhaustedException("[" + Thread.currentThread().getName() + "] " + "NoWait: Pool empty. Unable to fetch a connection, none available[" + this.busy.size() + " in use].");
            }
         } while(con != null || System.currentTimeMillis() - now < maxWait);

         if (this.jmxPool != null) {
            this.jmxPool.notify("POOL EMPTY", "Pool empty - timeout.");
         }

         throw new PoolExhaustedException("[" + Thread.currentThread().getName() + "] " + "Timeout: Pool empty. Unable to fetch a connection in " + maxWait / 1000L + " seconds, none available[size:" + this.size.get() + "; busy:" + this.busy.size() + "; idle:" + this.idle.size() + "; lastwait:" + timetowait + "].");
      }
   }

   protected PooledConnection createConnection(long now, PooledConnection notUsed, String username, String password) throws SQLException {
      PooledConnection con = this.create(false);
      if (username != null) {
         con.getAttributes().put("user", username);
      }

      if (password != null) {
         con.getAttributes().put("password", password);
      }

      boolean error = false;

      PooledConnection var8;
      try {
         con.lock();
         con.connect();
         if (!con.validate(4)) {
            throw new SQLException("Validation Query Failed, enable logValidationErrors for more details.");
         }

         con.setTimestamp(now);
         if (this.getPoolProperties().isLogAbandoned()) {
            con.setStackTrace(getThreadDump());
         }

         if (!this.busy.offer(con)) {
            log.debug("Connection doesn't fit into busy array, connection will not be traceable.");
         }

         var8 = con;
      } catch (Exception var13) {
         error = true;
         if (log.isDebugEnabled()) {
            log.debug("Unable to create a new JDBC connection.", var13);
         }

         if (var13 instanceof SQLException) {
            throw (SQLException)var13;
         }

         SQLException ex = new SQLException(var13.getMessage());
         ex.initCause(var13);
         throw ex;
      } finally {
         if (error) {
            this.release(con);
         }

         con.unlock();
      }

      return var8;
   }

   protected PooledConnection borrowConnection(long now, PooledConnection con, String username, String password) throws SQLException {
      boolean setToNull = false;

      PooledConnection var8;
      try {
         con.lock();
         boolean usercheck = con.checkUser(username, password);
         if (con.isReleased()) {
            var8 = null;
            return var8;
         }

         SQLException ex;
         if (!con.isDiscarded() && !con.isInitialized()) {
            try {
               con.connect();
            } catch (Exception var14) {
               this.release(con);
               setToNull = true;
               if (var14 instanceof SQLException) {
                  throw (SQLException)var14;
               }

               ex = new SQLException(var14.getMessage());
               ex.initCause(var14);
               throw ex;
            }
         }

         if (usercheck && !con.isDiscarded() && con.validate(1)) {
            con.setTimestamp(now);
            if (this.getPoolProperties().isLogAbandoned()) {
               con.setStackTrace(getThreadDump());
            }

            if (!this.busy.offer(con)) {
               log.debug("Connection doesn't fit into busy array, connection will not be traceable.");
            }

            var8 = con;
            return var8;
         }

         try {
            con.reconnect();
            if (!con.validate(4)) {
               this.release(con);
               setToNull = true;
               throw new SQLException("Failed to validate a newly established connection.");
            }

            con.setTimestamp(now);
            if (this.getPoolProperties().isLogAbandoned()) {
               con.setStackTrace(getThreadDump());
            }

            if (!this.busy.offer(con)) {
               log.debug("Connection doesn't fit into busy array, connection will not be traceable.");
            }

            var8 = con;
         } catch (Exception var15) {
            this.release(con);
            setToNull = true;
            if (var15 instanceof SQLException) {
               throw (SQLException)var15;
            }

            ex = new SQLException(var15.getMessage());
            ex.initCause(var15);
            throw ex;
         }
      } finally {
         con.unlock();
         if (setToNull) {
            con = null;
         }

      }

      return var8;
   }

   protected boolean terminateTransaction(PooledConnection con) {
      try {
         if (Boolean.FALSE.equals(con.getPoolProperties().getDefaultAutoCommit())) {
            boolean autocommit;
            if (this.getPoolProperties().getRollbackOnReturn()) {
               autocommit = con.getConnection().getAutoCommit();
               if (!autocommit) {
                  con.getConnection().rollback();
               }
            } else if (this.getPoolProperties().getCommitOnReturn()) {
               autocommit = con.getConnection().getAutoCommit();
               if (!autocommit) {
                  con.getConnection().commit();
               }
            }
         }

         return true;
      } catch (SQLException var3) {
         log.warn("Unable to terminate transaction, connection will be closed.", var3);
         return false;
      }
   }

   protected boolean shouldClose(PooledConnection con, int action) {
      if (con.getConnectionVersion() < this.getPoolVersion()) {
         return true;
      } else if (con.isDiscarded()) {
         return true;
      } else if (this.isClosed()) {
         return true;
      } else if (!con.validate(action)) {
         return true;
      } else if (!this.terminateTransaction(con)) {
         return true;
      } else if (this.getPoolProperties().getMaxAge() > 0L) {
         return System.currentTimeMillis() - con.getLastConnected() > this.getPoolProperties().getMaxAge();
      } else {
         return false;
      }
   }

   protected void returnConnection(PooledConnection con) {
      if (this.isClosed()) {
         this.release(con);
      } else {
         if (con != null) {
            try {
               con.lock();
               if (this.busy.remove(con)) {
                  if (!this.shouldClose(con, 2)) {
                     con.setStackTrace((String)null);
                     con.setTimestamp(System.currentTimeMillis());
                     if (this.idle.size() >= this.poolProperties.getMaxIdle() && !this.poolProperties.isPoolSweeperEnabled() || !this.idle.offer(con)) {
                        if (log.isDebugEnabled()) {
                           log.debug("Connection [" + con + "] will be closed and not returned to the pool, idle[" + this.idle.size() + "]>=maxIdle[" + this.poolProperties.getMaxIdle() + "] idle.offer failed.");
                        }

                        this.release(con);
                     }
                  } else {
                     if (log.isDebugEnabled()) {
                        log.debug("Connection [" + con + "] will be closed and not returned to the pool.");
                     }

                     this.release(con);
                  }
               } else {
                  if (log.isDebugEnabled()) {
                     log.debug("Connection [" + con + "] will be closed and not returned to the pool, busy.remove failed.");
                  }

                  this.release(con);
               }
            } finally {
               con.unlock();
            }
         }

      }
   }

   protected boolean shouldAbandon() {
      if (this.poolProperties.getAbandonWhenPercentageFull() == 0) {
         return true;
      } else {
         float used = (float)this.busy.size();
         float max = (float)this.poolProperties.getMaxActive();
         float perc = (float)this.poolProperties.getAbandonWhenPercentageFull();
         return used / max * 100.0F >= perc;
      }
   }

   public void checkAbandoned() {
      try {
         if (this.busy.size() == 0) {
            return;
         }

         Iterator locked = this.busy.iterator();
         int sto = this.getPoolProperties().getSuspectTimeout();

         while(locked.hasNext()) {
            PooledConnection con = (PooledConnection)locked.next();
            boolean setToNull = false;

            try {
               con.lock();
               if (!this.idle.contains(con)) {
                  long time = con.getTimestamp();
                  long now = System.currentTimeMillis();
                  if (this.shouldAbandon() && now - time > con.getAbandonTimeout()) {
                     this.busy.remove(con);
                     this.abandon(con);
                     setToNull = true;
                  } else if (sto > 0 && now - time > (long)(sto * 1000)) {
                     this.suspect(con);
                  }
               }
            } finally {
               con.unlock();
               if (setToNull) {
                  con = null;
               }

            }
         }
      } catch (ConcurrentModificationException var14) {
         log.debug("checkAbandoned failed.", var14);
      } catch (Exception var15) {
         log.warn("checkAbandoned failed, it will be retried.", var15);
      }

   }

   public void checkIdle() {
      this.checkIdle(false);
   }

   public void checkIdle(boolean ignoreMinSize) {
      try {
         if (this.idle.size() == 0) {
            return;
         }

         long now = System.currentTimeMillis();
         Iterator unlocked = this.idle.iterator();

         while((ignoreMinSize || this.idle.size() >= this.getPoolProperties().getMinIdle()) && unlocked.hasNext()) {
            PooledConnection con = (PooledConnection)unlocked.next();
            boolean setToNull = false;

            try {
               con.lock();
               if (!this.busy.contains(con)) {
                  long time = con.getTimestamp();
                  if (this.shouldReleaseIdle(now, con, time)) {
                     this.release(con);
                     this.idle.remove(con);
                     setToNull = true;
                  }
               }
            } finally {
               con.unlock();
               if (setToNull) {
                  con = null;
               }

            }
         }
      } catch (ConcurrentModificationException var15) {
         log.debug("checkIdle failed.", var15);
      } catch (Exception var16) {
         log.warn("checkIdle failed, it will be retried.", var16);
      }

   }

   protected boolean shouldReleaseIdle(long now, PooledConnection con, long time) {
      if (con.getConnectionVersion() < this.getPoolVersion()) {
         return true;
      } else {
         return con.getReleaseTime() > 0L && now - time > con.getReleaseTime() && this.getSize() > this.getPoolProperties().getMinIdle();
      }
   }

   public void testAllIdle() {
      try {
         if (this.idle.size() == 0) {
            return;
         }

         Iterator unlocked = this.idle.iterator();

         while(unlocked.hasNext()) {
            PooledConnection con = (PooledConnection)unlocked.next();

            try {
               con.lock();
               if (!this.busy.contains(con) && !con.validate(3)) {
                  this.idle.remove(con);
                  this.release(con);
               }
            } finally {
               con.unlock();
            }
         }
      } catch (ConcurrentModificationException var8) {
         log.debug("testAllIdle failed.", var8);
      } catch (Exception var9) {
         log.warn("testAllIdle failed, it will be retried.", var9);
      }

   }

   protected static String getThreadDump() {
      Exception x = new Exception();
      x.fillInStackTrace();
      return getStackTrace(x);
   }

   public static String getStackTrace(Throwable x) {
      if (x == null) {
         return null;
      } else {
         ByteArrayOutputStream bout = new ByteArrayOutputStream();
         PrintStream writer = new PrintStream(bout);
         x.printStackTrace(writer);
         String result = bout.toString();
         return x.getMessage() != null && x.getMessage().length() > 0 ? x.getMessage() + ";" + result : result;
      }
   }

   protected PooledConnection create(boolean incrementCounter) {
      if (incrementCounter) {
         this.size.incrementAndGet();
      }

      PooledConnection con = new PooledConnection(this.getPoolProperties(), this);
      return con;
   }

   public void purge() {
      this.purgeOnReturn();
      this.checkIdle(true);
   }

   public void purgeOnReturn() {
      this.poolVersion.incrementAndGet();
   }

   protected void finalize(PooledConnection con) {
      for(JdbcInterceptor handler = con.getHandler(); handler != null; handler = handler.getNext()) {
         handler.reset((ConnectionPool)null, (PooledConnection)null);
      }

   }

   protected void disconnectEvent(PooledConnection con, boolean finalizing) {
      for(JdbcInterceptor handler = con.getHandler(); handler != null; handler = handler.getNext()) {
         handler.disconnected(this, con, finalizing);
      }

   }

   public com.helion3.prism.libs.org.apache.tomcat.jdbc.pool.jmx.ConnectionPool getJmxPool() {
      return this.jmxPool;
   }

   protected void createMBean() {
      try {
         this.jmxPool = new com.helion3.prism.libs.org.apache.tomcat.jdbc.pool.jmx.ConnectionPool(this);
      } catch (Exception var2) {
         log.warn("Unable to start JMX integration for connection pool. Instance[" + this.getName() + "] can't be monitored.", var2);
      }

   }

   private static synchronized void registerCleaner(PoolCleaner cleaner) {
      unregisterCleaner(cleaner);
      cleaners.add(cleaner);
      if (poolCleanTimer == null) {
         ClassLoader loader = Thread.currentThread().getContextClassLoader();

         try {
            Thread.currentThread().setContextClassLoader(ConnectionPool.class.getClassLoader());
            poolCleanTimer = new Timer("PoolCleaner[" + System.identityHashCode(ConnectionPool.class.getClassLoader()) + ":" + System.currentTimeMillis() + "]", true);
         } finally {
            Thread.currentThread().setContextClassLoader(loader);
         }
      }

      poolCleanTimer.scheduleAtFixedRate(cleaner, cleaner.sleepTime, cleaner.sleepTime);
   }

   private static synchronized void unregisterCleaner(PoolCleaner cleaner) {
      boolean removed = cleaners.remove(cleaner);
      if (removed) {
         cleaner.cancel();
         if (poolCleanTimer != null) {
            poolCleanTimer.purge();
            if (cleaners.size() == 0) {
               poolCleanTimer.cancel();
               poolCleanTimer = null;
            }
         }
      }

   }

   public static Set getPoolCleaners() {
      return Collections.unmodifiableSet(cleaners);
   }

   public long getPoolVersion() {
      return this.poolVersion.get();
   }

   public static Timer getPoolTimer() {
      return poolCleanTimer;
   }

   protected static class PoolCleaner extends TimerTask {
      protected WeakReference pool;
      protected long sleepTime;
      protected volatile long lastRun = 0L;

      PoolCleaner(ConnectionPool pool, long sleepTime) {
         this.pool = new WeakReference(pool);
         this.sleepTime = sleepTime;
         if (sleepTime <= 0L) {
            ConnectionPool.log.warn("Database connection pool evicter thread interval is set to 0, defaulting to 30 seconds");
            this.sleepTime = 30000L;
         } else if (sleepTime < 1000L) {
            ConnectionPool.log.warn("Database connection pool evicter thread interval is set to lower than 1 second.");
         }

      }

      public void run() {
         ConnectionPool pool = (ConnectionPool)this.pool.get();
         if (pool == null) {
            this.stopRunning();
         } else if (!pool.isClosed() && System.currentTimeMillis() - this.lastRun > this.sleepTime) {
            this.lastRun = System.currentTimeMillis();

            try {
               if (pool.getPoolProperties().isRemoveAbandoned()) {
                  pool.checkAbandoned();
               }

               if (pool.getPoolProperties().getMinIdle() < pool.idle.size()) {
                  pool.checkIdle();
               }

               if (pool.getPoolProperties().isTestWhileIdle()) {
                  pool.testAllIdle();
               }
            } catch (Exception var3) {
               ConnectionPool.log.error("", var3);
            }
         }

      }

      public void start() {
         ConnectionPool.registerCleaner(this);
      }

      public void stopRunning() {
         ConnectionPool.unregisterCleaner(this);
      }
   }

   protected class ConnectionFuture implements Future, Runnable {
      Future pcFuture = null;
      AtomicBoolean configured = new AtomicBoolean(false);
      CountDownLatch latch = new CountDownLatch(1);
      volatile Connection result = null;
      SQLException cause = null;
      AtomicBoolean cancelled = new AtomicBoolean(false);
      volatile PooledConnection pc = null;

      public ConnectionFuture(Future pcf) {
         this.pcFuture = pcf;
      }

      public ConnectionFuture(PooledConnection pc) throws SQLException {
         this.pc = pc;
         this.result = ConnectionPool.this.setupConnection(pc);
         this.configured.set(true);
      }

      public boolean cancel(boolean mayInterruptIfRunning) {
         if (this.pc != null) {
            return false;
         } else {
            if (!this.cancelled.get() && this.cancelled.compareAndSet(false, true)) {
               ConnectionPool.this.cancellator.execute(this);
            }

            return true;
         }
      }

      public Connection get() throws InterruptedException, ExecutionException {
         try {
            return this.get(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
         } catch (TimeoutException var2) {
            throw new ExecutionException(var2);
         }
      }

      public Connection get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
         PooledConnection pc = this.pc != null ? this.pc : (PooledConnection)this.pcFuture.get(timeout, unit);
         if (pc != null) {
            if (this.result != null) {
               return this.result;
            } else {
               if (this.configured.compareAndSet(false, true)) {
                  try {
                     pc = ConnectionPool.this.borrowConnection(System.currentTimeMillis(), pc, (String)null, (String)null);
                     this.result = ConnectionPool.this.setupConnection(pc);
                  } catch (SQLException var9) {
                     this.cause = var9;
                  } finally {
                     this.latch.countDown();
                  }
               } else {
                  this.latch.await(timeout, unit);
               }

               if (this.result == null) {
                  throw new ExecutionException(this.cause);
               } else {
                  return this.result;
               }
            }
         } else {
            return null;
         }
      }

      public boolean isCancelled() {
         return this.pc == null && (this.pcFuture.isCancelled() || this.cancelled.get());
      }

      public boolean isDone() {
         return this.pc != null || this.pcFuture.isDone();
      }

      public void run() {
         try {
            Connection con = this.get();
            con.close();
         } catch (ExecutionException var2) {
         } catch (Exception var3) {
            ConnectionPool.log.error("Unable to cancel ConnectionFuture.", var3);
         }

      }
   }
}

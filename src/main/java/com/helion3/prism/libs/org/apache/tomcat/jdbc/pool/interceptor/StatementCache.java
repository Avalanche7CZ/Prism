package com.helion3.prism.libs.org.apache.tomcat.jdbc.pool.interceptor;

import com.helion3.prism.libs.org.apache.tomcat.jdbc.pool.ConnectionPool;
import com.helion3.prism.libs.org.apache.tomcat.jdbc.pool.PoolProperties;
import com.helion3.prism.libs.org.apache.tomcat.jdbc.pool.PooledConnection;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class StatementCache extends StatementDecoratorInterceptor {
   protected static final String[] ALL_TYPES = new String[]{"prepareStatement", "prepareCall"};
   protected static final String[] CALLABLE_TYPE = new String[]{"prepareCall"};
   protected static final String[] PREPARED_TYPE = new String[]{"prepareStatement"};
   protected static final String[] NO_TYPE = new String[0];
   protected static final String STATEMENT_CACHE_ATTR = StatementCache.class.getName() + ".cache";
   private boolean cachePrepared = true;
   private boolean cacheCallable = false;
   private int maxCacheSize = 50;
   private PooledConnection pcon;
   private String[] types;
   private static ConcurrentHashMap cacheSizeMap = new ConcurrentHashMap();
   private AtomicInteger cacheSize;

   public boolean isCachePrepared() {
      return this.cachePrepared;
   }

   public boolean isCacheCallable() {
      return this.cacheCallable;
   }

   public int getMaxCacheSize() {
      return this.maxCacheSize;
   }

   public String[] getTypes() {
      return this.types;
   }

   public AtomicInteger getCacheSize() {
      return this.cacheSize;
   }

   public void setProperties(Map properties) {
      super.setProperties(properties);
      PoolProperties.InterceptorProperty p = (PoolProperties.InterceptorProperty)properties.get("prepared");
      if (p != null) {
         this.cachePrepared = p.getValueAsBoolean(this.cachePrepared);
      }

      p = (PoolProperties.InterceptorProperty)properties.get("callable");
      if (p != null) {
         this.cacheCallable = p.getValueAsBoolean(this.cacheCallable);
      }

      p = (PoolProperties.InterceptorProperty)properties.get("max");
      if (p != null) {
         this.maxCacheSize = p.getValueAsInt(this.maxCacheSize);
      }

      if (this.cachePrepared && this.cacheCallable) {
         this.types = ALL_TYPES;
      } else if (this.cachePrepared) {
         this.types = PREPARED_TYPE;
      } else if (this.cacheCallable) {
         this.types = CALLABLE_TYPE;
      } else {
         this.types = NO_TYPE;
      }

   }

   public void poolStarted(ConnectionPool pool) {
      cacheSizeMap.putIfAbsent(pool, new AtomicInteger(0));
      super.poolStarted(pool);
   }

   public void poolClosed(ConnectionPool pool) {
      cacheSizeMap.remove(pool);
      super.poolClosed(pool);
   }

   public void reset(ConnectionPool parent, PooledConnection con) {
      super.reset(parent, con);
      if (parent == null) {
         this.cacheSize = null;
         this.pcon = null;
      } else {
         this.cacheSize = (AtomicInteger)cacheSizeMap.get(parent);
         this.pcon = con;
         if (!this.pcon.getAttributes().containsKey(STATEMENT_CACHE_ATTR)) {
            ConcurrentHashMap cache = new ConcurrentHashMap();
            this.pcon.getAttributes().put(STATEMENT_CACHE_ATTR, cache);
         }
      }

   }

   public void disconnected(ConnectionPool parent, PooledConnection con, boolean finalizing) {
      ConcurrentHashMap statements = (ConcurrentHashMap)con.getAttributes().get(STATEMENT_CACHE_ATTR);
      if (statements != null) {
         Iterator i$ = statements.entrySet().iterator();

         while(i$.hasNext()) {
            Map.Entry p = (Map.Entry)i$.next();
            this.closeStatement((CachedStatement)p.getValue());
         }

         statements.clear();
      }

      super.disconnected(parent, con, finalizing);
   }

   public void closeStatement(CachedStatement st) {
      if (st != null) {
         st.forceClose();
      }
   }

   protected Object createDecorator(Object proxy, Method method, Object[] args, Object statement, Constructor constructor, String sql) throws InstantiationException, IllegalAccessException, InvocationTargetException {
      boolean process = this.process(this.types, method, false);
      if (process) {
         Object result = null;
         CachedStatement statementProxy = new CachedStatement((Statement)statement, sql);
         result = constructor.newInstance(statementProxy);
         statementProxy.setActualProxy(result);
         statementProxy.setConnection(proxy);
         statementProxy.setConstructor(constructor);
         return result;
      } else {
         return super.createDecorator(proxy, method, args, statement, constructor, sql);
      }
   }

   public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      boolean process = this.process(this.types, method, false);
      if (process && args.length > 0 && args[0] instanceof String) {
         CachedStatement statement = this.isCached((String)args[0]);
         if (statement != null) {
            this.removeStatement(statement);
            return statement.getActualProxy();
         } else {
            return super.invoke(proxy, method, args);
         }
      } else {
         return super.invoke(proxy, method, args);
      }
   }

   public CachedStatement isCached(String sql) {
      ConcurrentHashMap cache = (ConcurrentHashMap)this.pcon.getAttributes().get(STATEMENT_CACHE_ATTR);
      return (CachedStatement)cache.get(sql);
   }

   public boolean cacheStatement(CachedStatement proxy) {
      ConcurrentHashMap cache = (ConcurrentHashMap)this.pcon.getAttributes().get(STATEMENT_CACHE_ATTR);
      if (proxy.getSql() == null) {
         return false;
      } else if (cache.containsKey(proxy.getSql())) {
         return false;
      } else if (this.cacheSize.get() >= this.maxCacheSize) {
         return false;
      } else if (this.cacheSize.incrementAndGet() > this.maxCacheSize) {
         this.cacheSize.decrementAndGet();
         return false;
      } else {
         cache.put(proxy.getSql(), proxy);
         return true;
      }
   }

   public boolean removeStatement(CachedStatement proxy) {
      ConcurrentHashMap cache = (ConcurrentHashMap)this.pcon.getAttributes().get(STATEMENT_CACHE_ATTR);
      if (cache.remove(proxy.getSql()) != null) {
         this.cacheSize.decrementAndGet();
         return true;
      } else {
         return false;
      }
   }

   protected class CachedStatement extends StatementDecoratorInterceptor.StatementProxy {
      boolean cached = false;

      public CachedStatement(Statement parent, String sql) {
         super(parent, sql);
      }

      public void closeInvoked() {
         boolean shouldClose = true;
         if (StatementCache.this.cacheSize.get() < StatementCache.this.maxCacheSize) {
            CachedStatement proxy = StatementCache.this.new CachedStatement(this.getDelegate(), this.getSql());

            try {
               Object actualProxy = this.getConstructor().newInstance(proxy);
               proxy.setActualProxy(actualProxy);
               proxy.setConnection(this.getConnection());
               proxy.setConstructor(this.getConstructor());
               if (StatementCache.this.cacheStatement(proxy)) {
                  proxy.cached = true;
                  shouldClose = false;
               }
            } catch (Exception var4) {
               StatementCache.this.removeStatement(proxy);
            }
         }

         if (shouldClose) {
            super.closeInvoked();
         }

         this.closed = true;
         this.delegate = null;
      }

      public void forceClose() {
         StatementCache.this.removeStatement(this);
         super.closeInvoked();
      }
   }
}

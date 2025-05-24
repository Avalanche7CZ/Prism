package com.helion3.prism.libs.org.apache.tomcat.jdbc.pool.interceptor;

import com.helion3.prism.libs.org.apache.juli.logging.Log;
import com.helion3.prism.libs.org.apache.juli.logging.LogFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class AbstractQueryReport extends AbstractCreateStatementInterceptor {
   private static final Log log = LogFactory.getLog(AbstractQueryReport.class);
   protected long threshold = 1000L;
   protected static final Constructor[] constructors;

   protected abstract void prepareStatement(String var1, long var2);

   protected abstract void prepareCall(String var1, long var2);

   protected String reportFailedQuery(String query, Object[] args, String name, long start, Throwable t) {
      String sql = query == null && args != null && args.length > 0 ? (String)args[0] : query;
      if (sql == null && this.compare("executeBatch", name)) {
         sql = "batch";
      }

      return sql;
   }

   protected String reportQuery(String query, Object[] args, String name, long start, long delta) {
      String sql = query == null && args != null && args.length > 0 ? (String)args[0] : query;
      if (sql == null && this.compare("executeBatch", name)) {
         sql = "batch";
      }

      return sql;
   }

   protected String reportSlowQuery(String query, Object[] args, String name, long start, long delta) {
      String sql = query == null && args != null && args.length > 0 ? (String)args[0] : query;
      if (sql == null && this.compare("executeBatch", name)) {
         sql = "batch";
      }

      return sql;
   }

   public long getThreshold() {
      return this.threshold;
   }

   public void setThreshold(long threshold) {
      this.threshold = threshold;
   }

   protected Constructor getConstructor(int idx, Class clazz) throws NoSuchMethodException {
      if (constructors[idx] == null) {
         Class proxyClass = Proxy.getProxyClass(SlowQueryReport.class.getClassLoader(), clazz);
         constructors[idx] = proxyClass.getConstructor(InvocationHandler.class);
      }

      return constructors[idx];
   }

   public Object createStatement(Object proxy, Method method, Object[] args, Object statement, long time) {
      try {
         Object result = null;
         String name = method.getName();
         String sql = null;
         Constructor constructor = null;
         if (this.compare("createStatement", name)) {
            constructor = this.getConstructor(0, Statement.class);
         } else if (this.compare("prepareStatement", name)) {
            sql = (String)args[0];
            constructor = this.getConstructor(1, PreparedStatement.class);
            if (sql != null) {
               this.prepareStatement(sql, time);
            }
         } else {
            if (!this.compare("prepareCall", name)) {
               return statement;
            }

            sql = (String)args[0];
            constructor = this.getConstructor(2, CallableStatement.class);
            this.prepareCall(sql, time);
         }

         result = constructor.newInstance(new StatementProxy(statement, sql));
         return result;
      } catch (Exception var11) {
         log.warn("Unable to create statement proxy for slow query report.", var11);
         return statement;
      }
   }

   static {
      constructors = new Constructor[AbstractCreateStatementInterceptor.STATEMENT_TYPE_COUNT];
   }

   protected class StatementProxy implements InvocationHandler {
      protected boolean closed = false;
      protected Object delegate;
      protected final String query;

      public StatementProxy(Object parent, String query) {
         this.delegate = parent;
         this.query = query;
      }

      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
         String name = method.getName();
         boolean close = AbstractQueryReport.this.compare("close", name);
         if (close && this.closed) {
            return null;
         } else if (AbstractQueryReport.this.compare("isClosed", name)) {
            return this.closed;
         } else if (this.closed) {
            throw new SQLException("Statement closed.");
         } else {
            boolean process = false;
            process = AbstractQueryReport.this.isExecute(method, process);
            long start = process ? System.currentTimeMillis() : 0L;
            Object result = null;

            try {
               result = method.invoke(this.delegate, args);
            } catch (Throwable var14) {
               AbstractQueryReport.this.reportFailedQuery(this.query, args, name, start, var14);
               if (var14 instanceof InvocationTargetException && var14.getCause() != null) {
                  throw var14.getCause();
               }

               throw var14;
            }

            long delta = process ? System.currentTimeMillis() - start : Long.MIN_VALUE;
            if (delta > AbstractQueryReport.this.threshold) {
               try {
                  AbstractQueryReport.this.reportSlowQuery(this.query, args, name, start, delta);
               } catch (Exception var15) {
                  if (AbstractQueryReport.log.isWarnEnabled()) {
                     AbstractQueryReport.log.warn("Unable to process slow query", var15);
                  }
               }
            } else if (process) {
               AbstractQueryReport.this.reportQuery(this.query, args, name, start, delta);
            }

            if (close) {
               this.closed = true;
               this.delegate = null;
            }

            return result;
         }
      }
   }
}

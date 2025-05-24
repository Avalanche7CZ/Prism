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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class StatementDecoratorInterceptor extends AbstractCreateStatementInterceptor {
   private static final Log logger = LogFactory.getLog(StatementDecoratorInterceptor.class);
   private static final String[] EXECUTE_QUERY_TYPES = new String[]{"executeQuery"};
   protected static final Constructor[] constructors;
   protected static Constructor resultSetConstructor;

   public void closeInvoked() {
   }

   protected Constructor getConstructor(int idx, Class clazz) throws NoSuchMethodException {
      if (constructors[idx] == null) {
         Class proxyClass = Proxy.getProxyClass(StatementDecoratorInterceptor.class.getClassLoader(), clazz);
         constructors[idx] = proxyClass.getConstructor(InvocationHandler.class);
      }

      return constructors[idx];
   }

   protected Constructor getResultSetConstructor() throws NoSuchMethodException {
      if (resultSetConstructor == null) {
         Class proxyClass = Proxy.getProxyClass(StatementDecoratorInterceptor.class.getClassLoader(), ResultSet.class);
         resultSetConstructor = proxyClass.getConstructor(InvocationHandler.class);
      }

      return resultSetConstructor;
   }

   public Object createStatement(Object proxy, Method method, Object[] args, Object statement, long time) {
      Throwable cause;
      try {
         String name = method.getName();
         cause = null;
         String sql = null;
         Constructor constructor;
         if (this.compare("createStatement", name)) {
            constructor = this.getConstructor(0, Statement.class);
         } else if (this.compare("prepareStatement", name)) {
            constructor = this.getConstructor(1, PreparedStatement.class);
            sql = (String)args[0];
         } else {
            if (!this.compare("prepareCall", name)) {
               return statement;
            }

            constructor = this.getConstructor(2, CallableStatement.class);
            sql = (String)args[0];
         }

         return this.createDecorator(proxy, method, args, statement, constructor, sql);
      } catch (Exception var10) {
         if (var10 instanceof InvocationTargetException) {
            cause = var10.getCause();
            if (cause instanceof ThreadDeath) {
               throw (ThreadDeath)cause;
            }

            if (cause instanceof VirtualMachineError) {
               throw (VirtualMachineError)cause;
            }
         }

         logger.warn("Unable to create statement proxy for slow query report.", var10);
         return statement;
      }
   }

   protected Object createDecorator(Object proxy, Method method, Object[] args, Object statement, Constructor constructor, String sql) throws InstantiationException, IllegalAccessException, InvocationTargetException {
      Object result = null;
      StatementProxy statementProxy = new StatementProxy((Statement)statement, sql);
      result = constructor.newInstance(statementProxy);
      statementProxy.setActualProxy(result);
      statementProxy.setConnection(proxy);
      statementProxy.setConstructor(constructor);
      return result;
   }

   protected boolean isExecuteQuery(String methodName) {
      return EXECUTE_QUERY_TYPES[0].equals(methodName);
   }

   protected boolean isExecuteQuery(Method method) {
      return this.isExecuteQuery(method.getName());
   }

   static {
      constructors = new Constructor[AbstractCreateStatementInterceptor.STATEMENT_TYPE_COUNT];
      resultSetConstructor = null;
   }

   protected class ResultSetProxy implements InvocationHandler {
      private Object st;
      private Object delegate;

      public ResultSetProxy(Object st, Object delegate) {
         this.st = st;
         this.delegate = delegate;
      }

      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
         if (method.getName().equals("getStatement")) {
            return this.st;
         } else {
            try {
               return method.invoke(this.delegate, args);
            } catch (Throwable var5) {
               if (var5 instanceof InvocationTargetException && var5.getCause() != null) {
                  throw var5.getCause();
               } else {
                  throw var5;
               }
            }
         }
      }
   }

   protected class StatementProxy implements InvocationHandler {
      protected boolean closed = false;
      protected Statement delegate;
      private Object actualProxy;
      private Object connection;
      private String sql;
      private Constructor constructor;

      public StatementProxy(Statement delegate, String sql) {
         this.delegate = delegate;
         this.sql = sql;
      }

      public Statement getDelegate() {
         return this.delegate;
      }

      public String getSql() {
         return this.sql;
      }

      public void setConnection(Object proxy) {
         this.connection = proxy;
      }

      public Object getConnection() {
         return this.connection;
      }

      public void setActualProxy(Object proxy) {
         this.actualProxy = proxy;
      }

      public Object getActualProxy() {
         return this.actualProxy;
      }

      public Constructor getConstructor() {
         return this.constructor;
      }

      public void setConstructor(Constructor constructor) {
         this.constructor = constructor;
      }

      public void closeInvoked() {
         if (this.getDelegate() != null) {
            try {
               this.getDelegate().close();
            } catch (SQLException var2) {
            }
         }

         this.closed = true;
         this.delegate = null;
      }

      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
         if (StatementDecoratorInterceptor.this.compare("toString", method)) {
            return this.toString();
         } else {
            boolean close = StatementDecoratorInterceptor.this.compare("close", method);
            if (close && this.closed) {
               return null;
            } else if (StatementDecoratorInterceptor.this.compare("isClosed", method)) {
               return this.closed;
            } else if (this.closed) {
               throw new SQLException("Statement closed.");
            } else if (StatementDecoratorInterceptor.this.compare("getConnection", method)) {
               return this.connection;
            } else {
               boolean process = StatementDecoratorInterceptor.this.isExecuteQuery(method);
               Object result = null;

               try {
                  if (close) {
                     this.closeInvoked();
                  } else {
                     result = method.invoke(this.delegate, args);
                  }
               } catch (Throwable var8) {
                  if (var8 instanceof InvocationTargetException && var8.getCause() != null) {
                     throw var8.getCause();
                  }

                  throw var8;
               }

               if (process) {
                  Constructor cons = StatementDecoratorInterceptor.this.getResultSetConstructor();
                  result = cons.newInstance(StatementDecoratorInterceptor.this.new ResultSetProxy(this.actualProxy, result));
               }

               return result;
            }
         }
      }

      public String toString() {
         StringBuffer buf = new StringBuffer(StatementProxy.class.getName());
         buf.append("[Proxy=");
         buf.append(System.identityHashCode(this));
         buf.append("; Sql=");
         buf.append(this.getSql());
         buf.append("; Delegate=");
         buf.append(this.getDelegate());
         buf.append("; Connection=");
         buf.append(this.getConnection());
         buf.append("]");
         return buf.toString();
      }
   }
}

package com.helion3.prism.libs.org.apache.tomcat.jdbc.pool;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;

public class TrapException extends JdbcInterceptor {
   public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      try {
         return super.invoke(proxy, method, args);
      } catch (Exception var8) {
         Throwable exception = var8;
         if (var8 instanceof InvocationTargetException && var8.getCause() != null) {
            exception = var8.getCause();
            if (exception instanceof Error) {
               throw (Throwable)exception;
            }
         }

         Class exceptionClass = exception.getClass();
         if (!this.isDeclaredException(method, exceptionClass)) {
            if (this.isDeclaredException(method, SQLException.class)) {
               SQLException sqlx = new SQLException("Uncaught underlying exception.");
               sqlx.initCause((Throwable)exception);
               exception = sqlx;
            } else {
               RuntimeException rx = new RuntimeException("Uncaught underlying exception.");
               rx.initCause((Throwable)exception);
               exception = rx;
            }
         }

         throw (Throwable)exception;
      }
   }

   public boolean isDeclaredException(Method m, Class clazz) {
      Class[] arr$ = m.getExceptionTypes();
      int len$ = arr$.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         Class cl = arr$[i$];
         if (cl.equals(clazz) || cl.isAssignableFrom(clazz)) {
            return true;
         }
      }

      return false;
   }

   public void reset(ConnectionPool parent, PooledConnection con) {
   }
}

package com.helion3.prism.libs.org.apache.tomcat.jdbc.pool;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.SQLException;

public class DisposableConnectionFacade extends JdbcInterceptor {
   protected DisposableConnectionFacade(JdbcInterceptor interceptor) {
      this.setUseEquals(interceptor.isUseEquals());
      this.setNext(interceptor);
   }

   public void reset(ConnectionPool parent, PooledConnection con) {
   }

   public int hashCode() {
      return System.identityHashCode(this);
   }

   public boolean equals(Object obj) {
      return this == obj;
   }

   public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if (this.compare("equals", method)) {
         return this.equals(Proxy.getInvocationHandler(args[0]));
      } else if (this.compare("hashCode", method)) {
         return this.hashCode();
      } else {
         if (this.getNext() == null) {
            if (this.compare("isClosed", method)) {
               return Boolean.TRUE;
            }

            if (this.compare("close", method)) {
               return null;
            }

            if (this.compare("isValid", method)) {
               return Boolean.FALSE;
            }
         }

         String var5;
         try {
            try {
               Object var4 = super.invoke(proxy, method, args);
               return var4;
            } catch (NullPointerException var9) {
               if (this.getNext() != null) {
                  throw var9;
               }
            }

            if (!this.compare("toString", method)) {
               throw new SQLException("PooledConnection has already been closed.");
            }

            var5 = "DisposableConnectionFacade[null]";
         } finally {
            if (this.compare("close", method)) {
               this.setNext((JdbcInterceptor)null);
            }

         }

         return var5;
      }
   }
}

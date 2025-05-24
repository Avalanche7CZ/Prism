package com.helion3.prism.libs.org.apache.tomcat.jdbc.pool;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import javax.sql.XAConnection;

public class ProxyConnection extends JdbcInterceptor {
   protected PooledConnection connection = null;
   protected ConnectionPool pool = null;

   public PooledConnection getConnection() {
      return this.connection;
   }

   public void setConnection(PooledConnection connection) {
      this.connection = connection;
   }

   public ConnectionPool getPool() {
      return this.pool;
   }

   public void setPool(ConnectionPool pool) {
      this.pool = pool;
   }

   protected ProxyConnection(ConnectionPool parent, PooledConnection con, boolean useEquals) throws SQLException {
      this.pool = parent;
      this.connection = con;
      this.setUseEquals(useEquals);
   }

   public void reset(ConnectionPool parent, PooledConnection con) {
      this.pool = parent;
      this.connection = con;
   }

   public boolean isWrapperFor(Class iface) throws SQLException {
      return iface == XAConnection.class && this.connection.getXAConnection() != null ? true : iface.isInstance(this.connection.getConnection());
   }

   public Object unwrap(Class iface) throws SQLException {
      if (iface == PooledConnection.class) {
         return this.connection;
      } else if (iface == XAConnection.class) {
         return this.connection.getXAConnection();
      } else if (this.isWrapperFor(iface)) {
         return this.connection.getConnection();
      } else {
         throw new SQLException("Not a wrapper of " + iface.getName());
      }
   }

   public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if (this.compare("isClosed", method)) {
         return this.isClosed();
      } else {
         PooledConnection poolc;
         if (this.compare("close", method)) {
            if (this.connection == null) {
               return null;
            } else {
               poolc = this.connection;
               this.connection = null;
               this.pool.returnConnection(poolc);
               return null;
            }
         } else if (this.compare("toString", method)) {
            return this.toString();
         } else if (this.compare("getConnection", method) && this.connection != null) {
            return this.connection.getConnection();
         } else if (method.getDeclaringClass().equals(XAConnection.class)) {
            try {
               return method.invoke(this.connection.getXAConnection(), args);
            } catch (Throwable var5) {
               if (var5 instanceof InvocationTargetException) {
                  throw var5.getCause() != null ? var5.getCause() : var5;
               } else {
                  throw var5;
               }
            }
         } else if (this.isClosed()) {
            throw new SQLException("Connection has already been closed.");
         } else if (this.compare("unwrap", method)) {
            return this.unwrap((Class)args[0]);
         } else if (this.compare("isWrapperFor", method)) {
            return this.isWrapperFor((Class)args[0]);
         } else {
            try {
               poolc = this.connection;
               if (poolc != null) {
                  return method.invoke(poolc.getConnection(), args);
               } else {
                  throw new SQLException("Connection has already been closed.");
               }
            } catch (Throwable var6) {
               if (var6 instanceof InvocationTargetException) {
                  throw var6.getCause() != null ? var6.getCause() : var6;
               } else {
                  throw var6;
               }
            }
         }
      }
   }

   public boolean isClosed() {
      return this.connection == null || this.connection.isDiscarded();
   }

   public PooledConnection getDelegateConnection() {
      return this.connection;
   }

   public ConnectionPool getParentPool() {
      return this.pool;
   }

   public String toString() {
      return "ProxyConnection[" + (this.connection != null ? this.connection.toString() : "null") + "]";
   }
}

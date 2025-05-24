package com.helion3.prism.libs.org.apache.tomcat.jdbc.pool.interceptor;

import com.helion3.prism.libs.org.apache.juli.logging.Log;
import com.helion3.prism.libs.org.apache.juli.logging.LogFactory;
import com.helion3.prism.libs.org.apache.tomcat.jdbc.pool.ConnectionPool;
import com.helion3.prism.libs.org.apache.tomcat.jdbc.pool.JdbcInterceptor;
import com.helion3.prism.libs.org.apache.tomcat.jdbc.pool.PoolConfiguration;
import com.helion3.prism.libs.org.apache.tomcat.jdbc.pool.PooledConnection;
import java.lang.reflect.Method;
import java.sql.SQLException;

public class ConnectionState extends JdbcInterceptor {
   private static final Log log = LogFactory.getLog(ConnectionState.class);
   protected final String[] readState = new String[]{"getAutoCommit", "getTransactionIsolation", "isReadOnly", "getCatalog"};
   protected final String[] writeState = new String[]{"setAutoCommit", "setTransactionIsolation", "setReadOnly", "setCatalog"};
   protected Boolean autoCommit = null;
   protected Integer transactionIsolation = null;
   protected Boolean readOnly = null;
   protected String catalog = null;

   public void reset(ConnectionPool parent, PooledConnection con) {
      if (parent != null && con != null) {
         PoolConfiguration poolProperties = parent.getPoolProperties();
         if (poolProperties.getDefaultTransactionIsolation() != -1) {
            try {
               if (this.transactionIsolation == null || this.transactionIsolation != poolProperties.getDefaultTransactionIsolation()) {
                  con.getConnection().setTransactionIsolation(poolProperties.getDefaultTransactionIsolation());
                  this.transactionIsolation = poolProperties.getDefaultTransactionIsolation();
               }
            } catch (SQLException var8) {
               this.transactionIsolation = null;
               log.error("Unable to reset transaction isolation state to connection.", var8);
            }
         }

         if (poolProperties.getDefaultReadOnly() != null) {
            try {
               if (this.readOnly == null || this.readOnly != poolProperties.getDefaultReadOnly()) {
                  con.getConnection().setReadOnly(poolProperties.getDefaultReadOnly());
                  this.readOnly = poolProperties.getDefaultReadOnly();
               }
            } catch (SQLException var7) {
               this.readOnly = null;
               log.error("Unable to reset readonly state to connection.", var7);
            }
         }

         if (poolProperties.getDefaultAutoCommit() != null) {
            try {
               if (this.autoCommit == null || this.autoCommit != poolProperties.getDefaultAutoCommit()) {
                  con.getConnection().setAutoCommit(poolProperties.getDefaultAutoCommit());
                  this.autoCommit = poolProperties.getDefaultAutoCommit();
               }
            } catch (SQLException var6) {
               this.autoCommit = null;
               log.error("Unable to reset autocommit state to connection.", var6);
            }
         }

         if (poolProperties.getDefaultCatalog() != null) {
            try {
               if (this.catalog == null || !this.catalog.equals(poolProperties.getDefaultCatalog())) {
                  con.getConnection().setCatalog(poolProperties.getDefaultCatalog());
                  this.catalog = poolProperties.getDefaultCatalog();
               }
            } catch (SQLException var5) {
               this.catalog = null;
               log.error("Unable to reset default catalog state to connection.", var5);
            }
         }

      } else {
         this.autoCommit = null;
         this.transactionIsolation = null;
         this.readOnly = null;
         this.catalog = null;
      }
   }

   public void disconnected(ConnectionPool parent, PooledConnection con, boolean finalizing) {
      this.autoCommit = null;
      this.transactionIsolation = null;
      this.readOnly = null;
      this.catalog = null;
      super.disconnected(parent, con, finalizing);
   }

   public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      String name = method.getName();
      boolean read = false;
      int index = -1;

      for(int i = 0; !read && i < this.readState.length; ++i) {
         read = this.compare(name, this.readState[i]);
         if (read) {
            index = i;
         }
      }

      boolean write = false;

      for(int i = 0; !write && !read && i < this.writeState.length; ++i) {
         write = this.compare(name, this.writeState[i]);
         if (write) {
            index = i;
         }
      }

      Object result = null;
      if (read) {
         switch (index) {
            case 0:
               result = this.autoCommit;
               break;
            case 1:
               result = this.transactionIsolation;
               break;
            case 2:
               result = this.readOnly;
               break;
            case 3:
               result = this.catalog;
         }

         if (result != null) {
            return result;
         }
      }

      result = super.invoke(proxy, method, args);
      if (read || write) {
         switch (index) {
            case 0:
               this.autoCommit = (Boolean)((Boolean)(read ? result : args[0]));
               break;
            case 1:
               this.transactionIsolation = (Integer)((Integer)(read ? result : args[0]));
               break;
            case 2:
               this.readOnly = (Boolean)((Boolean)(read ? result : args[0]));
               break;
            case 3:
               this.catalog = (String)((String)(read ? result : args[0]));
         }
      }

      return result;
   }
}

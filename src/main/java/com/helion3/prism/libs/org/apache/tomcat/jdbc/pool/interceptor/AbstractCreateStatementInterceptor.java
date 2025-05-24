package com.helion3.prism.libs.org.apache.tomcat.jdbc.pool.interceptor;

import com.helion3.prism.libs.org.apache.tomcat.jdbc.pool.ConnectionPool;
import com.helion3.prism.libs.org.apache.tomcat.jdbc.pool.JdbcInterceptor;
import com.helion3.prism.libs.org.apache.tomcat.jdbc.pool.PooledConnection;
import java.lang.reflect.Method;

public abstract class AbstractCreateStatementInterceptor extends JdbcInterceptor {
   protected static final String CREATE_STATEMENT = "createStatement";
   protected static final int CREATE_STATEMENT_IDX = 0;
   protected static final String PREPARE_STATEMENT = "prepareStatement";
   protected static final int PREPARE_STATEMENT_IDX = 1;
   protected static final String PREPARE_CALL = "prepareCall";
   protected static final int PREPARE_CALL_IDX = 2;
   protected static final String[] STATEMENT_TYPES = new String[]{"createStatement", "prepareStatement", "prepareCall"};
   protected static final int STATEMENT_TYPE_COUNT;
   protected static final String EXECUTE = "execute";
   protected static final String EXECUTE_QUERY = "executeQuery";
   protected static final String EXECUTE_UPDATE = "executeUpdate";
   protected static final String EXECUTE_BATCH = "executeBatch";
   protected static final String[] EXECUTE_TYPES;

   public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if (this.compare("close", method)) {
         this.closeInvoked();
         return super.invoke(proxy, method, args);
      } else {
         boolean process = false;
         process = this.isStatement(method, process);
         if (process) {
            long start = System.currentTimeMillis();
            Object statement = super.invoke(proxy, method, args);
            long delta = System.currentTimeMillis() - start;
            return this.createStatement(proxy, method, args, statement, delta);
         } else {
            return super.invoke(proxy, method, args);
         }
      }
   }

   public abstract Object createStatement(Object var1, Method var2, Object[] var3, Object var4, long var5);

   public abstract void closeInvoked();

   protected boolean isStatement(Method method, boolean process) {
      return this.process(STATEMENT_TYPES, method, process);
   }

   protected boolean isExecute(Method method, boolean process) {
      return this.process(EXECUTE_TYPES, method, process);
   }

   protected boolean process(String[] names, Method method, boolean process) {
      String name = method.getName();

      for(int i = 0; !process && i < names.length; ++i) {
         process = this.compare(names[i], name);
      }

      return process;
   }

   public void reset(ConnectionPool parent, PooledConnection con) {
   }

   static {
      STATEMENT_TYPE_COUNT = STATEMENT_TYPES.length;
      EXECUTE_TYPES = new String[]{"execute", "executeQuery", "executeUpdate", "executeBatch"};
   }
}

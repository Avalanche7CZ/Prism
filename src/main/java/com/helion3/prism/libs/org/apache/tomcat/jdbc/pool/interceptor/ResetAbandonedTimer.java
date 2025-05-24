package com.helion3.prism.libs.org.apache.tomcat.jdbc.pool.interceptor;

import com.helion3.prism.libs.org.apache.tomcat.jdbc.pool.JdbcInterceptor;
import com.helion3.prism.libs.org.apache.tomcat.jdbc.pool.PooledConnection;
import com.helion3.prism.libs.org.apache.tomcat.jdbc.pool.ProxyConnection;
import java.lang.reflect.Method;

public class ResetAbandonedTimer extends AbstractQueryReport {
   public boolean resetTimer() {
      boolean result = false;

      for(JdbcInterceptor interceptor = this.getNext(); interceptor != null && !result; interceptor = interceptor.getNext()) {
         if (interceptor instanceof ProxyConnection) {
            PooledConnection con = ((ProxyConnection)interceptor).getConnection();
            if (con == null) {
               break;
            }

            con.setTimestamp(System.currentTimeMillis());
            result = true;
         }
      }

      return result;
   }

   public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      Object result = super.invoke(proxy, method, args);
      this.resetTimer();
      return result;
   }

   protected void prepareCall(String query, long time) {
      this.resetTimer();
   }

   protected void prepareStatement(String sql, long time) {
      this.resetTimer();
   }

   public void closeInvoked() {
      this.resetTimer();
   }

   protected String reportQuery(String query, Object[] args, String name, long start, long delta) {
      this.resetTimer();
      return super.reportQuery(query, args, name, start, delta);
   }

   protected String reportSlowQuery(String query, Object[] args, String name, long start, long delta) {
      this.resetTimer();
      return super.reportSlowQuery(query, args, name, start, delta);
   }
}

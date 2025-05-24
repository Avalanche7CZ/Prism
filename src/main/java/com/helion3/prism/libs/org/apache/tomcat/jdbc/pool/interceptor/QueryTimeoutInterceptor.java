package com.helion3.prism.libs.org.apache.tomcat.jdbc.pool.interceptor;

import com.helion3.prism.libs.org.apache.juli.logging.Log;
import com.helion3.prism.libs.org.apache.juli.logging.LogFactory;
import com.helion3.prism.libs.org.apache.tomcat.jdbc.pool.PoolProperties;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

public class QueryTimeoutInterceptor extends AbstractCreateStatementInterceptor {
   private static Log log = LogFactory.getLog(QueryTimeoutInterceptor.class);
   int timeout;

   public void setProperties(Map properties) {
      super.setProperties(properties);
      this.timeout = ((PoolProperties.InterceptorProperty)properties.get("queryTimeout")).getValueAsInt(-1);
   }

   public Object createStatement(Object proxy, Method method, Object[] args, Object statement, long time) {
      if (statement instanceof Statement && this.timeout > 0) {
         Statement s = (Statement)statement;

         try {
            s.setQueryTimeout(this.timeout);
         } catch (SQLException var9) {
            log.warn("[QueryTimeoutInterceptor] Unable to set query timeout:" + var9.getMessage(), var9);
         }
      }

      return statement;
   }

   public void closeInvoked() {
   }
}

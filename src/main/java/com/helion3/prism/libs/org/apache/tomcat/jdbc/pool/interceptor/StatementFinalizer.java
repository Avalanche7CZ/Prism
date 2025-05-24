package com.helion3.prism.libs.org.apache.tomcat.jdbc.pool.interceptor;

import com.helion3.prism.libs.org.apache.juli.logging.Log;
import com.helion3.prism.libs.org.apache.juli.logging.LogFactory;
import com.helion3.prism.libs.org.apache.tomcat.jdbc.pool.ConnectionPool;
import com.helion3.prism.libs.org.apache.tomcat.jdbc.pool.PooledConnection;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.sql.Statement;
import java.util.ArrayList;

public class StatementFinalizer extends AbstractCreateStatementInterceptor {
   private static final Log log = LogFactory.getLog(StatementFinalizer.class);
   protected ArrayList statements = new ArrayList();

   public Object createStatement(Object proxy, Method method, Object[] args, Object statement, long time) {
      try {
         if (statement instanceof Statement) {
            this.statements.add(new WeakReference((Statement)statement));
         }
      } catch (ClassCastException var8) {
      }

      return statement;
   }

   public void closeInvoked() {
      while(this.statements.size() > 0) {
         WeakReference ws = (WeakReference)this.statements.remove(0);
         Statement st = (Statement)ws.get();
         if (st != null) {
            try {
               st.close();
            } catch (Exception var4) {
               if (log.isDebugEnabled()) {
                  log.debug("Unable to closed statement upon connection close.", var4);
               }
            }
         }
      }

   }

   public void reset(ConnectionPool parent, PooledConnection con) {
      this.statements.clear();
      super.reset(parent, con);
   }
}

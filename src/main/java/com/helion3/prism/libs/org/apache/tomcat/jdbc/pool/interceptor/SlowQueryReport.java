package com.helion3.prism.libs.org.apache.tomcat.jdbc.pool.interceptor;

import com.helion3.prism.libs.org.apache.juli.logging.Log;
import com.helion3.prism.libs.org.apache.juli.logging.LogFactory;
import com.helion3.prism.libs.org.apache.tomcat.jdbc.pool.ConnectionPool;
import com.helion3.prism.libs.org.apache.tomcat.jdbc.pool.PoolProperties;
import com.helion3.prism.libs.org.apache.tomcat.jdbc.pool.PooledConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

public class SlowQueryReport extends AbstractQueryReport {
   private static final Log log = LogFactory.getLog(SlowQueryReport.class);
   protected static ConcurrentHashMap perPoolStats = new ConcurrentHashMap();
   protected volatile ConcurrentHashMap queries = null;
   protected int maxQueries = 1000;

   public static ConcurrentHashMap getPoolStats(String poolname) {
      return (ConcurrentHashMap)perPoolStats.get(poolname);
   }

   public void setMaxQueries(int maxQueries) {
      this.maxQueries = maxQueries;
   }

   protected String reportFailedQuery(String query, Object[] args, String name, long start, Throwable t) {
      String sql = super.reportFailedQuery(query, args, name, start, t);
      if (this.maxQueries > 0) {
         long now = System.currentTimeMillis();
         long delta = now - start;
         QueryStats qs = this.getQueryStats(sql);
         qs.failure(delta, now);
         if (log.isWarnEnabled()) {
            log.warn("Failed Query Report SQL=" + sql + "; time=" + delta + " ms;");
         }
      }

      return sql;
   }

   protected String reportSlowQuery(String query, Object[] args, String name, long start, long delta) {
      String sql = super.reportSlowQuery(query, args, name, start, delta);
      if (this.maxQueries > 0) {
         QueryStats qs = this.getQueryStats(sql);
         qs.add(delta, start);
         if (log.isWarnEnabled()) {
            log.warn("Slow Query Report SQL=" + sql + "; time=" + delta + " ms;");
         }
      }

      return sql;
   }

   public void closeInvoked() {
   }

   public void prepareStatement(String sql, long time) {
      QueryStats qs = this.getQueryStats(sql);
      qs.prepare(time);
   }

   public void prepareCall(String sql, long time) {
      QueryStats qs = this.getQueryStats(sql);
      qs.prepare(time);
   }

   public void poolStarted(ConnectionPool pool) {
      super.poolStarted(pool);
      this.queries = (ConcurrentHashMap)perPoolStats.get(pool.getName());
      if (this.queries == null) {
         this.queries = new ConcurrentHashMap();
         if (perPoolStats.putIfAbsent(pool.getName(), this.queries) != null) {
            this.queries = (ConcurrentHashMap)perPoolStats.get(pool.getName());
         }
      }

   }

   public void poolClosed(ConnectionPool pool) {
      perPoolStats.remove(pool.getName());
      super.poolClosed(pool);
   }

   protected QueryStats getQueryStats(String sql) {
      if (sql == null) {
         sql = "";
      }

      ConcurrentHashMap queries = this.queries;
      if (queries == null) {
         return null;
      } else {
         QueryStats qs = (QueryStats)queries.get(sql);
         if (qs == null) {
            qs = new QueryStats(sql);
            if (queries.putIfAbsent(sql, qs) != null) {
               qs = (QueryStats)queries.get(sql);
            } else if (queries.size() > this.maxQueries) {
               this.removeOldest(queries);
            }
         }

         return qs;
      }
   }

   protected void removeOldest(ConcurrentHashMap queries) {
      Iterator it = queries.keySet().iterator();

      while(queries.size() > this.maxQueries && it.hasNext()) {
         String sql = (String)it.next();
         it.remove();
         if (log.isDebugEnabled()) {
            log.debug("Removing slow query, capacity reached:" + sql);
         }
      }

   }

   public void reset(ConnectionPool parent, PooledConnection con) {
      super.reset(parent, con);
      if (parent != null) {
         this.queries = (ConcurrentHashMap)perPoolStats.get(parent.getName());
      } else {
         this.queries = null;
      }

   }

   public void setProperties(Map properties) {
      super.setProperties(properties);
      String threshold = "threshold";
      String maxqueries = "maxQueries";
      PoolProperties.InterceptorProperty p1 = (PoolProperties.InterceptorProperty)properties.get("threshold");
      PoolProperties.InterceptorProperty p2 = (PoolProperties.InterceptorProperty)properties.get("maxQueries");
      if (p1 != null) {
         this.setThreshold(Long.parseLong(p1.getValue()));
      }

      if (p2 != null) {
         this.setMaxQueries(Integer.parseInt(p2.getValue()));
      }

   }

   public static class QueryStats {
      static final String[] FIELD_NAMES = new String[]{"query", "nrOfInvocations", "maxInvocationTime", "maxInvocationDate", "minInvocationTime", "minInvocationDate", "totalInvocationTime", "failures", "prepareCount", "prepareTime", "lastInvocation"};
      static final String[] FIELD_DESCRIPTIONS = new String[]{"The SQL query", "The number of query invocations, a call to executeXXX", "The longest time for this query in milliseconds", "The time and date for when the longest query took place", "The shortest time for this query in milliseconds", "The time and date for when the shortest query took place", "The total amount of milliseconds spent executing this query", "The number of failures for this query", "The number of times this query was prepared (prepareStatement/prepareCall)", "The total number of milliseconds spent preparing this query", "The date and time of the last invocation"};
      static final OpenType[] FIELD_TYPES;
      private final String query;
      private volatile int nrOfInvocations;
      private volatile long maxInvocationTime = Long.MIN_VALUE;
      private volatile long maxInvocationDate;
      private volatile long minInvocationTime = Long.MAX_VALUE;
      private volatile long minInvocationDate;
      private volatile long totalInvocationTime;
      private volatile long failures;
      private volatile int prepareCount;
      private volatile long prepareTime;
      private volatile long lastInvocation = 0L;

      public static String[] getFieldNames() {
         return FIELD_NAMES;
      }

      public static String[] getFieldDescriptions() {
         return FIELD_DESCRIPTIONS;
      }

      public static OpenType[] getFieldTypes() {
         return FIELD_TYPES;
      }

      public String toString() {
         SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy HH:mm:ss z", Locale.US);
         sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
         StringBuilder buf = new StringBuilder("QueryStats[query:");
         buf.append(this.query);
         buf.append(", nrOfInvocations:");
         buf.append(this.nrOfInvocations);
         buf.append(", maxInvocationTime:");
         buf.append(this.maxInvocationTime);
         buf.append(", maxInvocationDate:");
         buf.append(sdf.format(new Date(this.maxInvocationDate)));
         buf.append(", minInvocationTime:");
         buf.append(this.minInvocationTime);
         buf.append(", minInvocationDate:");
         buf.append(sdf.format(new Date(this.minInvocationDate)));
         buf.append(", totalInvocationTime:");
         buf.append(this.totalInvocationTime);
         buf.append(", averageInvocationTime:");
         buf.append((float)this.totalInvocationTime / (float)this.nrOfInvocations);
         buf.append(", failures:");
         buf.append(this.failures);
         buf.append(", prepareCount:");
         buf.append(this.prepareCount);
         buf.append(", prepareTime:");
         buf.append(this.prepareTime);
         buf.append("]");
         return buf.toString();
      }

      public CompositeDataSupport getCompositeData(CompositeType type) throws OpenDataException {
         Object[] values = new Object[]{this.query, this.nrOfInvocations, this.maxInvocationTime, this.maxInvocationDate, this.minInvocationTime, this.minInvocationDate, this.totalInvocationTime, this.failures, this.prepareCount, this.prepareTime, this.lastInvocation};
         return new CompositeDataSupport(type, FIELD_NAMES, values);
      }

      public QueryStats(String query) {
         this.query = query;
      }

      public void prepare(long invocationTime) {
         ++this.prepareCount;
         this.prepareTime += invocationTime;
      }

      public void add(long invocationTime, long now) {
         this.maxInvocationTime = Math.max(invocationTime, this.maxInvocationTime);
         if (this.maxInvocationTime == invocationTime) {
            this.maxInvocationDate = now;
         }

         this.minInvocationTime = Math.min(invocationTime, this.minInvocationTime);
         if (this.minInvocationTime == invocationTime) {
            this.minInvocationDate = now;
         }

         ++this.nrOfInvocations;
         this.totalInvocationTime += invocationTime;
         this.lastInvocation = now;
      }

      public void failure(long invocationTime, long now) {
         this.add(invocationTime, now);
         ++this.failures;
      }

      public String getQuery() {
         return this.query;
      }

      public int getNrOfInvocations() {
         return this.nrOfInvocations;
      }

      public long getMaxInvocationTime() {
         return this.maxInvocationTime;
      }

      public long getMaxInvocationDate() {
         return this.maxInvocationDate;
      }

      public long getMinInvocationTime() {
         return this.minInvocationTime;
      }

      public long getMinInvocationDate() {
         return this.minInvocationDate;
      }

      public long getTotalInvocationTime() {
         return this.totalInvocationTime;
      }

      public int hashCode() {
         return this.query.hashCode();
      }

      public boolean equals(Object other) {
         if (other instanceof QueryStats) {
            QueryStats qs = (QueryStats)other;
            return qs.query.equals(this.query);
         } else {
            return false;
         }
      }

      public boolean isOlderThan(QueryStats other) {
         return this.lastInvocation < other.lastInvocation;
      }

      static {
         FIELD_TYPES = new OpenType[]{SimpleType.STRING, SimpleType.INTEGER, SimpleType.LONG, SimpleType.LONG, SimpleType.LONG, SimpleType.LONG, SimpleType.LONG, SimpleType.LONG, SimpleType.INTEGER, SimpleType.LONG, SimpleType.LONG};
      }
   }
}

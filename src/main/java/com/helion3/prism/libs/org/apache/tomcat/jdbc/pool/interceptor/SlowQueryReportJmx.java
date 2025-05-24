package com.helion3.prism.libs.org.apache.tomcat.jdbc.pool.interceptor;

import com.helion3.prism.libs.org.apache.juli.logging.Log;
import com.helion3.prism.libs.org.apache.juli.logging.LogFactory;
import com.helion3.prism.libs.org.apache.tomcat.jdbc.pool.ConnectionPool;
import com.helion3.prism.libs.org.apache.tomcat.jdbc.pool.PoolProperties;
import com.helion3.prism.libs.org.apache.tomcat.jdbc.pool.PooledConnection;
import java.lang.management.ManagementFactory;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationEmitter;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.RuntimeOperationsException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;

public class SlowQueryReportJmx extends SlowQueryReport implements NotificationEmitter, SlowQueryReportJmxMBean {
   public static final String SLOW_QUERY_NOTIFICATION = "SLOW QUERY";
   public static final String FAILED_QUERY_NOTIFICATION = "FAILED QUERY";
   public static final String objectNameAttribute = "objectName";
   protected static CompositeType SLOW_QUERY_TYPE;
   private static final Log log = LogFactory.getLog(SlowQueryReportJmx.class);
   protected static ConcurrentHashMap mbeans = new ConcurrentHashMap();
   protected volatile NotificationBroadcasterSupport notifier = new NotificationBroadcasterSupport();
   protected String poolName = null;
   protected static AtomicLong notifySequence = new AtomicLong(0L);
   protected boolean notifyPool = true;
   protected ConnectionPool pool = null;

   public void addNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) throws IllegalArgumentException {
      this.notifier.addNotificationListener(listener, filter, handback);
   }

   public MBeanNotificationInfo[] getNotificationInfo() {
      return this.notifier.getNotificationInfo();
   }

   public void removeNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
      this.notifier.removeNotificationListener(listener);
   }

   public void removeNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) throws ListenerNotFoundException {
      this.notifier.removeNotificationListener(listener, filter, handback);
   }

   protected static CompositeType getCompositeType() {
      if (SLOW_QUERY_TYPE == null) {
         try {
            SLOW_QUERY_TYPE = new CompositeType(SlowQueryReportJmx.class.getName(), "Composite data type for query statistics", SlowQueryReport.QueryStats.getFieldNames(), SlowQueryReport.QueryStats.getFieldDescriptions(), SlowQueryReport.QueryStats.getFieldTypes());
         } catch (OpenDataException var1) {
            log.warn("Unable to initialize composite data type for JMX stats and notifications.", var1);
         }
      }

      return SLOW_QUERY_TYPE;
   }

   public void reset(ConnectionPool parent, PooledConnection con) {
      super.reset(parent, con);
      if (parent != null) {
         this.poolName = parent.getName();
         this.pool = parent;
         this.registerJmx();
      }

   }

   public void poolClosed(ConnectionPool pool) {
      this.poolName = pool.getName();
      this.deregisterJmx();
      super.poolClosed(pool);
   }

   public void poolStarted(ConnectionPool pool) {
      this.pool = pool;
      super.poolStarted(pool);
      this.poolName = pool.getName();
   }

   protected String reportFailedQuery(String query, Object[] args, String name, long start, Throwable t) {
      query = super.reportFailedQuery(query, args, name, start, t);
      this.notifyJmx(query, "FAILED QUERY");
      return query;
   }

   protected void notifyJmx(String query, String type) {
      try {
         long sequence = notifySequence.incrementAndGet();
         if (this.isNotifyPool()) {
            if (this.pool != null && this.pool.getJmxPool() != null) {
               this.pool.getJmxPool().notify(type, query);
            }
         } else if (this.notifier != null) {
            Notification notification = new Notification(type, this, sequence, System.currentTimeMillis(), query);
            this.notifier.sendNotification(notification);
         }
      } catch (RuntimeOperationsException var7) {
         if (log.isDebugEnabled()) {
            log.debug("Unable to send failed query notification.", var7);
         }
      }

   }

   protected String reportSlowQuery(String query, Object[] args, String name, long start, long delta) {
      query = super.reportSlowQuery(query, args, name, start, delta);
      this.notifyJmx(query, "SLOW QUERY");
      return query;
   }

   public String[] getPoolNames() {
      Set keys = perPoolStats.keySet();
      return (String[])keys.toArray(new String[0]);
   }

   public String getPoolName() {
      return this.poolName;
   }

   public boolean isNotifyPool() {
      return this.notifyPool;
   }

   public void setNotifyPool(boolean notifyPool) {
      this.notifyPool = notifyPool;
   }

   public void resetStats() {
      ConcurrentHashMap queries = (ConcurrentHashMap)perPoolStats.get(this.poolName);
      if (queries != null) {
         Iterator it = queries.keySet().iterator();

         while(it.hasNext()) {
            it.remove();
         }
      }

   }

   public CompositeData[] getSlowQueriesCD() throws OpenDataException {
      CompositeDataSupport[] result = null;
      ConcurrentHashMap queries = (ConcurrentHashMap)perPoolStats.get(this.poolName);
      if (queries != null) {
         Set stats = queries.entrySet();
         if (stats != null) {
            result = new CompositeDataSupport[stats.size()];
            Iterator it = stats.iterator();

            SlowQueryReport.QueryStats qs;
            for(int pos = 0; it.hasNext(); result[pos++] = qs.getCompositeData(getCompositeType())) {
               Map.Entry entry = (Map.Entry)it.next();
               qs = (SlowQueryReport.QueryStats)entry.getValue();
            }
         }
      }

      return result;
   }

   protected void deregisterJmx() {
      try {
         if (mbeans.remove(this.poolName) != null) {
            ObjectName oname = this.getObjectName(this.getClass(), this.poolName);
            ManagementFactory.getPlatformMBeanServer().unregisterMBean(oname);
         }
      } catch (MBeanRegistrationException var2) {
         log.debug("Jmx deregistration failed.", var2);
      } catch (InstanceNotFoundException var3) {
         log.debug("Jmx deregistration failed.", var3);
      } catch (MalformedObjectNameException var4) {
         log.warn("Jmx deregistration failed.", var4);
      } catch (RuntimeOperationsException var5) {
         log.warn("Jmx deregistration failed.", var5);
      }

   }

   public ObjectName getObjectName(Class clazz, String poolName) throws MalformedObjectNameException {
      Map properties = this.getProperties();
      ObjectName oname;
      if (properties != null && properties.containsKey("objectName")) {
         oname = new ObjectName(((PoolProperties.InterceptorProperty)properties.get("objectName")).getValue());
      } else {
         oname = new ObjectName("tomcat.jdbc:type=" + clazz.getName() + ",name=" + poolName);
      }

      return oname;
   }

   protected void registerJmx() {
      try {
         if (!this.isNotifyPool()) {
            if (getCompositeType() != null) {
               ObjectName oname = this.getObjectName(this.getClass(), this.poolName);
               if (mbeans.putIfAbsent(this.poolName, this) == null) {
                  ManagementFactory.getPlatformMBeanServer().registerMBean(this, oname);
               }
            } else {
               log.warn(SlowQueryReport.class.getName() + "- No JMX support, composite type was not found.");
            }
         }
      } catch (MalformedObjectNameException var2) {
         log.error("Jmx registration failed, no JMX data will be exposed for the query stats.", var2);
      } catch (RuntimeOperationsException var3) {
         log.error("Jmx registration failed, no JMX data will be exposed for the query stats.", var3);
      } catch (MBeanException var4) {
         log.error("Jmx registration failed, no JMX data will be exposed for the query stats.", var4);
      } catch (InstanceAlreadyExistsException var5) {
         log.error("Jmx registration failed, no JMX data will be exposed for the query stats.", var5);
      } catch (NotCompliantMBeanException var6) {
         log.error("Jmx registration failed, no JMX data will be exposed for the query stats.", var6);
      }

   }

   public void setProperties(Map properties) {
      super.setProperties(properties);
      String threshold = "notifyPool";
      PoolProperties.InterceptorProperty p1 = (PoolProperties.InterceptorProperty)properties.get("notifyPool");
      if (p1 != null) {
         this.setNotifyPool(Boolean.parseBoolean(p1.getValue()));
      }

   }
}

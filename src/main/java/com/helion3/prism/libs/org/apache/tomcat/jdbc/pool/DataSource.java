package com.helion3.prism.libs.org.apache.tomcat.jdbc.pool;

import com.helion3.prism.libs.org.apache.juli.logging.Log;
import com.helion3.prism.libs.org.apache.juli.logging.LogFactory;
import com.helion3.prism.libs.org.apache.tomcat.jdbc.pool.jmx.ConnectionPoolMBean;
import java.lang.management.ManagementFactory;
import java.util.Hashtable;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.sql.ConnectionPoolDataSource;

public class DataSource extends DataSourceProxy implements javax.sql.DataSource, MBeanRegistration, ConnectionPoolMBean, ConnectionPoolDataSource {
   private static final Log log = LogFactory.getLog(DataSource.class);
   protected volatile ObjectName oname = null;

   public DataSource() {
   }

   public DataSource(PoolConfiguration poolProperties) {
      super(poolProperties);
   }

   public void postDeregister() {
      if (this.oname != null) {
         this.unregisterJmx();
      }

   }

   public void postRegister(Boolean registrationDone) {
   }

   public void preDeregister() throws Exception {
   }

   public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception {
      try {
         if (this.isJmxEnabled()) {
            this.oname = this.createObjectName(name);
            if (this.oname != null) {
               this.registerJmx();
            }
         }
      } catch (MalformedObjectNameException var4) {
         log.error("Unable to create object name for JDBC pool.", var4);
      }

      return name;
   }

   public ObjectName createObjectName(ObjectName original) throws MalformedObjectNameException {
      String domain = "tomcat.jdbc";
      Hashtable properties = original.getKeyPropertyList();
      String origDomain = original.getDomain();
      properties.put("type", "ConnectionPool");
      properties.put("class", this.getClass().getName());
      if (original.getKeyProperty("path") != null || properties.get("context") != null) {
         properties.put("engine", origDomain);
      }

      ObjectName name = new ObjectName(domain, properties);
      return name;
   }

   protected void registerJmx() {
      try {
         if (this.pool.getJmxPool() != null) {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            mbs.registerMBean(this.pool.getJmxPool(), this.oname);
         }
      } catch (Exception var2) {
         log.error("Unable to register JDBC pool with JMX", var2);
      }

   }

   protected void unregisterJmx() {
      try {
         MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
         mbs.unregisterMBean(this.oname);
      } catch (InstanceNotFoundException var2) {
      } catch (Exception var3) {
         log.error("Unable to unregister JDBC pool with JMX", var3);
      }

   }
}

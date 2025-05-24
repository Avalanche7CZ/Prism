package com.helion3.prism.libs.org.apache.tomcat.jdbc.pool.jmx;

import com.helion3.prism.libs.org.apache.tomcat.jdbc.pool.PoolConfiguration;

public interface ConnectionPoolMBean extends PoolConfiguration {
   int getSize();

   int getIdle();

   int getActive();

   int getNumIdle();

   int getNumActive();

   int getWaitCount();

   void checkIdle();

   void checkAbandoned();

   void testIdle();

   void purge();

   void purgeOnReturn();
}

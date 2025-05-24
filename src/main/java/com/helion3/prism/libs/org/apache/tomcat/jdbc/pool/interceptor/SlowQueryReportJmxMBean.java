package com.helion3.prism.libs.org.apache.tomcat.jdbc.pool.interceptor;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.OpenDataException;

public interface SlowQueryReportJmxMBean {
   CompositeData[] getSlowQueriesCD() throws OpenDataException;
}

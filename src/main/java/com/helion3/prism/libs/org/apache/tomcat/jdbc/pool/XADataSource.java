package com.helion3.prism.libs.org.apache.tomcat.jdbc.pool;

public class XADataSource extends DataSource implements javax.sql.XADataSource {
   public XADataSource() {
   }

   public XADataSource(PoolConfiguration poolProperties) {
      super(poolProperties);
   }
}

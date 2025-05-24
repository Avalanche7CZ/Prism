package com.helion3.prism.libs.org.apache.tomcat.jdbc.pool;

import java.sql.SQLException;

public class PoolExhaustedException extends SQLException {
   private static final long serialVersionUID = 3501536931777262475L;

   public PoolExhaustedException() {
   }

   public PoolExhaustedException(String reason) {
      super(reason);
   }

   public PoolExhaustedException(Throwable cause) {
      super(cause);
   }

   public PoolExhaustedException(String reason, String SQLState) {
      super(reason, SQLState);
   }

   public PoolExhaustedException(String reason, Throwable cause) {
      super(reason, cause);
   }

   public PoolExhaustedException(String reason, String SQLState, int vendorCode) {
      super(reason, SQLState, vendorCode);
   }

   public PoolExhaustedException(String reason, String sqlState, Throwable cause) {
      super(reason, sqlState, cause);
   }

   public PoolExhaustedException(String reason, String sqlState, int vendorCode, Throwable cause) {
      super(reason, sqlState, vendorCode, cause);
   }
}

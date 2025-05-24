package com.helion3.prism.libs.org.apache.tomcat.jdbc.pool;

import java.util.Properties;

public class PoolUtilities {
   public static final String PROP_USER = "user";
   public static final String PROP_PASSWORD = "password";

   public static Properties clone(Properties p) {
      Properties c = new Properties();
      c.putAll(p);
      return c;
   }

   public static Properties cloneWithoutPassword(Properties p) {
      Properties result = clone(p);
      result.remove("password");
      return result;
   }
}

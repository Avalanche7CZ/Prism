package com.helion3.prism.libs.org.apache.tomcat.jdbc.pool;

import java.sql.Connection;

public interface Validator {
   boolean validate(Connection var1, int var2);
}

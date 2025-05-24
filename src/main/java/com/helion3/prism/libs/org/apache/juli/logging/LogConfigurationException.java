package com.helion3.prism.libs.org.apache.juli.logging;

public class LogConfigurationException extends RuntimeException {
   private static final long serialVersionUID = 1L;
   protected Throwable cause;

   public LogConfigurationException() {
      this.cause = null;
   }

   public LogConfigurationException(String message) {
      super(message);
      this.cause = null;
   }

   public LogConfigurationException(Throwable cause) {
      this(cause == null ? null : cause.toString(), cause);
   }

   public LogConfigurationException(String message, Throwable cause) {
      super(message);
      this.cause = null;
      this.cause = cause;
   }

   public Throwable getCause() {
      return this.cause;
   }
}

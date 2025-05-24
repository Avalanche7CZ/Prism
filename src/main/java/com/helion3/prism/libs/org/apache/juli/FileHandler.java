package com.helion3.prism.libs.org.apache.juli;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.ErrorManager;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

public class FileHandler extends Handler {
   private volatile String date;
   private String directory;
   private String prefix;
   private String suffix;
   private boolean rotatable;
   private volatile PrintWriter writer;
   protected ReadWriteLock writerLock;
   private int bufferSize;

   public FileHandler() {
      this((String)null, (String)null, (String)null);
   }

   public FileHandler(String directory, String prefix, String suffix) {
      this.date = "";
      this.directory = null;
      this.prefix = null;
      this.suffix = null;
      this.rotatable = true;
      this.writer = null;
      this.writerLock = new ReentrantReadWriteLock();
      this.bufferSize = -1;
      this.directory = directory;
      this.prefix = prefix;
      this.suffix = suffix;
      this.configure();
      this.openWriter();
   }

   public void publish(LogRecord record) {
      if (this.isLoggable(record)) {
         Timestamp ts = new Timestamp(System.currentTimeMillis());
         String tsString = ts.toString().substring(0, 19);
         String tsDate = tsString.substring(0, 10);

         try {
            this.writerLock.readLock().lock();
            if (this.rotatable && !this.date.equals(tsDate)) {
               try {
                  this.writerLock.readLock().unlock();
                  this.writerLock.writeLock().lock();
                  if (!this.date.equals(tsDate)) {
                     this.closeWriter();
                     this.date = tsDate;
                     this.openWriter();
                  }
               } finally {
                  this.writerLock.writeLock().unlock();
                  this.writerLock.readLock().lock();
               }
            }

            String result = null;

            try {
               result = this.getFormatter().format(record);
            } catch (Exception var18) {
               this.reportError((String)null, var18, 5);
               return;
            }

            try {
               if (this.writer != null) {
                  this.writer.write(result);
                  if (this.bufferSize < 0) {
                     this.writer.flush();
                     return;
                  }
               } else {
                  this.reportError("FileHandler is closed or not yet initialized, unable to log [" + result + "]", (Exception)null, 1);
               }

               return;
            } catch (Exception var17) {
               this.reportError((String)null, var17, 1);
            }
         } finally {
            this.writerLock.readLock().unlock();
         }

      }
   }

   public void close() {
      this.closeWriter();
   }

   protected void closeWriter() {
      this.writerLock.writeLock().lock();

      try {
         if (this.writer == null) {
            return;
         }

         this.writer.write(this.getFormatter().getTail(this));
         this.writer.flush();
         this.writer.close();
         this.writer = null;
         this.date = "";
      } catch (Exception var5) {
         this.reportError((String)null, var5, 3);
      } finally {
         this.writerLock.writeLock().unlock();
      }

   }

   public void flush() {
      this.writerLock.readLock().lock();

      try {
         if (this.writer == null) {
            return;
         }

         this.writer.flush();
      } catch (Exception var5) {
         this.reportError((String)null, var5, 2);
      } finally {
         this.writerLock.readLock().unlock();
      }

   }

   private void configure() {
      Timestamp ts = new Timestamp(System.currentTimeMillis());
      String tsString = ts.toString().substring(0, 19);
      this.date = tsString.substring(0, 10);
      String className = this.getClass().getName();
      ClassLoader cl = Thread.currentThread().getContextClassLoader();
      this.rotatable = Boolean.parseBoolean(this.getProperty(className + ".rotatable", "true"));
      if (this.directory == null) {
         this.directory = this.getProperty(className + ".directory", "logs");
      }

      if (this.prefix == null) {
         this.prefix = this.getProperty(className + ".prefix", "juli.");
      }

      if (this.suffix == null) {
         this.suffix = this.getProperty(className + ".suffix", ".log");
      }

      String sBufferSize = this.getProperty(className + ".bufferSize", String.valueOf(this.bufferSize));

      try {
         this.bufferSize = Integer.parseInt(sBufferSize);
      } catch (NumberFormatException var13) {
      }

      String encoding = this.getProperty(className + ".encoding", (String)null);
      if (encoding != null && encoding.length() > 0) {
         try {
            this.setEncoding(encoding);
         } catch (UnsupportedEncodingException var12) {
         }
      }

      this.setLevel(Level.parse(this.getProperty(className + ".level", "" + Level.ALL)));
      String filterName = this.getProperty(className + ".filter", (String)null);
      if (filterName != null) {
         try {
            this.setFilter((Filter)cl.loadClass(filterName).newInstance());
         } catch (Exception var11) {
         }
      }

      String formatterName = this.getProperty(className + ".formatter", (String)null);
      if (formatterName != null) {
         try {
            this.setFormatter((Formatter)cl.loadClass(formatterName).newInstance());
         } catch (Exception var10) {
            this.setFormatter(new SimpleFormatter());
         }
      } else {
         this.setFormatter(new SimpleFormatter());
      }

      this.setErrorManager(new ErrorManager());
   }

   private String getProperty(String name, String defaultValue) {
      String value = LogManager.getLogManager().getProperty(name);
      if (value == null) {
         value = defaultValue;
      } else {
         value = value.trim();
      }

      return value;
   }

   protected void open() {
      this.openWriter();
   }

   protected void openWriter() {
      File dir = new File(this.directory);
      if (!dir.mkdirs() && !dir.isDirectory()) {
         this.reportError("Unable to create [" + dir + "]", (Exception)null, 4);
         this.writer = null;
      } else {
         this.writerLock.writeLock().lock();
         FileOutputStream fos = null;
         OutputStream os = null;

         try {
            File pathname = new File(dir.getAbsoluteFile(), this.prefix + (this.rotatable ? this.date : "") + this.suffix);
            File parent = pathname.getParentFile();
            if (!parent.mkdirs() && !parent.isDirectory()) {
               this.reportError("Unable to create [" + parent + "]", (Exception)null, 4);
               this.writer = null;
               return;
            }

            String encoding = this.getEncoding();
            fos = new FileOutputStream(pathname, true);
            os = this.bufferSize > 0 ? new BufferedOutputStream(fos, this.bufferSize) : fos;
            this.writer = new PrintWriter(encoding != null ? new OutputStreamWriter((OutputStream)os, encoding) : new OutputStreamWriter((OutputStream)os), false);
            this.writer.write(this.getFormatter().getHead(this));
         } catch (Exception var14) {
            this.reportError((String)null, var14, 4);
            this.writer = null;
            if (fos != null) {
               try {
                  fos.close();
               } catch (IOException var13) {
               }
            }

            if (os != null) {
               try {
                  ((OutputStream)os).close();
               } catch (IOException var12) {
               }
            }
         } finally {
            this.writerLock.writeLock().unlock();
         }

      }
   }
}

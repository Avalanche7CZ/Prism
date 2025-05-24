package com.helion3.prism.libs.org.apache.juli;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateFormatCache {
   private static final String msecPattern = "#";
   private final String format;
   private int cacheSize = 0;
   private Cache cache;

   private String tidyFormat(String format) {
      boolean escape = false;
      StringBuilder result = new StringBuilder();
      int len = format.length();

      for(int i = 0; i < len; ++i) {
         char x = format.charAt(i);
         if (!escape && x == 'S') {
            result.append("#");
         } else {
            result.append(x);
         }

         if (x == '\'') {
            escape = !escape;
         }
      }

      return result.toString();
   }

   public DateFormatCache(int size, String format, DateFormatCache parent) {
      this.cacheSize = size;
      this.format = this.tidyFormat(format);
      Cache parentCache = null;
      if (parent != null) {
         synchronized(parent) {
            parentCache = parent.cache;
         }
      }

      this.cache = new Cache(parentCache);
   }

   public String getFormat(long time) {
      return this.cache.getFormat(time);
   }

   private class Cache {
      private long previousSeconds;
      private String previousFormat;
      private long first;
      private long last;
      private int offset;
      private final Date currentDate;
      private String[] cache;
      private SimpleDateFormat formatter;
      private Cache parent;

      private Cache(Cache parent) {
         this.previousSeconds = Long.MIN_VALUE;
         this.previousFormat = "";
         this.first = Long.MIN_VALUE;
         this.last = Long.MIN_VALUE;
         this.offset = 0;
         this.currentDate = new Date();
         this.parent = null;
         this.cache = new String[DateFormatCache.this.cacheSize];

         for(int i = 0; i < DateFormatCache.this.cacheSize; ++i) {
            this.cache[i] = null;
         }

         this.formatter = new SimpleDateFormat(DateFormatCache.this.format, Locale.US);
         this.formatter.setTimeZone(TimeZone.getDefault());
         this.parent = parent;
      }

      private String getFormat(long time) {
         long seconds = time / 1000L;
         if (seconds == this.previousSeconds) {
            return this.previousFormat;
         } else {
            this.previousSeconds = seconds;
            int index = (this.offset + (int)(seconds - this.first)) % DateFormatCache.this.cacheSize;
            if (index < 0) {
               index += DateFormatCache.this.cacheSize;
            }

            if (seconds >= this.first && seconds <= this.last) {
               if (this.cache[index] != null) {
                  this.previousFormat = this.cache[index];
                  return this.previousFormat;
               }
            } else {
               int i;
               if (seconds < this.last + (long)DateFormatCache.this.cacheSize && seconds > this.first - (long)DateFormatCache.this.cacheSize) {
                  if (seconds > this.last) {
                     for(i = 1; (long)i < seconds - this.last; ++i) {
                        this.cache[(index + DateFormatCache.this.cacheSize - i) % DateFormatCache.this.cacheSize] = null;
                     }

                     this.first = seconds - (long)(DateFormatCache.this.cacheSize - 1);
                     this.last = seconds;
                     this.offset = (index + 1) % DateFormatCache.this.cacheSize;
                  } else if (seconds < this.first) {
                     for(i = 1; (long)i < this.first - seconds; ++i) {
                        this.cache[(index + i) % DateFormatCache.this.cacheSize] = null;
                     }

                     this.first = seconds;
                     this.last = seconds + (long)(DateFormatCache.this.cacheSize - 1);
                     this.offset = index;
                  }
               } else {
                  this.first = seconds;
                  this.last = this.first + (long)DateFormatCache.this.cacheSize - 1L;
                  index = 0;
                  this.offset = 0;

                  for(i = 1; i < DateFormatCache.this.cacheSize; ++i) {
                     this.cache[i] = null;
                  }
               }
            }

            if (this.parent != null) {
               synchronized(this.parent) {
                  this.previousFormat = this.parent.getFormat(time);
               }
            } else {
               this.currentDate.setTime(time);
               this.previousFormat = this.formatter.format(this.currentDate);
            }

            this.cache[index] = this.previousFormat;
            return this.previousFormat;
         }
      }

      // $FF: synthetic method
      Cache(Cache x1, Object x2) {
         this(x1);
      }
   }
}

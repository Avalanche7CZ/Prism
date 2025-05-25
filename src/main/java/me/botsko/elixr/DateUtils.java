package me.botsko.elixr;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateUtils {
   public static String translateTimeStringToFutureDate(String arg_value) {
      String dateFrom = null;
      Pattern p = Pattern.compile("([0-9]+)(s|h|m|d|w)");
      Calendar cal = Calendar.getInstance();
      String[] matches = me.botsko.elixr.TypeUtils.preg_match_all(p, arg_value);
      if (matches.length > 0) {
         String[] var5 = matches;
         int var6 = matches.length;

         for(int var7 = 0; var7 < var6; ++var7) {
            String match = var5[var7];
            Matcher m = p.matcher(match);
            if (m.matches() && m.groupCount() == 2) {
               int tfValue = Integer.parseInt(m.group(1));
               String tfFormat = m.group(2);
               if (tfFormat.equals("w")) {
                  cal.add(3, tfValue);
               } else if (tfFormat.equals("d")) {
                  cal.add(5, tfValue);
               } else if (tfFormat.equals("h")) {
                  cal.add(10, tfValue);
               } else if (tfFormat.equals("m")) {
                  cal.add(12, tfValue);
               } else if (tfFormat.equals("s")) {
                  cal.add(13, tfValue);
               }
            }
         }

         SimpleDateFormat form = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
         dateFrom = form.format(cal.getTime());
      }

      return dateFrom;
   }

   public static String getTimeSince(String date) {
      String time_ago = "";

      try {
         Date start = (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).parse(date);
         Date end = new Date();
         long diffInSeconds = (end.getTime() - start.getTime()) / 1000L;
         long[] diff = new long[]{diffInSeconds /= 24L, (diffInSeconds /= 60L) >= 24L ? diffInSeconds % 24L : diffInSeconds, (diffInSeconds /= 60L) >= 60L ? diffInSeconds % 60L : diffInSeconds, diffInSeconds >= 60L ? diffInSeconds % 60L : diffInSeconds};
         if (diff[0] > 1L) {
            time_ago = time_ago + diff[0] + "d";
         }

         if (diff[1] >= 1L) {
            time_ago = time_ago + diff[1] + "h";
         }

         if (diff[2] > 1L && diff[2] < 60L) {
            time_ago = time_ago + diff[2] + "m";
         }

         if (!time_ago.isEmpty()) {
            time_ago = time_ago + " ago";
         }

         if (diff[0] == 0L && diff[1] == 0L && diff[2] <= 1L) {
            time_ago = "just now";
         }

         return time_ago;
      } catch (ParseException var7) {
         var7.printStackTrace();
         return "";
      }
   }

   public static String getTimeUntil(String date) {
      String time_ago = "";

      try {
         Date start = (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).parse(date);
         Date end = new Date();
         long diffInSeconds = (start.getTime() - end.getTime()) / 1000L;
         long[] diff = new long[]{diffInSeconds /= 24L, (diffInSeconds /= 60L) >= 24L ? diffInSeconds % 24L : diffInSeconds, (diffInSeconds /= 60L) >= 60L ? diffInSeconds % 60L : diffInSeconds, diffInSeconds >= 60L ? diffInSeconds % 60L : diffInSeconds};
         if (diff[0] > 1L) {
            time_ago = time_ago + diff[0] + "d";
         }

         if (diff[1] >= 1L) {
            time_ago = time_ago + diff[1] + "h";
         }

         if (diff[2] > 1L && diff[2] < 60L) {
            time_ago = time_ago + diff[2] + "m";
         }

         if (diff[0] == 0L && diff[1] == 0L && diff[2] <= 1L) {
            time_ago = "now";
         }

         return time_ago;
      } catch (ParseException var7) {
         var7.printStackTrace();
         return "";
      }
   }
}

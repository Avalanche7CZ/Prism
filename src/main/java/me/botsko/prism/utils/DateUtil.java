package me.botsko.prism.utils;

import com.helion3.prism.libs.elixr.TypeUtils;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateUtil {
   public static Long translateTimeStringToDate(String arg_value) {
      Long dateFrom = 0L;
      Pattern p = Pattern.compile("([0-9]+)(s|h|m|d|w)");
      Calendar cal = Calendar.getInstance();
      String[] matches = TypeUtils.preg_match_all(p, arg_value);
      if (matches.length > 0) {
         String[] arr$ = matches;
         int len$ = matches.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            String match = arr$[i$];
            Matcher m = p.matcher(match);
            if (m.matches() && m.groupCount() == 2) {
               int tfValue = Integer.parseInt(m.group(1));
               String tfFormat = m.group(2);
               if (tfFormat.equals("w")) {
                  cal.add(3, -1 * tfValue);
               } else if (tfFormat.equals("d")) {
                  cal.add(5, -1 * tfValue);
               } else if (tfFormat.equals("h")) {
                  cal.add(10, -1 * tfValue);
               } else if (tfFormat.equals("m")) {
                  cal.add(12, -1 * tfValue);
               } else {
                  if (!tfFormat.equals("s")) {
                     return null;
                  }

                  cal.add(13, -1 * tfValue);
               }
            }
         }

         dateFrom = cal.getTime().getTime();
      }

      return dateFrom;
   }
}

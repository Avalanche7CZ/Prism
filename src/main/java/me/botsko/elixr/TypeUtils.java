package me.botsko.elixr;

import java.text.DecimalFormat;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.ChatColor;

public class TypeUtils {
   public static boolean isNumeric(String str) {
      try {
         Integer.parseInt(str);
         return true;
      } catch (NumberFormatException var2) {
         return false;
      }
   }

   public static float formatDouble(double val) {
      return Float.parseFloat((new DecimalFormat("#.##")).format(val));
   }

   public static String getStringFromTemplate(String msg, Hashtable replacer) {
      Map.Entry entry;
      if (msg != null && !replacer.isEmpty()) {
         for(Iterator var2 = replacer.entrySet().iterator(); var2.hasNext(); msg = msg.replace("%(" + (String)entry.getKey() + ")", (CharSequence)entry.getValue())) {
            entry = (Map.Entry)var2.next();
         }
      }

      return msg;
   }

   public static String colorize(String text) {
      return ChatColor.translateAlternateColorCodes('&', text);
   }

   public static String stripTextFormatCodes(String text) {
      return ChatColor.stripColor(text.replaceAll("(&+([a-z0-9A-Z])+)", ""));
   }

   public static String join(List s, String delimiter) {
      StringBuffer buffer = new StringBuffer();
      Iterator iter = s.iterator();

      while(iter.hasNext()) {
         buffer.append(iter.next());
         if (iter.hasNext()) {
            buffer.append(delimiter);
         }
      }

      return buffer.toString();
   }

   public static String join(String[] inputArray, String glueString) {
      String output = "";
      if (inputArray.length > 0) {
         StringBuilder sb = new StringBuilder();
         if (!inputArray[0].isEmpty()) {
            sb.append(inputArray[0]);
         }

         for(int i = 1; i < inputArray.length; ++i) {
            if (!inputArray[i].isEmpty()) {
               if (sb.length() > 0) {
                  sb.append(glueString);
               }

               sb.append(inputArray[i]);
            }
         }

         output = sb.toString();
      }

      return output;
   }

   public static String strToUpper(String s) {
      return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
   }

   public static String[] preg_match_all(Pattern p, String subject) {
      Matcher m = p.matcher(subject);
      StringBuilder out = new StringBuilder();

      boolean split;
      for(split = false; m.find(); split = true) {
         out.append(m.group());
         out.append("~");
      }

      return split ? out.toString().split("~") : new String[0];
   }

   public static int subStrOccurences(String str, String findStr) {
      int lastIndex = 0;
      int count = 0;

      while(lastIndex != -1) {
         lastIndex = str.indexOf(findStr, lastIndex);
         if (lastIndex != -1) {
            ++count;
            lastIndex += findStr.length();
         }
      }

      return count;
   }

   public static String padStringRight(String str, int desiredLength) {
      if (str.length() >= desiredLength) {
         return str.substring(0, desiredLength);
      } else {
         StringBuilder sb = new StringBuilder();
         int rest = desiredLength - str.length();

         for(int i = 1; i < rest; ++i) {
            sb.append(" ");
         }

         return str + sb.toString();
      }
   }
}

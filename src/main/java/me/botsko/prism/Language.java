package me.botsko.prism;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import org.bukkit.configuration.file.FileConfiguration;

public class Language {
   protected final FileConfiguration lang;

   public Language(FileConfiguration lang) {
      this.lang = lang;
   }

   public String getString(String key) {
      if (this.lang != null) {
         String msg = this.lang.getString(key);
         if (msg != null) {
            return this.colorize(msg);
         }
      }

      return "";
   }

   public String getString(String key, Hashtable replacer) {
      String msg = this.getString(key);
      Map.Entry entry;
      if (!replacer.isEmpty()) {
         for(Iterator i$ = replacer.entrySet().iterator(); i$.hasNext(); msg = msg.replace("%(" + (String)entry.getKey() + ")", (CharSequence)entry.getValue())) {
            entry = (Map.Entry)i$.next();
         }
      }

      return msg;
   }

   protected String colorize(String text) {
      return text.replaceAll("(&([a-f0-9A-F]))", "§$2");
   }
}

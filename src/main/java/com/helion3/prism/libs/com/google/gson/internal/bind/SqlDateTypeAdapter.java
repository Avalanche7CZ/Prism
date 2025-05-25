package com.helion3.prism.libs.com.google.gson.internal.bind;

import com.helion3.prism.libs.com.google.gson.Gson;
import com.helion3.prism.libs.com.google.gson.JsonSyntaxException;
import com.helion3.prism.libs.com.google.gson.TypeAdapter;
import com.helion3.prism.libs.com.google.gson.TypeAdapterFactory;
import com.helion3.prism.libs.com.google.gson.reflect.TypeToken;
import com.helion3.prism.libs.com.google.gson.stream.JsonReader;
import com.helion3.prism.libs.com.google.gson.stream.JsonToken;
import com.helion3.prism.libs.com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.sql.Date;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public final class SqlDateTypeAdapter extends TypeAdapter {
   public static final TypeAdapterFactory FACTORY = new TypeAdapterFactory() {
      @Override
      public TypeAdapter create(Gson gson, TypeToken typeToken) {
         return typeToken.getRawType() == Date.class ? new SqlDateTypeAdapter() : null;
      }
   };
   private final DateFormat format = new SimpleDateFormat("MMM d, yyyy"); // Corrected format string

   @Override
   public Object read(JsonReader in) throws IOException { // Changed to Object
      if (in.peek() == JsonToken.NULL) {
         in.nextNull();
         return null;
      } else {
         try {
            long utilDate = this.format.parse(in.nextString()).getTime();
            return new Date(utilDate);
         } catch (ParseException var5) {
            throw new JsonSyntaxException(var5);
         }
      }
   }

   @Override
   public void write(JsonWriter out, Object value) throws IOException { // Changed to Object
      out.value(value == null ? null : this.format.format((Date) value)); // Cast back to Date
   }
}

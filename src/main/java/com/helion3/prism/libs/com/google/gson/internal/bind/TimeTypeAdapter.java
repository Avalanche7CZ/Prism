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
import java.sql.Time;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class TimeTypeAdapter extends TypeAdapter {
   public static final TypeAdapterFactory FACTORY = new TypeAdapterFactory() {
      @Override
      public TypeAdapter create(Gson gson, TypeToken typeToken) {
         return typeToken.getRawType() == Time.class ? new TimeTypeAdapter() : null;
      }
   };
   private final DateFormat format = new SimpleDateFormat("hh:mm:ss a");

   @Override
   public Object read(JsonReader in) throws IOException { // Changed to Object
      if (in.peek() == JsonToken.NULL) {
         in.nextNull();
         return null;
      } else {
         try {
            Date date = this.format.parse(in.nextString());
            return new Time(date.getTime());
         } catch (ParseException var3) {
            throw new JsonSyntaxException(var3);
         }
      }
   }

   @Override
   public void write(JsonWriter out, Object value) throws IOException { // Changed to Object
      out.value(value == null ? null : this.format.format((Time) value)); // Cast back to Time
   }
}

package com.helion3.pste.api;

import com.helion3.prism.libs.com.google.gson.Gson;
import com.helion3.prism.libs.com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.xml.bind.DatatypeConverter;

public class PsteApi {
   private final String apiUsername;
   private final String apiKey;
   private final String apiUrl = "https://pste.me/api/v1/";

   public PsteApi(String apiUsername, String apiKey) {
      this.apiUsername = apiUsername;
      this.apiKey = apiKey;
   }

   public Results createPaste(Paste paste) throws IOException {
      Gson gson = (new GsonBuilder()).disableHtmlEscaping().create();
      String jsonPayload = gson.toJson((Object)paste);
      URL url = new URL("https://pste.me/api/v1//paste/");
      HttpURLConnection connection = (HttpURLConnection)url.openConnection();
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Content-Length", Integer.toString(jsonPayload.getBytes().length));
      connection.setRequestProperty("Content-Language", "en-US");
      connection.setRequestProperty("Authorization", "Basic " + DatatypeConverter.printBase64Binary((this.apiUsername + ":" + this.apiKey).getBytes()));
      connection.setUseCaches(false);
      connection.setDoInput(true);
      connection.setDoOutput(true);
      DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
      wr.writeBytes(jsonPayload);
      wr.flush();
      wr.close();
      InputStream is = connection.getInputStream();
      BufferedReader rd = new BufferedReader(new InputStreamReader(is));
      String json = this.readAll(rd);
      Results response = (Results)gson.fromJson(json, Results.class);
      return response;
   }

   private String readAll(Reader rd) throws IOException {
      StringBuilder sb = new StringBuilder();

      int cp;
      while((cp = rd.read()) != -1) {
         sb.append((char)cp);
      }

      return sb.toString();
   }
}

package com.gamuphi.slickmap;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.http.HttpResponseCache;

public class MapboxTileSource implements TileSource {

  public class InvalidVersion extends Exception {

    private static final long serialVersionUID = 1L;
    
  }
  String[] urls;
  int current_url = 0;
  

  public MapboxTileSource(String tilejson_url) throws IOException, JSONException, InvalidVersion {

      URL url = new URL(tilejson_url);
      HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
      InputStream in = new BufferedInputStream(urlConnection.getInputStream());

      StringBuilder x = new StringBuilder();
      byte[] buffer = new byte[256];
      int read = 0;
      while((read = in.read(buffer )) >= 0 ) {
        x.append(new String(buffer, 0, read));
      }
      JSONObject obj = new JSONObject(x.toString());
      JSONArray j_urls = obj.getJSONArray("tiles");

      
      urls = new String[j_urls.length()];
      for(int i = 0;i < j_urls.length();i++) {
        urls[i] = j_urls.getString(i);
      }


  }
  
  @Override
  public void close() {
  }

  @Override
  public void cancel() {
    
  }


  @Override
  public Bitmap getTile(int x, int y, int z) {
    HttpURLConnection urlConnection = null;
    try {
      String urlString = urls[current_url++];
      if (current_url >= urls.length)
        current_url = 0;
      
      urlString = urlString.replaceFirst("\\{x\\}", Integer.toString(x));
      urlString = urlString.replaceFirst("\\{y\\}", Integer.toString(y));
      urlString = urlString.replaceFirst("\\{z\\}", Integer.toString(z));
       
//      Logger.debug(String.format("GET: %s", urlString));
//      HttpResponseCache c = HttpResponseCache.getInstalled();
//      Logger.debug(String.format("getHitCount(): %d, getRequestCount(): %d, getNetworkCount(): %d",
//          c.getHitCount(), c.getRequestCount(), c.getNetworkCount()));
      URL url = new URL(urlString);
      urlConnection = (HttpURLConnection) url.openConnection();
      InputStream in = new BufferedInputStream(urlConnection.getInputStream());
      return BitmapFactory.decodeStream(in);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    } finally {
      if (urlConnection != null)
        urlConnection.disconnect();
    }
  }
}

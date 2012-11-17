package com.gamuphi.slickmap;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

public interface TileSource {

  public void close();
  public void cancel();

  public Bitmap   getTile(int x, int y, int z);


}
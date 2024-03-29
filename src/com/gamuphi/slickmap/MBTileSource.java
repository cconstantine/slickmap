package com.gamuphi.slickmap;

import java.io.File;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class MBTileSource implements TileSource {
  private SQLiteDatabase db;

  public MBTileSource(Context ctx, File dbpath) {
    db = SQLiteDatabase.openDatabase(dbpath.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
  }
  
  public void close() {
    if (db.isOpen())
      db.close();
  }

  public Bitmap getTile(int x, int y, int z) {
    Cursor c = db.rawQuery(
        "select tile_data from tiles where tile_column=? and tile_row=? and zoom_level=?",
        new String[]{Integer.toString(x),Integer.toString(y), Integer.toString(z)}
        );
    if (!c.moveToFirst()) {
      c.close();
      return null;
    }
    byte[] bb = c.getBlob(c.getColumnIndex("tile_data"));
    c.close();
    return BitmapFactory.decodeByteArray(bb, 0, bb.length);
    
  }

  @Override
  public void cancel() { }

}

package com.gamuphi.slickmap;

import java.io.File;
import java.util.ArrayDeque;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import android.content.Context;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.TextView;

class SerialExecutor implements Executor {
  final ArrayDeque<Runnable> mTasks = new ArrayDeque<Runnable>();
  Runnable mActive;

  public synchronized void execute(final Runnable r) {
    mTasks.offer(new Runnable() {
      public void run() {
        try {
          r.run();
        } finally {
          scheduleNext();
        }
      }
    });
    if (mActive == null) {
      scheduleNext();
    }
  }

  protected synchronized void scheduleNext() {
    if ((mActive = mTasks.poll()) != null) {
      AsyncTask.THREAD_POOL_EXECUTOR.execute(mActive);
    }
  }
}

class Tile extends TextView {

  protected Point loc;
  
  private final File sdcard = Environment.getExternalStorageDirectory();
  private final String db_name = "map.mbtiles"; 
  private final File sqlitefile = new File(sdcard, db_name); // sqlite file to load 

  static private MBTileSource tile_source;
  private Executor executor = new SerialExecutor();
  
  public Tile(Context context) {
    super(context);
    loc = new Point(0, 0);
    if (tile_source == null) 
      tile_source = new MBTileSource(context, sqlitefile);


    executor = new SerialExecutor();
    Drawable d = getContext().getResources().getDrawable(R.drawable.kitty);
    this.setBackgroundDrawable(d);
  }

  public Point offset;

  private AsyncTask<Point, Integer, Drawable> at;

  public void setLoc(int x, int y, final int zoom) {
    loc.x = x;
    loc.y = y;

    int dim = (int) Math.pow(2, zoom);
    y = dim - y;

    Drawable d = getContext().getResources().getDrawable(R.drawable.kitty);

    this.setBackgroundDrawable(d);
    
    if(at != null) {
      at.cancel(true);
    }
    
    at = new AsyncTask<Point, Integer, Drawable>() {
      protected Drawable doInBackground(Point... points) {
        return tile_source.getTileAsDrawable(points[0].x, points[0].y, zoom);
      }


      protected void onPostExecute(Drawable result) {
        Tile.this.setBackgroundDrawable(result);
      }
    };
    boolean posted = false;
    while(!posted) {
      try {
        at.executeOnExecutor(executor, new Point(x, y));
        posted = true;
      } catch (RejectedExecutionException e) { 
      } catch (IllegalStateException e) { posted = true;}
    }
  }
  
  public Point getLoc() {
    return loc;
  }

}
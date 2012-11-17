package com.gamuphi.slickmap;

import java.util.ArrayDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;


import android.content.Context;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
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

public class Tile extends TextView {

  protected Point loc;
  
  static private CountDownLatch readyLock = new CountDownLatch(1);
  static private TileSource tile_source;
  private Executor executor = new SerialExecutor();
  
  public Tile(Context context) {
    super(context);
    loc = new Point(0, 0);

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

    Drawable d = getContext().getResources().getDrawable(R.drawable.test);

    this.setBackgroundDrawable(d);
    
    if(at != null) {
      at.cancel(true);
    }
    
    at = new AsyncTask<Point, Integer, Drawable>() {
      protected Drawable doInBackground(Point... points) {
        try {
          readyLock.await();
          return new BitmapDrawable(tile_source.getTile(points[0].x, points[0].y, zoom));
        } catch (InterruptedException e) {
          e.printStackTrace();
          return null;
        }
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
  
  static public void setTileSource(TileSource ts) {
    readyLock.countDown();
    tile_source = ts;
  }
  
  public Point getLoc() {
    return loc;
  }

}
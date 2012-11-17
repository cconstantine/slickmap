package com.gamuphi.slickmap;

import android.content.Context;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.widget.FrameLayout;
import android.widget.GridView;

import android.widget.Scroller;

public class MapFrameView extends FrameLayout {

  protected GestureDetector mGD;

  Tile[] views = null;
  static int subview_dim = 256;

  protected boolean resumed = false;

  protected int layout_width;
  protected int layout_height;

  protected int rows;
  protected int cols;

  protected Point view_offset;

  protected int zoom;

  protected int map_dim;

  protected Scroller scroller;

  protected boolean mIsBeingDragged;

  protected int maxY;
  protected int maxX;

  protected Point lower_right;

  private float x_ratio;
  private float y_ratio;


  public MapFrameView(Context context) {
    super(context);
//    Logger.debug("MapFrameView::MapFrameView(Context context)");
    
    init(8);
  }

  public MapFrameView(Context context, AttributeSet attrs) {
    super(context, attrs);
//    Logger.debug("MapFrameView::MapFrameView(Context context, AttributeSet attrs)");
    init(8);
  }

  public MapFrameView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
//    Logger.debug("MapFrameView::MapFrameView(Context context, AttributeSet attrs, int defStyle)");
    init(8);
  }

  private void init(int zoom_level) {
    view_offset = new Point(0, 0);
    lower_right = new Point(0, 0);
    x_ratio = 0.5f;
    y_ratio = 0.5f;

    zoom = zoom_level;
    map_dim = (int) Math.pow(2, zoom);
    setSaveEnabled(true);

    scroller = new Scroller(getContext());
    mIsBeingDragged = false;

    mGD = new GestureDetector(getContext(), new SimpleOnGestureListener() {

      @Override
      public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
          float distanceY) {
        MapFrameView.this.slide(distanceX, distanceY);
        return true;
      }

      @Override
      public boolean onFling(MotionEvent e1, MotionEvent e2, float vX, float vY) {
        MapFrameView.this.fling(vX, vY);
        return true;
      }

      @Override
      public boolean onDown(MotionEvent e) {
        scroller.abortAnimation();
        mIsBeingDragged = !scroller.isFinished();
        return true;
      }
    });

  }
  
  public void setTileSource(TileSource ts) {
    Tile.setTileSource(ts);
  }

  @Override
  protected Parcelable onSaveInstanceState() {
//    Logger.debug("onSaveInstanceState");
//    Logger.debug(String.format("MapFrameView::onSaveInstanceState() x_ratio: %f, y_ratio: %f", x_ratio, y_ratio));
    return new SaveRatio( super.onSaveInstanceState(), x_ratio, y_ratio);
  }

  @Override
  protected void onRestoreInstanceState(Parcelable state) {
//    Logger.debug("onRestoreInstanceState");
    if(!(state instanceof SaveRatio)) {
      super.onRestoreInstanceState(state);
      return;
    } 

    SaveRatio ss = (SaveRatio)state;
    super.onRestoreInstanceState(ss.getSuperState());
    //end

    this.x_ratio = ss.x_ratio;
    this.y_ratio = ss.y_ratio;
//    Logger.debug(String.format("MapFrameView::onRestoreInstanceState() x_ratio: %f, y_ratio: %f", x_ratio, y_ratio));
  }

  @Override
  public void computeScroll() {
    if (scroller.computeScrollOffset()) {
      slide(view_offset.x - scroller.getCurrX(),
          view_offset.y - scroller.getCurrY());
      invalidate();
    }
  }

  public void fling(float vX, float vY) {
    scroller.fling(view_offset.x, view_offset.y, (int) vX, (int) vY,
        view_offset.x - 10000, view_offset.x + 10000, view_offset.y - 10000,
        view_offset.y + 10000);

    MapFrameView.this.invalidate();
  }

  synchronized public void slide(float distanceX, float distanceY) {

    if (views != null) {
      int delta_x = (int) distanceX;
      int delta_y = (int) distanceY;
  
      if (view_offset.y - delta_y > 0) {
        delta_y = view_offset.y;
      } else if (view_offset.y - delta_y < -lower_right.y) {
        delta_y = view_offset.y + lower_right.y;
      }
  
      view_offset.x -= delta_x;
      view_offset.y -= delta_y;
  
      x_ratio = ((float) -view_offset.x) / lower_right.x;
      y_ratio = ((float) -view_offset.y) / lower_right.y;

      for (Tile vh : MapFrameView.this.views) {
        vh.offset.x -= delta_x;
        vh.offset.y -= delta_y;

        Point loc = vh.getLoc();

        int loc_x = loc.x;
        int loc_y = loc.y;

        while (vh.offset.x < -(subview_dim + (subview_dim >> 1))) {
          vh.offset.x += rows * subview_dim;
          loc_x += rows;
        }

        while (vh.offset.x + subview_dim > layout_width + (subview_dim + (subview_dim >> 1))) {
          vh.offset.x -= rows * subview_dim;
          loc_x -= rows;
        }

        while (loc_y + cols < map_dim && vh.offset.y < -(subview_dim + (subview_dim >> 1))) {
          vh.offset.y += cols * subview_dim;
          loc_y += cols;
        } 
        while(loc_y - cols >= 0
            && vh.offset.y + subview_dim > layout_height
                + (subview_dim + (subview_dim >> 1))) {
          vh.offset.y -= cols * subview_dim;
          loc_y -= cols;
        }

        if (loc_x != loc.x || loc_y != loc.y) {
          if (loc_x < 0) {
            loc_x += map_dim;
          } else {
            loc_x = loc_x % map_dim;
          }
          vh.setLoc(loc_x, loc_y, zoom);
        }
        vh.setTranslationX(vh.offset.x);
        vh.setTranslationY(vh.offset.y);
      }
    }

  }
  
  synchronized protected void changeLayout(int l, int t, int r, int b) {
    layout_width = r - l;
    layout_height = b - t;

    rows = (int) (2 + FloatMath.ceil((float) layout_width / subview_dim));
    cols = (int) (2 + FloatMath.ceil((float) layout_height / subview_dim));

    lower_right.x = map_dim * subview_dim - layout_width;
    lower_right.y = map_dim * subview_dim - layout_height;

    view_offset.x = 0;
    view_offset.y = 0;

    if (rows > map_dim)
      rows = (int) map_dim;
    if (cols > map_dim)
      cols = (int) map_dim;

/*
    Logger.debug(String.format("lower_right.x: %d, lower_right.y: %d", lower_right.x, lower_right.y));
    Logger.debug(String.format("view_offset.x: %d, view_offset.y: %d", view_offset.x, view_offset.y));
    
    Logger.debug(String.format("rows: %d,  cols: %d", rows, cols));
*/
    if (views == null) {
      views = new Tile[rows * cols];
    }

    for (int y = 0; y < cols; y++) {
      for (int x = 0; x < rows; x++) {
        int i = x + y * rows;
        Tile vh = views[i];
        if (vh == null) {
          vh = new Tile(getContext());
          views[i] = vh;
          vh.setLayoutParams(new GridView.LayoutParams(subview_dim,
              subview_dim));

          vh.offset = new Point(x * subview_dim, y * subview_dim);
        } else {
          vh.offset.x = x * subview_dim;
          vh.offset.y = y * subview_dim;
        }
        vh.setLoc(x, y, zoom);

        vh.setTranslationX(vh.offset.x);
        vh.setTranslationY(vh.offset.y);
      }
    }
    this.slide(lower_right.x * x_ratio, lower_right.y * y_ratio);
  }

  @Override
  protected void onLayout(boolean changed, final int l, final int t, final int r, final int b) {
    super.onLayout(changed, l, t, r, b);

    
    if (changed) {
      AsyncTask<Void, Void, Void> at = new AsyncTask<Void, Void, Void>() {

        @Override
        protected Void doInBackground(Void... params) {

          MapFrameView.this.changeLayout(l,  t,  r,  b);
          return null;
        }
        
        @Override
        protected void onPostExecute(Void voider) {
          for(Tile v : views) {
            MapFrameView.this.addView(v);
          }
        }
        
      };
      at.execute();
    }
  }

  @Override
  public boolean onTouchEvent(MotionEvent me) {
    return mGD.onTouchEvent(me);
  }
}
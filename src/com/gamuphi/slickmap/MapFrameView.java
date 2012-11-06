package com.gamuphi.slickmap;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.util.FloatMath;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Scroller;
import android.widget.TextView;

class Tile extends TextView{

  private Point loc;
  
  public Tile(Context context) {
    super(context);
    loc = new Point(0, 0);

    Drawable d = getContext().getResources().getDrawable(R.drawable.kitty);
    this.setBackgroundDrawable(d);
  }

  public Point offset;
  

  public void setLoc(int x, int y) {
    loc.x = x;
    loc.y = y;
    String t = String.format("(%d, %d)", x, y);
    this.setText(t);
  }
  
  public Point getLoc() {
    return loc;
  }

}


public class MapFrameView extends FrameLayout {

  private GestureDetector mGD;
  
  Tile[] views = null;
  static int subview_dim = 256;

  private boolean resumed = false;

  private int layout_width;

  private int layout_height;

  private int rows;

  private int cols;

  private Point view_offset;

  private int zoom;

  private int map_dim;

  private Scroller scroller;

  private boolean mIsBeingDragged;

  private int maxY;

  private int maxX;

  private Point lower_right;
  
  public MapFrameView(Context context, int zoom_level) {
    super(context);
    view_offset = new Point(0, 0);
    lower_right = new Point(0, 0);
    zoom = zoom_level;
    
    map_dim = (int)Math.pow(2, zoom);
    Logger.debug(String.format("map_dim: %d", map_dim));
    scroller = new Scroller(context);
    mIsBeingDragged = false;
    
    
    mGD = new GestureDetector(getContext(),
        new SimpleOnGestureListener() {

          @Override
          public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
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

  @Override
  public void computeScroll() {
    if (scroller.computeScrollOffset()) {
      Logger.debug("computeScroll");
      Logger.debug(String.format("getCurrX(): %d, getCurrY(): %d", scroller.getCurrX(), scroller.getCurrY()));
      slide(view_offset.x - scroller.getCurrX(), view_offset.y - scroller.getCurrY());
      invalidate();
    }
  }
  
  public void fling(float vX, float vY) {
    Logger.debug(String.format("vX: %f,  vY: %f", vX, vY));
    
    scroller.fling(
        view_offset.x, view_offset.y,
        (int)vX, (int)vY,
        view_offset.x - 10000, view_offset.x + 10000,
        view_offset.y - 10000, view_offset.y + 10000);
    
    MapFrameView.this.invalidate();
  }
  public void slide(float distanceX, float distanceY ) {
    int delta_x = (int) distanceX;
    int delta_y = (int) distanceY;

    if (view_offset.y - delta_y > 0) {
      delta_y = view_offset.y;
    } else if( view_offset.y - delta_y < -lower_right.y) {
      delta_y = view_offset.y + lower_right.y;
    }

    Logger.debug(String.format("view_offset.x: %d, view_offset.y: %d", view_offset.x, view_offset.y));
    Logger.debug(String.format("lower_right.x: %d, lower_right.y: %d", lower_right.x, lower_right.y));
    Logger.debug(String.format("delta_x: %d, delta_y: %d", delta_x, delta_y));
    
    view_offset.x -= delta_x;
    view_offset.y -= delta_y;
    
    if (views != null) {
      for (Tile vh : MapFrameView.this.views) {
        vh.offset.x -= delta_x;
        vh.offset.y -= delta_y;

        Point loc = vh.getLoc();

        int loc_x = loc.x;
        int loc_y = loc.y;
        
        if (vh.offset.x < - (subview_dim + (subview_dim >> 1))) {
          vh.offset.x += rows * subview_dim;
          loc_x += rows;
          
        } else if (vh.offset.x + subview_dim > layout_width + (subview_dim + (subview_dim >> 1))) {
          vh.offset.x -= rows * subview_dim;
          loc_x -= rows;
        }

        if (loc_y + cols <= map_dim && vh.offset.y < - (subview_dim + (subview_dim >> 1))) {
          vh.offset.y += cols * subview_dim;
          loc_y += cols;
        } else if (loc_y - cols >= 0 && vh.offset.y + subview_dim > layout_height + (subview_dim + (subview_dim >> 1))) {
          vh.offset.y -= cols * subview_dim;
          loc_y -= cols;
        }
        
        if (loc_x != loc.x || loc_y != loc.y){
          if (loc_x < 0)
            loc_x += map_dim;
          else
            loc_x = loc_x % map_dim;
          vh.setLoc(loc_x, loc_y);
        }
        vh.setTranslationX(vh.offset.x);
        vh.setTranslationY(vh.offset.y);
      }
    }

  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b)
  {
    super.onLayout(changed, l, t, r, b);
    Logger.debug(String.format("changed: %b, l: %d, t: %d, r: %d, b: %d", changed, l, t, r, b));

    if (changed) {

      layout_width = r-l;
      layout_height = b-t;
      
      rows = (int) (2 + FloatMath.ceil((float)layout_width / subview_dim));
      cols = (int) (2 + FloatMath.ceil((float)layout_height / subview_dim));
      
      float x_ratio = ((float)view_offset.x) / lower_right.x;
      float y_ratio = ((float)view_offset.y) / lower_right.y;
      
      lower_right.x = (map_dim+1)*subview_dim - layout_width;
      lower_right.y = (map_dim+1)*subview_dim     - layout_height;

      view_offset.x = (int)(lower_right.x * x_ratio);
      view_offset.y = (int)(lower_right.y * y_ratio);
      
      Logger.debug(String.format("lower_right.x: %d, lower_right.y: %d", lower_right.x, lower_right.y));
      if (rows > map_dim)
        rows = (int) map_dim;
      if (cols > map_dim)
        cols = (int) map_dim;
      
      Logger.debug(String.format("rows: %d,  cols: %d", rows, cols));

      Tile[] newViews = new Tile[rows*cols];
      this.removeAllViews();
      for(int y = 0;y < cols;y++) {
        for(int x = 0;x < rows;x++) {
          Tile vh = new Tile(getContext());

          vh.setLayoutParams(new GridView.LayoutParams(subview_dim, subview_dim));
          //vh.setScaleType(ImageView.ScaleType.CENTER_CROP);
          
          vh.offset = new Point(x*subview_dim, y*subview_dim);
          vh.setLoc(x, y);
          newViews[x + y*rows] = vh;
          addView(vh);
          
//          Logger.debug(String.format("(%d, %d) offset.x: %d, offset.y: %d", x, y, vh.offset.x, vh.offset.y));
          
          vh.setTranslationX(vh.offset.x);
          vh.setTranslationY(vh.offset.y);
        }
      }

      views = newViews;
    }
  }
  

  @Override
  public boolean onTouchEvent(MotionEvent me) {
    return mGD.onTouchEvent(me);
  }
}
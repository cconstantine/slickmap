package com.gamuphi.slickmap;

import android.content.Context;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;
import android.widget.FrameLayout;

public class SlickMap extends FrameLayout {

  private MapFrameView layer;
  private GestureDetector mGD;
  private ScaleGestureDetector mSD;

  public SlickMap(Context context) {
    super(context);
//    Logger.debug("MapFrameView::MapFrameView(Context context)");
    
    layer = new MapFrameView(context);
    init();
  }

  public SlickMap(Context context, AttributeSet attrs) {
    super(context, attrs);
//    Logger.debug("MapFrameView::MapFrameView(Context context, AttributeSet attrs)");
    layer = new MapFrameView(context, attrs);
    init();
  }

  public SlickMap(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    layer = new MapFrameView(context, attrs, defStyle);
//    Logger.debug("MapFrameView::MapFrameView(Context context, AttributeSet attrs, int defStyle)");
    init();
  }

  private void init() {
    addView(layer);
    

    mSD = new ScaleGestureDetector(getContext() , new SimpleOnScaleGestureListener() {
      float prevScale;
      @Override
      public boolean onScale(ScaleGestureDetector sgd) {
        float scale = sgd.getPreviousSpan() / sgd.getCurrentSpan();
        if (scale > 2) {
//          layer.setZoom(layer.getZoom() + 1);
          Logger.debug(String.format("Zoom in from %d", layer.getZoom()));
          SlickMap.this.removeView(layer);
          layer = new MapFrameView(getContext(), layer.getZoom() + 1);
          addView(layer);
        } else if (scale < 0.5) {
          Logger.debug(String.format("Zoom out from %d", layer.getZoom()));
//          layer.setZoom(layer.getZoom() - 1);
          SlickMap.this.removeView(layer);
          layer = new MapFrameView(getContext(), layer.getZoom() - 1);
          addView(layer);
        }
        return false;        
      }
    });
    mGD = new GestureDetector(getContext(), new SimpleOnGestureListener() {

      @Override
      public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        SlickMap.this.slide(distanceX, distanceY);
        return false;
      }

      @Override
      public boolean onFling(MotionEvent e1, MotionEvent e2, float vX, float vY) {
        SlickMap.this.fling(vX, vY);
        return false;
      }

      @Override
      public boolean onDown(MotionEvent e) {
        SlickMap.this.stopFling();
        return false;
      }
    });
  }
  
  protected void stopFling() {
    layer.stopFling();
  }

  protected void fling(float vX, float vY) {
    layer.fling(vX, vY);
  }

  @Override
  protected void onLayout(boolean changed, final int l, final int t, final int r, final int b) {
    super.onLayout(changed, l, t, r, b);    
  }

  @Override
  public boolean onTouchEvent(MotionEvent me) {
    mSD.onTouchEvent(me);
    mGD.onTouchEvent(me);
    return true;
  }
  
  public void slide(float distanceX, float distanceY) {
    layer.slide(distanceX, distanceY);
  }
  

  @Override
  protected Parcelable onSaveInstanceState() {
    Logger.debug("SlickMap::onSaveInstanceState()");
    return super.onSaveInstanceState();
  }

  @Override
  protected void onRestoreInstanceState(Parcelable state) {
    Logger.debug("SlickMap::onRestoreInstanceState()");

    if(!(state instanceof BaseSavedState)) {
      super.onRestoreInstanceState(state);
      return;
    } else {
      super.onRestoreInstanceState(((BaseSavedState)state).getSuperState());
      return;
    }
  }
  
  public void setTileSource(TileSource ts) {
    Tile.setTileSource(ts);
  }

}
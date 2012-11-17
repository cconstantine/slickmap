package com.gamuphi.slickmap;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.View.BaseSavedState;

class SaveRatio extends BaseSavedState {
  public float x_ratio;
  public float y_ratio;

  SaveRatio(Parcelable superState, float x_ratio, float y_ratio) {
    super(superState);
    this.x_ratio = x_ratio;
    this.y_ratio = y_ratio;
  }

  private SaveRatio(Parcel in) {
    super(in);
    x_ratio = in.readFloat();
    y_ratio = in.readFloat();
  }

  @Override
  public void writeToParcel(Parcel out, int flags) {
    super.writeToParcel(out, flags);
    out.writeFloat(x_ratio);
    out.writeFloat(y_ratio);
  }

  public static final Parcelable.Creator<SaveRatio> CREATOR = new Parcelable.Creator<SaveRatio>() {
    public SaveRatio createFromParcel(Parcel in) {
      return new SaveRatio(in);
    }

    public SaveRatio[] newArray(int size) {
      return new SaveRatio[size];
    }
  };
}
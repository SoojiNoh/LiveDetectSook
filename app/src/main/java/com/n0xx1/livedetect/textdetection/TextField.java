package com.n0xx1.livedetect.textdetection;

import android.os.Parcel;
import android.os.Parcelable;

/** Information about a text field. */
public class TextField implements Parcelable {

    public static final Creator<TextField> CREATOR =
            new Creator<TextField>() {
                @Override
                public TextField createFromParcel(Parcel in) {
                    return new TextField(in);
                }

                @Override
                public TextField[] newArray(int size) {
                    return new TextField[size];
                }
            };

    final String label;
    final String value;

    public TextField(String label, String value) {
        this.label = label;
        this.value = value;
    }

    private TextField(Parcel in) {
        label = in.readString();
        value = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(label);
        dest.writeString(value);
    }
}
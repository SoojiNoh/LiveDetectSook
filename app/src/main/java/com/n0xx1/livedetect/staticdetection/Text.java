package com.n0xx1.livedetect.staticdetection;

import android.graphics.Bitmap;

import com.google.api.services.vision.v1.model.Vertex;

import java.util.ArrayList;

public class Text {
    final Bitmap image;
    final String text;
    ArrayList<Vertex> vertices;

    Text(Bitmap image, String text, ArrayList<Vertex> vertices) {
//        this.textThumbnail = textThumbnail;
        this.image = null;
        this.text = text;
        this.vertices = vertices;
    }


    public synchronized Bitmap getBitmap() {
//        if (textThumbnail == null) {
//            textThumnail =
//                    Utils.getCornerRoundedBitmap(textThumbnail, objectThumbnailCornerRadius);
//        }
        return image;
    }
}

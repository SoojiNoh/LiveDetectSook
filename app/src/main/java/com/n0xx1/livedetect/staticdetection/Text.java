package com.n0xx1.livedetect.staticdetection;

import android.graphics.Bitmap;

import com.google.api.services.vision.v1.model.Vertex;

import java.util.ArrayList;

public class Text {
    final Bitmap textThumbnail;
    final String text;
    ArrayList<Vertex> vertices;

    Text(Bitmap textThumbnail, String text, ArrayList<Vertex> vertices) {
//        this.textThumbnail = textThumbnail;
        this.textThumbnail = null;
        this.text = text;
        this.vertices = vertices;
    }


//    public synchronized Bitmap getTextThumnail() {
////        if (textThumbnail == null) {
////            textThumnail =
////                    Utils.getCornerRoundedBitmap(textThumbnail, objectThumbnailCornerRadius);
////        }
//        return textThumnail;
//    }
}

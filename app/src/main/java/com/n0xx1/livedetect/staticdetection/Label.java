package com.n0xx1.livedetect.staticdetection;

import android.content.res.Resources;
import android.graphics.Bitmap;

import com.google.api.services.vision.v1.model.Vertex;
import com.n0xx1.livedetect.R;

import java.util.ArrayList;

public class Label {
    final Bitmap image;
    final String label;
    ArrayList<Vertex> vertices;
    int entityThumbnailCornerRadius;

    Label(Resources resources, Bitmap image, String label, ArrayList<Vertex> vertices) {
//        this.textThumbnail = textThumbnail;
        this.image = null;
        this.label = label;
        this.vertices = vertices;
        this.entityThumbnailCornerRadius =
                resources.getDimensionPixelOffset(R.dimen.bounding_box_corner_radius);
    }


    public synchronized Bitmap getBitmap() {
//        if (textThumbnail == null) {
//            textThumnail =
//                    Utils.getCornerRoundedBitmap(textThumbnail, entityThumbnailCornerRadius);
//        }
        return image;
    }
}
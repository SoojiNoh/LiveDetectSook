package com.n0xx1.livedetect.staticdetection;

import android.content.res.Resources;
import android.graphics.Bitmap;

import com.google.api.services.vision.v1.model.Vertex;
import com.n0xx1.livedetect.R;
import com.n0xx1.livedetect.Utils;

import java.util.ArrayList;

public class Text {
    final Bitmap image;
    final String text;
    ArrayList<Vertex> vertices;
    int entityThumbnailCornerRadius;


    Text(Resources resources, Bitmap image, String text, ArrayList<Vertex> vertices) {
//        this.textThumbnail = textThumbnail;
        this.image = null;
        this.text = text;
        this.vertices = vertices;
        this.entityThumbnailCornerRadius =
                resources.getDimensionPixelOffset(R.dimen.bounding_box_corner_radius);
    }


    public synchronized Bitmap getBitmap() {
//        if (textThumbnail == null) {
//            textThumnail =
//                    Utils.getCornerRoundedBitmap(textThumbnail, entityThumbnailCornerRadius);
//        }
        return Utils.getCornerRoundedBitmap(image, entityThumbnailCornerRadius);
    }
}

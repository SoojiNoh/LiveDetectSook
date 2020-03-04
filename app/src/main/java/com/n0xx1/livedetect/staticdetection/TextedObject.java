package com.n0xx1.livedetect.staticdetection;

import android.content.res.Resources;
import android.graphics.Bitmap;

import androidx.annotation.Nullable;

import com.n0xx1.livedetect.R;

import java.util.List;

public class TextedObject {

//    private final DetectedObject object;
    private final List<Text> textList;
    private final int objectThumbnailCornerRadius;

    @Nullable
    private Bitmap textFullImage;

    public TextedObject(Resources resources, List<Text> textList) {
        this.textList = textList;
        this.objectThumbnailCornerRadius =
                resources.getDimensionPixelOffset(R.dimen.bounding_box_corner_radius);
    }

//    public int getObjectIndex() {
//        return object.getObjectIndex();
//    }

    public List<Text> getTextList() {
        return textList;
    }

//    public Rect getBoundingBox() {
//        return new Rect();
//
//    }
//
//    public synchronized Bitmap getTextFullImage() {
//        if (textFullImage == null) {
//            textFullImage =
//                    Utils.getCornerRoundedBitmap(object.getBitmap(), objectThumbnailCornerRadius);
//        }
//        return objectThumbnail;
//    }
}

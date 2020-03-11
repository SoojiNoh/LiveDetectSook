package com.n0xx1.livedetect.staticdetection;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;

import androidx.annotation.Nullable;

import com.n0xx1.livedetect.R;
import com.n0xx1.livedetect.Utils;

import java.util.List;

public class LabeledObject {

    //    private final DetectedObject object;
    private final List<Label> labelList;
    private final int objectThumbnailCornerRadius;

    @Nullable
    private Bitmap image;
    private Bitmap image_rect;


    public LabeledObject(Resources resources, List<Label> labelList, Bitmap image, Bitmap image_rect) {
        this.labelList = labelList;
        this.objectThumbnailCornerRadius =
                resources.getDimensionPixelOffset(R.dimen.bounding_box_corner_radius);
        this.image = image;
        this.image_rect = image_rect;
    }

//    public int getObjectIndex() {
//        return object.getObjectIndex();
//    }

    public List<Label> getLabelList() {
        return labelList;
    }

    public Rect getBoundingBox() {
        return new Rect();

    }
    //
    public synchronized Bitmap getLabelThumbnail(){
        if (image == null) {
            image =
                    Utils.getCornerRoundedBitmap(image, objectThumbnailCornerRadius);
        }
        return image;
    }

    public Bitmap getLabelRectBitmap(){
        if (image == null) {
            image =
                    Utils.getCornerRoundedBitmap(image_rect, objectThumbnailCornerRadius);
        }
        return image_rect;
    }
}
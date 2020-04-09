package com.n0xx1.livedetect.staticdetection;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;

import androidx.annotation.Nullable;

import com.n0xx1.livedetect.R;
import com.n0xx1.livedetect.Utils;

import java.util.List;

public class TextedEntity {

//    private final DetectedEntity entity;
    private final List<Text> textList;
    private final int entityThumbnailCornerRadius;

    @Nullable
    private Bitmap image;
    private Bitmap image_rect;


    public TextedEntity(Resources resources, List<Text> textList, Bitmap image, Bitmap image_rect) {
        this.textList = textList;
        this.entityThumbnailCornerRadius =
                resources.getDimensionPixelOffset(R.dimen.bounding_box_corner_radius);
        this.image = image;
        this.image_rect = image_rect;
    }

//    public int getEntityIndex() {
//        return entity.getEntityIndex();
//    }

    public List<Text> getTextList() {
        return textList;
    }

    public Rect getBoundingBox() {
        return new Rect();

    }
//
    public synchronized Bitmap getTextThumbnail(){
        if (image == null) {
            image =
                    Utils.getCornerRoundedBitmap(image, entityThumbnailCornerRadius);
        }
        return image;
    }

    public Bitmap getTextRectBitmap(){
        if (image == null) {
            image =
                    Utils.getCornerRoundedBitmap(image_rect, entityThumbnailCornerRadius);
        }
        return image_rect;
    }
}

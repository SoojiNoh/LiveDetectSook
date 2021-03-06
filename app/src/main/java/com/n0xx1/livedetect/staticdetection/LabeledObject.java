package com.n0xx1.livedetect.staticdetection;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;

import androidx.annotation.Nullable;

import com.n0xx1.livedetect.R;
import com.n0xx1.livedetect.Utils;

import java.util.List;

public class LabeledObject {

    //    private final DetectedEntity entity;
    private final List<Label> labelList;
    private final int entityThumbnailCornerRadius;

    @Nullable
    private Bitmap image;
    private Bitmap croppedEntity;
    private Rect boundingBox;


    public LabeledObject(Resources resources, List<Label> labelList, StaticDetectRequest request) {
        this.labelList = labelList;
        this.entityThumbnailCornerRadius =
                resources.getDimensionPixelOffset(R.dimen.bounding_box_corner_radius);
        this.image = request.getBitmap();
        this.croppedEntity = request.getCroppedBitmap();
        this.boundingBox = request.getBoundingBox();
    }

//    public int getEntityIndex() {
//        return entity.getEntityIndex();
//    }

    public List<Label> getLabelList() {
        return labelList;
    }

    public Rect getBoundingBox() {
        return boundingBox;

    }
    //
    public synchronized Bitmap getLabelThumbnail(){
        if (image == null) {
            image =
                    Utils.getCornerRoundedBitmap(image, entityThumbnailCornerRadius);
        }
        return image;
    }

    public Bitmap getLabelCroppedBitmap(){
        if (image == null) {
            image =
                    Utils.getCornerRoundedBitmap(croppedEntity, entityThumbnailCornerRadius);
        }
        return croppedEntity;
    }
}
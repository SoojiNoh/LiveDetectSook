package com.n0xx1.livedetect.productsearch;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;

import androidx.annotation.Nullable;

import com.n0xx1.livedetect.R;
import com.n0xx1.livedetect.Utils;
import com.n0xx1.livedetect.objectdetection.DetectedObject;

import java.util.List;

public class SearchedObject {

    private final DetectedObject object;
    private final List<Product> productList;
    private final int objectThumbnailCornerRadius;

    @Nullable
    private Bitmap objectThumbnail;

    public SearchedObject(Resources resources, DetectedObject object, List<Product> productList) {
        this.object = object;
        this.productList = productList;
        this.objectThumbnailCornerRadius =
                resources.getDimensionPixelOffset(R.dimen.bounding_box_corner_radius);
    }

    public int getObjectIndex() {
        return object.getObjectIndex();
    }

    public List<Product> getProductList() {
        return productList;
    }

    public Rect getBoundingBox() {
        return object.getBoundingBox();
    }

    public synchronized Bitmap getObjectThumbnail() {
        if (objectThumbnail == null) {
            objectThumbnail =
                    Utils.getCornerRoundedBitmap(object.getBitmap(), objectThumbnailCornerRadius);
        }
        return objectThumbnail;
    }
}

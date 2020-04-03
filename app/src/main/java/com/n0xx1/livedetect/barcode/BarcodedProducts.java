package com.n0xx1.livedetect.barcode;

import android.content.res.Resources;
import android.graphics.Bitmap;

import androidx.annotation.Nullable;

import com.n0xx1.livedetect.R;

import java.util.List;

public class BarcodedProducts {

    private final BarcodedObject object;
    private final List<Product> productList;
//    private final int objectThumbnailCornerRadius;

    @Nullable
    private Bitmap objectThumbnail;

    public BarcodedProducts(BarcodedObject object, List<Product> productList) {
        this.object = object;
        this.productList = productList;
//        this.objectThumbnailCornerRadius =
//                resources.getDimensionPixelOffset(R.dimen.bounding_box_corner_radius);
    }

//    public int getObjectIndex() {
//        return object.getObjectIndex();
//    }

    public List<Product> getProductList() {
        return productList;
    }

//    public Rect getBoundingBox() {
//        return object.getBoundingBox();
//    }

//    public synchronized Bitmap getObjectThumbnail() {
//        if (objectThumbnail == null) {
//            objectThumbnail =
//                    Utils.getCornerRoundedBitmap(object.getBitmap(), objectThumbnailCornerRadius);
//        }
//        return objectThumbnail;
//    }
}

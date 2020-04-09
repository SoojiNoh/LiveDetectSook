package com.n0xx1.livedetect.barcode;

import android.graphics.Bitmap;

import androidx.annotation.Nullable;

import com.n0xx1.livedetect.Utils;

import java.util.List;

public class BarcodedEntity {

    Barcode barcode;
    List<BarcodedProduct> barcodedProducts;
    private final int entityThumbnailCornerRadius;

    @Nullable
    private Bitmap image;
    private Bitmap image_rect;

    BarcodedEntity(Barcode barcode, List<BarcodedProduct> barcodedProducts) {
//        this.name = name;
        this.barcode = barcode;
        this.barcodedProducts = barcodedProducts;
//        this.description = description;
        this.entityThumbnailCornerRadius = 12;
    }

    public void setBarcodedProducts(List<BarcodedProduct> barcodedProducts){
        this.barcodedProducts = barcodedProducts;
    }

    public List<BarcodedProduct> getBarcodedProducts(){
        return barcodedProducts;
    }

//

    public synchronized Bitmap getEntityThumbnail(){
        if (image == null) {
            image =
                    Utils.getCornerRoundedBitmap(image, entityThumbnailCornerRadius);
        }
        return image;
    }

    public Bitmap getTextEntityBitmap(){
        if (image == null) {
            image =
                    Utils.getCornerRoundedBitmap(image_rect, entityThumbnailCornerRadius);
        }
        return image_rect;
    }
}

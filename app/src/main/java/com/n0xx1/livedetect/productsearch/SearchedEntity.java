package com.n0xx1.livedetect.productsearch;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;

import androidx.annotation.Nullable;

import com.n0xx1.livedetect.R;
import com.n0xx1.livedetect.Utils;
import com.n0xx1.livedetect.entitydetection.DetectedEntity;

import java.util.List;

public class SearchedEntity {

    private final DetectedEntity entity;
    private final List<Entity> productList;
    private final int entityThumbnailCornerRadius;

    @Nullable
    private Bitmap entityThumbnail;

    public SearchedEntity(Resources resources, DetectedEntity entity, List<Entity> productList) {
        this.entity = entity;
        this.productList = productList;
        this.entityThumbnailCornerRadius =
                resources.getDimensionPixelOffset(R.dimen.bounding_box_corner_radius);
    }

    public int getEntityIndex() {
        return entity.getEntityIndex();
    }

    public List<Entity> getEntityList() {
        return productList;
    }

    public Rect getBoundingBox() {
        return entity.getBoundingBox();
    }

    public synchronized Bitmap getEntityThumbnail() {
        if (entityThumbnail == null) {
            entityThumbnail =
                    Utils.getCornerRoundedBitmap(entity.getBitmap(), entityThumbnailCornerRadius);
        }
        return entityThumbnail;
    }
}

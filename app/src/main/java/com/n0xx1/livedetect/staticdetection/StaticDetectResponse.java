package com.n0xx1.livedetect.staticdetection;

import android.content.res.Resources;
import android.graphics.Bitmap;

import com.google.api.services.vision.v1.model.BoundingPoly;
import com.n0xx1.livedetect.R;

public class StaticDetectResponse {



    private Bitmap croppedEntity;
    private BoundingPoly rectText;
    private LabeledObject labeledObject;
    private StaticDetectRequest request;
    private int objectThumbnailCornerRadius;

    TextedObject textedObject;

    public StaticDetectResponse(LabeledObject labeledObject, Bitmap croppedEntity, StaticDetectRequest request, Resources resources){
        this.croppedEntity = croppedEntity;
        this.labeledObject = labeledObject;
        this.request = request;
        this.objectThumbnailCornerRadius =
                resources.getDimensionPixelOffset(R.dimen.bounding_box_corner_radius);
    }

    public StaticDetectResponse(TextedObject textedObject, BoundingPoly rectText){
        this.textedObject = textedObject;
        this.rectText = rectText;

    }


    //GETTER
    public Bitmap getCroppedImage() {
        return croppedEntity;
    }

    public LabeledObject getLabeledObject() {
        return labeledObject;
    }

    public StaticDetectRequest getRequest() {
        return request;
    }
}

package com.n0xx1.livedetect.staticdetection;

import android.graphics.Bitmap;
import android.graphics.Rect;

import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.objects.FirebaseVisionObject;
import com.google.firebase.ml.vision.text.FirebaseVisionText;

public class StaticDetectRequest {

    private FirebaseVisionImage image;
    private Rect boundingBox;
    private Bitmap croppedEntity;
    private FirebaseVisionObject entity;
    private FirebaseVisionText text;
    private int requestIndex;


    public StaticDetectRequest(int requestIndex, FirebaseVisionImage image, FirebaseVisionObject entity){
        this.requestIndex = requestIndex;
        this.image = image;
        if (entity!=null){
            this.entity = entity;
            this.boundingBox = entity.getBoundingBox();
        }

    }

    public StaticDetectRequest(FirebaseVisionImage image, FirebaseVisionText text){
        this.image = image;
        this.text = text;
    }

    //GETTER
    public int getRequestIndex() {
        return requestIndex;
    }
    public Bitmap getBitmap(){
        return image.getBitmap();
    }
    public FirebaseVisionObject getEntityObject() {
        return  entity;
    }
    public FirebaseVisionText getTextObject(){
        return text;
    }
    public Bitmap getCroppedBitmap() {
        return croppedEntity;
    }
    public Rect getBoundingBox() {
        return boundingBox;
    }

    //SETTER
    public void setCroppedBitmap(Bitmap croppedEntity){
        this.croppedEntity = croppedEntity;
    }
}

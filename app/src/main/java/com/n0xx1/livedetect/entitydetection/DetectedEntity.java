package com.n0xx1.livedetect.entitydetection;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.objects.FirebaseVisionObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class DetectedEntity {

    private static final String TAG = "DetectedEntity";
    private static final int MAX_IMAGE_WIDTH = 640;

    private final FirebaseVisionObject entity;
    private final int entityIndex;
    private final FirebaseVisionImage image;

    @Nullable
    private Bitmap bitmap = null;
    @Nullable
    private byte[] jpegBytes = null;

    public DetectedEntity(FirebaseVisionObject entity, int entityIndex, FirebaseVisionImage image) {
        this.entity = entity;
        this.entityIndex = entityIndex;
        this.image = image;
    }

    @Nullable
    public Integer getEntityId() {
        return entity.getTrackingId();
    }

    public int getEntityIndex() {
        return entityIndex;
    }

    public Rect getBoundingBox() {
        return entity.getBoundingBox();
    }

    public synchronized Bitmap getBitmap() {
        if (bitmap == null) {
            Rect boundingBox = entity.getBoundingBox();
            bitmap =
                    Bitmap.createBitmap(
                            image.getBitmap(),
                            boundingBox.left,
                            boundingBox.top,
                            boundingBox.width(),
                            boundingBox.height());
            if (bitmap.getWidth() > MAX_IMAGE_WIDTH) {
                int dstHeight = (int) ((float) MAX_IMAGE_WIDTH / bitmap.getWidth() * bitmap.getHeight());
                bitmap = Bitmap.createScaledBitmap(bitmap, MAX_IMAGE_WIDTH, dstHeight, /* filter= */ false);
            }
        }

        return bitmap;
    }

    @Nullable
    public synchronized byte[] getImageData() {
        if (jpegBytes == null) {
            try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
                getBitmap().compress(Bitmap.CompressFormat.JPEG, /* quality= */ 100, stream);
                jpegBytes = stream.toByteArray();
            } catch (IOException e) {
                Log.e(TAG, "Error getting entity image data!");
            }
        }

        return jpegBytes;
    }
}

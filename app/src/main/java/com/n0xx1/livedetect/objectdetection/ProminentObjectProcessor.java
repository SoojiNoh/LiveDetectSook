package com.n0xx1.livedetect.objectdetection;

import android.graphics.RectF;
import android.util.Log;

import androidx.annotation.MainThread;

import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.objects.FirebaseVisionObject;
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetector;
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetectorOptions;
import com.n0xx1.livedetect.R;
import com.n0xx1.livedetect.camera.CameraReticleAnimator;
import com.n0xx1.livedetect.camera.FrameProcessorBase;
import com.n0xx1.livedetect.camera.GraphicOverlay;
import com.n0xx1.livedetect.camera.WorkflowModel;
import com.n0xx1.livedetect.camera.WorkflowModel.WorkflowState;
import com.n0xx1.livedetect.settings.PreferenceUtils;
import com.n0xx1.livedetect.staticdetection.StaticConfirmationController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** A processor to run object detector in prominent object only mode. */
public class ProminentObjectProcessor extends FrameProcessorBase<List<FirebaseVisionObject>> {

    private static final String TAG = "ProminentObjProcessor";

    private final FirebaseVisionObjectDetector detector;
    private final WorkflowModel workflowModel;
    private final ObjectConfirmationController confirmationController;
    private final CameraReticleAnimator cameraReticleAnimator;
    private final int reticleOuterRingRadius;


    private final StaticConfirmationController staticConfirmationController;

    public ProminentObjectProcessor(GraphicOverlay graphicOverlay, WorkflowModel workflowModel, StaticConfirmationController staticConfirmationController) {
        this.workflowModel = workflowModel;
        confirmationController = new ObjectConfirmationController(graphicOverlay);
        cameraReticleAnimator = new CameraReticleAnimator(graphicOverlay);
        reticleOuterRingRadius =
                graphicOverlay
                        .getResources()
                        .getDimensionPixelOffset(R.dimen.object_reticle_outer_ring_stroke_radius);

        FirebaseVisionObjectDetectorOptions.Builder optionsBuilder =
                new FirebaseVisionObjectDetectorOptions.Builder()
                        .setDetectorMode(FirebaseVisionObjectDetectorOptions.STREAM_MODE);
        if (PreferenceUtils.isClassificationEnabled(graphicOverlay.getContext())) {
            optionsBuilder.enableClassification();
        }
        this.detector = FirebaseVision.getInstance().getOnDeviceObjectDetector(optionsBuilder.build());

        this.staticConfirmationController = staticConfirmationController;
    }

    @Override
    public void stop() {
        try {
            detector.close();
        } catch (IOException e) {
            Log.e(TAG, "Failed to close object detector!", e);
        }
    }

    @Override
    protected Task<List<FirebaseVisionObject>> detectInImage(FirebaseVisionImage image) {
        staticConfirmationController.setImage(image.getBitmap());
        return detector.processImage(image);
    }

    @MainThread
    @Override
    protected void onSuccess(
            FirebaseVisionImage image,
            List<FirebaseVisionObject> objects,
            GraphicOverlay graphicOverlay) {
        if (!workflowModel.isCameraLive()) {
            return;
        }

        if (PreferenceUtils.isClassificationEnabled(graphicOverlay.getContext())) {
            List<FirebaseVisionObject> qualifiedObjects = new ArrayList<>();
            for (FirebaseVisionObject object : objects) {
                if (object.getClassificationCategory() != FirebaseVisionObject.CATEGORY_UNKNOWN) {
                    qualifiedObjects.add(object);
                }
            }
            objects = qualifiedObjects;
        }

        if (objects.isEmpty()) {
            confirmationController.reset();
            workflowModel.setWorkflowState(WorkflowState.DETECTING);
        } else {
            int objectIndex = 0;
            FirebaseVisionObject object = objects.get(objectIndex);
            if (objectBoxOverlapsConfirmationReticle(graphicOverlay, object)) {
                // User is confirming the object selection.
                confirmationController.confirming(object.getTrackingId());
                workflowModel.confirmingObject(
                        new DetectedObject(object, objectIndex, image), confirmationController.getProgress());
            } else {
                // Object detected but user doesn't want to pick this one.
                confirmationController.reset();
                workflowModel.setWorkflowState(WorkflowModel.WorkflowState.DETECTED);
            }
        }

        graphicOverlay.clear();
        if (objects.isEmpty()) {
            graphicOverlay.add(new ObjectReticleGraphic(graphicOverlay, cameraReticleAnimator));
            cameraReticleAnimator.start();
        } else {
            if (objectBoxOverlapsConfirmationReticle(graphicOverlay, objects.get(0))) {
                // User is confirming the object selection.
                cameraReticleAnimator.cancel();
                graphicOverlay.add(
                        new ObjectGraphicInProminentMode(
                                graphicOverlay, objects.get(0), confirmationController));
                if (!confirmationController.isConfirmed()
                        && PreferenceUtils.isAutoSearchEnabled(graphicOverlay.getContext())) {
                    // Shows a loading indicator to visualize the confirming progress if in auto search mode.
                    graphicOverlay.add(new ObjectConfirmationGraphic(graphicOverlay, confirmationController));
                }
            } else {
                // Object is detected but the confirmation reticle is moved off the object box, which
                // indicates user is not trying to pick this object.
                graphicOverlay.add(
                        new ObjectGraphicInProminentMode(
                                graphicOverlay, objects.get(0), confirmationController));
                graphicOverlay.add(new ObjectReticleGraphic(graphicOverlay, cameraReticleAnimator));
                cameraReticleAnimator.start();
            }
        }
        graphicOverlay.invalidate();
    }

    private boolean objectBoxOverlapsConfirmationReticle(
            GraphicOverlay graphicOverlay, FirebaseVisionObject object) {
        RectF boxRect = graphicOverlay.translateRect(object.getBoundingBox());
        float reticleCenterX = graphicOverlay.getWidth() / 2f;
        float reticleCenterY = graphicOverlay.getHeight() / 2f;
        RectF reticleRect =
                new RectF(
                        reticleCenterX - reticleOuterRingRadius,
                        reticleCenterY - reticleOuterRingRadius,
                        reticleCenterX + reticleOuterRingRadius,
                        reticleCenterY + reticleOuterRingRadius);
        return reticleRect.intersect(boxRect);
    }

    @Override
    protected void onFailure(Exception e) {
        Log.e(TAG, "Object detection failed!", e);
    }
}

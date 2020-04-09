package com.n0xx1.livedetect.entitydetection;

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

/** A processor to run entity detector in prominent entity only mode. */
public class ProminentEntityProcessor extends FrameProcessorBase<List<FirebaseVisionObject>> {

    private static final String TAG = "ProminentObjProcessor";

    private final FirebaseVisionObjectDetector detector;
    private final WorkflowModel workflowModel;
    private final EntityConfirmationController confirmationController;
    private final CameraReticleAnimator cameraReticleAnimator;
    private final int reticleOuterRingRadius;


    private final StaticConfirmationController staticConfirmationController;

    public ProminentEntityProcessor(GraphicOverlay graphicOverlay, WorkflowModel workflowModel, StaticConfirmationController staticConfirmationController) {
        this.workflowModel = workflowModel;
        confirmationController = new EntityConfirmationController(graphicOverlay);
        cameraReticleAnimator = new CameraReticleAnimator(graphicOverlay);
        reticleOuterRingRadius =
                graphicOverlay
                        .getResources()
                        .getDimensionPixelOffset(R.dimen.entity_reticle_outer_ring_stroke_radius);

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
            Log.e(TAG, "Failed to close entity detector!", e);
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
            List<FirebaseVisionObject> entitys,
            GraphicOverlay graphicOverlay) {
        if (!workflowModel.isCameraLive()) {
            return;
        }

        if (PreferenceUtils.isClassificationEnabled(graphicOverlay.getContext())) {
            List<FirebaseVisionObject> qualifiedEntitys = new ArrayList<>();
            for (FirebaseVisionObject entity : entitys) {
                if (entity.getClassificationCategory() != FirebaseVisionObject.CATEGORY_UNKNOWN) {
                    qualifiedEntitys.add(entity);
                }
            }
            entitys = qualifiedEntitys;
        }

        if (entitys.isEmpty()) {
            confirmationController.reset();
            workflowModel.setWorkflowState(WorkflowState.DETECTING);
        } else {
            int entityIndex = 0;
            FirebaseVisionObject entity = entitys.get(entityIndex);
            if (entityBoxOverlapsConfirmationReticle(graphicOverlay, entity)) {
                // User is confirming the entity selection.
                confirmationController.confirming(entity.getTrackingId());
                workflowModel.confirmingEntity(
                        new DetectedEntity(entity, entityIndex, image), confirmationController.getProgress());
            } else {
                // Entity detected but user doesn't want to pick this one.
                confirmationController.reset();
                workflowModel.setWorkflowState(WorkflowModel.WorkflowState.DETECTED);
            }
        }

        graphicOverlay.clear();
        if (entitys.isEmpty()) {
            graphicOverlay.add(new EntityReticleGraphic(graphicOverlay, cameraReticleAnimator));
            cameraReticleAnimator.start();
        } else {
            if (entityBoxOverlapsConfirmationReticle(graphicOverlay, entitys.get(0))) {
                // User is confirming the entity selection.
                cameraReticleAnimator.cancel();
                graphicOverlay.add(
                        new EntityGraphicInProminentMode(
                                graphicOverlay, entitys.get(0), confirmationController));
                if (!confirmationController.isConfirmed()
                        && PreferenceUtils.isAutoSearchEnabled(graphicOverlay.getContext())) {
                    // Shows a loading indicator to visualize the confirming progress if in auto search mode.
                    graphicOverlay.add(new EntityConfirmationGraphic(graphicOverlay, confirmationController));
                }
            } else {
                // Entity is detected but the confirmation reticle is moved off the entity box, which
                // indicates user is not trying to pick this entity.
                graphicOverlay.add(
                        new EntityGraphicInProminentMode(
                                graphicOverlay, entitys.get(0), confirmationController));
                graphicOverlay.add(new EntityReticleGraphic(graphicOverlay, cameraReticleAnimator));
                cameraReticleAnimator.start();
            }
        }
        graphicOverlay.invalidate();
    }

    private boolean entityBoxOverlapsConfirmationReticle(
            GraphicOverlay graphicOverlay, FirebaseVisionObject entity) {
        RectF boxRect = graphicOverlay.translateRect(entity.getBoundingBox());
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
        Log.e(TAG, "Entity detection failed!", e);
    }
}

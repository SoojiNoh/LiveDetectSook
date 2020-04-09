package com.n0xx1.livedetect.entitydetection;

import android.graphics.PointF;
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
import com.n0xx1.livedetect.settings.PreferenceUtils;
import com.n0xx1.livedetect.staticdetection.StaticConfirmationController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiEntityProcessor extends FrameProcessorBase<List<FirebaseVisionObject>> {

    private static final String TAG = "MultiEntityProcessor";

    private final WorkflowModel workflowModel;
    private final EntityConfirmationController confirmationController;
    private final CameraReticleAnimator cameraReticleAnimator;
    private final int entitySelectionDistanceThreshold;
    private final FirebaseVisionObjectDetector detector;
    // Each new tracked entity plays appearing animation exactly once.
    private final Map<Integer, EntityDotAnimator> entityDotAnimatorMap = new HashMap<>();

    private final StaticConfirmationController staticConfirmationController;

    public MultiEntityProcessor(GraphicOverlay graphicOverlay, WorkflowModel workflowModel, StaticConfirmationController staticConfirmationController) {
        this.workflowModel = workflowModel;
        this.confirmationController = new EntityConfirmationController(graphicOverlay);
        this.cameraReticleAnimator = new CameraReticleAnimator(graphicOverlay);
        this.entitySelectionDistanceThreshold =
                graphicOverlay
                        .getResources()
                        .getDimensionPixelOffset(R.dimen.entity_selection_distance_threshold);

        FirebaseVisionObjectDetectorOptions.Builder optionsBuilder =
                new FirebaseVisionObjectDetectorOptions.Builder()
                        .setDetectorMode(FirebaseVisionObjectDetectorOptions.STREAM_MODE)
                        .enableMultipleObjects();
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

        removeAnimatorsFromUntrackedEntitys(entitys);

        graphicOverlay.clear();

        DetectedEntity selectedEntity = null;
        for (int i = 0; i < entitys.size(); i++) {
            FirebaseVisionObject entity = entitys.get(i);
            if (selectedEntity == null && shouldSelectEntity(graphicOverlay, entity)) {
                selectedEntity = new DetectedEntity(entity, i, image);
                // Starts the entity confirmation once an entity is regarded as selected.
                confirmationController.confirming(entity.getTrackingId());
                graphicOverlay.add(new EntityConfirmationGraphic(graphicOverlay, confirmationController));

                graphicOverlay.add(
                        new EntityGraphicInMultiMode(
                                graphicOverlay, selectedEntity, confirmationController));
            } else {
                if (confirmationController.isConfirmed()) {
                    // Don't render other entitys when an entity is in confirmed state.
                    continue;
                }

                EntityDotAnimator entityDotAnimator = entityDotAnimatorMap.get(entity.getTrackingId());
                if (entityDotAnimator == null) {
                    entityDotAnimator = new EntityDotAnimator(graphicOverlay);
                    entityDotAnimator.start();
                    entityDotAnimatorMap.put(entity.getTrackingId(), entityDotAnimator);
                }
                graphicOverlay.add(
                        new EntityDotGraphic(
                                graphicOverlay, new DetectedEntity(entity, i, image), entityDotAnimator));
            }
        }

        if (selectedEntity == null) {
            confirmationController.reset();
            graphicOverlay.add(new EntityReticleGraphic(graphicOverlay, cameraReticleAnimator));
            cameraReticleAnimator.start();
        } else {
            cameraReticleAnimator.cancel();
        }

        graphicOverlay.invalidate();

        if (selectedEntity != null) {
            workflowModel.confirmingEntity(selectedEntity, confirmationController.getProgress());
        } else {
            workflowModel.setWorkflowState(
                    entitys.isEmpty()
                            ? WorkflowModel.WorkflowState.DETECTING
                            : WorkflowModel.WorkflowState.DETECTED);
        }
    }

    private void removeAnimatorsFromUntrackedEntitys(List<FirebaseVisionObject> detectedEntitys) {
        List<Integer> trackingIds = new ArrayList<>();
        for (FirebaseVisionObject entity : detectedEntitys) {
            trackingIds.add(entity.getTrackingId());
        }
        // Stop and remove animators from the entitys that have lost tracking.
        List<Integer> removedTrackingIds = new ArrayList<>();
        for (Map.Entry<Integer, EntityDotAnimator> entry : entityDotAnimatorMap.entrySet()) {
            if (!trackingIds.contains(entry.getKey())) {
                entry.getValue().cancel();
                removedTrackingIds.add(entry.getKey());
            }
        }
        entityDotAnimatorMap.keySet().removeAll(removedTrackingIds);
    }

    private boolean shouldSelectEntity(GraphicOverlay graphicOverlay, FirebaseVisionObject entity) {
        // Considers an entity as selected when the camera reticle touches the entity dot.
        RectF box = graphicOverlay.translateRect(entity.getBoundingBox());
        PointF entityCenter = new PointF((box.left + box.right) / 2f, (box.top + box.bottom) / 2f);
        PointF reticleCenter =
                new PointF(graphicOverlay.getWidth() / 2f, graphicOverlay.getHeight() / 2f);
        double distance =
                Math.hypot(entityCenter.x - reticleCenter.x, entityCenter.y - reticleCenter.y);
        return distance < entitySelectionDistanceThreshold;
    }

    @Override
    protected void onFailure(Exception e) {
        Log.e(TAG, "Entity detection failed!", e);
    }
}

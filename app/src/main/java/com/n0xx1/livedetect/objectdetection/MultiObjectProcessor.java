package com.n0xx1.livedetect.objectdetection;

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

public class MultiObjectProcessor extends FrameProcessorBase<List<FirebaseVisionObject>> {

    private static final String TAG = "MultiObjectProcessor";

    private final WorkflowModel workflowModel;
    private final ObjectConfirmationController confirmationController;
    private final CameraReticleAnimator cameraReticleAnimator;
    private final int objectSelectionDistanceThreshold;
    private final FirebaseVisionObjectDetector detector;
    // Each new tracked object plays appearing animation exactly once.
    private final Map<Integer, ObjectDotAnimator> objectDotAnimatorMap = new HashMap<>();

    private final StaticConfirmationController staticConfirmationController;

    public MultiObjectProcessor(GraphicOverlay graphicOverlay, WorkflowModel workflowModel, StaticConfirmationController staticConfirmationController) {
        this.workflowModel = workflowModel;
        this.confirmationController = new ObjectConfirmationController(graphicOverlay);
        this.cameraReticleAnimator = new CameraReticleAnimator(graphicOverlay);
        this.objectSelectionDistanceThreshold =
                graphicOverlay
                        .getResources()
                        .getDimensionPixelOffset(R.dimen.object_selection_distance_threshold);

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

        removeAnimatorsFromUntrackedObjects(objects);

        graphicOverlay.clear();

        DetectedObject selectedObject = null;
        for (int i = 0; i < objects.size(); i++) {
            FirebaseVisionObject object = objects.get(i);
            if (selectedObject == null && shouldSelectObject(graphicOverlay, object)) {
                selectedObject = new DetectedObject(object, i, image);
                // Starts the object confirmation once an object is regarded as selected.
                confirmationController.confirming(object.getTrackingId());
                graphicOverlay.add(new ObjectConfirmationGraphic(graphicOverlay, confirmationController));

                graphicOverlay.add(
                        new ObjectGraphicInMultiMode(
                                graphicOverlay, selectedObject, confirmationController));
            } else {
                if (confirmationController.isConfirmed()) {
                    // Don't render other objects when an object is in confirmed state.
                    continue;
                }

                ObjectDotAnimator objectDotAnimator = objectDotAnimatorMap.get(object.getTrackingId());
                if (objectDotAnimator == null) {
                    objectDotAnimator = new ObjectDotAnimator(graphicOverlay);
                    objectDotAnimator.start();
                    objectDotAnimatorMap.put(object.getTrackingId(), objectDotAnimator);
                }
                graphicOverlay.add(
                        new ObjectDotGraphic(
                                graphicOverlay, new DetectedObject(object, i, image), objectDotAnimator));
            }
        }

        if (selectedObject == null) {
            confirmationController.reset();
            graphicOverlay.add(new ObjectReticleGraphic(graphicOverlay, cameraReticleAnimator));
            cameraReticleAnimator.start();
        } else {
            cameraReticleAnimator.cancel();
        }

        graphicOverlay.invalidate();

        if (selectedObject != null) {
            workflowModel.confirmingObject(selectedObject, confirmationController.getProgress());
        } else {
            workflowModel.setWorkflowState(
                    objects.isEmpty()
                            ? WorkflowModel.WorkflowState.DETECTING
                            : WorkflowModel.WorkflowState.DETECTED);
        }
    }

    private void removeAnimatorsFromUntrackedObjects(List<FirebaseVisionObject> detectedObjects) {
        List<Integer> trackingIds = new ArrayList<>();
        for (FirebaseVisionObject object : detectedObjects) {
            trackingIds.add(object.getTrackingId());
        }
        // Stop and remove animators from the objects that have lost tracking.
        List<Integer> removedTrackingIds = new ArrayList<>();
        for (Map.Entry<Integer, ObjectDotAnimator> entry : objectDotAnimatorMap.entrySet()) {
            if (!trackingIds.contains(entry.getKey())) {
                entry.getValue().cancel();
                removedTrackingIds.add(entry.getKey());
            }
        }
        objectDotAnimatorMap.keySet().removeAll(removedTrackingIds);
    }

    private boolean shouldSelectObject(GraphicOverlay graphicOverlay, FirebaseVisionObject object) {
        // Considers an object as selected when the camera reticle touches the object dot.
        RectF box = graphicOverlay.translateRect(object.getBoundingBox());
        PointF objectCenter = new PointF((box.left + box.right) / 2f, (box.top + box.bottom) / 2f);
        PointF reticleCenter =
                new PointF(graphicOverlay.getWidth() / 2f, graphicOverlay.getHeight() / 2f);
        double distance =
                Math.hypot(objectCenter.x - reticleCenter.x, objectCenter.y - reticleCenter.y);
        return distance < objectSelectionDistanceThreshold;
    }

    @Override
    protected void onFailure(Exception e) {
        Log.e(TAG, "Object detection failed!", e);
    }
}

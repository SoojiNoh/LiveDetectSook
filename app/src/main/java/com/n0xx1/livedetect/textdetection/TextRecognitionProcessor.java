package com.n0xx1.livedetect.textdetection;

import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.n0xx1.livedetect.camera.FrameProcessorBase;
import com.n0xx1.livedetect.camera.GraphicOverlay;
import com.n0xx1.livedetect.camera.WorkflowModel;

import java.io.IOException;
import java.util.List;

public class TextRecognitionProcessor extends FrameProcessorBase<FirebaseVisionText> {

    private static final String TAG = "TextRecogniProcessor";

    private final FirebaseVisionTextRecognizer detector;
    private final WorkflowModel workflowModel;
//    private final ObjectConfirmationController confirmationController;
//    private final CameraReticleAnimator cameraReticleAnimator;
//    private final int reticleOuterRingRadius;


    public TextRecognitionProcessor(GraphicOverlay graphicOverlay, WorkflowModel workflowModel) {
        this.workflowModel = workflowModel;
//        confirmationController = new ObjectConfirmationController(graphicOverlay);
//        cameraReticleAnimator = new CameraReticleAnimator(graphicOverlay);
//        reticleOuterRingRadius =
//                graphicOverlay
//                        .getResources()
//                        .getDimensionPixelOffset(R.dimen.object_reticle_outer_ring_stroke_radius);

//        FirebaseVisionObjectDetectorOptions.Builder optionsBuilder =
//                new FirebaseVisionObjectDetectorOptions.Builder()
//                        .setDetectorMode(FirebaseVisionObjectDetectorOptions.STREAM_MODE);
//        if (PreferenceUtils.isClassificationEnabled(graphicOverlay.getContext())) {
//            optionsBuilder.enableClassification();
//        }

        this.detector = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
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
    protected Task<FirebaseVisionText> detectInImage(FirebaseVisionImage image) {
        return detector.processImage(image);
    }



    @MainThread
    @Override
    protected void onSuccess(
            FirebaseVisionImage image,
            FirebaseVisionText results,
            GraphicOverlay graphicOverlay) {
        graphicOverlay.clear();

        List<FirebaseVisionText.TextBlock> blocks = results.getTextBlocks();

        for (int i = 0; i < blocks.size(); i++) {
            List<FirebaseVisionText.Line> lines = blocks.get(i).getLines();
            for (int j = 0; j < lines.size(); j++) {
                List<FirebaseVisionText.Element> elements = lines.get(j).getElements();
                for (int k = 0; k < elements.size(); k++) {
                    GraphicOverlay.Graphic textGraphic = new TextGraphic(graphicOverlay, elements.get(k));
                    graphicOverlay.add(textGraphic);

                }
            }
        }
    }

    @Override
    protected void onFailure(@NonNull Exception e) {
        Log.w(TAG, "Text detection failed." + e);
    }

}

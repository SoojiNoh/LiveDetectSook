package com.n0xx1.livedetect.staticdetection;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

import androidx.annotation.MainThread;

import com.google.common.collect.ImmutableList;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.objects.FirebaseVisionObject;
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetector;
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetectorOptions;
import com.n0xx1.livedetect.MainActivity;
import com.n0xx1.livedetect.camera.GraphicOverlay;
import com.n0xx1.livedetect.camera.WorkflowModel;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class StaticDetectProcessor {

    private static String TAG = "StaticDetectProcessor";

    private static int detectedObjectNum = 0;
    private FirebaseVisionObjectDetector detector;
    private StaticDetectEngine engine;
    private WorkflowModel workflowModel;
    private GraphicOverlay graphicOverlay;
    private MainActivity mainActivity;

    private int CURRENT_MODE;


    public List<StaticDetectRequest> staticDetectRequests = new ArrayList<StaticDetectRequest>();
    private TreeMap<Integer, StaticDetectResponse> staticDetectedMap = new TreeMap<>();

    public StaticDetectProcessor(GraphicOverlay graphicOverlay, WorkflowModel workflowModel) {
        this.graphicOverlay = graphicOverlay;
        this.workflowModel = workflowModel;
        this.mainActivity = workflowModel.mainActivity;
        detector =
                FirebaseVision.getInstance()
                        .getOnDeviceObjectDetector(
                                new FirebaseVisionObjectDetectorOptions.Builder()
                                        .setDetectorMode(FirebaseVisionObjectDetectorOptions.SINGLE_IMAGE_MODE)
                                        .enableMultipleObjects()
                                        .build());
    }

    public void detectEntityRegion(Bitmap bitmap) {

        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
        detector
                .processImage(image)
                .addOnSuccessListener(objects -> onEntityToDetect(image, objects))
                .addOnFailureListener(e -> onEntityToDetect(image, ImmutableList.of()));
    }

    @MainThread
    private void onEntityToDetect(FirebaseVisionImage image, List<FirebaseVisionObject> objects) {
        detectedObjectNum = objects.size();
        Log.d(TAG, "Detected objects num: " + detectedObjectNum);
        if (detectedObjectNum == 0) {
            StaticDetectRequest request = new StaticDetectRequest(0, image, null);
            workflowModel.staticDetectRequest.setValue(request);
//            mainActivity.showBottomPromptChip(mainActivity.getString(R.string.static_image_prompt_detected_no_results));
//            mainActivity.tts.speech(mainActivity.getString(R.string.static_image_prompt_detected_no_results));
        } else {
            staticDetectRequests.clear();

            for (int i = 0; i < objects.size(); i++) {
                Log.i(TAG, "***** objects.get(i): " +  objects.get(i));
                StaticDetectRequest request = new StaticDetectRequest(i, image, objects.get(i));
                workflowModel.staticDetectRequest.setValue(request);
//                searchEngine.search(new DetectedObject(objects.get(i), i, image), /* listener= */ this);
            }
        }
    }


    public void requestStaticDetect(StaticDetectRequest request){
        staticDetectRequests.add(request);
        engine = new StaticDetectEngine(workflowModel.mainActivity, workflowModel, graphicOverlay);
        this.CURRENT_MODE = mainActivity.CURRENT_MODE;
        if (CURRENT_MODE == mainActivity.TEXT_MODE)
            engine.detectText(request.getBitmap(), workflowModel);
        else if (CURRENT_MODE == mainActivity.PROMI_MODE || CURRENT_MODE == mainActivity.MULTI_MODE) {

            if (request.getEntityObject()!=null) {
                Rect coord = request.getEntityObject().getBoundingBox();
                Bitmap croppedEntity = Bitmap.createBitmap(request.getBitmap(), coord.left, coord.top, coord.width(), coord.height());
                request.setCroppedBitmap(croppedEntity);
            }
            engine.detectLabel(request, workflowModel);
        }
    }


    public void onStaticDetectCompleted(StaticDetectRequest request, StaticDetectResponse response) {
        Log.d(TAG, "Search completed for object index: " + request.getRequestIndex());
        staticDetectedMap.put(request.getRequestIndex(), response);
        if (staticDetectedMap.size() < detectedObjectNum) {
            // Hold off showing the result until the search of all detected objects completes.
            Log.i(TAG, "******"+staticDetectedMap+"/"+detectedObjectNum+"of detect results recieved. ");
            return;
        }
        else {
            workflowModel.staticDetectedMap.setValue(staticDetectedMap);
        }

    }



}

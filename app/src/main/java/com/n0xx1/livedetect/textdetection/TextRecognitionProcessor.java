package com.n0xx1.livedetect.textdetection;

import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetectorOptions;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionText.Element;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.n0xx1.livedetect.R;
import com.n0xx1.livedetect.camera.CameraReticleAnimator;
import com.n0xx1.livedetect.camera.FrameProcessorBase;
import com.n0xx1.livedetect.camera.GraphicOverlay;
import com.n0xx1.livedetect.camera.GraphicOverlay.Graphic;
import com.n0xx1.livedetect.camera.WorkflowModel;
import com.n0xx1.livedetect.settings.PreferenceUtils;
import com.n0xx1.livedetect.staticdetection.StaticConfirmationController;
import com.n0xx1.livedetect.text2speech.Text2Speech;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TextRecognitionProcessor extends FrameProcessorBase<FirebaseVisionText> {

    private static final String TAG = "TextRecognizeProcessor";

    ScheduledExecutorService textService = Executors.newSingleThreadScheduledExecutor();
    ScheduledExecutorService ttsService = Executors.newSingleThreadScheduledExecutor();


    Text2Speech tts;
    private final FirebaseVisionTextRecognizer detector;
    private final WorkflowModel workflowModel;
    private final GraphicOverlay graphicOverlay;
    private FirebaseVisionImage image;
    private StaticConfirmationController staticConfirmationController;


    private final CameraReticleAnimator cameraReticleAnimator;
    private final int reticleOuterRingRadius;

    List<FirebaseVisionText.TextBlock> blocks;

    private boolean hasFoundText = false;
    private boolean hasFoundBestBefore = false;
    private boolean hasFoundMainText = false;
    private String[] bestBefore = new String[3];
    private String mainText;
    HashMap<Double, Element> textScaleMap =  new HashMap<Double, Element>();

    private LinkedList<Element> mainTextBuffer = new LinkedList<Element>();

    int TEXT_DELAY = 0;
    int TEXT_INTERVAL = 120;
    int MAIN_DELAY = 5;
    int MAIN_INTERVAL = 3;
    int BESTBEFORE_DELAY = 5;
    int BESTBEFORE_INTERVAL = 10;


    String[] BEST_BEFORE_REGEXES_US = {
            "^[0-3]?[0-9][^0-9][0-3]?[0-9][^0-9](?:[0-9]{2})?[0-9]{2}$",
            "^[0-3][0-9][^0-9][0-3][0-9][^0-9](?:[0-9][0-9])?[0-9][0-9]$",
            "^(1[0-2]|0[1-9])[^0-9](3[01]|[12][0-9]|0[1-9])[^0-9][0-9]{4}$",
            "^(3[01]|[12][0-9]|0[1-9])[^0-9](1[0-2]|0[1-9])[^0-9][0-9]{4}$",
    };
    String[] BEST_BEFORE_REGEXES_KO = {
            "^(?:[0-9]{2})?[0-9]{2}[^0-9][0-3]?[0-9][^0-9][0-3]?[0-9]$",
            "^(?:[0-9][0-9])?[0-9][0-9][^0-9][0-3][0-9][^0-9][0-3][0-9]$",
            "^[0-9]{4}[^0-9](3[01]|[12][0-9]|0[1-9])[^0-9](1[0-2]|0[1-9])$",
            "^[0-9]{4}[^0-9](1[0-2]|0[1-9])[^0-9](3[01]|[12][0-9]|0[1-9])$",
    };


    public TextRecognitionProcessor(GraphicOverlay graphicOverlay, WorkflowModel workflowModel, StaticConfirmationController staticConfirmationController) {

        this.workflowModel = workflowModel;
        this.graphicOverlay = graphicOverlay;
        this.staticConfirmationController = staticConfirmationController;
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

        this.detector = FirebaseVision.getInstance().getOnDeviceTextRecognizer();

        textService.scheduleAtFixedRate(resetTextFoundTimer, TEXT_DELAY, TEXT_INTERVAL, TimeUnit.SECONDS);
        textService.scheduleAtFixedRate(resetBestBeforeFoundTimer, BESTBEFORE_DELAY, BESTBEFORE_INTERVAL, TimeUnit.SECONDS);
        textService.scheduleAtFixedRate(resetMainTextFoundTimer, MAIN_DELAY, MAIN_INTERVAL, TimeUnit.SECONDS);
        textService.scheduleAtFixedRate(sortTextScale, 0, 2, TimeUnit.SECONDS);
        textService.scheduleAtFixedRate(processBuffer, 0, 1, TimeUnit.SECONDS);

        tts = new Text2Speech(workflowModel.getApplication().getApplicationContext(), workflowModel.mainActivity);
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
    protected Task<FirebaseVisionText> detectInImage(FirebaseVisionImage image) {
        staticConfirmationController.setImage(image.getBitmap());
        return detector.processImage(image);
    }


    @MainThread
    @Override
    protected void onSuccess(
            FirebaseVisionImage image,
            FirebaseVisionText results,
            GraphicOverlay graphicOverlay) {
        graphicOverlay.clear();


        graphicOverlay.add(new TextDotGraphic(graphicOverlay));


//        List<FirebaseVisionText.TextBlock> blocks = results.getTextBlocks();
        blocks = results.getTextBlocks();


        for (int i = 0; i < blocks.size(); i++) {
            List<FirebaseVisionText.Line> lines = blocks.get(i).getLines();
            for (int j = 0; j < lines.size(); j++) {
                List<Element> elements = lines.get(j).getElements();
                for (int k = 0; k < elements.size(); k++) {
                    Element element = elements.get(k);
                    double textScale = element.getBoundingBox().width() * element.getBoundingBox().height();


                    if (textScale > 1000 && workflowModel.isTtsAvailable() && !hasFoundText) {
                        alertTextFound();
                    }


                    for (String regex : BEST_BEFORE_REGEXES_KO ) {
                        if (element.getText().matches(regex) && workflowModel.isTtsAvailable() && !hasFoundBestBefore && textScale > 1000) {
                            bestBefore = element.getText().split("[^0-9]");
                            alertBestBeforeFoundKo(bestBefore[0], bestBefore[1], bestBefore[2]);
                            continue;
                        }
                    }

                    if (textScale > 10000 && !hasFoundMainText) {
                        Log.i(TAG, "***textScale: " + textScale);
                        textScaleMap.put(textScale, element);
                        hasFoundMainText=true;
                    }
//                        Log.i(TAG, "******textAreaSize: "+element.getBoundingBox().width()*element.getBoundingBox().height());
                    Graphic textGraphic = new TextGraphic(graphicOverlay, element);
                    graphicOverlay.add(textGraphic);

                }

            }
        }
    }

    @Override
    protected void onFailure(@NonNull Exception e) {
        Log.w(TAG, "Text detection failed." + e);
    }


    Runnable resetTextFoundTimer = new Runnable() {
        @Override
        public void run() {
            if (hasFoundText) {
                Log.i(TAG, "******textFoundReset");
                hasFoundText = false;
            }
        }
    };

    Runnable resetMainTextFoundTimer = new Runnable() {
        @Override
        public void run() {
            if (hasFoundMainText) {
                Log.i(TAG, "******mainTextFoundReset");
                hasFoundMainText = false;
            }
        }
    };

    Runnable resetBestBeforeFoundTimer = new Runnable() {
        @Override
        public void run() {
            if (hasFoundBestBefore) {
                Log.i(TAG, "******bestBeforeFoundReset");
                hasFoundBestBefore = false;
            }
        }
    };

    Runnable sortTextScale = new Runnable() {
        @Override
        public void run() {
            if (!textScaleMap.isEmpty()) {
                TreeMap<Double, Element> textSortMap = new TreeMap<Double, Element>(textScaleMap);
                Iterator<Double> iteratorKey = textSortMap.keySet().iterator();

                ArrayList<Element> textSorted = new ArrayList<Element>();

                while (iteratorKey.hasNext()) {
                    Double key = iteratorKey.next();
                    textSorted.add(textSortMap.get(key));
                }
                Log.i(TAG, "******textSortedSize: " + textSorted.size());
                for (int l = 0; l < textSorted.size(); l++)
                    workflowModel.queueBuffer(textSorted.get(l).getText());
            }
            textScaleMap =  new HashMap<Double, Element>();
        }
    };



    Runnable processBuffer = new Runnable() {
        @Override
        public void run() {

            Log.i(TAG, "*****processingBufferStart: "+workflowModel.getTtsAvailable());
            if (workflowModel.getTtsAvailable() && !workflowModel.ttsBuffer.isEmpty()) {
                String message = workflowModel.ttsBuffer.removeFirst();
                Log.i(TAG, "*****processingBuffer: "+message);
                tts.speech(message.toLowerCase());
                workflowModel.printBuffer();
            }
        }
    };



    protected void alertTextFound() {
        if (!hasFoundText) {
            hasFoundText = true;
            tts.speech("텍스트 발견. 조사하시려면 화면을 길게 눌러주세요");
        }
    }

    protected void alertBestBeforeFoundKo(String year, String month, String day) {
        if (!hasFoundBestBefore) {
            hasFoundBestBefore = true;
            tts.speech("유통기한 발견." + year + "년" + month + "월" + day + "일" + "입니다.");
        }
    }

    protected void alertBestBeforeFoundUS(String day, String month, String year) {
        if (!hasFoundBestBefore) {
            hasFoundBestBefore = true;
            tts.speech("유통기한 발견." + year + "년" + month + "월" + day + "일" + "입니다.");
        }
    }





}


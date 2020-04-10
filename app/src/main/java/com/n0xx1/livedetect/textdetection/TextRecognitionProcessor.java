package com.n0xx1.livedetect.textdetection;

import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetectorOptions;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
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
import java.util.List;

public class TextRecognitionProcessor extends FrameProcessorBase<FirebaseVisionText>{

    private static final String TAG = "TextRecognizeProcessor";

    private final FirebaseVisionTextRecognizer detector;
    private final WorkflowModel workflowModel;
    private final GraphicOverlay graphicOverlay;
    private FirebaseVisionImage image;
    private StaticConfirmationController staticConfirmationController;

//    private final StaticConfirmationController confirmationController;
    private final CameraReticleAnimator cameraReticleAnimator;
    private final int reticleOuterRingRadius;

    List<FirebaseVisionText.TextBlock> blocks;
    String text;

    boolean hasFoundText = false;
    boolean hasFoundBestBefore = false;
    String[] bestBefore = new String[3];
    boolean hasFoundMainText = false;
    String mainText;
    String[] BEST_BEFORE_REGEXES_PRE = {
            "^[0-3]?[0-9][^0-9][0-3]?[0-9][^0-9](?:[0-9]{2})?[0-9]{2}$",
            "^[0-3][0-9][^0-9][0-3][0-9][^0-9](?:[0-9][0-9])?[0-9][0-9]$",
            "^(1[0-2]|0[1-9])[^0-9](3[01]|[12][0-9]|0[1-9])[^0-9][0-9]{4}$",
            "^(3[01]|[12][0-9]|0[1-9])[^0-9](1[0-2]|0[1-9])[^0-9][0-9]{4}$",
    };
    String[] BEST_BEFORE_REGEXES_POST = {
            "^(?:[0-9]{2})?[0-9]{2}[^0-9][0-3]?[0-9][^0-9][0-3]?[0-9]$",
            "^(?:[0-9][0-9])?[0-9][0-9][^0-9][0-3][0-9][^0-9][0-3][0-9]$",
            "^[0-9]{4}[^0-9](3[01]|[12][0-9]|0[1-9])[^0-9](1[0-2]|0[1-9])$",
            "^[0-9]{4}[^0-9](1[0-2]|0[1-9])[^0-9](3[01]|[12][0-9]|0[1-9])$",
    };

    Text2Speech tts;

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
//        this.image = image;
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

        if (!blocks.isEmpty()) alertTextFound();

        for (int i = 0; i < blocks.size(); i++) {
            List<FirebaseVisionText.Line> lines = blocks.get(i).getLines();
            for (int j = 0; j < lines.size(); j++) {
                List<FirebaseVisionText.Element> elements = lines.get(j).getElements();
                for (int k = 0; k < elements.size(); k++) {
                    FirebaseVisionText.Element element = elements.get(k);
                    for(String regex : BEST_BEFORE_REGEXES_PRE){
                        if (element.getText().matches(regex)) {
                            Log.i(TAG, "bestBeforeFound: ");
                            bestBefore = element.getText().split("[^0-9]");
                            alertBestBeforeFound(bestBefore[2],bestBefore[1],bestBefore[0]);
                        }
                    }

                    for(String regex : BEST_BEFORE_REGEXES_POST){
                        if (element.getText().matches(regex)) {
                            Log.i(TAG, "bestBeforeFound: ");
                            bestBefore = element.getText().split("[^0-9]");
                            alertBestBeforeFound(bestBefore[0],bestBefore[1],bestBefore[2]);
                        }
                    }
                    if (element.getBoundingBox().width()*element.getBoundingBox().height()>6500)
                        alertMainTextFound();
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


    public void alertTextFound(){
        if (!hasFoundText) {
            hasFoundText = true;
            tts.speech("텍스트 발견. 조사하시려면 화면을 길게 눌러주세요");
        }
    }

    public void alertBestBeforeFound(String year, String month, String day){
        if (!hasFoundBestBefore) {
            hasFoundBestBefore = true;
            tts.speech("유통기한 발견. 해당 상품의 유통기한은"+year+"년"+month+"월"+day+"일"+"입니다.");
        }
    }

    public void alertMainTextFound(){
        if (!hasFoundMainText) {
            hasFoundMainText = true;
            tts.speech(mainText);
        }
    }

    public void resetFounds(){

    }

}

package com.n0xx1.livedetect.staticdetection;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.CountDownTimer;
import android.view.View;

import androidx.annotation.Nullable;

import com.n0xx1.livedetect.R;
import com.n0xx1.livedetect.camera.CameraReticleAnimator;
import com.n0xx1.livedetect.camera.GraphicOverlay;
import com.n0xx1.livedetect.camera.WorkflowModel;
import com.n0xx1.livedetect.camera.WorkflowModel.WorkflowState;
import com.n0xx1.livedetect.settings.PreferenceUtils;

public class StaticConfirmationController extends TouchTimer implements View.OnTouchListener{

    private static final String TAG = "StaticConfirmation";

    @Nullable
    private boolean confirming = false;
    private boolean searching = false;
    private float progress = 0;


    private final CameraReticleAnimator cameraReticleAnimator;
    private final int reticleOuterRingRadius;


    private final Context context;
    private final WorkflowModel workflowModel;
    private final GraphicOverlay graphicOverlay;
    private CountDownTimer countDownTimer;
    private long confirmationTimeMs;

    private int mode;


    private Bitmap image;
    /**
     * @param graphicOverlay Used to refresh camera overlay when the confirmation progress updates.
     */
    public StaticConfirmationController(GraphicOverlay graphicOverlay, WorkflowModel workflowModel, Context context) {

        this.workflowModel = workflowModel;
        this.graphicOverlay = graphicOverlay;
        this.context = context;

        cameraReticleAnimator = new CameraReticleAnimator(graphicOverlay);
        reticleOuterRingRadius =
                graphicOverlay
                        .getResources()
                        .getDimensionPixelOffset(R.dimen.entity_reticle_outer_ring_stroke_radius);



        confirmationTimeMs = PreferenceUtils.getStaticConfirmationTimeMs(graphicOverlay.getContext());
    }

    public void activate(int mode){
        this.mode = mode;
        this.graphicOverlay.setOnTouchListener(this);
    }

    public void disactivate(){
        this.graphicOverlay.setOnTouchListener(null);
    }


    public void setImage(Bitmap image){
        this.image = image;
    }

    public Bitmap getImage() {return image;}

    void confirming() {
        if (confirming) {
            // Do nothing if it's already in confirming.
            return;
        }

        reset();
        confirming = true;
        searching = false;
        countDownTimer.start();
    }

    boolean isConfirmed() {
        return Float.compare(progress, 1) == 0;
    }

    void reset() {
        countDownTimer.cancel();
        confirming = false;
        progress = 0;
    }

    /** Returns the confirmation progress described as a float value in the range of [0, 1]. */
    float getProgress() {
        return progress;
    }


    @Override
    protected void onTouching(long touchTimeInMillis) {

        countDownTimer =
                new CountDownTimer(confirmationTimeMs, /* countDownInterval= */ 20) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        progress = (float) (confirmationTimeMs - millisUntilFinished) / confirmationTimeMs;
                        graphicOverlay.invalidate();
                    }

                    @Override
                    public void onFinish() {
                        progress = 1;
                    }
                };

        if (touchTimeInMillis < PreferenceUtils.getConfirmationTimeMs(graphicOverlay.getContext())) {
            reset();
            workflowModel.setWorkflowState(WorkflowState.DETECTING);
        } else {
            // User is confirming the static selection.
            confirming();
            boolean isConfirmed = (Float.compare(getProgress(), 1f) == 0);
            if (isConfirmed) {
                workflowModel.setWorkflowState(WorkflowState.CONFIRMED);
                workflowModel.detectedImage.setValue(image);
                    if (!searching) {
                        searching = true;
                        workflowModel.setWorkflowState(WorkflowState.SEARCHING);
                        workflowModel.staticToDetect.setValue(image);

                    }


            }
            else
                workflowModel.setWorkflowState(WorkflowState.CONFIRMING);
//                workflowModel.setWorkflowState(WorkflowState.CONFIRMED);

        }

        graphicOverlay.clear();
        if (!isConfirmed()) {
            // Shows a loading indicator to visualize the confirming progress if in auto search mode.
            graphicOverlay.add(new StaticConfirmationGraphic(graphicOverlay, this));
        } else {


        }
        graphicOverlay.invalidate();

    }


}

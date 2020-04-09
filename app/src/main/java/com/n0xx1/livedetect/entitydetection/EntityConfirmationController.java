package com.n0xx1.livedetect.entitydetection;

import android.os.CountDownTimer;

import androidx.annotation.Nullable;

import com.n0xx1.livedetect.camera.GraphicOverlay;
import com.n0xx1.livedetect.settings.PreferenceUtils;

public class EntityConfirmationController {

    private final CountDownTimer countDownTimer;

    @Nullable
    private Integer entityId = null;
    private float progress = 0;

    /**
     * @param graphicOverlay Used to refresh camera overlay when the confirmation progress updates.
     */
    EntityConfirmationController(GraphicOverlay graphicOverlay) {
        long confirmationTimeMs = PreferenceUtils.getConfirmationTimeMs(graphicOverlay.getContext());
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
    }

    void confirming(Integer entityId) {
        if (entityId.equals(this.entityId)) {
            // Do nothing if it's already in confirming.
            return;
        }

        reset();
        this.entityId = entityId;
        countDownTimer.start();
    }

    boolean isConfirmed() {
        return Float.compare(progress, 1) == 0;
    }

    void reset() {
        countDownTimer.cancel();
        entityId = null;
        progress = 0;
    }

    /** Returns the confirmation progress described as a float value in the range of [0, 1]. */
    float getProgress() {
        return progress;
    }
}

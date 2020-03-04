package com.n0xx1.livedetect.barcode;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;

import androidx.core.content.ContextCompat;

import com.n0xx1.livedetect.R;
import com.n0xx1.livedetect.camera.CameraReticleAnimator;
import com.n0xx1.livedetect.camera.GraphicOverlay;

/**
 * A camera reticle that locates at the center of canvas to indicate the system is active but has
 * not detected a barcode yet.
 */
class BarcodeReticleGraphic extends BarcodeGraphicBase {

    private final CameraReticleAnimator animator;

    private final Paint ripplePaint;
    private final int rippleSizeOffset;
    private final int rippleStrokeWidth;
    private final int rippleAlpha;

    BarcodeReticleGraphic(GraphicOverlay overlay, CameraReticleAnimator animator) {
        super(overlay);
        this.animator = animator;

        Resources resources = overlay.getResources();
        ripplePaint = new Paint();
        ripplePaint.setStyle(Style.STROKE);
        ripplePaint.setColor(ContextCompat.getColor(context, R.color.reticle_ripple));
        rippleSizeOffset =
                resources.getDimensionPixelOffset(R.dimen.barcode_reticle_ripple_size_offset);
        rippleStrokeWidth =
                resources.getDimensionPixelOffset(R.dimen.barcode_reticle_ripple_stroke_width);
        rippleAlpha = ripplePaint.getAlpha();
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        // Draws the ripple to simulate the breathing animation effect.
        ripplePaint.setAlpha((int) (rippleAlpha * animator.getRippleAlphaScale()));
        ripplePaint.setStrokeWidth(rippleStrokeWidth * animator.getRippleStrokeWidthScale());
        float offset = rippleSizeOffset * animator.getRippleSizeScale();
        RectF rippleRect =
                new RectF(
                        boxRect.left - offset,
                        boxRect.top - offset,
                        boxRect.right + offset,
                        boxRect.bottom + offset);
        canvas.drawRoundRect(rippleRect, boxCornerRadius, boxCornerRadius, ripplePaint);
    }
}
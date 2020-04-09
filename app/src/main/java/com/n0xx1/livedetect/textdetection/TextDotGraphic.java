package com.n0xx1.livedetect.textdetection;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.RectF;

import androidx.core.content.ContextCompat;

import com.n0xx1.livedetect.R;
import com.n0xx1.livedetect.camera.GraphicOverlay;
import com.n0xx1.livedetect.camera.GraphicOverlay.Graphic;
import com.n0xx1.livedetect.settings.PreferenceUtils;

;

/**
 * Similar to the camera reticle but with additional progress ring to indicate an entity is getting
 * confirmed for a follow up processing, e.g. product search.
 */

public class TextDotGraphic extends Graphic {


    private final Paint outerRingFillPaint;
    private final Paint outerRingStrokePaint;
    private final Paint innerRingPaint;
    private final Paint progressRingStrokePaint;
    private final int outerRingFillRadius;
    private final int outerRingStrokeRadius;
    private final int innerRingStrokeRadius;

    TextDotGraphic(
            GraphicOverlay overlay) {

        super(overlay);

        Resources resources = overlay.getResources();
        outerRingFillPaint = new Paint();
        outerRingFillPaint.setStyle(Style.FILL);
        outerRingFillPaint.setColor(
                ContextCompat.getColor(context, R.color.entity_reticle_outer_ring_fill));

        outerRingStrokePaint = new Paint();
        outerRingStrokePaint.setStyle(Style.STROKE);
        outerRingStrokePaint.setStrokeWidth(
                resources.getDimensionPixelOffset(R.dimen.entity_reticle_outer_ring_stroke_width));
        outerRingStrokePaint.setStrokeCap(Cap.ROUND);
        outerRingStrokePaint.setColor(
                ContextCompat.getColor(context, R.color.entity_reticle_outer_ring_stroke));

        progressRingStrokePaint = new Paint();
        progressRingStrokePaint.setStyle(Style.STROKE);
        progressRingStrokePaint.setStrokeWidth(
                resources.getDimensionPixelOffset(R.dimen.entity_reticle_outer_ring_stroke_width));
        progressRingStrokePaint.setStrokeCap(Cap.ROUND);
        progressRingStrokePaint.setColor(ContextCompat.getColor(context, R.color.white));

        if (PreferenceUtils.isMultipleEntitysMode(overlay.getContext())) {
            innerRingPaint = new Paint();
            innerRingPaint.setStyle(Style.FILL);
            innerRingPaint.setColor(ContextCompat.getColor(context, R.color.entity_reticle_inner_ring));
        } else {
            innerRingPaint = new Paint();
            innerRingPaint.setStyle(Style.STROKE);
            innerRingPaint.setStrokeWidth(
                    resources.getDimensionPixelOffset(R.dimen.entity_reticle_inner_ring_stroke_width));
            innerRingPaint.setStrokeCap(Cap.ROUND);
            innerRingPaint.setColor(ContextCompat.getColor(context, R.color.white));
        }

        outerRingFillRadius =
                resources.getDimensionPixelOffset(R.dimen.entity_reticle_outer_ring_fill_radius);
        outerRingStrokeRadius =
                resources.getDimensionPixelOffset(R.dimen.entity_reticle_outer_ring_stroke_radius);
        innerRingStrokeRadius =
                resources.getDimensionPixelOffset(R.dimen.entity_reticle_inner_ring_stroke_radius);
    }

    @Override
    public void draw(Canvas canvas) {
        float cx = canvas.getWidth() / 2f;
        float cy = canvas.getHeight() / 2f;
        canvas.drawCircle(cx, cy, outerRingFillRadius, outerRingFillPaint);
        canvas.drawCircle(cx, cy, outerRingStrokeRadius, outerRingStrokePaint);
        canvas.drawCircle(cx, cy, innerRingStrokeRadius, innerRingPaint);

        RectF progressRect =
                new RectF(
                        cx - 2 * outerRingStrokeRadius,
                        cy - 2 * outerRingStrokeRadius,
                        cx + 2 * outerRingStrokeRadius,
                        cy + 2 * outerRingStrokeRadius);
        float sweepAngle = 1 * 360;
        canvas.drawArc(
                progressRect,
                /* startAngle= */ 0,
                sweepAngle,
                /* useCenter= */ false,
                progressRingStrokePaint);
    }
}

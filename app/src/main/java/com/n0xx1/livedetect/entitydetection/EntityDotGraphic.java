package com.n0xx1.livedetect.entitydetection;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PointF;
import android.graphics.Rect;

import com.n0xx1.livedetect.R;
import com.n0xx1.livedetect.camera.GraphicOverlay;
import com.n0xx1.livedetect.camera.GraphicOverlay.Graphic;

/** A dot to indicate a detected entity used by multiple entitys detection mode. */
class EntityDotGraphic extends Graphic {

    private final EntityDotAnimator animator;
    private final Paint paint;
    private final PointF center;
    private final int dotRadius;
    private final int dotAlpha;

    EntityDotGraphic(GraphicOverlay overlay, DetectedEntity entity, EntityDotAnimator animator) {
        super(overlay);

        this.animator = animator;

        Rect box = entity.getBoundingBox();
        center =
                new PointF(
                        overlay.translateX((box.left + box.right) / 2f),
                        overlay.translateY((box.top + box.bottom) / 2f));

        paint = new Paint();
        paint.setStyle(Style.FILL);
        paint.setColor(Color.WHITE);

        dotRadius = context.getResources().getDimensionPixelOffset(R.dimen.entity_dot_radius);
        dotAlpha = paint.getAlpha();
    }

    @Override
    public void draw(Canvas canvas) {
        paint.setAlpha((int) (dotAlpha * animator.getAlphaScale()));
        canvas.drawCircle(center.x, center.y, dotRadius * animator.getRadiusScale(), paint);
    }
}

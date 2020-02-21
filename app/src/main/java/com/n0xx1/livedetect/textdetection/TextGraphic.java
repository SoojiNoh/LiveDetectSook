package com.n0xx1.livedetect.textdetection;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.n0xx1.livedetect.camera.GraphicOverlay;

/**
 * Graphic instance for rendering TextBlock position, size, and ID within an associated graphic
 * overlay view.
 */
public class TextGraphic extends GraphicOverlay.Graphic {

    private static final int TEXT_COLOR = Color.WHITE;
    private static final float TEXT_SIZE = 54.0f;
    private static final float STROKE_WIDTH = 4.0f;

    private final Paint rectPaint;
    private final Paint textPaint;
    private final RectF rect;
    private final FirebaseVisionText.Element text;

    TextGraphic(GraphicOverlay overlay, FirebaseVisionText.Element text) {
        super(overlay);

        this.text = text;

        rectPaint = new Paint();
        rectPaint.setColor(TEXT_COLOR);
        rectPaint.setStyle(Paint.Style.STROKE);
        rectPaint.setStrokeWidth(STROKE_WIDTH);

        rect = new RectF(text.getBoundingBox());
        rect.left =  overlay.translateX(rect.left);
        rect.top = overlay.translateY(rect.top);
        rect.right = overlay.translateX(rect.right);
        rect.bottom = overlay.translateY(rect.bottom);

        textPaint = new Paint();
        textPaint.setColor(TEXT_COLOR);
        textPaint.setTextSize(TEXT_SIZE);
        // Redraw the overlay, as this graphic has been added.
        postInvalidate();
    }

    /** Draws the text block annotations for position, size, and raw value on the supplied canvas. */
    @Override
    public void draw(Canvas canvas) {
        if (text == null) {
            throw new IllegalStateException("Attempting to draw a null text.");
        }

        // Draws the bounding box around the TextBlock.

        canvas.drawRect(rect, rectPaint);

        // Renders the text at the bottom of the box.
        canvas.drawText(text.getText(), rect.left, rect.bottom, textPaint);
    }
}

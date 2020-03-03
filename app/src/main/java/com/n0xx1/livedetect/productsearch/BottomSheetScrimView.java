package com.n0xx1.livedetect.productsearch;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

import com.n0xx1.livedetect.R;

import static com.google.common.base.Preconditions.checkArgument;

public class BottomSheetScrimView extends View{

    private static final String TAG = "BottomSheetScrimView";

    private static final float DOWN_PERCENT_TO_HIDE_THUMBNAIL = 0.42f;

    private final Paint scrimPaint;
    private final Paint thumbnailPaint;
    private final Paint boxPaint;
    private final int thumbnailHeight;
    private final int thumbnailMargin;
    private final int boxCornerRadius;

    private Bitmap thumbnailBitmap;
    private RectF thumbnailRect;
    private float downPercentInCollapsed;

    public BottomSheetScrimView(Context context, AttributeSet attrs) {
        super(context, attrs);

        Resources resources = context.getResources();
        scrimPaint = new Paint();
        scrimPaint.setColor(ContextCompat.getColor(context, R.color.dark));

        thumbnailPaint = new Paint();

        boxPaint = new Paint();
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(
                resources.getDimensionPixelOffset(R.dimen.object_thumbnail_stroke_width));
        boxPaint.setColor(Color.WHITE);

        thumbnailHeight = resources.getDimensionPixelOffset(R.dimen.object_thumbnail_height);
        thumbnailMargin = resources.getDimensionPixelOffset(R.dimen.object_thumbnail_margin);
        boxCornerRadius = resources.getDimensionPixelOffset(R.dimen.bounding_box_corner_radius);
    }

    /**
     * Translates the object thumbnail up or down along with bottom sheet's sliding movement, with
     * keeping thumbnail size fixed.
     */
    public void updateWithThumbnailTranslate(
            Bitmap thumbnailBitmap, int collapsedStateHeight, float slideOffset, View bottomSheet) {
        this.thumbnailBitmap = thumbnailBitmap;

        float currentSheetHeight;
        if (slideOffset < 0) {
            downPercentInCollapsed = -slideOffset;
            currentSheetHeight = collapsedStateHeight * (1 + slideOffset);
        } else {
            downPercentInCollapsed = 0;
            currentSheetHeight =
                    collapsedStateHeight + (bottomSheet.getHeight() - collapsedStateHeight) * slideOffset;
        }

        float thumbnailWidth =
                (float) thumbnailBitmap.getWidth() / thumbnailBitmap.getHeight() * thumbnailHeight;
        thumbnailRect = new RectF();
        thumbnailRect.left = thumbnailMargin;
        thumbnailRect.top = getHeight() - currentSheetHeight - thumbnailMargin - thumbnailHeight;
        thumbnailRect.right = thumbnailRect.left + thumbnailWidth;
        thumbnailRect.bottom = thumbnailRect.top + thumbnailHeight;

        invalidate();
    }

    /**
     * Translates the object thumbnail from original bounding box location to at where the bottom
     * sheet is settled as COLLAPSED state, with its size scales gradually.
     *
     * <p>It's only used by sliding the sheet up from hidden state to collapsed state.
     */
    public void updateWithThumbnailTranslateAndScale(
            Bitmap thumbnailBitmap, int collapsedStateHeight, float slideOffset, RectF srcThumbnailRect) {
        checkArgument(
                slideOffset <= 0,
                "Scale mode works only when the sheet is between hidden and collapsed states.");

        this.thumbnailBitmap = thumbnailBitmap;
        this.downPercentInCollapsed = 0;

        float dstX = thumbnailMargin;
        float dstY = getHeight() - collapsedStateHeight - thumbnailMargin - thumbnailHeight;
        float dstHeight = thumbnailHeight;
        float dstWidth = srcThumbnailRect.width() / srcThumbnailRect.height() * dstHeight;
        RectF dstRect = new RectF(dstX, dstY, dstX + dstWidth, dstY + dstHeight);

        float progressToCollapsedState = 1 + slideOffset;
        thumbnailRect = new RectF();
        thumbnailRect.left =
                srcThumbnailRect.left + (dstRect.left - srcThumbnailRect.left) * progressToCollapsedState;
        thumbnailRect.top =
                srcThumbnailRect.top + (dstRect.top - srcThumbnailRect.top) * progressToCollapsedState;
        thumbnailRect.right =
                srcThumbnailRect.right
                        + (dstRect.right - srcThumbnailRect.right) * progressToCollapsedState;
        thumbnailRect.bottom =
                srcThumbnailRect.bottom
                        + (dstRect.bottom - srcThumbnailRect.bottom) * progressToCollapsedState;

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draws the dark background.
        canvas.drawRect(0, 0, getWidth(), getHeight(), scrimPaint);
        if (thumbnailBitmap != null && downPercentInCollapsed < DOWN_PERCENT_TO_HIDE_THUMBNAIL) {
            int alpha = (int) ((1 - (downPercentInCollapsed / DOWN_PERCENT_TO_HIDE_THUMBNAIL)) * 255);

            // Draws the object thumbnail.
            thumbnailPaint.setAlpha(alpha);
            canvas.drawBitmap(thumbnailBitmap, /* src= */ null, thumbnailRect, thumbnailPaint);

            // Draws the bounding box.
            boxPaint.setAlpha(alpha);
            canvas.drawRoundRect(thumbnailRect, boxCornerRadius, boxCornerRadius, boxPaint);
        }
    }



    // Hold a reference to the current animator,
    // so that it can be canceled mid-way.
    private Animator currentAnimator;

    // The system "short" animation time duration, in milliseconds. This
    // duration is ideal for subtle animations or animations that occur
    // very frequently.
    private int shortAnimationDuration;



    public void zoomImageFromThumb(ImageView imageView, Bitmap image) {

        View thumbView = this;


        shortAnimationDuration = getResources().getInteger(
                android.R.integer.config_shortAnimTime);

        // If there's an animation in progress, cancel it
        // immediately and proceed with this one.
        if (currentAnimator != null) {
            currentAnimator.cancel();
        }

        // Load the high-resolution "zoomed-in" image.
        final ImageView expandedImageView = imageView;
        expandedImageView.setImageBitmap(image);

        // Calculate the starting and ending bounds for the zoomed-in image.
        // This step involves lots of math. Yay, math.
        final Rect startBounds = new Rect();
        final Rect finalBounds = new Rect();
        final Point globalOffset = new Point();

        // The start bounds are the global visible rectangle of the thumbnail,
        // and the final bounds are the global visible rectangle of the container
        // view. Also set the container view's offset as the origin for the
        // bounds, since that's the origin for the positioning animation
        // properties (X, Y).
        thumbView.getGlobalVisibleRect(startBounds);
//        findViewById(R.id.container)
        imageView
                .getGlobalVisibleRect(finalBounds, globalOffset);
        startBounds.offset(-globalOffset.x, -globalOffset.y);
        finalBounds.offset(-globalOffset.x, -globalOffset.y);

        // Adjust the start bounds to be the same aspect ratio as the final
        // bounds using the "center crop" technique. This prevents undesirable
        // stretching during the animation. Also calculate the start scaling
        // factor (the end scaling factor is always 1.0).
        float startScale;
        if ((float) finalBounds.width() / finalBounds.height()
                > (float) startBounds.width() / startBounds.height()) {
            // Extend start bounds horizontally
            startScale = (float) startBounds.height() / finalBounds.height();
            float startWidth = startScale * finalBounds.width();
            float deltaWidth = (startWidth - startBounds.width()) / 2;
            startBounds.left -= deltaWidth;
            startBounds.right += deltaWidth;
        } else {
            // Extend start bounds vertically
            startScale = (float) startBounds.width() / finalBounds.width();
            float startHeight = startScale * finalBounds.height();
            float deltaHeight = (startHeight - startBounds.height()) / 2;
            startBounds.top -= deltaHeight;
            startBounds.bottom += deltaHeight;
        }

        // Hide the thumbnail and show the zoomed-in view. When the animation
        // begins, it will position the zoomed-in view in the place of the
        // thumbnail.
        thumbView.setAlpha(0f);
        expandedImageView.setVisibility(View.VISIBLE);

        // Set the pivot point for SCALE_X and SCALE_Y transformations
        // to the top-left corner of the zoomed-in view (the default
        // is the center of the view).
        expandedImageView.setPivotX(0f);
        expandedImageView.setPivotY(0f);

        // Construct and run the parallel animation of the four translation and
        // scale properties (X, Y, SCALE_X, and SCALE_Y).
        AnimatorSet set = new AnimatorSet();
        set
                .play(ObjectAnimator.ofFloat(expandedImageView, View.X,
                        startBounds.left, finalBounds.left))
                .with(ObjectAnimator.ofFloat(expandedImageView, View.Y,
                        startBounds.top, finalBounds.top))
                .with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_X,
                        startScale, 1f))
                .with(ObjectAnimator.ofFloat(expandedImageView,
                        View.SCALE_Y, startScale, 1f));
        set.setDuration(shortAnimationDuration);
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                currentAnimator = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                currentAnimator = null;
            }
        });
        set.start();
        currentAnimator = set;

        // Upon clicking the zoomed-in image, it should zoom back down
        // to the original bounds and show the thumbnail instead of
        // the expanded image.
        final float startScaleFinal = startScale;
        expandedImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentAnimator != null) {
                    currentAnimator.cancel();
                }

                // Animate the four positioning/sizing properties in parallel,
                // back to their original values.
                AnimatorSet set = new AnimatorSet();
                set.play(ObjectAnimator
                        .ofFloat(expandedImageView, View.X, startBounds.left))
                        .with(ObjectAnimator
                                .ofFloat(expandedImageView,
                                        View.Y,startBounds.top))
                        .with(ObjectAnimator
                                .ofFloat(expandedImageView,
                                        View.SCALE_X, startScaleFinal))
                        .with(ObjectAnimator
                                .ofFloat(expandedImageView,
                                        View.SCALE_Y, startScaleFinal));
                set.setDuration(shortAnimationDuration);
                set.setInterpolator(new DecelerateInterpolator());
                set.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        thumbView.setAlpha(1f);
                        expandedImageView.setVisibility(View.GONE);
                        currentAnimator = null;
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        thumbView.setAlpha(1f);
                        expandedImageView.setVisibility(View.GONE);
                        currentAnimator = null;
                    }
                });
                set.start();
                currentAnimator = set;
            }
        });
    }
}

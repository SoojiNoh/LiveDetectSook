package com.n0xx1.livedetect.staticdetection;

import android.view.MotionEvent;
import android.view.View;

public abstract class TouchTimer implements View.OnTouchListener {

    private static final String TAG = "TouchTimer";

    private long touchStart = 0l;
    private long touchNow = 0l;


    private boolean Touching = false;

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                this.touchStart = System.currentTimeMillis();
                Touching = true;
                return true;

            case MotionEvent.ACTION_UP:
                return true;

            case MotionEvent.ACTION_MOVE:
                this.touchNow = System.currentTimeMillis();
                long touchTime = this.touchNow - this.touchStart;
                onTouching(touchTime);
                return true;

            default:
                return false;
        }
    }


    protected abstract void onTouching(long touchTimeInMillis);


    public boolean isTouching(){
        return Touching;
    }
}
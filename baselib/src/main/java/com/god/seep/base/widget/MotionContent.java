package com.god.seep.base.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

public class MotionContent extends LinearLayout {

    public MotionContent(Context context) {
        super(context);
    }

    public MotionContent(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public MotionContent(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }

    float x = 0, y = 0;
    int mTouchSlop;
    boolean isMoving = false;

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (animator != null && animator.isRunning())
            return true;
        float tmpX = event.getRawX();   //rawX为屏幕上的绝对位置，getX为当前View内部的相对位置
        float tmpY = event.getRawY();   //使用getY会出现抖动现象
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                x = tmpX;
                y = tmpY;
                isMoving = false;
                break;
            case MotionEvent.ACTION_MOVE:
                if (y == 0) {
                    x = tmpX;
                    y = tmpY;
                }
                float absX = Math.abs(x - tmpX);
                float absY = Math.abs(y - tmpY);
                if (absY > absX && absY > mTouchSlop) {
                    isMoving = true;
                    if (mListener != null)
                        mListener.onMove(tmpY - y);
                    y = tmpY;
                    x = tmpX;
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                x = 0;
                y = 0;
                if (mListener != null) {
                    mListener.onStop();
                }
                if (isMoving) {
                    isMoving = false;
                    return true;
                }
                break;
        }
        return super.dispatchTouchEvent(event);
    }

    private ValueAnimator animator;

    public void startAnimation(View view, int startHeight, int endHeight, long duration) {
        if (duration <= 0) return;
        if (animator != null && animator.isRunning())
            return;
        animator = ValueAnimator.ofInt(startHeight, endHeight);
        animator.setDuration(duration);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int value = (int) animation.getAnimatedValue();
                ViewGroup.LayoutParams lp = view.getLayoutParams();
                lp.height = value;
                view.setLayoutParams(lp);
            }
        });
        animator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (animator != null) {
            animator.cancel();
            animator.removeAllUpdateListeners();
        }
    }

    private MoveListener mListener;

    public void setMoveListener(MoveListener mListener) {
        this.mListener = mListener;
    }

    public interface MoveListener {
        void onMove(float distance);

        void onStop();
    }
}

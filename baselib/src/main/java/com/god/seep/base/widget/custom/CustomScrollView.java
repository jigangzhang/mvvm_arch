package com.god.seep.base.widget.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;

/**
 */
public class CustomScrollView extends NestedScrollView {
    private OnContinueFlingListener listener;
    private int mTouchSlop;
    private float lastY;
    private float lastX;
    private float velocityY;//竖向滑动速度

    public CustomScrollView(@NonNull Context context) {
        this(context, null);
    }

    public CustomScrollView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomScrollView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            lastY = ev.getY();
            lastX = ev.getX();
        } else if (ev.getAction() == MotionEvent.ACTION_MOVE) {
            float diffY = ev.getY() - lastY;
            float diffX = ev.getX() - lastX;
            lastY = ev.getY();
            lastX = ev.getX();
            if (Math.abs(diffX) > Math.abs(diffY))
                return false;

            //向上滑动，手指下滑
            if (diffY > 0 && diffY > mTouchSlop) {
                if (listener != null && listener.shouldInterceptUp())
                    return true;
            }
            //向下滑动，手指上滑, -1向上滚动检查，1 向下滚动检查。
            boolean canScrollVertical = canScrollVertically(1);
            if (diffY < 0 && -diffY > mTouchSlop) {
                if (canScrollVertical)
                    return true;
                else
                    return false;
//                if (listener != null && !listener.shouldInterceptDown())
//                    return super.onInterceptTouchEvent(ev);
//                return true;
            }
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            lastY = ev.getY();
            lastX = ev.getX();
        } else if (ev.getAction() == MotionEvent.ACTION_MOVE) {
            float diffY = ev.getY() - lastY;
            lastY = ev.getY();
            lastX = ev.getX();
            boolean canScrollDown = canScrollVertically(1);
            if (diffY < 0 && -diffY > mTouchSlop) {
                if (!canScrollDown) {
//                    dispatchTouchEvent(ev);
//                    return false;
                }
            }
        }
        return super.onTouchEvent(ev);
    }

    public void setOnFlingListener(OnContinueFlingListener listener) {
        this.listener = listener;
    }

    /**
     * 获取fling被中断时的Y速度（未知？？），然后将其传至下面的RecyclerView继续fling
     */
    public interface OnContinueFlingListener {

        void onFling(int y);

        boolean shouldInterceptUp();

        boolean shouldInterceptDown();
    }
}

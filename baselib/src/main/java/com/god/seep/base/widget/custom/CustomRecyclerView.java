package com.god.seep.base.widget.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

/**
 */
public class CustomRecyclerView extends RecyclerView {
    private int mTouchSlop;
    private float lastY;

    public CustomRecyclerView(@NonNull Context context) {
        super(context);
    }

    public CustomRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            lastY = e.getY();
        } else if (e.getAction() == MotionEvent.ACTION_MOVE) {
            float diffY = e.getY() - lastY;
            lastY = e.getY();

            //向上滑动，手指下滑
            if (diffY > 0 && diffY > mTouchSlop) {
                if (!canScrollVertically(-1)) {
//                    requestDisallowInterceptTouchEvent(false);
//                    return false;
                }
            }
            //向下滑动，手指上滑.  -1向上滚动检查，1 向下滚动检查。
            boolean canScrollVertical = canScrollVertically(1);
            if (diffY < 0 && -diffY > mTouchSlop && canScrollVertical) {
                requestDisallowInterceptTouchEvent(true);
//                return true;
            }
        }
        return super.onTouchEvent(e);
    }
}

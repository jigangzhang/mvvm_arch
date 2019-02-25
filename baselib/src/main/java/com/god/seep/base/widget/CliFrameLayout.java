package com.god.seep.base.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CliFrameLayout extends FrameLayout {
    public CliFrameLayout(@NonNull Context context) {
        super(context);
    }

    public CliFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CliFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        View view = getChildAt(0);
        int count = getChildCount();
        if (count > 1) {
            View child = getChildAt(1);
            if (child != null && child.getVisibility() == View.VISIBLE)
                return super.onTouchEvent(event);
        }
        if (view instanceof CliViewPager)
            return view.dispatchTouchEvent(event);
        else
            return super.onTouchEvent(event);
    }
}

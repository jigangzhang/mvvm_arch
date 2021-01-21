package com.god.seep.base.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

public class StickinessLayout extends LinearLayout {
    public StickinessLayout(Context context) {
        super(context);
    }

    public StickinessLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public StickinessLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void addOnLayoutChangeListener(OnLayoutChangeListener listener) {
        super.addOnLayoutChangeListener(listener);
        new OnLayoutChangeListener(){
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
//                if (top==left){}
//                v.getLocationInWindow();
//                v.getLocationOnScreen();
            }
        };
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (t == l) {}
    }
}

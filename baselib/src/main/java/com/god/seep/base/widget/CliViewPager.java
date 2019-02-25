package com.god.seep.base.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import com.god.seep.base.util.ScreenHelper;

import androidx.viewpager.widget.ViewPager;

public class CliViewPager extends ViewPager {
    private int mTouchSlop;
    private float x;
    private float y;
    private int offset;

    public CliViewPager(Context context) {
        super(context);
    }

    public CliViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
        offset = ScreenHelper.dp2Px(getContext(), 50);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                x = ev.getX();
                y = ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
                float dx = Math.abs(x - ev.getX());
                float dy = Math.abs(y - ev.getY());
                if (dx <= mTouchSlop && dx <= dy) {
                    if (x < offset && this.getCurrentItem() > 0) {
                        this.setCurrentItem(getCurrentItem() - 1, true);
                        return true;
                    }
                    if ((getWidth() + offset) < x && this.getCurrentItem() < getAdapter().getCount()) {
                        this.setCurrentItem(getCurrentItem() + 1, true);
                        return true;
                    }
                }
                break;
        }
        return super.onTouchEvent(ev);
    }
}

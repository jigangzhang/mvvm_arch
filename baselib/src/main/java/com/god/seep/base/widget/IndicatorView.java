package com.god.seep.base.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.god.seep.base.R;
import com.god.seep.base.util.ScreenHelper;

import androidx.annotation.Nullable;

public class IndicatorView extends LinearLayout {
    private GradientDrawable mSelectDrawable;
    private GradientDrawable mUnSelectDrawable;

    public IndicatorView(Context context) {
        this(context, null);
    }

    public IndicatorView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IndicatorView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr);
    }

    private void init(AttributeSet attrs, int defStyle) {
        TypedArray array = getContext().obtainStyledAttributes(attrs, R.styleable.IndicatorView, defStyle, 0);
        int size = array.getInt(R.styleable.IndicatorView_size, 0);
        float diameter = array.getDimension(R.styleable.IndicatorView_diameter, ScreenHelper.dp2Px(getContext(), 11));
        int selectedColor = array.getColor(R.styleable.IndicatorView_selectedColor, getContext().getResources().getColor(R.color.white));
        int unSelectedColor = array.getColor(R.styleable.IndicatorView_unSelectedColor, getContext().getResources().getColor(R.color.white));
        array.recycle();

        mUnSelectDrawable = new GradientDrawable();
        mUnSelectDrawable.setStroke(ScreenHelper.dp2Px(getContext(), 1), unSelectedColor);
        mUnSelectDrawable.setShape(GradientDrawable.OVAL);
        mUnSelectDrawable.setSize((int) diameter, (int) diameter);

        mSelectDrawable = new GradientDrawable();
        mSelectDrawable.setColor(selectedColor);
        mSelectDrawable.setShape(GradientDrawable.OVAL);
        mSelectDrawable.setSize((int) (diameter), (int) (diameter));

        for (int i = 0; i < size; i++) {
            ImageView view = new ImageView(getContext());
            int px = ScreenHelper.dp2Px(getContext(), 4);
            view.setPadding(px, 0, px, 0);
            view.setImageDrawable(mUnSelectDrawable);
            addView(view);
        }
    }

    public void setCurrentPosition(int currentPosition) {
        for (int i = 0; i < getChildCount(); i++)
            ((ImageView) getChildAt(i)).setImageDrawable(i == currentPosition ? mSelectDrawable : mUnSelectDrawable);
    }
}

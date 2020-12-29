package com.god.seep.base.widget.custom;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager2.widget.ViewPager2;

import com.god.seep.base.R;
import com.god.seep.base.util.ScreenHelper;

public class CustomIndicatorView extends View {
    private int size;
    private int radius;
    private int indicatorSpace;
    private int currentIndicator = 0;
    private int selectedColor;
    private int unselectedColor;
    private Paint mPaint;

    public CustomIndicatorView(Context context) {
        this(context, null);
    }

    public CustomIndicatorView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomIndicatorView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CustomIndicatorView, defStyleAttr, 0);
        size = typedArray.getInt(R.styleable.CustomIndicatorView_size, 0);
        int diameter = typedArray.getDimensionPixelSize(R.styleable.CustomIndicatorView_diameter, ScreenHelper.dp2Px(context, 6));
        radius = diameter / 2;
        indicatorSpace = typedArray.getDimensionPixelSize(R.styleable.CustomIndicatorView_indicatorSpace, ScreenHelper.dp2Px(context, 5));
        selectedColor = typedArray.getColor(R.styleable.CustomIndicatorView_selectedColor, context.getResources().getColor(R.color.colorAccent));
        unselectedColor = typedArray.getColor(R.styleable.CustomIndicatorView_unSelectedColor, context.getResources().getColor(R.color.white));
        typedArray.recycle();
        initPaint();
    }

    private void initPaint() {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
    }

    public void setSize(int size) {
        this.size = size;
        requestLayout();
        invalidate();
    }

    /**
     * 像素尺寸
     */
    public void setRadius(int radius) {
        this.radius = radius;
        requestLayout();
        invalidate();
    }

    public void setIndicatorSpace(int indicatorSpace) {
        this.indicatorSpace = indicatorSpace;
        requestLayout();
        invalidate();
    }

    public void setCurrentIndicator(int currentIndicator) {
        if (currentIndicator >= size)
            currentIndicator = size - 1;
        this.currentIndicator = currentIndicator;
        invalidate();
    }

    public void setSelectedColor(int selectedColor) {
        this.selectedColor = selectedColor;
        invalidate();
    }

    public void setUnselectedColor(int unselectedColor) {
        this.unselectedColor = unselectedColor;
        invalidate();
    }

    public void setWithViewPager(ViewPager viewPager) {
        if (viewPager != null) {
            PagerAdapter adapter = viewPager.getAdapter();
            if (adapter != null)
                size = adapter.getCount();
            viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                }

                @Override
                public void onPageSelected(int position) {
                    setCurrentIndicator(position);
                }

                @Override
                public void onPageScrollStateChanged(int state) {

                }
            });
        }
    }

    public void setWithViewPager2(ViewPager2 viewPager) {
        if (viewPager != null) {
            RecyclerView.Adapter adapter = viewPager.getAdapter();
            if (adapter != null)
                size = adapter.getItemCount();
            viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {

                @Override
                public void onPageSelected(int position) {
                    setCurrentIndicator(position);
                }
            });
        }
    }

    /**
     * WRAP_CONTENT时，要给定最小的宽高值，setMeasuredDimension给出一个确定值
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int minHeight = radius * 2;
        int minWidth = radius * 2 * size + indicatorSpace * (size - 1);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (width < minWidth)
            width = minWidth;
        if (height < minHeight)
            height = minHeight;
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx;
        int cy = getHeight() / 2;
        int diameter = radius * 2;
        float totalWidth = size * diameter + indicatorSpace * (size - 1);
        float startX = (getWidth() - totalWidth) / 2;
        for (int i = 0; i < size; i++) {
            cx = startX + i * diameter + i * indicatorSpace + radius;
            mPaint.setColor(i == currentIndicator ? selectedColor : unselectedColor);
            canvas.drawCircle(cx, cy, radius, mPaint);
        }
    }
}

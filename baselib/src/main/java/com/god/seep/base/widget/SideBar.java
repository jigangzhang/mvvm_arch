package com.god.seep.base.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import com.god.seep.base.R;
import com.god.seep.base.util.ScreenHelper;

import java.util.List;

import androidx.annotation.Nullable;

public class SideBar extends View {
    private Rect mRect;
    private Paint mPaint;
    private float itemHeight;
    private float itemWidth;

    public SideBar(Context context) {
        this(context, null);
    }

    public SideBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SideBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

//    private char characters[] = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
//            'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};

    private String[] characters;

    public void setCharacters(List<String> characters) {
        int[] a = new int[1];
        this.characters = new String[characters.size()];
        for (int i = 0; i < characters.size(); i++)
            this.characters[i] = characters.get(i);
        requestLayout();
        invalidate();
    }

    void init(Context context, AttributeSet attrs) {
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.SideBar);
        int textSize = array.getDimensionPixelSize(R.styleable.SideBar_side_textSize, 16);
        array.recycle();

        mRect = new Rect();
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.parseColor("#2B2A39"));
        mPaint.setTextSize(textSize);
    }

    public void setTextSize(float size) {
        float dimension = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, size, getContext().getResources().getDisplayMetrics());
        if (mPaint != null && mPaint.getTextSize() != dimension) {
            mPaint.setTextSize(dimension);
            requestLayout();
            invalidate();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (characters == null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        if (widthMode == MeasureSpec.AT_MOST) {
            float width = mPaint.measureText(characters[0], 0, 1)
                    + ScreenHelper.dp2Px(getContext(), 2) + getPaddingStart() + getPaddingEnd();
            widthMeasureSpec = MeasureSpec.makeMeasureSpec((int) width, widthMode);
        }
        if (heightMode == MeasureSpec.AT_MOST) {
            mRect.setEmpty();
            mPaint.getTextBounds(characters[0], 0, 1, mRect);
            float height = (mRect.height() + ScreenHelper.dp2Px(getContext(), 4)) * characters.length;
            heightMeasureSpec = MeasureSpec.makeMeasureSpec((int) height, MeasureSpec.EXACTLY);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        itemWidth = mPaint.measureText(characters[0], 0, 1) + ScreenHelper.dp2Px(getContext(), 2);
        itemHeight = getMeasuredHeight() / characters.length;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (characters == null) return;
        for (int i = 0; i < characters.length; i++) {
//            float textWidth = mPaint.measureText(characters, i, 1);
            mRect.setEmpty();
            mPaint.getTextBounds(characters[i], 0, 1, mRect);
            //y值为字的基线，在字的底部
            canvas.drawText(characters[i], 0, 1, getPaddingStart() + (itemWidth - mRect.width()) / 2,
                    i * itemHeight + (itemHeight + mRect.height()) / 2, mPaint);
        }
    }

    int touchIndex = -1;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        touchIndex = (int) (event.getY() / itemHeight);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                if (mListener != null && touchIndex > -1 && touchIndex < characters.length)
                    mListener.onLetterChanged(characters[touchIndex]);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                touchIndex = -1;
                if (mListener != null)
                    mListener.onLetterChangeFinish();
                break;
        }
        return true;
//        return super.onTouchEvent(event);
    }

    private OnLetterChangeListener mListener;

    public void setOnLetterChangeListener(OnLetterChangeListener mListener) {
        this.mListener = mListener;
    }

    public interface OnLetterChangeListener {
        void onLetterChanged(String letter);

        void onLetterChangeFinish();
    }
}

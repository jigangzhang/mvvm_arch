package com.god.seep.base.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.god.seep.base.R;
import com.god.seep.base.util.ScreenHelper;

import androidx.annotation.Nullable;

public class CornerLayout extends LinearLayout {
    private Paint mPaint;
    private Path mPath;
    private float mRadius;
    private float mRectHeight;
    private float mCenterX;

    public CornerLayout(Context context) {
        this(context, null);
    }

    public CornerLayout(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CornerLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initPaint();
    }

    private void initPaint() {
        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(getContext().getResources().getColor(R.color.white));
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(ScreenHelper.dp2Px(getContext(), 1));

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(getMeasuredWidth(), measureHeight(heightMeasureSpec));
    }

    private int measureHeight(int heightMeasureSpec) {
        int height = ScreenHelper.dp2Px(getContext(), 48);
        int mode = MeasureSpec.getMode(heightMeasureSpec);
        int size = MeasureSpec.getSize(heightMeasureSpec);
        if (mode == MeasureSpec.EXACTLY) {
            height = size;
        } else if (mode == MeasureSpec.AT_MOST) {
            height = size;
        }
        return height;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        mRectHeight = getHeight() - ScreenHelper.dp2Px(getContext(), 10);
        mRadius = mRectHeight / 2;
        mCenterX = getWidth() / 2;
        //画圆及阴影
        mPaint.setShadowLayer(20, 3, 5, getContext().getResources().getColor(R.color.blue_shadow));
        canvas.drawCircle(mRadius, mRadius, mRadius, mPaint);
        mPaint.setShadowLayer(20, -3, 5, getContext().getResources().getColor(R.color.blue_shadow));
        canvas.drawCircle(getWidth() - mRadius, mRadius, mRadius, mPaint);
        //画矩形不加阴影
        mPaint.clearShadowLayer();
        canvas.drawRect(mRadius, 0, getWidth() - mRadius, mRectHeight, mPaint);
        //画线段
        mPaint.setShadowLayer(20, 0, 5, getContext().getResources().getColor(R.color.blue_shadow));
        canvas.drawLine(mRadius, mRectHeight - 1,
                mCenterX - ScreenHelper.dp2Px(getContext(), 5), mRectHeight - 1, mPaint);
        canvas.drawLine(mCenterX + ScreenHelper.dp2Px(getContext(), 5), mRectHeight - 1,
                getWidth() - mRadius, mRectHeight - 1, mPaint);
        //画三角形
        mPath = new Path();
        mPath.moveTo(mCenterX - ScreenHelper.dp2Px(getContext(), 5), mRectHeight);
        mPath.lineTo(mCenterX + ScreenHelper.dp2Px(getContext(), 5), mRectHeight);
        mPath.lineTo(mCenterX, getHeight());
        mPath.close();
        canvas.drawPath(mPath, mPaint);
        super.dispatchDraw(canvas);
    }
}

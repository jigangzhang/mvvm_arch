package com.god.seep.base.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import com.god.seep.base.R;

import androidx.annotation.Nullable;


/**
 * <p> 雷达扩散效果
 * </p>
 */

public class RadarView extends View {
    private Paint mPaint;
    private ValueAnimator mAnimator;
    private long mDuration = 15000;
    private float mRadius;
    private float mPercent;
    private float mCenterY;
    private float mCenterX;
    private float r1;
    private float r2;

    public RadarView(Context context) {
        this(context, null);
    }

    public RadarView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RadarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initPaint();
        initAnimator();
    }

    private void initPaint() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setAntiAlias(true);
    }

    private void initAnimator() {
        mAnimator = ValueAnimator.ofFloat(0, 1);
        mAnimator.setDuration(mDuration);
        mAnimator.setInterpolator(new LinearInterpolator());
        mAnimator.setRepeatMode(ValueAnimator.RESTART);
        mAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mPercent = (Float) animation.getAnimatedValue();
                setPercent(mPercent);
            }
        });
    }

    public void setPercent(float percent) {
        this.mPercent = percent;
        invalidate();
    }

    public void startRadar() {
        if (mAnimator == null)
            initAnimator();
        isStop = false;
        mAnimator.start();
    }

    public void stopRadar() {
        mPercent = 0;
        if (mAnimator != null)
            mAnimator.end();
        isStop = true;
        mAnimator = null;
        clearAnimation();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        mCenterX = width / 2;
        mCenterY = height / 2;
        mRadius = (float) Math.sqrt(width * width + height * height);
        r1 = mRadius / 3;
        r2 = r1 * 2;
        setMeasuredDimension(width, height);
    }

    boolean isStop = false;
    boolean secondDrawed = false;
    boolean thirdDrawed = false;

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (isStop) return;
        float r = mRadius * mPercent;

        //画第三个圆
        float third = r - r2;
        if (third >= 0) {
            drawCircle(canvas, third);
            thirdDrawed = true;
        } else if (thirdDrawed) {
            third = r1 + r;
            drawCircle(canvas, third);
        }

        //画第二个圆
        float temp = r - r1;// 小于0  r1<  <r2 大于r2及小于0的情况
        if (temp >= 0) {
            drawCircle(canvas, temp);
            secondDrawed = true;
        } else if (secondDrawed) {
            temp = r2 + r;
            drawCircle(canvas, temp);
        }

        //画第一个圆
        drawCircle(canvas, r);

    }

    private void drawCircle(Canvas canvas, float radius) {
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(getContext().getResources().getColor(R.color.blue_accent));
        canvas.drawCircle(mCenterX, mCenterY, radius, mPaint);
        mPaint.setStyle(Paint.Style.FILL);
        String hex = Integer.toHexString((int) ((1 - radius / mRadius) * 95));
        if (hex.length() < 2)
            hex = "0" + hex;
        mPaint.setColor(Color.parseColor("#" + hex + "E8EDfD"));
        canvas.drawCircle(mCenterX, mCenterY, radius, mPaint);
    }

    @Override
    protected void onDetachedFromWindow() {
        stopRadar();
        super.onDetachedFromWindow();
    }
}

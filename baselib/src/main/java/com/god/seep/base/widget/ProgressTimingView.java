package com.god.seep.base.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.os.Build;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import com.god.seep.base.R;
import com.god.seep.base.util.ScreenHelper;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * <p>
 * 圆环进度计时控件
 * </p>
 */

public class ProgressTimingView extends View {
    private ValueAnimator mAnimator;
    private String mText; //view 显示文字
    private float mPercent = 100; //进度百分比
    private float mRadius;//圆半径
    private float mSweepAngle;//偏移弧度
    private int mSize;//view 尺寸

    private float mTextSize;//文字大小
    private float mCircleStrokeWidth;//底层圆描边宽度（画笔描边宽度）
    private int mCircleBackgroundColor;//底层园颜色
    private int mFillCircleColor;//实心小圆颜色
    private int mTextColor;//文字颜色
    private int mDuration;//计时时间长度

    private OnTimingListener mListener;

    public ProgressTimingView(@NonNull Context context) {
        this(context, null);
    }


    public ProgressTimingView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ProgressTimingView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.ProgressTimingView);
        mText = array.getString(R.styleable.ProgressTimingView_text);
        mTextSize = array.getDimension(R.styleable.ProgressTimingView_textSize, ScreenHelper.sp2px(context, 13));
        mTextColor = array.getColor(R.styleable.ProgressTimingView_textColor, getResources().getColor(R.color.black_light));
        mCircleBackgroundColor = array.getColor(R.styleable.ProgressTimingView_backgroundCircleColor, getResources().getColor(R.color.gray_dd));
        mFillCircleColor = array.getColor(R.styleable.ProgressTimingView_solidCircleColor, getResources().getColor(R.color.blue_fill));
        mCircleStrokeWidth = array.getDimension(R.styleable.ProgressTimingView_circleStrokeWidth, ScreenHelper.dp2Px(context, 2));
        mDuration = array.getInteger(R.styleable.ProgressTimingView_duration, 60);
        array.recycle();
    }

    private Paint getPaint(int color, float strokeWidth, Paint.Style style) {
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setAntiAlias(true);//抗锯齿
        paint.setStyle(style);
        paint.setStrokeWidth(strokeWidth);//描边宽度
        return paint;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        return super.onSaveInstanceState();
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(state);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        mSize = width > height ? height : width;
        setMeasuredDimension(mSize, mSize);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Paint paint = getPaint(mCircleBackgroundColor, mCircleStrokeWidth, Paint.Style.STROKE);
        drawCircle(canvas, paint);
        drawArc(canvas, paint);
        drawText(canvas, paint);
        drawFillCircle(canvas, paint);
    }

    //画底层园
    private void drawCircle(Canvas canvas, Paint paint) {
        float centerPoint = mSize / 2;
        mRadius = centerPoint - paint.getStrokeWidth() / 2 * 3;
        canvas.drawCircle(centerPoint, centerPoint, mRadius, paint);
    }

    //画圆弧
    private void drawArc(Canvas canvas, Paint paint) {
        mSweepAngle = mPercent * 360 / 100;
        float thick = paint.getStrokeWidth() / 2 * 3;
        RectF rectF = new RectF(thick, thick, mSize - thick, mSize - thick);
        //颜色放反效果才正常
        SweepGradient gradient = new SweepGradient(mSize / 2, mSize / 2,
                getResources().getColor(R.color.blue_fill), getResources().getColor(R.color.blue_cyan));
        Matrix matrix = new Matrix();
        matrix.setRotate(-90, mSize / 2, mSize / 2);
        gradient.setLocalMatrix(matrix);
        paint.setShader(gradient);
        canvas.drawArc(rectF, -90, -mSweepAngle, false, paint);
    }

    //绘制文字
    private void drawText(Canvas canvas, Paint paint) {
        paint.setColor(mTextColor);
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(mTextSize);
        float textWidth = paint.measureText(formatText());
        float x = mSize / 2 - textWidth / 2;
        Paint.FontMetricsInt metricsInt = paint.getFontMetricsInt();
        int dy = (metricsInt.bottom - metricsInt.top) / 2 - metricsInt.bottom;
        float y = getHeight() / 2 + dy;
        mText = formatText();
        canvas.drawText(mText, x, y, paint);
    }

    //绘制实心圆
    private void drawFillCircle(Canvas canvas, Paint paint) {
        if (mPercent == 0)
            return;
        paint.setColor(mFillCircleColor);
        float x = (float) (mSize / 2 + mRadius * Math.cos((-90 - mSweepAngle) * Math.PI / 180));
        float y = (float) (mSize / 2 + mRadius * Math.sin((-90 - mSweepAngle) * Math.PI / 180));
        float radius = (float) (paint.getStrokeWidth() / 2 * 3);
        canvas.drawCircle(x, y, radius, paint);

        radius = radius + ScreenHelper.dp2Px(getContext(), 1);
        paint.setStrokeWidth(ScreenHelper.dp2Px(getContext(), 1));
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(x, y, radius, paint);
    }

    private String formatText() {
        double time = 1.2 * mPercent;
        String text;
        if (time < 60) {
            if (time < 10)
                text = "0′0" + (int) time + "″";
            else
                text = "0′" + (int) time + "″";
        } else if (time >= 60 && time < 120) {
            int m = (int) (time - 60);
            if (m < 10)
                text = "1′0" + m + "″";
            else
                text = "1′" + m + "″";
        } else {
            text = "2′00″";
        }
        return text;
    }

    /**
     * @param playTime 动画已启动时间
     */
    public void startTiming(long playTime) {
        if (mAnimator == null) {
            mAnimator = ValueAnimator.ofFloat(100, 0);//倒计时
            mAnimator.setDuration(mDuration * 1000);
            mAnimator.setInterpolator(new LinearInterpolator());
            mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mPercent = (float) animation.getAnimatedValue();
                    setPercent(mPercent);
                    if (mListener != null && mPercent == 0) {
                        mListener.onTimingEnd();
                    }
                }
            });
        }
        mAnimator.start();
        if (playTime > 0)
            mAnimator.setCurrentPlayTime(playTime);
    }

    public boolean isTiming() {
        if (mAnimator != null)
            return mAnimator.isRunning();
        return false;
    }

    //暂停动画
    public void pauseTiming() {
        if (mAnimator != null && mAnimator.isRunning()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                mAnimator.pause();
            }
        }
    }

    //恢复动画
    public void resumeTiming() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (mAnimator != null && mAnimator.isPaused())
                mAnimator.resume();
        }
    }

    //停止 结束动画
    public void stopTiming() {
        if (mAnimator != null && mAnimator.isRunning()) {
            mAnimator.end();
            mAnimator.removeAllUpdateListeners();
            mAnimator = null;
        }
    }

    public String getText() {
        return mText;
    }

    public void setText(String text) {
        mText = text;
    }

    public float getPercent() {
        return mPercent;
    }

    public void setPercent(float percent) {
        mPercent = percent;
        invalidate();//重新绘制View
    }

    public float getRadius() {
        return mRadius;
    }

    public void setRadius(float radius) {
        mRadius = radius;
        invalidate();
    }

    public float getSweepAngle() {
        return mSweepAngle;
    }

    public void setSweepAngle(float sweepAngle) {
        mSweepAngle = sweepAngle;
        invalidate();
    }

    public int getFillCircleColor() {
        return mFillCircleColor;
    }

    public void setFillCircleColor(int fillCircleColor) {
        mFillCircleColor = fillCircleColor;
        invalidate();
    }

    public int getTextColor() {
        return mTextColor;
    }

    public void setTextColor(int textColor) {
        mTextColor = textColor;
        invalidate();
    }

    public void setOnTimingListener(OnTimingListener listener) {
        mListener = listener;
    }

    public interface OnTimingListener {
        //倒计时结束
        void onTimingEnd();
    }
}

package com.god.seep.base.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.AttributeSet;
import android.view.View;

import com.god.seep.base.util.ScreenHelper;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

public class ProjectionTextView extends AppCompatTextView {
    private final Paint mPaint;
    private GradientDrawable mProjection;
    private float mRadius;

    public ProjectionTextView(Context context) {
        this(context, null);
    }

    public ProjectionTextView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
        mRadius = ScreenHelper.dp2Px(context, 15);
        mProjection = new GradientDrawable();
        mProjection.setShape(GradientDrawable.OVAL);
        mProjection.setCornerRadius(mRadius + ScreenHelper.dp2Px(getContext(), 1));
        mProjection.setColor(Color.parseColor("#662b2a39"));
    }

    public ProjectionTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setAntiAlias(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//        setMeasuredDimension(measure(widthMeasureSpec), measure(heightMeasureSpec));
        setMeasuredDimension(ScreenHelper.dp2Px(getContext(), 32), ScreenHelper.dp2Px(getContext(), 32));
    }

    private int measure(int measureSpec) {
        int dimension = ScreenHelper.dp2Px(getContext(), 32);
        int mode = View.MeasureSpec.getMode(measureSpec);
        int size = View.MeasureSpec.getSize(measureSpec);
        if (mode == View.MeasureSpec.EXACTLY) {
            dimension = size;
        } else if (mode == View.MeasureSpec.AT_MOST) {
            dimension = size;
        }
        return dimension;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        mPaint.setColor(Color.parseColor("#662b2a39"));
        canvas.drawBitmap(blur(), 0, 0, mPaint);
        @SuppressLint("DrawAllocation")
        RectF rectF = new RectF(0, 0, getMeasuredWidth(), getMeasuredHeight() - ScreenHelper.dp2Px(getContext(), 3));
        mPaint.setColor(Color.WHITE);
        canvas.drawCircle(getWidth() / 2, mRadius, mRadius, mPaint);
        super.onDraw(canvas);
    }

    private Bitmap blur() {
        Bitmap inBitmap = Bitmap.createBitmap(getMeasuredWidth(),
                getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(inBitmap);
        mProjection.setBounds(0, 0, getMeasuredWidth(), getMeasuredHeight());
        mProjection.draw(canvas);
        Bitmap outBitmap = Bitmap.createBitmap(inBitmap);
        RenderScript script = RenderScript.create(getContext());
        ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(script, Element.U8_4(script));
        Allocation tmpIn = Allocation.createFromBitmap(script, inBitmap);
        Allocation tmpOut = Allocation.createFromBitmap(script, outBitmap);
        blur.setRadius(15f);//模糊度
        blur.setInput(tmpIn);
        blur.forEach(tmpOut);
        tmpOut.copyTo(outBitmap);
        inBitmap.recycle();
        script.destroy();
        return outBitmap;
    }
}

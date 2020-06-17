package com.god.seep.base.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.LinearLayout;

import com.god.seep.base.util.ScreenHelper;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

public class CarItemDecoration extends RecyclerView.ItemDecoration {
    public static final int HORIZONTAL = LinearLayout.HORIZONTAL;
    public static final int VERTICAL = LinearLayout.VERTICAL;

    private static final int[] ATTRS = new int[]{android.R.attr.listDivider};

    private Drawable mDivider;
    private int mOrientation;
    private int mStartPx;
    private int mEndPx;

    private Context mContext;
    private final Rect mBounds = new Rect();
    private List<String> mCars;

    public CarItemDecoration(Context context, List<String> cars) {
        this.mContext = context;
        final TypedArray a = context.obtainStyledAttributes(ATTRS);
        mDivider = a.getDrawable(0);
        a.recycle();
        mStartPx = ScreenHelper.dp2Px(context, 0);
        mEndPx = ScreenHelper.dp2Px(context, 0);
        setOrientation(VERTICAL);
        mCars = cars;
    }

    public void setOrientation(int orientation) {
        if (orientation != HORIZONTAL && orientation != VERTICAL) {
            throw new IllegalArgumentException(
                    "Invalid orientation. It should be either HORIZONTAL or VERTICAL");
        }
        mOrientation = orientation;
    }

    public void setDrawable(@NonNull Drawable drawable) {
        if (drawable == null) {
            throw new IllegalArgumentException("Drawable cannot be null.");
        }
        mDivider = drawable;
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        if (parent.getLayoutManager() == null) {
            return;
        }
        if (mOrientation == VERTICAL) {
            drawVertical(c, parent);
        } else {
            drawHorizontal(c, parent);
        }
    }

    @SuppressLint("NewApi")
    private void drawVertical(Canvas canvas, RecyclerView parent) {
        canvas.save();
        final int left;
        final int right;
        if (parent.getClipToPadding()) {
            left = parent.getPaddingLeft() + mStartPx;
            right = parent.getWidth() - parent.getPaddingRight() - mEndPx;
            canvas.clipRect(left, parent.getPaddingTop(), right,
                    parent.getHeight() - parent.getPaddingBottom());
        } else {
            left = mStartPx;
            right = parent.getWidth() - mEndPx;
        }

        final int childCount = parent.getChildCount();  //返回列表中的显示个数，不是item总个数
        for (int i = 0; i < childCount - 1; i++) {
            final View child = parent.getChildAt(i);
            parent.getDecoratedBoundsWithMargins(child, mBounds);
            final int bottom = mBounds.bottom + Math.round(ViewCompat.getTranslationY(child));
            final int top = bottom - mDivider.getIntrinsicHeight();
            mDivider.setBounds(left, top, right, bottom);
            mDivider.draw(canvas);
        }
        canvas.restore();
    }

    /**
     * @return true.应该画线；false，跳过不画
     */
    private boolean shouldDrawLine(int position) {
        if (mCars == null || mCars.size() <= position + 1)
            return false;
        String businessType1 = mCars.get(position);
        String businessType2 = mCars.get(position + 1);
        return !businessType1.equals(businessType2);
    }

    /**
     * 在此处将HORIZONTAL分割线截断为RecyclerView的1/3,作VERTICAL分割线时类似
     */
    @SuppressLint("NewApi")
    private void drawHorizontal(Canvas canvas, RecyclerView parent) {
        canvas.save();
        final int top;
        final int bottom;
        if (parent.getClipToPadding()) {
            top = parent.getPaddingTop() + ScreenHelper.dp2Px(mContext, 10);
            bottom = parent.getHeight() - parent.getPaddingBottom() - ScreenHelper.dp2Px(mContext, 10);
            canvas.clipRect(parent.getPaddingLeft(), top,
                    parent.getWidth() - parent.getPaddingRight(), bottom);
        } else {
            top = parent.getHeight() + ScreenHelper.dp2Px(mContext, 10);
            bottom = parent.getHeight() - ScreenHelper.dp2Px(mContext, 10);
        }

        final int childCount = parent.getChildCount();
        for (int i = 0; i < childCount - 1; i++) {      //当前屏幕内的view
            final View child = parent.getChildAt(i);
            parent.getLayoutManager().getDecoratedBoundsWithMargins(child, mBounds);
            final int right = mBounds.right + Math.round(ViewCompat.getTranslationX(child));
            final int left = right - mDivider.getIntrinsicWidth();
            mDivider.setBounds(left, top, right, bottom);
            mDivider.draw(canvas);
        }
        canvas.restore();
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                               RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view);    //当前View的位置
        int last = state.getItemCount() - 1;    //最后一个Item的位置
        if (mOrientation == VERTICAL) {
            if (position == last)
                outRect.set(0, 0, 0, 0);    //去掉最后一条 divider
            else {
                int bottom;
                if (shouldDrawLine(position))
                    bottom = mDivider.getIntrinsicHeight();
                else
                    bottom = 0;
                outRect.set(0, 0, 0, bottom);
            }
        } else {
            if (position == last)
                outRect.set(0, 0, 0, 0);
            else
                outRect.set(0, 0, mDivider.getIntrinsicWidth(), 0);
        }
    }
}

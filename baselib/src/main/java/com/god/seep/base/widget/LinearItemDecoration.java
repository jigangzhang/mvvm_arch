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

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

/**
 * @description: <p>
 * <p>
 * RecyclerView线性Item分割线
 * </p>
 */

public class LinearItemDecoration extends RecyclerView.ItemDecoration {
    public static final int HORIZONTAL = LinearLayout.HORIZONTAL;
    public static final int VERTICAL = LinearLayout.VERTICAL;

    private static final int[] ATTRS = new int[]{android.R.attr.listDivider};

    private Drawable mDivider;
    private int mOrientation;
    private int mStartPx;
    private int mEndPx;

    private Context mContext;
    private final Rect mBounds = new Rect();

    public LinearItemDecoration(Context context, int orientation, int dpStart, int dpEnd) {
        this.mContext = context;
        final TypedArray a = context.obtainStyledAttributes(ATTRS);
        mDivider = a.getDrawable(0);
        a.recycle();
        mStartPx = ScreenHelper.dp2Px(context, dpStart);
        mEndPx = ScreenHelper.dp2Px(context, dpEnd);
        setOrientation(orientation);
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

        final int childCount = parent.getChildCount();
        for (int i = 0; i < childCount - 1; i++) {        // //当前屏幕内的view，不画最后一条divider
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
        for (int i = 0; i < childCount - 1; i++) {
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
                outRect.set(0, 0, 0, 0);    //去掉最后一条 divider，即使在上面不画最后一条divider，但在这里也会占位，应该去掉占位
            else
                outRect.set(0, 0, 0, mDivider.getIntrinsicHeight());
        } else {
            if (position == last)
                outRect.set(0, 0, 0, 0);
            else
                outRect.set(0, 0, mDivider.getIntrinsicWidth(), 0);
        }
    }
}

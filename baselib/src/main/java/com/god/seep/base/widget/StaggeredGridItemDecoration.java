package com.god.seep.base.widget;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.god.seep.base.util.ScreenHelper;

/**
 * 瀑布流item分割线，瀑布流以高度差确定位置
 */
public class StaggeredGridItemDecoration extends RecyclerView.ItemDecoration {
    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
//        int position = ((RecyclerView.LayoutParams) view.getLayoutParams()).getViewLayoutPosition();
        StaggeredGridLayoutManager.LayoutParams lp = (StaggeredGridLayoutManager.LayoutParams) view.getLayoutParams();
        int position = lp.getSpanIndex();       //实际顺序，待对比
        int width = 0;
        if (position % 2 == 0)
            width = ScreenHelper.dp2Px(view.getContext(), 10);
        outRect.set(0, 0, width, ScreenHelper.dp2Px(view.getContext(), 10));
    }
}

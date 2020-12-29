package com.god.seep.base.widget;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.god.seep.base.util.ScreenHelper;

public class GridItemDecoration extends RecyclerView.ItemDecoration {
    private float width;  //offset宽度，dp
    private float height; //offset高度，dp

    public GridItemDecoration(float width, float height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int position = ((RecyclerView.LayoutParams) view.getLayoutParams()).getViewLayoutPosition();
        int width = 0;
        int spanCount = 1;
        if (parent.getLayoutManager() instanceof GridLayoutManager)
            spanCount = ((GridLayoutManager) parent.getLayoutManager()).getSpanCount();
        if (position % spanCount == 0)
            width = ScreenHelper.dp2Px(view.getContext(), this.width);
        int height = ScreenHelper.dp2Px(view.getContext(), this.height);
        outRect.set(0, 0, width, height);
    }
}

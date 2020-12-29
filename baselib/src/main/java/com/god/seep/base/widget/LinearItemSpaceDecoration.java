package com.god.seep.base.widget;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.god.seep.base.util.ScreenHelper;

public class LinearItemSpaceDecoration extends RecyclerView.ItemDecoration {
    private float offset;   //offset dp值

    public LinearItemSpaceDecoration(float offset) {
        this.offset = offset;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int offset = ScreenHelper.dp2Px(view.getContext(), this.offset);
        RecyclerView.LayoutManager layoutManager = parent.getLayoutManager();
        int width = 0;
        int height = 0;
        if (layoutManager instanceof LinearLayoutManager) {
            LinearLayoutManager manager = (LinearLayoutManager) layoutManager;
            int orientation = manager.getOrientation();
            if (orientation == LinearLayoutManager.VERTICAL) {
                width = 0;
                height = offset;
            } else {
                width = offset;
                height = 0;
            }
        }
        outRect.set(0, 0, width, height);
    }
}

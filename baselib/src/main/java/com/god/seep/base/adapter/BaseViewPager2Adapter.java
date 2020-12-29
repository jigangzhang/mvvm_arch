package com.god.seep.base.adapter;

import androidx.databinding.ViewDataBinding;

public abstract class BaseViewPager2Adapter<D extends ViewDataBinding, T> extends BaseRecyclerViewAdapter<D, T> {

    public BaseViewPager2Adapter(int layoutResId) {
        super(layoutResId);
    }
}

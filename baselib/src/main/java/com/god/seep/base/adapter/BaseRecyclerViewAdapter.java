package com.god.seep.base.adapter;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;

public abstract class BaseRecyclerViewAdapter<D extends ViewDataBinding, T> extends BaseQuickAdapter<T, BaseViewHolder> {


    public BaseRecyclerViewAdapter(int layoutResId) {
        super(layoutResId);
    }

    @Override
    protected void convert(BaseViewHolder helper, T item) {
        D binding = DataBindingUtil.bind(helper.itemView);
        if (binding != null) {
            bindItem(binding, item);
            binding.executePendingBindings();
        }
    }

    protected abstract void bindItem(D binding, T item);
}

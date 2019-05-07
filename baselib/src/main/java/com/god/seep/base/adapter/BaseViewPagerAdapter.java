package com.god.seep.base.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import java.util.List;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.viewpager.widget.PagerAdapter;

public abstract class BaseViewPagerAdapter<D extends ViewDataBinding, T> extends PagerAdapter {
    private List<T> dataList;
    private int resId;

    public BaseViewPagerAdapter(@LayoutRes int resId, @NonNull List<T> dataList) {
        this.resId = resId;
        this.dataList = dataList;
    }

    @Override
    public int getCount() {
        return dataList.size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        D binding = DataBindingUtil.inflate(LayoutInflater.from(container.getContext()), resId, container, false);
        bindData(binding, dataList.get(position));
        return binding.getRoot();
    }

    protected abstract void bindData(D binding, T data);

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
    }
}

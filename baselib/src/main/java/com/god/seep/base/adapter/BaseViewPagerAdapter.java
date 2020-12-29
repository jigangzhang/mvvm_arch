package com.god.seep.base.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import java.util.ArrayList;
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

    public List<T> getData() {
        if (dataList == null)
            dataList = new ArrayList<>();
        return dataList;
    }

    @Override
    public int getCount() {
        return dataList == null ? 0 : dataList.size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        View view = DataBindingUtil.inflate(LayoutInflater.from(container.getContext()), resId, container, false).getRoot();
        D binding = DataBindingUtil.bind(view);
        container.addView(view);
        bindData(binding, dataList.get(position));
        return view;
    }

    @Override
    public void setPrimaryItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
//        bindData(DataBindingUtil.getBinding((View) object), dataList.get(position));
    }

    protected abstract void bindData(D binding, T data);

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        DataBindingUtil.getBinding((View) object).unbind();
        container.removeView((View) object);
    }
}
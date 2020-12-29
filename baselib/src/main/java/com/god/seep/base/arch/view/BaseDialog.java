package com.god.seep.base.arch.view;

import android.app.Dialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.god.seep.base.R;
import com.god.seep.base.arch.viewmodel.BaseViewModel;

public abstract class BaseDialog<D extends ViewDataBinding> extends DialogFragment implements IView<BaseViewModel> {
    protected D mBinding;
    private int mGravity = Gravity.CENTER;
    private int mWidth = WindowManager.LayoutParams.MATCH_PARENT;
    private float mHorizontalMargin = 0;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return super.onCreateDialog(savedInstanceState);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_TITLE, 0);
    }

    @Override
    public BaseViewModel createViewModel() {
        return null;
    }

    @Override
    public void registerEvent() {

    }

    @Override
    public void loginInvalid(String errCode) {

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view;
        if (getLayoutId() == 0) {
            view = super.onCreateView(inflater, container, savedInstanceState);
        } else {
            mBinding = DataBindingUtil.inflate(inflater, getLayoutId(), container, false);
            view = mBinding.getRoot();
        }
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initData();
        //setContentView 是在onActivityCreated中调用的，所以对window的操作放在这里
        setupDialog();
    }

    public void setGravity(int gravity) {
        mGravity = gravity;
    }

    protected void setWindowGravity(int gravity) {
        Window window = getDialog().getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.gravity = gravity;
        window.setAttributes(lp);
    }

    /**
     * 在 setupDialog 之前调用才有作用
     */
    public void setWidth(int width) {
        mWidth = width;
    }

    public void setHorizontalMargin(float margin) {
        mHorizontalMargin = margin;
    }

    private void setupDialog() {
        getDialog();
        Window window = getDialog().getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.width = mWidth;
        lp.gravity = mGravity;
        lp.horizontalMargin = mHorizontalMargin;
        window.setAttributes(lp);
        window.setBackgroundDrawableResource(R.color.transparent);
        window.getDecorView().setPadding(0, 0, 0, 0);
//        window.setDimAmount(0.5f);    //0-1，背景暗度
//        window.setWindowAnimations();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mBinding != null) {
            mBinding.unbind();
            mBinding = null;
        }
    }

    public void show(@NonNull FragmentManager manager) {
        super.show(manager, BaseDialog.class.getName());
    }
}

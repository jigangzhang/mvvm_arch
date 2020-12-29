package com.god.seep.base.ui.image_video;

import android.view.View;

import androidx.viewpager2.widget.ViewPager2;

import com.god.seep.base.R;
import com.god.seep.base.arch.view.BaseActivity;
import com.god.seep.base.arch.viewmodel.BaseViewModel;
import com.god.seep.base.bean.ContentInfo;
import com.god.seep.base.databinding.ActivityPagingImageVideoBinding;

import java.util.ArrayList;

public class PagingImageVideoActivity extends BaseActivity<ActivityPagingImageVideoBinding, BaseViewModel> {
    private ImageVideoAdapter adapter;

    @Override
    public int getLayoutId() {
        return R.layout.activity_paging_image_video;
    }

    @Override
    public BaseViewModel createViewModel() {
        return null;
    }

    @Override
    public void initData() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        ArrayList<ContentInfo> list = getIntent().getParcelableArrayListExtra("key_content_list");
        adapter = new ImageVideoAdapter(true);
        adapter.setNewInstance(list);
        adapter.addChildClickViewIds(R.id.exo_zoom, R.id.image);
        adapter.setOnItemChildClickListener((adapter1, view, position) -> finish());
        adapter.setZoomListener(this::finish);
        adapter.setOnItemClickListener((adapter12, view, position) -> finish());
        mBinding.viewPager.setAdapter(adapter);
        mBinding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                adapter.selectPage(position);
            }
        });
        if (adapter.getData().size() > 0)
            mBinding.viewPager.setOffscreenPageLimit(adapter.getData().size());
    }

    @Override
    public void registerEvent() {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adapter != null)
            adapter.recyclePlayer();
    }
}
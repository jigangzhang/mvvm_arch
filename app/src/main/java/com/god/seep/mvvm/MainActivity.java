package com.god.seep.mvvm;

import com.god.seep.base.adapter.BaseRecyclerViewAdapter;
import com.god.seep.base.arch.view.BaseActivity;
import com.god.seep.media.ui.main.MediaFragment;
import com.god.seep.mvvm.databinding.ActivityMainBinding;
import com.god.seep.mvvm.databinding.ItemChapterBinding;

import android.view.Menu;
import android.view.MenuItem;

import java.util.List;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

public class MainActivity extends BaseActivity<ActivityMainBinding, MainViewModel> {

    @Override
    public int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    public MainViewModel createViewModel() {
        return getViewModel(MainViewModel.class);
//        return new MainViewModel(this.getApplication());
    }

    BaseRecyclerViewAdapter adapter = new BaseRecyclerViewAdapter<ItemChapterBinding, Chapter>(R.layout.item_chapter) {
        @Override
        protected void bindItem(ItemChapterBinding binding, Chapter item) {
            binding.setChapter(item);
        }
    };

    @Override
    public void initData() {
        mBinding.setViewModel(mViewModel);
        setSupportActionBar(mBinding.toolbar);
        mBinding.list.setAdapter(adapter);
        //直接實例化Fragment
        MediaFragment.Companion.newInstance();
        mViewModel.getChapterListEvent().observe(this, new Observer<List<Chapter>>() {
            @Override
            public void onChanged(List<Chapter> chapters) {
                adapter.addData(chapters);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

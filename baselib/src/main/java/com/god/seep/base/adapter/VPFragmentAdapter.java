package com.god.seep.base.adapter;

import java.util.List;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

/**
 * FragmentStatePagerAdapter: 当有大量页面时，这个版本的 pager 更有用，工作起来更像一个列表视图。当页面对用户不可见时，它们的整个片段可能被销毁，
 * 只保留该片段的保存状态。
 * 与FragmentPagerAdapter相比，这允许寻呼机占用与每个访问页面关联的内存少得多，但在页面之间切换时可能会增加开销。
 * <p>
 * FragmentPagerAdapter: 该版本的 pager 最适合在需要翻页的静态片段(比如一组选项卡)很少的情况下使用。用户访问的每个页面的片段都将保存在内存中，
 * 尽管它的视图层次结构可能在不可见时被销毁。这可能导致使用大量内存，因为fragment实例可以保持任意数量的状态。
 * 对于较大的页面集，请考虑FragmentStatePagerAdapter。
 */
public class VPFragmentAdapter extends FragmentPagerAdapter {
    private List<Fragment> fragments;

    public VPFragmentAdapter(FragmentManager fm, List<Fragment> fragments) {
        super(fm);
        this.fragments = fragments;
    }

    @Override
    public Fragment getItem(int position) {
        return fragments == null ? null : fragments.get(position);
    }

    @Override
    public int getCount() {
        return fragments == null ? 0 : fragments.size();
    }
}

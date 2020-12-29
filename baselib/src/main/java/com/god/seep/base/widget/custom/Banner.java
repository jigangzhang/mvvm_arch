package com.god.seep.base.widget.custom;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.god.seep.base.R;
import com.god.seep.base.adapter.BaseViewPagerAdapter;
import com.god.seep.base.util.ImageLoader;
import com.god.seep.base.util.ScreenHelper;

import java.util.List;

public class Banner extends FrameLayout {
    private static final int MSG_BANNER = 0x001;
    private boolean autoPlay;
    private boolean showIndicator;
    private boolean switchEnabled = true;
    private int duration; //轮播时间
    private ViewPager viewPager;
    private CustomIndicatorView indicatorView;
    private List<String> contents;
    private OnBannerItemListener listener;
    private PagerAdapter adapter;

    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == MSG_BANNER && contents != null) {
                int currentItem = viewPager.getCurrentItem();
                int next = currentItem + 1;
                if (next == contents.size())
                    next = 0;
                viewPager.setCurrentItem(next, next != 0);
                this.sendEmptyMessageDelayed(MSG_BANNER, duration * 1000);
            }
        }
    };

    public Banner(@NonNull Context context) {
        this(context, null);
    }

    public Banner(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Banner(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.Banner, defStyleAttr, 0);
        autoPlay = typedArray.getBoolean(R.styleable.Banner_autoPlay, true);
        duration = typedArray.getInt(R.styleable.Banner_playDuration, 3);
        showIndicator = typedArray.getBoolean(R.styleable.Banner_showIndicator, true);
        typedArray.recycle();
        init();
    }

    private void init() {
        viewPager = new ViewPager(getContext());
        LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        viewPager.setLayoutParams(lp);
//        initViewPager();
        addView(viewPager);
        if (showIndicator) {
            indicatorView = new CustomIndicatorView(getContext());
            initIndicator();
            addView(indicatorView);
        }
    }

    public void setBannerListener(OnBannerItemListener listener) {
        this.listener = listener;
    }

    private void initViewPager() {
        if (this.adapter == null)
            adapter = new PagerAdapter() {
                @Override
                public int getCount() {
                    return contents.size();
                }

                @NonNull
                @Override
                public Object instantiateItem(@NonNull ViewGroup container, int position) {
                    ImageView imageView = new ImageView(container.getContext());
                    ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                    imageView.setLayoutParams(lp);
                    imageView.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onItemClick(position);
                        }
                    });
                    ImageLoader.loadImage(getContext(), contents.get(position), imageView, true);
                    container.addView(imageView);
                    return imageView;
                }

                @Override
                public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
                    container.removeView((View) object);
                }

                @Override
                public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
                    return view == object;
                }
            };
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(contents.size());
        if (indicatorView != null)
            indicatorView.setSize(contents.size());
        viewPager.setCurrentItem(0, true);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (indicatorView != null)
                    indicatorView.setCurrentIndicator(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    private void initIndicator() {
        if (indicatorView == null || contents == null) return;
        int minHeight = ScreenHelper.dp2Px(getContext(), 12);   //直径+padding高度
        int minWidth = ScreenHelper.dp2Px(getContext(), 20 + 6 * contents.size() + 5 * (contents.size() - 1));  //直径+间隔+padding
        LayoutParams lp = new LayoutParams(minWidth, minHeight);
        lp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        lp.bottomMargin = ScreenHelper.dp2Px(getContext(), 10);
        indicatorView.setLayoutParams(lp);
        int horizontalPadding = ScreenHelper.dp2Px(getContext(), 10);
        int verticalPadding = ScreenHelper.dp2Px(getContext(), 3);
        indicatorView.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);
        indicatorView.setBackgroundResource(R.color.colorAccent);
        indicatorView.setRadius(ScreenHelper.dp2Px(getContext(), 3));
        indicatorView.setIndicatorSpace(ScreenHelper.dp2Px(getContext(), 5));
        indicatorView.setSelectedColor(getContext().getResources().getColor(R.color.black_1b));
        indicatorView.setUnselectedColor(getContext().getResources().getColor(R.color.white));
    }

    public void setAutoPlay(boolean autoPlay) {
        this.autoPlay = autoPlay;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public void setContents(List<String> contents) {
        this.contents = contents;
        if (contents == null) return;
        initViewPager();
        initIndicator();
    }

    public void setAdapter(BaseViewPagerAdapter adapter) {
        if (adapter == null) return;
        this.adapter = adapter;
        if (contents == null) {
            contents = adapter.getData();
        }
        if (contents != null) {
            initViewPager();
            initIndicator();
        }
    }

    /**
     * 会覆盖事件，多个事件的情况下不可用
     */
    public void setScrollParent(NestedScrollView scrollView) {
        if (autoPlay && scrollView != null) {
            scrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
                @Override
                public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                    if (handler == null) return;
                    Rect rect = new Rect();
                    scrollView.getHitRect(rect);
                    boolean visible = getLocalVisibleRect(rect);
                    if (visible) {
                        if (!handler.hasMessages(MSG_BANNER))
                            handler.sendEmptyMessageDelayed(MSG_BANNER, duration * 1000);
                    } else {
                        handler.removeMessages(MSG_BANNER);
                    }
                }
            });
        }
    }

    public ViewPager getViewPager() {
        return viewPager;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (viewPager != null && autoPlay) {
                    switchEnabled = false;
                    handler.removeMessages(MSG_BANNER);
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (viewPager != null && autoPlay) {
                    handler.sendEmptyMessageDelayed(MSG_BANNER, duration * 1000);
                    switchEnabled = true;
                }
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        handleVisible(visibility == VISIBLE);
    }

    public void handleVisible(boolean visible) {
        if (handler == null) {
            return;
        }
        if (visible && autoPlay) {
            if (!handler.hasMessages(MSG_BANNER) && switchEnabled) {
                handler.sendEmptyMessageDelayed(MSG_BANNER, duration * 1000);
            }
        } else {
            handler.removeMessages(MSG_BANNER);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        handler.removeMessages(MSG_BANNER);
    }

    public interface OnBannerItemListener {
        void onItemClick(int position);
    }
}

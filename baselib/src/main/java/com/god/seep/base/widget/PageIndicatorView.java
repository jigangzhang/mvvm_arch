package com.god.seep.base.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import com.god.seep.base.R;
import com.god.seep.base.util.ScreenHelper;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


/**
 * @company: 甘肃诚诚网络技术有限公司
 * @project: ymyc_customer_4.0
 * @author: zhangjigang
 * @date: 2019/11/22 14:15
 * @description:用于列表翻页时的页码指示器
 */
public class PageIndicatorView extends View {
    private static final int PageItemSize = 3;      //每页中的Item个数，默认3个
    private static final int ScrollLeft = 0x00;
    private static final int ScrollRight = 0x01;
    private RecyclerView mRecyclerView;
    private Paint mPaint;
    private RectF mRectF;
    private float mIndicatorWidth;
    private float mIndicatorHeight;
    private float mIndicatorMargin;
    private float mOutIndicatorWidth;   //外层Indicator的宽度
    private float mInnerIndicatorWidth;   //内层Indicator的宽度
    private float mCornerRadius;    //圆角半径
    private float mInnerOffset; //内层Indicator的滑动距离
    private int mPageItemSize;
    private int mNormalColor;
    private int mSelectedColor;
    private int scrollDirection;    //暂为左右滑动

    public PageIndicatorView(Context context) {
        this(context, null);
    }

    public PageIndicatorView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PageIndicatorView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.PageIndicatorView, defStyleAttr, 0);
        mPageItemSize = typedArray.getInteger(R.styleable.PageIndicatorView_pageItemSize, PageItemSize);
        mIndicatorMargin = typedArray.getDimension(R.styleable.PageIndicatorView_indicator_margin, ScreenHelper.dp2Px(context, 6));
        mIndicatorWidth = typedArray.getDimension(R.styleable.PageIndicatorView_indicator_width, ScreenHelper.dp2Px(context, 12));
        mIndicatorHeight = typedArray.getDimension(R.styleable.PageIndicatorView_indicator_height, ScreenHelper.dp2Px(context, 3));
        mOutIndicatorWidth = typedArray.getDimension(R.styleable.PageIndicatorView_out_indicator_width, ScreenHelper.dp2Px(context, 30));
        mNormalColor = typedArray.getColor(R.styleable.PageIndicatorView_indicatorNormalColor, context.getResources().getColor(R.color.gray_light));
        mSelectedColor = typedArray.getColor(R.styleable.PageIndicatorView_indicatorSelectedColor, context.getResources().getColor(R.color.blue_accent));
        typedArray.recycle();
        mInnerIndicatorWidth = mOutIndicatorWidth / 2 + ScreenHelper.dp2Px(getContext(), 2);
        mCornerRadius = ScreenHelper.dp2Px(context, 1.5f);
        mRectF = new RectF();
        initPaint();
    }

    private void initPaint() {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);  //抗锯齿？
    }

    /**
     * 暂时只与RecyclerView做联动，必须在给RecyclerView setAdapter后再调用该方法
     */
    public void setupWithRecyclerView(@NonNull RecyclerView recyclerView) {
        mRecyclerView = recyclerView;
        if (getItemCount() <= 3)
            setVisibility(GONE);
        else
            setVisibility(VISIBLE);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
//                RecyclerView.SCROLL_STATE_IDLE    //空闲
//                RecyclerView.SCROLL_STATE_DRAGGING  //拖动
//                RecyclerView.SCROLL_STATE_SETTLING  //拖动到底了，还在拖动
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
//                int extent = mRecyclerView.computeHorizontalScrollExtent(); //RecyclerView的宽度
                int offset = mRecyclerView.computeHorizontalScrollOffset(); //滑动距离
                if (dx > 0) {
                    scrollDirection = ScrollRight;
                } else {
                    scrollDirection = ScrollLeft;
                }
                mInnerOffset = offset / computeRate();
                invalidate();
                //if (getPageCount() <= 1) return;        //只有大于1页时，才显示下标切换效果
                //获取RecyclerView当前顶部显示的第一个条目对应的索引
                //position = mRecyclerView.getLayoutManager().findFirstVisibleItemPosition();
                //根据索引来获取对应的itemView
                //View firstVisiableChildView = mMTrainLinearLayoutManager.findViewByPosition(position);
                //获取当前显示条目的高度
                //itemHeight = firstVisiableChildView.getHeight();
                //获取当前Recyclerview 偏移量
                //flag = (position) * itemHeight - firstVisiableChildView.getTop();
                //根据当前显示的第一个View在列表中的位置计算出inner x的坐标
                //滑动时，根据剩余的item数（即剩余要滑动的距离）和offset计算inner x坐标
                //计算出当前选中页，滑动方向
                //最后一页不足最大Item数时的下标切换效果--最后一页与前一页间切换时考虑要这两页的已见Item数--切换页的滑动过半时强制翻页？
            }
        });
    }

    int ItemOffset = 4;   //列表中的divider的宽或高度

    private int computeRemainWidth() {
        if (mRecyclerView != null) {
            int itemWidth = (mRecyclerView.computeHorizontalScrollExtent() - ScreenHelper.dp2Px(getContext(), ItemOffset * 2)) / 3;
            int count = getItemCount();
            int remainCount = count - 3;
            if (remainCount > 0) {
                return remainCount * (itemWidth + ScreenHelper.dp2Px(getContext(), ItemOffset));
            }
        }
        return 0;
    }

    private float computeRate() {
        return computeRemainWidth() / (mOutIndicatorWidth - mInnerIndicatorWidth);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mRecyclerView != null)
            mRecyclerView.clearOnScrollListeners();
    }

    public void marginBottom(float dp_bottom) {
        if (getItemCount() <= 3) return;
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) getLayoutParams();
        lp.setMargins(lp.leftMargin, lp.topMargin, lp.rightMargin, ScreenHelper.dp2Px(getContext(), dp_bottom));
        setLayoutParams(lp);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawScrollIndicator(canvas);

//        int count = getPageCount();
//        if (count <= 0) return;
//        float startX = getStartX(count);
//        float startY = (getMeasuredHeight() - mIndicatorHeight) / 2;
//        mPaint.setColor(mNormalColor);
//        for (int position = 0; position < count; position++) {
//            float left = (int) (startX + mIndicatorWidth * position + mIndicatorMargin * position);
//            float top = startY;
//            float right = left + mIndicatorWidth;
//            float bottom = startY + mIndicatorHeight;
//            mRectF.set(left, top, right, bottom);
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                canvas.drawRoundRect(mRectF, mCornerRadius, mCornerRadius, mPaint);
//            } else {
//                canvas.drawRect(mRectF, mPaint);
//            }
//        }
//        mPaint.setColor(mSelectedColor);
//        //计算出当前页和滑动方向
//        calculateStartX();
//        calculateEndX();
    }


    /**
     * 绘制单个Indicator中的indicator滑动效果
     */
    private void drawScrollIndicator(Canvas canvas) {
        float innerX;
        float outX = (getMeasuredWidth() - mOutIndicatorWidth) / 2;
        float outY = (getMeasuredHeight() - mIndicatorHeight) / 2;
        mRectF.set(outX, outY, outX + mOutIndicatorWidth, outY + mIndicatorHeight);
        mPaint.setColor(mNormalColor);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            canvas.drawRoundRect(mRectF, mCornerRadius, mCornerRadius, mPaint);     //外层Indicator，考虑替换为Drawable？
        } else {
            canvas.drawRect(mRectF, mPaint);
        }
        //根据滑动计算inner x起点坐标值
        innerX = outX + mInnerOffset;
        mRectF.set(innerX, outY, innerX + mInnerIndicatorWidth, outY + mIndicatorHeight);
        mPaint.setColor(mSelectedColor);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            canvas.drawRoundRect(mRectF, mCornerRadius, mCornerRadius, mPaint);     //外层Indicator
        } else {
            canvas.drawRect(mRectF, mPaint);
        }
    }

    /**
     * 列表中的所有item数
     */
    private int getItemCount() {
        if (mRecyclerView != null) {
            RecyclerView.Adapter adapter = mRecyclerView.getAdapter();
            if (adapter != null) {
                return adapter.getItemCount();
            }
        }
        return 0;
    }

    /**
     * 分页，默认为3个一页
     */
    private int getPageCount() {
        int count = getItemCount();
        if (count > 0)
            return (count - 1) / mPageItemSize + 1;
        return 0;   //测试，正式返回0
    }

    /**
     * 计算第一个下标的起始x坐标，用于多个Indicator时计算每个下标的x坐标起点
     */
    private float getStartX(int count) {
        if (count <= 0) return 0;
        return (getMeasuredWidth() - mIndicatorWidth - mIndicatorMargin * (count - 1)) / 2;
    }

    /**
     * 计算页面切换时位于左边选中状态下标的x坐标值
     */
    private void calculateStartX() {

    }

    /**
     * 计算页面切换时位于右边选中状态下标的x坐标值
     */
    private void calculateEndX() {

    }
}

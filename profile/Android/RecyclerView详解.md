
## RecyclerView详解

    ClipToPadding：true，使子View绘制到padding区域
    RecyclerView.getChildCount()：返回RecyclerView中所有item个数，可见个数使用：LayoutManager.getChildCount，getChildAt同
        getChildViewHolderInt，获取view对应的ViewHolder
    要使列表某一项滑动至list顶部使用下面：
       ((LinearLayoutManager) mBinding.list.getLayoutManager()).scrollToPositionWithOffset(i, 0);    
        list.scrollToPosition(i)，只是使某一项滑动至可见
    Adapter.setHasStableIds，指示数据集中的每个item是否可以用Long类型的惟一标识符表示，hasStableIds为true时，在缓存回收时会跳过item
    Adapter.getItemId，与setHasStableIds对应，设为true时，该项必须重写，否则所有项都使用同一个id
    
    ViewHolder获取步骤：先从mChangedScrap（有动画时可能会），然后是mAttachedScrap、mChildHelper.mHiddenViews，
            接着是mCachedViews，若stableIds为true，则又会从mAttachedScrap、mCachedViews中查找一次（根据id），
            再是mViewCacheExtension，再然后是RecycledViewPool，最后是createViewHolder 创建

#### 数据加载流程

    使用RecyclerView时，一般都是在Adapter.setData后才使数据显示，而实际生效的是：Adapter.notifyDataSetChanged、notifyItemChanged这些方法
    调用notifyDataSetChanged 这些方法后，最终会调用到ViewRootImpl.requestLayout，最终触发scheduleTraversals，以及Choreographer的doFrame（帧），垂直同步等
    Adapter中的大多数方法都是发生在LayoutManager.onLayoutChildren阶段的
    
    Adapter：RecyclerView的内部类
        notifyDataSetChanged：通知所有注册的观察者数据集已更改，会调用mObservable.notifyChanged()
        notifyItemRangeChanged：通知指定区间的数据集更改，调用mObservable.notifyItemRangeChanged()
        registerAdapterDataObserver：mObservable.registerObserver，观察者类型是AdapterDataObserver
    
    AdapterDataObservable：是上面的 mObservable 对应的类（被观察者）
        notifyChanged：遍历mObservers，调用其onChanged()方法，mObservers.get(i).onChanged()
        notifyItemRangeChanged：同上，调用的是onItemRangeChanged方法
        mObservers是Adapter中注册的，元素类型是AdapterDataObserver
    
    AdapterDataObserver：Adapter内容更改的观察者
    RecyclerViewDataObserver：AdapterDataObserver的实现类
        onChanged：
            mState.mStructureChanged = true;        //在dispatchLayoutStep 中使用，layout阶段使用
            processDataSetCompletelyChanged(true);  //使集合中所有数据无效，回收缓存等
            if (!mAdapterHelper.hasPendingUpdates()) 
                requestLayout();        //触发layout
        onItemRangeChanged：
            if (mAdapterHelper.onItemRangeChanged(positionStart, itemCount, payload)) 
                triggerUpdateProcessor();
        triggerUpdateProcessor：
            if (POST_UPDATES_ON_ANIMATION && mHasFixedSize && mIsAttached) {
                ViewCompat.postOnAnimation(RecyclerView.this, mUpdateChildViewsRunnable);
            } else {
                mAdapterUpdateDuringMeasure = true;
                requestLayout();        //触发layout
            }
            
    
    RecyclerView：
        processDataSetCompletelyChanged：处理数据集已经完全改变
            一旦布局发生，所有附加的项目应该被丢弃或动画
            附加的项目被标记为无效
            由于items仍然可能在“数据集完全更改”事件和布局事件之间预取，所以所有缓存的item将被丢弃
            dispatchItemsChanged，是否在measure/layout过程中调用RecyclerView.layoutManager.onItemsChanged(RecyclerView)
            会调用markKnownViewsInvalid
        markKnownViewsInvalid：将所有已知的View标记为无效。用于响应“整个世界可能已经改变”的数据改变事件
            关键点有：ViewHolder holder = getChildViewHolderInt(mChildHelper.getUnfilteredChildAt(i))    //实际是RecyclerView.getChildAt
                if (holder != null && !holder.shouldIgnore()) {         //FLAG_IGNORE，这个ViewHolder完全由LayoutManager管理。我们不会废弃，回收或删除它，除非LayoutManager被替换。LayoutManager仍然完全可以看到它
                    holder.addFlags(ViewHolder.FLAG_UPDATE | ViewHolder.FLAG_INVALID);      //标记无效
                }
                mRecycler.markKnownViewsInvalid();  //与缓存有关，使缓存无效，回收view
                Recycler是缓存相关的内部类
        dispatchLayout:
            Adapter数据更新后，会触发layout，最终会调用到该方法
            dispatchLayoutStep1，State.STEP_START时调用，STEP_START在dispatchLayoutStep3、State.prepareForNestedPrefetch中被设置（也是mLayoutStep的初始值）
            dispatchLayoutStep2，在dispatchLayoutStep1之后调用，或者在尺寸改变后或有要更新的item时调用
            dispatchLayoutStep3，最后一定会调用
        dispatchLayoutStep1：
            layout的第一步：处理Adapter更新；决定应该运行哪个动画；保存当前views的信息；如果需要，预测布局并保存其信息
            startInterceptRequestLayout，拦截其他的request请求
            processAdapterUpdatesAndSetAnimationFlags，处理Adapter更新和动画设置，会调用到LayoutManager.onItemsChanged，mItemAnimator和hasStableIds与动画有关
            saveFocusInfo，当前焦点item，暂存与item对应的ViewHolder的相关信息（包含hasStableIds）
            holder.shouldIgnore() || (holder.isInvalid() && !mAdapter.hasStableIds()时，不会运行动画
            if (mState.mRunPredictiveAnimations) {      //预测动画
                mLayout.onLayoutChildren(mRecycler, mState);    //在LayoutManager中对各item进行layout
            }
            clearOldPositions，stopInterceptRequestLayout，与上面对应
            mState.mLayoutStep = State.STEP_LAYOUT
        dispatchLayoutStep2：
            layout的第二步，我们对最终状态的views进行实际布局。如果需要，此步骤可能会运行多次(例如measure)
            startInterceptRequestLayout，拦截其他request
            onEnterLayoutOrScroll，
            mLayout.onLayoutChildren(mRecycler, mState);    //通过LayoutManager对item进行layout
            mState.mRunSimpleAnimations = mState.mRunSimpleAnimations && mItemAnimator != null;   //onLayoutChildren可能导致客户端代码禁用项目动画；重新检查
            mState.mLayoutStep = State.STEP_ANIMATIONS;
            onExitLayoutOrScroll();
            stopInterceptRequestLayout(false)，对应
        dispatchLayoutStep3：
            layout的最后一步，保存views的动画信息，触发动画并进行必要的清理
            mState.mLayoutStep = State.STEP_START，步骤状态
            if (mState.mRunSimpleAnimations)，触发动画，animateChange
            mViewInfoStore.process(mViewInfoProcessCallback)，处理view信息列表和触发动画
            mLayout.removeAndRecycleScrapInt(mRecycler)，LayoutManager的方法，回收mAttachedScrap列表中的view，
                若Detached则removeDetachedView，停止动画，最后将scrap列表中的view添加到mCachedViews列表中，或添加到mRecyclerPool，或直接清除
            mRecycler.mChangedScrap.clear()，清理缓存列表（废弃的）
            mRecycler.updateViewCacheSize，可能会更新缓存大小（mCachedViews），addViewHolderToRecycledViewPool
            mLayout.onLayoutCompleted，LayoutManager在完整的布局计算完成后调用，这是LayoutManager做一些清理的好地方，比如挂起的滚动位置，保存的状态等等
            dispatchOnScrolled，可能会触发，从而调用到onScrollChanged、onScrolled、ScrollListener.onScrolled等
        onMeasure：
            defaultOnMeasure，
            if(mLayout.isAutoMeasureEnabled())，默认false，LinearLayoutManager中为true
                mLayout.onMeasure，内部又调用RecyclerView.defaultOnMeasure
                dispatchLayoutStep1，
                mLayout.setMeasureSpecs，
                dispatchLayoutStep2，
                mLayout.setMeasuredDimensionFromChildren，从item获取测量的宽高
                if (mLayout.shouldMeasureTwice())，RecyclerView是non-exact的宽高，并且至少1个子view也是non-exact的宽高，则测量两次，重复上面3项
            else，   默认情况下
                if (mHasFixedSize) {        //固定大小
                   mLayout.onMeasure(mRecycler, mState, widthSpec, heightSpec);
                   return;
                }
                processAdapterUpdatesAndSetAnimationFlags，
                ...
                mLayout.onMeasure，
                ...
        draw：
            mItemDecorations.get(i).onDrawOver，绘制分割线
            ...
        onDraw：
            mItemDecorations.get(i).onDraw，绘制分割线（上面的绘制已过时）

#### 缓存

    Recycler：RecyclerView内部类，负责管理废弃的（scrapped）或分离的（detached）item view，以便重用
        scrapped view是一个仍然attach到它的RecyclerView的view，但是已经被标记为删除或重用
        ViewHolder获取步骤：先从mChangedScrap（有动画时可能会），然后是mAttachedScrap、mChildHelper.mHiddenViews，
            接着是mCachedViews，若stableIds为true，则又会从mAttachedScrap、mCachedViews中查找一次（根据id），
            再是mViewCacheExtension，再然后是RecycledViewPool，最后是createViewHolder 创建
        
        ArrayList<ViewHolder> mAttachedScrap，将要废弃的缓存？在 scrapView 中有添加操作
        ArrayList<ViewHolder> mChangedScrap，预先布局相关的缓存？hasStableIds为true时，ViewHolder也会保存到该列表中，在 scrapView 中有添加操作
        ArrayList<ViewHolder> mCachedViews， 在recycleViewHolderInternal 中有添加操作
        RecycledViewPool mRecyclerPool，     addViewHolderToRecycledViewPool，在recycleViewHolderInternal中若没有添加到mCachedViews则添加到mRecyclerPool
        ViewCacheExtension mViewCacheExtension，扩展缓存（用户自定义）
        DEFAULT_CACHE_SIZE = 2    
        markKnownViewsInvalid：
            遍历mCachedViews，给其添加flag：ViewHolder.FLAG_UPDATE | ViewHolder.FLAG_INVALID
            if (mAdapter == null || !mAdapter.hasStableIds())   //在这种情况下，不能重用缓存的view。回收所有
                recycleAndClearCachedViews();       
        recycleAndClearCachedViews：遍历mCachedViews，回收 recycleCachedViewAt，最后 mCachedViews.clear()
        recycleCachedViewAt：
            回收缓存的view并从列表中删除它。当且仅当view可回收时，view才会添加到缓存中，因此此方法不会再次检查它
            这个规则的一个小例外是，当view没有animator引用，但transient状态为true时(由于在ItemAnimator之外创建的动画)。在这种情况下，Adapter可以选择回收它。从RecyclerView的角度来看，view仍然是可回收的，因为Adapter希望这样做
            会调用addViewHolderToRecycledViewPool
        addViewHolderToRecycledViewPool：
            准备要removed/recycled的ViewHolder，并将其插入到RecycledViewPool中
            对于未绑定的view，传递false给dispatchRecycle
            if (dispatchRecycled) 
                dispatchViewRecycled(holder);   //view被回收的回调
            holder.mOwnerRecyclerView = null;   //回收
            getRecycledViewPool().putRecycledView(holder);  //是上面的 mRecyclerPool
            
        tryGetViewHolderForPositionByDeadline：
            从缓存中获取ViewHolder，从scrap，cache, RecycledViewPool, 或直接创建
            if (mState.isPreLayout()) {     //1、预先layout？只在onMeasure中且mState.mRunPredictiveAnimations时设置为true
                holder = getChangedScrapViewForPosition(position);//预先布局时，从mChangedScrap中获取数据
                fromScrapOrHiddenOrCache = holder != null;
            }
            if (holder == null)，2、根据位置从scrap/hidden list/cache 中查找
                getScrapOrHiddenOrCachedHolderForPosition， 先从mAttachedScrap中查找，
                    然后从mChildHelper中的mHiddenViews列表中查找，若有找到则会将ViewHolder添加到mAttachedScrap或mChangedScrap中（scrapView）
                    若没有找到，则接着从mCachedViews中查找
                validateViewHolderForOffsetPosition，对holder做一些检查，是否可使用等，包括hasStableIds时getItemId的对应情况；返回false，会回收该ViewHolder，否则fromScrapOrHiddenOrCache=true
            if (holder == null)，上面没有找到
                if (mAdapter.hasStableIds())，
                    getScrapOrCachedViewForId，从scrap/cache中通过stable ids查找，先从mAttachedScrap中查找，getItemId是否相等，然后从mCachedViews中查找
                if (holder == null && mViewCacheExtension != null)，还没找到，且有用户扩展缓存mViewCacheExtension
                    mViewCacheExtension.getViewForPositionAndType，返回view，然后holder = getChildViewHolder(view)
                if (holder == null)，没找到，回退到池
                    holder = getRecycledViewPool().getRecycledView(type)
                if (holder == null)，还没有，则直接创建
                    holder = mAdapter.createViewHolder，
                if (fromScrapOrHiddenOrCache && !mState.isPreLayout() && holder.hasAnyOfTheFlags(ViewHolder.FLAG_BOUNCED_FROM_HIDDEN_LIST))
                    从隐藏列表中得到的ViewHolder？
                    mState.mRunSimpleAnimations，mItemAnimator.recordPreLayoutInformation等动画相关操作
                if (!holder.isBound() || holder.needsUpdate() || holder.isInvalid())，
                    tryBindViewHolderByDeadline，绑定viewHolder， bindViewHolder
                rvLayoutParams.mViewHolder = holder，保存ViewHolder
        tryBindViewHolderByDeadline：
            该方法在 tryGetViewHolderForPositionByDeadline 中调用
            mAdapter.bindViewHolder，绑定viewHolder

#### 优化

    能使用Adapter.notifyItemRangeChanged 就不使用 notifyDataSetChanged，即数据量很大时尽量不要使用 notifyDataSetChanged
    
    item不需要使用动画时，最好显示的设置mItemAnimator为null，因为其默认值是DefaultItemAnimator，而mItemAnimator不为null，可能会在layout阶段有不必要的消耗，运行简单动画、预测动画等
        设置动画会多次触发LayoutManager.onLayoutChildren
    setHasFixedSize：true，表示应用程序指定Adapter内容的更改不能影响RecyclerView本身的大小
        如果可以预先知道RecyclerView的大小不受Adapter内容的影响，则可以执行一些优化。
        RecyclerView仍然可以根据其他因素改变它的大小（例如它的parent的大小），但是这个大小的计算不能依赖于它的children的大小或者Adapter的内容（除了Adapter中item的数量）
        如果对RecyclerView的使用属于这一类，请将其设置为true。当Adapter内容更改时，它将允许RecyclerView避免使整个布局无效（避免重绘）

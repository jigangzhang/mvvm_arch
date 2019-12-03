
## View绘制流程
    
[参考](https://juejin.im/post/5d53ddd6f265da03d15549b8)
    
    Window作为View的容器，在View绘制流程中有重要作用，其实现类为PhoneWindow
    重要类有：ViewRootImpl、View、PhoneWindow、WindowManagerGlobal、DecorView、Surface等
    View的绘制流程中的重要点有：
        第一次的绘制是发生在Activity生命周期的什么时候？是在onCreate中，还是onResume？
            经测试，View绘制相关的三个方法是在onResume之后才执行的，即View在要显示的时候才进行绘制，所以在Activity的生命周期中不能获取View的尺寸吗？
            在onCreate阶段，虽然也有requestLayout、invalidate，但是并为触发真正的View绘制流程
        干什么可以触发View绘制？
            setLayoutParams、addView等
            主动调用requestLayout、invalidate、postInvalidate等
            系统属性更改等
        onMeasure、onLayout、onDraw三个重要过程
        事件分发流程
        onClock、onTouch等可与用户交互的事件在事件分发中的位置
        View树的遍历（在View绘制的三个流程中的具体操作）：
            ViewTreeObserver，视图树观察器用于注册侦听器，这些侦听器可以通知视图树中的全局更改。
            这样的全局事件包括但不限于整棵树的布局，绘制过程的开始，触摸模式更改...。ViewTreeObserver永远不应由应用程序实例化，因为它由视图层次结构提供。有关更多信息，请参考View.getViewTreeObserver()
            包含View层次结构的focusChanged、onGlobalFocusChanged、onGlobalLayout、onPreDraw、onScrollChanged等等
    
    invalidate、postInvalidate、requestLayout：
        invalidate()和postInvalidate()能够触发View的重画，这两个方法最终会调用到performTraversals()中的performDraw()来完成重绘制。
        invalidate()与postInvalidate()都是用于被调用来触发View的更新（重画）动作，区别在于invalidate()方法是在UI线程自身中使用，而postInvalidate()是非UI线程中使用
        requestLayout方法，会标记当前View及父容器，同时逐层向上提交，直到ViewRootImpl处理该事件，ViewRootImpl会调用三大流程，从measure开始，对于每一个含有标记位的view及其子View都会进行测量、布局、绘制（onMeasure，onLayout，onDraw）
        requestLayout会直接递归调用父窗口的requestLayout，直到ViewRootImpl,然后触发peformTraversals，由于mLayoutRequested为true，会导致onMeasure和onLayout被调用。不一定会触发OnDraw
        requestLayout在layout过程中发现l,t,r,b和以前不一样，就会触发一次invalidate，这种情况下回调用onDraw方法
        只要刷新的时候就调用invalidate，需要重新measure就调用requestLayout

#### View绘制的执行流程
    
    要显示一个UI页面，必须要在Activity的onCreate中通过setContentView设置View资源（layout.xml文件或代码生成的View）
    在Activity的setContentView中，不论是layout资源文件还是new View的方式，都会调用Window的对应setContentView方法，这个Window是在Activity attach方法中创建的PhoneWindow实例
    以layout资源文件方式加载页面时，会通过LayoutInflater.inflate方法解析xml文件，并创建对应的View
    LayoutInflater.inflate(int resource, ViewGroup root, boolean attachToRoot) {
        final Resources res = getContext().getResources();
        final XmlResourceParser parser = res.getLayout(resource);   //找到对应资源Id的xml文件，返回一个xml解析器，其中具体操作和AssetManager有关
        try {
            return inflate(parser, root, attachToRoot); //解析创建View，root不为null（PhoneWindow中的mContentParent），attachToRoot为true
        } finally {
            parser.close();
        }
    }
    LayoutInflater.inflate(XmlPullParser parser, ViewGroup root, boolean attachToRoot)：
        synchronized (mConstructorArgs) {   //mConstructorArgs为一个Object数组，size为2（mConstructorArgs = new Object[2]）
            final Context inflaterContext = mContext;   //mContext为Activity
            final AttributeSet attrs = Xml.asAttributeSet(parser);  //只是将parser转换为AttributeSet接口，XmlResourceParser实现了AttributeSet接口
            Context lastContext = (Context) mConstructorArgs[0];    //暂存上一个Activity
            mConstructorArgs[0] = inflaterContext;  //赋予当前Activity
            View result = root;     //为PhoneWindow中的mContentParent
            try {
                //查找根节点
                int type;
                while ((type = parser.next()) != XmlPullParser.START_TAG &&
                        type != XmlPullParser.END_DOCUMENT) {   //空循环，根节点必须是start tag，只要是 start tag或者是文档结尾，就向下走？
                    // Empty
                }
                if (type != XmlPullParser.START_TAG) {  //必须是start tag？一开始应该是xml文件中的根view？
                    throw new InflateException(parser.getPositionDescription() + ": No start tag found!");
                }
                final String name = parser.getName();
                System.out.println("Creating root view: " + name);
                if (TAG_MERGE.equals(name)) {       //merge标签，其必须附加到一个ViewGroup中，并且attachToRoot必须为true
                    if (root == null || !attachToRoot) {
                        throw new InflateException("<merge /> can be used only with a valid ViewGroup root and attachToRoot=true");
                    }
                    rInflate(parser, root, inflaterContext, attrs, false);  //解析merge标签下的View，并实例化它们
                } else {
                    //Temp是在xml中找到的根View
                    final View temp = createViewFromTag(root, name, inflaterContext, attrs);    //最终通过ClassLoader实例化name的View，一般都是一个ViewGroup
                    ViewGroup.LayoutParams params = null;
                    if (root != null) {
                        System.out.println("Creating params from root: " + root);
                        //创建与root匹配的布局参数（如果提供）
                        params = root.generateLayoutParams(attrs);  //内部设置了ViewGroup.LayoutParams的width和height，通过获取xml中的layout_width和layout_height
                        if (!attachToRoot) {    //attachToRoot为true（在Activity中setContentView的情况下）
                            //如果我们不附加，请为temp设置布局参数。（如果是的话，我们在下面使用addView）
                            temp.setLayoutParams(params);   //内部调用requestLayout，最终会调用到ViewRootImpl的scheduleTraversals，从而触发onMeasure、onLayout
                        }
                    }
                    System.out.println("-----> start inflating children");
                    //根据上下文加载所有处于temp下的子View
                    rInflateChildren(parser, temp, attrs, true);    //以迭代方式遍历xml文件中ViewGroup下的所有子View，并创建其实例
                    System.out.println("-----> done inflating children");
                    //我们应该将找到的所有View（在temp中）附加到root。现在就做
                    if (root != null && attachToRoot) {
                        root.addView(temp, params);     //内部调用requestLayout和invalidate，从而完成View的绘制
                    }
                    //确定是返回传入的root还是在xml中找到的根View
                    if (root == null || !attachToRoot) {        //在当前情况下，条件不成立，返回的是传入的root
                        result = temp;
                    }
                }
            } catch (XmlPullParserException e) {
                final InflateException ie = new InflateException(e.getMessage(), e);
                ie.setStackTrace(EMPTY_STACK_TRACE);
                throw ie;
            } catch (Exception e) {
                final InflateException ie = new InflateException(parser.getPositionDescription() + ": " + e.getMessage(), e);
                ie.setStackTrace(EMPTY_STACK_TRACE);
                throw ie;
            } finally {
                //不要在上下文中保留静态引用
                mConstructorArgs[0] = lastContext;
                mConstructorArgs[1] = null;
                Trace.traceEnd(Trace.TRACE_TAG_VIEW);
            }
            return result;
        }
    看LayoutInflater中的inflate方法，其输入参数ViewGroup root 和 boolean attachToRoot
    若root不为空，解析xml后得到的View会add到root中，若为空的话，直接返回xml中的根View
    attachToRoot，解析的View数是否应附加到root中？如果为false，则root仅用于为XML中的根View创建LayoutParams的正确子类
    
    即root不为空时，attachToRoot为false，为根View设置LayoutParams，最后返回的是xml中的根View，只有为true时才会add到root中，最终呈现到屏幕
    root为null时，无论attachToRoot为false/true，都会返回xml解析的View
    root==null ？ 根View : attachToRoot ？ root : 根View
    即无论怎样都会返回一个View，最终会显示？
    在当前情况下，inflate中，最后解析出所有View后，add到root中，即PhoneWindow中的mContentParent
    
    PhoneWindow.setContentView(View view, ViewGroup.LayoutParams params)：
        //注意：当主题属性等明确化时，可以在安装窗口装饰的过程中设置FEATURE_CONTENT_TRANSITIONS。 在这种情况发生之前，请勿检查该功能
        if (mContentParent == null) {
            installDecor();     //内部会实例化一个DecorView和mContentParent
        } else if (!hasFeature(FEATURE_CONTENT_TRANSITIONS)) {
            mContentParent.removeAllViews();
        }
        if (hasFeature(FEATURE_CONTENT_TRANSITIONS)) {
            view.setLayoutParams(params);   //内部调用requestLayout
            final Scene newScene = new Scene(mContentParent, view);     //转场动画？切换页面？
            transitionTo(newScene);
        } else {
            mContentParent.addView(view, params);   //内部调用requestLayout和invalidate
        }
        mContentParent.requestApplyInsets();    //最终也会触发ViewRootImpl的scheduleTraversals？
        final Callback cb = getCallback();
        if (cb != null && !isDestroyed()) {
            cb.onContentChanged();      //View内容改变的通知回调，是Window中的Callback接口的函数，由Activity实现了
        }
        mContentParentExplicitlySet = true;
    Scene，场景表示应用场景时，View层次结构中各种属性将具有的值的集合。 场景可以配置为在应用场景时自动运行过渡，这将为场景更改过程中发生的各种属性更改设置动画
    以xml资源文件方式加载View与代码方式加载View，除了加载方式不同外，其他都一样
    最终都是将View添加到了mContentParent（ViewGroup）中，在addView中，最终会触发View的绘制？
    
    ViewGroup.addView：
        //设置新的LayoutParams时，addViewInner()将调用child.requestLayout()，因此，在此之前对自己调用了requestLayout()，这样子View的请求将在我们的级别被阻止
        requestLayout();    //当某些更改使该视图的布局无效时，调用此方法。这将安排view树的布局遍历。当视图层次结构当前处于布局阶段（isInLayout()中）时，不应调用此方法。如果正在进行布局，则可以在当前布局阶段结束时（然后布局将再次运行）或当前阶段结束时接受请求。绘制框架并进行下一个布局。重写此方法的子类应调用超类方法以正确处理可能的request-during-layout错误
        invalidate(true);
        addViewInner(child, index, params, false);
    View.requestLayout：
        if (mMeasureCache != null) mMeasureCache.clear();   //一个集合类，作缓存
        if (mAttachInfo != null && mAttachInfo.mViewRequestingLayout == null) {     //mAttachInfo是在dispatchAttachedToWindow中赋值
            //仅当这是它自己请求时才触发request-during-layout逻辑，而不是其父层次结构中的视图
            ViewRootImpl viewRoot = getViewRootImpl();  //ViewRootImpl在AttachInfo中
            if (viewRoot != null && viewRoot.isInLayout()) {        //当前View是否正在layout阶段（布局中），应该为false
                if (!viewRoot.requestLayoutDuringLayout(this)) {
                    return;
                }
            }
            mAttachInfo.mViewRequestingLayout = this;   //mViewRequestingLayout用于跟踪哪个视图发起了requestLayout()调用，在布局期间调用requestLayout()时使用
        }
        mPrivateFlags |= PFLAG_FORCE_LAYOUT;    //会在执行View的measure()和layout()方法时判断,只有设置过该标志位，才会执行measure()和layout()流程
        mPrivateFlags |= PFLAG_INVALIDATED;     //指示此视图被专门无效，而不仅仅是因为某些子视图无效而被污染。该标志用于确定何时需要重新创建视图的显示列表（与仅返回对其现有显示列表的引用相反）
        //isLayoutRequested，指示是否在下一次层次结构布局遍历期间请求此视图的布局。如果在下一次布局传递期间将强制布局，则为true
        if (mParent != null && !mParent.isLayoutRequested()) {  //mParent的值会在addViewInner中赋予，当前View为PhoneWindow中的mContentParent，其父视图（即mParent）应该为DecorView
            mParent.requestLayout();    //请求父视图布局
        }
        if (mAttachInfo != null && mAttachInfo.mViewRequestingLayout == this) {
            mAttachInfo.mViewRequestingLayout = null;
        }
    PhoneWindow中的mContentParent与应用主题（AppTheme）有很大的关系，其是在PhoneWindow.generateLayout中创建的（很多主题属性都是在这个方法中设置的）
    mContentParent对应为ID_ANDROID_CONTENT（id为content的系统布局，我们的布局的实际容器），其父布局为DecorView，DecorView的父视图为ViewRootImpl？
    ViewRootImpl是AttachInfo内的一个变量，mAttachInfo是在dispatchAttachedToWindow中赋给View的，而View.AttachInfo是在ViewRootImpl构造函数中创建的
    
    ViewRootImpl是在哪里创建的？？在WindowManagerGlobal中的addView中创建的，要使页面可见，必须要addView到WindowManager中（在onResume后），而WindowManagerGlobal是WindowManager的实际操作类
    DecorView有没有mParent？是不是ViewRootImpl？若是，在requestLayout中最终会调用到ViewRootImpl的requestLayout
    在WindowManagerGlobal的addView中，创建ViewRootImpl并调用其setView方法（设置的应该是DecorView），在setView中给view设置mPatent为当前ViewRootImpl
    所以在onCreate阶段的requestLayout不会触发ViewRootImpl的requestLayout？
    ViewRootImpl是View层次结构的顶部，在View和WindowManager之间实现所需的协议。这大部分是WindowManagerGlobal的内部实现细节
    ViewRootImpl.requestLayout：
        if (!mHandlingLayoutInLayoutRequest) {  //是否正在进行performLayout
            checkThread();      //检查线程，只有创建View的线程才能对View作操作
            mLayoutRequested = true;    //表示已请求layout，将等待完成布局？
            scheduleTraversals();   //最终执行到View的onMeasure、onLayout、onDraw
        }
    在执行View中的requestLayout时，最后将会执行到ViewRootImpl的requestLayout，最后会执行scheduleTraversals，触发View的onMeasure和onLayout
    
    View.invalidate(boolean invalidateCache)，这是invalidate()工作实际发生的地方。完整的invalidate()会导致图形缓存无效，但是可以在将invalidateCache设置为false的情况下调用此函数，以在不需要的情况下跳过该无效步骤(例如，与尺寸保持相同尺寸的组件相同的内容)
    invalidateCache，此视图的图形缓存是否也应该无效。通常是true使缓存完全失效，但是如果视图的内容或尺寸未更改，则可以将其设置为false
    invalidate内部调用invalidateInternal
    View.invalidateInternal(int l, int t, int r, int b, boolean invalidateCache, boolean fullInvalidate)：
        //当前情况下，invalidateCache、fullInvalidate均为true
        if (mGhostView != null) {   //GhostView，覆盖物将绘制此View，而不是绘制为该View父级的一部分。 mGhostView是覆盖物中的View，当此View无效时，该视图必须无效
            mGhostView.invalidate(true);    //未找到赋值处，暂认为是null
            return;
        }
        if (skipInvalidate()) { //若View不可见且未运行动画，将跳过绘制。它们不会被绘制，也不应设置脏标志，就好像它们将被绘制一样
            return;
        }
        if ((mPrivateFlags & (PFLAG_DRAWN | PFLAG_HAS_BOUNDS)) == (PFLAG_DRAWN | PFLAG_HAS_BOUNDS)
                || (invalidateCache && (mPrivateFlags & PFLAG_DRAWING_CACHE_VALID) == PFLAG_DRAWING_CACHE_VALID)
                || (mPrivateFlags & PFLAG_INVALIDATED) != PFLAG_INVALIDATED || (fullInvalidate && isOpaque() != mLastIsOpaque)) {
            if (fullInvalidate) {   //是否全部重绘，默认是true
                mLastIsOpaque = isOpaque();     //是否不透明
                mPrivateFlags &= ~PFLAG_DRAWN;
            }
            mPrivateFlags |= PFLAG_DIRTY;
            if (invalidateCache) {  //是全刷新还是只重绘部分View，true 缓存无效，全刷新
                mPrivateFlags |= PFLAG_INVALIDATED;
                mPrivateFlags &= ~PFLAG_DRAWING_CACHE_VALID;
            }
            //将损坏矩形传播到父视图
            final AttachInfo ai = mAttachInfo;
            final ViewParent p = mParent;   //一个View的mParent是在ViewGroup的addView中设置的，在当前方法后的addViewInner中
            if (p != null && ai != null && l < r && t < b) {
                final Rect damage = ai.mTmpInvalRect;
                damage.set(l, t, r, b);
                p.invalidateChild(this, damage);    //将要重绘的区域用Rect传递给父View
            }
            //如有必要，损坏整个投影接收器
            if (mBackground != null && mBackground.isProjected()) {
                final View receiver = getProjectionReceiver();  //递归所有ViewParent，直到找到投影接收者（其mBackground不为null）
                if (receiver != null) {
                    receiver.damageInParent();  //告诉父视图破坏这个视图的边界
                }
            }
        }
    若有图层，则绘制图层，否则将需要重绘的区域传给父View绘制
    ViewGroup.invalidateChild(View child, final Rect dirty)：        //用于实现View层级？
        final AttachInfo attachInfo = mAttachInfo;
        if (attachInfo != null && attachInfo.mHardwareAccelerated) {    //硬件加速
            //硬件加速快速路径
            onDescendantInvalidated(child, child);      //若启用硬件加速，将转入onDescendantInvalidated
            return;
        }
        ViewParent parent = this;
        if (attachInfo != null) {
            //如果child正在绘制动画，我们想将此标志复制到我们自己和parent身上，以确保无效请求通过
            final boolean drawAnimation = (child.mPrivateFlags & PFLAG_DRAW_ANIMATION) != 0;
            //检查请求无效的child是否完全不透明，正在动画或变换的视图不被认为是不透明的，因为我们可能使他们的旧位置无效并且需要parent在他们后面绘画
            Matrix childMatrix = child.getMatrix();
            //是否不透明的判断，child若正在动画或正在矩阵变换，则认为不是不透明，即为false
            final boolean isOpaque = child.isOpaque() && !drawAnimation && child.getAnimation() == null && childMatrix.isIdentity();
            //使用适当的标记将孩子标记为肮脏，确保我们不要同时设置两个标志
            int opaqueFlag = isOpaque ? PFLAG_DIRTY_OPAQUE : PFLAG_DIRTY;
            if (child.mLayerType != LAYER_TYPE_NONE) {
                mPrivateFlags |= PFLAG_INVALIDATED;
                mPrivateFlags &= ~PFLAG_DRAWING_CACHE_VALID;
            }
            final int[] location = attachInfo.mInvalidateChildLocation;   //mInvalidateChildLocation,视图层次结构的全局变量，用作在ViewGroup.invalidateChild实现中处理x/y点的临时对象
            location[CHILD_LEFT_INDEX] = child.mLeft;
            location[CHILD_TOP_INDEX] = child.mTop;     //左上这个点
            //FLAG_SUPPORT_STATIC_TRANSFORMATIONS标记表示子类是否覆盖了getChildStaticTransformation
            if (!childMatrix.isIdentity() || (mGroupFlags & ViewGroup.FLAG_SUPPORT_STATIC_TRANSFORMATIONS) != 0) {
                RectF boundingRect = attachInfo.mTmpTransformRect;
                boundingRect.set(dirty);        //设置要重绘的区域？
                Matrix transformMatrix;
                if ((mGroupFlags & ViewGroup.FLAG_SUPPORT_STATIC_TRANSFORMATIONS) != 0) {   //若当前ViewGroup覆盖了getChildStaticTransformation，则调用这个方法进行变换操作
                    Transformation t = attachInfo.mTmpTransformation;
                    boolean transformed = getChildStaticTransformation(child, t);   //是否要变换？
                    if (transformed) {
                        transformMatrix = attachInfo.mTmpMatrix;
                        transformMatrix.set(t.getMatrix());     //将变换后的矩阵赋给transformMatrix？
                        if (!childMatrix.isIdentity()) {
                            transformMatrix.preConcat(childMatrix);
                        }
                    } else {
                        transformMatrix = childMatrix;      //不需要变换则将child的矩阵直接赋值？
                    }
                } else {
                    transformMatrix = childMatrix;      //不需要变换则将child的矩阵直接赋值？
                }
                transformMatrix.mapRect(boundingRect);  //对需要重绘的区域（RectF）做矩阵操作？
                dirty.set((int) Math.floor(boundingRect.left), (int) Math.floor(boundingRect.top),
                        (int) Math.ceil(boundingRect.right), (int) Math.ceil(boundingRect.bottom));     //操作完后赋值？
            }
            do {    //循环遍历parent
                View view = null;
                if (parent instanceof View) {
                    view = (View) parent;   //parent（当前这个ViewGroup）为View
                }
                if (drawAnimation) {    //动画中？
                    if (view != null) {
                        view.mPrivateFlags |= PFLAG_DRAW_ANIMATION;     //将动画中的标志赋予flag
                    } else if (parent instanceof ViewRootImpl) {    //遍历到最终的ViewRootImpl
                        ((ViewRootImpl) parent).mIsAnimating = true;    //动画中
                    }
                }
                //如果parent是dirty opaque或不dirty，请使用触发invalidate的child的不透明标志将其标记为dirty
                if (view != null) {
                    //FADING_EDGE_MASK表示淡出边缘？动画效果？getSolidColor默认为0
                    if ((view.mViewFlags & FADING_EDGE_MASK) != 0 && view.getSolidColor() == 0) {
                        opaqueFlag = PFLAG_DIRTY;
                    }
                    if ((view.mPrivateFlags & PFLAG_DIRTY_MASK) != PFLAG_DIRTY) {
                        view.mPrivateFlags = (view.mPrivateFlags & ~PFLAG_DIRTY_MASK) | opaqueFlag;
                    }
                }
                parent = parent.invalidateChildInParent(location, dirty);   //调用当前ViewParent的该方法，返回的是当前ViewParent的Parent，最终调用到ViewRootImpl
                if (view != null) {     //是View，而不是ViewRootImpl
                    //当前parent的转换帐户
                    Matrix m = view.getMatrix();
                    if (!m.isIdentity()) {
                        RectF boundingRect = attachInfo.mTmpTransformRect;
                        boundingRect.set(dirty);
                        m.mapRect(boundingRect);    //对RectF做矩阵变换
                        dirty.set((int) Math.floor(boundingRect.left), (int) Math.floor(boundingRect.top),
                                (int) Math.ceil(boundingRect.right), (int) Math.ceil(boundingRect.bottom));
                    }
                }
            } while (parent != null);
        }
    某个View调用invalidate时，将该View需要重绘的区域传递到该View的parent中（调用parent的invalidateChild）
    在invalidateChild中，检查一些标志，对要重绘的区域做矩阵变换，然后进入一个循环（遍历parent），循环中先调用parent的invalidateChildInParent，然后再对重绘区域做矩阵变换，直到ViewRootImpl为止
    
    invalidateChildInParent，child的全部或部分被标记为dirty，需要重画。location数组是由两个int值组成的数组，它们分别定义dirty child的左侧和顶部位置。如果指定的矩形在parent中必须要invalidate，则此方法必须返回此ViewParent的parent。 如果指定的矩形不需要在parent中invalidate，或者如果parent不存在，则此方法必须返回null。当此方法返回非null值时，必须使用此ViewParent的left坐标和top坐标来更新location数组。
    invalidateChildInParent，如果此ViewGroup没有parent，或者该ViewGroup已经完全invalidate过了，或者脏矩形不与此ViewGroup的边界相交，则此实现返回null。
    ViewGroup.invalidateChildInParent：
        if ((mPrivateFlags & (PFLAG_DRAWN | PFLAG_DRAWING_CACHE_VALID)) != 0) {     //绘制标记
            //DRAWN 或 DRAWING_CACHE_VALID
            if ((mGroupFlags & (FLAG_OPTIMIZE_INVALIDATE | FLAG_ANIMATION_DONE)) != FLAG_OPTIMIZE_INVALIDATE) {
                dirty.offset(location[CHILD_LEFT_INDEX] - mScrollX, location[CHILD_TOP_INDEX] - mScrollY);
                if ((mGroupFlags & FLAG_CLIP_CHILDREN) == 0) {
                    dirty.union(0, 0, mRight - mLeft, mBottom - mTop);      //做并集
                }       //上面根据flag对重绘区域做一些操作
                final int left = mLeft;
                final int top = mTop;
                if ((mGroupFlags & FLAG_CLIP_CHILDREN) == FLAG_CLIP_CHILDREN) {
                    if (!dirty.intersect(0, 0, mRight - left, mBottom - top)) {     //求交集？是否相交？
                        dirty.setEmpty();
                    }
                }
                location[CHILD_LEFT_INDEX] = left;
                location[CHILD_TOP_INDEX] = top;
            } else {
                if ((mGroupFlags & FLAG_CLIP_CHILDREN) == FLAG_CLIP_CHILDREN) {
                    dirty.set(0, 0, mRight - mLeft, mBottom - mTop);
                } else {
                    //万一dirty矩形延伸到该容器的边界之外
                    dirty.union(0, 0, mRight - mLeft, mBottom - mTop);
                }
                location[CHILD_LEFT_INDEX] = mLeft;
                location[CHILD_TOP_INDEX] = mTop;
                mPrivateFlags &= ~PFLAG_DRAWN;
            }
            mPrivateFlags &= ~PFLAG_DRAWING_CACHE_VALID;
            if (mLayerType != LAYER_TYPE_NONE) {
                mPrivateFlags |= PFLAG_INVALIDATED;
            }
            return mParent;
        }
        return null;
    invalidateChildInParent中主要对重绘区域做了一些操作，最终的绘制在ViewRootImpl中
    ViewRootImpl的invalidateChild中直接调用invalidateChildInParent
    ViewRootImpl.invalidateChildInParent：
        checkThread();      //检查线程，是否在主线程       
        if (dirty == null) {    //dirty为null表示需要全部绘制？页面还没绘制，是第一次绘制？
            invalidate();
            return null;
        } else if (dirty.isEmpty() && !mIsAnimating) {  //mIsAnimating在ViewGroup中已被设为true
            return null;
        }
        if (mCurScrollY != 0 || mTranslator != null) {
            mTempRect.set(dirty);
            dirty = mTempRect;
            if (mCurScrollY != 0) {
                dirty.offset(0, -mCurScrollY);      //做向上的位移
            }
            if (mTranslator != null) {  //坐标转换器
                mTranslator.translateRectInAppWindowToScreen(dirty);    //将Rect在应用程序的窗口中转换为屏幕，做了Rect的缩放
            }
            if (mAttachInfo.mScalingRequired) {
                dirty.inset(-1, -1);    //移动矩形
            }
        }
        invalidateRectOnScreen(dirty);
        return null;
    ViewRootImpl.invalidateRectOnScreen(Rect dirty)：
        final Rect localDirty = mDirty;     //本地Dirty区域
        if (!localDirty.isEmpty() && !localDirty.contains(dirty)) {     //重绘区域包含在本地区域中
            mAttachInfo.mSetIgnoreDirtyState = true;
            mAttachInfo.mIgnoreDirtyState = true;
        }
        //将新的dirty矩形添加到当前的矩形
        localDirty.union(dirty.left, dirty.top, dirty.right, dirty.bottom);
        //与窗口的边界相交以跳过可见区域之外的更新
        final float appScale = mAttachInfo.mApplicationScale;
        final boolean intersected = localDirty.intersect(0, 0, (int) (mWidth * appScale + 0.5f), (int) (mHeight * appScale + 0.5f));    //是否相交，若相交返回true，并更改local矩形为交集
        if (!intersected) {     //不相交，即重绘区域在可视区域之外？
            localDirty.setEmpty();
        }
        if (!mWillDrawSoon && (intersected || mIsAnimating)) {  //mWillDrawSoon只在进行performTraversals是为true
            scheduleTraversals();   //安排绘制操作，最终执行view的onMeasure、onLayout、onDraw
        }
    
    mAttachInfo是在View的dispatchAttachedToWindow中被赋予的，而dispatchAttachedToWindow是在addViewInner中被调用，即在addViewInner之前子View的mAttachInfo、ViewRootImpl为null
    ViewGroup.addViewInner(View child, int index, LayoutParams params, boolean preventRequestLayout)：
        //preventRequestLayout为false
        if (mTransition != null) {
            //不要阻止其他添加过渡完成，但要取消删除过渡，以使其在我们添加到容器之前完成该过程
            mTransition.cancel(LayoutTransition.DISAPPEARING);
        }
        if (child.getParent() != null) {    //子View的parent只在addView时设置的
            throw new IllegalStateException("The specified child already has a parent. You must call removeView() on the child's parent first.");
        }
        if (mTransition != null) {
            mTransition.addChild(this, child);  //动画？
        }
        if (!checkLayoutParams(params)) {   //params是否为null，不为null返回true
            params = generateLayoutParams(params);
        }
        if (preventRequestLayout) {
            child.mLayoutParams = params;
        } else {
            child.setLayoutParams(params);  //会触发requestLayout
        }
        if (index < 0) {
            index = mChildrenCount;
        }
        addInArray(child, index);   //添加到View数组中
        //告诉我们的children
        if (preventRequestLayout) {
            child.assignParent(this);
        } else {
            child.mParent = this;   //给子View设置parent
        }
        if (child.hasUnhandledKeyListener()) {  //子View有UnhandledKeyListener
            incrementChildUnhandledKeyListeners();      //将parent的mChildUnhandledKeyListeners值加1
        }
        final boolean childHasFocus = child.hasFocus();     //该View或其子View是否有焦点
        if (childHasFocus) {
            requestChildFocus(child, child.findFocus());    //请求聚焦？
        }
        AttachInfo ai = mAttachInfo;    //mAttachInfo在View的dispatchAttachedToWindow中赋予值
        if (ai != null && (mGroupFlags & FLAG_PREVENT_DISPATCH_ATTACHED_TO_WINDOW) == 0) {
            boolean lastKeepOn = ai.mKeepScreenOn;  //暂存
            ai.mKeepScreenOn = false;   //复位
            child.dispatchAttachedToWindow(mAttachInfo, (mViewFlags&VISIBILITY_MASK));  //将mAttachInfo赋予给子View
            if (ai.mKeepScreenOn) {
                needGlobalAttributesUpdate(true);   //只是设置了AttachInfo的值？
            }
            ai.mKeepScreenOn = lastKeepOn;
        }
        if (child.isLayoutDirectionInherited()) {   //布局方向是继承的？
            child.resetRtlProperties();     //重置所有RTL相关属性的分辨率
        }
        dispatchViewAdded(child);       //分发View添加的事件，ViewGroup的OnHierarchyChangeListener事件和ViewGroup的onViewAdded方法
        if ((child.mViewFlags & DUPLICATE_PARENT_STATE) == DUPLICATE_PARENT_STATE) {    //赋值parent状态？
            mGroupFlags |= FLAG_NOTIFY_CHILDREN_ON_DRAWABLE_STATE_CHANGE;   //通知子View Drawable状态改变？
        }
        if (child.hasTransientState()) {    //瞬态？
            childHasTransientStateChanged(child, true);     //当子View发生变化时调用，不管它是否跟踪瞬态状态
        }
        if (child.getVisibility() != View.GONE) {   //子View可见
            notifySubtreeAccessibilityStateChangedIfNeeded();
        }
        if (mTransientIndices != null) {        //遍历mTransientIndices
            final int transientCount = mTransientIndices.size();
            for (int i = 0; i < transientCount; ++i) {
                final int oldIndex = mTransientIndices.get(i);
                if (index <= oldIndex) {
                    mTransientIndices.set(i, oldIndex + 1);
                }
            }
        }
        if (mCurrentDragStartEvent != null && child.getVisibility() == VISIBLE) {
            notifyChildOfDragStart(child);      //通知子View拖拽开始
        }
        if (child.hasDefaultFocus()) {  //返回以该View为根的View层次结构是否包含默认焦点的View
            //在Inflate期间或手动组装View层次结构时，添加包含默认焦点的子View时，请更新祖先默认焦点链
            setDefaultFocus(child);
        }
        touchAccessibilityNodeProviderIfNeeded(child);  //我们可能需要联系provider以提出a11y层。在a11y模式下，客户端会检查屏幕或用户触摸屏幕，从而触发a11y基础架构的建立，而在自动填充模式下，我们希望从一开始就开始运行，因为我们会观察a11y事件来推动自动填充。
    addViewInner中主要是给子View设置Parent和AttachInfo，以及焦点等的处理
    到此addView相关项结束
    
    在onCreate中第一次addView时，各子View的parent为null，且此时ViewRootImpl也为null，因此不会触发各子View的绘制流程
    在addViewInner中，给子View设置了parent和AttachInfo，AttachInfo中包含ViewRootImpl，即在addView之后，子View才会拥有绘制相关的一些必须参数，比如mParent、AttachInfo等
    在onCreate中addView之后，当前Activity的页面还不可见，得等到onResume后调用WindowManager的addView之后当前页面才能可见
    
#### 使Activity页面可见阶段

    在onCreate阶段并未触发真正的View绘制，onStart和onResume是两个连续的阶段，只有在onResume后调用WindowManager的addView之后才进行真正的绘制
    ActivityThread.handleResumeActivity：
        ...
        final ActivityClientRecord r = performResumeActivity(token, finalStateRequest, reason);     //内部执行onStart、onResume
        ...
        ViewManager wm = a.getWindowManager();      //a是Activity，即调用Activity的getWindowManager
        WindowManager.LayoutParams l = r.window.getAttributes();
        a.mDecor = decor;   //Activity的DecorView
        l.type = WindowManager.LayoutParams.TYPE_BASE_APPLICATION;  //Window的层级
        l.softInputMode |= forwardBit;
        if (r.mPreserveWindow) {
            a.mWindowAdded = true;
            r.mPreserveWindow = false;
            ViewRootImpl impl = decor.getViewRootImpl();        //第一次添加时ViewRootImpl应该是null？
            if (impl != null) {
                impl.notifyChildRebuilt();
            }
        }
        if (a.mVisibleFromClient) {     //mVisibleFromClient默认为true，表示可见？
            if (!a.mWindowAdded) {      //Window还未添加View
                a.mWindowAdded = true;
                wm.addView(decor, l);   //添加decor到Window
            } else {
                a.onWindowAttributesChanged(l);     //最终将调用ViewRootImpl的setLayoutParams，从而触发View的绘制
            }
        }
        ...
    Activity第一次执行onCreate后，会将DecorView添加到WindowManager，若不是首次，则调用onWindowAttributesChanged，最终都将触发View的绘制
    Activity的getWindowManager返回mWindowManager，而mWindowManager是在attach中被赋值的（mWindowManager = mWindow.getWindowManager();）
    mWindow是PhoneWindow的实例，但是PhoneWindow未覆写getWindowManager方法，所以见Window的getWindowManager方法
    Window的getWindowManager方法直接返回mWindowManager，而mWindowManager是在Window的setWindowManager中赋值的，而Window的setWindowManager也是在Activity的attach中被调用
    Window.setWindowManager(WindowManager wm, IBinder appToken, String appName, boolean hardwareAccelerated) {
        mAppToken = appToken;
        mAppName = appName;
        mHardwareAccelerated = hardwareAccelerated || SystemProperties.getBoolean(PROPERTY_HARDWARE_UI, false);
        if (wm == null) {
            wm = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
        }
        mWindowManager = ((WindowManagerImpl)wm).createLocalWindowManager(this);
    }
    WindowManagerImpl的createLocalWindowManager返回的是WindowManagerImpl的实例
    而WindowManagerImpl的addView内部又调用WindowManagerGlobal的addView方法，即WindowManagerImpl是WindowManagerGlobal的代理？
    
    WindowManagerGlobal是个单例，即整个App内都只存在这一个实例
    WindowManagerGlobal.addView(View view, ViewGroup.LayoutParams params, Display display, Window parentWindow)：
        if (view == null) throw new IllegalArgumentException("view must not be null");
        if (display == null) throw new IllegalArgumentException("display must not be null");
        if (!(params instanceof WindowManager.LayoutParams)) throw new IllegalArgumentException("Params must be WindowManager.LayoutParams");
        final WindowManager.LayoutParams wparams = (WindowManager.LayoutParams) params;
        if (parentWindow != null) {
            parentWindow.adjustLayoutParamsForSubWindow(wparams);   //给子Window设置params
        } else {
            //如果没有父Window，则从应用程序的硬件加速设置中设置此View的硬件加速
            final Context context = view.getContext();
            if (context != null && (context.getApplicationInfo().flags 
                & ApplicationInfo.FLAG_HARDWARE_ACCELERATED) != 0) {    //在Manifest中设置了硬件加速？
                wparams.flags |= WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;  //设置硬件加速
            }
        }
        ViewRootImpl root;
        View panelParentView = null;
        synchronized (mLock) {
            //开始观察系统属性更改
            if (mSystemPropertyUpdater == null) {
                mSystemPropertyUpdater = new Runnable() {
                    @Override public void run() {
                        synchronized (mLock) {
                            for (int i = mRoots.size() - 1; i >= 0; --i) {
                                mRoots.get(i).loadSystemProperties();   //遍历所有的ViewRootImpl，将系统属性更改应用到所有App中的所有View中
                            }
                        }
                    }
                };
                SystemProperties.addChangeCallback(mSystemPropertyUpdater);     //系统属性更改的回调，应用到所有的ViewRootImpl，即所有App
            }
            int index = findViewLocked(view, false);    //只返回当前的DecorView在mView列表中的索引
            if (index >= 0) {       //大于0表示已经添加到WIndow了？
                if (mDyingViews.contains(view)) {
                    //不要等待MSG_DIE通过root的队列
                    mRoots.get(index).doDie();      //将对应的ViewRootImpl销毁
                } else {
                    throw new IllegalStateException("View " + view + " has already been added to the window manager.");
                }
                //先前的removeView()尚未完成执行。现在可以了
            }
            //如果这是一个面板窗口，请查找该窗口附加到的窗口，以备将来引用
            if (wparams.type >= WindowManager.LayoutParams.FIRST_SUB_WINDOW &&
                    wparams.type <= WindowManager.LayoutParams.LAST_SUB_WINDOW) {   //该Window是子窗口类型
                final int count = mViews.size();
                for (int i = 0; i < count; i++) {
                    if (mRoots.get(i).mWindow.asBinder() == wparams.token) {
                        panelParentView = mViews.get(i);    //panel View，子窗口中的View？
                    }
                }
            }
            root = new ViewRootImpl(view.getContext(), display);    //创建一个ViewRootImpl实例，其中会实例化一个mAttachInfo
            view.setLayoutParams(wparams);
            mViews.add(view);
            mRoots.add(root);
            mParams.add(wparams);       //使用全局变量保存所有App的view、ViewRootImpl、params，每个App进程对应一个
            //最后执行此操作是因为它会触发启动操作的消息
            try {
                root.setView(view, wparams, panelParentView);   //会给DecorView设置parent，并触发View的绘制
            } catch (RuntimeException e) {
                //BadTokenException或InvalidDisplayException，清除
                if (index >= 0) {
                    removeViewLocked(index, true);      //若已经成功添加到列表中，则将ViewRootImpl添加到MDyingViews列表中
                }
                throw e;
            }
        }
    首先添加一个系统属性更改的回调，然后找传入View对应的ViewRootImpl是否存在，若存在，将其die，或抛异常，最后创建一个新的ViewRootImpl，将其保存到全局列表中，并调用root.setView触发View绘制
    
    ViewRootImpl.setView(View view, WindowManager.LayoutParams attrs, View panelParentView)：
        synchronized (this) {
            if (mView == null) {
                mView = view;       //该View就是传入的DecorView
                mAttachInfo.mDisplayState = mDisplay.getState();    //获取显示的状态，显示状态:STATE_OFF、STATE_ON、STATE_DOZE、STATE_DOZE_SUSPEND或STATE_UNKNOWN中的一个
                mDisplayManager.registerDisplayListener(mDisplayListener, mHandler);    //注册显示侦听器，以便接收关于何时添加、删除或更改显示的通知，display off on doze状态切换也会触发View绘制
                mViewLayoutDirectionInitial = mView.getRawLayoutDirection();        //View方向
                mFallbackEventHandler.setView(view);
                mWindowAttributes.copyFrom(attrs);      //复制attrs
                if (mWindowAttributes.packageName == null) {
                    mWindowAttributes.packageName = mBasePackageName;   //包名
                }
                attrs = mWindowAttributes;
                setTag();   //设置tag，String
                if (DEBUG_KEEP_SCREEN_ON && (mClientWindowLayoutFlags & WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0
                        && (attrs.flags&WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) == 0) {
                    Slog.d(mTag, "setView: FLAG_KEEP_SCREEN_ON changed from true to false!");
                }
                //跟踪客户端提供的实际窗口标志
                mClientWindowLayoutFlags = attrs.flags;
                setAccessibilityFocus(null, null);
                if (view instanceof RootViewSurfaceTaker) {
                    mSurfaceHolderCallback = ((RootViewSurfaceTaker)view).willYouTakeTheSurface();
                    if (mSurfaceHolderCallback != null) {
                        mSurfaceHolder = new TakenSurfaceHolder();      //SurfaceHolder貌似和View绘制有关
                        mSurfaceHolder.setFormat(PixelFormat.UNKNOWN);
                        mSurfaceHolder.addCallback(mSurfaceHolderCallback);
                    }
                }
                //计算以指定的Z值绘制所需的表面插图
                // TODO:使用实数阴影插图获得恒定的最大Z值
                if (!attrs.hasManualSurfaceInsets) {
                    attrs.setSurfaceInsets(view, false /*manual*/, true /*preservePrevious*/);
                }
                CompatibilityInfo compatibilityInfo = mDisplay.getDisplayAdjustments().getCompatibilityInfo();
                mTranslator = compatibilityInfo.getTranslator();
                //如果应用程序拥有surface，则不要启用硬件加速
                if (mSurfaceHolder == null) {
                    //虽然应该只启用它，但它也可以有效地禁用加速
                    enableHardwareAcceleration(attrs);
                    final boolean useMTRenderer = MT_RENDERER_AVAILABLE && mAttachInfo.mThreadedRenderer != null;
                    if (mUseMTRenderer != useMTRenderer) {
                        //不应调整大小，因为它仅在窗口设置中完成，但以防万一
                        endDragResizing();  //结束拖动调整大小，这将通知所有侦听器窗口调整大小已结束。设置了mFullRedrawNeeded为true
                        mUseMTRenderer = useMTRenderer;
                    }
                }
                boolean restore = false;
                if (mTranslator != null) {
                    mSurface.setCompatibilityTranslator(mTranslator);
                    restore = true;
                    attrs.backup();     //备份
                    mTranslator.translateWindowLayout(attrs);   //动画？
                }
                if (!compatibilityInfo.supportsScreen()) {
                    attrs.privateFlags |= WindowManager.LayoutParams.PRIVATE_FLAG_COMPATIBLE_WINDOW;
                    mLastInCompatMode = true;
                }
                mSoftInputMode = attrs.softInputMode;   //软键盘模式
                mWindowAttributesChanged = true;    //标志属性更改
                mWindowAttributesChangesFlag = WindowManager.LayoutParams.EVERYTHING_CHANGED;   //everything changed
                mAttachInfo.mRootView = view;       //DecorView作为RootView
                mAttachInfo.mScalingRequired = mTranslator != null;     //缩放？
                mAttachInfo.mApplicationScale = mTranslator == null ? 1.0f : mTranslator.applicationScale;  //缩放比例？
                if (panelParentView != null) {  //Window上附着的子Window，只是Activity时为null
                    mAttachInfo.mPanelParentWindowToken = panelParentView.getApplicationWindowToken();
                }
                mAdded = true;  //标记
                int res; /* = WindowManagerImpl.ADD_OKAY; */
                //在添加到窗口管理器之前，安排第一个布局--确保在从系统接收任何其他事件之前进行重新布局
                requestLayout();
                if ((mWindowAttributes.inputFeatures & WindowManager.LayoutParams.INPUT_FEATURE_NO_INPUT_CHANNEL) == 0) {
                    mInputChannel = new InputChannel();
                }
                mForceDecorViewVisibility = (mWindowAttributes.privateFlags & PRIVATE_FLAG_FORCE_DECOR_VIEW_VISIBILITY) != 0;
                try {
                    mOrigWindowType = mWindowAttributes.type;
                    mAttachInfo.mRecomputeGlobalAttributes = true;
                    collectViewAttributes();        //给mAttachInfo的变量赋值
                    res = mWindowSession.addToDisplay(mWindow, mSeq, mWindowAttributes, getHostVisibility(), mDisplay.getDisplayId(), mWinFrame,
                            mAttachInfo.mContentInsets, mAttachInfo.mStableInsets, mAttachInfo.mOutsets, mAttachInfo.mDisplayCutout, mInputChannel);
                } catch (RemoteException e) {
                    mAdded = false;
                    mView = null;
                    mAttachInfo.mRootView = null;
                    mInputChannel = null;
                    mFallbackEventHandler.setView(null);
                    unscheduleTraversals();         //取消View绘制的操作
                    setAccessibilityFocus(null, null);
                    throw new RuntimeException("Adding window failed", e);
                } finally {
                    if (restore) {
                        attrs.restore();
                    }
                }
                if (mTranslator != null) {
                    mTranslator.translateRectInScreenToAppWindow(mAttachInfo.mContentInsets);
                }
                mPendingOverscanInsets.set(0, 0, 0, 0);
                mPendingContentInsets.set(mAttachInfo.mContentInsets);
                mPendingStableInsets.set(mAttachInfo.mStableInsets);
                mPendingDisplayCutout.set(mAttachInfo.mDisplayCutout);
                mPendingVisibleInsets.set(0, 0, 0, 0);
                mAttachInfo.mAlwaysConsumeNavBar = (res & WindowManagerGlobal.ADD_FLAG_ALWAYS_CONSUME_NAV_BAR) != 0;
                mPendingAlwaysConsumeNavBar = mAttachInfo.mAlwaysConsumeNavBar;    
                if (DEBUG_LAYOUT) Log.v(mTag, "Added window " + mWindow);
                if (res < WindowManagerGlobal.ADD_OKAY) {   //add 失败
                    mAttachInfo.mRootView = null;
                    mAdded = false;
                    mFallbackEventHandler.setView(null);
                    unscheduleTraversals();
                    setAccessibilityFocus(null, null);
                    switch (res) {
                        case WindowManagerGlobal.ADD_BAD_APP_TOKEN:
                        case WindowManagerGlobal.ADD_BAD_SUBWINDOW_TOKEN:
                            throw new WindowManager.BadTokenException("Unable to add window -- token " + attrs.token + " is not valid; is your activity running?");
                        case WindowManagerGlobal.ADD_NOT_APP_TOKEN:
                            throw new WindowManager.BadTokenException("Unable to add window -- token " + attrs.token + " is not for an application");
                        case WindowManagerGlobal.ADD_APP_EXITING:
                            throw new WindowManager.BadTokenException("Unable to add window -- app for token " + attrs.token + " is exiting");
                        case WindowManagerGlobal.ADD_DUPLICATE_ADD:
                            throw new WindowManager.BadTokenException("Unable to add window -- window " + mWindow + " has already been added");
                        case WindowManagerGlobal.ADD_STARTING_NOT_NEEDED:
                            //静默忽略-无论如何，我们将立即删除它
                            return;
                        case WindowManagerGlobal.ADD_MULTIPLE_SINGLETON:
                            throw new WindowManager.BadTokenException("Unable to add window " + mWindow + " -- another window of type " + mWindowAttributes.type + " already exists");
                        case WindowManagerGlobal.ADD_PERMISSION_DENIED:
                            throw new WindowManager.BadTokenException("Unable to add window " + mWindow + " -- permission denied for window type " + mWindowAttributes.type);
                        case WindowManagerGlobal.ADD_INVALID_DISPLAY:
                            throw new WindowManager.InvalidDisplayException("Unable to add window " + mWindow + " -- the specified display can not be found");
                        case WindowManagerGlobal.ADD_INVALID_TYPE:
                            throw new WindowManager.InvalidDisplayException("Unable to add window " + mWindow + " -- the specified window type " + mWindowAttributes.type + " is not valid");
                    }
                    throw new RuntimeException("Unable to add window -- unknown error code " + res);
                }  
                if (view instanceof RootViewSurfaceTaker) {
                    mInputQueueCallback = ((RootViewSurfaceTaker)view).willYouTakeTheInputQueue();
                }
                if (mInputChannel != null) {
                    if (mInputQueueCallback != null) {
                        mInputQueue = new InputQueue();
                        mInputQueueCallback.onInputQueueCreated(mInputQueue);
                    }
                    mInputEventReceiver = new WindowInputEventReceiver(mInputChannel, Looper.myLooper());
                }
                view.assignParent(this);        //给View即DecorView设置parent
                mAddedTouchMode = (res & WindowManagerGlobal.ADD_FLAG_IN_TOUCH_MODE) != 0;
                mAppVisible = (res & WindowManagerGlobal.ADD_FLAG_APP_VISIBLE) != 0;
                if (mAccessibilityManager.isEnabled()) {
                    mAccessibilityInteractionConnectionManager.ensureConnection();
                }
                if (view.getImportantForAccessibility() == View.IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
                    view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
                }
                //设置输入管道，主要给最后三项赋值
                CharSequence counterSuffix = attrs.getTitle();
                mSyntheticInputStage = new SyntheticInputStage();
                InputStage viewPostImeStage = new ViewPostImeInputStage(mSyntheticInputStage);
                InputStage nativePostImeStage = new NativePostImeInputStage(viewPostImeStage, "aq:native-post-ime:" + counterSuffix);
                InputStage earlyPostImeStage = new EarlyPostImeInputStage(nativePostImeStage);
                InputStage imeStage = new ImeInputStage(earlyPostImeStage, "aq:ime:" + counterSuffix);
                InputStage viewPreImeStage = new ViewPreImeInputStage(imeStage);        //使用了Wrapper模式
                InputStage nativePreImeStage = new NativePreImeInputStage(viewPreImeStage, "aq:native-pre-ime:" + counterSuffix);
                mFirstInputStage = nativePreImeStage;
                mFirstPostImeInputStage = earlyPostImeStage;
                mPendingInputEventQueueLengthCounterName = "aq:pending:" + counterSuffix;
            }
        }
    在Window中添加View时，调用addView，通过requestLayout触发了View的绘制
    最终都是调用ViewRootImpl的scheduleTraversals进行View绘制的
    
#### 真正的View绘制流程

    ViewRootImpl.scheduleTraversals：
        if (!mTraversalScheduled) {     //true表示将要进行View绘制，在取消或者进行绘制时置为false
            mTraversalScheduled = true;
            //postSyncBarrier，将同步屏障发布到Looper的消息队列中。 消息处理将照常进行，直到消息队列遇到已发布的同步屏障。 
            //遇到障碍时，队列中稍后的同步消息将被暂停（防止执行），直到通过调用removeSyncBarrier并指定标识同步障碍的令牌来释放该障碍。 
            //此方法用于立即推迟所有后续发布的同步消息的执行，直到满足释放障碍的条件为止。 异步消息（请参阅Message.isAsynchronous被免除障碍，并继续照常进行处理。
            //此调用必须始终与具有相同令牌的removeSyncBarrier调用相匹配，以确保消息队列恢复正常操作。否则，应用程序可能会 挂！
            mTraversalBarrier = mHandler.getLooper().getQueue().postSyncBarrier();
            //在指定的延迟后发布回调（Runnable）以在下一帧上运行（垂直同步？）。回调运行一次，然后会自动删除
            mChoreographer.postCallback(Choreographer.CALLBACK_TRAVERSAL, mTraversalRunnable, null);    //在下一次帧开始时执行Runnable
            if (!mUnbufferedInputDispatch) {
                scheduleConsumeBatchedInput();      //和事件分发有关
            }
            notifyRendererOfFramePending();     //通知HardwareRenderer即将进行新的帧绘制。当前，只有ThreadedRenderer关心此事，并使用此知识来调整线程外动画的调度
            pokeDrawLockIfNeeded();     //睡眠状态，唤醒？
        }
    Choreographer，协调动画、输入和绘图的时间。choreographer从显示子系统接收定时脉冲（例如垂直同步），然后安排工作以进行渲染下一个显示帧的一部分。
    应用程序通常使用动画框架或视图层次结构中的更高级别的抽象间接与choreographer交互。 以下是一些可以使用高级API进行操作的示例：
        - 要发布要与显示帧渲染同步的常规时间的动画，请使用ValueAnimator.start()
        - 要在下一个显示帧的开始处发布要被调用一次的Runnable，请使用View.postOnAnimation(Runnable)
        - 要在延迟后的下一个显示帧开始时发布要被调用一次的Runnable，请使用View.postOnAnimationDelayed(Runnable, long)
        - 若要将对View.invalidate()的调用发布到下一个显示帧的开头一次，请使用View.postInvalidateOnAnimation()或View.postInvalidateOnAnimation(int, int, int, int)
        - 为确保View的内容平滑滚动并与显示框架渲染同步绘制，请不要执行任何操作。 这已经自动发生。View.onDraw(Canvas)将在适当的时间被调用
    但是，在某些情况下，您可能希望直接在应用程序中使用choreographer的功能。这里有些例子：
        如果您的应用程序可能使用GL在不同的线程中进行渲染，或者根本不使用动画框架或视图层次结构，并且想要确保它与显示正确同步，请使用Choreographer.postFrameCallback(Choreographer.FrameCallback)
    每个Looper线程都有其自己的choreographer。其他线程可以发布回调以在choreographer上运行，但它们将在choreographer所属的Looper上运行
    scheduleTraversals中将mTraversalRunnable发布到Choreographer中的Handler消息队列中，在下一次帧开始时进行Runnable
    
    现在看这个Runnable，mTraversalRunnable = new TraversalRunnable()，TraversalRunnable的run方法中只调用了doTraversal
    ViewRootImpl.doTraversal：
        if (mTraversalScheduled) {      //在scheduleTraversals中被置为true
            mTraversalScheduled = false;    //置为false，则可以进行下一次的scheduleTraversals
            mHandler.getLooper().getQueue().removeSyncBarrier(mTraversalBarrier);   //释放同步屏障，使消息队列恢复正常
            if (mProfile) {
                Debug.startMethodTracing("ViewAncestor");   //方法追踪
            }
            performTraversals();    //进入绘制流程
            if (mProfile) {
                Debug.stopMethodTracing();  //结束追踪
                mProfile = false;
            }
        }
    scheduleTraversals在每一帧期间只能调用一次（除非被取消），在每一帧的开始时执行scheduleTraversals中的runnable，然后就可以进行下一次的scheduleTraversals
    ViewRootImpl.performTraversals：
        //缓存mView，因为它在下面经常使用...
        final View host = mView;
        ...
        mIsInTraversal = true;      //在traversal中
        mWillDrawSoon = true;
        boolean windowSizeMayChange = false;
        boolean newSurface = false;
        boolean surfaceChanged = false;
        WindowManager.LayoutParams lp = mWindowAttributes;
        int desiredWindowWidth;
        int desiredWindowHeight;
        final int viewVisibility = getHostVisibility();     //当前页面是否可见
        final boolean viewVisibilityChanged = !mFirst && (mViewVisibility != viewVisibility || mNewSurfaceNeeded
                //另外，请检查是否存在双重可见性更新，这将使当前的viewVisibility值等于mViewVisibility，我们可能会错过它
                || mAppVisibilityChanged);      //当前页面可见状态是否改变了
        mAppVisibilityChanged = false;
        final boolean viewUserVisibilityChanged = !mFirst && ((mViewVisibility == View.VISIBLE) != (viewVisibility == View.VISIBLE));
        WindowManager.LayoutParams params = null;
        if (mWindowAttributesChanged) {
            mWindowAttributesChanged = false;   //窗口属性有改变，则surface也有改变？
            surfaceChanged = true;
            params = lp;
        }
        CompatibilityInfo compatibilityInfo = mDisplay.getDisplayAdjustments().getCompatibilityInfo();
        if (compatibilityInfo.supportsScreen() == mLastInCompatMode) {      //兼容模式？
            params = lp;
            mFullRedrawNeeded = true;
            mLayoutRequested = true;
            if (mLastInCompatMode) {
                params.privateFlags &= ~WindowManager.LayoutParams.PRIVATE_FLAG_COMPATIBLE_WINDOW;
                mLastInCompatMode = false;
            } else {
                params.privateFlags |= WindowManager.LayoutParams.PRIVATE_FLAG_COMPATIBLE_WINDOW;
                mLastInCompatMode = true;
            }
        }
        mWindowAttributesChangesFlag = 0;
        Rect frame = mWinFrame;         //帧
        if (mFirst) {       //mFirst初始为true，表示第一次添加View
            mFullRedrawNeeded = true;
            mLayoutRequested = true;
            final Configuration config = mContext.getResources().getConfiguration();
            if (shouldUseDisplaySize(lp)) {     //LayoutParams类型为TYPE_STATUS_BAR_PANEL、TYPE_INPUT_METHOD或TYPE_VOLUME_OVERLAY
                //注意-系统代码将不会尝试执行兼容模式
                Point size = new Point();
                mDisplay.getRealSize(size);     //display为上述类型，状态栏面板、输入法、音量窗口？
                desiredWindowWidth = size.x;    //窗口宽和高
                desiredWindowHeight = size.y;
            } else {
                desiredWindowWidth = mWinFrame.width();
                desiredWindowHeight = mWinFrame.height();   //窗口宽和高
            }
            //我们曾经使用以下条件来选择32位图形缓存：PixelFormat.hasAlpha（lp.format）|| lp.format == PixelFormat.RGBX_8888，但是，默认情况下，窗口现在始终为32位，因此请选择32位
            mAttachInfo.mUse32BitDrawingCache = true;
            mAttachInfo.mHasWindowFocus = false;
            mAttachInfo.mWindowVisibility = viewVisibility;
            mAttachInfo.mRecomputeGlobalAttributes = false;
            mLastConfigurationFromResources.setTo(config);
            mLastSystemUiVisibility = mAttachInfo.mSystemUiVisibility;
            //如果尚未设置布局方向，则设置布局方向（继承是默认设置）
            if (mViewLayoutDirectionInitial == View.LAYOUT_DIRECTION_INHERIT) {
                host.setLayoutDirection(config.getLayoutDirection());   //设置布局方向，host是DecorView
            }
            host.dispatchAttachedToWindow(mAttachInfo, 0);      //内部会调用到View的onAttachedToWindow、OnAttachStateChangeListener回调、onWindowVisibilityChanged、onVisibilityChanged等
            mAttachInfo.mTreeObserver.dispatchOnWindowAttachedChange(true); //onWindowAttached、onWindowDetached状态
            dispatchApplyInsets(host);  //与fitSystemWindow有关？
        } else {    //不是第一次绘制了
            desiredWindowWidth = frame.width();
            desiredWindowHeight = frame.height();
            if (desiredWindowWidth != mWidth || desiredWindowHeight != mHeight) {   //Window大小有变
                mFullRedrawNeeded = true;
                mLayoutRequested = true;
                windowSizeMayChange = true;
            }
        }
        if (viewVisibilityChanged) {
            mAttachInfo.mWindowVisibility = viewVisibility;
            host.dispatchWindowVisibilityChanged(viewVisibility);   //View的onWindowVisibilityChanged被调用，会被传递至各个View
            if (viewUserVisibilityChanged) {
                host.dispatchVisibilityAggregated(viewVisibility == View.VISIBLE);  //View内部调用的？ViewGroup使用？
            }
            if (viewVisibility != View.VISIBLE || mNewSurfaceNeeded) {
                endDragResizing();      //结束拖动调整大小，这将通知所有侦听器窗口调整大小已结束。
                destroyHardwareResources();     //销毁硬件资源，ThreadedRenderer.destroy
            }
            if (viewVisibility == View.GONE) {
                //使窗口消失后，下次将其聚焦时，我们将其视为首次显示
                mHasHadWindowFocus = false;
            }
        }
        //不可见的窗口不能保持可访问性焦点
        if (mAttachInfo.mWindowVisibility != View.VISIBLE) {
            host.clearAccessibilityFocus();     //拥有焦点，并且希望parent寻找下一个焦点？
        }
        //在每个遍历上执行队列中的actions，以防detached view将一个action入队
        getRunQueue().executeActions(mAttachInfo.mHandler);
        boolean insetsChanged = false;
        //mLayoutRequested在requestLayout或上面的mFirst为true或窗口大小有变时为true，mStopped代表当前窗口是否处于stop状态，mReportNextDraw在reportNextDraw中设为true
        boolean layoutRequested = mLayoutRequested && (!mStopped || mReportNextDraw);   //窗口活跃时为true
        if (layoutRequested) {
            final Resources res = mView.getContext().getResources();
            if (mFirst) {
                //通过将缓存的值设置为与添加的触摸模式相反的值，确保执行触摸模式代码
                mAttachInfo.mInTouchMode = !mAddedTouchMode;
                ensureTouchModeLocally(mAddedTouchMode);    //确保已设置此窗口的触摸模式，并且如果正在更改，请采取适当的措施
            } else {
                if (!mPendingOverscanInsets.equals(mAttachInfo.mOverscanInsets)) insetsChanged = true;
                if (!mPendingContentInsets.equals(mAttachInfo.mContentInsets)) insetsChanged = true;
                if (!mPendingStableInsets.equals(mAttachInfo.mStableInsets)) insetsChanged = true;
                if (!mPendingDisplayCutout.equals(mAttachInfo.mDisplayCutout)) insetsChanged = true;
                if (!mPendingVisibleInsets.equals(mAttachInfo.mVisibleInsets)) {
                    mAttachInfo.mVisibleInsets.set(mPendingVisibleInsets);
                    if (DEBUG_LAYOUT) Log.v(mTag, "Visible insets changing to: " + mAttachInfo.mVisibleInsets);
                }
                if (!mPendingOutsets.equals(mAttachInfo.mOutsets)) insetsChanged = true;
                if (mPendingAlwaysConsumeNavBar != mAttachInfo.mAlwaysConsumeNavBar) insetsChanged = true;
                if (lp.width == ViewGroup.LayoutParams.WRAP_CONTENT || lp.height == ViewGroup.LayoutParams.WRAP_CONTENT) {
                    windowSizeMayChange = true;
                    if (shouldUseDisplaySize(lp)) {     //display类型为悬浮窗类？
                        //注意-系统代码将不会尝试执行兼容模式
                        Point size = new Point();
                        mDisplay.getRealSize(size);
                        desiredWindowWidth = size.x;
                        desiredWindowHeight = size.y;
                    } else {
                        Configuration config = res.getConfiguration();
                        desiredWindowWidth = dipToPx(config.screenWidthDp);     //dp转px
                        desiredWindowHeight = dipToPx(config.screenHeightDp);
                    }
                }
            }
            //询问host（DecorView）想要多大
            windowSizeMayChange |= measureHierarchy(host, lp, res, desiredWindowWidth, desiredWindowHeight);    //内部调用performMeasure，View的measure、onMeasure
        }
        if (collectViewAttributes()) {      //给AttachInfo赋值
            params = lp;
        }
        if (mAttachInfo.mForceReportNewAttributes) {
            mAttachInfo.mForceReportNewAttributes = false;
            params = lp;
        }
        if (mFirst || mAttachInfo.mViewVisibilityChanged) {
            mAttachInfo.mViewVisibilityChanged = false;
            int resizeMode = mSoftInputMode & WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST;    //输入法适应屏幕？
            //如果我们处于自动调整大小模式，则需要确定现在使用哪种模式
            if (resizeMode == WindowManager.LayoutParams.SOFT_INPUT_ADJUST_UNSPECIFIED) {   //自动调整大小
                final int N = mAttachInfo.mScrollContainers.size();
                for (int i=0; i<N; i++) {
                    if (mAttachInfo.mScrollContainers.get(i).isShown()) {   //ScrollView内的View？
                        resizeMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;   //在显示输入法时调整窗口大小，以使输入法不覆盖其内容，如果窗口的布局参数标志包括FLAG_FULLSCREEN，则softInputMode的此值将被忽略；窗口不会调整大小，但会保持全屏显示。
                    }
                }
                if (resizeMode == 0) {
                    resizeMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN;    //在显示输入法时有一个窗口平移，因此它不需要处理调整大小，而只是由框架平移以确保当前输入焦点可见。不能与SOFT_INPUT_ADJUST_RESIZE结合使用；如果未设置任何一个，则系统将尝试根据窗口的内容选择一个或另一个。
                }
                if ((lp.softInputMode & WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST) != resizeMode) {
                    lp.softInputMode = (lp.softInputMode & ~WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST) | resizeMode;    //设置输入法显示模式
                    params = lp;
                }
            }
        }
        if (params != null) {
            if ((host.mPrivateFlags & View.PFLAG_REQUEST_TRANSPARENT_REGIONS) != 0) {
                if (!PixelFormat.formatHasAlpha(params.format)) {
                    params.format = PixelFormat.TRANSLUCENT;    //params.format为期望的位图格式，默认为OPAQUE，系统选择TRANSLUCENT半透明
                }
            }
            mAttachInfo.mOverscanRequested = (params.flags & WindowManager.LayoutParams.FLAG_LAYOUT_IN_OVERSCAN) != 0;  //是否通过FLAG_LAYOUT_IN_OVERSCAN请求窗口扩展到过扫描区域
        }
        if (mApplyInsetsRequested) {
            mApplyInsetsRequested = false;
            mLastOverscanRequested = mAttachInfo.mOverscanRequested;
            dispatchApplyInsets(host);  //请求将给定的窗口插图应用于此视图或其子树中的另一个视图，此方法替代了较旧的fitSystemWindows方法。
            if (mLayoutRequested) {
                //Short-circuit会在这里捕获新的布局请求，因此当由于安装了合适的系统窗口而发生更改时，我们不需要经过两次布局遍历。
                windowSizeMayChange |= measureHierarchy(host, lp, mView.getContext().getResources(), desiredWindowWidth, desiredWindowHeight);
            }
        }
        if (layoutRequested) {
            //立即清除此内容，以便在此功能的其余部分中如果有任何请求布局的内容，我们将抓住它并重新运行完整的布局过程
            mLayoutRequested = false;
        }
        boolean windowShouldResize = layoutRequested && windowSizeMayChange && ((mWidth != host.getMeasuredWidth() || mHeight != host.getMeasuredHeight())
                || (lp.width == ViewGroup.LayoutParams.WRAP_CONTENT && frame.width() < desiredWindowWidth && frame.width() != mWidth)
                || (lp.height == ViewGroup.LayoutParams.WRAP_CONTENT && frame.height() < desiredWindowHeight && frame.height() != mHeight));    //是否需要重新测量的条件判断
        windowShouldResize |= mDragResizing && mResizeMode == RESIZE_MODE_FREEFORM;
        //如果只是重新启动了该Activity，则它可能已经冻结了任务边界（在重新启动时），因此我们需要强制调用窗口管理器以获取最新的边界
        windowShouldResize |= mActivityRelaunched;
        //确定是否计算insets。如果没有剩余的inset侦听器，那么我们可能仍需要计算inset，以防旧的inset为非空且必须重置
        final boolean computesInternalInsets = mAttachInfo.mTreeObserver.hasComputeInternalInsetsListeners() || mAttachInfo.mHasNonEmptyGivenInternalInsets;
        boolean insetsPending = false;
        int relayoutResult = 0;     //layout的结果？
        boolean updatedConfiguration = false;   //更新配置？
        final int surfaceGenerationId = mSurface.getGenerationId();
        final boolean isViewVisible = viewVisibility == View.VISIBLE;
        final boolean windowRelayoutWasForced = mForceNextWindowRelayout;   //是否强制重新layout？
        if (mFirst || windowShouldResize || insetsChanged || viewVisibilityChanged || params != null || mForceNextWindowRelayout) {   //满足条件则进行Measure？
            mForceNextWindowRelayout = false;
            if (isViewVisible) {
                //如果此窗口正在为窗口管理器提供内部插图，并且正在添加窗口或更改其可见性，那么我们要首先给窗口管理器提供“伪”插图，以使其在布局过程中有效地忽略窗口的内容。 这样可以避免它短暂地导致其他窗口根据窗口的原始框架调整大小/移动，而不必等到我们可以完成对这个窗口的布局并返回到带有最终计算出的插图的窗口管理器。
                insetsPending = computesInternalInsets && (mFirst || viewVisibilityChanged);    //占位？
            }        
            if (mSurfaceHolder != null) {
                mSurfaceHolder.mSurfaceLock.lock();
                mDrawingAllowed = true;
            }
            boolean hwInitialized = false;
            boolean contentInsetsChanged = false;
            boolean hadSurface = mSurface.isValid();
            try {
                if (DEBUG_LAYOUT) Log.i(mTag, "host=w:" + host.getMeasuredWidth() + ", h:" + host.getMeasuredHeight() + ", params=" + params);
                if (mAttachInfo.mThreadedRenderer != null) {
                    //relayoutWindow可能决定销毁mSurface。 由于该决定是在WindowManager服务中发生的，因此我们需要在此采取防御措施，并在损坏surface时停止使用该surface
                    if (mAttachInfo.mThreadedRenderer.pauseSurface(mSurface)) {     //停止将任何当前渲染渲染到surface中。如果不清楚ThreadedRenderer使用的surface是否会改变，请使用此选项。它会将所有渲染暂停到表面，但不会造成任何破坏。任何后续draw将覆盖暂停，恢复正常操作。
                        //动画正在运行，因此我们需要推动一个帧以恢复它们
                        mDirty.set(0, 0, mWidth, mHeight);
                    }
                    mChoreographer.mFrameInfo.addFlags(FrameInfo.FLAG_WINDOW_LAYOUT_CHANGED);
                }
                relayoutResult = relayoutWindow(params, viewVisibility, insetsPending);
                //如果从relayoutWindow移交的未决MergedConfiguration与上次报告的不匹配，则WindowManagerService已从客户端尚未处理的配置中报告了一个帧。在这种情况下，我们需要接受配置，因此我们不会布局和绘制错误的配置
                if (!mPendingMergedConfiguration.equals(mLastReportedMergedConfiguration)) {
                    performConfigurationChange(mPendingMergedConfiguration, !mFirst, INVALID_DISPLAY /* same display */);   //通知所有回调配置更改
                    updatedConfiguration = true;
                }
                final boolean overscanInsetsChanged = !mPendingOverscanInsets.equals(mAttachInfo.mOverscanInsets);
                contentInsetsChanged = !mPendingContentInsets.equals(mAttachInfo.mContentInsets);
                final boolean visibleInsetsChanged = !mPendingVisibleInsets.equals(mAttachInfo.mVisibleInsets);
                final boolean stableInsetsChanged = !mPendingStableInsets.equals(mAttachInfo.mStableInsets);
                final boolean cutoutChanged = !mPendingDisplayCutout.equals(mAttachInfo.mDisplayCutout);
                final boolean outsetsChanged = !mPendingOutsets.equals(mAttachInfo.mOutsets);
                final boolean surfaceSizeChanged = (relayoutResult & WindowManagerGlobal.RELAYOUT_RES_SURFACE_RESIZED) != 0;
                surfaceChanged |= surfaceSizeChanged;
                final boolean alwaysConsumeNavBarChanged = mPendingAlwaysConsumeNavBar != mAttachInfo.mAlwaysConsumeNavBar;
                if (contentInsetsChanged) {
                    mAttachInfo.mContentInsets.set(mPendingContentInsets);
                    if (DEBUG_LAYOUT) Log.v(mTag, "Content insets changing to: " + mAttachInfo.mContentInsets);
                }
                if (overscanInsetsChanged) {
                    mAttachInfo.mOverscanInsets.set(mPendingOverscanInsets);
                    if (DEBUG_LAYOUT) Log.v(mTag, "Overscan insets changing to: " + mAttachInfo.mOverscanInsets);
                    //需要重新布局内容插图
                    contentInsetsChanged = true;
                }
                if (stableInsetsChanged) {
                    mAttachInfo.mStableInsets.set(mPendingStableInsets);
                    if (DEBUG_LAYOUT) Log.v(mTag, "Decor insets changing to: " + mAttachInfo.mStableInsets);
                    // Need to relayout with content insets.
                    contentInsetsChanged = true;
                }
                if (cutoutChanged) {
                    mAttachInfo.mDisplayCutout.set(mPendingDisplayCutout);
                    // Need to relayout with content insets.
                    contentInsetsChanged = true;
                }
                if (alwaysConsumeNavBarChanged) {
                    mAttachInfo.mAlwaysConsumeNavBar = mPendingAlwaysConsumeNavBar;
                    contentInsetsChanged = true;
                }
                if (contentInsetsChanged || mLastSystemUiVisibility != mAttachInfo.mSystemUiVisibility || mApplyInsetsRequested
                        || mLastOverscanRequested != mAttachInfo.mOverscanRequested || outsetsChanged) {
                    mLastSystemUiVisibility = mAttachInfo.mSystemUiVisibility;
                    mLastOverscanRequested = mAttachInfo.mOverscanRequested;
                    mAttachInfo.mOutsets.set(mPendingOutsets);
                    mApplyInsetsRequested = false;
                    dispatchApplyInsets(host);      //fitSystemWindow
                }
                if (visibleInsetsChanged) {
                    mAttachInfo.mVisibleInsets.set(mPendingVisibleInsets);
                    if (DEBUG_LAYOUT) Log.v(mTag, "Visible insets changing to: " + mAttachInfo.mVisibleInsets);
                }   
                if (!hadSurface) {
                    if (mSurface.isValid()) {
                        //如果要创建一个新surface，则需要完全重绘它。 同样，当我们到达绘制点时，我们将推迟并安排新的遍历。 这样一来，我们可以在实际绘制窗口之前告诉窗口管理器所有正在显示的窗口，这样它就可以一次显示所有窗口。
                        newSurface = true;
                        mFullRedrawNeeded = true;
                        mPreviousTransparentRegion.setEmpty();
                        //仅在不要求透明区域的情况下预先初始化，否则请推迟去看整个窗口是否透明
                        if (mAttachInfo.mThreadedRenderer != null) {
                            try {
                                hwInitialized = mAttachInfo.mThreadedRenderer.initialize(mSurface);
                                if (hwInitialized && (host.mPrivateFlags & View.PFLAG_REQUEST_TRANSPARENT_REGIONS) == 0) {
                                    //如果要求透明区域，则不要预先分配，因为可能不需要透明区域
                                    mSurface.allocateBuffers();     //提前分配缓冲区以避免渲染期间的分配延迟
                                }
                            } catch (OutOfResourcesException e) {
                                handleOutOfResourcesException(e);
                                return;
                            }
                        }
                    }
                } else if (!mSurface.isValid()) {   //surface不可用
                    //如果surface已被移除，则重置scroll位置
                    if (mLastScrolledFocus != null) {
                        mLastScrolledFocus.clear();
                    }
                    mScrollY = mCurScrollY = 0;
                    if (mView instanceof RootViewSurfaceTaker) {
                        ((RootViewSurfaceTaker) mView).onRootViewScrollYChanged(mCurScrollY);
                    }
                    if (mScroller != null) {
                        mScroller.abortAnimation();     //停止动画
                    }
                    //surface is gone
                    if (mAttachInfo.mThreadedRenderer != null && mAttachInfo.mThreadedRenderer.isEnabled()) {
                        mAttachInfo.mThreadedRenderer.destroy();    //回收，渲染线程
                    }
                } else if ((surfaceGenerationId != mSurface.getGenerationId() || surfaceSizeChanged || windowRelayoutWasForced)
                        && mSurfaceHolder == null && mAttachInfo.mThreadedRenderer != null) {
                    mFullRedrawNeeded = true;
                    try {
                        //如果更改了Surface（如generation ID所示）或WindowManager更改了Surface尺寸，则需要执行updateSurface（导致CanvasContext :: setSurface并重新创建EGLSurface）。
                        //后者是因为在某些芯片上，除非我们创建新的EGLSurface，否则更改用户端BufferQueue的大小可能不会立即生效。 请注意，frame尺寸的改变并不总是意味着Surface尺寸的改变（例如，拖动调整大小使用全屏Surface），需要从WindowManager中检查surfaceSizeChanged标志。
                        mAttachInfo.mThreadedRenderer.updateSurface(mSurface);  //更新指定surface的线程渲染器
                    } catch (OutOfResourcesException e) {
                        handleOutOfResourcesException(e);
                        return;
                    }
                }
                final boolean freeformResizing = (relayoutResult & WindowManagerGlobal.RELAYOUT_RES_DRAG_RESIZING_FREEFORM) != 0;
                final boolean dockedResizing = (relayoutResult & WindowManagerGlobal.RELAYOUT_RES_DRAG_RESIZING_DOCKED) != 0;
                final boolean dragResizing = freeformResizing || dockedResizing;
                if (mDragResizing != dragResizing) {
                    if (dragResizing) {
                        mResizeMode = freeformResizing ? RESIZE_MODE_FREEFORM : RESIZE_MODE_DOCKED_DIVIDER;
                        // TODO: Need cutout?
                        startDragResizing(mPendingBackDropFrame, mWinFrame.equals(mPendingBackDropFrame), mPendingVisibleInsets, mPendingStableInsets, mResizeMode);    //启动拖动调整大小，这将通知所有侦听器正在调整窗口大小。onWindowDragResizeStart
                    } else {
                        //我们不应该来这里，但是如果我们来了，我们应该结束调整大小。
                        endDragResizing();      //onWindowDragResizeEnd
                    }
                }
                if (!mUseMTRenderer) {
                    if (dragResizing) {     //拖动位置调整？
                        mCanvasOffsetX = mWinFrame.left;
                        mCanvasOffsetY = mWinFrame.top;
                    } else {
                        mCanvasOffsetX = mCanvasOffsetY = 0;
                    }
                }
            } catch (RemoteException e) {
            }
            if (DEBUG_ORIENTATION) Log.v(TAG, "Relayout returned: frame=" + frame + ", surface=" + mSurface);
            mAttachInfo.mWindowLeft = frame.left;
            mAttachInfo.mWindowTop = frame.top;
            // !!FIXME!! 下一部分将处理我们未获得所需窗口大小的情况。我们应该通过事先从窗口会话中获取最大大小来避免这种情况。
            if (mWidth != frame.width() || mHeight != frame.height()) {
                mWidth = frame.width();
                mHeight = frame.height();
            }        
            if (mSurfaceHolder != null) {
                //该应用拥有surface；告诉它发生了什么
                if (mSurface.isValid()) {
                    //mSurfaceHolder.mSurface.copyFrom(mSurface);   // XXX .copyFrom() doesn't work!
                    mSurfaceHolder.mSurface = mSurface;
                }
                mSurfaceHolder.setSurfaceFrameSize(mWidth, mHeight);    //设置Rect
                mSurfaceHolder.mSurfaceLock.unlock();   //解锁
                if (mSurface.isValid()) {
                    if (!hadSurface) {
                        mSurfaceHolder.ungetCallbacks();
                        mIsCreating = true;
                        SurfaceHolder.Callback callbacks[] = mSurfaceHolder.getCallbacks();
                        if (callbacks != null) {
                            for (SurfaceHolder.Callback c : callbacks) {
                                c.surfaceCreated(mSurfaceHolder);       //surface的surfaceCreated回调，在照相机中有用到
                            }
                        }
                        surfaceChanged = true;
                    }
                    if (surfaceChanged || surfaceGenerationId != mSurface.getGenerationId()) {
                        SurfaceHolder.Callback callbacks[] = mSurfaceHolder.getCallbacks();
                        if (callbacks != null) {
                            for (SurfaceHolder.Callback c : callbacks) {
                                c.surfaceChanged(mSurfaceHolder, lp.format, mWidth, mHeight);   //surface的surfaceChanged回调
                            }
                        }
                    }
                    mIsCreating = false;
                } else if (hadSurface) {
                    mSurfaceHolder.ungetCallbacks();
                    SurfaceHolder.Callback callbacks[] = mSurfaceHolder.getCallbacks();
                    if (callbacks != null) {
                        for (SurfaceHolder.Callback c : callbacks) {
                            c.surfaceDestroyed(mSurfaceHolder);     //surface的surfaceDestroyed回调
                        }
                    }
                    mSurfaceHolder.mSurfaceLock.lock();
                    try {
                        mSurfaceHolder.mSurface = new Surface();        //新建？
                    } finally {
                        mSurfaceHolder.mSurfaceLock.unlock();
                    }
                }
            }
            final ThreadedRenderer threadedRenderer = mAttachInfo.mThreadedRenderer;
            if (threadedRenderer != null && threadedRenderer.isEnabled()) {
                if (hwInitialized || mWidth != threadedRenderer.getWidth() || mHeight != threadedRenderer.getHeight() || mNeedsRendererSetup) {
                    threadedRenderer.setup(mWidth, mHeight, mAttachInfo, mWindowAttributes.surfaceInsets);   //设置绘图的渲染器
                    mNeedsRendererSetup = false;
                }
            }
            if (!mStopped || mReportNextDraw) {
                boolean focusChangedDueToTouchMode = ensureTouchModeLocally((relayoutResult&WindowManagerGlobal.RELAYOUT_RES_IN_TOUCH_MODE) != 0);  //确保已设置此窗口的触摸模式，并且如果正在更改，请采取适当的措施。
                if (focusChangedDueToTouchMode || mWidth != host.getMeasuredWidth() || mHeight != host.getMeasuredHeight() || contentInsetsChanged || updatedConfiguration) {
                    int childWidthMeasureSpec = getRootMeasureSpec(mWidth, lp.width);
                    int childHeightMeasureSpec = getRootMeasureSpec(mHeight, lp.height);
                    //Ooops, something changed!  mWidth=" + mWidth + " measuredWidth=" + host.getMeasuredWidth() + " mHeight=" + mHeight + " measuredHeight=" + host.getMeasuredHeight() + " coveredInsetsChanged=" + contentInsetsChanged);
                    //询问host（DecorView）想要多大
                    performMeasure(childWidthMeasureSpec, childHeightMeasureSpec);      //View的尺寸测量
                    //从WindowManager.LayoutParams实现权重我们只是根据需要增加尺寸，如果需要则重新测量
                    int width = host.getMeasuredWidth();
                    int height = host.getMeasuredHeight();
                    boolean measureAgain = false;
                    if (lp.horizontalWeight > 0.0f) {
                        width += (int) ((mWidth - width) * lp.horizontalWeight);
                        childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
                        measureAgain = true;    //对于有weight的的View要重新测量一次
                    }
                    if (lp.verticalWeight > 0.0f) {
                        height += (int) ((mHeight - height) * lp.verticalWeight);
                        childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
                        measureAgain = true;    ////对于有weight的的View要重新测量一次，如LinearLayout中的View
                    }
                    if (measureAgain) {
                        if (DEBUG_LAYOUT) Log.v(mTag, "And hey let's measure once more: width=" + width + " height=" + height);
                        performMeasure(childWidthMeasureSpec, childHeightMeasureSpec);
                    }
                    layoutRequested = true;
                }
            }
        } else {
            //不是第一次通过，并且 window/insets/visibility 没有变化，但是窗口可能已经移动了，我们需要检查一下是否可以更新AttachInfo中的left right。 我们仅翻译window frame，因为在窗口移动时，窗口管理器仅告诉我们新frame，但是insets相同，因此我们不希望将它们翻译多次
            maybeHandleWindowMove(frame);   //窗口移动，作动画、更新位置？
        }
        final boolean didLayout = layoutRequested && (!mStopped || mReportNextDraw);
        boolean triggerGlobalLayoutListener = didLayout || mAttachInfo.mRecomputeGlobalAttributes;
        if (didLayout) {
            performLayout(lp, mWidth, mHeight);     //内部调用（DecorView）view的layout、onLayout
            //至此，所有视图的大小和位置都已确定，我们可以计算出透明区域
            if ((host.mPrivateFlags & View.PFLAG_REQUEST_TRANSPARENT_REGIONS) != 0) {   //有透明区域的flag
                //开始透明  TODO:通过缓存结果来避免调用?
                host.getLocationInWindow(mTmpLocation);     //计算此视图在其窗口中的坐标。参数必须是两个整数组成的数组。方法返回后，数组按该顺序包含x和y位置。
                mTransparentRegion.set(mTmpLocation[0], mTmpLocation[1], mTmpLocation[0] + host.mRight - host.mLeft, mTmpLocation[1] + host.mBottom - host.mTop);
                host.gatherTransparentRegion(mTransparentRegion);   //是否透明区域？
                if (mTranslator != null) {
                    mTranslator.translateRegionInWindowToScreen(mTransparentRegion);
                }
                if (!mTransparentRegion.equals(mPreviousTransparentRegion)) {
                    mPreviousTransparentRegion.set(mTransparentRegion);
                    mFullRedrawNeeded = true;
                    //重新配置窗口管理器
                    try {
                        mWindowSession.setTransparentRegion(mWindow, mTransparentRegion);
                    } catch (RemoteException e) {
                    }
                }
            }
            System.out.println("performTraversals -- after setFrame");
        }
        if (triggerGlobalLayoutListener) {
            mAttachInfo.mRecomputeGlobalAttributes = false;
            mAttachInfo.mTreeObserver.dispatchOnGlobalLayout();     //分发onGlobalLayout事件
        }
        if (computesInternalInsets) {
            //清除原始insets
            final ViewTreeObserver.InternalInsetsInfo insets = mAttachInfo.mGivenInternalInsets;
            insets.reset();
            //计算适当的新insets
            mAttachInfo.mTreeObserver.dispatchOnComputeInternalInsets(insets);
            mAttachInfo.mHasNonEmptyGivenInternalInsets = !insets.isEmpty();
            //告诉窗口管理器
            if (insetsPending || !mLastGivenInsets.equals(insets)) {
                mLastGivenInsets.set(insets);
                //翻译insets到屏幕坐标，如果需要
                final Rect contentInsets;
                final Rect visibleInsets;
                final Region touchableRegion;
                if (mTranslator != null) {
                    contentInsets = mTranslator.getTranslatedContentInsets(insets.contentInsets);
                    visibleInsets = mTranslator.getTranslatedVisibleInsets(insets.visibleInsets);
                    touchableRegion = mTranslator.getTranslatedTouchableArea(insets.touchableRegion);
                } else {
                    contentInsets = insets.contentInsets;
                    visibleInsets = insets.visibleInsets;
                    touchableRegion = insets.touchableRegion;
                }
                try {
                    mWindowSession.setInsets(mWindow, insets.mTouchableInsets, contentInsets, visibleInsets, touchableRegion);
                } catch (RemoteException e) {
                }
            }
        }
        if (mFirst) {
            if (sAlwaysAssignFocus || !isInTouchMode()) {
                //处理首次聚焦请求
                if (DEBUG_INPUT_RESIZE) Log.v(mTag, "First: mView.hasFocus()=" + mView.hasFocus());
                if (mView != null) {
                    if (!mView.hasFocus()) {
                        mView.restoreDefaultFocus();
                        if (DEBUG_INPUT_RESIZE) Log.v(mTag, "First: requested focused view=" + mView.findFocus());
                    } else {
                        if (DEBUG_INPUT_RESIZE) {
                            Log.v(mTag, "First: existing focused view=" + mView.findFocus());
                        }
                    }
                }
            } else {
                //某些视图（例如ScrollView）不会将焦点移到不在其viewport内的后代。 在布局之前，这些视图的大小为0，这是一个很好的变化，这意味着没有孩子可以获得焦点。 布局之后，此视图现在具有大小，但不能保证将焦点移交给可聚焦的子对象（特别是边缘情况，在该情况下，子对象在布局之前具有大小，因此不会触发focusableViewAvailable）
                View focused = mView.findFocus();
                if (focused instanceof ViewGroup && ((ViewGroup) focused).getDescendantFocusability() == ViewGroup.FOCUS_AFTER_DESCENDANTS) {
                    focused.restoreDefaultFocus();  //将焦点赋予以该视图为根的视图层次结构中的默认焦点视图。如果找不到默认焦点视图，则退回到调用requestFocus（int）
                }
            }
        }
        final boolean changedVisibility = (viewVisibilityChanged || mFirst) && isViewVisible;
        final boolean hasWindowFocus = mAttachInfo.mHasWindowFocus && isViewVisible;
        final boolean regainedFocus = hasWindowFocus && mLostWindowFocus;
        if (regainedFocus) {
            mLostWindowFocus = false;
        } else if (!hasWindowFocus && mHadWindowFocus) {
            mLostWindowFocus = true;
        }
        if (changedVisibility || regainedFocus) {
            //吐司以通知形式显示-也不要以窗口形式显示
            boolean isToast = (mWindowAttributes == null) ? false : (mWindowAttributes.type == WindowManager.LayoutParams.TYPE_TOAST);
            if (!isToast) {     //不是Toast
                host.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
            }
        }
        mFirst = false;
        mWillDrawSoon = false;
        mNewSurfaceNeeded = false;
        mActivityRelaunched = false;
        mViewVisibility = viewVisibility;
        mHadWindowFocus = hasWindowFocus;       //layout完成后，一些变量复位？
        if (hasWindowFocus && !isInLocalFocusMode()) {
            final boolean imTarget = WindowManager.LayoutParams
                    .mayUseInputMethod(mWindowAttributes.flags);
            if (imTarget != mLastWasImTarget) {
                mLastWasImTarget = imTarget;
                InputMethodManager imm = InputMethodManager.peekInstance();     //输入法相关
                if (imm != null && imTarget) {
                    imm.onPreWindowFocus(mView, hasWindowFocus);
                    imm.onPostWindowFocus(mView, mView.findFocus(), mWindowAttributes.softInputMode, !mHasHadWindowFocus, mWindowAttributes.flags);
                }
            }
        }
        //请记住，是否必须报告下一次draw
        if ((relayoutResult & WindowManagerGlobal.RELAYOUT_RES_FIRST_TIME) != 0) {  //非及时？
            reportNextDraw();   //延迟通知WM draw完成，直到调用pendingDrawFinished
        }
        //dispatchOnPreDraw，分发onPreDraw，通知注册的侦听器绘图过程即将开始。 如果侦听器返回true，则取消绘制过程并重新计划。 如果您要强制在未附加到Window或处于GONE状态的View或View的层次结构上进行绘图，则可以手动调用此方法。
        boolean cancelDraw = mAttachInfo.mTreeObserver.dispatchOnPreDraw() || !isViewVisible;
        if (!cancelDraw && !newSurface) {
            if (mPendingTransitions != null && mPendingTransitions.size() > 0) {
                for (int i = 0; i < mPendingTransitions.size(); ++i) {
                    mPendingTransitions.get(i).startChangingAnimations();   //动画？
                }
                mPendingTransitions.clear();
            }
            performDraw();  //内部调用到drawSoftware，从而调用到View的draw、onDraw等流程
        } else {
            if (isViewVisible) {
                //再试一次
                scheduleTraversals();
            } else if (mPendingTransitions != null && mPendingTransitions.size() > 0) {
                for (int i = 0; i < mPendingTransitions.size(); ++i) {
                    mPendingTransitions.get(i).endChangingAnimations();     //结束动画？
                }
                mPendingTransitions.clear();
            }
        }
        mIsInTraversal = false;     //View绘制流程结束
    View绘制的三个重要流程分别对应performMeasure、performLayout、performDraw
    对于performDraw流程，可以通过TreeObserver.dispatchOnPreDraw的OnPreDrawListener中拦截掉，取消performDraw
    至此，View的绘制暂时到此，performMeasure、performLayout、performDraw的细节，再论
    
#### 事件分发流程

    看ViewRootImpl的setView的最后一段：
    ViewRootImpl.setView：
        ...
        // Set up the input pipeline.
        CharSequence counterSuffix = attrs.getTitle();
        mSyntheticInputStage = new SyntheticInputStage();
        InputStage viewPostImeStage = new ViewPostImeInputStage(mSyntheticInputStage);
        InputStage nativePostImeStage = new NativePostImeInputStage(viewPostImeStage, "aq:native-post-ime:" + counterSuffix);
        InputStage earlyPostImeStage = new EarlyPostImeInputStage(nativePostImeStage);
        InputStage imeStage = new ImeInputStage(earlyPostImeStage, "aq:ime:" + counterSuffix);
        InputStage viewPreImeStage = new ViewPreImeInputStage(imeStage);
        InputStage nativePreImeStage = new NativePreImeInputStage(viewPreImeStage, "aq:native-pre-ime:" + counterSuffix);
        mFirstInputStage = nativePreImeStage;
        mFirstPostImeInputStage = earlyPostImeStage;
    SyntheticInputStage，从未处理的输入事件执行新输入事件的综合
    ViewPostImeInputStage，将post-ime输入事件传递到视图层次结构
    NativePostImeInputStage，将post-ime输入事件传递给一个native activity
    EarlyPostImeInputStage，进行post-ime输入事件的早期处理
    ImeInputStage，向ime提供输入事件。不支持指针事件
    ViewPreImeInputStage，将pre-ime输入事件发送到视图层次结构。不支持指针事件
    NativePreImeInputStage，将pre-ime输入事件传送到native activity。不支持指针事件
    这些类是事件处理的相关类，它们使用了Wrapper模式，实际用到的只有mSyntheticInputStage、nativePreImeStage、earlyPostImeStage
    nativePreImeStage是包装后的最终类
    
    由一系列的传感器或其他硬件处理后，最终调用ViewRootImpl的dispatchInputEvent/synthesizeInputEvent等将一个InputEvent发送到ViewRootImpl内部的ViewRootHandler中进行处理
    在这个Handler中，最终是调用enqueueInputEvent进行处理，其输入参数receiver应该均为null
    ViewRootImpl.enqueueInputEvent(InputEvent event, InputEventReceiver receiver, int flags, boolean processImmediately)：
        adjustInputEventForCompatibility(event);        //兼容设置，对mTargetSdkVersion小于23
        QueuedInputEvent q = obtainQueuedInputEvent(event, receiver, flags);    //从mQueuedInputEventPool（类似对象池）中取出一个QueuedInputEvent对象，并将event、receiver、flags赋予它
        //始终按顺序将输入事件排入队列，而不考虑其时间戳。 这样做是因为应用程序或IME可能会响应触摸事件而注入键事件，并且我们希望确保按接收到的键的顺序处理注入的键，并且我们不能相信注入事件的时间戳是无变化的。
        QueuedInputEvent last = mPendingInputEventTail;
        if (last == null) {
            mPendingInputEventHead = q;
            mPendingInputEventTail = q;
        } else {
            last.mNext = q;
            mPendingInputEventTail = q;     //链表处理
        }
        mPendingInputEventCount += 1;
        Trace.traceCounter(Trace.TRACE_TAG_INPUT, mPendingInputEventQueueLengthCounterName, mPendingInputEventCount);
        if (processImmediately) {
            doProcessInputEvents();     //立即处理输入事件
        } else {
            scheduleProcessInputEvents();   //通过Handler进行处理，在Handler中调用doProcessInputEvents
        }
    ViewRootImpl.doProcessInputEvents()：
        //在队列中传递所有待处理的输入事件
        while (mPendingInputEventHead != null) {    //遍历队列中的所有事件
            QueuedInputEvent q = mPendingInputEventHead;
            mPendingInputEventHead = q.mNext;
            if (mPendingInputEventHead == null) {   //遍历完毕
                mPendingInputEventTail = null;
            }
            q.mNext = null;
            mPendingInputEventCount -= 1;
            Trace.traceCounter(Trace.TRACE_TAG_INPUT, mPendingInputEventQueueLengthCounterName, mPendingInputEventCount);
            long eventTime = q.mEvent.getEventTimeNano();   //时间
            long oldestEventTime = eventTime;
            if (q.mEvent instanceof MotionEvent) {
                MotionEvent me = (MotionEvent)q.mEvent;
                if (me.getHistorySize() > 0) {
                    oldestEventTime = me.getHistoricalEventTimeNano(0);     //找到最老的时间
                }
            }
            mChoreographer.mFrameInfo.updateInputEventTime(eventTime, oldestEventTime);     //更新时间，最近和最老？
            deliverInputEvent(q);   //传递到下一步进行处理
        }   //循环遍历结束，已处理完所有事件
        //我们已经处理完所有现在可以处理的输入事件，因此我们可以立即清除挂起标志
        if (mProcessInputEventsScheduled) {
            mProcessInputEventsScheduled = false;
            mHandler.removeMessages(MSG_PROCESS_INPUT_EVENTS);      //移除待处理的事件？
        }
        
    ViewRootImpl.deliverInputEvent(QueuedInputEvent q)：
        Trace.asyncTraceBegin(Trace.TRACE_TAG_VIEW, "deliverInputEvent", q.mEvent.getSequenceNumber());
        if (mInputEventConsistencyVerifier != null) {   //输入事件自洽？
            mInputEventConsistencyVerifier.onInputEvent(q.mEvent, 0);  //检查任意输入事件，一致性验证。嵌套级别：如果从基类调用，则为0，或者从子类调用为1。 如果该一致性验证程序已经在更高的嵌套级别上检查了事件，则不会再次检查该事件。 用于处理以下情况：子类的调度方法委托给其父类的调度方法，并且两种调度方法均调用一致性验证程序。
        }        
        InputStage stage;
        if (q.shouldSendToSynthesizer()) {      //是否是flag为FLAG_UNHANDLED的事件
            stage = mSyntheticInputStage;
        } else {
            stage = q.shouldSkipIme() ? mFirstPostImeInputStage : mFirstInputStage;    //是否是flag为FLAG_DELIVER_POST_IME或者是MotionEvent且SOURCE_CLASS_POINTER或SOURCE_ROTARY_ENCODER
        }        
        if (q.mEvent instanceof KeyEvent) {     //KeyEvent，比如home键、音量等
            mUnhandledKeyManager.preDispatch((KeyEvent) q.mEvent);  //在事件派发到任何事件之前调用，只是处理了UP事件？
        }        
        if (stage != null) {
            handleWindowFocusChanged();    //处理窗口焦点问题，会调用View.dispatchWindowFocusChanged
            stage.deliver(q);   //传递要处理的事件
        } else {
            finishInputEvent(q);    //结束输入事件，回收QueuedInputEvent至对象池
        }
    InputEventConsistencyVerifier，检查输入事件序列是否自洽。记录每个检测到的问题的描述。当检测到问题时，事件被污染。 此机制可防止多次报告同一错误。
    SyntheticInputStage、EarlyPostImeStage、NativePreImeStage分别对应上面有到的三个stage
    
    stage的deliver方法未被子类重载
    InputStage.deliver(QueuedInputEvent q)：
        if ((q.mFlags & QueuedInputEvent.FLAG_FINISHED) != 0) {     //若该事件flag是FLAG_FINISHED，表示该事件处理完毕
            forward(q);     //在AsyncInputStage中有重载（子类为NativePreImeInputStage、ImeInputStage、EarlyPostImeInputStage），调用了onDeliverToNext
        } else if (shouldDropInputEvent(q)) {   //是否要删除这个事件？
            finish(q, false);   //设置该事件flag为FLAG_FINISHED
        } else {
            apply(q, onProcess(q));     //先调用onProcess处理事件，然后根据结果调用forward或finish
        }
    onDeliverToNext中调用下一个InputStage的deliver方法（包装类）或调用finishInputEvent结束事件处理
    onProcess为事件的实际处理方法，每个子类都有不同的实现
    
    输入事件分为物理按键、屏幕触摸等事件、输入法事件等，InputStage的子类中有对应的处理
    SyntheticInputStage中处理的有：轨迹球运动事件（SyntheticTrackballHandler）、操纵杆动作事件（SyntheticJoystickHandler）、触摸导航移动事件（SyntheticTouchNavigationHandler）、键盘（SyntheticKeyboardHandler）
    ViewPostImeInputStage.onProcess(QueuedInputEvent q)：
        if (q.mEvent instanceof KeyEvent) {     //KeyEvent，用于报告键和按钮事件的对象，软键盘（IME）也会产生KeyEvent
            return processKeyEvent(q);      //会调用dispatchKeyEvent，向下分发事件
        } else {
            final int source = q.mEvent.getSource();    //获取事件输入源类型
            if ((source & InputDevice.SOURCE_CLASS_POINTER) != 0) { //SOURCE_CLASS_POINTER，输入源是与显示器关联的定点设备。示例：SOURCE_TOUCHSCREEN，SOURCE_MOUSE。应根据View层次结构将MotionEvent解释为显示单位中的绝对坐标。当手指触摸显示屏或按下/释放选择按钮时，指示指针向下/向上。使用getMotionRange(int)查询定点设备的范围。某些设备允许触摸显示区域外，因此有效范围可能会比实际显示尺寸小一些或大一些
                return processPointerEvent(q);      //会调用View.dispatchPointerEvent，向下分发事件
            } else if ((source & InputDevice.SOURCE_CLASS_TRACKBALL) != 0) {    //输入源是轨迹球导航设备。示例：SOURCE_TRACKBALL。应将MotionEvent解释为用于导航目的的特定于设备的单位中的相对运动。指针向下/向上指示何时按下/释放选择按钮。使用getMotionRange(int)查询运动范围
                return processTrackballEvent(q);    //View.dispatchCapturedPointerEvent（鼠标？）或View.dispatchTrackballEvent（轨迹球？）
            } else {
                return processGenericMotionEvent(q);    //View.dispatchGenericMotionEvent，调度通用运动事件。源类为SOURCE_CLASS_POINTER的通用运动事件将传递到指针下的视图。所有其他通用运动事件都传递到聚焦视图。悬停事件经过特殊处理，并传递给onHoverEvent(MotionEvent)
            }
        }
    按键事件、屏幕点击事件等都将在此处向下分发，等待处理，比如，在Activity中的onKeyDown、View中的OnClick、OnTouch等都将出现在后续步骤
    根据事件类型分类处理，如按键、触摸、鼠标、轨迹球等
    ViewPostImeInputStage.processKeyEvent：
        final KeyEvent event = (KeyEvent)q.mEvent;      //将QueuedInputEvent转为KeyEvent
        if (mUnhandledKeyManager.preViewDispatch(event)) {  //事件的预处理，可能会调用的View的onUnhandledKeyEvent（允许该视图处理正常分发未处理的KeyEvent）
            return FINISH_HANDLED;
        }
        //将keyEvent传递到视图层次结构
        if (mView.dispatchKeyEvent(event)) {    //若View中有OnKeyListener，则会调用它。将key event分发到焦点路径上的下一个视图。该路径从视图树的顶部一直延伸到当前聚焦的视图。如果此视图具有焦点，它将分派给自己。否则，它将沿着焦点路径调度下一个节点。此方法还会触发所有key侦听器
            return FINISH_HANDLED;
        }
        if (shouldDropInputEvent(q)) {  //是否要终止事件。这是一个焦点事件，若窗口当前没有输入焦点或已停止；这可能是从上一阶段返回的事件，但是窗口已失去焦点或同时停止。
            return FINISH_NOT_HANDLED;
        }
        //此分派适用于没有Window.Callback的Windows。否则，Window.Callback通常将已经调用了此方法（请参见DecorView.superDispatchKeyEvent），从而使此调用无效
        if (mUnhandledKeyManager.dispatch(mView, event)) {  //内部会调用的View的dispatchUnhandledKeyEvent（若View中有对应的listener）
            return FINISH_HANDLED;
        }
        int groupNavigationDirection = 0;
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_TAB) {  //TAB键？
            if (KeyEvent.metaStateHasModifiers(event.getMetaState(), KeyEvent.META_META_ON)) {
                groupNavigationDirection = View.FOCUS_FORWARD;
            } else if (KeyEvent.metaStateHasModifiers(event.getMetaState(), KeyEvent.META_META_ON | KeyEvent.META_SHIFT_ON)) {  //shift键？
                groupNavigationDirection = View.FOCUS_BACKWARD;
            }
        }
        //如果持有修饰符，请尝试将键解释为快捷键
        if (event.getAction() == KeyEvent.ACTION_DOWN && !KeyEvent.metaStateHasNoModifiers(event.getMetaState())
                && event.getRepeatCount() == 0 && !KeyEvent.isModifierKey(event.getKeyCode()) && groupNavigationDirection == 0) {
            if (mView.dispatchKeyShortcutEvent(event)) {    //发送一个快捷键事件，View应重写onKeyShortcut来处理快捷键（按键快捷方式事件未处理时，在焦点视图上调用此方法。重写此方法以实现View的本地键快捷方式。快捷键也可以通过设置菜单项的快捷方式属性来实现。）
                return FINISH_HANDLED;
            }
            if (shouldDropInputEvent(q)) {  //根据一些条件判断是否要终止此事件
                return FINISH_NOT_HANDLED;
            }
        }
        //应用后备事件策略
        if (mFallbackEventHandler.dispatchKeyEvent(event)) {    //备用的事件处理策略？
            return FINISH_HANDLED;
        }
        if (shouldDropInputEvent(q)) {  //是否要终止事件
            return FINISH_NOT_HANDLED;
        }
        //处理自动对焦更改
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (groupNavigationDirection != 0) {
                if (performKeyboardGroupNavigation(groupNavigationDirection)) {   //会调用的playSoundEffect，与声音有关？AudioManager中的上下左右处理
                    return FINISH_HANDLED;
                }
            } else {
                if (performFocusNavigation(event)) {    //tab、方向键的处理？
                    return FINISH_HANDLED;
                }
            }
        }
        return FORWARD;
    对按键事件的处理：先分发给View层次中的处于焦点下的View进行处理（该View必须实现OnKeyListener）；
    若各View都未处理，则将其分发到Window中？Activity等，若还未处理则由系统自己处理？
    Activity中有实现KeyEvent.Callback接口，对KeyEvent的一些事件有默认的处理（如：onKeyDown等）
        
    点击事件、触摸等事件的处理
    ViewPostImeInputStage.processPointerEvent：
        final MotionEvent event = (MotionEvent)q.mEvent;
        mAttachInfo.mUnbufferedDispatchRequested = false;   //未缓存的分发请求？
        mAttachInfo.mHandlingPointerEvent = true;   //正在处理？
        boolean handled = mView.dispatchPointerEvent(event);    //向View树中分发事件
        maybeUpdatePointerIcon(event);      //鼠标悬停时的图标变化？
        maybeUpdateTooltip(event);      //更新提示框的显示与隐藏？
        mAttachInfo.mHandlingPointerEvent = false;  //事件处理完毕？
        if (mAttachInfo.mUnbufferedDispatchRequested && !mUnbufferedInputDispatch) {
            mUnbufferedInputDispatch = true;
            if (mConsumeBatchedInputScheduled) {
                scheduleConsumeBatchedInputImmediately();   //事件批处理？
            }
        }
        return handled ? FINISH_HANDLED : FORWARD;
        
    将与触摸相关的pointer事件调度到onTouchEvent(MotionEvent)，并将所有其他事件调度到onGenericMotionEvent(MotionEvent)。关注点的这种分离强化了onTouchEvent(MotionEvent)确实与触摸有关的不变性，不应期望它可以处理其他定点设备功能
    View.dispatchPointerEvent(MotionEvent event)：
        if (event.isTouchEvent()) {   //如果此运动事件是触摸事件，则返回true。特别排除具有动作ACTION_HOVER_MOVE，ACTION_HOVER_ENTER，ACTION_HOVER_EXIT或ACTION_SCROLL的指针事件，因为它们实际上不是触摸事件（指针未按下）
            return dispatchTouchEvent(event);   //开发中经常处理的事件的分发，将触摸屏motion事件向下传递到目标视图，如果当前View是目标，则将其传递给该视图
        } else {
            return dispatchGenericMotionEvent(event);   //调度通用motion事件。源类为SOURCE_CLASS_POINTER的通用运动事件将传递到指针下的视图。所有其他通用motion事件都传递到聚焦视图。悬停事件经过特殊处理，并传递给onHoverEvent（MotionEvent）
        }
    
    ViewRootImpl中的mView应该是DecorView，它是一个ViewGroup，在它的dispatchTouchEvent中：
        先会将事件分发至Activity（调用Window.Callback.dispatchTouchEvent，Activity实现了Callback这个接口）；
        Activity的dispatchTouchEvent通过PhoneWindow的superDispatchTouchEvent将事件分发至ViewGroup（DecorView）；
        在PhoneWindow的superDispatchTouchEvent中调用DecorView的superDispatchTouchEvent向子View分发（流程就是ViewGroup）；
        在Activity中，先将事件分发至各个View，若事件未处理，则在Activity的onTouchEvent中进行处理（有个关闭Activity的默认处理）
    
    ViewGroup.dispatchTouchEvent，对触摸屏幕事件的分发和处理：
        onInterceptTouchEvent，返回true拦截事件，在当前View中处理，不向下分发，false继续向下分发；
        onTouch、onTouchEvent、onClick等同View的处理？
        dispatchTransformedTouchEvent内部调用子View的dispatchTouchEvent，将事件分发到View
    
    View.dispatchTouchEvent，对触摸屏幕的事件的处理：
        首先调用stopNestedScroll，若屏幕上有列表真正滑动，则停止滑动；
        然后是handleScrollBarDragging，若在拖动列表等时对ScrollBar的处理；？
        接着是OnTouchListener.onTouch，若View实现的该接口，则调用它；
        若未实现OnTouchListener或上面的onTouch事件返回false，则调用View.onTouchEvent；
        在onTouchEvent中，若View可点击：
            ACTION_UP中，performClick响应点击事件（ClickListener.onClick）
            ACTION_DOWN中，checkForLongClick检查是否是长按点击？会调用的performLongClick、OnLongClickListener.onLongClick响应长按事件
            ACTION_CANCEL中，removeTapCallback()、removeLongPressCallback()取消点击和长按事件？
            ACTION_MOVE中，pointInView是否在当前View中滑动？
    
    ViewGroup.dispatchTouchEvent：
        if (mInputEventConsistencyVerifier != null) {
            mInputEventConsistencyVerifier.onTouchEvent(ev, 1);     //一致性校验
        }
        //如果该事件以可访问性为焦点的视图为目标，则启动正常的事件分发。也许后代将处理点击
        if (ev.isTargetAccessibilityFocus() && isAccessibilityFocusedViewOrHost()) {    //是否是当前View？
            ev.setTargetAccessibilityFocus(false);
        }
        boolean handled = false;
        if (onFilterTouchEventForSecurity(ev)) {    //过滤触摸事件以应用安全策略。FILTER_TOUCHES_WHEN_OBSCURED View被遮盖时过滤（不分发）、FLAG_WINDOW_IS_OBSCURED 当前Window被另一个Window遮盖时过滤不分发（如恶意劫持），返回false不分发
            final int action = ev.getAction();
            final int actionMasked = action & MotionEvent.ACTION_MASK;      //掩码？
            // 处理最初的DOWN事件
            if (actionMasked == MotionEvent.ACTION_DOWN) {
                //开始新的触摸手势时，放弃所有先前的状态。框架可能由于应用程序切换，ANR或其他一些状态更改而放弃了上一个手势的上移或取消事件
                cancelAndClearTouchTargets(ev);     //取消并清除所有触摸目标。
                resetTouchState();      //重置所有触摸状态，为新的周期做准备。这两项都是在mFirstTouchTarget不为null时执行                
            }
            //检查拦截
            final boolean intercepted;
            if (actionMasked == MotionEvent.ACTION_DOWN || mFirstTouchTarget != null) {     //触摸按下时或按下之后的后续动作
                final boolean disallowIntercept = (mGroupFlags & FLAG_DISALLOW_INTERCEPT) != 0;   //FLAG_DISALLOW_INTERCEPT被设置时该ViewGroup不允许拦截
                if (!disallowIntercept) {   //只有在FLAG_DISALLOW_INTERCEPT标志未被设置时，才会调用onInterceptTouchEvent方法
                    intercepted = onInterceptTouchEvent(ev);    //是否拦截事件
                    ev.setAction(action);   //如果已更改，请还原操作
                } else {
                    intercepted = false;    //FLAG_DISALLOW_INTERCEPT不允许拦截时intercepted为false，否则由onInterceptTouchEvent决定
                }
            } else {
                //没有触摸目标，并且此动作并非最初的DOWN事件，因此该ViewGroup继续拦截触摸事件
                intercepted = true;
            }
            //如果被拦截，请开始正常事件分发。另外，如果已经有一个正在处理手势的视图，则进行常规事件调度
            if (intercepted || mFirstTouchTarget != null) {
                ev.setTargetAccessibilityFocus(false);
            }
            //检查取消
            final boolean canceled = resetCancelNextUpFlag(this) || actionMasked == MotionEvent.ACTION_CANCEL;  //是否设置了PFLAG_CANCEL_NEXT_UP_EVENT标志，或者是取消事件
            //如有必要，更新触摸点下的目标View列表
            final boolean split = (mGroupFlags & FLAG_SPLIT_MOTION_EVENTS) != 0;    //FLAG_SPLIT_MOTION_EVENTS设置后，此ViewGroup将在适当时将MotionEvents拆分为多个子视图。
            TouchTarget newTouchTarget = null;
            boolean alreadyDispatchedToNewTouchTarget = false;      //是否已经分发到新目标了
            if (!canceled && !intercepted) {    //事件未取消且未拦截
                //如果事件以可访问性焦点为目标，则将其提供给具有可访问性焦点的视图，如果事件未处理，则清除标志并将事件照常分派给所有子View。我们正在寻找以可访问性为焦点的View，以避免保持状态，因为这些事件非常少见
                View childWithAccessibilityFocus = ev.isTargetAccessibilityFocus() ? findChildWithAccessibilityFocus() : null;     //findChildWithAccessibilityFocus返回当前ViewGroup或null？
                //childWithAccessibilityFocus要么是当前ViewGroup要么为null？
                if (actionMasked == MotionEvent.ACTION_DOWN || (split && actionMasked == MotionEvent.ACTION_POINTER_DOWN) || actionMasked == MotionEvent.ACTION_HOVER_MOVE) {
                    final int actionIndex = ev.getActionIndex(); // always 0 for down，对于getActionMasked()返回的ACTION_POINTER_DOWN或ACTION_POINTER_UP，这将返回关联的指针索引。 该索引可以与getPointerId(int)，getX(int)，getY(int)，getPressure(int)和getSize(int)一起使用，以获取有关指针下降或上升的信息，返回与操作关联的索引
                    final int idBitsToAssign = split ? 1 << ev.getPointerId(actionIndex) : TouchTarget.ALL_POINTER_IDS;   //索引
                    //清除此指针ID的早期触摸目标，以防它们变得不同步
                    removePointersFromTouchTargets(idBitsToAssign);     //从目标中移除该pointerIdBits
                    final int childrenCount = mChildrenCount;
                    if (newTouchTarget == null && childrenCount != 0) {     //有子View
                        final float x = ev.getX(actionIndex);   //返回坐标
                        final float y = ev.getY(actionIndex);
                        //找到一个可以接收该事件的子View。从前到后扫描子View
                        final ArrayList<View> preorderedList = buildTouchDispatchChildList();   //按顺序构造一个View列表
                        final boolean customOrder = preorderedList == null && isChildrenDrawingOrderEnabled();
                        final View[] children = mChildren;
                        for (int i = childrenCount - 1; i >= 0; i--) {      //遍历子View
                            final int childIndex = getAndVerifyPreorderedIndex(childrenCount, i, customOrder);  //索引
                            final View child = getAndVerifyPreorderedView(preorderedList, children, childIndex);  //View
                            //如果有一个具有可访问性焦点的视图，我们希望它首先获取事件，如果未处理，我们将执行常规分派。我们可以进行两次迭代，但这在一定时间范围内比较安全
                            if (childWithAccessibilityFocus != null) {
                                if (childWithAccessibilityFocus != child) {
                                    continue;
                                }
                                childWithAccessibilityFocus = null;     //childWithAccessibilityFocus为当前child时
                                i = childrenCount - 1;
                            }
                            //canViewReceivePointerEvents child可见或child有动画时返回true，isTransformedTouchPointInView将触摸点转为坐标，如果子视图在转换为其坐标空间时包含指定的点，则返回true
                            if (!canViewReceivePointerEvents(child) || !isTransformedTouchPointInView(x, y, child, null)) {     //子View不可见或子View不包含该点
                                ev.setTargetAccessibilityFocus(false);
                                continue;
                            }
                            newTouchTarget = getTouchTarget(child);  //获取指定子View的触摸目标。如果没有找到，则返回null
                            if (newTouchTarget != null) {
                                //子View已经在其范围内接触。除了要处理的指针外，还要为其提供新的指针
                                newTouchTarget.pointerIdBits |= idBitsToAssign;
                                break;
                            }
                            resetCancelNextUpFlag(child);   //重置“下一步取消”标志。如果之前设置了该标志，则返回true
                            if (dispatchTransformedTouchEvent(ev, false, child, idBitsToAssign)) {  //向子View分发事件，若返回true事件被处理
                                //子View希望在其范围内获得触摸
                                mLastTouchDownTime = ev.getDownTime();
                                if (preorderedList != null) {
                                    //childIndex指向预排序列表，找到原始索引
                                    for (int j = 0; j < childrenCount; j++) {  //遍历mChildren，找出childIndex的子View
                                        if (children[childIndex] == mChildren[j]) { 
                                            mLastTouchDownIndex = j;
                                            break;
                                        }
                                    }
                                } else {
                                    mLastTouchDownIndex = childIndex;
                                }
                                mLastTouchDownX = ev.getX();
                                mLastTouchDownY = ev.getY();
                                newTouchTarget = addTouchTarget(child, idBitsToAssign);  //将指定子项的触摸目标添加到列表的开头。假设目标Child还不存在。内部给mFirstTouchTarget赋值
                                alreadyDispatchedToNewTouchTarget = true;
                                break;      //事件被处理，结束循环
                            }
                            //可访问性焦点未处理该事件，因此请清除标志并向所有子View进行正常调度
                            ev.setTargetAccessibilityFocus(false);
                        }
                        if (preorderedList != null) preorderedList.clear();
                    }
                    if (newTouchTarget == null && mFirstTouchTarget != null) {
                        //找不到要接收事件的子View。将指针分配给最近添加的目标
                        newTouchTarget = mFirstTouchTarget;
                        while (newTouchTarget.next != null) {
                            newTouchTarget = newTouchTarget.next;
                        }   //遍历到链表尾部
                        newTouchTarget.pointerIdBits |= idBitsToAssign;     //设置指针id？
                    }
                }
            }
            //分发到touch targets
            if (mFirstTouchTarget == null) {    //在上面分发事件被处理后，mFirstTouchTarget被赋值了，所以在此为null表示事件还未被处理
                //没有触摸目标，因此请将其视为普通视图
                handled = dispatchTransformedTouchEvent(ev, canceled, null, TouchTarget.ALL_POINTER_IDS);   //传入的child为null，表示由当前这个ViewGroup进行处理
            } else {    //表示第一次的DOWN事件已被处理了或者是后续的UP、MOVE等事件还未被处理
                //调度到触摸目标，如果我们已经分发到了新的触摸目标，则不包括它。如有必要，取消触摸目标。
                TouchTarget predecessor = null;
                TouchTarget target = mFirstTouchTarget;
                while (target != null) {
                    final TouchTarget next = target.next;
                    if (alreadyDispatchedToNewTouchTarget && target == newTouchTarget) {
                        handled = true;
                    } else {
                        final boolean cancelChild = resetCancelNextUpFlag(target.child) || intercepted; //拦截事件或由CANCEL_NEXT标识时为true
                        if (dispatchTransformedTouchEvent(ev, cancelChild, target.child, target.pointerIdBits)) {   //分发给目标子View
                            handled = true;     //在上面若子View处理了事件，则mFirstTouchTarget为处理了事件的子View，next为null，分发后续的UP等事件时在此处直接分发给目标子View
                        }
                        if (cancelChild) {  //取消分发事件
                            if (predecessor == null) {
                                mFirstTouchTarget = next;
                            } else {
                                predecessor.next = next;
                            }
                            target.recycle();
                            target = next;
                            continue;
                        }
                    }
                    predecessor = target;
                    target = next;
                }
            }
            //如果需要，为pointer的UP或cancel更新触摸目标列表
            if (canceled || actionMasked == MotionEvent.ACTION_UP || actionMasked == MotionEvent.ACTION_HOVER_MOVE) {
                resetTouchState();  //重置所有触摸状态，为新的循环做准备
            } else if (split && actionMasked == MotionEvent.ACTION_POINTER_UP) {
                final int actionIndex = ev.getActionIndex();
                final int idBitsToRemove = 1 << ev.getPointerId(actionIndex);
                removePointersFromTouchTargets(idBitsToRemove);
            }
        }
        if (!handled && mInputEventConsistencyVerifier != null) {
            mInputEventConsistencyVerifier.onUnhandledEvent(ev, 1);     //事件未消费时做一致性校验
        }
        return handled;
    ViewGroup先检查是否要拦截（onInterceptTouchEvent）或取消事件，若都不是且是ACTION_DOWN则找出符合条件的子View调用dispatchTransformedTouchEvent进行事件处理
    ACTION_DOWN表示第一次事件，后续有UP、MOVE等，在ACTION_DOWN时：
        若有子View处理了事件则为mFirstTouchTarget赋值，后续事件直接使用该target处理
        若子View都未处理，则让当前ViewGroup进行处理（调用super.dispatchTouchEvent，走View的处理流程）
    在取消或ACTION_UP时要重置一些标识
    
    将Motion事件转换为特定子View的坐标空间，过滤掉无关的指针ID，并在必要时覆盖其动作。如果child为null，则假定将MotionEvent发送到此ViewGroup
    ViewGroup.dispatchTransformedTouchEvent(MotionEvent event, boolean cancel, View child, int desiredPointerIdBits)：
        final boolean handled;
        //取消动作是一种特殊情况。我们不需要执行任何转换或过滤。重要的是动作，而不是内容
        final int oldAction = event.getAction();
        if (cancel || oldAction == MotionEvent.ACTION_CANCEL) {     //取消事件
            event.setAction(MotionEvent.ACTION_CANCEL);
            if (child == null) {
                handled = super.dispatchTouchEvent(event);  //执行View中的步骤
            } else {
                handled = child.dispatchTouchEvent(event);  //子View中的取消操作
            }
            event.setAction(oldAction);     //重置之前的Action
            return handled; 
        }
        //计算要传递的指针id？
        final int oldPointerIdBits = event.getPointerIdBits();
        final int newPointerIdBits = oldPointerIdBits & desiredPointerIdBits;   //逻辑与
        //如果由于某种原因我们最终处于一种不一致的状态，在这种状态下，我们可能会产生一个没有指针的运动事件，则删除该事件
        if (newPointerIdBits == 0) {
            return false;
        }
        //如果指针的Id一样，并且我们不需要执行任何奇特的不可逆转换，那么只要我们谨慎地还原所做的任何更改，就可以将运动事件重用于此分派。否则，我们需要进行复制
        final MotionEvent transformedEvent;
        if (newPointerIdBits == oldPointerIdBits) {
            if (child == null || child.hasIdentityMatrix()) {
                if (child == null) {
                    handled = super.dispatchTouchEvent(event);      //当前ViewGroup处理
                } else {
                    final float offsetX = mScrollX - child.mLeft;
                    final float offsetY = mScrollY - child.mTop;    //计算坐标
                    event.offsetLocation(offsetX, offsetY);     //设置坐标
                    handled = child.dispatchTouchEvent(event);  //子View处理
                    event.offsetLocation(-offsetX, -offsetY);   //还原坐标？
                }
                return handled;
            }
            transformedEvent = MotionEvent.obtain(event);
        } else {
            transformedEvent = event.split(newPointerIdBits);   //拆分事件？
        }
        //执行任何必要的转换和调度
        if (child == null) {
            handled = super.dispatchTouchEvent(transformedEvent);   //自己处理
        } else {
            final float offsetX = mScrollX - child.mLeft;
            final float offsetY = mScrollY - child.mTop;        //计算坐标
            transformedEvent.offsetLocation(offsetX, offsetY);  //设置坐标
            if (!child.hasIdentityMatrix()) {
                transformedEvent.transform(child.getInverseMatrix());   //子View的矩阵变换？
            }
            handled = child.dispatchTouchEvent(transformedEvent);   //子View处理
        }
        //结束
        transformedEvent.recycle();     //回收该MotionEvent
        return handled;
    四种处理情况：
        1、取消时，包括ACTION_CANCEL和传入cancel为true时，设置Event action为ACTION_CANCEL
        2、PointerId bit位不包含传入的id时直接返回false
        3、传入的PointerId bit位与当前事件包含的一致时，由当前View或子View进行处理
        4、传入的PointerId bit位与当前事件包含的不一致时，由当前View或子View进行处理，对子View可能有个矩阵变换？
    
    最后看子View中的处理：
    View.dispatchTouchEvent(MotionEvent event)：
        //如果该事件应由可访问性焦点优先处理
        if (event.isTargetAccessibilityFocus()) {
            //我们没有焦点，或者没有虚拟后代拥有焦点，因此不处理事件
            if (!isAccessibilityFocusedViewOrHost()) {
                return false;
            }
            //我们有焦点并得到了事件，然后使用常规事件调度
            event.setTargetAccessibilityFocus(false);
        }
        boolean result = false;
        if (mInputEventConsistencyVerifier != null) {
            mInputEventConsistencyVerifier.onTouchEvent(event, 0);    //一致性校验
        }
        final int actionMasked = event.getActionMasked();   //掩码
        if (actionMasked == MotionEvent.ACTION_DOWN) {
            //防御性清理新手势
            stopNestedScroll();     //手指按下时停止正在进行的嵌套滑动（当前未进行嵌套滚动时调用此方法是无害的）
        }
        if (onFilterTouchEventForSecurity(event)) {   //过滤触摸事件以应用安全策略，FILTER_TOUCHES_WHEN_OBSCURED View被遮盖时过滤（不分发）、FLAG_WINDOW_IS_OBSCURED 当前Window被另一个Window遮盖时过滤不分发（如恶意劫持），返回false不分发
            if ((mViewFlags & ENABLED_MASK) == ENABLED && handleScrollBarDragging(event)) {   //enabled为true，并且事件处理为鼠标拖动滚动条？则为真，否则为假
                result = true;
            }
            /无检查可简化If陈述
            ListenerInfo li = mListenerInfo;      //可见 setOnTouchListener 赋值
            if (li != null && li.mOnTouchListener != null && (mViewFlags & ENABLED_MASK) == ENABLED && li.mOnTouchListener.onTouch(this, event)) {
                result = true;      //设置了OnTouchListener，enabled为true，并且OnTouchListener.onTouch返回true时才表示事件被处理了
            }
            if (!result && onTouchEvent(event)) {   //事件未被OnTouchListener消费，则调用onTouchEvent处理
                result = true;      //onTouchEvent返回true表示被处理
            }
        }
        if (!result && mInputEventConsistencyVerifier != null) {
            mInputEventConsistencyVerifier.onUnhandledEvent(event, 0);      //事件未被消费，则进行一致性校验
        }
        //如果这是手势的结束，请在嵌套滚动后清理；如果我们尝试了ACTION_DOWN，但也不想取消其余手势，也可以取消它
        if (actionMasked == MotionEvent.ACTION_UP || actionMasked == MotionEvent.ACTION_CANCEL ||
                (actionMasked == MotionEvent.ACTION_DOWN && !result)) {     //UP、CANCEL或 DOWN 但事件未被处理，停止嵌套滚动
            stopNestedScroll();     //停止正在进行的嵌套滑动
        }
        return result;
    首先检查当前View是否处于焦点下，在手指按下时，先停止正在进行的嵌套滑动（若有），然后过滤遮盖情况
    若View中设置了OnTouchListener，则先调用OnTouchListener的onTouch方法，
    若onTouch返回false（表示事件未被消费，继续传递），则调用onTouchEvent方法（其中有onClick等的处理）
    
    View.onTouchEvent(MotionEvent event)：
        final float x = event.getX();
        final float y = event.getY();   //手指在屏幕上的坐标
        final int viewFlags = mViewFlags;
        final int action = event.getAction();   //action
        final boolean clickable = ((viewFlags & CLICKABLE) == CLICKABLE     //表示可以单击此View。单击时，View通过通知OnClickListener对点击做出反应
                || (viewFlags & LONG_CLICKABLE) == LONG_CLICKABLE)          //表示该View可以长按。当可长时间单击时，View通过通知OnLongClickListener或显示上下文菜单来响应长按
                || (viewFlags & CONTEXT_CLICKABLE) == CONTEXT_CLICKABLE;    //表示可以在上下文中单击此View。当上下文可点击时，View通过通知OnContextClickListener来响应上下文单击（例如，按下主手写笔按钮或单击鼠标右键）
        if ((viewFlags & ENABLED_MASK) == DISABLED) {       //DISABLED，该View被禁用。解释因子类而异。调用setFlags时与ENABLED_MASK一起使用
            if (action == MotionEvent.ACTION_UP && (mPrivateFlags & PFLAG_PRESSED) != 0) {  //ACTION_UP（手指离开）且Pressed（按下状态）
                setPressed(false);      //设置Pressed为false
            }
            mPrivateFlags3 &= ~PFLAG3_FINGER_DOWN;    //取反？标识手指未按下？PFLAG3_FINGER_DOWN，指示用户当前正在触摸屏幕。当前仅用于工具提示定位
            //一个可单击的但被禁用的View仍然消耗触摸事件，只是不响应它们
            return clickable;   //当前View可单击或长按或，则表示事件被消费，否则事件未被消费，继续向上传递
        }
        //委托 Helper类，用于处理您希望视图的触摸区域大于其实际视图范围的情况。触摸区域已更改的视图称为委托视图。此类应由委托的祖先使用。要使用TouchDelegate，首先创建一个实例，该实例指定应映射到委托和委托视图本身的范围
        if (mTouchDelegate != null) {       //代理？如果事件在构造函数中指定的范围内，则将触摸事件转发到委托视图
            if (mTouchDelegate.onTouchEvent(event)) {       //如果将事件转发给委托，则为true，否则为false
                return true;
            }
        }
        if (clickable || (viewFlags & TOOLTIP) == TOOLTIP) {    //tooltip 表示此视图可以在悬停或长按时显示工具提示
            switch (action) {
                case MotionEvent.ACTION_UP:     //ACTION_UP事件
                    mPrivateFlags3 &= ~PFLAG3_FINGER_DOWN;
                    if ((viewFlags & TOOLTIP) == TOOLTIP) {     //先处理工具提示
                        handleTooltipUp();      //隐藏工具提示？mHideTooltipRunnable
                    }
                    if (!clickable) {   //不可点击
                        removeTapCallback();    //删除tap检测计时器
                        removeLongPressCallback();      //移除长按检测定时器
                        mInContextButtonPress = false;
                        mHasPerformedLongPress = false;
                        mIgnoreNextUpEvent = false;
                        break;
                    }
                    boolean prepressed = (mPrivateFlags & PFLAG_PREPRESSED) != 0;   //指示预压状态；从ACTION_DOWN到识别“真实”按压之间的时间很短。Prepressed用于识别快速点击，即使它们短于ViewConfiguration.getTapTimeout()
                    if ((mPrivateFlags & PFLAG_PRESSED) != 0 || prepressed) {
                        //如果我们还没有获得focus，我们应该进入触摸模式
                        boolean focusTaken = false;
                        if (isFocusable() && isFocusableInTouchMode() && !isFocused()) {    //View可聚焦且在触摸模式下可聚焦且当前没有交点。当视图可聚焦时，在触摸模式下它可能不希望聚焦。 例如，当用户通过D-pad导航时，按钮希望聚焦，以便用户可以单击它，但是一旦用户开始触摸屏幕，该按钮就不应聚焦
                            focusTaken = requestFocus();    //请求焦点，调用此选项可尝试将焦点集中于特定视图或其后代之一。如果视图不可聚焦（isFocusable()返回false），或者在设备处于触摸模式时可聚焦并且在触摸模式下不可聚焦（isFocusableInTouchMode()，则视图实际上不会获得焦点。另请参见focusSearch(int)，这是您所谓的拥有焦点，并且希望您的父母寻找下一个焦点。这等效于使用参数FOCUS_DOWN和null调用requestFocus（int，Rect）
                        }
                        if (prepressed) {
                            //在实际显示为按下之前，该按钮已被释放。 现在（在安排点击之前）使其显示按下状态，以确保用户看到它
                            setPressed(true, x, y);   //设置此视图的按下状态并为动画提示提供触摸坐标。按压效果？
                        }
                        if (!mHasPerformedLongPress && !mIgnoreNextUpEvent) {
                            //这是一个tap（轻按），因此取消长按检查
                            removeLongPressCallback();
                            //仅在我们处于按下状态时才执行点击操作
                            if (!focusTaken) {  //没有焦点？
                                //使用Runnable并post它，而不是直接调用performClick。这样，在单击操作开始之前，视图的其他视觉状态就会更新
                                if (mPerformClick == null) {
                                    mPerformClick = new PerformClick();     //点击事件的执行体
                                }
                                if (!post(mPerformClick)) {     //post到主线程的handler中
                                    performClickInternal();     //添加的handler中失败，则直接执行点击
                                }
                            }
                        }
                        if (mUnsetPressedState == null) {
                            mUnsetPressedState = new UnsetPressedState();   //runnable 执行setPressed(false)
                        }
                        if (prepressed) {
                            postDelayed(mUnsetPressedState, ViewConfiguration.getPressedStateDuration());   //延迟press状态持续时间后执行上面的runnable
                        } else if (!post(mUnsetPressedState)) {     //无论如何都要执行runnable
                            mUnsetPressedState.run();   //如果发布失败，请立即取消按
                        }
                        removeTapCallback();    //点击事件已发布或执行？移除tap计时检测
                    }
                    mIgnoreNextUpEvent = false;
                    break;
                case MotionEvent.ACTION_DOWN:
                    if (event.getSource() == InputDevice.SOURCE_TOUCHSCREEN) {      //事件来源为触摸屏
                        mPrivateFlags3 |= PFLAG3_FINGER_DOWN;       //手指按下
                    }
                    mHasPerformedLongPress = false;     //长按是否执行
                    if (!clickable) {       //不可点击、不可长按、不可Context click
                        checkForLongClick(0, x, y);     //检查后执行长按事件
                        break;
                    }
                    if (performButtonActionOnTouchDown(event)) {    //在触摸事件（down）期间执行与按钮相关的操作，事件源为鼠标且按下的是鼠标右键，展示菜单
                        break;
                    }
                    //遍历层次结构以确定我们是否在滚动容器内
                    boolean isInScrollingContainer = isInScrollingContainer();  //容器是否可滚动，如果此ViewGroup的子级或子级的按下状态应延迟，则返回true。 通常，应该对可滚动的容器（例如列表）执行此操作。这样可以防止在用户实际尝试滚动内容时出现按下状态。默认实现返回true
                    //对于滚动容器内部的视图，如果滚动，则将按下的反馈延迟一小段时间
                    if (isInScrollingContainer) {
                        mPrivateFlags |= PFLAG_PREPRESSED;
                        if (mPendingCheckForTap == null) {
                            mPendingCheckForTap = new CheckForTap();    //轻按 的检查
                        }
                        mPendingCheckForTap.x = event.getX();
                        mPendingCheckForTap.y = event.getY();
                        postDelayed(mPendingCheckForTap, ViewConfiguration.getTapTimeout());    //以轻按的时间延迟发送，执行Runnable时检查执行长按？
                    } else {
                        //不在滚动容器中，因此请立即显示反馈
                        setPressed(true, x, y);
                        checkForLongClick(0, x, y);     //长按，延迟一个长按时间后检查press状态，若还是按下状态则执行长按
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                    if (clickable) {
                        setPressed(false);      //press状态
                    }
                    removeTapCallback();        //移除tap
                    removeLongPressCallback();  //移除长按
                    mInContextButtonPress = false;
                    mHasPerformedLongPress = false;
                    mIgnoreNextUpEvent = false;
                    mPrivateFlags3 &= ~PFLAG3_FINGER_DOWN;
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (clickable) {
                        drawableHotspotChanged(x, y);   //给背景Drawable设置setHotspot，并向下分发（dispatchDrawableHotspotChanged），每当视图热点发生变化并且需要传播到该视图管理的可绘制对象或子视图时，都会调用此函数。子视图的调度由dispatchDrawableHotspotChanged（float，float）处理。覆盖此函数时，请务必调用超类。
                    }
                    //宽容移出按钮
                    if (!pointInView(x, y, mTouchSlop)) {   //一种确定局部坐标中给定点是否在视图内部的实用程序方法，该视图的面积将通过倾斜因子扩展。在处理触摸移动事件以确定该事件是否仍在视图内时，将调用此方法。
                        //移到按钮外部，删除以后所有的长按/点击检查
                        removeTapCallback();        //移除tap
                        removeLongPressCallback();  //移除长按
                        if ((mPrivateFlags & PFLAG_PRESSED) != 0) {
                            setPressed(false);
                        }
                        mPrivateFlags3 &= ~PFLAG3_FINGER_DOWN;      //finger_down 取反
                    }
                    break;
            }
            return true;
        }
        return false;
    onTouchEvent处理MotionEvent
    在ACTION_DOWN时：
        若不可点击、不可长按等，会执行checkForLongClick，处理与TOOLTIP相关？showLongClickTooltip？然后break
        若事件源为鼠标并且按下右键，则展示菜单 break
        若是在一个可滚动的容器内，则CheckForTap，内部执行checkForLongClick，若一直长按，则最终会触发 longClick
        若不在可滚动容器内，则直接checkForLongClick
    在ACTION_MOVE时：
        若可点击，会给背景Drawable设置setHotspot
        若移动时移出了View范围，则会取消 tap、长按事件的检查和执行，press状态复原、finger_down 取反
    在ACTION_UP时：
        先处理TOOLTIP，即隐藏工具提示，mHideTooltipRunnable
        若不可点击，则取消 tap、长按事件的检查和执行，再break
        mHasPerformedLongPress会在执行长按事件时被设为true，
        若在此时为false，则认为是一个点击事件，取消长按检查（removeLongPressCallback）
        执行PerformClick（点击事件），press状态复原（UnsetPressedState），移除tap检查（removeTapCallback）
    在ACTION_CANCEL时：
        会取消 tap、长按事件的检查和执行，press状态复原、finger_down 取反等
    综上，在手指按下时，会立即发送一个长按检查，若一直处于ACTION_DOWN，则到达时间时执行长按事件
    在手指抬起时（ACTION_UP），检查是否执行了长按事件，若未执行，则移除长按检查，执行点击事件
    
    CheckForTap中也有执行checkForLongClick，其用于在可滚动容器内的延迟点击？
    View.checkForLongClick：
        if ((mViewFlags & LONG_CLICKABLE) == LONG_CLICKABLE || (mViewFlags & TOOLTIP) == TOOLTIP) {     //View可长按或有 tooltip
            mHasPerformedLongPress = false;
            if (mPendingCheckForLongPress == null) {
                mPendingCheckForLongPress = new CheckForLongPress();    //长按事件实际执行的Runnable
            }
            mPendingCheckForLongPress.setAnchor(x, y);      //设置锚点坐标
            mPendingCheckForLongPress.rememberWindowAttachCount();  //复制一个 mWindowAttachCount
            mPendingCheckForLongPress.rememberPressedState();       //备份 press当前状态
            postDelayed(mPendingCheckForLongPress, ViewConfiguration.getLongPressTimeout() - delayOffset);  //LongPressTimeout，按下变成长按之前的持续时间（以毫秒为单位），默认500毫秒，可在系统配置文件中更改IntCoreSetting
        }
    CheckForLongPress.run：
        if ((mOriginalPressedState == isPressed()) && (mParent != null)     //press状态是否一致
                && mOriginalWindowAttachCount == mWindowAttachCount) {      //mWindowAttachCount是否一致
            if (performLongClick(mX, mY)) {         //执行长按事件
                mHasPerformedLongPress = true;      //在ACTION_UP中检查为true，则不会执行点击事件
            }
        }
    performLongClick内部最后会执行performLongClickInternal
    performLongClickInternal：
        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_LONG_CLICKED);
        boolean handled = false;
        final ListenerInfo li = mListenerInfo;
        if (li != null && li.mOnLongClickListener != null) {        //OnLongClickListener
            handled = li.mOnLongClickListener.onLongClick(View.this);   //onLongClick
        }
        if (!handled) {     //OnLongClickListener为null或onLongClick返回false
            final boolean isAnchored = !Float.isNaN(x) && !Float.isNaN(y);
            handled = isAnchored ? showContextMenu(x, y) : showContextMenu();   //展示Context菜单，同鼠标右键点击后展示的菜单
        }
        if ((mViewFlags & TOOLTIP) == TOOLTIP) {
            if (!handled) {     //showContextMenu 返回false
                handled = showLongClickTooltip((int) x, (int) y);   //展示 tooltip
            }
        }
        if (handled) {
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);  //执行长按的触觉反馈，仅当isHapticFeedbackEnabled()返回true时才执行
        }
        return handled;
    长按事件，先执行OnLongClickListener，若我们未实现该接口或onLongClick返回false，则接着执行showContextMenu（同鼠标右键点击）
    若showContextMenu返回false，且当前View有tooltip，则展示tooltip窗口，最后，若事件被消费掉了，则显示一个触觉反馈
    
    点击事件：
        PerformClick是一个Runnable，其run方法中直接执行performClickInternal
        performClickInternal，performClick()的入口点-View上的其他方法应直接调用它，而不是performClick()来确保在必要时通知自动填充管理器（因为子类可以扩展performClick()而不调用父方法）
        View.performClickInternal：
            //在执行单击操作之前，必须通知自动填充管理器，以避免应用程序具有单击侦听器的情况，该侦听器会更改自动填充服务可能感兴趣的视图状态
            notifyAutofillManagerOnClick();     //内部执行 getAutofillManager().notifyViewClicked(this)
            return performClick();
        
        调用此视图的OnClickListener（如果已定义）。执行与单击相关的所有常规操作：报告可访问性事件，播放声音等。    
        注意：View上的其他方法不应直接调用此方法，而应调用performClickInternal()，以确保在必要时通知自动填充管理器（因为子类可以扩展此方法而无需调用super.performClick()）。
        View.performClick：
            //我们仍然需要调用此方法来处理在外部而不是通过performClickInternal()调用performClick()的情况
            notifyAutofillManagerOnClick();
            final boolean result;
            final ListenerInfo li = mListenerInfo;
            if (li != null && li.mOnClickListener != null) {        //OnClickListener
                playSoundEffect(SoundEffectConstants.CLICK);    //播放声音效果？
                li.mOnClickListener.onClick(this);      //执行 onClick
                result = true;
            } else {
                result = false;
            }
            sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);    //同长按，发送事件？
            notifyEnterOrExitForAutoFillIfNeeded(true);    //View支持自动填充的视图时调用，内部最终会调用自动填充相关的接口
            return result;
    点击事件，若我们实现了OnClickListener，则直接返回true，即只要执行了onClick，则表示该事件已被消费，不会再被传递
    在执行onClick前，会播放声音效果（若有），执行onClick后，会调用自动填充相关回调接口（若支持自动填充）
    
    至此，事件传递分析完毕！
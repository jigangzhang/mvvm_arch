
## Android常见问题汇集

    常见问题汇集
    View相关问题

#### 问题解决思路

    ScrollView/RecyclerView等使用smoothScrollBy等相关方法时滑动未生效：
        检查输入参数是否有效；
        检查滑动的同时是否有触发measure、layout等的操作，如：设置margin等；
        检查是否有禁止滑动的设置，如：重写scrollVerticallyBy、canScrollVertically等；
    
#### View相关
    
    getMeasuredWidth/getMeasuredHeight的生效时机：
        在完成measure/onMeasure步骤之后，setMeasuredDimensionRaw方法中
        onMeasure中一定要调用setMeasuredDimension，其中会调用setMeasuredDimensionRaw方法，此时赋的值
    
    getWidth/getHeight的生效时机：
        在完成layout步骤之后，如 getWidth = mRight - mLeft
        mRight、mLeft、mTop、mBottom是在setFrame方法中被赋值的
        setFrame在layout中被调用，setFrame中会判断当前尺寸是否方式变化（mRight - mLeft与旧值的比较）
        之后，若有变化才会调用sizeChange（是在setFrame中调用）、onLayout、onLayoutChange
        
    View.post(Runnable)：
        在AttachedToWindow之前的调用，runnable会等到AttachedToWindow之后再调用，但是是在measure之前？
        AttachedToWindow之后的调用，会发送到handler中执行（UI handler）
        AttachedToWindow是发生在ViewRootImpl.performTraversals中（第一次绘制），在Activity的onResume阶段发生
    
    ViewGroup中测量尺寸的常用方法有 measureChildWithMargins、getChildMeasureSpec，measure阶段
    ViewGroup的性能主要看measure的次数
    View的Visible为GONE时不会进行measure、onLayout
    ViewGroup中一般不重写onDraw（draw中一般不会调用），若需要在ViewGroup中绘制，可重写dispatchDraw()
    ViewGroup中可使用setWillNotDraw(bool)强制绘制（false为绘制），在LinearLayout中有使用，且有重写onDraw(Canvas)
    
    FrameLayout：
        width/height为wrap（非EXACTLY）且子View的width/height为match时，会进行两次测量
        第一次测量时，会调用measureChildWithMargins进行，
        此时，若FrameLayout的width/height为wrap（非EXACTLY）且子View的width/height为match时，将子View添加到缓存到列表中
        第一次遍历测量之后，若缓存的View大于1时，会进行第二次测量（仅仅是对子View含有match的情况下）
        测量的关键方法有：getChildMeasureSpec
    LinearLayout：
        支持使用Divider，ShowDividers：SHOW_DIVIDER_BEGINNING、SHOW_DIVIDER_MIDDLE、SHOW_DIVIDER_END
        子View的weight大于0会导致测量两次
        measureChildBeforeLayout，无论weight是否大于0都会调用到
    
    RelativeLayout：
        有两次测量，但正确使用会降低View层级
        
    ConstraintLayout：
        约束布局，降低View层级
    
    ViewStub：
        在布局渲染优化时考虑使用
    merge：
        用于减少View层级；<merge /> can be used only with a valid ViewGroup root and attachToRoot=true
        使用时ViewGroup不能为null，attachToRoot必须为true
    
    TextView：
        要使文本可被选择、复制，则设置 textIsSelectable 为true
        使TextView可编辑的关键是 setText(text, BufferType.EDITABLE)
    
    TabLayout：
        indicator宽度设置--tabIndicatorFullWidth
    
    在windowFullscreen为true 或windowIsTranslucent 为true时，软键盘弹出效果与false时有差别    
    在windowFullscreen为true时，adjustResize、adjustPan 与非全屏时软键盘显示效果有差别
    
    windowIsTranslucent为true时，动画属性无效，可试一下 Animation.Translucent
    windowIsTranslucent为true时，onStop生命周期不执行
    
#### 常见场景

    Activity与Fragment之间通信：
        适用情景是Activity与其包含的Fragment之间（Activity与其他Activity的Fragment之间使用Intent，因为两个Activity不能同时可见，没有频繁通信的需求？）
        Activity中有Fragment的实例，故可直接使用该实例调用Fragment内的方法获取信息；
        Activity实现Fragment指定的接口，在Fragment内部调用接口方法获取Activity中的信息。
    
    Activity与Service通信：
        使用bindService()；

    需要数据恢复的两种场景：
        场景1：资源相关的配置发生改变导致 Activity 被杀死并重新创建。
        场景2：资源内存不足导致低优先级的 Activity 被杀死。
    Activity对应的数据恢复：
        场景1不考虑在清单文件中配置 android:configChanges 的特殊情况；
        使用onSaveInstanceState与onRestoreInstanceState，适用原始数据和简单对象，因为要本地序列化，比较耗时；
        ViewModel，复杂数据；
        使用onRetainNonConfigurationInstance与getLastNonConfigurationInstance。    
    Fragment对应的数据恢复：
        使用Fragment.setRetainInstance(true)，系统允许Fragment绕开销毁-重建的过程，使用该方法后，不会调用Fragment的onDestory()方法，但仍然会调用onDetach()方法，不会调用Fragment的onCreate(Bundle)方法。因为Fragment没有被重建。onAttach(Activity)与onActivityCreated(Bundle)方法仍然会被调用；
    ViewModel内部采用onRetainNonConfigurationInstance的方式来恢复

    ViewModel在Activity、Fragment之间共享数据：
        在Activity中创建的ViewModel会保存在ViewModelStore中，在配置更改时，会在onRetainNonConfigurationInstance中保存ViewModelStore，从而恢复数据；
        Fragment中创建ViewModel时，会先在Activity的ViewModelStore中查找该ViewModel，从而保证Activity与Fragment使用同一个实例；
        1、在Activity中，同时会在FragmentManager中创建一个FragmentManagerViewModel，并将其保存到Activity的ViewModelStore；
        2、在Fragment中，从宿主Activity或父Fragment中的FragmentManager中获取对应的FragmentManagerViewModel，并使用自身的ChildFragmentManager中mNonConfig变量保存；
        3、将Fragment中所创建的ViewModel与其自身的ViewModelStore关联，并将该ViewModelStore存储在mNonConfig所指向的FragmentManaerViewModel中的mViewModelStores中。
        最终所有ViewModel相关数据都在Activity中有保存？

    多dex处理：
        详见：https://developer.android.google.cn/studio/build/multidex#about
        minSdkVersion 设为 21 或更高的值，则默认情况下启用多 dex 文件，并且您不需要多 dex 文件支持库
        multiDexKeepFile、multiDexKeepProguard的使用（将特定类置于主dex中），见官方文档（上述链接）

#### Jetpack && Androidx

    生命周期：
        使用 LifecycleObserver时，一定要将其注入到某个LifecycleOwner中。
        如Activity中使用ViewModel：getLifecycle().addObserver(mViewModel)
    Jetpack组件使用协程：
        https://developer.android.google.cn/topic/libraries/architecture/coroutines    
    
    DataBinding：
        双向绑定的实现关键是：InverseBindingAdapter
        参考TextViewBindingAdapter：@InverseBindingAdapter(attribute = "android:text", event = "android:textAttrChanged")
            text、textAttrChanged 都需要使用 BindingAdapter 定义实现
    
    Room：
        与LiveData一起使用时：
            先查询，再更新数据（Update）后，更新后的结果会再次通知到LiveData
            onPause时触发更新：不同手机有不同现象，华为手机上onPause前返回数据，oppo在onResume后返回数据

#### NDK编译

    openssl编译：
        linux下编译，下载最新源码，解压 编译
        不是首次编译的话，最好 make clean之后再重新配置执行 Configure，否则可能后出问题
        配置prefix，make install 安装到固定位置
        编译时使用 ./Configure android-arm64 -D__ANDROID_API__=29 no-stdio no-ui --prefix=/home/xxx/openssl/，可解决 undefined reference to 'stdin'/'stderr'问题
    CMakeLists：
        message()信息，可在Build/run中查看（只在第一次编译时打印）
        若Build/run中无信息，可在 .cxx/.externalNativeBuild下的cmake/debug/${android_abi}/build_output.txt中查看
    
    C/C++中常见的问题：
        打印字符串（指针）时，会打印到其他内容，就像是指针指到了临近地方
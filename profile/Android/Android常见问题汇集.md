
## Android常见问题汇集

    常见问题汇集
    View相关问题

#### 问题解决思路

    ScrollView/RecyclerView等使用smoothScrollBy等相关方法时滑动未生效：
        检查输入参数是否有效；
        检查滑动的同时是否有触发measure、layout等的操作，如：设置margin等；
        检查是否有禁止滑动的设置，如：重写scrollVerticallyBy、canScrollVertically等；

#### 屏幕适配

    1dp = 1px * (dpi/160)， px = dp * (dpi / 160)， dpi = width(px) / 360 *160 （360为基准值，即将屏幕宽度默认为360dp）
    设计稿以360dp的宽度为基准，不同的分辨率下的dpi值不同，如：720x1280 的dpi为 320，1080x1920 的dpi为 480
    dp是像素无关的，一般情况下使用dp即可，但是在同样的分辨率，但是不同的dpi值时，显示效果是不一样的，dpi越大，1dp所占的px越多，控件显示越大；dpi越小，1dp所占的px越少，控件显示小
    宽高限定符适配：
        设定一个基准分辨率（如：360x640），其他分辨率都根据这个分辨率计算一个倍数，如：720为2倍的尺寸大小
        在不同尺寸文件内部，根据尺寸倍数编写对应的dimens文件
        运行时，根据dimens引用到对应分辨率下找
        资源文件夹：values-widthxheight，如：value-1080x1920
        只能适配对应分辨率的屏幕，对全面屏的适配会失效，因为分辨率不确定（全面屏高度不一致）
    
    smallestWidth适配（最小宽度）：
        sw限定符适配
        android识别屏幕可用高度和宽度的最小尺寸的dp值（实际就是宽度），然后在资源文件中寻找对应限定符的文件夹下的资源文件
        如：宽为1080px、dpi为480的屏幕的dp值为360，对应 values-sw360dp 文件夹下的资源文件
        使用sw可适配 相同分辨率但不同dpi的屏幕，dp值为：width(dp) /(dpi / 160)，对应的资源文件夹名称为：values-sw<N>dp
        如果没有找到对应文件夹，系统会向下寻找，如没有360，但有350，那么系统会选择values-sw350dp
        尺寸（dimens）值的计算：以360dp为基准，大于360dp的屏幕的dimens值要缩小，小于360dp的dimens值要放大， dimen=dimen*(360/sw)
        缺点：对多个不同屏幕可能会有多个values文件夹，从而导致apk体积增大

    今日头条适配方案：
        直接修改density值，强行把不同分辨率的手机的宽度dp值改为一个统一值
        保证px/density（手机像素宽度）这个值始终是一个统一值，如：360dp
        注意点：所有设计稿都要以一个统一值为基准宽度（如：360dp）
        可能会影响一些第三方库的显示
        px = dp * density，density = dpi / 160， 以360dp为基准：density = width(屏幕宽px) / 360， dpi =  width/360 * 160
        和 density 相关的还有 densityDpi、scaledDensity，我们根据 density 等比修改 densityDpi、scaledDensity
        设置代码（在setContentView之前调用，若未拦截字体设置，可加上scaledDensity）：
            DisplayMetrics appMetrics = application.getResources().getDisplayMetrics();
            DisplayMetrics sysMetrics = Resources.getSystem().getDisplayMetrics();
            int targetDensity = appMetrics.widthPixels / 360;
            appMetrics.density = targetDensity;
            appMetrics.scaledDensity = (sysMetrics.scaledDensity / sysMetrics.density) * targetDensity;
            appMetrics.densityDpi = targetDensity * 160;
    
            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            metrics.density = targetDensity;
            metrics.scaledDensity = (sysMetrics.scaledDensity / sysMetrics.density) * targetDensity;
            metrics.densityDpi = targetDensity * 160;
            Configuration configuration = context.getResources().getConfiguration();
            configuration.densityDpi = targetDensity * 160;
            context.getResources().updateConfiguration(configuration, metrics);
            同时设置Activity和Application的density值
        修改dpi后，可能会遇到一些问题（我遇到的问题）：
            RecyclerView 的 item 中修改未生效（特例，只有一个RecyclerView出现问题，非全部）； 解决：在Adapter中调用上述代码，重新修改context的dpi等
                public BaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                    ScreenHelper.setDefaultDensity((Activity) parent.getContext(), App.getInstance());
                    return super.onCreateViewHolder(parent, viewType);
                }
            代码方式生成的布局被截断，宽高都被截，显示不全（是添加到百度地图上的Marker，只有这一个出现问题，density未生效）；解决：dp转px的方法中使用的density使用手动计算所得（width/360）
            修改的dpi、density、scaledDensity未全部生效，感觉就是部分生效（问题手机是华为10.1系统）

#### View相关
    
    SingleInstance启动模式：
        在新的任务栈中运行，且栈中只允许运行此Activity的一个实例
        这个Activity获得一个唯一的任务栈，且只有它自己在其中运行，如果它以相同的intent再次启动，那么该task将被带至前台，onNewIntent()方法调用。
        如果此Activity尝试启动新Activity，则该新Activity将在单独的任务栈中启动
        affinity（Manifest中对应为taskAffinity），Activity、task、Application的一个属性，task的affinity由第一个入栈的Activity指定，拥有相同affinity的Activity属于同一个task，Activity默认从Application继承该属性，其默认值为包名
        该Activity中启动另一个Activity（非SingleInstance模式），将切换到另一个任务栈（到前台），此时back操作将优先处理新Activity所在的栈，最后处理SingleInstance所在的栈
    
    FLAG_ACTIVITY_NEW_TASK：根据目标Activity的affinity进行匹配，若存在affinity相同的task，则压入栈，否则新建task，同一应用下，若都使用默认affinity，那么此flag无效？
    FLAG_ACTIVITY_CLEAR_TOP：
        检查task中是否存在目标，没有的话压入，若有将目标之上的所有Activity都弹出栈，此时：
            如果同时设置了Flag_ACTIVITY_SINGLE_TOP，则直接使用栈内对应的Activity（两者同时设置就是SingleTask模式）
            若没有设置，则将栈内对应的Activity销毁重建
    
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
    
    View.getWindowVisibleDisplayFrame：
        全面屏兼容问题，在onResume之前，获取到的是不包含导航的高度，之后是实际高度？？？？
        全面屏兼容问题，Popupwindow不适用时 getWindowVisibleDisplayFrame 的使用
    
    ViewGroup中测量尺寸的常用方法有 measureChildWithMargins、getChildMeasureSpec，measure阶段
    ViewGroup的性能主要看measure的次数
    ViewGroup在没有子View的区域不能触发dispatchTouchEvent：
        子View实现点击事件后，触摸事件生效；
        ViewGroup设置点击事件后，触摸事件生效；
    
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
    
    RecyclerView：
        数据错乱，一般是由ViewHolder的复用引起的，可由设置tag等方式解决；
        item图片闪烁，也是由ViewHolder的复用引起view重绘等，解决：setHasStableIds(true)、复写getItemId、notifyItemChanged，具体原因待看源码；
            // Find from scrap/cache via stable ids, if exists
            if (mAdapter.hasStableIds()) {
                holder = getScrapOrCachedViewForId(mAdapter.getItemId(offsetPosition),
                        type, dryRun);
                if (holder != null) {
                    // update position
                    holder.mPosition = offsetPosition;
                    fromScrapOrHiddenOrCache = true;
                }
            }
        RecyclerView.getChildCount();  //返回列表中的显示个数，不是item总个数
        要使列表某一项滑动至list顶部使用下面：
        ((LinearLayoutManager) mBinding.list.getLayoutManager()).scrollToPositionWithOffset(i, 0);    
        list.scrollToPosition(i)，只是使某一项滑动至可见
    
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
            先查询，再更新数据（Update）后，更新后的结果会再次通过查询通道通知到LiveData
            onPause时触发更新：可能会在onPause前返回数据，或者在onResume后返回数据
        查询操作返回LiveData：
            根据源码RoomTrackingLiveData，触发onActive时，会从数据库中查询数据，并通知到LiveData（postValue）
            即只要更新、查询、删除操作返回的LiveData对象没有被回收，触发onActive时就可能会多次收到数据
            更新、查询、删除操作完成都会调用endTransaction，最终都会触发 RoomTrackingLiveData中的 mRefreshRunnable
            onActive的触发条件：Activity生命周期变化等等
            联动详细讲解：https://blog.csdn.net/weixin_34358092/article/details/91431952

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

#### String相关

    String的内存分配：
        字符串常量池，JDK1.7及以后，将字符串常量池从方法区移动到堆
        String str = "abs";     //这种声明方式叫作字面量声明，把字符串用双引号包起来，然后赋值给一个变量，会把字符串放到字符串常量池，然后返回给变量
        String str1 = new String("abs"); //这种方式，不管在字符串常量池中有没有，都会在堆中创建一个新的对象
        上面两种方式创建的对象引用指向不同的位置，用 == 比较返回false
        String.intern，native方法，作用是：
            如果当前字符串存在于字符串常量池（判断条件是equals为true），就返回这个字符串在字符串常量池中的引用
            如果不存在，就在字符串常量池中创建一个引用，指向堆中已存在的字符串，然后返回对应的字符串常量池的引用
        上面的str == str1，返回false，地址不同
        str1 = str1.intern()，然后 str == str1，返回true，指向字符串常量池中的同一个地址
    
    StringBuffer：
        线程安全的，同步方法（synchronized）
        使用字符数组保存，默认初始容量为16，或字符串长度+16
        toSting中，new String的方式返回字符串
    
    StringBuilder:
        非线程安全
        toString中，使用一个native方法 StringFactory.newStringFromChars 返回字符串
        其他同StringBuffer
    二者都继承AbstractStringBuilder    
    String中，使用native方法操作字符串    

#### 集合

    HashMap：数组+单向链表实现
        初始容量（DEFAULT_INITIAL_CAPACITY）默认为16，默认加载因子（DEFAULT_LOAD_FACTOR）为0.75
        阈值（threshold）为：位运算，CAPACITY的2被幂
        key、value、hash组成一个Node，hash为key的hashCode值
        根据hash和数组长度确定Node在数组中的位置（position=hash & (n-1)）
        Node是个链表结点
        存取数据时，根据key找到对应位置，与该位置处的Node的key做比较，若key相等，进行存取操作，否则，沿着链表查看下一结点
        当存放的数据容量（size）大于threshold（阈值）时，会进行resize，扩容，容量为2的幂，阈值也会相应增大，
            已存在的数据会重新确定对应位置（index计算方式见上面）
    
    LinkedHashMap：继承HashMap，比HashMap多了一个双向链表
        head、tail，对应双向链表的头、尾指针
        结点类型为LinkedHashMapEntry，其中有before、after，指向结点的前后结点
        每次做保存数据时，对新加的结点进行before、after结点的链接，同时也会调用linkNodeLast 移动tail指针
        做遍历操作时，使用双向链表：head、tail进行遍历
        
    HashSet：构造函数中创建了一个HashMap，所有操作（存取等）都是通过HashMap实现，存到数据对应HashMap的key，value是PRESENT（是个伪值）    

    LinkedHashSet：仅仅是继承HashSet
    
    ArrayList：内部使用数组实现
        初始化时，若不指定大小，elementData数组会初始化为一个空数组，第一次添加数据时，数组容量会指定为默认值
        初始容量（DEFAULT_CAPACITY）默认为10，大于10后，会自增，但不能超过最大容量
        数组允许的最大容量为 Integer.MAX_VALUE - 8
        数组增长算法：new>= oldCapacity + (oldCapacity >> 1)
        每次添加数据时，都会检查数组容量，+1后是否会超过数组长度，若超过就扩容
        trimToSize，将数组长度减小到size（数据数量）

    LinkedList：使用双向链表实现，内部还有栈、队列的方法实现
        结点，Node：next、pre，双向链表的指针
        常用的双向链表操作
        删除时，仅仅只是将item的pre、next置为null

    Vector：ArrayList的线程安全版，同步方法
    HashTable：HashMap的线程安全版，同步方法

#### 二进制运算

    <<：表示左移，不分正负数，低位补0，如：-16<<2，其补码：1111 0000，左移两位：1100 0000（低位补了0）-64
        num << 1，相当于num乘以2，表示加位
    >>：表示右移，如果该数为正，则高位补0，若为负数，则高位补1，如：-16>>2，其补码：1111 0000，右移两位：1111 1100，取其原码：1000 0100，-4（正数取补码，负数取原码）
        num >> 1，相当于num除以2，表示减位
    移位符号右侧的整数表示的是2的幂
        
    >>>：表示无符号右移，也叫逻辑右移，即若该数为正，则高位补0，而若该数为负数，则右移后高位同样补0
        忽略符号位，空位都以0补齐
    计算机指令中的右移位运算符有2种，左移就是补0
    算术右移：移动后补的是最高位的值，正数补0，负数补1
    逻辑右移：移动后补的是最高位的值，正负都补0
    
    ^：二进制的按位异或运算，对应位置不同则为1，相同则为0
    &：同位与运算，对应位置只有都为1时才为1，否则为0
    |：逻辑或运算，对应位置有1时为1，否则为0
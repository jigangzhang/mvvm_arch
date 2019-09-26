
## ART中的GC

    Garbage Collection，GC
    属于内存管理范畴
    用于解决释放谁的内存以及何时释放内存

#### GC基础
    
    Mark-Sweep Collection、Copying Collection、Mark-Compact Collection、Reference Counting，四种GC基本方法
    GC方法的指标：吞吐量（throughput）、暂停时间（pause time）、空间利用率（space overhead）等
    
    Mark-Sweep Collection原理：
        标记-清除回收法
        Mark阶段：搜索内存中的Java对象（ART中，遍历mirror Object对象），对那些能搜到的对象进行标记
        Sweep阶段：释放那些没有被标记的对象所占据的内存
        GC前：
            虚拟机中，内存根据其用途大体可分为堆内存（堆，heap）和栈内存（栈，stack，主要用于函数调用）两种
            通过new创建的Java对象的内存位于堆中（ART中的Heap是一个类，其作用更像是堆内存的管理器）
            Object VisitReferences函数可用于搜索（追踪，trace）一个对象所引用的其他对象
            Mark阶段将从root set出发进行对象遍历，从root set出发并能遍历到的对象被标记为活的对象
            判断一个对象是死是活的关键在于能否从root set出发并能遍历到它们，不在于它们之间的引用关系
            root set是一个集合，保存的信息就是所谓的root（根）信息，通过根不能找到的对象就是垃圾对象
            root具体是什么和虚拟机的实现有关，如ART中JNI kLocal或kGlobal型的IndirectReferenceTable中的一个元素
            虚拟机能直接找到的Object对象都属于root set
        GC后：
            垃圾对象在Sweep阶段被释放
            Mark-Sweep的问题--内存碎片化
    
    Copying Collection原理：
        Copying Collection将堆分为大小相同的两个空间--semispace，一个叫fromspace，另一个空间叫tospace
        对象的内存分配只使用tospace，虚拟机实现中，一般会有两个不同的变量分别指向这两个空间
        tospace空间不够用时将触发GC
        GC的第一个工作是空间flip--就是将指向这两个空间的变量进行互换，原来指向fromspace的变量指向tospace，原来指向tospace的变量现在指向fromspace
        然后，从root set开始遍历，将遍历过程中访问到的对象从fromspace拷贝到tospace（上面已互换），过程不需要标记
        拷贝时可以将对象的内存连续排放，通过这种方式，内存碎片化问题得以解决
        当fromspace中活对象全部拷贝完后，该空间的内存就可以整体释放
        缺点：空间利用率太低，只有50%--只能使用两个semispace中的一个用于内存分配
    
    Mark-Compact Collection原理：
        可以说是Mark-Sweep和Copying Collection两种GC方法的综合体
        Mark-Compact有一个Mark阶段，从root set出发遍历对象以标记存活的对象，没有被标记的对象则认为是垃圾对象
        标记之后进入Compact（压缩）阶段，压缩是指内存压缩，其实就是将存活对象挪到一起去
        Mark-Compact相比Mark-Sweep解决了内存碎片问题，比Copying Collection极大地提高了内存空间的使用率
        Mark-Compact需要遍历heap至少两次，对程序性能影响非常明显
        
    其他概念：
        mutator和collector：collector表示内存回收相关的功能模块，mutator一般情况下代表应用程序中除collector之外的其他部分
        Incremental Collection：
            增量式回收
            早期的GC，垃圾回收扫描全部的堆内存，需要暂停所有其他非GC线程的运行才能执行一次GC
            增量式回收可以每次只针对heap的一部分做GC，从而大幅减少停顿时间
            分代GC（generational GC）只是增量式回收的一种实现形式
            分代GC中，heap被划分为新生代、老年代等部分，GC往往只针对其中某一代（一部分内存），符合增量式回收的定义
        Parallel Collection：
            并行回收，指程序中有多个垃圾回收线程，它们可以同时执行回收工作中的某些任务
            比如对Mark-Sweep，可以使用多个线程来做标记工作
        Concurrent Collection：
            并发回收，指程序中垃圾回收线程虽然只有一个，但在回收工作的某个阶段，回收线程可以和其他非回收线程（mutator）同时运行，这样对程序运行的影响更小
            不使用Concurrent Collection的话，回收线程在工作的时候可能就需要暂停mutator线程的执行（stop-the-world）
            
#### Runtime VisitRoots

    确定root set包含哪些root是隐含在GC方法里的一个重要工作
    root set就是包含了一些Java对象（mirror Object），这些Object对象并不是通过其他Object对象的引用型成员变量来找到，而只能由虚拟机根据其实现的特点来确定
    ART虚拟机中，root的类型可由RootType枚举变量来描述：
        RootType：
            kRootUnknown=0，
            kRootJNIGlobal，kRootJNILocal，kRootJavaFrame，kRootNativeStack，kRootStickyClass，
            kRootThreadBlock，kRootMonitorUsed，kRootThreadObject，kRootInternedString
            kRootFinalizing，kRootDebugger，kRootReferenceCleanup，和HPROF（A Heap/CPU Profiling Tool，性能调优工具）以及调试有关
            kRootVMInternal，kRootJNIMonitor
        VisitRoots函数访问虚拟机进程中的所有root
        Runtime VisitRoots：
            RootVisitor是一个纯虚类，其定义函数供root访问时调用，参数flags 默认值为kVisitRootFlagAllRoots，表示访问所有的root
            VisitNonConcurrentRoots，
            VisitConcurrentRoots
        Runtime VisitNonConcurrentRoots：
            thread_list_->VisitRoots，调用所有Thread对象的VisitRoots函数
            VisitNonThreadRoots
        Runtime VisitNonThreadRoots：
            java_vm_->VisitRoots，调用JavaVMExt的VisitRoots函数
            sentinel_.VisitRootIfNonNull，sentinel_是Runtime的成员变量，类型为GcRoot<Object>，对应Java层的Object对象
            pre_allocated_OutOfMemoryError_、pre_allocated_NoClassDefFoundError_，类型为GcRoot<Throwable>，属于由虚拟机直接创建的Java Object对象
            调用RegTypeCache的VisitStaticRoots函数
            VisitTransactionRoots
        Runtime VisitConcurrentRoots：
            intern_table_->VisitRoots，intern_table_类型为InternTable，和Intern String有关
            class_linker_->VisitRoots，调用ClassLinker的函数
            VisitConstantRoots
            Dbg::VisitRoots，和调试有关
        root set包含的内容大致可从下面几方面获取：
            每一个Thread对象的VisitRoots函数
            JavaVMExt的VisitRoots
            Runtime成员变量sentinel_、pre_allocated_OutOfMemoryError_、pre_allocated_NoClassDefFoundError_，这三个变量代表Java层的三个对象，由虚拟机直接持有，root类型为kRootVMInternal
            RegTypeCache VisitStaticRoots函数
            InternTable VisitRoots函数
            ClassLinker VisitRoots函数
            Runtime VisitConstantRoots函数等
            
    关键数据结构：
        在gc_root.h中声明，辅助类型
        RootInfo：
            用于描述一个root的信息，包括该root的类型以及所在的线程id
            type_，该root的类型
            thread_id_，该root所在的线程，默认为0，不是所有root信息都和线程有关系
        GcRoot：
            模板类，模板参数表示mirror Object对象的类型
            一个GcRoot实例就代表一个被认为是根的Object对象（称之为root Object或 根Object）
            root_，唯一的成员变量，类型为CompressedReference，CompressedReference中只有一个reference_，是某个Object对象的内存地址，所以，root_也就代表某个Object对象
            VisitRoot函数，root访问函数，访问的就是root_的一个对象
        GcRootSource：
            field_，
            method_
        RootVisitor：
            虚类
            VisitRoots，虚函数，用于访问一组root Object对象
            VisitRoot，辅助函数，用于访问单个root Object对象
        BufferedRootVisitor：
            用于收集root Object然后再一次性访问它们
            visitor_，RootVisitor类型，该类本身不是RootVisitor
            roots_，数组，最大容量由模板参数kBufferSize决定，该数组中的root Object对应同一种RootType
            root_info_，
            buffer_pos_，roots_数组中的元素个数
            VisitRoot函数，把root存到roots_数组中，若数组已满，则调用Flush
            Flush函数，一次性访问roots_数组中的root Object内容
    
    和线程有关的root Object
    一个root Object实际就是一个Java层的对象，和非root的Object没有区别
    Thread VisitRoots：
        thread_id，返回的是thin_lock_id，是由虚拟机自己维护的用于线程同步的id，不是OS里的tid
        tlsPtr_opeer指向一个Java层Thread对象，是一个mirror Thread对象在Java层的对应物，这类根Object的类型为kRootThreadObject
        GetDeoptimizationException，和HDeoptimize有关的处理
        使用kRootNativeStack作为tlsPtr_ exception的root类型
        tlsPrt_ monitor_enter_object指向用于monitor-enter的那个Java对象
        locals.VisitRoots，tlsPtr_ jni_env locals的类型为IndirectReferenceTable（IRTable）
        monitors，tlsPtr_ jni_env monitors与synchronized修饰的java native函数的调用有关，用于同步native函数的调用
        HandleScopeVisitRoots，和JNI有关，将找到那些传递给了native函数的引用型对象
        RootCallbackVisitor、ReferenceMapVisitor，遍历线程的调用栈帧，找到其中所有引用型的参数，把它们视为根对象
        mapper.WalkStack
    作为函数调用的引用型参数的对象，也属于root的一种，其对应root类型为kRootJavaFrame
    ReferenceMapVisitor：
        派生自StackVisitor
        用于遍历调用栈，找到其中的引用型参数
        VisitFrame，每找到一个函数调用的栈帧就会调用这个函数，内部调用VisitShadowFrame和VisitQuickFrame
        VisitShadowFrame：
            解释执行模式下的栈帧，解释执行模式下，每调用一个函数都会创建一个ShadowFrame对象，其中存储了函数调用所需的参数
            VisitDeclaringClass，访问ArtMethod对应函数所在的类的对象（也是一种root对象）
            shadow_frame->GetVRegReference，获取栈帧中的参数
            visitor_，是Thread VisitRoots中的RootCallbackVisitor函数对象，用于访问root对象
            SetVRegReference，如果root对象被visitor_修改了，则需要更新栈帧中的值
        VisitDeclaringClass，访问method所属的Class对象
        VisitQuickFrame：
            机器码执行模式下的栈帧，用于访问机器码模式下的函数栈帧
            VisitDeclaringClass，
            从栈帧中找到引用型参数
            GetNumberOfStackMaskBits
            获取存储在栈上的参数，visitor_，Assign
            获取存储在寄存器中的参数信息，map.GetRegisterMask
            
#### ART GC概览

    关键数据结构：
        GarbageCollector：
            虚基类，是ART中垃圾回收器的代表，GC工作就是从GarbageCollector的Run函数开始的
            Run函数由GarbageCollector类实现，但GC的具体工作则由其子类实现的RunPhases函数来处理
            RootVisitor、MarkObjectVisitor、IsMarkedVisitor，GarbageCollector实现的三个接口类
            4个GarbageCollector的直接子类：MarkSweep、MarkCompact、ConcurrentCopying、SemiSpace
        Iteration：
            统计信息，包括一次GC暂停其他线程运行的时间、GC运行的时间、回收了多少字节的内存等
        CollectorType：
            枚举变量，垃圾回收器类型
            kCollectorTypeNone
            kCollectorTypeMS、kCollectorTypeCMS，和MarkSweep类有关
            kCollectorTypeSS、kCollectorTypeGSS，和SemiSpace类有关
            kCollectorTypeMC，和MarkCompact类有关
            kCollectorTypeHeapTrim，和GarbageCollector类家族无关
            kCollectorTypeCC，和ConcurrentCopying类有关
            kCollectorTypeAddRemoveAppImageSpace，和GarbageCollector类家族无关
            kCollectorTypeHomogeneousSpaceCompact，和CMS有关
            kCollectorTypeClassLinker，和GarbageCollector类家族无关
        GcType：
            枚举变量，代表回收策略
            kGcTypeNone
            kGcTypeSticky，表示仅扫描和回收上次GC到本次GC这个时间段内所创建的对象
            kGcTypePartial，仅扫描和回收应用进程自己的堆，不处理zygote的堆，和Android中Java应用程序的创建方式有关
            kGcTypeFull，力度最大的一种回收策略，扫描App自己以及它从父进程zygote继承得到的堆
            kGcTypeMax
        
        GarbageCollector Run：
            GcCause，枚举变量，表示触发本次gc的原因
            start_time，本次GC的GC开始时间
            GetCurrentIteration，返回Heap current_gc_iteration_成员变量，用于统计GC的执行效果
            RunPhases，由子类实现，完成真正的GC工作，GC结束
            total_freed_objects_、total_freed_bytes_，代表虚拟机从运行开始所有GC操作释放的对象总个数以及内存大小总数
            GetFreedObjects、GetFreedBytes，返回一次GC所释放的对象个数以及内存大小（即每次调用Run函数）
            end_time，本次GC的结束时间
            SetDurationNs，设置本次GC的耗时时间
            total_time_ns_，更新暂停时间以及总的GC运行时间等统计信息
        
    ART GC选项：
        通过mk文件来设定默认的回收器类型：Android.common_build.mk，ART_DEFAULT_GC_TYPE ?= CMS
        默认回收器类型由静态常量kCollectorTypeDefault表示，在ART虚拟机启动时用于控制GC类型的运行参数XGcOption中使用
        通过设置属性的方式来设置回收器类型：
            AndroidRuntime::startVm中的parseRuntimeOption，dalvik.vm.gctype、dalvik.vm.backgroundgctype，设置App位于前台/后台时的回收器类
        最终都是通过XGcOption选项来获取最终的回收器类型
        ParsedOptions::DoParse：
            low_memory_mode_，如果设备属性 ro.config.low_ram 为true，则这个参数为true
            BackgroundGc对应 -XX:BackgroundGC= 选项
            XGcOption对应 -XGc: 选项
        Heap构造函数中，传入了foreground_collector_type_和background_collector_type_参数值，在Runtime::Init中
        前台回收器为CMS时，后台回收器为HSC
        前台回收器为SS时，后台回收器为HSC（HomogeneousSpaceCompact）
        前台回收器为GSS时，后台回收器类型也必须是GSS
    
    创建回收器和设置回收策略：
        ART中，回收器对象将在Heap构造函数中根据使用的回收器类型来创建，垃圾回收器的类型决定了内存分配器的类型
        Heap 构造函数：
            ChangeCollector，设置回收器类型和回收策略
            创建Space对象等工作
            for循环创建回收器：
                garbage_collectors_，数组，元素类型为GarbageCollector*
                MayUseCollector，检查前后或后台回收器类型是否为输入的回收器类型，只要有一个回收器类型满足条件，则返回true
                会创建MarkSweep、PartialMarkSweep、StickyMarkSweep上垃圾回收器对象
                kMovingCollector，默认为true
                generational，前台回收器类型为GSS时，才为true
                semi_space_collector_，如果使用SS、GSS或HSC，则再创建一个SemiSpace collector对象
        回收器类型为CMS时，前台回收器类型为CMS，后台回收器类型为HSC
        garbage_collectors_包含四个回收器对象，MarkSweep、PartialMarkSweep、StickyMarkSweep启用concurrent gc功能，SemiSpace关闭分代gc的功能
        回收器类型为SS时，前台回收器类型为SS，后台回收器类型为HSC
        garbage_collectors_包含一个SemiSpace回收器对象，SemiSpace关闭分代gc的功能，generational为false
        回收器类型为GSS时，前后台回收器类型都为GSS
        garbage_collectors_包含一个SemiSpace回收器对象，SemiSpace启用分代gc的功能，generational为true
        
        Heap::ChangeCollector：
            为不同的回收器设置不同的内存分配器
            为不同的回收器设置不同的回收策略
            collector_type_、gc_plan_，均为Heap成员变量
            gc_plan_为数组，保存了回收策略，ART 在GC时将用到它
            kCollectorTypeGSS：设置内存分配器类型为kAllocatorTypeBumpPointer，回收策略有kGcTypeFull
            kCollectorTypeCMS：设置内存分配器类型为kAllocatorTypeRosAlloc，回收策略有kGcTypeSticky、kGcTypePartial、kGcTypeFull
            IsGcConcurrent，判断collector_type_是否为CMShuoCC
            concurrent_start_bytes_，和concurrent gc有关，kMinConcurrentRemainingBytes取值为128KB
        如果回收器类型为CMS，则gc_plan_依次为kGcTypeSticky、kGcTypePartial、kGcTypeFull
        如果回收器类型为SS或GSS，则gc_plan_只有kGcTypeFull一种策略
        
#### MarkSweep

    基于Mark-Sweep collection原理
    Heap相关成员变量取值情况：
        Heap continuous_spaces_数组的成员以及与之关联的RememberedSet或ModUnionTable的情况
        Heap live_bitmap_和mark_bitmap_的情况，数据类型为HeapBitmap
        Heap mark_stack_、live_stack_及allocation_stack_的情况，都是ObjectStack，代表线程的Allocation Stack
        HeapBitmap有一个数组成员变量（continuous_space_bitmap_），其成员为各个ContinuousSpace对象中对应的位图成员
        main_space_和non_moving_space_内部包含三个ContinuousSpaceBitmap成员变量，分别叫live_bitmap_、mark_bitmap_和temp_bitmap
        一个ContinuousSpaceBitmap位图和一个ContinuousSpace对象关联，该位图就管理这个空间对象所覆盖的内存范围
        Heap管理多个ContinuousSpace对象，所以，HeapBitmap用于管理这些ContinuousSpace对象中的ContinuousSpaceBitmap位图
        一个HeapBitmap对象可以管理多个ContinuousSpace的某一种位图对象
        mark_stack_等三个成员变量分别指向三个AllocationStack对象，其中，各个Thread对象使用allocation_stack_提供的空间来存储在该线程上所创建的对象
    
    MarkSweep概貌：
        MarkSweep家族包含MarkSweep、PartialMarkSweep和StickyMarkSweep三个类成员
        MarkSweep是基类，PartialMarkSweep是其子类，StickyMarkSweep又是PartialMarkSweep的子类
        
        MarkSweep::GetCollectorType（Partial和Sticky均未重载该函数）：
            is_concurrent_，为MarkSweep的成员变量，如果为true，则返回kCollectorTypeCMS
            return is_concurrent_ ? kCollectorTypeCMS : kCollectorTypeMS
        MarkSweep家族的三个类都对应同一种回收器类型--CMS或MS
        
        MarkSweep::GetGcType：
            return kGcTypeFull，最强力度的GC策略
            支持的回收策略为kGcTypeFull，两个子类重载了该函数
            StickyMarkSweep，kGcTypeSticky，利用Allocation Stack保存两次GC过程中创建的对象
            PartialMarkSweep，kGcTypePartial，只处理除ImageSpace和ZygoteSpace外的其他空间中的对象
            MarkSweep，kGcTypeFull，处理除ImageSpace外其他所有空间中的对象
        MarkSweep 构造函数：
            current_space_bitmap_，类型为ContinuousSpaceBitmap*
            mark_bitmap_，类型为HeapBitmap*
            mark_stack_，类型为ObjectStack*
            MemMap::MapAnonymous，创建一个内存映射对象，ART内部大量使用内存映射对象
            sweep_array_free_buffer_mem_map_，可看作为一块内存
        MarkSweep::RunPhases：
            InitializePhase，初始化MarkSweep类的几个成员变量，其中，MarkSweep的mark_bitmap_将设置为Heap的成员变量mark_bitmap_
            if IsConcurrent，返回true，则是CMS的行为
                ReaderMutexLock，mutator_lock_为真正用于同步的关键对象，ReaderMutexLock在构造函数中针对mutator_lock_申请一个读锁，在析构函数中释放读锁
                MarkingPhase，标记工作
                ScopedPause，辅助类，构造函数中暂停调用线程外其他所有Java线程的运行，其内部调用ThreadList的SuspendAll，析构函数中恢复这些线程的运行
                PausePhase，
                RevokeAllThreadLocalBuffers，撤销线程对象的TLAB空间，此后Thread TLAB空间为0，TLAB的作用在于加速内存分配的速度
            false，则为MS
                ScopedPause pause，回收器类型为MS，先暂停其他Java线程的运行
                MarkingPhase，
                PausePhase，
                RevokeAllThreadLocalBuffers，撤销TLAB空间，创建在TLAB上的对象依然存在，这些对象中的垃圾对象将在后续清除阶段回收
            ReaderMutexLock mu，mutator_lock_，标记相关的工作结束，开始准备清除工作，CMS或MS，清除任务可以和mutator同时执行
            ReclaimPhase，回收工作（上面的清除任务）
            FinishPhase，GC的收尾
        MarkSweep中完成GC相关工作的4个核心函数是MarkingPhase、PausePhase、ReclaimPhase和FinishPhase
        MarkSweep既支持Concurrent Collection（并发），也支持Parallel Collection（并行）
    
    MarkingPhase：
        Mark-Sweep中有两个信息共同决定哪些对象为垃圾对象：
            1、进程中当前存在的所有对象，可以用集合Live来表示它们
            2、标记时能被扫描到的对象，可以用集合Mark表示它们
            最终的垃圾对象就是集合Live减去集合Mark
        MarkingPhase函数功能就是确定集合Mark
        MarkSweep::MarkingPhase：
            BindBitmaps、FindDefaultSpaceBitmap、ProcessCards，标记前的准备工作
            MarkRoots，标记相关函数
            MarkReachableObjects，
            preCleanCards，和CMS有关
        
        准备工作：
        MarkSweep::BindBitmaps：
            Heap->GetContinuousSpaces，
            kGcRetentionPolicyNeverCollect，
            immune_spaces_.AddSpace，
            搜索Heap continuous_spaces_数组中的空间对象
            如果某个空间对象的gc_retention_policy_成员变量取值为kGcRetentionPolicyNeverCollect，则将它加入MarkSweep immune_spaces_中
            只有ImageSpace满足这个条件
        ImmuneSpaces::AddSpace：
            GetLiveBitmap、GetMarkBitmap，ImageSpace重载了这两个函数，返回的都是ImageSpace的live_bitmap_成员
            space->AsContinuousMemMapAllocSpace->BindLiveToMarkBitmap，MarkSweep中用不到
            spaces_.insert，是ImmuneSpace的成员，std set集合
        MarkSweep::FindDefaultSpaceBitmap：
            遍历Heap continuous_space_数组
                GetMarkBitmap，
                满足条件 kGcRetentionPolicyAlwaysCollect，将把空间对象中的mark_bitmap_赋值给MarkSweep的成员变量current_space_bitmap_
        FindDefaultSpaceBitmap用于搜索Heap的连续内存空间对象，然后找到其中的某个Space并将它的mark_bitmap_赋值给MarkSweep成员变量current_space_bitmap_
        集合Mark在代码中的体现就是mark_bitmap_所表示的位图对象
        Heap::ProcessCards：
            遍历continuous_spaces_数组
                FindModUnionTableFromSpace，找到和这个space关联的ModUnionTable
                FindRememberedSetFromSpace，找到和这个space关联的RememberedSet
                在Heap PreZygoteFork被调用前，ImageSpace对象关联了一个ModUnionTable对象，其余两个空间对象个关联了一个RememberedSet对象
                table->ClearCards，如果card旧值为kCardDirty，则设置其新值为kCardDirty-1，否则设置其新值为0
                card_table_->ClearCardRange，将设置对应card的值为0
                card_table_->ModifyCardsAtomic，修改对应内存范围的card的值，如果card旧值为kCardDirty，则设置其新值为kCardDirty-1，否则设置其新值为0
                
        标记工作：
        MarkRoots，用于标记根对象
        MarkReachableObjects，从根对象出发以标记所有能追踪到的对象
        MarkSweep::MarkRoots：
            if IsExclusiveHeld，若true，说明其他Java线程已暂停运行
                Runtime VisitRoots，
                RevokeAllThreadLocalAllocationStacks，遍历所有Thread对象，设置tlsPtr_ thread_local_alloc_stack_end和thread_local_alloc_stack_top为空，即收回线程的Allocation Stack空间
            上面为false，后面是和CMS有关
        MarkSweep::VisitRoots： for循环调用MarkObjectNonNull函数，访问roots数组元素
        MarkSweep::MarkObjectNonNull：
            if immune_spaces_.IsInImmuneRegion，如果obj位于immune_spaces_包含的空间对象中，则无需标记
            if current_space_bitmap_->HasAddress，检查current_space_bitmap_对应的内存空间是否包含obj对象
                current_space_bitmap_->Set，设置obj对应位的值为1，这个Obj就算被标记了
                PushOnMarkStack，Set函数返回旧值，若旧值为0，说明这个obj之前没被标记，调用该函数将obj加入到mark_stack_容器中
            else，说明obj不在current_space_bitmap_所关联的那个空间对象中，此时需要搜索Heap所有的空间对象，比直接操作current_space_bitmap_耗时，使用current_space_bitmap_是一种优化处理
                mark_bitmap_->Set，mark_bitmap_指向一个HeapBitmap对象，就是Heap中的mark_bitmap_，set函数搜索所有空间对象，找到包含这个Obj的Space对象，然后设置对应位的值
                PushOnMarkStack
        MarkObjectNonNull，对Obj进行标记，标记就是设置该Obj所在空间对象的mark_bitmap_位图对应位的值为1，然后将被标记的Obj保存到MarkSweep mark_stack_容器中
        MarkSweep::MarkReachableObjects：
            遍历根对象，沿着它们的引用型成员变量顺藤摸瓜进行追踪，期间所找到的对象都将被标记
            UpdateAndMarkModUnion，处理immune_spaces_空间中的对象
            RecursiveMark
        MarkSweep::UpdateAndMarkModUnion：
            for循环，遍历immune_spaces_.GetSpaces 的space
                space要么是ImageSpace，要么是ZygoteSpace，如果它们关联一个ModUnionTable对象，则通过ModUnionTable的UpdateAndMarkReference来处理，否则通过它们的live_bitmap_来处理
                FindModUnionTableFromSpace，找到ModUnionTable对象，即是否与ModUnionTable对象关联
                mod_union_table->UpdateAndMarkReferences，
                live_bitmap_->VisitMarkedRange，如果该空间没有关联ModUnionTable，则遍历该空间的所有存活对象
        UpdateAndMarkModUnion的最终结果是那些被immune_spaces_里的对象所引用并且位于其他空间中的对象都将被标记
        MarkSweep::RecursiveMark：
            if kUseRecursiveMark，kUseRecursiveMark编译常量，默认值为false
            ProcessMarkStack(false)
        MarkSweep::ProcessMarkStack：
            遍历mark_stack_中的obj对象，追踪它们的引用型参数，追踪根对象的引用型成员变量是非常耗时的工作，可利用多线程来处理
            GetThreadCount，其中涉及kProcessStateJankPerceptible枚举变量，当应用处于前台时，进程状态被设置为这个值，表示应用卡顿用户可感知
            ProcessMarkStackParallel，使用parallel gc，注意其if使用条件
            不使用parallel gc时，遍历mark_stack_找到元素，调用它们的VisitReference函数，每找到一个引用型参数就调用MarkSweep MarkObject函数进行标记，如果是新标记的对象，就将其加入mark_stack_容器中
            最后，ScanObject，内部调用Object VisitReferences，所设置的函数调用对象最终会通过MarkSweep MarkObject函数来标记所找到的引用型成员变量
        到此，进程中当前能被追踪到的对象均已完成标记，标记的信息保存在各个空间的mark_bitmap_中
    
    PausePhase：
        CMS或MS，在PausePhase运行时，其他Java线程均暂停运行，PausePhase执行时间越短越好
        MarkSweep::PausePhase：
            if IsConcurrent，是CMS，需要调用下面两个函数
                ReMarkRoots，对CMS而言，MarkingPhase标记的对象可能不全面（因mutator线程可同时运行），在PausePhase时重新做一次标记
                RecursiveMarkDirtyObjects
            WriterMutexLock heap_bitmap_lock_，写锁，使用全局静态变量heap_bitmap_lock_同步对象
            heap_->SwapStacks，将live_stack_和allocation_stack_内容进行交换
            RevokeAllThreadLocalAllocationStacks，再次清空Thread的Allocation Stack空间，在MarkRoots函数中调用过该函数，再次执行它，是因为在MarkRoots中清空之后到此时，可能有mutator线程又重新分配和使用了Allocation Stack
            Runtime DisallowNewSystemWeaks，
            GetHeap->GetReferenceProcessor->EnableSlowPath，和Java Reference对象的处理有关
        PausePhase主要功能（和ART虚拟机实现密切相关）：
            1、若回收器类型为CMS，则需要重新找一遍根对象并搜集新的存活对象，和Concurrent GC有关
            2、交换Heap的allocation_stack_和live_stack_的内容，交换前，live_stack_内容为空，allocation_stack_保存了mutator线程创建的对象
                交换后，live_stack_保存了mutator创建的对象，而allocation_stack_则变为空的容器，后续StickyMarkSweep将用到
            3、禁止系统其它模块创建或解析新的 weak 对象，由Runtime DisallowNewSystemWeaks实现
            4、处理和Java引用型对象有关的工作，和ReferenceProcessor EnableSlowPath函数有关
        ART GC相比Dalvik GC的一大改进点就是PausePhase的运行时间比较短，RunPhases中，collector线程执行PausePhase时，mutator线程必须暂停运行，所有PausePhase运行时间越短越好
        Runtime DisallowNewSystemWeaks：
            控制虚拟机其他模块禁止创建或解析新的 Weak 对象
            monitor_list_->DisAllowNewMonitors
            intern_table_->ChangeWeakRootState
            java_vm_->DisallowNewWeakGlobals，禁止JNI层创建新的WeakGlobal对象，或解析一个WeakGlobal对象
            heap_->DisallowNewAllocationRecords
            lambda_box_table_->DisallowNewWeakBoxedLambdas
        JavaVMExt::DisallowNewWeakGlobals：
            allow_accessing_weak_globals_，为Atomic<bool>类型，设置其值为false
        DisallowNewWeakGlobals就是设置allow_accessing_weak_globals_变量的值为false，该变量的影响主要体现在JNI层创建一个WeakGlobal对象的地方
        一个WeakGlobal对象A实际上指向一个Local型对象B，GC时，A不会被回收，但是B可能会被回收，所以使用A时需要调用JNI IsSameObject判断A是否等于空指针，若为空，则A指向的B已被回收
        NewWeakGlobalRef：
            JNI层创建WeakGlobal对象，将调用该函数
            soa.Vm->AddWeakGlobalRef
        JavaVMExt::AddWeakGlobalRef：
            MayAccessWeakGlobals，将检查allow_accessing_weak_globals_的值是否为true，如果不满条件，则需要等待
            WaitHoldingLocks，等待
        禁止JNI层创建新的WeakGlobal对象，包括解析WeakGlobal对象，对应的函数是JavaVMExt DecodeWeakGlobal函数
    
    ReclaimPhase：
        垃圾对象的内存释放
        RunPhases中，gc线程执行ReclaimPhase时，mutator线程可以同时运行
        MarkSweep::ReclaimPhase：
            ProcessReferences，对Java Reference对象的处理
            SweepSystemWeaks，清除系统中 Weak 型的垃圾对象，WeakGlobal型对象的清除
            runtime->AllowNewSystemWeaks，重新允许 Weak 对象的创建和解析
            ClassLinker->CleanupClassLoaders，清除不再需要的ClassLoader对象
            Sweep，用于清理之前未被标记的对象
            SwapBitmaps，用于处理空间对象中的位图
            Heap->UnBindBitmaps，结合StickyMarkSweep使用
        MarkSweep::SweepSystemWeaks：
            用于清除 weak 型对象，和PausePhase中调用的DisallowNewSystemWeaks相对应
            Runtime->SweepSystemWeaks，参数是 IsMarkedVisitor 类型的对象
        Runtime::SweepSystemWeaks：
            InternTable->SweepInternTableWeaks
            MonitorList->SweepMonitorList
            JavaVM->SweepJniWeakGlobals
            Heap->SweepAllocationRecords
            LambdaBoxTable->SweepWeakBoxedLambdas
        JavaVMExt::SweepJniWeakGlobals：
            for循环遍历weak_globals_中的元素，元素是WeakGlobal型的对象
            entry->Read，调用GcRoot的Read函数，不使用ReadBarrier
            visitor->IsMarked，调用IsMarkedVisitor的IsMarked函数，返回一个Object对象，若为空指针，说明输入obj没有被标记，该obj是垃圾对象
            runtime->GetClearedJniWeakGlobal，返回Runtime的sentinel_成员变量，是一个Java Object对象，在ClassLinker InitFromBootImage函数中创建
            然后修改WeakGlobal型对象的内容
        MarkSweep::IsMarked：
            immune_spaces_.IsInImmuneRegion，obj是否属于immune_spaces_空间中的对象
            current_space_bitmap_->HasAddress，检查obj是否属于该空间，然后判断它是否被标记，current_space_bitmap_来自某个Space对象的mark_bitmap_
            mark_bitmap_就是Heap mark_bitmap_的成员，它将遍历Heap的所有space对象，先判断Object属于哪个空间，然后检查是否被标记
        MarkSweep::Sweep：
            用于清除进程中的垃圾对象
            heap_->GetLiveStack，返回Heap的live_stack_，保存了mutator线程所创建的对象，属于集合Live，只是集合Live的一部分
            heap_->MarkAllocStackAsLive，对live_stack_中的元素进行处理，这些元素是mirror Object对象，属于集合Live，MarkAllocStackAsLive找到这些对象所在的空间，对这些空间对象的live_bitmap_位图进行设置，即集合Live由空间对象的live_bitmap_表示
            live_stack->Reset，清空Heap live_stack_的内容
            heap->GetContinuousSpaces，返回Space集合
            遍历Heap continuous_spaces_成员
                space->IsContinuousMemMapAllocSpace，判断类型
                alloc_space->Sweep(swap_bitmaps)，调用ContinuousMemMapAllocSpace的Sweep函数
            SweepLargeObjects，回收DiscontinuousSpace对象中的垃圾
        ContinuousMemMapAllocSpace::Sweep：
            返回值类型为ObjectBytePair，类似std pair类，第一个信息是回收垃圾对象的个数，第二个信息是回收的内存的字节数
            GetLiveBitmap、GetMarkBitmap
            如果live_bitmap和mark_bitmap是同一个对象，则不需要处理，直接返回 0，0
            std::swap，交换live_bitmap和mark_bitmap的值
            ContinuousSpaceBitmap::SweepWalk，扫描从Begin开始到End结束的这段内存空间，live_bitmap代表集合Live，mark_bitmap代表集合Mark
            GetSweepCallback，由子类实现，返回一个处理垃圾对象回调函数，SweepWalk每找到一个垃圾对象都会调用这个回调函数进行处理
        GetSweepCallback是ContinuousMemMapAllocSpace中定义的虚函数，MallocSpace重载了该函数
        MallocSpace::SweepCallback：
            num_ptrs代表垃圾对象的个数，**ptrs代表一个垃圾对象数组的起始地址
            GetLiveBitmap，bitmap->Clear，垃圾对象，需要将其从live_bitmap_中去除
            space->FreeList，用于释放一组对象的内存
        RosAllocSpace和DIMallocSpace均实现了FreeList函数，其内部调用msalloc_bulk_free和RosAlloc的BulkFree函数来释放垃圾对象所占据的内存
        Sweep函数总结：
            空间对象的live_bitmap_代表集合Live，它的部分内容由Heap live_stack_提供，因为live_stack_只是保存了两次GC间所创建的对象（一部分为两次GC间新创建的对象，由live_stack_提供，另外一部分为上次GC后剩下的对象）
            空间对象的mark_bitmap_代表集合Mark，是这次GC在标记相关的任务中所能追踪到的对象
            Sweep检查空间的live_bitmap_和mark_bitmap_，live_bitmap_存在而mark_bitmap_不存在的对象被认为是垃圾对象，从而被清除，释放对象占据的内容，
            也就是调用RosAllocSpace和DIMallocSpace的FreeList函数，BumpPointerSpace和RegionSpace无法释放单个内存对象的空间，它们不能用于MarkSweep GC
            最后，更新集合Live，将垃圾对象对应的位清零
        GarbageCollector::SwapBitmaps：
            Sweep返回后，本次GC找到的垃圾对象就算回收完毕，现在对空间的live_bitmap_和mark_bitmap_做处理
            GetGcType，
            heap->GetContinuousSpaces，返回Space集合
            遍历Heap continuous_spaces_成员
                判断gc_retention_policy类型，ZygoteSpace的值为kGcRetentionPolicyFullCollect，即只在MarkSweep时才处理ZygoteSpace空间，PartialMarkSweep以及StickyMarkSweep均不需要处理它
                ReplaceBitmap，更新Heap live_bitmap_和mark_bitmap_数组中的元素，第一个参数为旧值，第二个参数为新值，内部先找到旧值所在的数组索引，然后将新值存储到该索引位置上，ReplaceBitmap就是交换Heap live_bitmap_和mark_bitmap_对应元素的信息
                ContinuousMemMapAllocSpace->SwapBitmaps，交换space中live_bitmap_和mark_bitmap_
        SwapBitmaps就是交换集合Live和集合Mark在相关数据结构中对应的成员变量
            集合Live包含此次GC中搜索到的对象，构成了集合Live第二部分的内容--即上一次GC后剩下的对象，本次GC的剩余对象将作为下一次GC中集合Live的内容
            集合Mark包含的信息是原集合Live去掉本次GC中的垃圾对象后的结果
            
    MarkSweep::FinishPhase：
        mark_stack_->Reset，清空MarkSweep mark_stack_的内容
        heap_->ClearMarkedObjects，清空空间对象mark_bitmap_。即GC结束后，集合Mark将被清空
    
    PartialMarkSweep：
        GetGcType，返回kGcTypePartial，即不扫描APP进程从zygote进程继承得来的空间对象--也就是ZygoteSpace空间
        BindBitmaps：
            MarkSweep::BindBitmaps，调用父类的BindBitmaps，ImageSpace将加入immune_spaces_
            heap->GetContinuousSpaces，返回Space集合
            遍历Heap continuous_spaces_成员
                if kGCRetentionPolicyFullCollect，只有ZygoteSpace空间的回收策略符合
                    immune_spaces_.AddSpace，    所以在此就是将ZygoteSpace加入immune_spaces_
        MarkSweep的GC中，位于immune_spaces_中的对象将不会被追踪，除非某些对象的引用型成员变量指向位于其他空间中的对象
        PartialMarkSweep之所以比MarkSweep运行速度快，根本原因在于要处理的空间对象较少，也就导致要处理的对象个数较少
    
    StickyMarkSweep：
        派生自PartialMarkSweep
        用于扫描和处理从上次GC到本次GC这段时间内所创建的对象
        Heap的allocation_stack_和live_stack_是MarkSweep GC代码中可记录两次GC之间所创建的对象
        GetGcType，返回kGcTypeSticky
        BindBitmaps，
        MarkReachableObjects，
        Sweep
        
        StickyMarkSweep::BindBitmaps：
            StickyMarkSweep不处理ImageSpace和ZygoteSpace
            PartialMarkSweep::BindBitmaps，
            heap->GetContinuousSpaces，返回Space集合
            遍历Heap continuous_spaces_成员
            if IsContinuousMemMapAllocSpace && kGcRetentionPolicyAlwaysCollect，
                ContinuousMemMapAllocSpace->BindLiveToMarkBitmap，
            后续是处理DiscontinuousSpace的情况
        ContinuousMemMapAllocSpace::BindLiveToMarkBitmap：
            GetLiveBitmap，
            if live_bitmap ！= mark_bitmap_，
                MarkBitmap->ReplaceBitmap，更新Heap mark_bitmap_中原mark_bitmap_所在的元素，更新前该元素旧值为mark_bitmap，更新后新值为live_bitmap
            temp_bitmap_、mark_bitmap_，mark_bitmap的值保存到temp_bitmap_中，原live_bitmap的信息保存到mark_bitmap_中
        对MarkSweep而言，immune_spaces_包含ImageSpace
        对PartialMarkSweep而言，immune_spaces_包含ImageSpace和ZygoteSpace
        对StickyMarkSweep而言，immune_spaces_包含ImageSpace、ZygoteSpace和其他符合要求的空间对象
        
        MarkSweep::MarkingPhase：
            BindBitmaps，调用StickyMarkSweep的BindBitmaps
            heap_->ProcessCards，对StickyMarkSweep而言，ProcessCards最后一个参数为false，除关联ModUnionTable的空间对象之外，其余空间对象对应的card的新值将变为kCardDirty-1或0
            MarkRoots，
            MarkReachableObjects，调用StickyMarkSweep的MarkReachableObjects
        StickyMarkSweep::MarkReachableObjects：
            mark_stack_->Reset，mark_stack_被清空，表示在MarkRoots中做过标记的对象不再需要，但集合Mark的信息却留了下来
            RecursiveMarkDirtyObjects，最后一个参数取值为kCardDirty-1
        MarkSweep::RecursiveMarkDirtyObjects：
            ScanGrayObjects，遍历Heap continuous_spaces_中的空间对象，每找到一个mark_bitmap_不为空的空间对象，就调用Heap card_table_的Scan函数，找到card值大于或等于minimum_age的card，然后根据这个card再到空间对象去找到对应的mirror Object对象（是被标记过的），每找到这样的Object对象就调用MarkSweep的ScanObject以标记它的引用型成员变量
            ScanGrayObjects就是确定被标记过的对象中有哪些对象的引用型成员变量被修改过
            ProcessMarkStack，
        RecursiveMarkDirtyObjects决定了集合Mark的内容
        
        StickyMarkSweep::Sweep：
            SweepArray，第一个参数为Heap live_stack_，包含两次GC间所创建的对象，就是StickyMarkSweep中的集合Live
        集合Live，来自Heap live_stack_，保存两次GC间所创建的对象
        集合Mark，上次GC所留存的对象均为标记过的对象，它们当中凡是修改了引用型成员变量的对象都会被遍历，并且要标记
        MarkSweep::SweepArray：
            sweep_array_free_buffer_mem_map_，是MarkSweep的成员变量，可看作一块内存
            chunk_free_buffer，数组，元素为Object*，由上面变量转化而成的数组
            allocations，输入参数，指向Heap live_stack_
            sweep_spaces，数组，用于保存此次GC所要扫描的空间对象
            遍历heap的ContinuousSpace，将non_moving_space_加到上面数组的最后
            遍历sweep_spaces
                alloc_space，后续需要调用AllocSpace的FreeList函数释放内存
                objects，heap live_stack_容器的起始元素，count为容器的元素个数
                遍历objects数组
                    kUseThreadLocalAllocationStack为编译常量，默认为true
                    HasAddress，判断space是否包含obj
                        空间对象的mark_bitmap没有设置obj，所以obj是垃圾对象
                        kSweepArrayChunkFreeSize值为1024，初值为0，先把找到的垃圾对象存起来，攒到一定数量后再一起清理
                        alloc_space->FreeList，释放一组垃圾对象
                        obj不是垃圾对象时，Assign，把obj存到Heap live_stack_新的位置上
                对DiscontinuousSpace的处理
                allocations->Reset，清空Heap live_stack_的内容
        
        如果空间对象调用过BindLiveToMarkBitmap，在MarkSweep的ReclaimPhase的最后，Heap UnBindBitmaps将被调用以复原live_bitmap_以及mark_bitmap_的关系
        Heap::UnBindBitmaps：
            遍历ContinuousSpaces：
                if IsContinuousMemMapAllocSpace，
                    if alloc_space->HasBoundBitmaps，temp_bitmap_不为空，说明之前曾经调用过BindLiveToMarkBitmap
                        alloc_space->UNBindBitmaps
        ContinuousMemMapAllocSpace::UnBindBitmaps：
            temp_bitmap_.release，temp_bitmap_保存了原mark_bitmap_的内容，而mark_bitmap_保存了原live_bitmap_的内容
            heap->GetMarkBitmap->ReplaceBitmap，恢复Heap mark_bitmap_对应索引的内容
            mark_bitmap_.reset，恢复mark_bitmap_的内容
        
    MarkSweep：
        集合Live构成，上一轮GC后剩余的对象（上一轮GC的集合Mark构成）和两次GC间新创建的对象（由Heap allocation_stack_提供）
        集合Mark构成，immune_spaces_中的对象（只有ImageSpace）、Runtime VisitRoots访问到的根对象、从前两者出发能访问到的对象
    PartialMarkSweep：
        集合Live构成，上一轮GC后剩余的对象和两次GC间新创建的对象
        集合Mark构成，immune_spaces_中的对象（ImageSpace、ZygoteSpace）、Runtime VisitRoots访问到的根对象、从前两者出发能访问到的对象
    StickyMarkSweep：
        集合Live构成，两次GC间新创建的对象（由Heap allocation_stack_提供）
        集合Mark构成，immune_spaces_中的对象（ImageSpace、ZygoteSpace、其他AllocSpace对象）、Runtime VisitRoots访问到的根对象、从前两者出发能访问到的对象
    
    Concurrent MarkSweep：
        Concurrent GC是GC的一种改进实现方式
        Mark-Sweep、Mark-Compact、Copying Collection这三种基础GC算法均有对应的Concurrent实现形式
        MarkSweep GC中：
            MarkingPhase：完成对象搜索和标记的工作，对MS而言，它必须在mutator暂停的时候才能执行，在CMS下，允许和mutator同时运行
            PausePhase：不论CMS和MS，都必须在mutator暂停的情况下运行
            ReclaimPhase：释放垃圾对象占据的内存，无论CMS和MS，都可以和mutator同时执行，因为mutator无法使用垃圾对象
        ART CMS和MS最大的差别在于MarkingPhase--即在对象追踪和标记的处理上
            CMS先有一个初始标记工作Initial Mark，用于搜索根对象，搜索某些类型的根对象时必须要暂停mutator，比如访问位于各个Java线程调用栈里的根对象时，mutator是必须要暂停的
            所以，CMS Initial Mark过程中存在一个pause阶段，CMS Initial Mark只在访问线程相关的根对象（线程根对象）时才会暂停mutator
            线程根对象被访问及标记完后，mutator恢复运行，同时collector从Initial Mark中得到根对象出发访问所能追踪到的对象
            mutator和collector同时运行的情况下，需要记住mutator所做的修改，否则很容易造成一个非垃圾对象出现未标记的情况
            CMS处理完Initial Mark后，将再次暂停mutator，这次暂停叫Remark pause，collector需要再次搜索和标记对象--Remark
            Remark存在的原因是mutator在和collector同时运行的时候很可能对内存做了修改，Remark不会再把所有类型的根对象都访问和追踪一遍
            Runtime VisitRoots有一个参数 VisitRootFlags，可以控制所要访问的根对象的范围
            Remark结束后，CMS所有的标记工作都完成，此后就可以释放垃圾对象了
        MarkSweep::MarkingPhase：
            MarkRoots，CMS和MS的情况不同
            MarkReachableObjects，
            PreCleanCards，只在CMS的情况下有作用
        CMS下，collector执行MarkingPhase时，mutator仍然在执行，和MS相比较，只有MarkRoots和PreCleanCards
        MarkSweep::MarkRoots：
            if IsExclusiveHeld，对应MS的情况，此时mutator被暂停，此时可以做一个比较完整的标记
                runtime->VisitRoots，
            else， 对应CMS的情况，可看作是Initial Mark
                MarkRootsCheckpoint，暂停mutator，其内部会调用Thread VisitRoots函数访问线程根对象，这段时间就是Initial Mark Pause，第二个参数取值为true，表示在线程恢复运行前会Revoke它们的TLAB
                MarkNonThreadRoots，不支持VisitFlags参数控制
                MarkConcurrentRoots，第二个参数表示访问所有能访问的根对象，和通知相关模块开启记录功能，记住后续新增加的对象
        MarkSweep::PreCleanCards：
            kPreCleanCard，编译常量，为true，可以和mutator同时运行
            heap_->ProcessCards，再次调用ProcessCards，最后一个参数值为false，调用结果，如果空间对象关联ModUnionTable对象，则调用ModUnionTable ClearCards，如果旧值为kCardDirty，则新值为kCardDirty-1，否则新值为0，对没有关联ModUnionTable的空间对象，直接更新它们的card，如果旧值为kCardDirty，则新值为kCardDirty-1，否则新值为0
            MarkRootsCheckpoints，再次访问线程根对象，会暂停mutator
            MarkNonThreadRoots，标记根对象
            MarkConcurrentRoots，标记根对象，kVisitRootFlagNewRoots表示只访问上次启用模块记录功能到此次访问这段时间内新创建的对象，kVisitRootFlagClearRootLog，表示这些新对象访问完后，情况保存它们的容器
            RecursiveMarkDirtyObjects，先跟踪card table的情况访问card值大于或等于kCardDirty-1的对象，上次GC结束后到目前为止引用型成员变量发生变化的对象，由于第一个参数为false，这些card访问完后，card的值不会被清零；从这些对象出发进行追踪（处理mark_stack_的内容，直到其中元素全部处理完）
        PreCleanCards，可以减少后续Pause的时间，因为PreCleanCards可以和mutator同时运行，所以它能完成一些标记工作的话自然能减少后续Remark pause的时间
        
        MarkSweep::PausePhase：
            if IsConcurrent，
                ReMarkRoots，内部调用Runtime VisitRoots函数，访问标志位变化了
                RecursiveMarkDirtyObjects，访问kCardDirty card对应的对象，然后设置值为0，最后再处理mark_stack_中的元素，到此CMS的标记工作全部完成
    
    Parallel GC：
        MarkSweep中，Parallel GC主要体现在标记上，即把标记工作交给多个线程来处理
        MarkSweep::ProcessMarkStack：
            GetThreadCount，
            kMinimumParallelMarkStackSize，取值为128，即mark_stack_中至少要有128个对象才会启用多线程，另外是否开启多线程还和进程的状态有关，如果进程处于用于和感知的状态，GetThreadCount为1，不允许使用多线程
            ProcessMarkStackParallel
        MarkSweep::ProcessMarkStackParallel：
            根据要标记对象的个数以及线程池中线程的个数进行划分，每个线程最多能处理mark_stack_中1KB（kMaxSize）大小的范围，32位平台上，1KB能存储1KB/4=256个对象
            对mark_stack_进行划分
            AddTask，每个MarkStackTask对象将标记自己所负责的那部分空间
            MarkStackTask，
        MarkSweep中还有一处使用多线程标记的函数是ScanGrayObjects
    
    小结：
        MarkSweep使用了增量式回收的方式
        不同的mirror Object对象放在不同的空间中，回收的时候可以只针对性地处理一部分空间，.art文件为ImageSpace空间，Zygote进程运行时创建的对象在ZygoteSpace中，其他APP进程使用别的空间进行内存分配（如 main rosalloc space）
        只处理两次GC间新创建的对象，由两次GC间新创建的对象由Heap allocation_stack_容器提供
        确认此次回收的两个集合--Live和Mark：
            Mark，使用mark_bitmap_表示，GC标记阶段填写
            Live，使用live_bitmap_表示，包括留存对象和两次GC间新创建的对象，留存对象由上一次回收完毕后的mark_bitmap_转换而来
            如果不想扫描某个空间对象，则需要提前设置它的集合Mark为集合Live
            回收垃圾前记得把新创建的对象添加到集合Live中
            垃圾回收后记得把集合Live换成Mark
        Heap card_table_的使用：
            card_table_记录了那些引用型成员变量被修改的对象
            在MarkSweep中，老年代和年轻代其实只是不同的空间对象
            CardTable中card的值有0、kCardDirty和kCardDirty-1三种，Write Barrier将修改card值为kCardDirty，GC将修改card值为0或kCardDirty-1，修改card值的函数对象类叫AgeCardVisitor
            ImageSpace和ZygoteSpace关联ModUnionTable，DIMallocSpace和RosAllocSpace关联RememberedSet，都是辅助性数据结构用于管理和操作CardTable
            MarkSweep没有使用RememberedSet来操作DIMallocSpace和RosAllocSpace
            CardTable主要为解决对象的跨空间引用问题
            ModUnionTable和RememberedSet是不同空间用来管理各自空间中跨空间引用对象的辅助用数据结构
            MarkSweep支持Concurrent Collector和parallel collector
                CMS的调用流程和MS区别不大
                parallel collection就是用多个线程来做标记工作

#### Concurrent Copying
    
    ConcurrentCopying类：
        Copying Collection的实现类
        GetGcType，返回kGcTypePartial，不扫描ImageSpace和ZygoteSpace（除了有dirty card的对象）
        GetCollectorType，返回kCollectorTypeCC，回收器类型
        垃圾回收器决定适合的内存分配器，kCollectorTypeCC对应的是RegionSpace
        ConcurrentCopying要回收的垃圾对象就在region_space_中
        ConcurrentCopying::RunPhases：
            InitializePhase，初始化阶段
            FlipThreadRoots，完成半空间Flip工作
            MarkingPhase，标记
            ReclaimPhase，回收
            FinishPhase，收尾工作
            
    ConcurrentCopying::InitializePhase：
        CheckEmptyMarkStack，
        immune_spaces_.Reset，保存了不需要GC的空间对象
        force_evacuate_all_，和RegionSpace有关
        BindBitmaps，
    ConcurrentCopying::CheckEmptyMarkStack：
        thread_local_mark_stack，是专门配合ConcurrentCopying而使用的，AtomicStack类型
        MarkStackMode，枚举变量，有4个值，kMarkStackModeOff、kMarkStackModeThreadLocal、kMarkStackModeShared、kMarkStackModeGcExclusive
        MarkStackMode初始值为kMarkStackModeOff，GC过程中将修改它
        if kMarkStackModeThreadLocal：
            RevokeThreadLocalMarkStacks，要求各个Java Thread执行一个CheckPoint任务，该任务，获取线程的tlsPtr_ thread_local_mark_stack对象，若不为空，则将对象保存到ConcurrentCopying revoked_mark_stacks_成员变量中，然后调用Thread SetThreadLocalMarkStack，将thread_local_mark_stack设置为空
            revoked_mark_stacks_，不为空，则需要逐个清除其中所包含的Object对象，mark_stack->PopBack，
        else，其他值
            gc_mark_stack_指向一个ObjectStack对象
            检查置空
    ConcurrentCopying::BindBitmaps：
        遍历ContinuousSpaces
            immune_spaces_.AddSpace，ConcurrentCopying只支持kGcTypePartial，ImageSpace、ZygoteSpace会被加到immune_spaces_中
        cc_heap_bitmap_，类型为HeapBitmap
        cc_bitmaps_，类型为Vector
        region_space_bitmap_
    BindBitmaps：
        对ImageSpace或ZygoteSpace空间对象，将它们加到immune_spaces_中
        针对ImageSpace、ZygoteSpace和RegionSpace各创建三个SpaceBitmap对象，RegionSpace没有live_bitmap_和mark_bitmap_位图对象
        设置ConcurrentCopying相关成员变量，cc_heap_bitmap_、cc_bitmap_、region_space_bitmap_等
    
    FlipThreadRoots：
        用于转换线程的内存分配空间，使之从from space转到to space
    Thread::TransitionFromSuspendedToRunnable：
        线程对象从暂停状态恢复运行前将执行flip_func
        Closure* flip_func=GetFlipFunction，返回线程对象tlsPtr_ flip_function成员变量
        flip_func->Run
    ConcurrentCopying::FlipThreadRoots：
        ThreadFlipVisitor，
        FlipCallback，
        Runtime FlipThreadRoots，暂停线程对象，然后设置它们的flip_function，接着再恢复它们的运行，前两个参数是闭包对象，GC线程（当前调用该函数的线程）先执行flip_callback，其他所有Java线程对象再执行thread_flip_visitor
    FlipCallback：
        只执行一次
        Run：
            SetFromSpace，调用RegionSpace的SetFromSpace函数，rb_table_为ReadBarrierTable，来自Heap，ReadBarrierTable的启用需要设置前台回收器类型为kCollectorTypeCC
            SwapStacks，内部调用Heap SwapStacks，交换Heap allocation_stack_和live_stack_
            mark_stack_mode_，设置该值为kMarkStackModeThreadLocal
    RegionSpace::SetFromSpace：
        RegionSpace把内存资源划分成数个块，每个块由一个Region对象描述，region_数组保存了各个内存块对应的Region信息
        RegionState，kRegionStateFree，表示Region为空闲待使用状态，kRegionStateAllocated，表示Region已经有一部分内存被分配
        RegionType，kRegionTypeAll，kRegionTypeFromSpace，位于From Space中，需要被清理，kRegionTypeUnevacFromSpace，位于From Space，但不需要被清除，kRegionTypeToSpace，Region位于To Space中，kRegionTypeNone，默认类型
        如果一个Region首先被使用，其类型从kRegionTypeNone转换为kRegionTypeToSpace，见Unfree函数
        if ！IsFree，false，该Region已经被使用
            ShouldBeEvacuated，用于判断一个Region是否需要被清除
            SetAsFromSpace，设置Region类型为kRegionTypeFromSpace
            SetAsUnevacFromSpace，设置Region类型为kRegionTypeUnevacFromSpace
    Region ShouldBeEvacuated：
        如果一个Region在上次GC中被释放（Region Clear被调用，Regionis_newly_allocated_为false），且在本次GC前又被使用（Regionis_newly_allocated_为true），则本次GC需要清除它
        如果一个Region中存活对象的字节数/总的内存分配字节数之比小于75%，则本次GC将清除它
    ThreadFlipVisitor：
        Run函数：
            thread->SetIsGcMarking，设置线程对象tls32_is_gc_marking为true
            region_space_->RevokeThreadLocalBuffers，撤销RegionSpace为线程thread分配的TLAB
            thread->RevokeThreadLocalAllocationStack，撤销线程本地Allocation Stack
            thread->VisitRoots，访问线程根对象，ConcurrentCopying的VisitRoots函数将被调用，其内部调用MarkRoot
    ConcurrentCopying::MarkRoot：
        对线程根对象进行标记
        ref，当前正在被访问的某个线程根对象
        Mark，返回一个to_ref对象，内容和ref一样，但他可能位于其他空间中（拷贝），如果to_ref和ref不相同，则需要修改存储ref的内存，使得它指向新的to_ref
        CompareExchangeWeakRelaxed
    ConcurrentCopying::Mark：
        实现Copying Collection的关键函数
        GetRegionType，获取from_ref所在Region的类型，若from_ref不是region_space_的对象，则返回kRegionTypeNone
        kRegionTypeToSpace，已经在To Space中，直接返回，不需要后续拷贝
        kRegionTypeFromSpace，
            GetFwdPtr，找到from_ref的拷贝对象
            Copy，如果from_ref不存在对应的拷贝对象，则调用Copy生成一个拷贝对象
            return
        kRegionTypeUnevacFromSpace，
            AtomicTestAndSet，不需要清理的Region，对该对象进行标记
            PushOntoMarkStack，如果from_ref是初次标记，则调用改函数
        kRegionTypeNone，
            return MarkNonMoving，说明from_ref不是region_space_中的对象，调用该函数
    ConcurrentCopying::GetFwdPtr：
        GetLockWord，Object monitor_实际指向LockWord对象，该对象包含一个Object的地址信息
        kForwardingAddress，说明lw包含一个mirror Object对象的地址信息，对Copying Collection而言，这个地址就是from_ref对应的拷贝对象的地址
        ForwardingAddress，返回值转换为fwd_ptr，就是from_ref的拷贝对象
    GetFwdPtr将获取from_ref对应的拷贝对象，ART中，这个拷贝对象内存地址存储在from_ref monitor_对应的LockWord中
    GetFwdPtr返回空，说明from_ref还没有被拷贝过，也不存在拷贝对象，这时就需要调用Copy对from_ref进行拷贝
    ConcurrentCopying::Copy：
        SizeOf，获取from_ref的内存大小
        RoundUp，按Region的要求对齐
        region_space_->AllocNonvirtual，从region_space_中分配一块内存用来存储from_ref的内容，这块内存的起始地址为to_ref
        region_space_分配失败的处理略过
        while true，
            memcpy，拷贝，将from_ref的信息拷贝到to_ref
            GetLockWord，获取to_ref对应的LockWord，把to_ref的地址值设置到from_ref monitor_中
            FromForwardingAddress，构造新的LockWord对象
            CasLockWordWeakSequentiallyConsistent，原子操作，设置到from_ref里
            PushOntoMarkStack，保存to_ref
    ConcurrentCopying::PushOntoMarkStack：
        用于将需要后续继续处理的对象保存起来
        可以由所有Java线程对象调用，运行在不同的Java线程里，是因为MarkRoot函数在ThreadFlipVisitor Run中由各个Java线程来调用
        mark_stack_mode，
        kMarkStackModeThreadLocal，在FlipCallback中，mark_stack_mode已经设置为kMarkStackModeThreadLocal了
            if thread_running_gc_，
                PushBack，调用者是GC线程自己，则把to_ref加到gc_mark_stack_中
            else，非GC线程调用该函数，则需要使用线程对象tlsPtr_ thread_local_mark_stack
                GetThreadLocalMarkStack，
                如果线程对象还没有thread_local_mark_stack容器或它已经满了，将从pooled_mark_stacks_容器中取一个空闲的容器给线程
                pooled_mark_stacks_.back，
                new_tl_mark_stack=Create，如果pooled_mark_stacks_被用完，则新建一个ObjectStack
                new_tl_mark_stack->PushBack，
                SetThreadLocalMarkStack
            mark_stack_mode取值为非kMarkStackModeThreadLocal的处理，也是将对象存储到ConcurrentCopying gc_mark_stack_中
    FlipThreadRoots：
        目标是遍历各个线程的线程根对象，如果某个线程根对象位于ToSpace中则无须拷贝和替换，如果位于FromSpace中则进行拷贝和替换
        由于拷贝时未处理引用关系，所有这些被拷贝的对象将存起来供后续处理
        ConcurrentCopying并未简单地将空间分为两个半空间，而是把处理单位缩小到RegionSpace中的Region
        Region除了ToSpace和FromSpace外，还有UnEvacFromSpace类型，表示某个Region属于FromSpace，但却不需要释放
        GC结束后，UnEvacFromSpace将更新为ToSpace
    
    ConcurrentCopying::MarkingPhase：
        MarkingPhase调用前只对线程根对象进行了Mark
        遍历ContinuousSpaces
            扫描ImageSpace中的根对象，
            image_root，
            Mark，ImageSpace中的根对象不会被拷贝。所以marked_image_root等于image_root
            VisitConcurrentRoots，访问其他类型的根对象
            VisitNonThreadRoots，访问其他类型的根对象
            遍历immune_spaces_
                ConcurrentCopyingImmuneSpaceObjVisitor，内部将在cc_heap_bitmap_中对扫描到的对象进行标记。同时调用PushOntoMarkStack
                live_bitmap->VisitMarkedRange，
            至此，所有根对象都进行了标记，并且，根对象如果发生拷贝，原始根对象将被替换为新的拷贝对象，接下来，遍历这些根对象，将它们引用的对象进行Mark，同时更新引用值
            ProcessMarkStack，
            SwitchToSharedMarkStackMode，切换mark_stack_mode为kMarkStackModeShared
            ProcessMarkStack，
            SwitchToGcExclusiveMarkStackMode，切换mark_stack_mode为kMarkStackModeGcExclusive
            ProcessReferences，对Java Reference对象的处理，各种回收器的处理都一样
            ProcessMarkStack，
    ProcessMarkStack内部将遍历通过PushOntoMarkStack保存下来的对象（都是拷贝后得到的对象，to_ref），其中最关键函数是Scan
    ConcurrentCopying::Scan：
        遍历to_ref的引用型成员变量，内部调用ConcurrentCopying的Process函数进行处理
        ConcurrentCopyingRefFieldsVisitor，
        to_ref->VisitReferences
    ConcurrentCopying::Process：
        obj->GetFieldObject，obj是to_ref，offset是obj的某个引用型成员变量
        Mark，对ref进行Mark，得到ref的to_ref，如果两个一样，则不需要更新obj offset内容
        更新obj offset的内容，使得它指向to_ref，使用原子操作，下面使用循环
    
    ConcurrentCopying::ReclaimPhase：
        ComputeUnevacFromSpaceLiveRatio，
        region_space_->ClearFromSpace，
        Sweep，清空除immune_spaces_、region_space_外的空间中其他的垃圾对象，和MarkSweep Sweep类似，内部调用ContinuousMemMapAllocSpace的Sweep函数进行处理
        SwapBitmaps，调用GarbageCollector的SwapBitmaps函数，和MarkSweep的处理一样
        heap_->UnBindBitmaps
    ConcurrentCopying::ComputeUnEvacFromSpaceLiveRatio：
        ConcurrentCopyingComputeUnEvacFromSpaceLiveRatioVisitor，对RegionSpace中的标记对象进行统计
        region_space_bitmap_->VisitMarkedRange
    如果一个Region中存活对象所占内存字节数/总内存分配数的比例小于75%，则该Region将被设置为kRegionTypeFromSpace类型以表示本次GC需要回收它
    每次GC后，ConcurrentCopying需要更新一个Region的存活对象内存字节数，这是由ComputeUnEvacFromSpaceLiveRatio完成的
    ConcurrentCopyingComputeUnEvacFromSpaceLiveRatioVisitor：
        operator：
            ref，是一个本次GC中被标记的对象
            RoundUp，对齐
            region_space_->AddLiveBytes，更新ref所在Region的live_bytes_
    RegionSpace::ClearFromSpace：
        清空RegionSpace的kRegionTypeFromSpace类型的Region
        if IsInFromSpace，
            Clear，清空
        if IsInUnEvacFromSpace，
            SetUnEvacFromSpaceAsToSpace，UnEvacFromSpace类型的region将设置其类型为kRegionTypeToSpace
    
    ConcurrentCopying实现了Copying Collection原理，并且支持Concurrent回收：
        ConcurrentCopying是以RegionSpace中的Region为单位进行处理，同时根据存活对象所占内存的比例来灵活设置Region被划归为From Space的条件
        Object通过monitor_及LockWord来保存所谓的forwarding address信息
        FlipThreadRoots中，线程根对象在被拷贝后，ConcurrentCopying还会更新栈中的数据，线程根对象的地址保存在调用栈中某个位置，例如stack[a]，如果根对象被拷贝，那么stack[a]的内容应该更新为拷贝后得到的根对象的地址
        ConcurrentCopying中有很多Read Barrier相关的代码

#### MarkCompact
    
    最简单的回收器
    GetGcType，返回kGcTypePartial，不处理ImageSpace和ZygoteSpace
    GetCollectorType，返回kCollectorTypeMC，对应的内存分配器为BumpPointerSpace
    MarkCompact要回收的垃圾对象就在space_指向的BumpPointerSpace中
    MarkCompact::RunPhases：
        mark_stack_、mark_bitmap_
        InitializePhase，
        ScopedPause，MarkCompact是stop-the-world类型的回收器
        MarkingPhase，标记阶段
        ReclaimPhase，回收阶段
        FinishPhase，收尾工作
    
    MarkCompact::MarkingPhase：
        objects_before_forwarding_，位图对象，BumpPointerSpace不包含位图对象，而MarkCompact基于Mark-Compact，需要标记能被搜索到的对象，所以为space_创建一个位图对象，用于记录搜索到的对象
        objects_with_lockword_，位图对象，和GC无关，用于保存一些信息
        BindBitmaps，将ImageSpace或ZygoteSpace加到MarkCompact immune_spaces_容器中
        ProcessCards，用于处理CardTable中对应的card，MarkCompact通过移动对象来实现内存回收，但移动内存的过程是在最后的回收阶段，此时，所有非垃圾对象都已经标记，所以MarkCompact可以使用WriteBarrier来记录跨空间的对象引用，ConcurrentCopying在标记阶段可能就会移动对象，这时不方便使用WriteBarrier，只能使用ReadBarrier
        ClearCardTable，用于处理CardTable中对应的card
        RevokeAllThreadLocalAllocationStacks，
        SwapStacks，
        MarkRoots，搜索并标记根对象
        UpdateAndMarkModUnion，借助CardTable来处理ImageSpace或ZygoteSpace中存在跨空间引用的对象，每找到一个对象就做标记并压入mark_stack_中
        MarkReachableObjects，从根对象出发，扫描它们所引用的对象
        Java Reference对象的处理等
    MarkCompact::MarkRoots，内部最后调用Runtime VisitRoots
    MarkCompact实现了RootVisitor接口的VisitRoots函数，其内部就是调用MarkCompact MarkObject函数
    MarkCompact::MarkObject：
        if ！IsInImmuneRegion，obj不在immune_spaces_中
          if objects_before_forwarding_->HasAddress，obj位于space_中，则到objects_before_forwarding_里标记它
            if ！objects_before_forwarding_->Set，
                MarkStackPush，  obj是第一次被标记，将其加入mark_stack_容器中
          else，obj位于immune_spaces_和space_之外的空间中，调用mark_bitmap_进行标记
            mark_bitmap_->Set，MarkStackPush
    MarkRoots对所有根对象进行标记并且保存到mark_stack_容器中
    MarkCompact::MarkReachableObjects：
        GetLiveStack，对Heap allocation_stack_中的对象标记为集合Live
        MarkAllocStackAsLive，
        ProcessMarkStack，对mark_stack_中的元素进行处理，内部对mark_stack_中的每一个元素调用ScanObject
    MarkCompact::ScanObject：
        访问obj引用的其他对象
        MarkCompactMarkObjectVisitor，内部调用MarkObject对这些被引用的对象进行标记
        obj->VisitReferences
    
    MarkCompact::ReclaimPhase：
        完成垃圾对象回收的工作
        Sweep，回收除space_、immune_spaces_外其他空间对象中的垃圾，内部调用ContinuousMemMapAllocSpace的Sweep函数进行回收，使用的是Mark-Sweep，不是Mark-Compact
        SwapBitmaps，调用GarbageCollector SwapBitmaps函数
        UnBindBitmaps，同MarkSweep中使用
        Compact，压缩，真正的MarkCompact的重点
    MarkCompact::Compact：
        CalculateObjectForwardingAddress，计算每个存活对象的forwarding address，这个地址也就是这些对象的新内存地址
        UpdateReferences，更新对象的引用关系，将所引用的对象修改为对应的forwarding address
        MoveObjects，将对象移动到它的forwarding address处
        space_->SetEnd，更新space_末尾位置，经上面压缩处理后，space_中的垃圾对象被清除，而非垃圾对象们又被移动到一起，这些非垃圾对象在space_中的末尾位置由bump_pointer_标示
        memset，清零一段内存空间，这段空间就是垃圾对象所占据的内存大小，bytes_freed
    MarkCompact::CalculateObjectForwardingAddress：
        用于计算每个存活对象的forwarding address
        bump_pointer_，初值为space_的起始位置
        CalculateObjectForwardingAddressVisitor，内部调用MarkCompact ForwardObject，对每一个非垃圾对象进行处理
        objects_before_forwarding_->VisitMarkedRange
    MarkCompact::ForwardObject：
        alloc_size，获取这个对象的所占内存的大小
        GetLockWord，
        如果这个obj之前有设置LockWord，下面将把LockWord旧值保存起来，等后续对象移动完毕后，需要恢复obj的LockWord的旧值
        objects_with_lockword_->Set，记录那个对象存在LockWord的旧值
        lock_words_to_restore_，是std queue，用于保存obj的LockWord旧值
        obj->SetLockWord，设置obj的forwarding address，为bump_pointer_
        bump_pointer_+=alloc_size，移动bump_pointer_，使得它指向下一个对象的forwarding address
    CalculateObjectForwardingAddress展示了MarkCompact中Compact的方法，就是将非垃圾对象一个一个排列起来，只有BumpPointerSpace支持这种操作
    CalculateObjectForwardingAddress计算完每个对象的新地址后，UpdateReferences也更新了对象的引用关系，接下来就是真正地把对象移动到它的新地址，由MoveObjects完成
    MarkCompact::MoveObjects：
        遍历存活对象
        MoveObjectVisitor，内部调用MoveObject函数进行处理
        objects_before_forwarding_->VisitMarkedRange
    MarkCompact::MoveObject：
        obj，输入参数
        dest_addr=obj->GetLockWord.ForwardingAddress，从LockWord中获取obj的目标地址
        dest_obj，将目标地址转换为mirror Object
        memmove，将obj移动到dest_addr处
        如果obj之前有LockWord旧值，则需要从lock_words_to_restore_中拿到旧值
        lock_words_to_restore_.front，拿到旧值
        dest_obj->SetLockWord，设置dest_obj的LockWord
    
    MarkCompact：
        Mark，和MarkSweep的Mark类似，只要对能搜索到的对象进行位图标记即可，和ConcurrentCopying Mark阶段有所不同，ConcurrentCopying Mark阶段实际上还完成了对象的拷贝
        Compact，将存活对象移动到一起，MarkCompact使用的是BumpPointerSpace，内存分配算法极为简单的空间，Compact仅仅就是把存活对象memmove到指定位置即可
        
#### SemiSpace

    GetGcType，返回kGcTypePartial，SemiSpace只支持kGcTypePartial
    GetCollectorType，generational_?kCollectorTypeGSS:kCollectorTypeSS，SemiSpace支持SS和GSS两种类型回收器，GSS为generationalSS，分代式SS回收
    Semispace综合了Semi-space（即Copying Collector）和Mark-Sweep方法，同时还支持压缩，而kCollectorTypeGSS则支持分代回收的SS方法
    SetToSpace、SetFromSpace，设置to_space_和from_space_，是ContinuousMemMapAllocSpace*类型
    from_space_是待扫描和回收处理的空间，to_space_是用于保存非垃圾对象，GC后from_space_将被清空
    SetSwapSemiSpaces，设置swap_semi_spaces_，控制GC后是否交换半空间（Copying Collection完成GC后要交换（flip）FromSpace和ToSpace）
    SemiSpace::RunPhases：
        InitializePhase，回收器初始化
        if mutator->IsExclusiveHeld，为true说明mutator线程已被暂停
            MarkingPhase，标记回收
            ReclaimPhase，回收工作
        else，mutator未暂停，则SemiSpace只有标记阶段需要暂停mutator
            ScopedPause，暂停mutator
            MarkingPhase，标记工作
            ReaderMutexLock，mutator恢复运行，可同时开展回收工作
            ReclaimPhase，
        FinishPhase，收尾工作
    
    SemiSpace::InitializePhase：
        mark_stack_，
        immune_spaces_，
        to_space_live_bitmap_，为to_space_的集合Live的代表，它存储的是上次GC后to_space_中的存活对象
        mark_bitmap_，指向Heap mark_bitmap_
        if generational_，generational_对应为GSS的情况
            promo_dest_space_=Heap->GetPrimaryFreeListSpace，
            GetPrimaryFreeListSpace返回Heap中rosalloc_space_，分代GC就是内存空间存储老年代的对象，
            GSS中存储老年代对象的内存空间就是promo_dest_space_
    
    SemiSpace::MarkingPhase：
        RevokeAllThreadLocalBuffers，撤销线程TLAB
        if generational_，分代GC
            collect_from_space_only_，和generational_相关，含义为是否只回收from_space_空间，，在GSS某些情况下，collect_from_space_only_需修改为false
        if generational_，
            bytes_promoted_，
        BindBitmaps，将ImageSpace或ZygoteSpace加入immune_spaces_中，并且将to_space_的集合Live绑定为集合Mark
        heap_->ProcessCards，SS时，第二、三个参数为false，只处理关联ModUnionTable的空间对象的card（新值为0或kCardDirty-1），GSS时，第二个参数为true，将处理关联ModUnionTable和RememberedSet的空间对象的card（新值为0或kCardDirty-1）
        ClearCardTable，Heap card_table_中所有card被清零，而之前的dirty card在上面ProcessCards处理中已保存在空间对象所关联的ModUnionTable或RememberedSet中
        heap_->RevokeAllThreadLocalAllocationStacks，
        heap_->SwapStacks，交换Heap allocation_stack_和live_stack_
        MarkRoots，调用Runtime VisitRoots，SemiSpace实现了RootVisitor VisitRoots接口函数，其内部将调用SemiSpace的MarkObjectIfNotInToSpace
        MarkReachableObjects，先处理immune_spaces_中存在跨空间引用的对象，然后调用ProcessMarkStack处理mark_stack_中的元素
        ProcessReferences，和Java Reference对象处理有关
        SweepSystemWeaks，调用Runtime SweepSystemWeaks
        RevokeAllThreadLocalBuffers，
        from_space_->Clear，清空from_space_，在MarkingPhase阶段，from_space_就已经被清空了
        if swap_semi_spaces_，
            heap_->SwapSemiSpaces，交换to_space_和from_space_，其内部将交换Heap bump_pointer_space_和temp_space_这两个成员变量，它们各指向一个大小相同的BumpPointerSpace空间
    SemiSpace::MarkObjectIfNotInToSpace：
        SemiSpace每次遍历一个对象都会调用MarkObjectIfNotInToSpace函数
        if ！to_space_->HasAddress，to_space_不包含这个对象
            MarkObject，标记该对象
    SemiSpace::MarkObject：
        obj_ptr是指向obj对象的指针
        if from_space_->HasAddress，obj位于from_space_中，在另外一个空间中创建obj的拷贝对象
            forward_address=GetForwardingAddressInFromSpace，读取obj的LockWord对象，获取其中的forwarding address
            如果obj还没有对应的拷贝对象，则调用MarkNonForwardedObject进行处理
            MarkNonForwardedObject，返回值就是obj的对应的拷贝对象的地址
            obj->SetLockWord，更新obj的LockWord对象
            MarkStackPush，将obj的拷贝对象保存到mark_stack_中以备后续处理
            obj_ptr->Assign，更新obj_ptr，将导致原来存储obj对象的地方抛弃obj，转而存储obj的拷贝对象，即以前指向obj对象的地方将指向obj的拷贝对象
        else if，collect_from_space_only_为false，并且obj不在immune_spaces_中，则对obj所在的位图进行标记处理
            BitmapSetSlowPathVisitor，
            mark_bitmap_->Set，
                MarkStackPush，
    MarkObject中：
        如果obj属于from_space_，则需要做一些工作
        如果obj不属于from_space_并且也不是immune_spaces_中的对象，obj是否被看作垃圾对象的关键在于变量collect_from_space_only_的值
    SemiSpace对空间的处理：
        肯定不会回收immune_spaces_中的空间
        to_space_也不会被回收，而from_space_会被整体清空
        collect_from_space_only_决定是否扫描和处理其他的空间对象
    SemiSpace::MarkNonForwardedObject：
        if generational_ && ...，GSS的处理
            promo_dest_space_->AllocThreadUnsafe，GSS在promo_dest_space_空间中创建一个拷贝对象
            promo_dest_space_就是GSS中的老年代空间，而将一个对象从年轻代空间提升到老年代空间的做法就是在老年代空间中创建一个年轻对象比如obj的拷贝对象
            此时还只是在老年代空间中分配了一个和obj同样大小的内存空间，而obj的内容还没有拷贝进去
            if，promo_dest_space_空间不足的处理
            else，
                Heap->WriteBarrierEveryFieldOf，设置forward_address对应的card为dirty card，因为forward_address是obj对象在老年代空间里的拷贝，大概率情况下obj会有指向其他对象的引用型成员变量，所以obj提升到老年代后，将对应的card修改为dirty
                live_bitmap->Set、mark_bitmap->Set，forward_address肯定不是垃圾对象，所以设置对应的位图
        else，SS的情况
            forward_address=to_space_->AllocThreadUnsafe，从to_space_中创建一个obj的拷贝对象
        CopyAvoidingDirtyingPages，进行拷贝工作，将obj的内容拷贝到forward_address中，此后forward_address就正式成为obj的拷贝对象，该函数内部使用memcopy进行内存拷贝
    SemiSpace::ProcessMarkStack：
        while，遍历mark_stack_中的元素
            ScanObject，
    SemiSpace::ScanObject：
        SemiSpaceMarkObjectVisitor，内部将为obj的每一个引用对象调用SemiSpace的MarkObject函数，最终MarkObjectIfNotInToSpace会被调用进行处理
        obj->VisitReferences
    MarkingPhase：
        对GSS而言，from_space_中能搜索到的对象将被 提升 到promo_dest_space_空间中（提升其实就是拷贝）
        对SS而言，from_space_中能搜索到的对象拷贝到to_space_中，GC之后，from_space_的空间被回收
        其他空间的处理和MarkSweep类似，处理好集合Live和集合Mark即可
        SS/GSS，空间中的对象被搜索和处理后，from_space_中的非垃圾对象就全部转移到to_space_或promo_dest_space_中了，所以在MarkingPhase函数最后，from_space_就被Clear了，而其他空间中的垃圾对象则在ReclaimPhase阶段处理
    
#### Java Reference对象的处理
    
    基础知识：
        Reference：
            是一个抽象模板类，内部没有声明任何abstract函数
            有4个派生类，SoftReference、WeakReference、PhantomReference（虚引用）、FinalizerReference
            FinalizerReference是隐藏类，一般的Java开发者无法使用，由JDK内部使用，主要是为了调用对象的finalize函数
        ReferenceQueue：
            是一个用于管理多个Reference对象的管理类
            PhantomReference必须配合ReferenceQueue使用
        一个非Reference对象obj的GC：
            通过非Reference的引用能搜索到obj，这种情况下，obj不会被回收
            通过Reference的引用搜索到obj，这种情况下，obj是否被释放取决于引用它的那个Reference对象的数据类型以及此次GC的回收策略
            无法搜索到obj，这种情况下obj会被回收
        假设引用obj的那个Reference对象叫refObj，以ART虚拟机CMS回收器为例：
            若refObj类型是SoftReference，则它引用的obj在某次GC时不一定会被释放，在CMS中，StickyMarkSweep不会释放SoftReference所引用的对象，PartialMarkSweep和MarkSweep（kGcTypeFull）均会释放SoftReference所引用的对象
            若refObj类型是WeakReference，则它引用的obj在每次GC时都会被释放，即不论什么回收策略，WeakReference所引用的对象都会被释放
            若refObj类型是PhantomReference，从垃圾对象判别的角度看，一个obj被PhantomReference引用和不被引用是毫无差别的（虚引用或幽灵引用），PhantomReference的作用在于它提供一种手段让使用者知道obj被回收了
        SoftReference、WeakReference会影响它们所引用对象的回收时机
        PhantomReference和finalize函数则提供一种机制，使得开发者知道某个对象被回收了，finalize函数对程序运行性能有一定影响，官方认为使用PhantomReference更优雅
        Java Reference：
            在虚拟机中有一个对应的类叫mirror Reference
            slowPathEnabled，
            referent，指向所引用的那个实际对象，在mirror Reference referent_成员
            queue，关联的ReferenceQueue对象
            queueNext，多个Reference对象借助queueNext可组成一个单向链表
            pendingNext，也用于将多个Reference对象组成一个单向链表，和GC有关
            get函数，获取自己所指向的实际对象，如果实际对象被回收，则返回null
            getReferent，
            clear函数，解绑自己和实际对象的关联
            getReferent的JNI实现为Reference_getReferent
            java_lang_ref_Reference.cc->Reference_getReferent：
                ScopedFastNativeObjectAccess soa，
                Heap->GetReferenceProcessor，返回Heap reference_processor_成员变量，其类型为ReferenceProcessor，是ART中专门处理Reference对象的模块
                GetReferenceProcessor->GetReferent，返回ref引用的那个实际对象
        PhantomReference的get函数永远返回null，说明和SoftReference、WeakReference的作用完全不同
        PhantomReference用法：
            先创建一个PhantomReference对象，将其和一个ReferenceQueue以及一个实际对象关联起来
            调用ReferenceQueue的remove函数，remove内部会等待，直到有一个PhantomReference对象所关联的实际对象被回收，这时，remove函数将返回这个PhantomReference对象，调用者可据此做一些清理工作（LeakCanary中有使用）
        finalize函数在对象被回收前调用
    
    MarkSweep中Reference对象的处理：
        MarkSweep::InitializePhase：
            GetCurrentIteration，调用Heap GetCurrentGcIteration，返回一个Iteration对象，用于控制GC的一些参数，并记录GC的效果（释放了多少对象、多少字节的内存等信息）
            SetClearSoftReferences，将设置Iteration clear_soft_references_成员变量，对CMS，只有回收策略为kGcTypeSticky时，clear_soft_references_才为false，其余情况均为true
        clear_soft_references_为false，表示此次回收不用处理SoftReference，只有kGCTypeSticky是才会如此处理
        MarkSweep::PausePhase：
            Heap->GetReferenceProcessor，返回Heap reference_processor_成员变量，其类型为ReferenceProcessor，是ART中专门处理Reference对象的模块
            GetReferenceProcessor->EnableSlowPath，将设置Reference类的静态成员变量slowPathEnabled为true，作用和Reference get函数有关
        MarkSweep::ScanObject：
            对象引用型成员变量的搜索，每找到一个对象，MarkSweep将调用ScanObject来搜索它的引用型成员变量
            遍历obj的引用型成员变量，如果obj是一个Java Reference对象，则调用DelayReferenceReferentVisitor
            DelayReferenceReferentVisitor，内部调用MarkSweep的DelayReferenceReferent函数
            ScanObjectVisit，
        如果被扫描的obj是一个Java Reference对象，则MarkSweep DelayReferenceReferent函数被调用
        MarkSweep::DelayReferenceReferent：
            heap_->GetReferenceProcessor->DelayReferenceReferent，调用ReferenceProcessor的DelayReferenceReferent函数进行处理
        MarkSweep::ReclaimPhase：
            ProcessReferences，
        MarkSweep::ProcessReferences：
            heap_->GetReferenceProcessor->ProcessReferences，
            Iteration->GetClearSoftReference，是否清除SoftReferences
    
    ReferenceProcessor：
        是Reference的处理者，在Heap构造函数中创建，属于内存管理的范畴
        Heap 构造函数：
            task_processor_.reset，TaskProcessor类，实现线程池并行处理Heap的一些工作
            reference_processor_.reset，指向创建的ReferenceProcessor
        ReferenceProcessor 构造函数：
            soft_reference_queue_、weak_reference_queue_、finalizer_reference_queue_、
            phantom_reference_queue_、cleared_references_均为ReferenceProcessor的成员变量，类型均为mirror ReferenceQueue
        MarkSweep在ReclaimPhase中调用ReferenceProcessor ProcessReference进行处理
        DelayReferenceReferent：
            ref，代表一个Java Reference对象
            GetReferentReferenceAddr，返回ref所引用的那个实际对象
            IsMarkedHeapReference，MarkSweep的函数检查实际对象是否被标记
            klass为ref所属的类，后面根据klass类型将ref加到不同的ReferenceQueue中
            AtomicEnqueueIfNotEnqueued调用后，ReferenceQueue中的ref对象将通过pendingNext成员变量串起来以构成一个单向链表
        在MarkSweep中，每找到一个Java Reference类型对象就会调用ReferenceProcessor DelayReferenceReferent函数：
            如果这个Reference对象引用的实际对象没有被MarkSweep标记，则把Reference对象加到ReferenceProcessor对应的ReferenceQueue中，
            此后，Reference对象将通过pending_next_构成一个单向链表
            如果这个Reference对象引用的实际对象被MarkSweep标记过了，说明这个实际对象不需要处理
        调用ProcessReferences之前，此处GC搜索到的引用型对象已经全部保存在ReferenceProcessor对应的成员变量中了，接下来就是通过该函数处理
        ReferenceProcessor::ProcessReferences：
            concurrent为true，主要用于collector与mutator同时工作而存在
            回收策略为kGcTypeSticky时clear_soft_references为false
            collector为实际的MarkSweep对象，主要由基类MarkSweep完成与Reference相关的处理工作
            collector_，
            if ！clear_soft_references
                soft_reference_queue_.ForwardSoftReference，如果不处理SoftReference时调用该函数
                collector->ProcessMarkStack，内部是搜索和标记对象
            soft_reference_queue_.ClearWhiteReferences，保存了SoftReference的队列，
            weak_reference_queue_.ClearWhiteReferences，保存了WeakReference的队列，
            finalizer_reference_queue_.EnqueueFinalizerReferences，保存的FinalizerReference对象，
            collector->ProcessMarkStack，
            针对SoftReference、WeakReference和PhantomReference再次调用ClearWhiteReferences，
            因为上面调用了回收器的ProcessMarkStack，其内部会遍历对象的引用型成员变量，这期间针对碰到Reference类型的对象又会调用DelayReferenceReferent，所以这里还需要再处理一次
        ReferenceQueue::ForwardSoftReferences：
            此次GC不用清理SoftReference对象时，针对ReferenceProcessor调用该函数
            对CMS，把SoftReference引用的实际对象加到集合Mark中，GC不清理该对象
            do...while循环
                referent_addr=ref->GetReferentReferenceAddr，referent_addr指向实际的对象，
                visitor->MarkHeapReference，MarkSweep的函数，内部将标记referent_addr对应的对象，并把它加到MarkSweep mark_stack_中，后续ProcessMarkStack时将访问该对象的引用型成员变量
                ref->GetPendingNext，遍历链表，在DelayReferenceReferent中，每找到一个Reference对象就把它加到对应的ReferenceQueue，通过Reference的成员变量pending_next_构成一个单向链表
        若不想回收SoftReference所指向的实际对象，只要对它们进行标记即可
        ReferenceQueue::ClearWhiteReferences：
            在ProcessReferences中，第一个参数cleared_references总是指向ProcessReference的cleared_references_成员变量
            while循环，遍历ReferenceQueue中通过pending_next_串起来的Reference对象，循环结束，链表也就空了
                ref=DequeuePendingReferences，从Reference pending_next_链表中取出一个元素
                referent_addr，指向实际的对象
                collector->IsMarkedHeapReference，判断ref指向的实际对象是否被回收器标记过（referent_addr对象是否在集合Mark中），若不需要回收SoftReference，在上面ForwardSoftReference函数中对实际对象进行标记
                ref->ClearReferent，若referent_addr没有被标记，该函数将解绑它和ref的关系，即ref不再引用任何实际对象
                cleared_references->EnqueueReference，把ref保存到ReferenceProcessor的cleared_references_中，其内部也是利用Reference pending_next_构建一个单向链表
        ClearWhiteReferences：
            遍历ReferenceQueue中的元素，如果这个Reference对象所指向的实际对象没有被回收器标记（说明这个实际对象除了
            被Reference引用之外不存在其他的引用关系，是可以被回收的垃圾对象），将解绑这个Reference对象和实际对象的引用关系，同时被解绑的Reference对象将加入cleared_references_ pending_next_单向链表中
            ClearWhiteReferences结束后，ReferenceQueue pending_next_链表将变成空链表，而cleared_references_保存的是实际对象被视为垃圾的Reference对象
        ReferenceQueue::EnqueueFinalizerReferences：
            针对保存FinalizerReference对象的finalizer_reference_queue_，ProcessReferences调用该函数
            while循环，
                ref=DequeuePendingReference->AsFinalizerReference，EnqueueFinalizerReferences只能用于保存了FinalizerReference对象的ReferenceQueue
                referent_addr，指向实际的对象
                collector->IsMarkedHeapReference，若FinalizerReference对象所引用的实际对象没有被标记，这里将主动标记这个实际对象，这是因为FinalizerReference对象所关联对象都是定义了finalize函数，这些对象被回收前要调用它们的finalize函数，所以不能在还没有调用finalize函数前就回收它们
                collector->MarkObject，标记实际对象
                ref->SetZombie，把实际对象和FinalizerReference的zombie_成员变量关联起来
                ref->ClearReferent，解绑ref和实际对象的关联
                cleared_references->EnqueueReference，把解绑的ref对象也保存到cleared_references中
        EnqueueFinalizerReferences：
            FinalizerReference关联的是定义了finalize函数的类的实例，Java规范要求这种对象在回收前必须调用它们的finalize函数，所以，这次GC时必须主动标记这些实际对象，要不它们在这次GC时就会被回收，后续也就无法调用它们的finalize函数（调用时出错）
            这次GC不会回收定义了finalize函数的对象，但下次回收还是需要释放它们，所以EnqueueFinalizerReferences将解绑它们和FinalizerReference的引用关系，不过为了调用finalize函数，这些对象保存在FinalizerReference的zombie_中，一旦finalize函数被调用，这个对象将和对应的FinalizerReference再无关系，此后下次GC时就可以被回收了
        ReferenceProcessor对所有类型的Reference对象的处理过程：
            1、标记不需要在本次中回收的实际对象，这些实际对象是通过SoftReference或FinalizerReference引用的
            2、解绑Reference和实际对象的引用关系--设置Reference referent_为空，FinalizerReference的zombie_成员变量依然指向实际对象
            3、所有被解绑的Reference对象加入到ReferenceProcessor cleared_references_中
        
        Heap::CollectGarbageInternal：
            Heap中GC的入口函数
            collector->Run，执行本次GC，Run返回后，垃圾对象都被回收了
            reference_processor_->EnqueueClearedReferences，调用ReferenceProcessor的函数
        ReferenceProcessor::EnqueueClearedReferences：
            Heap中回收器执行完后，Heap将调用该函数
            vm->AddGlobalRef，
            if kAsyncReferenceQueueAdd，默认为false
                Heap->GetTaskProcessor->AddTask，添加一个任务（ClearedReferenceTask）到TaskProcessor模块，其内部会使用单独一个线程来处理
            else，
                ClearedReferenceTask.Run，不使用TaskProcessor。直接执行任务ClearedReferenceTask
            cleared_references_.Clear，清空cleared_references的元素
        ClearedReferenceTask：
            Run：
                ScopedObjectAccess soa，
                InvokeWithJValues，java_lang_ref_referenceQueue_add，调用Java ReferenceQueue的add函数
                soa.Env->DeleteGlobalRef(cleared_references)
        ReferenceProcessor EnqueueClearedReferences仅仅是调用Java ReferenceQueue的add函数，该函数参数就是cleared_references成员变量（保存了本次GC中被解绑的Reference对象）
        把实际对象被解绑的Reference对象加到ReferenceQueue中和ReferenceQueue的功能有关
        实际对象被解绑而不是被回收，因为对FinalizerReference而言，它关联的实际对象并没有被回收
    
    PhantomReference的处理：
        Java Reference::add：
            ReferenceQueue中静态成员变量unenqueued，其类型为Java Reference，该函数把list加到unenqueued pending_next_所在的链表
            synchronized，
                unenqueued=list，unenqueued链表不存在的情况
                ReferenceQueue.class.notifyAll，唤醒另外一个等待的线程
        Java Reference add把Reference对象加到unenqueued链表中后将唤醒一个线程，
        在Java Daemons中，虚拟机启动后将调用Java Daemons的start函数
        Daemons::start：
            ReferenceQueueDaemon.start，启动ReferenceQueueDaemon线程
            FinalizerDaemon.start，启动FinalizerDaemon线程
            FinalizerWatchdogDaemon.start，
            HeapTaskDaemon.start，
        ReferenceQueueDaemon：
            run：
                while ReferenceQueue.unenqueued==null，
                    ReferenceQueue.class.wait，  等待unenqueued
                list=ReferenceQueue.unenqueued，保存到list
                ReferenceQueue.unenqueued=null，
                ...
                ReferenceQueue.enqueuePending，
        ReferenceQueueDaemon就是针对ReferenceQueue.unenqueued链表再次调用ReferenceQueue.enqueuePending函数
        Reference::enqueuePending：
          do..while循环
            queue=list.queue，list指向一个Reference对象，一个Reference对象关联一个ReferenceQueue对象，比如创建一个PhantomReference对象时需要指明一个ReferenceQueue，使用者可以在这个ReferenceQueue上等待与之关联的PhantomReference
            现在需要把这些Reference对象加入到和它们关联的ReferenceQueue对象中，并唤醒在等待的线程（例如调用ReferenceQueue remove的线程）
            if queue==null，为空说明Reference对象没有和ReferenceQueue关联，直接转到链表的下一个元素去处理
                next=list.pendingNext，
                list.pendingNext=list，
                list=next，
            else，
                do..while，遍历list链表，把属于queue的引用加到Reference queueNext构造的链表中
                    next=list.pendingNext，
                    list.pendingNext=list，
                    queue.enqueueLocked，
                    list=next，
                queue.lock.notifyAll，唤醒等待在queue上的线程，比如FinalizerDaemon
        finalize函数的调用（finalize函数的类实例在垃圾回收时的处理）
        ClassLinker::LoadMethod：
            加载类时用于解析其成员方法的函数
            GetMemberIndex，
            dex_file.GetMethodId，
            dex_file.StringDataByIdx，
            if strcmp(finalize，method_name)，方法名为finalize，说明这个类实现了finalize函数
                klass->SetFinalizable，设置类的标志位kAccClassIsFinalizable标志
        kAccClassIsFinalizable标志位用于说明一个类实现了finalize函数，也就是该类为finalizable
        Class::Alloc（class-inl.h）：
            （创建类实例对象的处理）
            模板函数，模板参数kCheckAddFinalizer默认值为true
            IsFinalizable，若类的标志位包含kAccClassIsFinalizable，则返回true
            obj=Heap->AllocObjectWithAllocator，obj是新创建的实例对象
            Heap->AddFinalizerReference，调用Heap的函数，传入的是指向obj的地址值，而不是obj
        如果类为finalizable，那么该类的每一个实例对象在创建时都会调用Heap AddFinalizerReference函数
        Heap::AddFinalizerReference：
            ScopedObjectAccess soa，
            InvokeWithJValues，WellKnownClasses::java_lang_ref_FinalizerReference_add，调用Java FinalizerReference add函数
            soa.Decode，
        AddFinalizerReference的功能就是调用Java FinalizerReference add函数
        具体见Java FinalizerReference类源码
        每创建一个finalizable类实例的对象，ART虚拟机内部会创建一个关联它的FinalizerReference对象，
        同时，这个FinalizerReference对象会和FinalizerReference静态成员变量queue关联
        ReferenceProcessor对FinalizerReference对象的处理：
            FinalizerReference对象所关联的实际对象被标记，只有如此处理，实际对象在本次GC时才不会被回收
            FinalizerReference对象和实际对象解绑，但任然会通过Java FinalizerReference zombie成员变量指向实际对象
            解绑的FinalizerReference对象将被添加到Java ReferenceQueue静态成员unenqueued链表中
            ReferenceQueueDaemon将把FinalizerReference投递到FinalizerReference投递到FinalizerReference静态成员queue对应的链表中，然后唤醒这个链表的线程
            等待FinalizerReference queue这个链表，见FinalizerDaemon
        具体见FinalizerDaemon源码
        FinalizerDaemon：
            private final ReferenceQueue<Object> queue = FinalizerReference.queue; 获取FinalizerReference的静态成员queue
            runInternal：
                while循环，
                    queue.poll，poll是非阻塞的，FinalizerDaemon配合FinalizerWatchdogDaemon实现线程同步/唤醒等功能
                    doFinalize，
            doFinalize：
                FinalizerReference.remove(reference); 从FinalizerReference的双向链表中移除reference
                Object object = reference.get();    获取实际对象，返回的是zombie
                reference.clear();  解除zombie和Object的关联关系
                try {
                    object.finalize();  调用finalize函数
                } catch (Throwable ex) {
                    // The RI silently swallows these, but Android has always logged.
                    System.logE("Uncaught exception thrown by finalizer", ex);
                } finally {
                    // Done finalizing, stop holding the object as live.
                    finalizingObject = null;
                }
        doFinalize返回后，实际对象object如果不能被引用到，那么它所占据的内存在后续GC相关的处理中会被释放
    总结：
        SoftReference，不保证每次GC都会回收它们所指向的实际对象，具体到ART虚拟机，回收策略为kGcTypeSticky时肯定不会回收
        WeakReference，每次GC都会回收它们所指向的实际对象
        PhantomReference，它的功能和回收没有关系，只是提供一种手段告诉使用者某个实际对象被回收了，使用者可以据此做一些清理工作，目的和finalize函数类似
        FinalizerReference，专用于调用垃圾对象的finalize函数，finalize函数调用后，垃圾对象会在下一次GC中被回收
    PhantomReference比finalize函数更优雅：    
        虚拟机中只有FinalizerDaemon这么一个线程来调用对象的finalize函数，并且FinalizerDaemon是虚拟机提供的，开发者无法干预它的工作
        若使用PhantomReference。开发者可以根据情况使用多个线程来处理。如根据绑定实际对象的类型，通过多个ReferenceQueue并使用多个线程来等待它们并处理实际对象被回收后的清理工作
    
#### Heap学习之三

    第一部分，介绍Heap构造函数的一部分以及涉及的几个关键数据结构
    第二部分，介绍Heap构造函数中涉及和空间对象有关的内容
    现在，介绍其中与内存回收有关的功能，包括：
        Heap Trim的作用
        Heap CollectorGarbageInternal，垃圾回收入口函数
        PreZygoteFork，ZygoteSpace空间在该函数中创建
        如何解决CMS导致的内存碎片问题
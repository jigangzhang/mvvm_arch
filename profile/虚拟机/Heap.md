
## Heap

#### Heap之一

    内存操作相关（P325）
    关键类：
        HeapBitmap相关类：
            new或malloc分配内存创建一个对象时得到的是该对象所在内存的地址，即指针，指针本身大小是32位或64位，所以很多对象指针本身所占内存也很大
            ART将对象的指针转换成一个位图里的索引，位图里的每一位指向一个唯一的指针，以减少指针本身所占内存空间
            位图中第x个索引位的值为1表明第x个对象存在，否则表示该对象不存在
            
            HeapBitmap：是一个辅助类，包含continuous_space_bitmaps_和large_object_bitmaps_两个vector数组，所存储的元素是ContinuousSpaceBitmap和LargeObjectBitmap
            SpaceBitmap：是一个模板类，模板参数表示内存地址对齐长度，取值为kObjectAlignment（值为8）表示按对象对齐，其对应类型别名为ContinuousSpaceBitmap
                        取值为kLargeObjectAlignment（值为4KB，一个内存页大小）表示按页对齐，对应类型别名为LargeObjectBitmap
                        SpaceBitmap类是承载位图功能的类，heap_begin_代表所针对内存的基地址（位图本身的内存基地址），bitmap_size_代表位图长度
            功能都是存储、移除、遍历对象
        ImageSpace相关类：
            Space 就是一块内存空间，这块空间存储的是Managed Object，就是Java对象在虚拟机里的表示
            Space有两个直接派生类 ContinuousSpace和 DiscontinuousSpace
            ContinuousSpace 表示该空间代表的内存块是连续的，其直接派生类为MemMapSpace 表示该空间管理的内存来自MemMap
            MemMapSpace 的一个派生类 ImageSpace 将加载编译得到的 art文件
                apk（.dex）经dex2oat编译处理后生成两个文件：.oat 和 .art
                .dex文件内容被完整拷贝到oat文件中
                .art文件就是ART虚拟机代码中常提到的Image文件
                Image根据来源（jar包或apk）分为boot镜像（boot image，来自jar）和 app镜像（app image，来自apk）
                如：/system/framework下的核心jar包的art文件统称为 boot镜像，这些核心类在ART虚拟机启动时就必须加载
                art文件通过mmap的方式加载到虚拟机中
                art虚拟机加载的是 /data/dalvik-cache下的art文件（系统更新时该路径下的art文件将重新生成）
            
            
            AllocSpace 多了一些内存分配的接口
            同时派生自 Space和 AllocSpace 表示一块来自MemMap的内存同时可在这个Space中分配对象
        一个art文件加载到虚拟机进程的内存空间后对应一个ImageSpace对象，借助 Bitmap 辅助类，可以高效管理分配在某块内存上的对象
    
    Heap 构造：
        Heap 负责管理ART中各种内存资源（不同的Space）、GC模块、.art文件加载、Java指令编译后得到的机器码和虚拟机内存相关模块交互等重要功能
        根据使用场景切换不同类型的收集器，回收方法采用逐步加大收集力度的方式
        加载art文件，如果art文件不存在，则fork一个子进程以执行dex2oat进行编译，否则直接返回ImageSpace对象
        
        Heap构造函数主要完成boot镜像所需art文件的加载，得到一系列ImageSpace对象，最后保存到Heap对应的成员变量中
        
#### Heap之二

    Heap如何创建和管理Space空间以及与之相关的一些关键类（P784）
    Heap 构造函数：
        入参：
            initial_size，由ART虚拟机运行参数-Xms指定，Android系统中该参数取值来自 dalvik.vm.heapstartsize 属性，默认为4MB
            capacity，虚拟机运行参数对应为-Xmx，取值来自 dalvik.vm.heapsize
            growth_limit，虚拟机运行参数对应为-XX:HeapGrowthLimit，取值来自属性 dalvik.vm.heapgrowthlimit，若没有设置该属性，growth_limit取值与capacity相同
            non_moving_space_capacity，对应runtime_options.def中的NonMovingSpaceCapacity参数，默认取值为Heap.h文件中的kDefaultNonMovingSpaceCapacity（64MB）
            foreground_collector_type，当应用处于前台时GC类型，如 kCollectorTypeCMS
            background_collector_type，应用处于后台时GC类型，如 kCollectorTypeHomogeneousSpaceCompact，这是一种空间压缩的方法，可减少内存碎片，但需要较长时间暂停程序运行，只能在程序位于后台时执行
            large_object_space_type，LargeObjectSpace类型，来自runtime_options.def中的LargeObjectSpace参数，如 kMap
            large_object_threshold，被认定为大对象的标准，来自runtime_options.def中的LargeObjectThreshold参数，默认取值为Heap.h中的kDefaultLargeObjectThreshold，大小为3个内存页，12KB
        ChangeCollector，desired_collector_type_取值同foreground_collector_type
        初始化live_bitmap_和mark_bitmap_，类型为HeapBitmap
        requested_alloc_space_begin，
        若image_file_name不为空，将创建ImageSpace对象：
            ImageSpace::CreateBootImage，创建ImageSpace*
            AddSpace，将boot_image_space保存到Heap对应的管理模块中
            GetOatFileEnd，为art文件ImageHeader结构体中的oat_file_end_
            requested_alloc_space_begin=AlignUp，更新requested_alloc_space_begin，使它指向oat文件末尾位置（按4KB向上对齐）
            boot_image_spaces_，数组，保存art文件对应的ImageSpace对象
        separate_non_moving_space，表示是否存在独立的non moving space
        request_begin，位于art文件对应ImageSpace的末尾，此后64MB空间预留给non moving space
        创建non_moving_space_mem_map对象
        main_mem_map_1，内存映射
        main_mem_map_2，内存映射，内存范围从main_mem_map1结束位置开始
        non_moving_space_mem_map，内存大小为64MB
        non_moving_space_，使用这个MemMap对象创建一个DIMallocSpace空间
        AddSpace，non_moving_space_加入Heap Space管理模块
        CreateMainMallocSpace，创建Heap main_space_，使用rosalloc，就是一个RosAllocSpace对象（并将main_space_赋值给rosalloc_space_）
        AddSpace
        CreateMallocSpaceFromMemMap，创建 main_space_backup_
        AddSpace
        创建 large_object_space_， AddSpace
        RemoveSpace，把main_space_backup_从Heap Space管理模块中移除，main_space_backup_本身还存在，只是不在Heap Space管理模块中
        card_table_，初始化card_table_变量
        ModUnionTableToZygoteAllocspace，为每一个ImageSpace对象创建一个ModUnionTable对象
        创建RememberedSet对象，AddRememberedSet
        mark_stack_，容器，保存的元素是指针，每个元素指向一个Object对象，growth limit和capacity取值为kDefaultMarkStackSize，大小为64KB
        allocation_stack_、live_stack_的growth limit以及capacity取值一样，growth limit为2MB，capacity为2MB+1024
    
    AddSpace：
        用于向Heap的Space管理模块中添加一个Space对象
        live/mark_bitmap_->AddContinuousSpaceBitmap，把一个位图对象加到HeapBitmap continuous_space_bitmaps_数组中
        live/mark_bitmap_->AddLargeObjectBitmap，将位图对象加入HeapBitmap large_object_bitmaps_数组中
        
        如果是连续内存地址的Space对象，则将其加入continuous_spaces_数组中，该数组元素按照内存地址的起始位置由小到大排列，
            如果这个Space对象有live_bitmap_和mark_bitmap_位图对象，则对应将其加入Heap的live_bitmap_和mark_bitmap_的管理
        如果是内存不连续的Space对象，则将其加入discontinuous_spaces_数组，如果这个Space对象内有live_bitmap_和mark_bitmap_位图对象，
            则对应将其加入Heap的live_bitmap_和mark_bitmap_的管理
        如果是可分配内存的Space对象，则将其加入alloc_spaces_数组
    RemoveSpace：
        用于从上述管理模块中移除一个Space对象
        live/mark_bitmap_->RemoveContinuousSpaceBitmap
        continuous_spaces_.erase
        alloc_spaces_.erase
    
    
    关键类介绍：
        4个防辅助性质的数据结构，作用和GC有关
        CardTable：可看作是一个元素大小为1字节的数组，该数组每一个元素叫作一个Card
                对AllocatedSpace按128字节大小进行划分，每一个Card对应为Space中的一个128字节区域
                凡是内存地址位于某一个128字节区域的Object对象都对应同一个Card
                在这个128字节区域内的Object对象中，只要有一个引用了新生代的对象，则该区域对应的Card元素将被标记为Dirty Card
                RememberedSet以单个Object为管理对象，CardTable以128字节空间为管理对象，128字节空间中可能有多个Object
                CardTable构造在一个内存映射对象上，起始位置4KB，结尾为4GB，Heap利用这一个CardTable对象维护Heap中其他空间所需的card信息
        RememberedSet：容器类，每个引用了新生代对象的老年代对象都保存在其中，优点是精确，缺点是需要额外的内存空间来保存这些老年代对象
                一个RememberedSet会关联一个ContinuousSpace，用于记录ContinuousSpace中的Object是否引用了其他Space空间中的Object等信息
                只有DIMallocSpace或RosAllocSpace会关联RememberedSet
        ModUnionTable：和RememberedSet的作用差不多，被ImageSpace或ZygoteSpace使用
        ObjectStack：
        Free Space：空闲区域（内存空间）
        Allocated Space：已被使用的内存区域
        分代GC的核心思想是通过将Allocated Space分为多个区域，针对这些区域做针对性地GC，从而减少GC的工作量
        老年代引用新生代中的对象，需要虚拟机主动记录哪些老年代对象引用了新生代的对象
        分代GC不仅要扫描新生代对应的root，还需要扫描这些纳入记录的老年代对象
        
        虚拟机每次执行iput-object指令--给某个对象的引用型成员变量赋值时，就会进行一次记录
        这个记录的地方就叫Write Barrier，表明执行写动作的时候需要在这个地方处理一些别的事情
        Read Barrier：在执行读动作的时候做一些事情
        
        解释执行模式下的处理（iput-object指令）：
            iput-object指令将触发DoFieldPut函数别调用（interpreter_common.cc）
            DoFieldPut函数内部调用ArtField的SetObj，SetObj内部调用Object的SetFieldObject函数
            SetFieldObject函数：
                SetFieldObjectWithoutWriteBarrier，设置对应成员变量的值使其不使用Write Barrier，其内部就是更新本Object对象 field_offset内存处的值为new_value
                WriteBarrierField，只要new_value不为空，都需要调用Heap的该函数
            WriteBarrierField：
                card_table_->MarkCard，将标记dst（源对象）对应的Card标志为kCardDirty
        WriteBarrier的功能就是如果设置了某个对象的引用型成员变量，则该对象在CardTable中对应的card值将设置为kCardDirty
        
        机器码执行模式下的处理（iput指令）：
            InstructionCodeGeneratorX86::HandleFieldSet：
                needs_write_barrier，如果成员变量的数据类型为引用，并且所赋的值不为空，则为true
                生成赋值操作相关的机器码
                生成Write Barrier相关机器码
                MarkGCCard
            CodeGeneratorX86::MarkGCCard：
                movl，获取调用线程card_table的内存位置，将其保存在Card寄存器中
                生成对应的机器码，功能为计算object对应的Card位置，然后设置该Card的状态，效果与调用解释模式中的MarkCard函数一样
            Thread::InitCardTable：
                每个线程 tlsPtr_的card_table成员变量由该函数在线程初始化时设置
                tlsPtr_.card_table=Runtime::Current().heap.cardTable.GetBiasedBegin
                GetBiasedBegin返回这个CardTable用于存储记录的内存空间的起始地址
        
        CardTable：
            在Heap构造函数中调用Create函数创建的
            Create：
                计算需要多少个Card， heap_capacity/kCardSize
                创建一个MemMap映射对象，其大小为capacity+256
                最后创建CardTable对象
            mem_map_，CardTable对应的用于保存信息的内存空间
            biased_begin_，CardTable记录信息不是从mem_map_起始处开始，而是从biased_begin_处开始
            offset_，主要作用和计算biased_begin_的计算有关
            MarkCard：
                返回addr对应的Card，然后设置其值为kCardDirty
                CardFromAddr(addr) = kCardDirty
            CardFromAddr：
                用于根据某个Object对象的地址找到对应的Card地址
                由基地址加上addr右移7位（相当于除以128）以得到对应card的位置，而CardTable的基准位置从biased_begin_算起
            IsDirty：
                入参为一个mirror Object对象，用于判断这个对象是否有引用型成员变量被设置
            ModifyCardsAtomic，用于修改从scan_begin到scan_end内存范围对应CardTable card的值
            Scan，用于扫描从scan_begin到scan_end内存范围对应的CardTable中的card，如果card的值大于或等于参数minimum_age，则调用bitmap的VisitMarkedRange函数
        RememberedSet：
            在Heap构造函数中构造
            CardSet，类型别名，是一个std set容器，一个CardTable中的一个Card，也就是该Card的地址保存在CardSet中
            name_，RememberedSet的名称
            heap_
            space_，关联一个Space
            dirty_cards_，CardSet类型
            ClearCards：
                清除space_里Object的跨Space的引用关系，也就是去除CardTable中对应card的kDirtyCard标志
                RememberedSetCardVisitor card_visitor，
                card_table->ModifyCardsAtomic，扫描space_对应的card，调用AgeCardVisitor函数获取card的新值，然后调用card_visitor函数对象
            AgeCardVisitor：用于给一个Card设置新值
            如果card的新旧值不同，则调用RememberedSetCardVisitor函数对象
            RememberedSetCardVisitor将把之前为kCardDirty的card保存到RememberedSet的dirty_cards_容器中
            ClearCards，清除space_对应的card的kDirtyCard标志，将旧值为kDirtyCard的card地址保存到dirty_cards_容器中以留作后用
            UpdateAndMarkReferences：
                将遍历space_里所有存在跨Space引用的Object，然后对它们进行标记
                target_space，将检查space_中的Object是否引用了位于target_space空间中的Object
                如果space_中的对象引用了target_space中的对象，则下面这个变量会被设置为true
                RememberedSetObjectVisitor，创建函数对象
                bitmap，要遍历一个ContinuousSpaceBitmap中包含的Object，需要借助与之关联的位图对象
                dirty_cards_容器已包含了space_中那些标志为kDirtyCard的card信息
                for循环遍历dirty_cards中的card：
                    AddrFromCard，将card地址转换为Space中对应的那个128字节单元的基地址
                    VisitMarkedRange，访问这个128字节单元中的Object，调用obj_visitor函数对象，每得到一个Object，就调用一次obj_visitor函数对象
                    remove_card_set，如果这个128字节中的Object没有引用target_space中的Object，则对应的card区域需要从dirty_cards容器中移除，先将card临时存到remove_card_set中，后续一次性移除
        ModUnionTable：
            用于减少GC的工作量
            是一个纯虚类
            ModUnionTable只和ImageSpace、ZygoteSpace相关联
            RememberedSet只和MallocSpace关联
            在Heap构造函数中有AddModUnionTable，为每个ImageSpace对象创建一个ModUnionTableToZygoteAllocspace对象
            
            space_，关联的空间对象
            CardSet，存储card的地址
            CardBitmap，类型别名，为MemoryRangeBitmap
            ClearCards函数，作用为清除ModUnionTable space_在Heap card_table_中对应的card的值，如果card旧值为kCardDirty，则设置其新值为kCardDirty-1，否则设置其新值为0
            SetCards函数，用于设置ModUnionTableCardCache内部的一些信息
            UpdateAndMarkReferences，用于扫描space_覆盖的card，如果是dirty card，说明space_中的对象有引用型成员变量，如果这个对象的引用型成员变量指向的对象位于别的空间中，则变量这个对象（调用Object VisitReference函数）
        ObjectStack：
            类型别名，定义为AtomicStack<Object>
            是一个可支持多线程原子操作的数据容器，容器保存的元素的数据类型为mirror Object*
            name_，实例名称
            mem_map_，使用内存映射资源来提供存储空间
            growth_limit_，存储容量，元素个数不允许超过growth_limit_
            capacity_，最大存储容量，growth_limit_小于或等于capacity_
            Create函数，创建一个AtomicStack实例
            AtomicPushBack，从尾部压入一个元素，原子操作
            PopBack、PopFront，可以从两端出栈（只能从尾部入栈）
            
    ObjectVisitReferences：
        用于遍历一个对象的引用型成员变量的关键函数Object VisitReferences
        mirror Class标志位：
            Access_flags_，用于描述类的访问权限，public、protected等信息将转换为对应的访问标志位，如：kAccPublic、kAccProtected
            class_flags，用于描述一个Object所属类的类型，是供ART虚拟机内部使用的，用于控制Object VisitReferences函数的行为
                kClassFlagNormal，值为0，是默认值，mirror Object有一个指向类的klass，如果还有其他引用型成员变量，则类标志位取值为此
                kClassFlagNoReferenceFields，值为1，不存在除klass外的其他引用型成员变量的类（String类，基础数据类型的数组类等）
                kClassFlagString，代表Java String类，Java String类标志位同时包括kClassFlagNoReferenceFields
                kClassFlagObjectArray，类的数据类型为数组，并且数组元素的数据类型必须为引用
                kClassFlagClass，类的数据类型为Java Class
                kClassFlagClassLoader，类的数据类型为ClassLoader或其子类
                kClassFlagDexCache，类的数据类型为DexCache
                kClassFlagSoftReference，类的数据类型为Java SoftReference
                kClassFlagWeakReference，类的数据类型为Java WeakReference
                kClassFlagFinalizerReference，类的数据类型为Java FinalizerReference
                kClassFlagPhantomReference，类的数据类型为Java PhantomReference
                kClassFlagReference，说明类属于Reference中的一种
        VisitReferences：
            是一个模板函数，kVisitNativeRoots、kVerifyFlags和kReadBarrierOption用于描述该如何访问对象的引用型成员变量
                Visitor和JavaLangRefVisitor用于通知调用者的函数对象
            klass，为Object所属的类（即mirror Object的成员变量klass）
            visitor，访问klass（Object的第一个引用型成员变量）
            class_flags，获取类的类型
            如果是kClassFlagNormal，VisitInstanceFieldsReferences，访问其引用型成员变量（非静态）
            其他类标志位，如果目标对象本身就是一个Java Class对象，则将其转换为Class对象，然后调用mirror Class VisitReferences函数
            如果是一个Object数组，则转换为mirror ObjectArray（AsObjectArray），调用它的VisitReferences
            如果是Java Reference的一种，则先调用VisitInstanceFieldsReferences，然后调用LavaLangRefVisitor
            如果是DexCache，则将自己转换为DexCache，调用它的VisitReferences函数
            ClassLoader，调用它的VisitReferences函数
        Class VisitReferences：
            VisitReferences在Class中的实现（基类Object的虚函数）
            VisitInstanceFieldsReferences，调用mirror Object的该函数，以访问非静态的引用型成员变量
            VisitStaticFieldsReferences，访问静态引用型成员变量
            VisitNativeRoots，用于访问类的成员函数（对应为ArtMethod）、成员变量（ArtField），均定义在mirror Class中
        Class VisitNativeRoots：
            field.VisitRoots，访问静态成员变量
            访问非静态成员变量
            for循环中，method.VIsitRoots，访问所有成员方法（不包括继承的）
        ArtField VisitRoots：
            visitor.VisitRoot，调用函数对象的VisitRoot函数，declaring_class_是该成员变量所属的类
        
        ObjectArray VisitReferences：
            for循环中，遍历并访问数组中的元素，visitor
        DexCache VisitReferences：
            VisitInstanceFieldsReferences
            GetStrings，访问DexCache里的字符串信息
            调用Visitor VisitRootIfNonNull函数
            访问DexCache里的类型信息，resolved_types_成员变量
        ClassLoader VisitReferences：
            VisitInstanceFieldsReferences
            GetClassTable
            VisitRoots，调用ClassTable的VisitRoots函数，内部将调用visitor的VisitRoots函数
        
#### Heap之三

    
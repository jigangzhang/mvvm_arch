
## ART中的JNI

    JNI在ART虚拟机中的实现
    Java Native Interface，Java层和Native层的接口，借助这个接口，Java可以和Native交互
    主要内容：
        ART虚拟机中的JavaVM、JNIEnv对象
        Java层中的native方法如何与Native层中对应的方法相关联
        Native层如何调用Java层中的方法
        JNI中的Local、Global和WeakGlobal Reference等
        
#### JavaVM和JNIEnv

    JavaVM，代表Java虚拟机，每一个Java进程有一个全局唯一的JavaVM对象
    JNIEnv，是JNI运行环境的含义，每一个线程都有一个JNIEnv对象，Java线程执行JNI相关操作时，都需要利用该线程对应的JNIEnv对象
    JavaVM和JNIEnv是jni.h里定义的数据结构，里边都是函数指针成员变量，不同虚拟机实现都会从它们派生出实际的实现类
    
    Runtime::Init函数中（runtime.cc）：
        java_vm_ = new JavaVMExt(this, runtime_options);
        java_vm_ 是Runtime的成员变量，指向一个JavaVMExt对象
        java_vm_ 对象就是ART虚拟机中全局唯一的虚拟机代表，通过Runtime GetJavaVM函数可返回这个成员
    
    Thread::Init函数中（thread.cc）：
        tlsPtr_.pthread_self = pthread_self();
        ...
        tlsPtr_.jni_env = JNIEnvExt::Create(this, java_vm);
        tlsPtr_是ART虚拟机中每个Thread对象都有的核心成员
        jni_env是tlsPtr_的成员变量，类型为JNIEnvExt*，就是每个Java线程所携带的JNIEnv对象
        
    JavaVMExt类：
        派生自JavaVM
        构造函数：
            初始化globals_对应的IndirectReferenceTable（简称IRTable）对象
            IRTable构造函数三个参数：
                gGlobalsInitial 值为512，表示IRTable初始容纳512个元素；
                gGlobalsMax 值为51200，表示IRTable最多容纳51200个元素；
                kGlobal 枚举变量，定义 IndirectRefKind{kHandleScopeOrInvalid=0，kLocal=1，kGlobal=2，kWeakGlobal=3}
            weak_globals_，用于管理WeakGlobal引用的IRTable对象，kWeakGlobalsInitial值为16（初始值），kWeakGlobalMax为51200
        LoadNativeLibrary函数：
            Java native方法对应Native层的代码逻辑封装在动态库文件中
            JNI的一个重要工作就是加载一个包含native方法实现的动态库文件
            LoadNativeLibrary就是用来加载指定的动态库文件
            入参，path 代表目标动态库的文件名，不包含路径信息，Java层通过System.LoadLibrary加载动态库时只需指定动态库名称，不包含路径和后缀；
                class_loader，目标库必须和一个ClassLoader对象相关联，同一个目标库不能由不同的ClassLoader对象加载；library_path，动态库文件搜索路径，在这个路径下搜索path对应的动态库文件
            libraries_->Get，可能会有多个线程触发目标动态库加载，所以先同步判断一下path对应的动态库是否已经加载
            libraries_内部有一个map容器，保存了动态库名和一个动态库的关系（SharedLibrary）
            如果libraries_中已经有库了，则需要检查加载它的ClassLoader对象和传入的class_loader是否为同一个
            OpenNativeLibrary，加载动态库，Linux平台使用dlopen方式加载，Android做了定制，比如，一个应用不能加载另一个应用携带的动态库
            new_library，构造一个SharedLibrary对象，将其保存到libraries_内部的map中
            FindSymbol，找到动态库中的JNI_OnLoad函数，如果有则需要执行它
            一般动态库会在JNI_OnLoad函数中将Java native方法和Native层对应的实现函数绑定，利用JNIEnv RegisterNativeMethods完成
            JNI_OnLoad函数返回值为JNI_ERR，表示JNI_OnLoad处理失败，返回值version取值必须为JNI_VERSION_1_2、JNI_VERSION_1_4、JNI_VERSION_1_6中的一个
            
            JNI加载动态库包括两步：
                1、动态库本身加载到虚拟机进程，保存到libraries_中
                2、如果有JNI_OnLoad函数，执行它，执行失败，表示动态库加载失败，无论失败与否，动态库都已经加载到虚拟机内存了，失败不会卸载动态库
        FindCodeForNativeMethod函数：
            搜索Java native方法在native层的实现，根据Java native方法的签名信息（所属类的全路径、返回值类型、参数类型等组成）来搜索动态库
            libraries_->FindNativeMethod，内部遍历libraries_的map容器，找到符合条件的Native函数，返回值是一个函数指针对象
        FindNativeMethod函数：
            JniShortName、JniLongName根据Java native方法的信息得到Native函数的函数名
            遍历libraries_容器，调用FindSymbol（内部使用dlsym）函数搜索目标函数
            JniLongName包含Java native方法的参数信息
            JniShortName不包含Java native方法的参数信息
    
    JNIEnvExt类：
        派生自JNIEnv
        Create，静态函数，返回JNIEnvExt*实例
        PushFrame、PopFrame，JNI层对引用对象的管理有关
        AddLocalReference、NewLocalRef、DeleteLocalRef，和Local引用对象的管理有关
        Java层传入的jobject参数以及JNI层内部通过NewObject等函数创建的对象都属于Local引用对象，从JNI返回Java层后，这些Local引用对象理论上就属于被回收的对象
        JNI层想在下一次JNI调用或其他JNI函数内使用一些引用对象，则需要通过NewGlobalRef或NewGlobalWeakRef将其转换成一个Global或WeakGlobal引用对象
        JavaVMExt保存了Global和WeakGlobal的引用对象
        
        构造函数：
            初始化locals，为IRTable对象，kLocalsInitial取值为64，kLocalsMax取值为512
        GetJniNativeInterface函数：
            JNI规范针对JNIEnv定义有两百多个函数，ART虚拟机中这些函数对应的实现由JNINativeInterface*指示
            
#### Java native方法的调用

    一个代表Java native方法的ArtMethod对象里两个和机器码入口有关的变量取值为：
        1、dex2oat编译Java native方法后将生成一段机器码，ArtMethod对象的机器码入口地址会指向这段生成的机器码，
            这段机器码本身会跳转到这个ArtMethod对象的JNI机器码入口地址，如果这个JNI方法还没注册过（native方法还未和Native层对应的函数相关联），
            这个JNI机器码入口地址是art_jni_dlsym_lookup_stub，否则，JNI机器码入口地址指向Native层对应的函数
        2、若dex2oat没编译过这个Java native方法，则ArtMethod对象的机器码入口地址为跳转代码art_quick_generic_jni_trampoline，
            同样，如果这个JNI方法还没注册过，则JNI机器码入口地址为跳转代码art_jni_dlsym_lookup_stub，否则，JNI机器码入口地址指向Native层对应的函数

    art_jni_dlsym_lookup_stub：
        如果JNI方法没有注册，则需要先关联JNI方法和它的目标Native函数，由art_jni_dlsym_lookup_stub实现
        宏内部调用artFindNativeMethod函数，如果找到对应的 Native函数，则返回该函数对应的函数指针，以jmp的方式跳转到Native对应的函数
        该函数中没有为Native函数准备参数，说明art_jni_dlsym_lookup_stub的调用者一定先准备好Native函数的参数，机器码先准备好参数，再调用ArtMethod对象的JNI机器码入口地址
        跳转指令有jmp、call
    artFindNativeMethod函数：
        FindCodeForNativeMethod，调用JavaVMExt的FindCodeForNativeMethod，搜索目标函数
        RegisterNative，如果存在满足条件的目标函数，则更新ArtMethod对象的JNI机器码入口地址，此后再调用这个Java Native方法，则无需借助art_jni_dlsym_lookup_stub
    对于一个Java native方法的调用，调用时进入的是机器码入口，而不是JNI机器码入口，因为JNI函数调用需要准备额外的参数，所以无法直接跳转到JNI机器码入口
    当外界调用Java native方法时：
        1、先跳转到它的机器码入口地址（entry_point_from_quick_compiled_code_），执行一段代码后，再跳转到2
        2、JNI机器码入口地址（entry_point_from_jni_），若目标函数未注册，跳转到4，已注册跳转到3
        3、目标函数
        4、art_jni_dlsym_lookup_stub，函数内部先搜索目标函数，然后jmp到目标函数去执行
    
    art_quick_generic_jni_trampoline：
        如果Java native方法没被dex2oat编译过，其ArtMethod对象的机器码入口就是art_quick_generic_jni_trampoline
        由汇编编写
        内部调用artQuickGenericJniTrampoline函数、artQuickGenericJniEndTrampoline函数、异常判断及处理
        
        先在栈上分配一块足够大的空间，然后调用artQuickGenericJniTrampoline函数，这个函数将为Native函数准备参数
        然后调用由EAX寄存器所指向的Native函数的位置去执行这个Native函数
        从Native函数返回后，调用artQuickGenericJniEndTrampoline函数
    
    artQuickGenericJniTrampoline函数：
        根据Java native方法的签名信息计算Native函数所需的栈空间以及准备参数
        BuildGenericJniFrameVisitor visitor(...);
        visitor.VisitArguments();
        visitor.FinalizeHandleScope(self);
        
        JniMethodStart函数
        GetEntryPointFromJni，获取ArtMethod对象的JNI机器码入口
        GetJniDlsymLookupStub，若还未和Native函数绑定（机器码入口地址为art_jni_dlsym_lookup_stub），将调用artFindNativeMethod函数查找目标Native函数
        artFindNativeMethod
        GetBottomOfUsedArea，返回针对此次Native函数调用所需栈空间的位置
    artQuickGenericJniEndTrampoline函数：
        调用了GenericJniMethodEnd，和JniMethodEnd功能类似
        
#### CallStaticVoidMethod
    
    Java程序的入口main函数就是由JNI来调用的
    CallStaticVoidMethod就是JNI给Native层调用一个Java方法的接口
    该函数实现在jni_internal.cc中
    
    函数中调用了InvokeWithVarArgs函数
    InvokeWithVarArgs函数：
        soa.DecodeMethod
        BuildArgArrayFromVarArgs函数
        InvokeWithArgArray函数
    InvokeWithArgArray函数中调用了ArtMethod的Invoke函数
    
    Native层借助JNI提供的API调用Java方法，最终会通过目标方法所属ArtMethod对象的Invoke来完成函数调用
    
#### JNI中引用型对象的管理
    
    C++创建对象使用new操作符以先分配内存，然后构造对象，通过delete操作符先析构这个对象，然后回收该对象所占的内存
    JNI层利用JNI NewObject等函数创建一个Java意义的对象（引用型），这个被New出来的对象是Local型的引用对象
    JNI层用过DeleteLocalRef释放Local型引用对象（等同Java的赋值null），如果不调用DeleteLocalRef，Local型对象在JNI函数返回后，有虚拟机根据垃圾回收策略进行标记回收
    除Local型对象外，JNI层借助JNI Global相关函数将Local型引用对象转换成Global型对象，Global型对象的回收只能先由程序显示调用Global相关函数删除，然后虚拟机才能借助垃圾回收机制回收
    
    关键类：
        IndirectReferenceTable，就是虚拟机JNI实现中用来管理JNI层中创建的引用型对象的关键数据结构
        JavaVMExt内有globals_和weak_globals_两个IRTable来管理JNI层中的Global型和WeakGlobal型引用对象
        JNIEnvExt内有locals一个IRTable成员变量，用来管理每个Java线程因JNI调用而创建的Local型引用对象
        Global型引用对象需要调用者显式释放
        Local型引用对象管理略复杂，需要记录对象在哪个native方法中创建的，这些状态信息存储在cookie变量中
        释放只是设置持有它们变量的值为null，还需要虚拟机的GC模块统一处理 回收
        
        IndirectReferenceTable类：
            table_mem_map_，内存映射，代表一块内存空间，就是IRTable内部用于存储管理引用对象数据结构的地方
            IrtEntry，是IRTable管理的一个引用对象的数据结构，表中元素对应的数据结构
            IRTSegmentState，32位长的联合体（union），cookie就是由它来表达的
            
            IRTable将IrtEntry元素按照数组的方式来管理，数组的头由table_成员变量表示
            添加或删除一个引用对象就是围绕这个table_数组展开的
            JniMethodStart、JniMethodEnd对表达IRTable存储状态的cookie做了精心的保护和还原
        
            Add函数：
                cookie为table_的存储空间状态
                将obj存到table_数组中（IrtEntry），若存在空洞，存到空洞位置上，没有的话，添加到数组末尾
                ToIndirectRef，将索引位置转换为IndirectRef
                ToIndirectRef，内部调用IrtEntry的GetSerial，得到serialChunk，ref的前两位表示引用对象的类型（kLocal、kGlobal），中间10位为存储空间的索引位，后20位为serialChunk
            
            Remove函数：
                从IRTable中移除对象
                从IndirectRef中提取索引位置，设置对应索引位IrtEntry references_[serial_]值为nullptr即可
                空洞的处理等
        
        NewObject、jobject的含义：
            NewObject是JNI提供的用来构造一个Java对象的函数，内部调用NewObjectV
            NewObjectV函数：
                返回值为jobject
                AllocObject，创建一个对象（mirror::Object），由于在JNI层，归为kLocal型的引用对象
                soa.AddLocalReference，该函数内部调用JNIEnvExt的AddLocalReference
                JNIEnvExt的AddLocalReference函数内部调用locals.Add函数（添加到IRTable中）
            NewObjectV返回值jobject其实就是IndirectRef，借助Thread的DecodeJObject，找到IndirectRef间接引用的mirror Object
            Thread::DecodeJObject函数：
                将jobject转换为IndirectRef
                GetIndirectRefKind，判断obj的引用类型，kHandleScopeOrInvalid、kLocal、kGlobal、kWeakGlobal
                如果是local型，从local的IRTable中找到对应的对象
                如果是kHandleScopeOrInvalid型，如果是栈上传递过来的对象，直接转换成mirror Object对象
                如果是global型，交给JavaVMExt global_来处理
                对WeakGlobal型，如果该对象已经回收，则返回nullptr
            jobject的真实类型是IndirectRef：
                如果jobject来自HandleScope，则IndirectRef的真实类型是StackReference<mirror::Object>*
                如果是kLocal、kGlobal、kWeakGlobal型对象，借助IRTable来解析这个IndirectRef以得到一个索引号，然后再从IRTable table_数组对应的位置里取得所保存的mirror Object对象
        
        JNI中引用对象相关：
            NewLocalRef、DeleteLocalRef：在JNIEnvExt中做add、remove操作
            NewGlobalRef、DeleteGlobalRef：在JavaVMExt中AddGlobalRef、DeleteGlobalRef
        
        PushLocalFrame、PopLocalFrame：
            JNIEnvExt的locals IRTable只能存储512个local型引用对象
            JNI提供这两个函数以扩展容量
            PushLocalFrame，压入一个栈帧，后续的NewObject将在这个栈帧对应的资源中分配
            PopLocalFrame，退出栈帧，这个栈帧上分配的资源将被释放
            AndroidRuntime::startReg函数中有使用这两个函数
            
        回收引用对象：
            ART虚拟机对local型引用对象释放的处理方法是：
                当JNI函数返回后，只要在JniMethodEnd里还原之前的IRTable状态即可
                真正的释放是在GC阶段做的，会调用Heap::TrimIndirectReferenceTable函数（GC模块中）
            TrimIndirectReferenceTable函数：
                TrimGlobals，释放JavaVMExt globals_ IRTable的资源，内部执行globals_.Trim()
                RunCheckpoint，所有Java线程在checkpoint处将运行闭包（TrimIndirectReferenceTableClosure）以释放各线程的Local型引用对象
            TrimIndirectReferenceTableClosure：
                Run函数，核心还是调用IRTable的Trim函数
            IndirectReferenceTable的Trim函数：
                释放不再需要的引用对象
                Capacity，找到当前存储的最高索引位置
                release_start，确认该位置对应到内存的起始位置
                table_mem_map_映射内存的尾部
                madvise函数，通知内核释放从release_start开始指定长度的内存
        
        调用一次native方法将包含三个函数的调用，分别是JniMethodStart、目标Native函数、JniMethodEnd
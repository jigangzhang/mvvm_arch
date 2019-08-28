
## 解释执行和JIT
    
    ART虚拟机中Java指令（dex指令或对应的机器码）的执行有两种方式：
        1、解释执行
        2、执行编译后得到的机器码
    两种执行方式是穿插运行：
        一个方法以解释方式执行，但其内部调用的某个方法却存在机器码，执行到调用内部方法的指令时解释执行将切换为以机器码方法运行
        一个以机器码执行的方法可能因其内部调用了某个没有对应机器码的函数后又将以解释方式执行被调用的方法
        某个方法在dex2oat阶段未被编译，但在多次运行后该方法被编译为对应的机器码，该方法在JIT编译处理之前以解释方式执行，编译后以机器码方式执行
        HBoundsCheckElimination优化（取消数组越界检查），若不能取消BoundsCheck，将添加HDeoptimize IR指令，最终生成机器码，在执行到HDeoptimize IR对应的机器码时后续指令将切换到解释模式执行
    
    本章核心：
        基础知识，包括ArtMethod中某些关键成员变量的设置、函数调用时参数传递等内容
        ART中解释执行的入口函数以及解释执行的流程
        ART中的JIT，OSR（On Stack Replacement）技术
        HDeoptimize IR处理相关的流程
        Instrumentation、异常投递和处理
        

#### 基础知识
    
    Java方法以何种方式执行与其对应的ArtMethod对象里保存的某些信息密切相关
    ClassLinker::LinkCode：
        oat_class，为OatFile::OatClass，内容由oat文件中OatClass区域相应位置处的信息构成
        oat_method，为OatFile::OatMethod，内容由oat文件中OatMethod区域对应的OatQuickMethodHeader信息构成
        GetOatMethod函数，获取Java方法对应的OatMethod信息，若没被编译过，返回的OatMethod对象的code_offset_取值为0，OatMethod.code_offset_指向对应机器码在oat文件中的位置
        oat_method.LinkMethod函数，设置ArtMethod ptr_sized_fields_ entry_point_from_quick_compiled_code_为Oat文件区域OatQuickMethodHeader的code_（code_处为编译得到的机器码）
        ShouldUseInterpreterEntrypoint函数，返回true表示使用解释执行模式（机器码入口为空（该方法未经过编译）或虚拟机进入调试模式状态）
        
        该函数主要为各种方法指定跳转地址
        tls_ptr_sized_entry_point_from_quick_compiled_code_，用于非jni方法，指向对应的机器码入口
        tls_ptr_sized_entry_point_from_jni_，用于jni方法，指向jni方法对应的机器码入口地址，tls_ptr_sized_entry_point_from_quick_compiled_code_也会被设置（jni方法）
        
    Runtime ArtMethod：
        art文件格式头ImageHeader中的RuntimeMethod Section区域，包含6个Runtime ArtMethod
        Runtime ArtMethod是由ART虚拟机创建的一个和Java方法无关的ArtMethod对象
        由 ClassLinker的CreateRuntimeMethod函数创建：
            AllocArtMethodArray，从linear_alloc中分配一块内存以构造一个ArtMethod数组，个数为1
            SetDexMethodIndex，Runtime Method和源码无关，其dex_method_index_成员变量（方法在dex文件method_ids中的索引）取值为kDexNoIndex
        六个Runtime ArtMethod：
            callee_save_methods_，是一个数组，包含三个元素，分别对应三种CalleeSaveType类型，
                    该数组元素为uint64_t类型，实际上是一个Runtime ArtMethod对象的地址，这三个Runtime ArtMethod主要用于跳转代码
            resolution_method_，用于解析被调用的函数是谁，在InitWithoutImage中通过CreateResolutionMethod函数创建
            imt_conflict_method_，解决冲突的，在InitWithoutImage中通过CreateImtConflictMethod函数创建
            imt_unimplemented_method_，用于处理一个未解析的接口方法，在InitWithoutImage中通过CreateImtConflictMethod函数创建
            后面三个对象都在ClassLinker的InitWithoutImage函数中设置创建，由dex2oat生成boot镜像时使用，这几个对象的内容和地址都写入到boot.art文件中，在zygote进程中的完整虚拟机中被设置到Runtime对象中
            
            Runtime::CreateCalleeSaveMethod，创建另外三个Runtime Method，和CalleeSaveType有关
    
        Trampoline code：
            有两类：
                1、针对jni方法的Trampoline code，封装在JniEntryPoints结构体中，只包含一个pDlsymLookup函数指针
                2、针对非jni方法的Trampoline code，封装在QuickEntryPoints结构体中，包含132个函数指针
            上面两组Trampoline code（函数指针）包含在Thread类的tlsPtr_相关成员变量中
            两组Trampoline code都由汇编实现
            
            oat文件里的Trampoline code
            
    栈和参数传递：
        函数调用的两个问题：目标函数是什么（目标函数代码在哪），函数的参数如何传递
        kSaveAll、kRefsOnly、kRefsAndArgs三种情况（是上面的callee_save_methods_数组对应的三个Runtime ArtMethod对象）
        寄存器相关操作
        栈操作
        
        遍历栈中的参数：
            ShadowFrame，该类用于描述解释执行模式下某个函数对应的栈帧：
                method_，ShadowFrame对象所关联的、代表某Java方法的ArtMethod对象
                code_item_，该Java方法对应的、来自dex文件的dex指令码
                dex_pc_，从dex指令码dex_pc_指定的位置处执行
                link_，函数的局部变量ShadowFrame对象内部的函数的局部变量ShadowFrame通过link_成员变量指向最开始的函数的ShadowFrame，从最里面的函数返回时它的ShadowFrame将被回收
                vregs_，代表该函数所需的参数
            StackedShadowFrameRecord，辅助的功能类
            ManagedStack，统一管理解释执行和机器码方式执行
            QuickArgumentVisitor，辅助类，用于访问kRefsAndArgs对应的栈帧中的参数
            BuildQuickShadowFrameVisitor，派生自QuickArgumentVisitor，用于访问解释执行模式下栈帧中的参数，解释执行下函数对应的栈帧用ShadowFrame描述
            
            ShadowFrame的实例不是在进程的堆空间中创建，而是利用alloca在调用函数的栈中先分配一块空间，然后利用placement new 在这块空间中创建ShadowFrame实例
            具体代码在 CREATE_SHADOW_FRAME宏中
            BuildQuickShadowFrameVisitor用于将参数填充到一个ShadowFrame对象中


#### 解释执行
    
    一个方法解释执行，其ArtMethod对象的机器码入口将指向一段跳转代码--art_quick_to_interpreter_bridge
    art_quick_to_interpreter_bridge是一段汇编写的函数
    art_quick_to_interpreter_bridge的跳转目标是函数artQuickToInterpreterBridge
    artQuickToInterpreterBridge函数：
        构造ShadowFrame对象（ArtMethod的栈帧），并借助BuildQuickShadowFrameVisitor将该方法（Java方法对应的ArtMethod对象）所需的参数存储到这个ShadowFrame对象中
        若ArtMethod所属类没有初始化，则先初始化它，调用ClassLinker的EnsureInitialized函数
        进入EnterInterpreterFromEntryPoint函数，就是解释执行模式的核心处理函数
    EnterInterpreterFromEntryPoint函数中有和JIT相关的处理，最后又调用Execute函数
    Execute函数：
        stay_in_interpreter，表示是否强制使用解释执行模式。默认false，表示如果方法存在jit编译得到的机器码则转到jit执行
        ShadowFrame的dex_pc_，如果为0，则表示该方法从一开始就以解释执行方式执行，称为纯解释执行的方法，需要检查它是否经过JIT编译了，若有，转入jit编译的机器码去执行并返回结果
        解释执行有三种实现方式，由kInterpreterImplKind取值控制：
            kMterpImplKind（默认取值），对应ExecuteMterpImpl函数，根据不同CPU，采用对应汇编语言编写，基于goto逻辑实现
            kSwitchImplKind，对应ExecuteSwitchImpl函数，由C++编写，基于switch/case逻辑实现
            kComputedGotoImplKind，对应ExecuteGotoImpl函数，由C++编写，基于goto逻辑实现，不支持使用clang编译器
    ExecuteSwitchImpl函数：
        dex_pc，指向要执行的dex指令
        insns，代表方法的dex指令码数组
        在while循环中，遍历方法的dex指令码数组，借助switch/case，针对每一种dex指令进行处理，每种指令处理前，都有一个PREAMBLE宏
        最后，循环结束后，记录dex指令执行的位置并更新到shadow_frame中（SetDexPC函数）
    DoInvoke函数：
        模板函数，在上面循环中处理invoke-direct/static/super/virtual/interface等指令
        当前方法B执行invoke指令调用方法C，方法C所需的参数已经通过对应的指令存储到方法B的ShadowFrame对象中了
        模板参数type，指明调用类型，比如 kStatic、kDirect等
        模板参数is_range，如果该方法参数多于5个，则需要使用invoke-xxx-range这样的指令
        模板参数do_access_check，是否需要访问检查（检查是否有权限调用invoke指令的目标方法）
        函数参数inst，invoke指令对应的Instruction对象
        函数参数inst_data，invoke指令对应的参数
        函数参数result，存储方法C执行的结果
        找到方法C对应的对象，作为参数存储在方法B的ShadowFrame对象中
        FindMethodFromCode函数用于查找代表目标方法C对应的ArtMethod对象，根据不同调用类型（kStatic、kDirect、kVirtual、kSuper、kInterface）以找到对应的ArtMethod对象的关键代码
        后面和JIT有关
        最后调用DoCall函数
    DoCall函数中将调用方法C的参数存储到arg数组中，最后DoCallCommon函数
    DoCallCommon函数：
        CREATE_SHADOW_FRAME，创建方法C所需的ShadowFrame对象
        AssignRegister，从调用方法B的ShadowFrame对象中拷贝方法C所需的参数到C的ShadowFrame对象中
        准备方法C对应的ShadowFrame对象后，跳转到目标方法C
        如果处于调试或方法C不存在机器码，调用 ArtInterpreterToInterpreterBridge函数（解释执行的继续）
        如果可以用机器码方式执行方法C，则调用 ArtInterpreterToCompiledCodeBridge函数，从解释执行模式进入机器码执行模式
    ArtInterpreterToInterpreterBridge函数：
        PushShadowFrame，方法C对应的ShadowFrame对象入栈
        如果方法C为静态方法，则判断该方法所属类是否初始化过了，如果没有，则先初始化这个类
        如果不是JNI方法，则调用Execute执行该方法
        最后，PopShadowFrame，方法C对应的ShadowFrame出栈
    ArtInterpreterToCompiledCodeBridge函数：
        如果可以用机器码方式执行方法C，从解释执行模式进入机器码执行模式
        如果方法C为静态方法，则判断该方法所属类是否初始化过了，如果没有，则先初始化这个类
        JIT相关
        最后调用ArtMethod* C的Invoke函数
    ArtMethod::Invoke：
        args，方法C所需的参数，数组；result，存储方法C调用结果的对象；shorty，方法C的简短描述
        PushManagedStackFragment，栈操作
        have_quick_code，再次判断方法C是否存在机器码
        if have，如果是非静态函数，则调用art_quick_invoke_stub函数，否则调用art_quick_invoke_static_stub函数（函数指针）
        和HDeoptimize有关
        最后，PopManagedStackFragment，出栈
    art_quick_invoke_stub函数：
        由汇编编写
        准备好栈空间，将机器码函数所需参数拷贝到栈上，EAX寄存器存储着目标方法对应的ArtMethod对象
        然后通过call指令跳转到该ArtMethod对象的机器码入口，以机器码方式执行这个方法
        该方法的机器码执行完后将返回到art_quick_invoke_stub执行，此时，art_quick_invoke_stub将把执行结果存储到result位置
        当调用流程从art_quick_invoke_stub返回后，解释执行的处理逻辑就得到了方法C机器码执行的结果
    
    调用栈的管理和遍历：
        Thread管理ShadowFrame、ManagedStack等
        ManagedStack的管理：
            ART虚拟机的三个不同的执行层：
                虚拟机自身代码逻辑的运行，artQuickToInterpreterBridge函数运行的是虚拟机自己的代码，称为虚拟机执行层
                Java方法的解释执行，当artQuickToInterpreterBridge准备好一个代表调用栈的ShadowFrame对象，然后进入EnterInterpreterFromEntryPoint后，目标Java方法就将以解释方式运行在解释执行层
                Java方法的机器码执行，Java方法将被编译成机器码，该方法以机器码方式运行，对应为机器码执行层
            这三个执行层穿插进行，虚拟机一开始是运行在虚拟机执行层，然后根据目标Java方法的情况，进入机器码执行层或解释执行层
            机器码执行层不能直接转入解释执行层，必须借助虚拟机执行层，反之亦然
            从虚拟机执行层进入机器码执行层或从虚拟机执行层进入解释执行层的过程叫transition（过渡或转变）
            ManagedStack类用来管理三个执行层的交织
            外部操作ManagedStack对象栈的入口函数在Thread类中：
                PushManagedStackFragment、PopManagedStackFragment，成对出现
                从虚拟机执行层进入机器码执行层或解释执行层时，虚拟机创建一个ManagedStack对象，调用Thread PushManagedStackFragment函数，当目标函数返回时PopManagedStackFragment被调用
                PushShadowFrame、PopShadowFrame，专供解释执行层使用，代表某个被调用的Java方法所用的栈帧
                机器码执行层和虚拟机执行层本身不需要单独的栈管理对象
        回溯调用栈：
            ART虚拟机中StackVisitor类的WalkStack成员函数，就可以用来向上回溯调用栈
            WalkStack函数：
                入参include_transitions，表示是否要遍历transition对应的ManagedStack，默认为false，异常处理过程中需要设为true
                遍历ManagedStack栈（Thread.GetManagedStack得到）：
                    cur_shadow_frame_不为空，可用来回溯解释执行层中的函数调用
                    cur_quick_frame_不为空，可用来回溯机器码执行层中的函数调用
                    两个多为空表示该对象位于虚拟机执行层
                    回溯机器码执行层，while循环中：
                        获取该方法对应的QuickOatMethodHeader信息
                        处理和内联函数有关
                        VisitFrame是StackVisitor类的虚成员函数，由其派生类实现，调用这个函数时表示已经解析得到一个栈帧，返回值表示是否继续向上遍历调用栈
                        一个函数的栈帧访问完毕，接下来定位它的调用者的栈帧，在当前栈帧顶部位置加上一个该栈帧的大小即可定位到调用者的栈帧顶部位置
                        FillCalleeSaves，保存上下文信息，做异常处理时可用到
                        cur_quick_frame_pc_，代表方法A的栈空间中，B函数返回后的pc值
                        cur_quick_frame_，指向方法A的栈空间
                        方法A对应的ArtMethod对象 cur_quick_frame_，不为空继续while循环
                    解释执行层的栈回溯：
                        do  while循环中调用VisitFrame虚函数
                    机器码执行层或解释执行层栈帧遍历完后，将进入虚拟机执行层，也是调用VisitFrame
                    
#### ART中的JIT

    Just-In-Time
    虚拟机对方法执行次数进行统计，当某方法执行次数达到一定阈值后（称为hot method），虚拟机会将hot method编译成本地机器码，方法后续将以机器码方式执行，这是JIT最常见的形式
    方法执行次数不多，但方法内部包含一个循环次数特别多的循环，当循环执行到一定次数后，JIT会被触发，方法被编译成机器码（不会等待该方法下次调用才执行，而是作为后续指令执行，这种处理方式需要将机器码执行所需的栈信息替换之前以解释执行的栈，也叫On Stack Replacement（OSR）技术）
    JIT编译也是使用OptimizingCompiler的
    编译角度看，ART中的JIT和AOT用到的技术没有区别，只不过编译的时机不同而已
    JIT模块相关类：
        Jit类，是ART JIT模块的门户，JIT模块提供的主要功能将借助该类的相关成员函数对外输出
        JitCodeCache类管理用来存储JIT编译结果的一段内存空间，当内存不够用时这部分内存空间可以被回收
        JitCompiler类用于处理JIT相关的编译，其内部用到的编译模块和dex2oat里AOT（Ahead-Of-Time，和JIT相对的编译）编译用到的编译模块同为OptimizingCompiler
        ProfilingInfo类用于管理一个方法的性能统计信息
    
    Jit类：
        Create函数用于创建Jit对象：
            LoadCompiler用于加载编译相关的模块
            创建JitCodeCache对象，code_cache_指向
            设置几个Jit成员变量：
                hot_method_threshold_，hot method阈值，默认为10000，方法执行次数超过时，JIT将对该方法进行编译
                warm_method_threshold_，warm method阈值，默认为5000，方法执行次数超过时，JIT将为该方法生成性能统计文件并进行性能统计
                osr_method_threshold_，osr metho阈值，默认为2000，和OSR有关
                priority_thread_weight_，线程的阈值权重默认为5
                invoke_transition_weight_，默认值为10，解释执行与机器码执行切换的阈值
            LoadCompiler函数：
                LoadCompilerLibrary通过dlopen的方式加载 libart.so，从中取出jit_load、jit_compile_method等函数并赋值给Jit对应的成员变量
                jit_load_为Jit的成员函数，是一个函数指针，执行jit_load函数
                jit_load函数内部创建了JitCompiler对象，JitCompiler构造函数内部创建CompilerDriver
                CompilerDriver内部将创建OptimizingCompiler对象用于完成具体的编译工作
                jit_compile_method函数，对某个Java方法进行编译
        AddSamples函数用于对某个方法进行性能统计
        CompileMethod函数对方法method进行JIT编译
        MaybeDoOnStackReplacement函数判断是否可以进行OSR，后续将转入机器码执行模式
        code_cache_指向一个JitCodeCache对象，用于管理JIT所需的内存空间
        thread_pool_，线程池对象，内部通过工作线程来处理和JIT相关的工作，编译等
    
    JitCodeCache类：
        JitCodeCache提供一个存储空间，用来存放JIT编译的结果，当内存不足时，该部分空间可以被释放
        Create函数：
            在Jit的Create函数中被调用
            initial_capacity、max_capacity，表示JitCodeCache初始存储空间大小以及最大能拓展到多大的空间，默认为64KB、64MB
            generate_debug_info，JIT编译时是否生成调试信息
            MemMap::MapAnonymous，创建一个匿名内存映射对象，名称为data-code-cache，大小64MB，该内存有rwx权限
            RoundDown，对64MB内存映射对象进行拆分，前32MB对应一个内存映射对象，由data_map指向，后32MB对应一个新的内存映射对象，由code_map指向
            RemapAtEnd，分拆data_map所指向的64MB空间，前32MB空间还由code_map指向，后32MB空间则放到一个新的名为jit-code-cache，由code_map指向，rwx权限
            最后创建JitCodeCache对象
            JitCodeCache的构造函数中使用dlmalloc分配内存，code_map_、data_map_来保存上述两个内存映射对象
        CommitCode函数：
            JIT模块编译完一个方法后，该方法的编译结果将通过调用JitCodeCache的CommitCode函数以存放到对应的存储空间中
            CommitCodeInternal，提交内容保存，需要存储的内容包括vmap_table、代表机器码内容的code数组
            GarbageCollectCache，如果提交的内容不能保存，则需要先尝试回收一部分内容，然后再次提交（CommitCodeInternal）
        CommitCodeInternal函数：
            和dex2oat里保存的信息一样，借助OatQuickMethodHeader来处理
            总的存储空间大小为code_size加上OatQuickMethodHeader的大小
            WaitForPotentialCollectionToComplete，如果其他线程正再JIT GC，则等待其他线程处理完毕
            AllocateCode，从code_mspace_空间中分配内存
            拷贝机器码的内容到code_ptr部分
            借助placement new构造一个OatQuickMethodHeader对象
            存储的结果和对应的method信息需要保存到JitCodeCache对应的成员变量中
            method_code_map_，保存了机器码入口地址以及对应的ArtMethod对象
            osr_code_map_，如果是osr的处理，则再保存一份ArtMethod对象及机器码入口地址的信息
            UpdateMethodsCode，设置ArtMethod的机器码入口地址为对应的地址值
        GarbageCollectionCache函数：
            用于从JitCodeCache中回收内存
            IncreaseCodeCacheCapacity，如果不能回收相关内存空间的话，就只能增加内存大小，但不能超过所设置的最大空间大小
            WaitForPotentialCollectionToComplete，如果其他线程正在JIT GC，则等待其他线程处理完毕
            live_bitmap_，是一个位图对象，成员变量
            collection_in_progress_，设置JIT GC标志位，其他要做JIT GC的线程要先检查这个变量
            ShouldDoFullCollection，返回值表示是否需要做Full GC，如果当前内存使用量达到最大允许值，或者上一次使用Partial GC，则返回true，其他情况false。对JIT GC来说，Full GC是指回收data_mspace_和code_mspace的空间，Partial GC专指回收code_mspace_的空间
            DoCollection，回收
        DoCollection函数：
            完成最终的GC任务，对JIT而言，算法就是Mark-Sweep，标记-清除法
            回收data_mspace_空间
            遍历method_code_map_：
                GetLiveBitmap，返回上面函数里提到的live_bitmap_成员变量
                设置live_bitmap_对应位的值为1，表示该位对应的code_mspace_空间上存储着一个方法的编译结果
            清空osr_code_map_容器
            MarkCompiledCodeOnThreadStacks，对应Mark处理
            RemoveUnmarkedCode，对应Sweep处理
        MarkCompiledCodeOnThreadStacks函数：
            MarkCodeClosure，JIT GC里Mark的实现，就是设置一个闭包对象，然后要求虚拟机进程里其他线程在某个时刻执行它。
                这个时刻在虚拟机中叫check point，不论方法是否以机器码方式运行，它都不能脱离虚拟机的管控，check point即是管控的一种方式，
                它要求线程在忙自己工作的时候还有检查一下虚拟机里发生的事情。此处JIT GC将设置一个标志位，当其他线程运行到check point时发现这个标志位，既而会执行MarkCodeClosure
            GetThreadList()->RunCheckpoint()
            本线程等待其他线程处理完Mark的操作
        MarkCodeClosure类：
            将遍历live_bitmap_，判断其中是否有方法包含在调用线程的栈中（判断JitCodeCache里有哪些方法正在被使用）
            基类为Closure，是一个纯虚类，子类要是其中的Run函数
            Run函数，其他线程会调用它，主要使用MarkCodeVisitor.WalkStack
        RemoveUnmarkedCode函数：
            遍历method_code_map_：
                Test，如果live_bitmap_对应位有值，则表明该位对应的方法正在被使用
                FreeCode，若没被使用，调用dlmalloc相关函数回收method对应的空间
        Jit::CanInvokeCompiledCode函数：
            判断一个方法是否存在JIT的编译结果
            ContainsPc，调用JitCodeCache的ContainsPc函数，输入参数为ArtMethod的机器码入口地址
            ContainsPc就是判断ptr是否位于code_map_所覆盖的内存区域
    
    JIT阈值控制与处理：
        包含两部分：
            性能统计埋点，解释执行某Java方法时在一些JIT关注的地方埋点以更新统计信息。埋点，指在关键地方调用JIT模块的性能统计相关的成员函数，AddSamples函数
            Jit AddSamples检查当前所执行方法的性能统计信息，根据不同阈值的设置进行不同的处理
        
        性能埋点统计：
            EnterInterpreterFromEntryPoint中：
                NotifyCompiledCodeToInterpreterTransition通知JIT模块表示本次调用是一个从机器码到解释执行的转换，其内部调用AddSamples最终的计算的执行次数为1*Jit invoke_transition_weight_（默认为10）
                最后调用Execute
            Execute函数：
                MethodEntered，内部调用AddSamples，性能统计次数加1
            ArtInterpreterToCompiledCodeBridge函数：
                NotifyInterpreterToCompiledCodeTransition，内部调用AddSamples，对caller所在的方法（正在解释执行的方法）进行性能统计，每调用一次，性能计数增加Jit invoke_transition_weight_次
            ExecuteSwitchImpl函数：
                是switch/case方式实现dex指令解释执行的核心函数，在某些指令的执行过程中将进行性能统计计数
                HOTNESS_UPDATE，宏，内部调用AddSamples，如果跳转的目标是往回跳（存在循环），则进行性能统计，次数加1
                BRANCH_INSTRUMENTATION 宏，和OSR有关
        
        AddSamples函数：
            入参，method代表需要进行性能统计的方法，count代表此次统计应增加的次数，with_backedges表示是否针对循环，在HOTNESS_COUNT中被设置为true
            GetCounter，返回ArtMethod hotness_count_成员变量
            ShouldUsePriorityThreadWeight，检查进程的状态是否为kProcessStateJankPerceptible（应用处于前台），卡顿可感知（VMRuntime updateProcessState函数设置状态）；
                    判断调用线程是否为敏感线程（sensitive Thread，由VMRuntime registerSensitiveThread设置）；这两个条件都为true时，返回true
            new_count为计算后的统计次数
            如果进入warm方法阈值区域，先为该方法创建ProfilingInfo信息，后续编译时使用
            如果进入hot 方法阈值区域，并且该方法不存在机器码，给线程池添加一个JitCompileTask任务，任务类型为编译（kCompile）
            如果进入osr 方法阈值区域，并且属于循环类处理，则添加一个JitCompileTask任务，类型为kCompileOsr

    OSR的处理：
        OSR 是 On Stack Replacement的缩写
        JIT中，是方法执行半道过程中，从解释执行模式切换到机器码执行模式的关键技术
        MaybeDoOnStackReplacement函数：
            入参，dex_pc表示当前执行的dex指令位置，dex_pc_offset表示goto等指令的跳转目标位置相对dex_pc的偏移量
            为机器码执行准备对应的参数
            LookupOsrMethodHeader，检查JitQuickMethodHeader osr_code_map_中是否已经存在本方法的编译结果
            从机器码编译信息里找到目标dex指令对应的StackMap信息（dex_pc+dex_pc_offset）
            malloc分配一块内存，将存储机器码执行时所需的参数等信息
            栈顶位置存储ArtMethod对象
            循环从dex指令及虚拟寄存器中拷贝参数到memory指定位置
            native_pc处为目标dex指令对应的机器码
            PushManagedStackFragment压入一个代表机器码执行的ManagedStack对象
            art_quick_osr_stub 是一个汇报trampoline code
            PopManagedStackFragment，执行完毕 ManagedStack出栈
        art_quick_osr_stub ：
            入参，memory存储着对应机器码执行时所需的参数，由malloc从堆上分配的，art_quick_osr_stub 会从memory中将参数拷贝到栈上
            frame_size，机器码对应的栈大小
            native_pc，目标机器码的位置，art_quick_osr_stub 利用jmp指令跳转到这个位置去执行
        在OSR之前，方法以解释模式执行
        在OSR之后，方法以机器码方式执行，直到方法返回
        方法返回后，执行逻辑依然处于解释执行的处理流程中
    
#### HDeoptimize的处理

    HDeoptimize是ART HInstruction IR 中的一种
    将导致指令从机器码执行模式切换进入解释执行模式
    相比将指令编译优化为机器码后再执行的做法而言，它是一种反优化的手段，所以叫Deoptimize
    从机器码执行模式强制跳转到解释执行模式的情况：
        循环时数组长度小于实际操作长度（操作前3个成员，但数组长度小于3）时跳转，大于3时一切正常，依然以机器码执行
        索引越界后需要抛出AOB异常，而代码中又无截获异常的处理时将for循环及后续代码整个转入解释执行模式来处理
        即数组越界异常时，未做try/catch等处理的话，就会触发HDeoptimize的处理，否则不会触发
    HBoundsCheckElimination优化中有HDeoptimize IR 对象的创建等
    
    VisitDeoptimize相关：
        CodeGenerator 针对HDeoptimize IR的处理
        InstructionCodeGeneratorX86::VisitDeoptimize：
            入参为HDeoptimize*
            创建一个DeoptimiztionSlowPathX86对象
            GenerateTestAndBranch生成一些机器码，这些机器码在执行时会判断是否转入上面的Deoptimiztion处理流程
        DeoptimiztionSlowPathX86类：
            派生自SlowPathCode类
            EmitNativeCode函数中，InvokeRuntime将生成调用art_quick_deoptimize_from_compiled_code（由汇编代码编写的跳转函数）的机器码
            art_quick_deoptimize_from_compiled_code代码中转入artDeoptimizeCompiledCode函数
        artDeoptimizeCompiledCode函数：
            PushDeoptimizationContext函数将构造一个DeoptimizationContextRecord对象，设置Thread tlsPtr_的deoptimization_context_stack指向这个对象
            QuickExceptionHandler的三个主函数：
                构造函数
                DeoptimizeSingleFrame
                DoLongJump
    
    QuickExceptionHandler相关：
        该类主要用途与异常投递有关
        构造函数；
            GetLongJumpContext为Thread的函数，将创建一个Context对象，用于生产机器码
            handler_quick_frame_，执行栈中的某个位置，解释执行的参数将放在该位置上
            handler_quick_frame_pc_，该变量描述异常处理的跳转目标，就是跳转地址
            其他变量赋值
        DeoptimizeSingleFrame：
            用于从机器码执行的栈帧中提取参数到一个ShadowFrame对象里，ShadowFrame将作为后续解释执行的栈
            DeoptimizeStackVisitor.WalkStack，遍历调用栈，具体见VisitFrame函数
            deopt_method为返回的被反优化的方法
            InvalidateCompiledCodeFor，如果该方法存在JIT编译结果，则将它从JitCodeCache中清除，并设置deopt_method对象的机器码入口地址为art_quick_to_interpreter_bridge
            UpdateMethodsCode，更新deopt_method对象的机器码入口地址为art_quick_to_interpreter_bridge
            handler_quick_frame_pc_，它的值指向art_quick_to_interpreter_bridge
            
            QuickExceptionHandler的VisitFrame函数：
                SetHandlerQuickFrame设置QuickExceptionHandler的handler_quick_frame_成员，指向反优化方法的调用者对应的栈帧
                从机器码进来，不存在ShadowFrame，需要创建一个ShadowFrame
                GetDexPc将根据当前机器码指令的位置转换为dex指令码的位置，并将该值设置到ShadowFrame对象的dex_pc_成员变量中
                HandleOptimizingDeoptimization，用于拷贝参数到ShadowFrame中
                SetHandlerQuickArg0，设置QuickExceptionHandler handler_quick_arg0_成员变量为代表反优化的method对象
                
        DoLongJump函数：
            跳转到目标位置，通过内嵌汇编代码的方式来实现
            SetSP，内部调用SetGPRS，含义是设置ESP寄存器位置为handler_quick_frame_
            SetPC，内部设置成员变量eip_为handler_quick_frame_pc_
            setArg0，内部调用SetGPRS，设置EAX寄存器的值
            DoLongJump，设置上述寄存器的值（上面只是将要设置的信息存到对应的成员变量中）
        DoLongJump的结果是程序将转入art_quick_to_interpreter_bridge处执行，就是重新进入解释执行模式
    
    解释执行中关于Deoptimize的处理：
        artQuickToInterpreterBridge函数：
            Deoptimize有关的操作
            Deoptimize情况时，调用EnterInterpreterFromDeoptimize函数，非Deoptimize时，调用EnterInterpreterFromEntryPoint函数
            
            EnterInterpreterFromDeoptimize函数：
                在while循环中，沿着调用栈向调用者方向逐个以解释方式执行
                解释执行依然是由Execute处理
                如果调用者也是解释执行，则存在调用者对应的ShadowFrame
                
#### Instrumentation介绍

    Instrumentation是ART虚拟机内及其重要的一个工具类，是ART虚拟机提供Java代码调试（Debug）、跟踪（Trace）、性能采样（Profiling）等功能的核心实现
    相关类：
        InstrumentationListener，接口类，函数接口向外界反馈虚拟机内部的执行情况（很多函数只能在解释执行模式下才能被调用）：
            MethodEntered函数，将在真正执行一个方法前被调用
            DexPcMoved函数，在解释执行一条指令时调用
            FieldRead、FieldWrite函数，在解释执行模式下处理读写成员变量相关的指令时被调用
            ExceptionCaught，在异常被捕获时调用
            Branch，在解释执行和分支跳转相关的指令时被调用
            InvokeVirtualOrInterface，在调用virtual或interface方法时被调用
        Trace、Dbg类，是ART虚拟机里负责跟踪和调试功能的类：
            Trace直接实现了InstrumentationListener
            Dbg 自身功能独立，内部借助DebugInstrumentationListener来处理和虚拟机执行有关的信息
        Instrumentation是上述功能实现的核心
        InstrumentationStackFrame是用于Instrumentation的栈帧，Thread tlsPtr_instrumentation_stack维护一个InstrumentationStackFrame对象的双向队列
    
    MethodEnterEvent和MethodExitEvent：
        MethodEnterEvent在解释执行流程中的Execute函数内被调用，内部调用InstrumentationListener的MethodEntered函数
        外界调用Instrumentation的MethodEnteredEvent（XXXEvent），向Instrumentation报告一个方法进入的事件
        MethodExitEvent的调用和处理RETURN相关的dex指令有关
        ExecuteSwitchImpl函数中将调用MethodExitEvent
    
    DexPcMovedEvent：
        ExecuteSwitchImpl函数中switch/case处理指令前的PREAMBLE宏中会调用DexPcMovedEvent（每执行一条指令都会调用）
    其他XXXEvent等函数也在处理对应指令时被调用
    
#### 异常投递和处理
    
    解释执行模式下，THROW指令的处理方法：
        ExecuteSwitchImpl函数中：
            case Instruction::THROW:{   //抛异常
                PREAMBLE();
                ThrowNullPointerException，Throw抛出的异常对象为空，则重新抛出一个空指针
                HANDLE_PENDING_EXCEPTION，检查是否有异常发生，并做对应处理
            }
        抛异常：
            构造对应的异常对象，THROW指令抛出的异常对象为变量exception，是由其他代码逻辑创建好的，代码中对exception进行了判断，若为空指针，则调用ThrowNullPointerException
            抛异常，对ART虚拟机而言，抛异常就是调用Thread类的SetException函数，将把异常对象赋值给 tlsPtr_exception成员变量
        处理异常：
            先检查是否有异常发生，通过判断Thread tlsPtr_exception 是否为空指针来实现的
            然后才是处理异常
    
    抛异常：
        ThrowNullPointerException函数内调用ThrowException
        ThrowException函数：
            入参，exception_descriptor 所抛异常的类名字符串，referrer 和抛出此异常有关的类，fmt、args 用于构造异常所携带的字符串信息
            AddReferrerLocation，提取referrer所属Dex文件的文件路径名到msg中
            ThrowNewException，Thread的ThrowNewException函数
        ThrowNewException函数内部调用ThrowNewWrappedException
        ThrowNewWrappedException函数：
            构造一个由exception_class_descriptor字符串指定类型的异常对象
            针对该对象调用其所属类的构造函数
            
            ClearException，先设置tlsPtr_exception成员变量为空指针
            找到所抛异常的类对象，FindClass
            创建该类的一个对象，即实际要抛出的异常对象，exception(...)
            构造Java Throwable成员变量detailMessage要用的jstring对象，以及确定调用哪个构造函数（信息由signature表示），AllocFromModifiedUtf8
            FindDeclaredDirectMethod，找到合适的构造函数
            InvokeWithJValues，调用异常类的构造函数
            把异常对象赋值给 tlsPtr_exception成员变量
            SetException
        Java Exception的基类是Throwable，其构造函数中调用fillInStackTrace函数构造堆栈信息
        fillInStackTrace函数内又调用nativeFillInStackTrace函数，遍历调用栈
        nativeFillInStackTrace为jni函数，由java_lang_Throwable.cc的Throwable_nativeFillInStackTrace实现，其内部就是调用Thread CreateInternalStackTrace来完成调用栈的回溯和信息获取
        
    异常处理：
        解释执行层中的异常处理：
            把异常对象赋值给 tlsPtr_exception成员变量后异常投递工作就算完成了
            以switch/case方式解释执行的代码中，HANDLE_PENDING_EXCEPTION宏用于判断是否有异常发生并处理它
            HANDLE_PENDING_EXCEPTION宏：
                FindNextInstructionFollowingException，用于从当前正在执行的方法里找到对应的catch处理语句，如果能处理所抛异常，则返回异常处理对应的dex指令码位置
                如果本方法无法处理这个异常，则要退出整个方法的执行
                return JValue，退出本方法的执行，调用者将继续检查并处理异常
                如果本方法catch住了所抛出的异常，则找到对应的处理指令
            
                即FindNextInstructionFollowingException判断本方法自身是否能处理异常：
                    如果可以处理异常，则更新下一条待执行的指令为对应catch处的dex指令
                    否则将直接退出本方法的执行，该异常由调用者继续处理
            FindNextInstructionFollowingException函数：
                调用ArtMethod的FindCatchBlock
                MethodUnwindEvent，如果本方法无法处理这个异常，则表示要进行栈回溯，此时将触发Instrumentation的MethodUnwindEvent函数
            
            多个函数嵌套调用，在解释执行层抛出异常，若当前方法D无法处理，当前方法D将从Execute函数中返回，
                进入上一层方法C，方法C通过处理INVOKE指令来调用方法D，方法D返回后方法C通过HANDLE_PENDING_EXCEPTION宏发现有异常存在，
                由于C依然无法处理此异常，C也退出自己的执行，
                沿着调用流程回溯，执行流程将返回到art_quick_to_interpreter_bridge函数（虚拟机执行层？），该函数最后，有一个RETURN_ON_DELIVER_PENDING_EXCEPTION宏
                RETURN_ON_DELIVER_PENDING_EXCEPTION宏将检查是否有异常发生并进行对应的处理
            RETURN_ON_DELIVER_PENDING_EXCEPTION宏：
                检查Thread tlsPtr_exception成员变量是否为空，如果不为空，则跳转到DELIVER_PENDING_EXCEPTION宏处
            DELIVER_PENDING_EXCEPTION宏：
                异常处理宏
                SETUP_SAVE_ALL_CALLEE_SAVE_FRAME该宏内部将设置tlsPtr_ managed_stack top_quick_frame_的值
                调用artDeliverPendingExceptionFromCode函数
        
        虚拟机执行层中的异常处理：
            artDeliverPendingExceptionFromCode函数位于虚拟机执行层
            artDeliverPendingExceptionFromCode函数内部调用了Thread的QuickDeliverException函数
            QuickDeliverException函数：
                判断是否为Deoptimize相关的异常
                QuickExceptionHandler exception_handler
                exception_handler.FindCatch，将找到异常处理处的指令位置（pc）
                exception_handler.DoLongJump，跳转到异常处理对应的地址
            FindCatch函数：
                CatchBlockStackVisitor.WalkStack，关键类
                SetException
                如果异常处理的代码位于机器码中，则再补充设置一些信息，这部分内容和机器码编译有一些关系
            CatchBlockStackVisitor.VisitFrame函数；
                用于找到对应的异常处理者
                GetMethod，获取当前正在访问的方法method
                如果method为空，表示当前所访问的方法为虚拟机执行层
                GetNextMethodAndDexPc函数内部会做WalkStack进行栈回溯操作，找到紧挨着当前虚拟机执行层中的上一个方法
                HandleTryItems，从method中catch语句中找到是否有能处理该异常的地方
            线程中存在一个未捕获异常：
                DetachCurrentThread内部将调用当前线程的Destroy方法
                Thread的Destroy方法中，会调用HandleUncaughtExceptions函数
                最终调用为该线程设置的UncaughtExceptionHandler对象
        
        机器码执行层中的异常处理：
            异常投递，只需将被抛出的异常对象设置到Thread tlsPtr_exception成员变量中即可
            构造异常对象时将回溯调用栈，把调用栈的信息保存到这个被抛出的异常对象中
            
            异常处理，解释执行层和机器码执行层可以捕获异常，但虚拟机执行层不能捕获异常，它只是辅助完成解释执行层和机器码执行层的切换
            如果没有捕获异常，栈回溯的结果是将返回到虚拟机执行层，就好像所有Java方法都执行完了一样，此后，虚拟机将退出当前线程，退出过程中会调用UncaughtExceptionHandler来处理未捕获的异常
            
            机器码中，Throw指令对应为HThrow IR
            在code_generator_x86中将HThrow IR转化为调用quick_entrypoints_X86.S中的art_quick_deliver_exception函数
            art_quick_deliver_exception函数又调用C++层artDeliverExceptionFromCode函数
            artDeliverExceptionFromCode函数位于虚拟机执行层，需要设置tlsPtr_ managed_stack top_quick_frame_
            artDeliverExceptionFromCode函数：
                SetException，设置 tlsPtr_exception
                QuickDeliverException，Thread的函数，同上步骤
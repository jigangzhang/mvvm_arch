
## dex2oat

    oatdump工具可输出oat文件内容，dexdump工具输出指令码？？
    用于将dex字节码编译成本地机器码，一般会生成连个文件，.oat 和 .art文件
    ART编译目标有两大类：
        针对系统核心库jar文件的编译，得到boot image，系统核心库位于 /system/framework 目录下，如 core-oj.jar等
        针对应用程序apk或相关jar包（如system_server进程的jar包）的编译，得到 app image，app image就是对apk或jar 包中的classes.dex的编译
    编译app image时必须用到boot image文件（Object类就是在核心库里定义的）
    
    dex2oat支持两种编译模式：
        host编译，指在主机（PC或编译服务器）上将系统中的jar包、相关apk文件里的dex编译成目标设备机器码，一般用于设备生产商、手机制造商，交叉编译（在Intel上编译ARM）
        target编译，指目标设备运行中执行dex2oat将设备上的jar包、apk文件转换成机器码
    设备生产商编译Android系统时会使用host编译生成boot镜像和一些系统级app镜像
    7.0之后，系统针对不同应用场景设置不同的编译过滤器（Compiler Filter）用于控制dex2oat的 工作力度
    编译过滤器：
        verify-profile，只对包含在性能采集文件里的类进行校验，并且只编译其中的jni函数，profile是指程序运行时对函数调用次数进行统计，达到阈值后当做热点函数编译成本地机器码
        interpret-only，只对dex文件进行校验，并且只编译jni函数
        speed-profile，对包含在性能采集文件里的类进行检验和编译
        speed，对dex文件进行检验和编译以最大能力提升代码执行速度
    应用场景：
        pm.dexopt.install，应用程序安装场景，对应编译过滤器为interpret-only，减少等待安装时间
        pm.dexopt.bg-dexopt，应用安装运行一段时间后系统根据应用的性能采集文件对热点函数进行编译，对应源码 BackgroundDexOptService.java
        pm.dexopt.core-app，针对系统关键应用启动或安装的处理
    
    dex2oat编译 boot image时的输入选项：
        dex2oat: option[0]=--image=/data/.../boot.art
        --dex-file，指定待编译的jar包
        --image，指定编译输出的.art文件的位置
        --oat-file，指定编译输出的.oat文件的位置
        --image-classes，指定一个文件，该文件包含需要包含到boot镜像中的类的类名
        --compiled-classes，指定一个文件，该文件包含需要编译到boot镜像中的类的类名
        --base，指定一个内存基地址
        --instruction-set、--instruction-set-features，指定机器码运行的CPU架构名和CPU支持的特性
        --runtime-args，选项及紧接其后的参数为创建runtime对象时使用的参数
        --compiler-filter，设置编译过滤器，如果不设置的话，默认为speed
        --multi-image，控制生成多个输出文件，针对每一个--dex-file输入文件都生成对应的art和oat文件
    dex2oat过程：
        1、解析参数，ParseArgs函数
        2、打开输入文件，OpenFile函数
        3、准备环境，Setup函数
        4、编译，编译镜像文件 CompileImage（指.art文件，有boot image和app image两大类）；编译app，CompileApp（不生成art文件，内容与前者差不多）
        5、清理工作，Shutdown函数


#### ParseArgs介绍
    
    输入选项解析
    dex2oat的输入参数和成员变量之间的关系：
        compiler_options_       保存dex2oat编译时所需的编译选项，如CompilerFilter取值等
        dex_filenames_          --dex-file      字符串数组
        image_filenames_        --image         编译输出的art镜像文件名
        oat_filenames_          --oat-file      编译输出的oat文件名
        dex_locations_          --dex-location  若未指定，内容与dex_filenames_一样
        image_classes_filename_ --image-classes 需要预加载的类，取值为/system/etc/preloaded-classes
        compiled_classes_filename_ --compiled-classes   需要与编译的类，/system/etc/compiled-classes
        image_base_             --base
        instruction_set_        --instruction-set       kX86
        instruction_set_features_ --instruction-set-features 特性
        runtime_args_           --runtime-arg       存储紧接其后的字符串  -Xms-64m等
        key_value_store_        存储参数和值对    
            
    CompilerOptions类介绍：
        成员变量用于描述编译相关的控制选项
        compiler_filter_，Filter类型，对应编译过滤器
        按Java方法对应dex字节码的数量，分为 huge（10000字节码）、large（6000）、small（60）、tiny（20），如 huge_method_threshold_(kDefaultHugeMethodThreshold)
        inline_depth_limit_、inline_max_code_units，编译内联方法有关
    ParseArgs调用CompilerOptions的ParseCompilerOption函数解析dex2oat命令行里和编译相关的选项，设置对应的成员变量
    
    ProcessOptions函数：
    InsertCompileOptions函数：

#### OpenFile介绍
    
    创建输出的.oat文件
    流程：
        检查dex_filenames_中的输入文件是否存在，不存在则将其从dex_filenames_中去掉
        遍历oat_filenames_（输出的oat文件路径），创建对应路径的文件对象（File，art封装的），存到oat_files_ vector中
    
#### Setup介绍
    
    第一段：
        MemMap::Init()
        PrepareImageClasses、PrepareCompiledClasses、PrepareCompiledMethods
        
        ClassReference：
            是类型别名，类型 pair<const DexFile*, uint32_t>，第一个Dex文件，第二个表示类的信息在Dex文件中类信息表（class_defs）里的索引（class_idx）
        MethodReference：结构体，dex_file 代表Dex文件，dex_method_index java方法在Dex文件里method_ids中的索引
        DexFileReference：结构体，dex_file 代表Dex文件，index 含义同MethodReference，指Java方法
        
        VerifiedMethod类：
            代表一个校验通过的Java方法
            ART中校验Java方法时，每校验一个Java方法时都会项创建一个MethodVerifier对象，如果该Java方法校验通过，dex2oat就会调用VerifiedMethod Create函数创建一个VerifiedMethod对象
            Java方法中有invoke-virtual或invoke-interface指令，在Create函数中，需要进行函数调用去虚拟化的工作，由GenerateDevirtMap函数完成
            
        QuickCompilationCallback：
            类校验时外界可传递一个回调接口对象，
                类校验失败时，该接口对象的ClassRejected函数将被调用，类Java方法校验通过时，该接口对象的MethodVerified函数将被调用
            回调接口类为 CompilerCallbacks，是虚基类，QuickCompilationCallback是其派生类
            QuickCompilationCallback内部有两个成员变量：
                verification_results_，保存类校验信息
                method_inliner_map_，和dex2oat中对Java方法进行内联优化有关
    
    第二段：
        为创建编译时使用的Runtime对象准备参数
        dex2oat用的不是完整虚拟机，dex2oat中runtime只有Init会被调用，Start函数不会被调用，叫unstarted runtime
        关键函数：CreateOatWriters、AddDexFileSources
        
        关键类：
            ElfWriter和ElfBuilder：
                ElfWriter是ART中用于往ELF文件中写入相关信息的工具类
                ElfWriter是一个虚基类，ART里ElfWriter的实现类是ElfWriterQuick（模板类），模板参数为ElfTypes32或ElfTypes64（ELF文件数据结构）
                ElfWriterQuick对象是由CreateElfWriterQuick函数创建的
                ElfWriterQuick的成员变量：
                    ElfBuilder，构造ELF文件各个section的辅助类，其成员变量是各种Section
                    OutputStream，ART中输出流相关类的基类
                        Section为OutputStream的子类，实现了对ELF某个Section内容进行写入的操作
            OatWriter：
                用于输出Oat相关信息的工具类
        
        OAT和ELF的关系：
            oat文件是经过谷歌定制的ELF文件
            readelf工具查看内容，如：readelf -hl
        
        CreateOatWriters函数：
            将创建ElfWriter和OatWriter对象
            遍历oat_files_（Oat文件File对象），
            创建和oat_file对应的ElfWriter对象（CreateElfWriterQuick），
            启动创建的ElfWriter对象，构造一个OatWriter对象，添加到oat_writers_数组中
            ElfWriter和OatWriter是通过在数组中的索引来关联的
        AddDexFileSources函数：
            遍历dex_filenames_，将输入的dex文件和代表oat文件的OatWriter对象关联起来（OatWriter的AddDexFileSource函数）
            AddDexFileSource函数：
                OpenAndReadMagic函数，打开filename指定的文件，返回文件描述符（ScopedFd，辅助类，在实例对象生命结束时自动关闭文件）
                IsZipMagic函数，jar包实际为ZIP压缩文件，打开文件是打开ZIP文件，真实目的是拿到其中的classes.dex项
                AddZippedDexFilesSource函数：
                    读取jar包中所有Dex项，多个Dex项，第一个为classes.dex，其后为classes2.dex，以此类推
                    从zip_archive（打开的jar包）中搜索指定名称的压缩项（Find函数，返回ZipEntry类型）
                    zipped_dex_files_成员变量保存jar包中对应的dex项（entry）
                    zipped_dex_file_locations_用于存储dex项的路径信息（由jar包路径信息处理而来），如：/system/framework/xxx.jar（第一个dex），/system/framework/xxx.jar:classes2.dex（第二个dex）
                    oat_dex_files_，数组，元素类型为OatDexFile，OatDexFile保存了dex项的路径名和dex项的源（DexFileSource）
    
    第三段：
        （为boot.oat文件对应的OatWriter准备数据）
         rodata_，Dex2Oat的成员变量，与ELF的.rodata section有关
         遍历oat_writers_数组：
            调用ElfWriter的StartRoData函数返回ELFBuilder的.rodata Section对象
            WriteAndOpenDexFiles，打开输入的dex项，将这些dex项的信息写入到oat文件中
            opened_dex_files，数组，保存dex相关
         赋值opened_dex_files_的内容给dex_files_数组
         
         OAT文件格式：
            oat文件定制的部分在于将oat的信息存储在.rodata和.text section中
            oat文件实际上是以ELF文件格式的形式封装了oat的信息，oat信息是以oat格式来组织的
            oat文件格式是指剥离ELF的封装，oat文件所含oat信息的组织方式
            oat文件格式：
                1、头结构，由OatHeader类表示
                2、OatDexFile区域，可存储多个OatDexFile项，每一个dex项对应一个OatDexFile对象
                3、DexFile区域，包含一到多个DexFile项（一个OatDexFile项对应一个DexFile项），一个DexFile项包含jar包中对应dex项的全部内容
                4、TypeLookup区域，包含一到多个TypeLookupTable，TypeLookupTable在oat文件中相对OatHeader的位置由OatDexFile的lookup_table_offset_指明
                    一个dex项对应一个TypeLookupTable
                5、ClassOffsets区域，每一个Dex文件对应一个ClassOffsets表，每个ClassOffsets表包含该Dex项里所有OatClass项在oat文件里相对OatHeader的位置信息
                6、OatClass区域
            oat文件对内容有字节对齐的要求
            
            上述文件格式信息存储在ELF的 .rodata section中
            
            OatHeader类：
            OatDexFile类：
            OatClass类；
                代表dex文件中的一个类
                offset_，在oat文件中OatClass区域中某个OatClass信息相对于OatHeader的位置偏移量
                compiled_methods_，数组，元素CompiledMethod 代表一个Java方法经dex2oat编译处理后的结果
                status_，代表类状态
                type_，枚举变量，表示类中的Java方法是否经过编译等
         
         WriteAndOpenDexFiles函数：
            InitOatHeader函数创建OatHeader结构体并设置其中的内容
            InitOatDexFiles函数用于计算各个OatDexFile的大小及它们在oat文件里的偏移量
            WriteDexFiles函数写入Dex文件信息
            WriteOatDexFiles函数将OatDexFile信息写入oat文件的OatDexFile区域
            OpenDexFiles函数：
                重点函数，调用这个函数时jar包里的dex项已经写入oat文件对应区域了
                遍历oat_dex_files_：
                    获取oat DexFile区域中的每一个dex文件内容
                    调用DexFile类的Open函数打开这些Dex文件，返回DexFile*，将其存储在dex_files_数组中
                更新输入参数
            WriteTypeLookupTables函数找到每个Dex文件对应的TypeLookupTable的位置，在这个位置上创建该Dex文件的TypeLookupTable
        主要点是Oat文件各个区域所包含的大体内容
        
    第四段：
        若编译boot镜像：
            为创建的Runtime对象设置boot类的来源
            CreateRuntime函数，创建art runtime对象，只做了Init操作，没有执行Start函数，为unstarted runtime
            初始化其他模块，Thread、ClassLinker等
            RegisterDexFile函数，往class_linker中注册dex文件对象和对应的class_loader_，boot类加载器时class_loader_为空
        创建一个虚拟机，但虚拟机不完整，只用于编译的
        
#### CompileImage

    加载profile文件，对基于profile文件的编译有效
    Compile函数，编译
    WriteOatFiles函数，输出.oat文件
    HandleImage函数，处理.art文件
    
    关键函数为上述三个，Compile用于编译dex字节码，WriteOatFiles和HandleImage生成最终的.oat和.art文件
    
    Compile函数：
        目标就是编译打开的那些Dex文件中的Java方法
        dex2oat中一个Java方法根据其具体情况有三种编译处理模式：
            1. dex到dex的编译（Dex To Dex Compilation）
            2. jni方法的编译，针对jni方法
            3. dex字节码到机器码的编译
        Compile流程：
            创建一个CompilerDriver对象
            调用CompileAll进行编译（CompilerDriver类方法）
        
        关键类和函数：
            CompilerDriver类，是dex2oat中负责编译相关工作的总管
            Compiler类，负责具体的编译工作，OptimizingCompiler是其唯一的实现子类
            DexCompiler类，专门用于Dex到Dex编译的
            CompilationVisitor虚基类，实现遍历类的功能，有4个子类：VerifyClassVisitor、CompileClassVisitor、ResolveTypeVisitor、ResolveClassFieldsAndMethodsVisitor
            CompiledMethodStorage类，用于管理存储Java方法编译的结果
            
            CompileAll函数：
                创建线程池
                PreCompile函数：
                    LoadImageClasses函数，遍历image_classes_中的类，通过ClassLinker的FindSystemClass进行加载，另外检查Java方法抛出的异常对应类型是否存在
                    Resolve函数，遍历dex文件，解析其中的类型，即dex文件中的type_ids数组，内部调用ClassLinker的ResolveType函数；解析dex里的类、成员变量、成员函数，内部通过ClassLinker对应方法
                    Verify函数，遍历dex文件，校验其中的类，校验结果通过QuickCompilationCallback存储在CompilerDriver的verification_results_中
                    InitializeClasses函数，遍历dex文件，确保类的初始化
                    UpdateImageClasses函数，遍历image_classes_中的类，检查类的引用型成员变量，将变量对应的class对象也加到image_classes_容器中
                Compile函数：
                    遍历dex文件，调用CompileDexFile函数进行编译
                    对不能编译成机器码的Java方法做dex到dex的优化
                    
                    CompileDexFile函数中，触发线程池进行编译工作，以类为单位进行处理，每个待编译的类交由CompileClassVisitor的Visit函数进行处理
                    Visit函数：
                        编译类中Java方法的入口函数
                        找到dex文件对象，根据class_def_index索引找到目标类对应的ClassDef信息
                        GetDexToDexCompilationLevel函数，对是否能编译优化的情况做判断，返回是否要做编译优化的枚举常量值
                        CompileMethod函数：
                            存储编译的结果，类型为CompiledMethod
                            针对dex到dex的编译优化，ArtCompileDEX函数
                            针对jni函数的编译，JniCompile函数，OptimizingCompiler类
                            dex到机器码的编译优化将由OptimizingCompiler的Compile完成
                            三种编译优化的结果都是CompiledMethod对象
                            最后保存到driver中，AddCompileMethod
                        
                CompiledMethod类：
                    派生自CompiledCode，基类的quick_code_指向一个定长数组，该数组存储的是编译得到的机器码
                    frame_size_in_bytes_，栈帧大小
                    core_spill_mask_，32位长，每一位对应一个核心寄存器，值为1表示对应寄存器的内容可能会存储在栈上
                    fp_spill_mask_，作用和core_spill_mask_一样，每一位代表一个浮点寄存器
                    src_mapping_table_，指向定长数组，元素为SrcMapElem，SrcMapElem建立从机器码到dex指令码的映射关系
                    vmap_table_，指向定长数组，元素反映了机器码中用到的物理寄存器到dex指令里虚拟寄存器的映射关系
                    cfi_info_，指向定长数组，存储和机器码相关的调试信息
                    patches_，指向定长数组，元素为LinkerPatch，和描述机器码中指令跳转的目标地址有密切关系
                    
    ArtCompileDEX函数：
        dex到dex的编译优化函数
        首先构造一个DexCompilationUnit对象（最后一个参数来自CompilerDriver的GetVerifiedMethod函数，为目标Java方法的校验结果）
        接着创建DexCompiler对象，调用它的Compile函数进行优化
        dex到dex的优化结果是将某些指令和操作数换成对应的快速操作指令和对应的操作数，ART中优化信息由QuickenedInfo结构体表示
        Java方法经dex到dex的优化后，处理结果对应的CompiledMethod对象中，vmap_table_的内容为一组QuickendedInfo信息，存储格式为LEB，其他为空
        
        DexCompiler的Compile函数：
            遍历并处理Java方法对应的dex指令码，只有18条指令可以尝试做优化
            
    OptimizingCompiler JniCompile：
        编译处理jni函数（Java的native函数）
        得到一些机器码，这些机器码的一个重要功能是建立栈帧、准备native函数的参数以及调用native函数
        
        关键类和函数：
            调用约定（Calling Convention），CallingConvention类家族：
                CallingConvention，代表调用约定的抽象类
                ManagedRuntimeCallingConvention（描述非jni函数调用约定）、JniCallingConvention（jni函数调用约定），两个派生抽象类
                Managed：Java指令的执行，无论是解释执行还是以执行编译后得到的机器码，都离不开虚拟机的管控
                X86JniCallingConvention、X86ManagedRuntimeCallingConvention，x86平台上的实现类
            Offset类家族：
                Offset类，代表偏移量，只有一个value_成员变量
                三个派生类
                FrameOffset，代表栈空间的偏移量
                ThreadOffset，代表ART Thread类中某成员变量在内存中相对该类一个对象基地址的偏移量
                MemberOffset，代表其他类中某个成员变量的偏移量
            ManagedRegister类，内部只有一个成员变量为id_，其取值为物理寄存器的编号
            ManagedRegisterSpill，ManagedRegister的派生类，代表一个溢出到栈上的寄存器信息，size_表示需占用栈空间的大小，spill_offset_代表溢出到栈上的起始位置
            HandleScope，在栈上存储mirror Object对象（引用类型）的地址
        GC，保存在栈上的对象就是根对象
        Java层的一个线程对应art中的一个Thread对象，每个Thread对象保存一条HandleScope对象链，链上的HandleScope对象保存了栈上的mirror Object对象
        
        JniCompile 第一段：
            内部调用ArtJniCompileMethodInternal函数
            创建两个调用约定对象，JNICallingConvention、ManagedRuntimeCallingConvention mr_conv
            mr_conv需要从jni函数调用者的栈帧中拷贝参数到jni函数自己的栈帧
            创建第三个CallingConvention对象，当jni函数从native层实现函数返回后，在Java层中有一些和栈相关的操作要指向
            创建用于输出机器码的Assembler对象
            JNICallingConvention的FrameSize函数确定jni函数栈帧的大小
            Assembler的BuildFrame函数构造建立栈帧的机器码
            各种寄存器操作
        JniCompile 第二段：
            ResetIterator函数用于重置调用约定对象内的各位置迭代器
            调用Assembler的StoreImmediateToFrame函数将立即数存储到栈帧的指定位置
            CopyRawPtrFromThread32函数用于将Thread对象中的top_handle_scope的值拷贝到栈上指定位置
            StoreStackOffsetToThread32函数更新根（top_handle_scope），将栈上的HandleScope对象的位置存储为新的根
        JniCompile 第三段：
            CurrentParamHandleScopeEntryOffset函数返回当前引用型参数位于栈上的位置
            LoadRef用于加载一个引用型参数（实际上是一个mirror Object对象）到指定寄存器
            StoreRef将LoadRef得到的Class对象存储到栈上的HandleScope的指定位置
            循环拷贝引用型参数到HandleScope的对应位置
        JniCompile 第四段：
            StoreStackPointerToThread32函数将ESP的值存储到Thread对象tlsPtr.managed_stack top_quick_frame_成员变量中
            OutArgSize函数返回jni对应native函数输入参数及返回值占据栈空间的大小
            IncreaseFrameSize拓展栈空间
            GetCurrentThread函数将把调用线程的Thread对象取出来，存放到栈的指定位置
            JniMethodStart函数返回值寄存器返给调用者，寄存器是IntReturnRegister
            Store函数生成机器指令
        JniCompile 第五段：
            主要工作：
                为native函数准备参数，先准备jobject和jint，然后准备jclass参数，最后才是JNIEnv
                调用native函数
                保存native函数的返回值到栈上
            CopyParameter，从mr_conv对应的栈空间中拷贝参数到main_jni_conv对应栈空间中去
            拷贝jclass对应的参数到栈空间
            拷贝JNIEnv对象的地址到栈空间指定位置
            jni对应的native函数的地址保存在ArtMethod对象ptr_sized_fields_ entry_point_from_jni_成员变量中
            保存native函数的返回值到栈上
        JniCompile 第六段：
            主要内容：
                调用JniMethodEnd函数
                检查native函数执行过程中是否有异常发生，若有，需跳转到异常处理函数去处理异常
                清理栈空间，准备回退到调用者函数
            调用JniMethodEnd函数：
                PopLocalReferences中PopHandleScope，修改Thread对应的HandleScope对象链，使之恢复为原HandleScope对象链条
            检查异常：
                调整栈顶位置
                检查Thread tlsPtr_.exception成员变量是否为空，不为空表示native函数执行时发生了异常，跳到art_quick_deliver_exception函数去执行
                检查无异常发送，继续执行
            栈清理以及返回到调用者：
                JniCompile函数生成的机器码最后一部分功能就是清理栈并返回到调用者
    
        在Java层调用一个jni函数时，实际上将产生三到四个函数调用：
            首先调用JniMethodStart
            调用native函数的实现entry_point_from_jni_，native函数内部执行发生异常时，不像Java那样直接抛出，而是先存储到调用线程Thread对象的tlsPtr_exception成员变量中
            从native函数返回后将调用JniMethodStart
            最后检查Thread对象的tlsPtr_exception是否为空，若不为空，调用art_quick_deliver_exception进行异常处理
            
    OptimizingCompiler Compile：
        非jni java函数编译处理过程
        主要内容：函数的整体流程和invoke相关的涉及函数调用指令的处理
        
        代表Java方法编译结果的CompiledMethod对象
        method_idx为待编译java方法在dex_file中method_ids数组中的索引
        TryCompile函数，编译dex字节码到机器码，包含构造CFG、执行优化任务（RunOptimizations）、编译ART IR、分配寄存器等
        Emit函数，对TryCompile的结果进行一些处理，返回编译结果的CompiledMethod对象，返回该对象
        
        Emit函数：
            EmitAndSortLinkerPatches函数，和dex2oat对invoke相关指令（函数调用）的处理有关
            BuildStackMaps函数，和CompiledMethod vmap_table_成员变量的设置有关
            返回编译结果的CompiledMethod对象
            
        invoke指令：
            invoke-static，调用类的静态方法
            invoke-virtual，调用类中的virtual方法，virtual方法指非private、static或final修饰的成员函数，类的构造函数不是virtual方法
            invoke-direct，调用类中的非静态方法，两类：private修饰的成员函数和类的构造函数
            invoke-super，在子类的函数中通过super来调用直接父类的函数
            invoke-interface，调用接口类中定义的函数
            dex中invoke指令格式描述为：
                invoke-kind {vC, vD, vE, vF, vG}, meth@BBBB
            dex文件中invoke-kind指令与它的参数的存储格式为：
                [A] op {vC, vD, vE, vF, vG}, meth@BBBB
            BBBB是目标函数在dex method_ids 中的索引，长16位
            VC、vD到vG均为虚拟寄存器，用于存储相关调用参数
            虚拟寄存器vA存储了参数个数，对非静态函数而言，参数个数不包含隐含的this变量
        
        处理dex指令的大致步骤为；
            首先将dex指令转成对应的ART IR对象--HInstruction类家族
            对ART IR进行优化，对invoke相关IR对象，Sharpening优化较重要
            
        HInvoke家族中和invoke-xxx指令相关的几个HInvoke类：
            HInvokeStaticOrDirect类，对应invoke-static/direct/super指令
            HInvokeInterface类，对应invoke-interface指令
            HInvokeVirtual类，对应invoke-virtual指令
            编译时目标函数对应的ArtMethod对象不能解析，则invoke指令将转换为一个HInvokeUnresolved对象
        
        总结：
            编译是以一个Java方法为基本单位来处理的
            Java方法包含的dex字节码编译分三种情况：
                1、dex到dex字节码的编译，实际上是一种优化处理
                2、针对jni函数的编译，编译结果是一串机器码，机器码的执行过程，也就是Jni函数执行的过程
                3、dex字节码到机器码的编译，编译、链接处理等过程
               

#### OAT和ART文件格式介绍

    OAT文件格式（去除ELF的封装，从上到下依次）：
        oat文件格式：
            1、头结构，由OatHeader类表示
            2、OatDexFile区域，可存储多个OatDexFile项，每一个dex项对应一个OatDexFile对象（OatDexFile类）
            3、DexFile区域，包含一到多个DexFile项（一个OatDexFile项对应一个DexFile项），一个DexFile项包含jar包中对应dex项的全部内容
            4、TypeLookup区域，包含一到多个TypeLookupTable，TypeLookupTable在oat文件中相对OatHeader的位置由OatDexFile的lookup_table_offset_指明
                一个dex项对应一个TypeLookupTable
            5、ClassOffsets区域，每一个Dex文件对应一个ClassOffsets表，每个ClassOffsets表包含该Dex项里所有OatClass项在oat文件里相对OatHeader的位置信息
            6、OatClass区域，每个经dex2oat编译的类都有对应的一个OatClass项（写入时 OatWriter::OatClass，读取时转换为OatFile::OatClass类）
            7、VmapTable区域，由Java方法编译的结果CompiledMethod的vmap_table_项构成，每一个包含vmap_table_  内容的CompiledMethod对象在该区域都有对应的存储空间，内容就是该对象的vmap_table_
            8、Trampoline code区域，该区域只在boot oat文件中存在，该区域中包含一组机器码，用于跳转到指定的函数
            9、OatMethod区域，包含一到多个OatQuickMethodHeader元素，对应的数据结构为OatQuickMethodHeader类，类中最后一个成员变量为code_，是一个数组，
                存储的是一个Java方法经过编译得到的机器码指令，读取该区域内容时代码中会借助OatFileManager::OatMethod类和相关成员变量
        OatDexFile类中有三个成员变量分别为指向DexFile、ClassOffsets、TypeLookupTable区域中某个元素的索引
        ClassOffsets是一个二维数组，ClassOffsets[a][b]，ClassOffsets[a]代表DexFile[a]中类信息偏移量数组，b表示第b个class_def信息存储在Oat文件中的区域
        OatClass区域为oat文件中存储的代表类的信息，其数据结构：
            status_，取值对应类的状态信息
            type_，取值为枚举类型OatClassType中的三个值，kOatClassAllCompiled为0，表示所有方法都编译了，kOatClassSomeCompiled为1，部分方法已编译，kOatClassNoneCompiled为2，没有方法编译
            method_bitmap_和method_bitmap_size_，位图对象及它的大小，method_bitmap_位图中每一位代表该类中所定义的方法，位取值为1表示该方法已经过编译，否则未编译，只在type_取值为kOatClassSomeCompiled时才存在这两个成员
            method_offset_，数组，元素为OatMethodOffset，其内部只有code_offset_，指向OatQuickMethodHeader区域中某个OatQuickMethodHeader的code_数组
        OatQuickMethodHeader中的code_数组存储的是Java方法经过编译后得到的机器码
        只有boot oat文件才会设置trampoline code区域
        
    ART文件格式：
        分为Image Section区域和Bitmap Section区域
        Image Section（包含8个Section，每个Section在文件中的偏移量和大小由ImageSection类描述）：
            1、Object Section，存储的是mirror Object对象，这些Object的内容存储在art文件中（类似序列化、反序列化）
                    前200个字节保存的是art文件头ImageHeader的内容，200个字节后才是mirror Object对象的内容
            2.3、ArtField和ArtMethod Section，存储的是ArtField对象和ArtMethod对象的内容
            4、RuntimeMethod Section，存储的也是ArtMethod对象的内容，但这些ArtMethod代表虚拟机本身提供的一些方法，而不是来自Java类中定义的方法，Runtime一共定义六种runtime方法，该区域元素为六
            5、ImtConflictTable Section，存储的是ImtConflictTable对象，和调用接口方法的处理有关
            6、DexCacheArrays Section，该区域存储的内容和DexCache有关，是通过DexCacheArraysLayout将一个DexCache对象关联的GcRoot<Class>数组（元素类型为Class*数组、ArtMethod*数组、ArtField*数组、String*数组）按顺序存储在该Section中，即该Section包含一到多个DexCache元素，每个DexCache元素的内容就是几组不同类型的指针
            7、InternedStrings Section，存储的是一个InternTable对象的内容，和ART虚拟机对Interned String处理有关
            8、ClassTable Section，存储的是一个ClassTable对象的内容
        Bitmap Section：
            Bitmap，是一个位图，用于描述Object Section里各个Object的地址，一个比特位的值为1，则它指向Object Section中的一个Object对象，一个比特位对应8字节
        Object Section存储的是各种Object派生类实例，大小不固定，Oat文件格式要求每一个Object在区域中的大小按8字节向上对齐
        Oat文件通过mmap方式加载到内存时，一个Object在文件中的位置其实就是一个指针地址
        
        ImageHeader结构（部分成员变量）：
            magic_，值为artn
            version_，值为 0290
            image_begin_，art文件通过mmap加载到内存中的基地址
            image_size_，不包含Bitmap Section的大小
            storage_mode_，用于描述art文件的格式，art文件可压缩等
            data_size_，取值为art文件中不包括ImageHeader和Bitmap Section之外的其他区域的大小
            ImageSection[9]，有9个元素的数组，元素类型为ImageSection，描述art文件中各个section在文件中的偏移量和大小
            image_methods_[6]，有6个元素的数组，元素类型为uint64_t，是一个指针，指向RuntimeMethod Section中某个ArtMethod对象
            image_roots_，是一个指针，指向只有两个元素的ObjectArray数组，元素类型也是ObjectArray，第一个保存DexCache数组，第二个保存Class数组（都是指针）
    
    OAT文件和ART文件的关系：
        ImageHeader中有几个成员变量关联到oat文件里的信息，oat_file_begin_指向oat文件加载到内存的虚拟地址，oat_data_begin_指向符号oatdata的值，oat_data_end_指向符号oatlastword的值
        art文件中的ArtMethod对象的成员变量ptr_sized_fields_结构体的entry_point_from_quick_compiled_code_指向位于oat文件里对应的code_数组
        art文件可看作是很多对象通过类似序列化的方法保存到文件里而得来的
        art文件里保存的对象有：mirror Object及派生类的实例（Class、String、DexCache等），ArtMethod、ArtField、ImtConflictTable等对象
        如果在dex2oat时不生成art文件的话，上述对象只能在程序运行时才创建，如此耗费一定的运行时间，7.0中，boot镜像必须生成art文件，app默认只生成oat文件，其art文件根据profile的情况由系统后台服务择机生成，以减少安装时间
    
    
    oatdump介绍：
        oatdump是Android系统提供的由来查看oat和art文件信息的优质工具
        adb shell oatdump --image=/system/framework/boot.art --header-only
            查看设备上boot.art文件内容，--header-only 只展示头信息
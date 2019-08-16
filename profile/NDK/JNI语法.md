
## JNI语法
    
    域描述（变量）：
        字符串描述为 B C D F I J S Z 对应 byte char double float int long short boolean
        引用类型的格式为 LClassName。ClassName为对应类的全路径名，如：Ljava/lang/String;
        数组也是引用类型，格式为 [其他类型描述名，如：int[]为 [I，二维数组为 [[I
    成员函数描述（Method Descriptor）：
        包含返回值及参数的数据类型
        (ParameterDescriptor*) ReturnDescriptor     #括号内是参数的数据类型，*表示可有0到多个
        eg： 函数 void a(String b) --> 描述 (Ljava/lang/String;)V
    Native对应的Java类型：
        jstring、jboolean、jint、jbyte、jchar、jlong、jdouble、jfloat、jobject、jarray
        jintArray、jbyteArray、jcharArray、jlongArray、jdoubleArray、jfloatArray

#### 与Java交互

    const char *str = env->GetStringUTFChars(jString, &isCopy);//获取 C字符串
    ## jString为从Java传过来的字符串，isCopy 表示复制副本还是对原字符串操作
    env->ReleaseStringUTFChars(jString, str);//释放 C字符串地址
    ## 这两个是成对出现的
        
    jintArray jArray = env->NewIntArray(10);//创建java 数组
    jint nativeArray[10];
    env->GetIntArrayRegion(jArray, 0, 10, nativeArray);//将java 数组 复制到 c 数组 对副本进行操作
    nativeArray[0] = 0;
    nativeArray[1] = 1;
    env->SetIntArrayRegion(jArray, 0, 2, nativeArray);//将对副本（c数组）的操作复制回java 数组
    jint *jint1 = env->GetIntArrayElements(jArray, nullptr);//对java 数组 直接指针操作
    jint1[3] = 3;
    env->ReleaseIntArrayElements(jArray, jint1, 0);//释放指针地址，0为释放模式，有3个选项:JNI_COMMIT,JNI_ABORT,0
    
    jclass clazz = env->GetObjectClass(obj);//获取给定java实例的java class 对象，obj为从Java传入的Obj
    jfieldID fieldId = env->GetFieldID(clazz, "name", "Ljava/lang/String;");//获取域Id，java对象实例中的变量，域描述符（变量类型）
    jstring name = (jstring) env->GetObjectField(obj, fieldId);//根据域Id 获取域，即native 获取java 成员变量
    const char *strName = env->GetStringUTFChars(name, 0);
    ...
    env->ReleaseStringUTFChars(name, strName);
    
    获取Java静态变量或静态方法：
        env->GetStaticBooleanField()
        env->GetStaticMethodID()
    
    jmethodID methodId = env->GetMethodID(clazz, "funcName", "()Ljava/lang/String;");//获取方法Id，后面参数为方法描述符（方法签名）
    jstring methodStr = (jstring) env->CallObjectMethod(obj, methodId);//执行java方法,java和原生代码之间转换代价较大，如非必要不建议这样使用
    jthrowable ex = env->ExceptionOccurred();//若java方法有异常抛出，需用此方法显示地做异常处理（查询VM中是否有异常挂起）
    if (0 != ex) {
        env->ExceptionClear();//显示地清除异常
        /*其他操作*/
        //抛出异常
        jclass nullEx = env->FindClass("java/lang/NullPointerException");//查找异常类，返回的是局部引用，原生方法返回，局部引用自动释放
        if (0 != nullEx)
            env->ThrowNew(nullEx, "null exception");//初始化并抛出异常，手动释放原生资源 返回等？
            env->DeleteLocalRef(nullEx);//显示释放局部引用（局部引用过多时应手动释放（允许最多16个局部引用？）），局部引用可初始化为全局/弱全局引用
    }
            
    java虚拟机不能识别 POSIX线程，需要将POSIX附着到虚拟机上（需要 JavaVM接口）
    当共享库开始加载时虚拟机自动调用该函数
    static JavaVM *gVm = NULL;//java虚拟机接口指针，全局变量，作缓存，供多线程使用
    jint JNI_OnLoad(JavaVM *vm, void *reserved) {
        LOG_INFO("JNI OnLoad 自动调用");
        gVm = vm;
        return JNI_VERSION_1_6;
    }
    
    FindClass：
        用于查找指定类名的类信息，该函数实现位于 jni_internal.cc中
        目标类的搜索工作由 ClassLinker 的 FindClass/FindSystemClass函数（返回 mirror Object对象）完成
        最后通过 ScopedObjectAccess 类实例的 AddLocalReference 将mirror Class对象转换成jclass的值返回
    RegisterNatives：
        用于将native层的函数与Java层中标记为native的函数关联起来，该函数是每一个JNI库（SO文件）使用前必须调用的
        内部调用 RegisterNativeMethods函数，输入参数 methods数组为JNINativeMethod结构体（自己构造）元素
        通过 ScopedObjectAccess的 Decode函数将jclass转换为 mirror Class，使用FindMethod函数在mirror Class中找到匹配函数名、签名相同的函数，返回一个ArtMethod
        最后调用 ArtMethod的RegisterNative函数

#### 内存管理
    
    静态分配：静态和全局变量，在应用程序启动时自动发生；
    自动分配：函数参数和局部变量，在包含声明的复合语句被输入时发生，退出复合语句时所分配的内存自动释放；
    动态分配：前两者假设所需内存大小和范围是固定的，编译时被定义，动态分配则在事先不知情的情况下起作用，
             内存大小和范围的分配取决于运行时因素。
    
    unsigned char *buffer = (unsigned char *) malloc(1024);//分配内存（标准C库函数）
    jobject directBuffer = env->NewDirectByteBuffer(buffer, 1024);//创建直接字节缓冲区（java可使用的）
    unsigned char *bufferAddress = (unsigned char *) env->GetDirectBufferAddress(directBuffer);//获取java创建的字节缓冲区的原生字节数组内存地址
    free(buffer);//释放内存
    
    auto *dInt = new int[16];//动态内存分配（C++支持）（调用构造函数）
    delete[] dInt;//释放内存（释放前先调用析构函数），释放数组原生使用[]
    
    全局引用可被多线程共享，局部引用不能， JNIEnv接口指针 在方法调用的线程中有效，不能被其他线程缓存或使用
    static jobject gObj = nullptr;//java对象的全局引用
    gObj = env->NewGlobalRef(obj);//为Java对象创建全局引用，方法内
    env->DeleteGlobalRef(gObj);//删除全局引用，否则会发生内存泄漏
    
    JNI层中创建的 jobject对象默认是局部引用（Local Reference），函数从JNI层返回后，局部引用的对象很可能被回收
    若需要长期保存一个jobject对象，将局部引用转换成全局引用（Global Reference）
    全局引用对象需要使用者主动释放，进程能持有的全局引用对象总个数是有限制的
    弱全局对象（Weak Global Reference）有可能被回收（无需主动释放），使用前调用 JNIEnv的IsSameObject函数与 nullptr比较
                
    NewGlobalRef    函数内部使用 ScopedObjectAccess 的Decode解析出jobject对应的mirror Object对象，再调用JavaVMExt的AddGlobalRef函数添加到容器中
    DeleteGlobalRef
    NewWeakGlobalRef
    DeleteWeakGlobalRef
    NewLocalRef
    DeleteLocalRef
    一个Java进程只有一个JavaVME对象，代表虚拟机
    JavaVME对象中有一个globals成员变量，是一个容器，存储进程中创建的全局引用对象
    AddGlobalRef函数返回一个IndirectRef值，与mirror Object地址有关，全局变量存取有关
    局部引用，使用JNIEnvExt对象的 AddLocalRef函数存储，JNIEnvExt对象包含一个Locals_成员变量，用于存储在JNIEnv环境中创建的局部引用对象
        

#### 文件操作

    写操作：
        fwrite 返回值同fread，fputs、fputc() 返回EOF 表示字符不能写入
        fprintf() 正常返回写入的字符个数，出错返回负数
    刷新缓冲区：
        fflush() 刷新输出缓冲区， 正常返回0， 不能写入返回 EOF
    读操作：
        fread，fgets() 读取以换行符结尾的字符序列 成功返回缓冲区指针 否则NULL
        fgetc() 返回整数位无符号字符，EOF结尾
        fscanf() 读取格式数据 返回读取的项目个数 错误返回EOF
    检查文件结尾：
        feof() 如果设置了文件结束指示符则使用，已到达结尾返回非0值， 还有数据返回0
    
    FILE *file = fopen("/sdcard/shared/viewcache", "r");
    if (NULL == file)
        LOG_ERROR("文件打开失败， null");
    else {
        char *buffer = (char *) malloc(sizeof(char) * 10);
        size_t count = fread(buffer, sizeof(buffer), 10, file);//返回读取流的元素个数，个数与预期不一致表示读取完毕？
        free(buffer);
        fclose(file);
    }

#### 执行Shell

    int result = system("mkdir /sdcard/shared/temp");//执行shell命令，与进程交互
    if (-1 == result || 127 == result)
        LOG_ERROR("shell命令执行失败");
    else
        LOG_INFO("shell result -- %i", result);
    
    与子进程通信，打开一个双向通道，阻塞式--命令执行结束前一直等待
    FILE *stream = popen("ls /", "r");//在父、子进程间打开一个双向通道，向ls命令打开只读通道
    if (NULL == stream)
        LOG_ERROR("ls 命令执行失败");
    else {
        char buffer[1024];
        while (NULL != fgets(buffer, 1024, stream)) {
            LOG_INFO("read: %s", buffer);
        }
        int status = pclose(stream);//关闭通道
        LOG_INFO("pclose status: %i", status);
    }

#### 获取系统属性

    char value[PROP_VALUE_MAX];
    if (__system_property_get("ro.product.model", value) == 0)//通过系统属性名称获取值
        LOG_ERROR("未找到系统属性");
    else
        LOG_INFO("product model: %s", value);
    
    char *user = getlogin();
    LOG_INFO("login user: %s, uid: %u, gid: %u", user, getuid(), getgid());
    const prop_info *info = __system_property_find(name);
    if (NULL == info)
        LOG_ERROR("%s 属性未找到", name);
    else {
        char name[PROP_NAME_MAX];
        char value[PROP_VALUE_MAX];
        int result = __system_property_read(info, name, value);
        if (result == 0)
            LOG_ERROR("prop_info 未找到具体信息");
        else
            LOG_INFO("prop info, name: %s, value: %s", name, value);
    }

#### Native线程

    POSIX线程同步：
        互斥锁：Mutexes，特定代码部分不同时执行
        信号量：Semaphores，控制对特定数目资源的访问，如果没有可用资源，调用线程在信号量涉及的资源上等待，直到资源可用
                信号量的值大于0， 可上锁，并且值递减，如果信号量的值为0，调用线程挂起，直到解锁。
    初始化互斥锁
    if (0 != pthread_mutex_init(&mutex, NULL)) {
        jclass rtClazz = env->FindClass("java/lang/RuntimeException");
        env->ThrowNew(rtClazz, "unable to initialize mutex.");
    }
    初始化信号量
    if (0 != sem_init(&sem, 0, 1)) {
        jclass rtClazz = env->FindClass("java/lang/RuntimeException");
        env->ThrowNew(rtClazz, "unable to initialize sem.");
    }
    销毁互斥锁，成功返回0，否则返回错误代码
    if (0 != pthread_mutex_destroy(&mutex)) {
    }
    销毁信号量
    if (0 != sem_destroy(&sem)) {
        jclass rtClazz = env->FindClass("java/lang/RuntimeException");
        env->ThrowNew(rtClazz, "unable to destroy sem.");
    }
    锁定互斥锁
    if (0 != pthread_mutex_lock(&mutex)) {
        jclass rtClazz = env->FindClass("java/lang/RuntimeException");
        env->ThrowNew(rtClazz, "unable to lock mutex.");
    }
    锁定信号量
    if (0 != sem_wait(&sem)) {
        jclass rtClazz = env->FindClass("java/lang/RuntimeException");
        env->ThrowNew(rtClazz, "unable to lock sem.");
    }
    解锁互斥锁
    if (0 != pthread_mutex_unlock(&mutex)) {
        jclass rtClazz = env->FindClass("java/lang/RuntimeException");
        env->ThrowNew(rtClazz, "unable to unlock mutex.");
    }
    解锁信号量
    if (0 != sem_post(&sem)) {
        jclass rtClazz = env->FindClass("java/lang/RuntimeException");
        env->ThrowNew(rtClazz, "unable to unlock sem.");
    }
    
    java虚拟机不能识别 POSIX线程，需要将POSIX附着到虚拟机上（需要 JavaVM接口）
    将POSIX线程附着到虚拟机上，已获得一个有效的 JNIEnv 接口指针
    将当前线程附着到JavaVM上，在线程所执行的方法中
    if (0 == gVm->AttachCurrentThread(&env, NULL)) {
        NativeWorkerArgs *workerArgs = (NativeWorkerArgs *) args;
        //执行上面的Worker程序
        doNativeWorker(env, gObj, workerArgs->id, workerArgs->iterations);
        //释放
        delete workerArgs;
        //从JavaVM 中分离当前线程
        gVm->DetachCurrentThread();
    }
    return (void *) 1;
    
    新建线程：
        pthread_t *handles = new pthread_t[threads];//线程句柄        
        int result = pthread_create(&handles[i], NULL, nativeWorkerThread, args);//创建线程
        if (0 != result) {
            jclass exClazz = env->FindClass("java/lang/RuntimeException");
            env->ThrowNew(exClazz, "create pthread failed");
        }
        nativeWorkerThread为线程执行方法，args为方法参数                  
    线程终止：
        pthread_join 连接每个线程句柄，等待线程终止（将挂起UI线程，直到创建的线程终止，先显示下面消息，后显示迭代消息）
        if (0 != pthread_join(handles[i], &result)) {
            jclass rtClazz = env->FindClass("java/lang/RuntimeException");
            env->ThrowNew(rtClazz, "native join thread failed");
        }
        
#### Socket

#### Media
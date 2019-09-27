
## Android系统启动流程

[参考](http://liuwangshu.cn/framework/booting/1-init.html)
[参考](https://juejin.im/post/5cfb97006fb9a07f0052cdc5)
[9.0源码](http://androidxref.com/9.0.0_r3/xref/)
    
    Android系统的启动流程：
        1、启动电源以及系统启动
        2、引导程序BootLoader——BootLoader是android操作系统开始运行前的一个小程序，它的主要功能就是把系统os拉起来并运行
        3、Linux内核启动——在内核启动后，会在系统文件中找到init.rc文件，并启动init进程
        4、init进程启动——初始化和启动属性服务，并启动zygote进程
        5、Zygote进程启动——创建java虚拟机并为java虚拟机注册jni方法，创建服务端socket，启动systemServer进程
        6、SystemServer进程启动——启动Binder线程池和systemServerManager，并启动各种服务
        7、Launcher启动——将SystemServer进程启动的AMS会启动Launcher，Launcher启动后把已安装的应用的快捷图标显示到界面上。

#### init进程启动Zygote
    
    init启动zygote时主要是调用app_main.cpp的main函数中的AppRuntime的start来启动zygote进程的
    
#### Zygote进程启动流程
    
    Zygote进程配置脚本文件位于/system/core/rootdir/init.zygote64.rc，对应64位CPU架构，/system/core/rootdir/init.zygote64.rc 是系统路径
    配置脚本第一行：
        service zygote /system/bin/app_process -Xzygote /system/bin --zygote --start-system-server
        service zygote：告诉init进程要配置一个名为 zygote的服务
        /system/bin/app_process：zygote服务对应的二进制文件路径，init fork一个子进程运行指定程序，这个程序就是/system/bin/app_process
        -Xzygote /system/bin --zygote --start-system-server：传递给app_process的启动参数
    app_process对应代码位于/frameworks/base/cmds/app_process/app_main.cpp：
        int main(int argc, char* const argv[]) {
            ...
            AppRuntime runtime(argv[0], computeArgBlockSize(argc, argv));
            ...
            int i;
            for (i = 0; i < argc; i++) {
                ...
                String8 className;  //const char* arg = argv[i++]; className.setTo(arg);    可见className由输入参数argv决定
                ...
                if (!className.isEmpty()) {
                    ...
                } else {
                    // We're in zygote mode.
                    maybeCreateDalvikCache();
                    if (startSystemServer) {
                        args.add(String8("start-system-server"));  //Zygote进程启动参数，会启动SystemServer进程
                    }
                    ...
                    if (zygote) {
                        runtime.start("com.android.internal.os.ZygoteInit", args, zygote);  //启动Zygote进程
                    } else if (className) {
                        runtime.start("com.android.internal.os.RuntimeInit", args, zygote);
                    }
                    ...
                 }
            }
        }
        在其main函数中，最终由AppRuntime的start函数启动Zygote，ZygoteInit为一个Java类
        
    AppRuntime继承自AndroidRuntime，实际调用的是AndroidRuntime的start函数
    /frameworks/base/core/jni/AndroidRuntime.cpp：
        /*
         * Start the Android runtime.  This involves starting the virtual machine
         * and calling the "static void main(String[] args)" method in the class
         * named by "className".
         *
         * Passes the main function two arguments, the class name and the specified
         * options string.
         */
        void AndroidRuntime::start(const char* className, const Vector<String8>& options, bool zygote) {
            ...
            JniInvocation jni_invocation;
            jni_invocation.Init(NULL);
            JNIEnv* env;
            if (startVm(&mJavaVM, &env, zygote) != 0) {     //启动ART虚拟机
                return;
            }
            onVmCreated(env);    
            if (startReg(env) < 0) {    //Register android functions，为虚拟机注册JNI
                ALOGE("Unable to register all android natives\n");
                return;
            }
            ...
            jstring classNameStr = env->NewStringUTF(className);    //将输入参数转为Java字符串
            ...
            /*  Start VM.  This thread becomes the main thread of the VM, and will not return until the VM exits.*/
            //启动VM，这个线程为VM的主线程，在VM退出前不会返回
            char* slashClassName = toSlashClassName(className != NULL ? className : "");    //将C字符串中的 . 转为 /
            jclass startClass = env->FindClass(slashClassName);     //找到Java类，ZygoteInit类
            if (startClass == NULL) {
                ALOGE("JavaVM unable to locate class '%s'\n", slashClassName);
                /* keep going */
            } else {
                jmethodID startMeth = env->GetStaticMethodID(startClass, "main", "([Ljava/lang/String;)V"); //找到main方法ID
                if (startMeth == NULL) {
                    ALOGE("JavaVM unable to find main() in '%s'\n", className);
                    /* keep going */
                } else {
                    env->CallStaticVoidMethod(startClass, startMeth, strArray);     //jni执行Java方法，ZygoteInit的main方法，strArray为Java字符串数组，数组第一项为classNameStr（com.android.internal.os.ZygoteInit），其余为输入参数options数组内容
            #if 0
                if (env->ExceptionCheck())
                    threadExitUncaughtException(env);   //异常检查
            #endif
                }
            }
            free(slashClassName);   //释放内存
            ...
        }
        看注释，将会启动ART虚拟机，执行ZygoteInit的main函数（在jni层执行Java方法）
        
    AndroidRuntime的start函数中通过jni调用了ZygoteInit的main函数
    由此进入Java层
    ZygoteInit的main函数：
        public static void main(String argv[]) {
            ZygoteServer zygoteServer = new ZygoteServer();
            Os.setpgid(0, 0);
            ...
            boolean startSystemServer = false;
            String socketName = "zygote";
            String abiList = null;
            boolean enableLazyPreload = false;
            for (int i = 1; i < argv.length; i++) {
                if ("start-system-server".equals(argv[i])) {    //解析输入参数，包括支持的ABI列表，app_main.cpp的main函数的输入参数
                    startSystemServer = true;
                } else if ("--enable-lazy-preload".equals(argv[i])) {
                    enableLazyPreload = true;
                } else if (argv[i].startsWith(ABI_LIST_ARG)) {  //支持的ABI 列表
                    abiList = argv[i].substring(ABI_LIST_ARG.length());
                } else if (argv[i].startsWith(SOCKET_NAME_ARG)) {
                    socketName = argv[i].substring(SOCKET_NAME_ARG.length());
                } 
            }
            ...
            zygoteServer.registerServerSocketFromEnv(socketName);   //为zygote命令连接注册server socket。这将通过ANDROID_SOCKET_环境变量来定位server socket的文件描述符（ file descriptor）
            if (!enableLazyPreload) {
                preload(bootTimingsTraceLog);   //预加载资源和类，在第一个fork之前预加载内容。
            } else {
                Zygote.resetNicePriority();     //将调用线程优先级重置为默认值
            }
            gcAndFinalize();    //启动后进行初始gc清理
            Zygote.nativeSecurityInit();    //在任何fork之前调用一些安全初始化
            Zygote.nativeUnmountStorageOnInit();    //zygote 进程初始化后卸载存储空间，此方法只调用一次。
            if (startSystemServer) {
                Runnable r = forkSystemServer(abiList, socketName, zygoteServer);   //fork SystemServer进程
                if (r != null) {    //r为空，在 zygote进程，r不为空，在子进程（system_server）
                    r.run();
                    return;
                }
            }
            // The select loop returns early in the child process after a fork and loops forever in the zygote.
            caller = zygoteServer.runSelectLoop(abiList);//select循环在fork之后的子进程中提前返回，在zygote中永久循环
            ...
            zygoteServer.closeServerSocket();   //有异常发生或循环结束后，关闭并清理zygote sockets。在shutdown和子进程的退出路径上调用。
            // We're in the child process and have exited the select loop. Proceed to execute the command.
            if (caller != null) {
                caller.run();
            }
        }
        AndroidRuntime的start函数中通过jni调用当前方法用来初始化zygote进程，并fork一个SystemServer子进程
        通过registerServerSocketFromEnv函数来创建一个Server端的Socket，这个name为”zygote”的Socket用来等待ActivityManagerService来请求Zygote来创建新的应用程序进程
        preload预加载资源和类，由--enable-lazy-preload参数决定，/system/etc/preloaded-classes为预加载的类，在dex2oat的输入参数中包含
        forkSystemServer启动SystemServer进程，系统的关键服务在此进程中启动
        runSelectLoop运行zygote进程的select循环。在新连接发生时接受它们，并从连接中读取命令，每次生成一个值的请求。
    
    registerServerSocketFromEnv：
        创建LocalServerSocket，也就是服务端的Socket
        当Zygote进程将SystemServer进程启动后，就会在这个服务端的Socket上来等待ActivityManagerService请求Zygote进程来创建新的应用程序进程
    
    forkSystemServer：
        返回一个Runnable，该Runnable在子进程中为system_server代码提供一个入口点，在父进程中为null
        String args[] = {
                    "--setuid=1000",
                    "--setgid=1000",
                    "--setgroups=1001,1002,1003,1004,1005,1006,1007,1008,1009,1010,1018,1021,1023,1024,1032,1065,3001,3002,3003,3006,3007,3009,3010",
                    "--capabilities=" + capabilities + "," + capabilities,
                    "--nice-name=system_server",
                    "--runtime-args",
                    "--target-sdk-version=" + VMRuntime.SDK_VERSION_CUR_DEVELOPMENT,
                    "com.android.server.SystemServer",
                };
        SystemServer的启动参数
        pid = Zygote.forkSystemServer(...);
        forkSystemServer进程
        如果pid为0，表示在新创建的子进程中执行的，由handleSystemServerProcess来启动SystemServer进程。
    
    handleSystemServerProcess：
        完成新创建的系统服务器进程的剩余工作，此时已经在SystemServer进程中执行
        解析上面的启动参数，设置相应值
        ClassLoader cl = null;
        if (systemServerClasspath != null) {
            cl = createPathClassLoader(systemServerClasspath, parsedArgs.targetSdkVersion);
            Thread.currentThread().setContextClassLoader(cl);
        }
        为SystemServer进程创建ClassLoader
        创建PathClassLoader（委托对象是System加载器），加载SYSTEMSERVERCLASSPATH环境变量中的dex文件，包含三个dex文件：services.jar、ethernet-service.jar、wifi-service.jar
        return ZygoteInit.zygoteInit(parsedArgs.targetSdkVersion, parsedArgs.remainingArgs, cl);
        最后执行zygoteInit方法
        zygoteInit中调用了RuntimeInit.applicationInit(targetSdkVersion, argv, classLoader)
        applicationInit中又调用了findStaticMain(args.startClass, args.startArgs, classLoader)（在RuntimeInit中）
        由上面可得 args.startClass 为 com.android.server.SystemServer
        findStaticMain函数以反射的方式找到SystemServer的main方法，并以main对应的Method作为参数返回一个MethodAndArgsCaller对象（实现了Runnable接口）
        MethodAndArgsCaller类实现的run方法中以反射方式执行main方法，mMethod.invoke(null, new Object[] { mArgs }); 方法参数为上面的args.startArgs
    
    即forkSystemServer返回的Runnable不为空，则fork成功，此时在子进程中，后续执行Runnable的run方法，即以反射方式执行上面的 SystemServer 的main方法
    forSystemServer返回null，表示还在zygote进程，继续执行后面的 runSelectLoop 进入循环
    到此SystemServer进程启动成功
    
    runSelectLoop方法：
        fds.add(mServerSocket.getFileDescriptor());
        peers.add(null);
            两个列表成对的，索引对应的元素相互关联
            mServerSocket为上面 registerServerSocketFromEnv方法中创建LocalServerSocket，获取其fd添加到列表中
        while (true) {      //无限循环用来等待ActivityManagerService请求Zygote进程创建新的应用程序进程
            StructPollfd[] pollFds = new StructPollfd[fds.size()];
            for (int i = 0; i < pollFds.length; ++i) {
                pollFds[i] = new StructPollfd();
                pollFds[i].fd = fds.get(i);         //通过遍历将fds存储的信息转移到pollFds数组中
                pollFds[i].events = (short) POLLIN;
            }
            Os.poll(pollFds, -1);       //会更改StructPollfd的revents状态？
            for (int i = pollFds.length - 1; i >= 0; --i) {     //遍历pollFds数组
                if ((pollFds[i].revents & POLLIN) == 0) {
                    continue;
                }
                //倒序遍历，
                if (i == 0) {   //i==0说明服务端Socket与客户端连接上，也就是当前Zygote进程与ActivityManagerService建立了连接
                    ZygoteConnection newPeer = acceptCommandPeer(abiList);  
                    peers.add(newPeer);     //通过acceptCommandPeer函数得到ZygoteConnection类并添加到Socket连接列表peers中，接着将该ZygoteConnection的fd添加到fd列表fds中，以便可以接收到ActivityManagerService发送过来的请求
                    fds.add(newPeer.getFileDesciptor());    //添加后进入下次循环
                } else {
                    ZygoteConnection connection = peers.get(i);
                    final Runnable command = connection.processOneCommand(this);    //此方法中fork子进程，后续执行在子进程中
                    if (mIsForkChild) {     //mIsForkChild为true表示在已经子进程中，子进程中不能运行命令行，mIsForkChild的值在processOneCommand方法中调用zygoteServer.setForkChild();更改的
                        if (command == null) {
                            throw new IllegalStateException("command == null");
                        }
                        return command;
                    } else {    //在zygote进程中，此时已经fork了子进程
                        if (command != null) {
                            throw new IllegalStateException("command != null");
                        }
                        // We don't know whether the remote side of the socket was closed or not until we attempt to read from it from processOneCommand. This shows up as a regular POLLIN event in our regular processing loop.
                        if (connection.isClosedByPeer()) {
                            connection.closeSocket();
                            peers.remove(i);
                            fds.remove(i);
                        }
                    }
                }
            }
        }
        runSelectLoop方法中主要是开启一个死循环接收AMS建立连接的请求，主要使用 Os.poll(pollFds, -1)？
        （AMS在SystemServer进程中）建立连接后，主要是在ZygoteConnection的processOneCommand方法中fork子进程：Zygote.forkAndSpecialize(...)
        然后在zygote进程中将这个连接的socket从fd、peer列表中清除
    
    Zygote启动流程：
        1、创建AppRuntime并调用其start方法，启动Zygote进程。
        2、创建ART并为ART虚拟机注册JNI。
        3、通过JNI调用ZygoteInit的main函数进入Zygote的Java框架层。
        4、通过registerServerSocketFromEnv函数创建服务端Socket。
        5、启动SystemServer进程。
        6、并通过runSelectLoop函数等待ActivityManagerService的请求来创建新的应用程序进程。

#### SystemServer进程启动流程
    
    在ZygoteInit的main函数中，通过forkSystemServer方法启动了SystemServer进程
    forkSystemServer：
            该函数返回一个Runnable，该Runnable在子进程中为system_server代码提供一个入口点，在父进程中为null
            String args[] = {
                        "--setuid=1000",
                        "--setgid=1000",
                        "--setgroups=1001,1002,1003,1004,1005,1006,1007,1008,1009,1010,1018,1021,1023,1024,1032,1065,3001,3002,3003,3006,3007,3009,3010",
                        "--capabilities=" + capabilities + "," + capabilities,
                        "--nice-name=system_server",
                        "--runtime-args",
                        "--target-sdk-version=" + VMRuntime.SDK_VERSION_CUR_DEVELOPMENT,
                        "com.android.server.SystemServer",
                    };
            SystemServer的启动参数
            pid = Zygote.forkSystemServer(...);
            fork SystemServer进程
            如果pid为0，表示在新创建的子进程中执行的，由handleSystemServerProcess来启动SystemServer进程。
            
        handleSystemServerProcess：
            完成新创建的系统服务器进程的剩余工作，此时已经在SystemServer进程中执行
            解析上面的启动参数，设置相应值
            ClassLoader cl = null;
            if (systemServerClasspath != null) {
                cl = createPathClassLoader(systemServerClasspath, parsedArgs.targetSdkVersion);
                Thread.currentThread().setContextClassLoader(cl);
            }
            为SystemServer进程创建ClassLoader
            创建PathClassLoader（委托对象是System加载器），加载SYSTEMSERVERCLASSPATH环境变量中的dex文件，包含三个dex文件：services.jar、ethernet-service.jar、wifi-service.jar
            return ZygoteInit.zygoteInit(parsedArgs.targetSdkVersion, parsedArgs.remainingArgs, cl);
            最后执行zygoteInit方法
            
        zygoteInit方法：
            public static final Runnable zygoteInit(int targetSdkVersion, String[] argv, ClassLoader classLoader) {
                RuntimeInit.redirectLogStreams();   //重定向System.out和System.err到Android log，out为INFO级别，err为WARN级别
                RuntimeInit.commonInit();   //统用设置，VM中的所有线程可用
                ZygoteInit.nativeZygoteInit();  //启动Binder线程池，对应native源码位于/frameworks/base/core/jni/AndroidRuntime.cpp中
                return RuntimeInit.applicationInit(targetSdkVersion, argv, classLoader);
            }
            commonInit主要用于设置
                Thread.setUncaughtExceptionPreHandler(loggingHandler);
                Thread.setDefaultUncaughtExceptionHandler(new KillApplicationHandler(loggingHandler));
            还有时区、LogManager、HTTP的User-Agent等
            抓取奔溃日志时，我们可替换setDefaultUncaughtExceptionHandler，不能设置setUncaughtExceptionPreHandler
        
        RuntimeInit.applicationInit：    
            protected static Runnable applicationInit(int targetSdkVersion, String[] argv,
                        ClassLoader classLoader) {
            // If the application calls System.exit(), terminate the process immediately without running any shutdown hooks.  
            It is not possible to shutdown an Android application gracefully.  Among other things, the
             Android runtime shutdown hooks close the Binder driver, which can cause leftover running threads to crash before the process actually exits.
                nativeSetExitWithoutCleanup(true);  //见上面内容，调用System.exit()时立即终止进程，因此会造成一些线程奔溃问题等
                VMRuntime.getRuntime().setTargetHeapUtilization(0.75f);//为避免占用大量不必要的内存，我们希望在堆利用率方面相当积极
                VMRuntime.getRuntime().setTargetSdkVersion(targetSdkVersion);   //设置target sdk version
                final Arguments args = new Arguments(argv); //参数规范化
                return findStaticMain(args.startClass, args.startArgs, classLoader);
            }
            最后又调用了findStaticMain方法
            由上面可得 args.startClass 为 com.android.server.SystemServer
        
        RuntimeInit.findStaticMain：
            protected static Runnable findStaticMain(String className, String[] argv,
                    ClassLoader classLoader) {    
                cl = Class.forName(className, true, classLoader);
                Method m = cl.getMethod("main", new Class[] { String[].class });
                return new MethodAndArgsCaller(m, argv);
            }
            主要是以反射的方式找到SystemServer的main方法，并以main对应的Method作为参数返回一个MethodAndArgsCaller对象（实现了Runnable接口）
            
        MethodAndArgsCaller类：
            MethodAndArgsCaller类实现run方法中以反射方式执行main方法
            mMethod.invoke(null, new Object[] { mArgs }); 
            方法参数为上面的args.startArgs
        
    forkSystemServer返回的Runnable不为空，即fork成功，此时在子进程中，后续执行Runnable的run方法，即以反射方式执行上面的 SystemServer 的main方法
    forSystemServer返回null，表示还在zygote进程，继续执行后面的 runSelectLoop 进入循环
    到此SystemServer进程启动成功
    
    SystemServer的main方法：
        public static void main(String[] args) {
            new SystemServer().run();
        }
    
    run方法（主要启动一些系统服务）：
        private void run() {
            ...     //设置时区、时间、语言等
            VMRuntime.getRuntime().clearGrowthLimit();      //more memory!
            VMRuntime.getRuntime().setTargetHeapUtilization(0.8f);//内存利用率，SystemServer一直在运行，所有得尽可能有效的利用内存
            BinderInternal.disableBackgroundScheduling(true);   //确保对系统的 binder 调用始终以前台优先级运行。
            BinderInternal.setMaxThreads(sMaxBinderThreads);    //增加system_server中绑定器线程的数量，设置最大值默认为31
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_FOREGROUND);//根据Linux的优先级设置调用线程的优先级，从高到低为-20到19，当前设为-2
            android.os.Process.setCanSelfBackground(false); //如果传递后台线程优先级，则使用“false”调用将导致对setThreadPriority(int)的未来调用引发异常。只有在构建JNI层时将GUARD_THREAD_PRIORITY定义为1时，这才有效
            Looper.prepareMainLooper();     //准备主线程的 main looper
            Looper.getMainLooper().setSlowLogThresholdMs(SLOW_DISPATCH_THRESHOLD_MS, SLOW_DELIVERY_THRESHOLD_MS);   //为slow dispatch/delivery日志设置阈值，分别为100/200
            System.loadLibrary("android_servers");      //初始化native服务，加载libandroid_servers.so
            performPendingShutdown();       //和重启有关
            createSystemContext();      // activityThread.getSystemContext
            mSystemServiceManager = new SystemServiceManager(mSystemContext);   //创建系统服务管理器
            mSystemServiceManager.setStartInfo(mRuntimeRestart, mRuntimeStartElapsedTime, mRuntimeStartUptime);
            LocalServices.addService(SystemServiceManager.class, mSystemServiceManager);    //将指定接口的服务实例添加到本地服务的全局注册表
            SystemServerInitThreadPool.get();       //为可以并行化的 init tasks 准备线程池
            
            startBootstrapServices();   //启动系统所需的一些小的关键服务。这些服务具有复杂的相互依赖关系，这就是我们在这里将它们全部初始化的原因。除非您的服务也缠绕在这些依赖项中，否则应该在其他函数中初始化它。
            startCoreServices();        //启动一些基本的服务，这些服务不会在引导过程中出现混乱。
            startOtherServices();       //其他服务
            SystemServerInitThreadPool.shutdown();      //关闭上面的线程池
            ...
            Looper.loop();      // Loop forever.
        }
        LocalServices这个类的使用方式与ServiceManager类似，只是这里注册的服务不是绑定器对象，并且只能在相同的进程中使用。一旦所有服务都转换为SystemService接口，这个类就可以被吸收到SystemServiceManager中
        SystemServiceManager对系统的服务进行创建、启动和生命周期管理。启动系统的各种服务
        SystemServerInitThreadPool 在系统服务器初始化期间使用的线程池，系统服务可以提交(可运行的)任务，以便在引导期间执行。在SystemService.PHASE_BOOT_COMPLETED之后，池将被关闭。之后不应提交新任务
        SystemServer通过Looper启动一个死循环，不使主线程结束，从而维持进程存在，有其他handler的使用吗？
        Android系统分三种服务：
            BootstrapServices，bootstrap服务有：
                Installer、DeviceIdentifiersPolicyService、ActivityManagerService、
                PowerManagerService、LightsService、DisplayManagerService、PackageManagerService、
                UserManagerService、SensorService（传感器服务）
            CoreServices，核心服务：
                BatteryService、UsageStatsService、WebViewUpdateService、BinderCallsStatsService
            OtherServices，其他服务：
                CameraService、AlarmManagerService、VrManagerService等服务，这些服务的父类为SystemService
    
    startBootstrapServices方法：
        private void startBootstrapServices() {
            Installer installer = mSystemServiceManager.startService(Installer.class); //等待installer完成启动，以便它有机会创建具有适当权限的关键目录，比如/data/user。我们需要在初始化其他服务之前完成此操作。
            mSystemServiceManager.startService(DeviceIdentifiersPolicyService.class); //在某些情况下，在启动应用程序后，我们需要访问设备标识符，因此，在activity manager之前注册设备标识符策略。
            
            //活动管理器负责运行
            mActivityManagerService = mSystemServiceManager.startService(ActivityManagerService.Lifecycle.class).getService();
            mActivityManagerService.setSystemServiceManager(mSystemServiceManager);
            mActivityManagerService.setInstaller(installer);    
            
            //Power manager需要尽早启动，因为其他服务需要它。本机守护进程可能正在监视它的注册，因此它必须准备好立即处理传入的binder调用(包括能够验证这些调用的权限)。
            mPowerManagerService = mSystemServiceManager.startService(PowerManagerService.class);
            //现在power manager已经启动，让活动管理器初始化power管理特性。
            mActivityManagerService.initPowerManagement();
            //启动恢复系统，以防救援队伍需要重新启动
            mSystemServiceManager.startService(RecoverySystemService.class);
            RescueParty.noteBoot(mSystemContext);   //防止重复启动的一个rescue...
            
            mSystemServiceManager.startService(LightsService.class);    //管理led和显示背光，所以我们需要它来打开显示。
            mSystemServiceManager.startService(WEAR_SIDEKICK_SERVICE_CLASS);//
            //需要Display manager在 package manager 启动之前提供显示指标。
            mDisplayManagerService = mSystemServiceManager.startService(DisplayManagerService.class);
            mSystemServiceManager.startBootPhase(SystemService.PHASE_WAIT_FOR_DEFAULT_DISPLAY);//在初始化包管理器之前，我们需要默认的显示。
            
            //启动包管理器。
            mPackageManagerService = PackageManagerService.main(mSystemContext, installer,
                            mFactoryTestMode != FactoryTest.FACTORY_TEST_OFF, mOnlyCore);
            mFirstBoot = mPackageManagerService.isFirstBoot();
            mPackageManager = mSystemContext.getPackageManager();
            
             //管理A/B OTA dexopt。这是一个引导服务，因为我们需要它来重命名启动后的A/B工件，在其他任何东西可能接触/需要它们之前。注意:在解密过程中不需要这样做(反正我们没有/数据)。
            OtaDexoptService.main(mSystemContext, mPackageManagerService);
            
            mSystemServiceManager.startService(UserManagerService.LifeCycle.class);
            AttributeCache.init(mSystemContext);//初始化用于缓存包中的资源的属性缓存
            
            mActivityManagerService.setSystemProcess();//为System进程设置应用程序实例并开始
            mDisplayManagerService.setupSchedulerPolicies();//DisplayManagerService需要设置android.display调度相关策略，因为setSystemProcess()会覆盖由于setProcessGroup而导致的策略
            
            mSystemServiceManager.startService(new OverlayManagerService(mSystemContext, installer));//管理覆盖包
            //传感器服务需要访问包管理器服务，app ops服务和权限服务，因此我们在它们之后启动它。在单独的线程中启动传感器服务。应在使用之前检查完成情况
            mSensorServiceStart = SystemServerInitThreadPool.get().submit(() -> {
                        startSensorService();/*传感器服务native方法*/ }, START_SENSOR_SERVICE); 
        }
    
    startCoreServices方法：    
        private void startCoreServices() {
            mSystemServiceManager.startService(BatteryService.class);   //跟踪电池电量。需要LightService。
            mSystemServiceManager.startService(UsageStatsService.class);    //跟踪应用程序使用情况。
            mActivityManagerService.setUsageStatsManager(LocalServices.getService(UsageStatsManagerInternal.class));
            
            //跟踪可更新的WebView是否处于就绪状态，并监视更新安装。
            if (mPackageManager.hasSystemFeature(PackageManager.FEATURE_WEBVIEW)) {
                mWebViewUpdateService = mSystemServiceManager.startService(WebViewUpdateService.class);
            }
            
            BinderCallsStatsService.start();    //跟踪binder调用中花费的cpu时间
        }
        
    startOtherServices方法：
        开始一个混乱的包，里面的东西还没有被重构和组织
        private void startOtherServices() {
            boolean isEmulator = SystemProperties.get("ro.kernel.qemu").equals("1");    //判断是否是虚拟机
            ServiceManager.addService("sec_key_att_app_id_provider",new KeyAttestationApplicationIdProviderService(context));
            mSystemServiceManager.startService(KeyChainSystemService.class);
            ServiceManager.addService("scheduling_policy", new SchedulingPolicyService());
            
            mSystemServiceManager.startService(TelecomLoaderService.class);
            telephonyRegistry = new TelephonyRegistry(context);
            ServiceManager.addService("telephony.registry", telephonyRegistry);
            
            mSystemServiceManager.startService(ACCOUNT_SERVICE_CLASS);  //AccountManager必须于ContentService之前加载
            mSystemServiceManager.startService(CONTENT_SERVICE_CLASS);  //ContentService
            
            mActivityManagerService.installSystemProviders();
            // Now that SettingsProvider is ready, reactivate SQLiteCompatibilityWalFlags
            SQLiteCompatibilityWalFlags.reset();
            //记录错误和日志，例如wtf()。目前，此服务间接地依赖于SettingsProvider，所以在InstallSystemProviders之后执行此操作
            mSystemServiceManager.startService(DropBoxManagerService.class);
            
            vibrator = new VibratorService(context);
            ServiceManager.addService("vibrator", vibrator);    //提示音服务
            consumerIr = new ConsumerIrService(context);
            ServiceManager.addService(Context.CONSUMER_IR_SERVICE, consumerIr); //StartConsumerIrService
            mSystemServiceManager.startService(AlarmManagerService.class);  //闹钟服务
            
            final Watchdog watchdog = Watchdog.getInstance();
            watchdog.init(context, mActivityManagerService);
            
            inputManager = new InputManagerService(context);    //InputManagerService
            // WMS needs sensor service ready
            ConcurrentUtils.waitForFutureNoInterrupt(mSensorServiceStart, START_SENSOR_SERVICE);
            mSensorServiceStart = null;
            wm = WindowManagerService.main(context, inputManager,
                    mFactoryTestMode != FactoryTest.FACTORY_TEST_LOW_LEVEL,
                    !mFirstBoot, mOnlyCore, new PhoneWindowManager());
            ServiceManager.addService(Context.WINDOW_SERVICE, wm, /* allowIsolated= */ false,
                                DUMP_FLAG_PRIORITY_CRITICAL | DUMP_FLAG_PROTO); //启动WindowManagerService
            ServiceManager.addService(Context.INPUT_SERVICE, inputManager,
                                /* allowIsolated= */ false, DUMP_FLAG_PRIORITY_CRITICAL);
            mActivityManagerService.setWindowManager(wm);
            wm.onInitReady();                    
            //开始接收来自HIDL服务的呼叫。从一个单独的线程开始，因为它需要连接到SensorManager。这必须在START_SENSOR_SERVICE完成之后开始
            SystemServerInitThreadPool.get().submit(() -> {
                            startHidlServices(); }, START_HIDL_SERVICES);
                            
            mSystemServiceManager.startService(VrManagerService.class); //VR服务
            inputManager.setWindowManagerCallbacks(wm.getInputMonitor());
            inputManager.start();                //StartInputManager
            
            mDisplayManagerService.windowManagerAndInputReady();
            mSystemServiceManager.startService(BluetoothService.class); //虚拟机中无蓝牙服务，会跳过启动步骤
            mSystemServiceManager.startService(IpConnectivityMetrics.class);    //IpConnectivityMetrics
            mSystemServiceManager.startService(NetworkWatchlistService.Lifecycle.class);    //NetworkWatchlistService
            mSystemServiceManager.startService(PinnerService.class);    //PinnerService
            
            //打开UI所需的服务
            mSystemServiceManager.startService(InputMethodManagerService.Lifecycle.class);  //输入法
            ServiceManager.addService(Context.ACCESSIBILITY_SERVICE, new AccessibilityManagerService(context));
            wm.displayReady();  //MakeDisplayReady
            
            //NotificationManagerService依赖于StorageManagerService(用于媒体/ usb通知)，因此我们必须首先启动StorageManagerService。
            mSystemServiceManager.startService(STORAGE_MANAGER_SERVICE_CLASS);
            storageManager = IStorageManager.Stub.asInterface(ServiceManager.getService("mount"));
            mSystemServiceManager.startService(STORAGE_STATS_SERVICE_CLASS);
            
            //我们从这里开始，以便更新配置以设置适当的watch或television
            mSystemServiceManager.startService(UiModeManagerService.class);     //StartUiModeManager
            mPackageManagerService.updatePackagesIfNeeded();
            mPackageManagerService.performFstrimIfNeeded();
            mSystemServiceManager.startService(LOCK_SETTINGS_SERVICE_CLASS);
            lockSettings = ILockSettings.Stub.asInterface(ServiceManager.getService("lock_settings"));
            mSystemServiceManager.startService(PersistentDataBlockService.class);   //StartPersistentDataBlock
            mSystemServiceManager.startService(OemLockService.class);   //StartOemLockService,实现依赖于pdb或OemLock HAL
            mSystemServiceManager.startService(DeviceIdleController.class);     //StartDeviceIdleController
            mSystemServiceManager.startService(DevicePolicyManagerService.Lifecycle.class); //总是启动设备策略管理器
            statusBar = new StatusBarManagerService(context, wm);
            ServiceManager.addService(Context.STATUS_BAR_SERVICE, statusBar);   //状态栏
            mSystemServiceManager.startService(ClipboardService.class);     //剪切板
            networkManagement = NetworkManagementService.create(context);
            ServiceManager.addService(Context.NETWORKMANAGEMENT_SERVICE, networkManagement);//StartNetworkManagementService
            ipSecService = IpSecService.create(context);
            ServiceManager.addService(Context.IPSEC_SERVICE, ipSecService);     //ipsec服务
            
            mSystemServiceManager.startService(TextServicesManagerService.Lifecycle.class);
            mSystemServiceManager.startService(TextClassificationManagerService.Lifecycle.class);
            mSystemServiceManager.startService(NetworkScoreService.Lifecycle.class);
            networkStats = NetworkStatsService.create(context, networkManagement);
            ServiceManager.addService(Context.NETWORK_STATS_SERVICE, networkStats); //网络状态
            networkPolicy = new NetworkPolicyManagerService(context, mActivityManagerService,networkManagement);
            ServiceManager.addService(Context.NETWORK_POLICY_SERVICE, networkPolicy);
            mSystemServiceManager.startService(WIFI_SERVICE_CLASS);     //wifi
            mSystemServiceManager.startService("com.android.server.wifi.scanner.WifiScanningService");  //WiFi扫描
            mSystemServiceManager.startService("com.android.server.wifi.rtt.RttService");   //wifi rtt
            mSystemServiceManager.startService(WIFI_AWARE_SERVICE_CLASS);
            mSystemServiceManager.startService(WIFI_P2P_SERVICE_CLASS);     //p2p
            mSystemServiceManager.startService(LOWPAN_SERVICE_CLASS);       //域网
            mSystemServiceManager.startService(ETHERNET_SERVICE_CLASS);     //以太网
            connectivity = new ConnectivityService(
                                context, networkManagement, networkStats, networkPolicy);   //网络连接
            ServiceManager.addService(Context.CONNECTIVITY_SERVICE, connectivity,/* allowIsolated= */ false,
                                DUMP_FLAG_PRIORITY_HIGH | DUMP_FLAG_PRIORITY_NORMAL);
            networkStats.bindConnectivityManager(connectivity);
            networkPolicy.bindConnectivityManager(connectivity);
            
            serviceDiscovery = NsdService.create(context);
            ServiceManager.addService(Context.NSD_SERVICE, serviceDiscovery);   //StartNsdService
            ServiceManager.addService(Context.SYSTEM_UPDATE_SERVICE,new SystemUpdateManagerService(context));//系统更新
            ServiceManager.addService(Context.UPDATE_LOCK_SERVICE, new UpdateLockService(context));
            mSystemServiceManager.startService(NotificationManagerService.class);   //通知服务
            SystemNotificationChannels.createAll(context);
            notification = INotificationManager.Stub.asInterface(ServiceManager.getService(Context.NOTIFICATION_SERVICE));
            mSystemServiceManager.startService(DeviceStorageMonitorService.class);
            location = new LocationManagerService(context);
            ServiceManager.addService(Context.LOCATION_SERVICE, location);  //位置服务
            countryDetector = new CountryDetectorService(context);
            ServiceManager.addService(Context.COUNTRY_DETECTOR, countryDetector);
            mSystemServiceManager.startService(SEARCH_MANAGER_SERVICE_CLASS);   //搜索服务
            mSystemServiceManager.startService(WALLPAPER_SERVICE_CLASS);    //壁纸
            mSystemServiceManager.startService(AudioService.Lifecycle.class);   //多媒体服务
            mSystemServiceManager.startService(BroadcastRadioService.class);    //广播
            mSystemServiceManager.startService(DockObserver.class);
            mSystemServiceManager.startService(THERMAL_OBSERVER_CLASS);     //主题监听服务
            inputManager.setWiredAccessoryCallbacks(new WiredAccessoryManager(context, inputManager));  //监听有线耳机的变化
            mSystemServiceManager.startService(MIDI_SERVICE_CLASS);     //MIDI Manager
            mSystemServiceManager.startService(USB_SERVICE_CLASS);      //USB服务，虚拟机中有启动
            serial = new SerialService(context);
            ServiceManager.addService(Context.SERIAL_SERVICE, serial);  //串口支持
            hardwarePropertiesService = new HardwarePropertiesManagerService(context);
            ServiceManager.addService(Context.HARDWARE_PROPERTIES_SERVICE, hardwarePropertiesService);
            mSystemServiceManager.startService(TwilightService.class);
            mSystemServiceManager.startService(ColorDisplayService.class);  //StartNightDisplay
            
            mSystemServiceManager.startService(JobSchedulerService.class);  //JobScheduler
            mSystemServiceManager.startService(SoundTriggerService.class);
            mSystemServiceManager.startService(TrustManagerService.class);
            mSystemServiceManager.startService(BACKUP_MANAGER_SERVICE_CLASS);   //备份管理
            mSystemServiceManager.startService(APPWIDGET_SERVICE_CLASS);    //StartAppWidgerService
            mSystemServiceManager.startService(VOICE_RECOGNITION_MANAGER_SERVICE_CLASS);    //需要一直启动这项服务
            mSystemServiceManager.startService(GestureLauncherService.class);   //手势
            mSystemServiceManager.startService(SensorNotificationService.class);    //StartSensorNotification
            mSystemServiceManager.startService(ContextHubSystemService.class);
            ServiceManager.addService("diskstats", new DiskStatsService(context));
            mSystemServiceManager.startService(TIME_ZONE_RULES_MANAGER_SERVICE_CLASS);  //此服务要求JobSchedulerService先启动
            networkTimeUpdater = new NetworkTimeUpdateService(context);
            ServiceManager.addService("network_time_update_service", networkTimeUpdater);   //更新网络时间
            commonTimeMgmtService = new CommonTimeManagementService(context);
            ServiceManager.addService("commontime_management", commonTimeMgmtService);  //StartCommonTimeManagementService
            
            CertBlacklister blacklister = new CertBlacklister(context);
            mSystemServiceManager.startService(EmergencyAffordanceService.class);
            mSystemServiceManager.startService(DreamManagerService.class);  //交互式空闲时间视图、a/k/a屏幕保护程序和打瞌睡模式
            ServiceManager.addService(GraphicsStatsService.GRAPHICS_STATS_SERVICE,new GraphicsStatsService(context));
            ServiceManager.addService(CoverageService.COVERAGE_SERVICE, new CoverageService());
            mSystemServiceManager.startService(PRINT_MANAGER_SERVICE_CLASS);    //打印服务
            mSystemServiceManager.startService(COMPANION_DEVICE_MANAGER_SERVICE_CLASS);
            mSystemServiceManager.startService(RestrictionsManagerService.class);
            mSystemServiceManager.startService(MediaSessionService.class);
            mSystemServiceManager.startService(MediaUpdateService.class);
            mSystemServiceManager.startService(HdmiControlService.class);
            mSystemServiceManager.startService(TvInputManagerService.class);
            mSystemServiceManager.startService(MediaResourceMonitorService.class);
            mSystemServiceManager.startService(TvRemoteService.class);
            mediaRouter = new MediaRouterService(context);
            ServiceManager.addService(Context.MEDIA_ROUTER_SERVICE, mediaRouter);
            mSystemServiceManager.startService(FingerprintService.class);       //StartFingerprintSensor
            BackgroundDexOptService.schedule(context);      //StartBackgroundDexOptService
            PruneInstantAppsJobService.schedule(context);
            mSystemServiceManager.startService(ShortcutService.Lifecycle.class);    //LauncherAppsService uses ShortcutService.
            mSystemServiceManager.startService(LauncherAppsService.class);
            mSystemServiceManager.startService(CrossProfileAppsService.class);
            mSystemServiceManager.startService(MediaProjectionManagerService.class);
            mSystemServiceManager.startService(WEAR_CONFIG_SERVICE_CLASS);
            mSystemServiceManager.startService(WEAR_CONNECTIVITY_SERVICE_CLASS);    //可穿戴设备
            mSystemServiceManager.startService(WEAR_DISPLAY_SERVICE_CLASS);
            mSystemServiceManager.startService(WEAR_TIME_SERVICE_CLASS);
            mSystemServiceManager.startService(WEAR_LEFTY_SERVICE_CLASS);
            mSystemServiceManager.startService(WEAR_GLOBAL_ACTIONS_SERVICE_CLASS);
            mSystemServiceManager.startService(SLICE_MANAGER_SERVICE_CLASS);
            mSystemServiceManager.startService(CameraServiceProxy.class);   //相机代理
            mSystemServiceManager.startService(IOT_SERVICE_CLASS);
            mSystemServiceManager.startService(StatsCompanionService.Lifecycle.class);
            wm.detectSafeMode();
            VMRuntime.getRuntime().disableJitCompilation();     //禁用system_server进程的JIT
            VMRuntime.getRuntime().startJitCompilation();       //为system_server进程启用JIT
            mmsService = mSystemServiceManager.startService(MmsServiceBroker.class);
            mSystemServiceManager.startService(AUTO_FILL_MANAGER_SERVICE_CLASS);
            
            // 现在是时候启动应用程序进程了……
            vibrator.systemReady();
            lockSettings.systemReady();
            mSystemServiceManager.startBootPhase(SystemService.PHASE_LOCK_SETTINGS_READY);//DevicePolicyManager需要初始化
            mSystemServiceManager.startBootPhase(SystemService.PHASE_SYSTEM_SERVICES_READY);
            wm.systemReady();
            // 手动更新这个上下文的配置，因为我们要要在wm.systemReady()中完成的配置更改传播到它之前开始使用它。
            final Configuration config = wm.computeNewConfiguration(DEFAULT_DISPLAY);
            DisplayMetrics metrics = new DisplayMetrics();
            WindowManager w = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
            w.getDefaultDisplay().getMetrics(metrics);
            context.getResources().updateConfiguration(config, metrics);
            // 系统上下文的主题可能与配置有关
            final Theme systemTheme = context.getTheme();
            if (systemTheme.getChangingConfigurations() != 0) {
                systemTheme.rebase();
            }
            mPowerManagerService.systemReady(mActivityManagerService.getAppOpsService());
            mPackageManagerService.systemReady();
            mDisplayManagerService.systemReady(safeMode, mOnlyCore);
            mSystemServiceManager.setSafeMode(safeMode);
            
            //启动特定于设备的服务
            final String[] classes = mSystemContext.getResources().getStringArray(R.array.config_deviceSpecificSystemServices);
            for (final String className : classes) {
                mSystemServiceManager.startService(className);
            }
            mSystemServiceManager.startBootPhase(SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
            
            //现在，activity manager可以运行第三方代码了。一旦到达第三方代码可以真正运行的状态时（但在它真正开始启动初始应用程序之前），它就会给我们反馈，以完成初始化
            mActivityManagerService.systemReady(() -> {
                mSystemServiceManager.startBootPhase(SystemService.PHASE_ACTIVITY_MANAGER_READY);
                mActivityManagerService.startObservingNativeCrashes();
                
                //在SystemServer中不依赖Webview的预准备。但这应该在允许第三方之前完成
                webviewPrep = SystemServerInitThreadPool.get().submit(() -> {
                        ConcurrentUtils.waitForFutureNoInterrupt(mZygotePreload, "Zygote preload");
                        mZygotePreload = null;
                        mWebViewUpdateService.prepareWebViewInSystemServer();
                    }, WEBVIEW_PREPARATION);
                    
                mSystemServiceManager.startService(CAR_SERVICE_HELPER_SERVICE_CLASS);   //车载服务
                startSystemUi(context, windowManagerF);     //启动系统UI服务，SystemUIService
                networkManagementF.systemReady();   //Make Network Management Service Ready
                CountDownLatch networkPolicyInitReadySignal = null;
                if (networkPolicyF != null) 
                    networkPolicyInitReadySignal = networkPolicyF.networkScoreAndNetworkManagementServiceReady();
                ipSecServiceF.systemReady();
                networkStatsF.systemReady();    //MakeNetworkStatsServiceReady
                connectivityF.systemReady();    //MakeConnectivityServiceReady
                networkPolicyF.systemReady(networkPolicyInitReadySignal);
                Watchdog.getInstance().start();
                mPackageManagerService.waitForAppDataPrepared();    //等待所有的包准备好
                
                //现在可以让各种系统服务启动它们的第三方代码了……
                ConcurrentUtils.waitForFutureNoInterrupt(webviewPrep, WEBVIEW_PREPARATION); //在启动第三方之前确认webview完成
                mSystemServiceManager.startBootPhase(SystemService.PHASE_THIRD_PARTY_APPS_CAN_START);
                locationF.systemRunning();      //MakeLocationServiceReady
                countryDetectorF.systemRunning();   //MakeCountryDetectionServiceReady
                networkTimeUpdaterF.systemRunning();    //MakeNetworkTimeUpdateReady
                commonTimeMgmtServiceF.systemRunning();
                inputManagerF.systemRunning();
                telephonyRegistryF.systemRunning();
                mediaRouterF.systemRunning();       //MakeMediaRouterServiceReady
                mmsServiceF.systemRunning();        //MakeMmsServiceReady
                //TODO:Switch from checkService to getService once it's always in the build and should reliably be there.
                IIncidentManager incident = IIncidentManager.Stub.asInterface(
                                        ServiceManager.getService(Context.INCIDENT_SERVICE));
                incident.systemRunning();       //Incident Daemon Ready，守护进程
            }, BOOT_TIMINGS_TRACE_LOG);
        }
        
    通过SystemServiceManager.startService启动的系统服务都是SystemService的子类
    都是以反射的方式创建实例，最后调用 onStart方法启动：
        public <T extends SystemService> T startService(Class<T> serviceClass) {
            String name = serviceClass.getName();
            if (!SystemService.class.isAssignableFrom(serviceClass)) {      //必须继承SystemService
                throw new RuntimeException("Failed to create " + name + ": service must extend " + SystemService.class.getName());
            }
            Constructor<T> constructor = serviceClass.getConstructor(Context.class);
            service = constructor.newInstance(mContext);
            startService(service);
            return service;
        }
        public void startService(@NonNull final SystemService service) {
            mServices.add(service);     // Register it.添加到列表中
            long time = SystemClock.elapsedRealtime();
            service.onStart();          // Start it.                       
        }
    SystemService的声明周期：
        构造函数：提供系统上下文来初始化系统服务
        onStart()：使服务运行。其中服务应该使用publishBinderService(String, IBinder)发布其binder接口
                    它还可以发布其他本地接口，SystemServer中的其他服务可以使用这些接口访问享有特权的内部函数。
        onBootPhase(int)：
            调用onBootPhase(int)的次数与启动阶段的次数相同，直到发送PHASE_BOOT_COMPLETED，这是最后一个启动阶段
            每个阶段都是进行特殊工作的机会，比如获取可选的服务依赖项、等待是否启用SafeMode，或者向在此之后启动的服务注册
    
    除了以SystemService.onStart方法启动，还有以服务的main方法启动的，比如 PackageManagerService.main(...)
    一般要将Service注册到ServiceManager中，ServiceManager用来管理系统中的各种Service，
    用于系统C/S架构中的Binder机制通信：
        Client端要使用某个Service，则需要先到ServiceManager查询Service的相关信息，
        然后根据Service的相关信息与Service所在的Server进程建立通讯通路，这样Client端就可以使用Service了。
    还有的服务是直接注册到ServiceManager中的：ServiceManager.addService("telephony.registry", telephonyRegistry);
       
    SystemServer在启动时做了如下工作：
        1、启动Binder线程池，这样就可以与其他进程进行通信。
        2、创建SystemServiceManager用于对系统的服务进行创建、启动和生命周期管理。
        3、启动各种系统服务。
        
#### Launcher启动过程

[AMS源码地址](http://androidxref.com/9.0.0_r3/xref/frameworks/base/services/core/java/com/android/server/am/ActivityManagerService.java)    

    Launcher介绍：
        Android系统启动的最后一步是启动一个Home应用程序，这个应用程序是用来显示系统中已经安装的应用程序，这个Home应用程序就叫做Launcher。
        应用程序Launcher在启动过程中会请求PackageManagerService返回系统中已经安装的应用程序的信息，
        并将这些信息封装成一个快捷图标列表显示在系统屏幕上，这样用户可以通过点击这些快捷图标来启动相应的应用程序。
    
    SystemServer进程在启动的过程中会启动PackageManagerService，PackageManagerService启动后会将系统中的应用程序安装完成。
    在此前已经启动的ActivityManagerService会将Launcher启动起来。
    启动Launcher的入口为ActivityManagerService的systemReady函数（SystemsServer的startOtherServices方法中）：
        mActivityManagerService.systemReady(() -> {
            mSystemServiceManager.startBootPhase(SystemService.PHASE_ACTIVITY_MANAGER_READY);
            mActivityManagerService.startObservingNativeCrashes();
            ...
        }, BOOT_TIMINGS_TRACE_LOG);
    ActivityManagerService的systemReady方法：
        synchronized(this) {
            if (mSystemReady) {
                //如果已经调用了所有的receiver，接着执行从SystemServer传递过来的 boot phase
                if (goingCallback != null) {
                    goingCallback.run();
                }
                return;
            }
            mHasHeavyWeightFeature = mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CANT_SAVE_STATE);
            mLocalDeviceIdleController= LocalServices.getService(DeviceIdleController.LocalService.class);
            mAssistUtils = new AssistUtils(mContext);
            mVrController.onSystemReady();
            // 确保我们有当前的profile信息，因为它是安全检查所需要的。
            mUserController.onSystemReady();
            mRecentTasks.onSystemReadyLocked();
            mAppOpsService.systemReady();
            mSystemReady = true;
        }
        sTheRealBuildSerial = IDeviceIdentifiersPolicyService.Stub.asInterface(
                            ServiceManager.getService(Context.DEVICE_IDENTIFIERS_SERVICE)).getSerial();
        ArrayList<ProcessRecord> procsToKill = null;
        synchronized(mPidsSelfLocked) {
            for (int i=mPidsSelfLocked.size()-1; i>=0; i--) {       遍历mPidsSelfLocked这个列表
                ProcessRecord proc = mPidsSelfLocked.valueAt(i);
                if (!isAllowedWhileBooting(proc.info)){             判断元素ProcessRecord 是否满足某种条件
                    if (procsToKill == null) {
                        procsToKill = new ArrayList<ProcessRecord>();
                    }
                    procsToKill.add(proc);                          将满足条件的元素加入另一个列表procsToKill中，貌似是要杀死的进程信息？
                }
            }
        }
        synchronized(this) {
            if (procsToKill != null) {
                for (int i=procsToKill.size()-1; i>=0; i--) {
                    ProcessRecord proc = procsToKill.get(i);
                    removeProcessLocked(proc, true, false, "system update done");   Removing system update proc
                }
            }        
            //现在我们已经清理了所有的更新过程，我们已经准备好启动真正的进程，并且知道我们不会再践踏它们。
            mProcessesReady = true;
        }
        synchronized(this) {
            //确保我们没有现成的processes
            if (mFactoryTest == FactoryTest.FACTORY_TEST_LOW_LEVEL) {
                ResolveInfo ri = mContext.getPackageManager().resolveActivity(new Intent(Intent.ACTION_FACTORY_TEST),STOCK_PM_FLAGS);
                CharSequence errorMsg = null;
                if (ri != null) {
                    ActivityInfo ai = ri.activityInfo;
                    ApplicationInfo app = ai.applicationInfo;
                    if ((app.flags&ApplicationInfo.FLAG_SYSTEM) != 0) {     判断系统应用？
                        mTopAction = Intent.ACTION_FACTORY_TEST;
                        mTopData = null;
                        mTopComponent = new ComponentName(app.packageName,ai.name);
                    } else {
                        errorMsg = mContext.getResources().getText(com.android.internal.R.string.factorytest_not_system);
                    }
                } else {
                    errorMsg = mContext.getResources().getText(com.android.internal.R.string.factorytest_no_action);
                }
                if (errorMsg != null) {
                    mTopAction = null;
                    mTopData = null;
                    mTopComponent = null;
                    Message msg = Message.obtain();
                    msg.what = SHOW_FACTORY_ERROR_UI_MSG;
                    msg.getData().putCharSequence("msg", errorMsg);
                    mUiHandler.sendMessage(msg);                        发送错误信息？
                }
            }
        }
        retrieveSettings();
        final int currentUserId = mUserController.getCurrentUserId();
        synchronized (this) {
            readGrantedUriPermissionsLocked();          权限相关
        }
        final PowerManagerInternal pmi = LocalServices.getService(PowerManagerInternal.class);
        if (pmi != null) {
            pmi.registerLowPowerModeObserver(ServiceType.FORCE_BACKGROUND_CHECK,
                            state -> updateForceBackgroundCheck(state.batterySaverEnabled));
            updateForceBackgroundCheck(
                            pmi.getLowPowerState(ServiceType.FORCE_BACKGROUND_CHECK).batterySaverEnabled);
        } else {
                    Slog.wtf(TAG, "PowerManagerInternal not found.");
        }
        if (goingCallback != null) goingCallback.run();             执行从SystemServer传递过来的 boot phase
        traceLog.traceBegin("ActivityManagerStartApps");
        mBatteryStatsService.noteEvent(BatteryStats.HistoryItem.EVENT_USER_RUNNING_START,Integer.toString(currentUserId), currentUserId);
        mBatteryStatsService.noteEvent(BatteryStats.HistoryItem.EVENT_USER_FOREGROUND_START,Integer.toString(currentUserId), currentUserId);
        mSystemServiceManager.startUser(currentUserId);
        synchronized (this) {
            //只启动支持加密的持久应用程序，一旦用户被解锁，我们将回来，并启动unaware的应用程序
            startPersistentApps(PackageManager.MATCH_DIRECT_BOOT_AWARE);
            mBooting = true;        启动最初的activity
            //为系统用户启用home activity，以便系统始终可以引导。当未设置系统用户时，我们不这样做，因为在这种情况下，设置向导应该去处理home activity
            if (UserManager.isSplitSystemUser() && Settings.Secure.getInt(mContext.getContentResolver(),
                                     Settings.Secure.USER_SETUP_COMPLETE, 0) != 0) {
                ComponentName cName = new ComponentName(mContext, SystemUserHomeActivity.class);
                AppGlobals.getPackageManager().setComponentEnabledSetting(cName,
                                     PackageManager.COMPONENT_ENABLED_STATE_ENABLED, 0, UserHandle.USER_SYSTEM);
            }
            startHomeActivityLocked(currentUserId, "systemReady");
            if (AppGlobals.getPackageManager().hasSystemUidErrors()) {
                Slog.e(TAG, "UIDs on the system are inconsistent, you need to wipe your data partition or your device will be unstable.");
                mUiHandler.obtainMessage(SHOW_UID_ERROR_UI_MSG).sendToTarget();
            }
            if (!Build.isBuildConsistent()) {
                Slog.e(TAG, "Build fingerprint is not consistent, warning user");
                mUiHandler.obtainMessage(SHOW_FINGERPRINT_ERROR_UI_MSG).sendToTarget();
            }
            long ident = Binder.clearCallingIdentity();
            Intent intent = new Intent(Intent.ACTION_USER_STARTED);
            intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY | Intent.FLAG_RECEIVER_FOREGROUND);
            intent.putExtra(Intent.EXTRA_USER_HANDLE, currentUserId);
            broadcastIntentLocked(null, null, intent, null, null, 0, null, null, null, OP_NONE,
                                    null, false, false, MY_PID, SYSTEM_UID, currentUserId);     发广播
            intent = new Intent(Intent.ACTION_USER_STARTING);
            intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
            intent.putExtra(Intent.EXTRA_USER_HANDLE, currentUserId);
            broadcastIntentLocked(null, null, intent, null, new IIntentReceiver.Stub() {
                @Override
                public void performReceive(Intent intent, int resultCode, String data, Bundle extras, 
                        boolean ordered, boolean sticky, int sendingUser) throws RemoteException {
                                        }
            }, 0, null, null, new String[] {INTERACT_ACROSS_USERS}, OP_NONE, null, true, false, MY_PID, SYSTEM_UID, UserHandle.USER_ALL);
            Binder.restoreCallingIdentity(ident);
            mStackSupervisor.resumeFocusedStackTopActivityLocked();             重点？
            mUserController.sendUserSwitchBroadcasts(-1, currentUserId);        用户切换广播？
            
            BinderInternal.nSetBinderProxyCountWatermarks(6000,5500);
            BinderInternal.nSetBinderProxyCountEnabled(true);
            BinderInternal.setBinderProxyCountCallback(new BinderInternal.BinderProxyLimitListener() {
                @Override
                public void onLimitReached(int uid) {
                    Slog.wtf(TAG, "Uid " + uid + " sent too many Binders to uid " + Process.myUid());
                    if (uid == Process.SYSTEM_UID) {
                        Slog.i(TAG, "Skipping kill (uid is SYSTEM)");
                    } else {
                        killUid(UserHandle.getAppId(uid), UserHandle.getUserId(uid), "Too many Binders sent to SYSTEM");
                    }
                }
            }, mHandler);
        }
    systemReady函数中调用了ActivityStackSupervisor的resumeFocusedStackTopActivityLocked函数
    通过ActivityStackSupervisor运行所有的ActivityStacks
    ActivityStackSupervisor resumeFocusedStackTopActivityLocked：
        if (!readyToResume()) {     初次应该返回true
            return false;
        }
        if (targetStack != null && isFocusedStack(targetStack)) {       从上面的ActivityManagerService中传过来的为null（系统启动时）
            return targetStack.resumeTopActivityUncheckedLocked(target, targetOptions);
        }
        final ActivityRecord r = mFocusedStack.topRunningActivityLocked();  mFocusedStack当前接收输入或启动下一个activity的堆栈
        if (r == null || !r.isState(RESUMED)) {
            mFocusedStack.resumeTopActivityUncheckedLocked(null, null);     从SystemServer过来可能会执行这里
        } else if (r.isState(RESUMED)) {
            //从MoveTaskToFront操作中启动任何残留的应用程序转换
            mFocusedStack.executeAppTransition(targetOptions);
        }
        return false;
    可能会执行resumeTopActivityUncheckedLocked或者executeAppTransition方法
    ActivityStack对象是用来描述Activity堆栈的
    ActivityStack resumeTopActivityUncheckedLocked：
        确保堆栈顶部的activity处于resumed，如果有东西resumed返回true，否则返回false
        直接调用此方法是不安全的，因为它可能导致非focused堆栈中的activity resumed
        应使用ActivityStackSupervisor.resumeFocusedStackTopActivityLocked resumed当前系统状态的正确activity
        if (mStackSupervisor.inResumeTopActivity) {     默认为false，是为防止该函数递归调用
            return false;       // 不要递归
        }
        boolean result = false;
        mStackSupervisor.inResumeTopActivity = true;        //防止递归
        result = resumeTopActivityInnerLocked(prev, options);   //使栈中的activity resume
        //恢复顶层activity时，可能有必要暂停顶层activity（例如，返回到锁屏。由于顶层activity最后会恢复，
        //因此我们在resumeTopActivityUncheckedLocked中取消了正常的暂停逻辑。 
        //在此处再次调用ActivityStackSupervisor.checkReadyForSleepLocked以确保任何必要的暂停逻辑发生。
        //在不管是否锁屏都要显示activity的情况下，将跳过对ActivityStackSupervisor.checkReadyForSleepLocked的调用。
        final ActivityRecord next = topRunningActivityLocked(true /* focusableOnly */);
        if (next == null || !next.canTurnScreenOn()) {
            checkReadyForSleep();
        }
        mStackSupervisor.inResumeTopActivity = false;       //重置为默认值
        return result;
        
    ActivityStack resumeTopActivityInnerLocked：
        ...
        if (!hasRunningActivity) {
            // 堆栈中没有剩余的activity，看看其他地方
            return resumeTopActivityInNextFocusableStack(prev, options, "noMoreActivities");
        }
        ...
    ActivityStack resumeTopActivityInNextFocusableStack：
        if (adjustFocusToNextFocusableStack(reason)) {      //找到下一个合适的可调焦堆栈并使其聚焦
            // 如果该堆栈未覆盖整个屏幕或在辅助显示器上（没有主堆栈），请尝试将焦点移至具有运行中的activity的下一个可见堆栈
            return mStackSupervisor.resumeFocusedStackTopActivityLocked(mStackSupervisor.getFocusedStack(), prev, null);
        }
        // 现在启动launcher…（未找到可运行的activity）
        ActivityOptions.abort(options);
        if (DEBUG_STATES) 
            Slog.d(TAG_STATES, "resumeTopActivityInNextFocusableStack: " + reason + ", go home");
        if (DEBUG_STACK) 
            mStackSupervisor.validateTopActivitiesLocked();
        //只有在home显示时才resume home
        return isOnHomeDisplay() && mStackSupervisor.resumeHomeStackTask(prev, reason);
    ActivityStackSupervisor resumeHomeStackTask：
        if (!mService.mBooting && !mService.mBooted) {
            return false;
        }
        mHomeStack.moveHomeStackTaskToTop();
        ActivityRecord r = getHomeActivity();
        final String myReason = reason + " resumeHomeStackTask";
        //只有当home activity还没有结束时才可以继续
        if (r != null && !r.finishing) {        //系统boot时launcher还未启动，此处应该为空
            moveFocusableActivityStackToFrontLocked(r, myReason);
            return resumeFocusedStackTopActivityLocked(mHomeStack, prev, null);
        }
        return mService.startHomeActivityLocked(mCurrentUser, myReason);
    ActivityManagerService startHomeActivityLocked：
        Intent intent = getHomeIntent();        创建home intent
        ActivityInfo aInfo = resolveActivityInfo(intent, STOCK_PM_FLAGS, userId);
        if (aInfo != null) {
            intent.setComponent(new ComponentName(aInfo.applicationInfo.packageName, aInfo.name));
            //如果home应用程序当前正在被instrumented，不要这样做
            aInfo = new ActivityInfo(aInfo);
            aInfo.applicationInfo = getAppInfoForUser(aInfo.applicationInfo, userId);
            ProcessRecord app = getProcessRecordLocked(aInfo.processName,aInfo.applicationInfo.uid, true);
            if (app == null || app.instr == null) {         //判断app是否已启动
                intent.setFlags(intent.getFlags() | FLAG_ACTIVITY_NEW_TASK);
                final int resolvedUserId = UserHandle.getUserId(aInfo.applicationInfo.uid);
                // 用于ANR调试，以验证用户activity是否是实际启动的activity
                final String myReason = reason + ":" + userId + ":" + resolvedUserId;
                mActivityStartController.startHomeActivity(intent, aInfo, myReason);
            }
        } else {
            Slog.wtf(TAG, "No home screen found for " + intent, new Throwable());
        }
        return true;
    getHomeIntent函数中创建了Intent，并将mTopAction和mTopData传入。
    mTopAction的值为Intent.ACTION_MAIN，并且如果系统运行模式不是低级工厂模式则将intent的Category设置为Intent.CATEGORY_HOME。
    ActivityManagerService的startHomeActivityLocked函数，假设系统的运行模式不是低级工厂模式，
    判断符合Action为Intent.ACTION_MAIN，Category为Intent.CATEGORY_HOME的应用程序是否已经启动，如果没启动则调用方法启动该应用程序。
    这个被启动的应用程序就是Launcher，因为Launcher的Manifest文件中的intent-filter标签匹配了Action为Intent.ACTION_MAIN，Category为Intent.CATEGORY_HOME
    packages/apps/Launcher2/AndroidManifest.xml：
        ...
        <activity
            android:name="com.android.launcher2.Launcher"
            android:launchMode="singleTask"
            android:clearTaskOnLaunch="true"
            android:stateNotNeeded="true"
            android:resumeWhilePausing="true"
            android:theme="@style/Theme"
            android:windowSoftInputMode="adjustPan"
            android:screenOrientation="nosensor">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.HOME" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.MONKEY"/>
            </intent-filter>
        </activity>
        ...
    看Launcher的 manifest文件，其action为Intent.ACTION_MAIN，Category为Intent.CATEGORY_HOME
    这样Launcher 应用程序就被启动起来了
    
    桌面应用图标显示流程：
        packages/apps/Launcher3/src/com/android/launcher3/Launcher.java：
            onCreate：
                LauncherAppState app = LauncherAppState.getInstance(this);
                mOldConfig = new Configuration(getResources().getConfiguration());
                mModel = app.setLauncher(this);
                if (!mModel.startLoader(currentScreen)) {
                    if (!internalStateHandled) {
                        //如果我们不同步绑定，请在第一页绑定完成时显示淡入淡出的动画
                        mDragLayer.getAlphaProperty(ALPHA_INDEX_LAUNCHER_LOAD).setValue(0);
                    }
                }
                ...
            获取LauncherAppState的实例并调用它的setLauncher函数并将Launcher对象传入，然后调用startLoader函数
        packages/apps/Launcher3/src/com/android/launcher3/LauncherAppState.java：
            LauncherModel setLauncher(Launcher launcher) {
                getLocalProvider(mContext).setLauncherProviderChangeListener(launcher);
                mModel.initialize(launcher);
                return mModel;
            }
        packages/apps/Launcher3/src/com/android/launcher3/LauncherModel.java：
            //将其设置为加载程序的当前启动程序活动对象（Set this as the current Launcher activity object for the loader）
            public void initialize(Callbacks callbacks) {
                synchronized (mLock) {
                    Preconditions.assertUIThread();
                    mCallbacks = new WeakReference<>(callbacks);
                }
            }
            //启动加载程序。如果可能，尝试同步绑定synchronousBindPage
            public boolean startLoader(int synchronousBindPage) {
                //启动加载程序前启用队列。它将在Launcher.finishBindingItems中禁用
                InstallShortcutReceiver.enableInstallQueue(InstallShortcutReceiver.FLAG_LOADER_RUNNING);
                synchronized (mLock) {
                    //如果我们知道线程不会做任何事情，就不要麻烦启动线程
                    if (mCallbacks != null && mCallbacks.get() != null) {
                        final Callbacks oldCallbacks = mCallbacks.get();
                        //从同步加载过程中清除任何挂起的绑定可运行项。
                        mUiExecutor.execute(oldCallbacks::clearPendingBinds);
                        // 如果已经有一个在运行，告诉它停止
                        stopLoader();
                        LoaderResults loaderResults = new LoaderResults(mApp, sBgDataModel,mBgAllAppsList, synchronousBindPage, mCallbacks);
                        if (mModelLoaded && !mIsLoaderTaskRunning) {
                            //将已加载的项集划分为同步绑定的项集，以及正常(异步)绑定的所有其他内容。
                            loaderResults.bindWorkspace();
                            // 现在，继续发布所有应用程序的绑定，因为还有其他问题。
                            loaderResults.bindAllApps();
                            loaderResults.bindDeepShortcuts();
                            loaderResults.bindWidgets();
                            return true;
                        } else {
                            startLoaderForResults(loaderResults);       model应该还未加载，所以应该调用此方法
                        }
                    }
                }
                return false;
            }
            public void startLoaderForResults(LoaderResults results) {
                synchronized (mLock) {
                    stopLoader();
                    mLoaderTask = new LoaderTask(mApp, mBgAllAppsList, sBgDataModel, results);
                    runOnWorkerThread(mLoaderTask);
                }
            }
            //如果从工作线程调用会立即执行runnable，否则会被工作线程的handler post 执行
            private static void runOnWorkerThread(Runnable r) {
                if (sWorkerThread.getThreadId() == Process.myTid()) {
                    r.run();
                } else {
                    sWorker.post(r);    //如果不在工作线程，那发送到属于工作线程的handler处理
                }
            }
        在initialize函数中会将Callbacks，也就是传入的Launcher 封装成一个弱引用对象。
        因此我们得知mCallbacks变量指的就是封装成弱引用对象的Launcher，这个mCallbacks后文会用到它。
        Launcher的onCreate函数又调用了LauncherModel的startLoader函数
        startLoader函数中有调用了startLoaderForResults
        startLoaderForResults函数中运行了LoaderTask，其继承了Runnable
        最终会执行LoaderTask
        packages/apps/Launcher3/src/com/android/launcher3/model/LoaderTask.java：
            run：
                synchronized (this) {
                    if (mStopped) {         如果我们已经停止，请快速跳过。
                        return;
                    }
                }
                LauncherModel.LoaderTransaction transaction = mApp.getModel().beginLoader(this)，
                //第一步开始
                loadWorkspace();        加载工作空间
                verifyNotStopped();
                mResults.bindWorkspace();   绑定工作空间
                sendFirstScreenActiveInstallsBroadcast();  //在第一个屏幕上通知安装程序包带有活动安装的包。发送第一个屏幕广播
                // Take a break，第一步完成，等待空闲
                waitForIdle();
                verifyNotStopped();
                // second step，第二步开始
                loadAllApps();          加载所有的app
                verifyNotStopped();
                mResults.bindAllApps(); 绑定所有的app
                verifyNotStopped();
                updateIconCache();      更新icon缓存
                // Take a break，第二步完成，等待空闲
                waitForIdle();
                verifyNotStopped();
                // third step，第三步开始
                loadDeepShortcuts();    加载deep shortcuts
                verifyNotStopped();
                mResults.bindDeepShortcuts();   绑定deep shortcuts
                // Take a break，第三步完成
                waitForIdle();
                verifyNotStopped();
                // fourth step，第四步开始
                TraceHelper.partitionSection(TAG, "step 4.1: loading widgets");
                mBgDataModel.widgetsModel.update(mApp, null);       加载widgets
                verifyNotStopped();
                mResults.bindWidgets();     绑定widgets
                transaction.commit();
            private void loadAllApps() {
                final List<UserHandle> profiles = mUserManager.getUserProfiles();
                mBgAllAppsList.clear();     // Clear the list of apps
                for (UserHandle user : profiles) {
                    //查询应用程序集
                    final List<LauncherActivityInfo> apps = mLauncherApps.getActivityList(null, user);
                    // 若没有任何App则失败， TODO: Fix this. Only fail for the current user.
                    if (apps == null || apps.isEmpty()) {
                        return;
                    }
                    boolean quietMode = mUserManager.isQuietModeEnabled(user);
                    // Create the ApplicationInfos
                    for (int i = 0; i < apps.size(); i++) {
                        LauncherActivityInfo app = apps.get(i);
                        // 这将构建图标位图
                        mBgAllAppsList.add(new AppInfo(app, user, quietMode), app);
                    }
                }
                if (FeatureFlags.LAUNCHER3_PROMISE_APPS_IN_ALL_APPS) {
                    // 获取所有活动会话并将它们添加到所有应用程序列表中
                    for (PackageInstaller.SessionInfo info :mPackageInstaller.getAllVerifiedSessions()) {
                        mBgAllAppsList.addPromiseApp(mApp.getContext(),
                                PackageInstallerCompat.PackageInstallInfo.fromInstallingState(info));
                    }
                }
                mBgAllAppsList.added = new ArrayList<>();
            }
        Launcher是用工作区的形式来显示系统安装的应用程序的快捷图标，每一个工作区都是来描述一个抽象桌面的，它由n个屏幕组成，
        每个屏幕又分n个单元格，每个单元格用来显示一个应用程序的快捷图标。
        loadWorkspace函数用来加载工作区信息，loadAllApps函数是用来加载系统已经安装的应用程序信息
        loadAllApps函数中，通过mLauncherApps获取所有的app，mBgAllAppsList中将构建的App信息传入（包含icon），在bindAllApps函数中显示
        packages/apps/Launcher3/src/com/android/launcher3/model/LoaderResults.java：
            public void bindAllApps() {
                final ArrayList<AppInfo> list = (ArrayList<AppInfo>) mBgAllAppsList.data.clone();   //浅拷贝，App信息
                Runnable r = new Runnable() {
                    public void run() {
                        Callbacks callbacks = mCallbacks.get();     //mCallbacks为上面传入的Launcher实例
                        if (callbacks != null) {
                            callbacks.bindAllApplications(list);    //见Launcher的bindAllApplications函数
                        }
                    }
                };
                mUiExecutor.execute(r);
            }
        Launcher：
            //添加所有应用程序的图标。方法的实现见LauncherModel.Callbacks
            public void bindAllApplications(ArrayList<AppInfo> apps) {
                mAppsView.getAppsStore().setApps(apps);     //显示app icon？
                if (mLauncherCallbacks != null) {
                    mLauncherCallbacks.bindAllApplications(apps);
                }
            }
        mAppsView，所有应用程序屏幕的主容器视图
        packages/apps/Launcher3/src/com/android/launcher3/allapps/AllAppsContainerView.java：
            public AllAppsStore getAppsStore() {
                return mAllAppsStore;
            }
            protected void onFinishInflate() {
                super.onFinishInflate();
                // 这是一个焦点侦听器，它将焦点从视图代理到列表视图，这是围绕搜索框工作，从获得第一个焦点和显示光标。
                setOnFocusChangeListener((v, hasFocus) -> {
                    if (hasFocus && getActiveRecyclerView() != null) {
                        getActiveRecyclerView().requestFocus();
                    }
                });
                mHeader = findViewById(R.id.all_apps_header);
                rebindAdapters(mUsingTabs, true /* force */);
                mSearchContainer = findViewById(R.id.search_container_all_apps);
                mSearchUiManager = (SearchUiManager) mSearchContainer;
                mSearchUiManager.initialize(this);
            }
            private void rebindAdapters(boolean showTabs, boolean force) {
                if (showTabs == mUsingTabs && !force) {
                    return;
                }
                replaceRVContainer(showTabs);
                mUsingTabs = showTabs;
                mAllAppsStore.unregisterIconContainer(mAH[AdapterHolder.MAIN].recyclerView);
                mAllAppsStore.unregisterIconContainer(mAH[AdapterHolder.WORK].recyclerView);
                if (mUsingTabs) {
                    mAH[AdapterHolder.MAIN].setup(mViewPager.getChildAt(0), mPersonalMatcher);
                    mAH[AdapterHolder.WORK].setup(mViewPager.getChildAt(1), mWorkMatcher);
                    onTabChanged(mViewPager.getNextPage());
                } else {
                    mAH[AdapterHolder.MAIN].setup(findViewById(R.id.apps_list_view), null);
                    mAH[AdapterHolder.WORK].recyclerView = null;
                }
                setupHeader();
                mAllAppsStore.registerIconContainer(mAH[AdapterHolder.MAIN].recyclerView);
                mAllAppsStore.registerIconContainer(mAH[AdapterHolder.WORK].recyclerView);
            }
            public class AdapterHolder {
                void setup(@NonNull View rv, @Nullable ItemInfoMatcher matcher) {
                    appsList.updateItemFilter(matcher);
                    recyclerView = (AllAppsRecyclerView) rv;
                    recyclerView.setEdgeEffectFactory(createEdgeEffectFactory());
                    recyclerView.setApps(appsList, mUsingTabs);         绑定app信息
                    recyclerView.setLayoutManager(layoutManager);
                    recyclerView.setAdapter(adapter);                   绑定adapter
                    recyclerView.setHasFixedSize(true);
                    // No animations will occur when changes occur to the items in this RecyclerView.
                    recyclerView.setItemAnimator(null);
                    FocusedItemDecorator focusedItemDecorator = new FocusedItemDecorator(recyclerView);
                    recyclerView.addItemDecoration(focusedItemDecorator);
                    adapter.setIconFocusListener(focusedItemDecorator.getFocusListener());
                    applyVerticalFadingEdgeEnabled(verticalFadingEdge);
                    applyPadding();
                }
            }
        开机流程，onFinishInflate函数在加载完xml文件时就会调用，
        其内部类AdapterHolder的setup函数中得到AllAppsRecyclerView用来显示App列表，
        并将apps的信息列表传进去，并为AllAppsRecyclerView设置Adapter。这样应用程序快捷图标的列表就会显示在屏幕上。
        到这里Launcher启动流程就算完了
        
        packages/apps/Launcher3/src/com/android/launcher3/allapps/AllAppsStore.java：
        //维护所有应用程序集合的实用程序类
            //设置当前应用程序集
            public void setApps(List<AppInfo> apps) {
                mComponentToAppMap.clear();
                addOrUpdateApps(apps);
            }
            //添加或更新列表中的现有应用程序
            public void addOrUpdateApps(List<AppInfo> apps) {
                for (AppInfo app : apps) {
                    mComponentToAppMap.put(app.toComponentKey(), app);
                }
                notifyUpdate();
            }
            private void notifyUpdate() {
                if (mDeferUpdates) {
                    mUpdatePending = true;
                    return;
                }
                int count = mUpdateListeners.size();
                for (int i = 0; i < count; i++) {
                    mUpdateListeners.get(i).onAppsUpdated();
                }
            }
            
#### Android系统启动流程
    
    1、启动电源以及系统启动
        当电源按下时引导芯片代码开始从预定义的地方（固化在ROM）开始执行。加载引导程序Bootloader到RAM，然后执行。
    2、引导程序BootLoader
        引导程序BootLoader是在Android操作系统开始运行前的一个小程序，它的主要作用是把系统OS拉起来并运行。
    3、Linux内核启动
        内核启动时，设置缓存、被保护存储器、计划列表、加载驱动。当内核完成系统设置，它首先在系统文件中寻找init.rc文件，并启动init进程。
    4、init进程启动
        初始化和启动属性服务，并且启动Zygote进程。
    5、Zygote进程启动
        创建JavaVM并为JavaVM注册JNI，创建服务端Socket，启动SystemServer进程。
    6、SystemServer进程启动
        启动Binder线程池和SystemServiceManager，并且启动各种系统服务。
    7、Launcher启动
        被SystemServer进程启动的ActivityManagerService会启动Launcher，Launcher启动后会将已安装应用的快捷图标显示到界面上。

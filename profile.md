## Code Source Profile

rxJava [使用参考](https://www.jianshu.com/p/9561ebdc5c0b)

##### Retrofit2 [网络请求流程分析](profile/Retrofit2.md)

##### LeakCanary [内存泄漏流程分析](profile/LeakCanary.md)

##### Glide 4.8.0 [源码分析](profile/Glide.md)

##### Handler [消息传递流程分析](profile/Handler.md)

##### 线程池 [线程池源码分析](profile/ThreadPool.md)

##### EventBus [事件传递流程分析](profile/EventBus.md)

## Android

##### APP启动流程 [启动流程](profile/Android/App启动流程.md)

##### PMS流程解析 [PackageManagerService解析]()

##### AMS流程解析 [ActivityManagerService解析]()

##### View绘制流程

##### service和多进程

##### Java Hook

##### 内存优化 [Android内存优化](profile/Android/内存优化.md)

##### Java异常处理[Java异常处理](profile/Android/Java异常处理.md)

##### 捕获Native奔溃及后续处理[]()

##### 生成Jar包和AAR包 [参考](https://blog.51cto.com/1206995290qq/2331959)
    
    task buildXXX(type: Copy, dependsOn: [build]) {
        delete 'libs/XXX.jar'
        from('build/intermediates/packaged-classes/release/')   //生成的classes.jar在此路径下
        into('libs/')
        include('classes.jar')
        rename('classes.jar', 'XXX.jar')        //如果有资源文件，同时会生成AAR文件，在 build/outputs/aar 路径下
    }

## NDK系列

##### [CMakeList.txt 配置](profile/NDK/CMakeList配置.md)

##### [C++11 语法](profile/NDK/C++11语法介绍.md)

##### [JNI语法规则](profile/NDK/JNI语法.md)

##### [Native Hook]()

## 设计模式

##### [OO设计原则](profile/设计模式/OO设计原则.md)

##### [策略模式](profile/设计模式/设计模式.md)

##### [观察者模式](profile/设计模式/观察者模式.md)

##### [装饰者模式](profile/设计模式/装饰者模式.md)

##### [工厂模式](profile/设计模式/工厂模式.md)

##### [单例模式](profile/设计模式/单例模式.md)

##### [命令模式](profile/设计模式/命令模式.md)

##### [适配器、外观模式](profile/设计模式/适配器模式.md)

##### [模板方法模式](profile/设计模式/模板方法模式.md)

##### [迭代器模式](profile/设计模式/迭代器模式.md)

##### [组合模式](profile/设计模式/迭代器模式.md)

##### [状态模式](profile/设计模式/状态模式.md)

##### [代理模式](profile/设计模式/代理模式.md)

##### [复合模式](profile/设计模式/复合模式.md)

## 虚拟机

##### [Class文件格式](profile/虚拟机/Class文件格式/Class文件格式.md)

##### [Dex文件格式](profile/虚拟机/Dex文件格式/Dex文件格式.md)

##### [ELF文件格式](profile/虚拟机/ELF文件格式/ELF文件格式.md)

##### [编译器](profile/虚拟机/编译器.md)

##### [虚拟机的创建](profile/虚拟机/虚拟机的创建.md)

##### [虚拟机的启动](profile/虚拟机/虚拟机的启动.md)

##### [dex2oat](profile/虚拟机/dex2oat.md)

##### [解释执行和JIT](profile/虚拟机/解释执行和JIT.md)

##### [ART中的JNI](profile/虚拟机/ART中的JNI.md)

##### [CheckPoints、线程同步及信号处理](profile/虚拟机/线程同步.md)

##### [内存分配与释放](profile/虚拟机/内存分配与释放.md)

##### [Heap介绍](profile/虚拟机/Heap.md)

##### [内存分配](profile/虚拟机/内存分配.md)

## Gradle

## Linux

##### Linux [常用命令](profile/linux_command.md)

## Java Web 
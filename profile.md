## Code Source Profile

##### rxJava [使用参考](https://www.jianshu.com/p/9561ebdc5c0b)

##### Retrofit2 [网络请求流程分析](profile/Retrofit2.md)

##### LeakCanary [内存泄漏流程分析](profile/LeakCanary.md)

##### Glide 4.8.0 [源码分析](profile/Glide.md)

##### Handler [消息传递流程分析](profile/Handler.md)

##### 线程池 [线程池源码分析](profile/ThreadPool.md)

##### EventBus [事件传递流程分析](profile/EventBus.md)

## Android

##### 多线程 [多线程](profile/Android/多线程.md)

##### 音视频系列 [音视频系列](profile/音视频/音视频.md)

##### Android系统启动流程 [系统启动流程](profile/Android/Android系统启动流程.md)

##### APP进程启动流程 [进程启动流程](profile/Android/APP进程启动流程.md)

##### Application启动流程 [Application启动流程](profile/Android/Application启动流程.md)

##### View绘制流程 [View绘制流程](profile/Android/View绘制流程.md)

##### PMS流程解析 [PackageManagerService解析]()

##### AMS流程解析 [ActivityManagerService解析](与SystemServer进程强相关)

##### service和多进程 [Service启动流程](profile/Android/Service和多进程.md)

##### AIDL、Binder机制 [AIDL使用和Binder机制](profile/Android/AIDL使用和Binder机制.md)

##### RecyclerView解析 [RecyclerView解析](profile/Android/RecyclerView详解.md)

##### 常见问题汇集 [Android常见问题汇集](profile/Android/Android常见问题汇集.md)

##### Java Hook []()

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

##### [嵌套滑动机制](profile/Android/嵌套滑动.md)

## NDK系列

##### [CMakeList.txt 配置](profile/NDK/CMakeList配置.md)

##### [C++11 语法](profile/NDK/C++11语法介绍.md)

##### [JNI语法规则](profile/NDK/JNI语法.md)

##### [Native Hook]()

##### [音视频系列]()

## 设计模式

##### [OO设计原则](profile/设计模式/OO设计原则.md)

##### [策略模式](profile/设计模式/策略模式.md)

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

##### [ART中的GC](profile/虚拟机/ART中的GC.md)

##### [Heap介绍](profile/虚拟机/Heap.md)

##### [内存分配](profile/虚拟机/内存分配.md)

## Gradle

## Linux

##### Linux文档项目 [地址](http://www.tldp.org)

##### Linux程序设计--源码下载地址 [URL](https://www.wrox.com/go/downloadcode)

##### Linux [常用命令](profile/Linux/linux_command.md)

##### [Shell程序设计](profile/Linux/Shell程序设计.md)

##### [文件操作](profile/Linux/Linux文件操作.md)

##### [Linux环境](profile/Linux/Linux环境.md)

##### [终端](profile/Linux/终端.md)

##### [使用curses函数库管理基于文本的屏幕](profile/Linux/使用curses管理屏幕.md)

##### [数据管理](profile/Linux/数据管理.md)

##### [MySQL](profile/Linux/MySQL.md)

##### [开发工具](profile/Linux/开发工具.md)

##### [调试](profile/Linux/Debug.md)

##### [进程和信号](profile/Linux/进程和信号.md)

##### [POSIX线程](profile/Linux/POSIX线程.md)

##### [进程间通信：管道](profile/Linux/管道.md)

##### [信号量、共享内存和消息队列](profile/Linux/信号量、共享内存和消息队列.md)

##### [套接字](profile/Linux/套接字.md)

## Java Web 

##### Tomcat

##### SpringMVC

##### Hibernate

##### MyBatis

## 数据结构与算法

##### [二叉树](profile/数据结构/二叉树.md)

##### [算法](profile/算法/算法.md)
[二分查找](profile/算法/BinarySearch.java)  [排序](profile/算法/Sort.java)    [回溯法](profile/算法/Backtrack.java)

##### 队列 [代码实现](profile/数据结构/queue_and_stack.c)

##### 栈 [代码实现](profile/数据结构/queue_and_stack.c)

##### 树 [代码实现](profile/数据结构/tree_and_graph.c)

##### 图 [代码实现](profile/数据结构/tree_and_graph.c)

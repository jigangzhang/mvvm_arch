
## CMakeList.txt 配置

[官方文档](https://developer.android.google.cn/studio/projects/add-native-code)
[参考资料1](https://blog.csdn.net/u012528526/article/details/80647537)
[参考资料2](https://www.cnblogs.com/chenxibobo/p/7678389.html)

    cmake_minimum_required(VERSION 3.4.1)
    设置构建Native库所需的CMake最小版本
    
    set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${PROJECT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI})
    so动态库最后的输出的路径，前面一个是命令，后面是路径
    
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DXXX")
    设置宏定义的全局变量？XXX为变量名称
    
    sourceSets {
            main {
                jniLibs.srcDirs = ['libs']
            }
        }
    设置SO库的输出路径后，直接编译会报错，原因是设置输出路径后与默认的输出路径重复，需要上述配置才可
    
    INCLUDE_DIRECTORIES(${CMAKE_CURRENT_SOURCE_DIR}/common)
    设置头文件搜索路径（和此txt同个路径的头文件无需设置），可选
    
    LINK_DIRECTORIES(/usr/local/lib)
    指定用到的系统库或者NDK库或者第三方库的搜索路径，可选。
    
    add_library( 
            native-lib
            SHARED
            native-lib.cpp)
    创建并命名SO库，可将其设置为静态或共享库，并提供其源代码的相对路径。
    可以定义多个库，并由CMake构建，Gradle自动将共享库打包到APK中。
    第一个参数为生成的so库名称，在java代码中加载so库需要与此一致
    第二个参数为将SO库设置为一个共享库 SHARED 或静态库 STATIC
    最后是源文件，提供一个相对路径，可以使用通配符，也可以一个一个文件的引入
    
    find_library( 
            log-lib # log日志,默认都需要添加
            log)
    搜索指定的预构建库并将路径存储为一个变量
    只需指定要添加的公共NDK库的名称，因为CMake的默认搜索路径中已经包含了系统库
    CMake会在完成构建前验证库是否存在
    第一个参数为路径变量的名称，后面会引用，log-lib # log日志,默认都需要添加
    第二个参数为要添加的NDK库名称，指定要CMake定位的NDK库的名称
      
    target_link_libraries( 
            native-lib
            ${log-lib})
    指定CMake要链接到目标库的库。目标库为add_library中命名的库
    可以链接多个库，如在此构建脚本中定义的库、预构建的第三方库或系统库
    第一个参数为目标库，通过add_library添加的直接写设置的名字，一种是SHARED,一般来说就是我们需要生成的so文件，二种是STATIC IMPORTED 添加的第三方静态库
    其余时要链接的库，包括NDK里面的库
    
    ADD_SUBDIRECTORY(echo)
    添加子目录,将会调用子目录中的CMakeLists.txt, 会生成多个 so库
    echo为子目录文件夹名称
    
    externalNativeBuild {
                cmake {
                    cppFlags "-std=c++11"
                    //abiFilters "armeabi-v7a"//需要什么构架的so，就在这边添加即可, 不指定默认生成4种架构的so
                }
            }
    在gradle的defaultConfig 内
    cppFlags中可配置其他信息
    

#### 工具

    控制台打印STDOUT、STDERR：
        adb shell stop
        adb shell setprop log.redirect-stdio true
        adb shell start
        
    javah -- 生成头文件
    swig -- 自动生成JNI代码
    ndk-gdb -- 调试
    
    ndk-stack -- 堆栈跟踪分析
        adb logcat | ndk-stack -sym obj/local/armeabi (logcat打印堆栈跟踪)
        eg：ndk-stack -sym app/build/intermediates/cmake/debug/obj/armeabi-v7a -dump ./log.txt
        eg：adb logcat | ndk-stack -sym encryption/build/intermediates/cmake/debug/obj/armeabi-v7a，亲测可用
    
    CheckJNI(对JNI的扩展检查):
        adb shell setprop debug.checkjni 1
    内存分析：
        libc调试模式
            adb shell setprop libc.debug.malloc 1
            adb shell stop
            adb shell start
    strace(监控系统调用):
        adb  shell strace -v -p pid
        (模拟器带有strace功能)
        
#### 库概念

[参考](https://blog.csdn.net/koibiki/article/details/80952367)
    
    动态库和静态库：
        动态库：
            是程序运行时动态加载进进程内的
            它的好处是如果程序中有很多独立的模块都需要使用同样的一个动态库，
            那么只需要在内存中加载一次就可以了
        静态库：
            是在模块编译的时候，在链接的过程中，将程序库中所需要的代码“拷贝”到模块内部实现的
            它的坏处就是同样的代码会在各个模块中都存在，浪费内存和磁盘存储空间，
            甚至不同模块间的库函数代码会相互干扰，出现诡异的行为
            
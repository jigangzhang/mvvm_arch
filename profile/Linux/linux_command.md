## Linux 常用命令

##### 安装/更新

    rpm（Redhat Linux Package Manager）：
        本地软件包的管理器
        具体使用见：man rpm
        rpm -q（查询）、-i（安装）、-U（更新）、-F（freshen）、-e（卸载）
        常用方式：
            rpm -qa |grep packagename 查询是否有本地包，-qa 查询所有软件包
            rpm -ivh packagename 安装某个包（xxx.rpm），并显示安装过程，v 显示详情， h
            rpm -e packagename 卸载某个包
    yum：
        属于Redhat、Centos包管理工具
        基于rpm的一个软件包管理工具，它可以从网上下载rpm包和依赖
        yum check-update softname， 检查更新
        yum upgrade name， 更新一个或多个软件包    
        yum clean， 删除已缓存数据
        yum list name， 列出一个或一组软件包    
        yum install name， 安装一个或多个软件包    
        yum reinstall name， 重装一个包
        yum remove name， 移除一个或多个软件包
        yum -y install name
    
    apt-get：
        属于ubuntu、Debian的包管理工具

    源码编译安装：
        ./configure
        make
        make install
        make clean
        make distclean

##### 编译

    GCC：
        一般位于/usr/bin或/usr/local/bin目录中
        gcc -o name xxx.c   //-o name 告诉编译器可执行程序的名字，默认是a.out文件（汇编输出）
        gcc -I/xx/xx xx.c   //-I 用于包含保存在子目录或非标准位置中的头文件
        
        链接库文件，库文件必须遵循特定的命名规范且需要在命令行中明确指定
        库文件：
            以lib开头，如：libm.a
            lib随后的部分指明是什么库，如：m代表数学库
            .a，代表传统的静态函数库
            .so，代表共享函数库，函数库通常同时以静态库和共享库两种格式存在
            gcc -o fred fred.c /usr/lib/libm.a，编译文件fred.c，同时搜索数学库
            gcc -o fred fred.c -lm，同上，lm 简写方式，-l指明除标准C语言运行库外需要使用的库
            gcc -o fred -L/xx/lib fred.c -lm，-L为编译器增加库的搜索路径，用/xx/lib目录制定libm库来编译和链接程序fred
            （在/xx/lib目录中搜索 libm 这个库）
        静态库：
            也称作归档文件（archive），以.a 结尾
            编译链接时将库中函数和程序代码结合在一起，以组成一个单独的可执行文件
            程序使用静态库时只将使用到的函数编译链接到一起
            类似Windows的.LIB文件
            创建静态库：
                使用ar（代表archive）程序和gcc -c 命令对函数分别进行编译
                第一步，gcc -c xx.c，-c作用是阻止编译器创建一个完整的程序，生成 xx.o文件，此为中间产物
                第二步，为xx.c 创建头文件
                第三步，ar crv libx.a xx.o，使用ar创建归档文件并将目标文件添加进去
                第四步，ranlib libx.a，为函数库生成一个内容表（类unix系统使用）
                完成，可以使用了。 -L -l
                nm libx.a，可查看库中的内容
        共享库：
            静态库的缺点：多个程序都使用同一函数库的函数时，程序文件中会有多份同样的副本，内存中也是
            程序使用共享库：引用运行时可访问的共享代码
            是在程序运行时加载，类似Windows的.DLL文件
            对linux，负责装载共享库并解析客户程序函数引用的程序是ld.so
            ldd xxx，查看一个程序需要的共享库
        正常使用：
            gcc -c a.c b.c      //编译生成中间产物 .o 文件
            gcc -o ab a.o b.o   //链接生成可执行文件 ab
            gcc -o d d.c        //直接编译生成可执行文件 d

##### 重要目录和文件

    /usr/bin：系统为正常使用提供的程序、用于程序开发的工具等
    /usr/local/bin：为某个特定的主机或本地网络添加的程序
    /opt：为某个特定的主机或本地网络添加的程序
    /usr/local：保存系统级的应用程序，为某个特定的主机或本地网络添加的程序
    
    开发用和个人的应用程序最后保存在自己的home目录中
    
    /usr/include：保存头文件，用于对常量的定义和对系统函数及库函数调用的声明
        子目录 /sys或 /linux 保存依赖于特定Linux版本的头文件
    /lib64、/lib和/usr/lib：存储标准系统库文件，库文件必须遵循特定的命名规范且需要在命令行中明确指定    
    /etc/passwd，用户账号数据库
    /etc/shadow，用户加密口令
    /dev/tty，始终指向当前终端或当前的登录会话
    /etc/services，系统文件，端口号及通过它们提供的服务
    /etc/hosts，域名-IP映射文件
    /etc/xinetd.conf和 /etc/xinetd.d 目录中的文件，因特网守护进程的配置文件

##### 用户管理

    id，查看当前用户的id、gid等信息（对应 passwd文件中的对应行）
    uname，查看主机信息，具体见 man uname（uname -a、-s、-i等等）
    who，当前登录的用户
    ps -e，当前虚拟控制台上运行的shell和执行的程序

##### 网络

    netstat命令，查看网络连接情况：
        netstat -A inet，显示TCP/IP连接？local address一栏显示的是服务器，foreign address一栏显示的是远程客户，端口号与程序中指定的不一致
    ifconfig

##### 常用命令

    标准文件描述符：
        0，代表一个程序的标准输入
        1，标准输出
        2，标准错误输出

    重定向：< >
        ls -l > lsop.txt， 将ls的输出保存到txt文件中
        set -o noclobber（set -C），设置noclobber选项，阻止重定向操作覆盖已有文件
        set +o noclobber，取消noclobber选项
        ps >> lsop.txt，将ps输出附加到指定文件尾部
        kill -HUP 1234 >kout.txt 2>kerr.txt，把标准输出和标准错误输出重定向到不同文件中
        kill -1 1234 >kouterr.txt 2>&1，把两组输出都重定向到一个文件中，用>&操作符来结合两个输出
        kill -1 1234 >/dev/null 2>&1，丢弃所有输出信息，/dev/null linux的通用回收站
        
        more < kout.txt，重定向标准输入
        
    管道：|，在同时执行的程序之间实现数据的管道传递，连接进程
        ps | sort > pssort.out，使用sort对ps的输出进行排序
        ps | sort | more，在屏幕上分页显示输出结果
        ps -xo comm | sort | uniq | grep -v sh | more，查看系统中运行的所有进程名字，不包括shell本身
        grep -v sh，删除名为sh的进程
        管道连接的进程数目是没有限制的
        不能在管道命令流中重复使用相同的文件名，因为相应的输出文件是在这一组命令被创建的同时立刻被创建或写入的
        
    
    $(...)，获取子进程的输出？
    
    复制：
        cp file /xx/xx/，将文件复制到某个目录下
    
    which ls，which用来检查执行的是哪一个命令，会列出命令路径  
    rm -f filename，删除文件  

##### 环境变量

    PATH=$PATH:path，临时设置，将path路径添加到环境变量
    .bash_profile，编辑这个文件（home目录下），将上方命令添加到文件末尾，然后重新登录使生效
    export PATH=$PATH:/usr/local/xxx，临时设置，shell退出后失效

##### 压缩包相关

        zip
    tar cvfB file.tar 1024，打包？？？    
    文件后缀名为.tar.gz或.tgz，这类文件通常也被称为tarballs文件：
        使用普通的tar命令压缩文件：
            tar cvf xxx-1.0.tar xxx.c xx1.c xx2.c xx.h xxx.1 Makefile，生成tar文件
            gzip xxx-1.0.tar，使用压缩程序gzip进行压缩，使容量更小，生成.tar.gz文件
        解压文件：
            gzip -d xxx-1.0.tar.gz，解压生成xxx-1.0.tar文件
            tar xvf xxx-1.0.tar，解压出原文件
        使用GNU版本的tar命令压缩文件：
            tar zcvf xxx-1.0.tgz xxx.c xx1.c xx2.c xx.h xxx.1 Makefile，生成压缩文件
        解压缩：
            tar zxvf xxx-1.0.tgz，解压出原始文件
        tar ztcf，在不解压文件了解压缩文件的内容
        tar [options] [list of files]：
            options可以是文件或设备
            列表中可以包含目录，默认目录中所有子目录都将被包含到档案文件中，添加到新档案文件或已有档案文件中
            c，创建新档案文件
            f，指定目标为一个文件而不是一个设备
            t，列出档案文件的内容，但不真正释放它们
            v(verbose)，显示tar命令执行的详细过程
            x，从档案文件中释放文件
            z，在GNU版本的tar命令中用gzip压缩档案文件
        其他选项参考tar命令手册页


##### 进程、线程

        top ps
    ps：
        ps -ef，显示进程信息，如：TTY列显示进程是从哪一个终端启动的，TIME列是进程目前为止所占用的CPU时间，CMD列显示启动进程所使用的命令
        ps -ax/ax，命令输出中的STAT列用来表明进程的当前状态
        ps -a，查看所有的进程， -f 选项显示进程完整的信息
        ps -l或-f选项，查看正在运行的进程的nice值，NI列
        ps -al，
        ps lx，可查看状态，S表示休眠状态，不消耗CPU资源
        ps x，查看状态栏，STAT中包含字符N表明这个进程的nice值已被修改过，不再是默认值
    kill：
        kill -HUP pid，向pid进程发送 挂断 信号
        killall server1 server2 ...
    TIMEFORMAT="" time ./exec，将打印程序执行时长，cpu利用率等
    time命令，查看系统利用率
    nice命令，改变程序的优先级
    nice xxx &，启动xxx，将给xxx分配一个+10的nice值
    renice 10 pid，调整进程的nice值，降低进程优先级，使其运行不频繁
    ulimit命令，为某一特定shell中运行的程序设置限制
    命令行下后台运行程序：./exec &

##### ssh、scp

##### 文件相关

    chmod chown chgrp：
        r 4 w 2 x 1
        chmod +x file，给文件可执行权限，group、owner、other三者同时赋予
        chmod 755 file，修改权限
        chmod u=rwx,go=rx file，同上
        
        chown root file，修改文件属主，owner
        
        chgrp root file，修改文件所属组，group
    
    拥有一个目录的写权限（w），就可以对目录下的文件添加和删除
    ls -i，查看文件节点号
    ls -l，查看指向文件的链接数（权限后面的数字）
    ls -lstr，ls -l -s -t -r，
    ls -lF，查看输出结果，第一个字符p，表示是一个管道，最后的|符号由-F选项添加的，也表示是一个管道；第一个字符s，表示是套接字，最后是=
    od -c，查看文件内容？
    ln，创建链接
    sort -r file，逆向排序
    dd if=/dev/fd0 of=/temp/file.dd bs=18k，复制？？？
    whereis mysql，查找mysql对应的文件夹

##### 文本相关

    man、info：查看相关命令行的具体使用方式
    文本编辑/查看：
        vim: a/i 编辑， esc 退出编辑 
             非编辑模式下：
                :q 退出， :wq 保存退出， :!q 强制退出
             编辑模式下：
                
        cat:
        tail:
        less:
        ed行编辑器：
        touch：
            touch filename，改变文件的修改时间，检查文件是否存在，如果不存在就创建它（不会把有内容的文件变成空文件）
    wc：单词统计
        wc -l file，统计行数
    od命令：八进制输出， od -c
    cut：
        
    文本检索：
    grep（在文件中搜索字符串）：
        grep [options] PATTERN [FILES]
        若没有提供文件名，则grep命令将搜索标准输入
        options：
            -c，输出匹配行的数目，而不是输出匹配的行
            -E，启用扩展表达式
            -h，取消每个输出行的普通前缀，即匹配查询模式的文件名
            -i，忽略大小写
            -l，只列出包含匹配行的文件名，而不输出真正的匹配行
            -v，对匹配模式取反，搜索不匹配行而不是匹配行
        grep xxx *.c    //在以.c结尾的所有文件中搜索字符串XXX，显示字符串所在行
        grep -c in words.txt words2.txt，输出所在行号
        grep -c -v in words.txt words2.txt，输出不匹配行号
        多个文件中搜索时，会输出文件名
        
    find（用于搜索文件）：
        find / -name test -print，在本地机器上从根目录开始查找名为test的文件，输出完整路径
        find / -mount -name test -print，不要搜索挂载的其他文件系统的目录
        find [path] [options] [tests] [actions]，完成语法格式
        options：
            -depth，在查看目录本身之前先搜索目录的内容
            -follow，跟随符号链接
            -maxdepth N，最多搜索N层目录
            -mount（-xdev），不搜索其他文件系统中的目录
        tests：
            测试返回true或false，返回false，停止处理当前，继续搜索，true，继续下一个测试或对当前文件采取行动
            -atime N，文件在N天前被最后访问
            -mtime N，文件在N天前被最后修改
            -name pattern，文件名匹配模式，pattern必须总是用引号括起
            -newer otherfile，文件比otherfile文件新
            -type c，文件类型为c，c特殊类型，常见的有d（目录），f（普通文件）
            -user username，文件的拥有者是指定的用户username
        操作符组合测试：
            ！ -not，取反
            -a -and，与
            -o -or，或
        \(-newer X -o -name "_*" \)，比X新或文件名以下划线开头
        find . -newer while2 -print，包括目录
        find . -newer while2 -type f -print，只是普通文件
        find . \( -name "_*" -or -newer while2 \) -type f -print，比X新或文件名以下划线开头的普通文件
        actions：
            -exec command，执行一条命令，这个动作必须使用\;结束
            -ok command，与exec类似，在执行命令前会针对要处理的文件提示用户进行确认
            -print， 打印文件名
            -ls，    对当前文件使用命令ls-dils
        find . -newer while2 -type f -exec ls -l {} \;
        -exec -ok 将后续的参数作为它们参数的一部分，直到被\;序列终止
        {}是-exec -ok的一个特殊类型的参数，它将被当前文件的完整路径取代

##### 服务：

        systemctl start/restart/stop/enable xxx.service    

##### 关机/重启

    重启命令：
        reboot
        shutdown -r now 立刻重启(root用户使用)
        shutdown -r 10 过10分钟自动重启(root用户使用) 
        shutdown -r 20:35 在时间为20:35时候重启(root用户使用)
        如果是通过shutdown命令设置重启的话，可以用shutdown -c命令取消重启
    关机命令：
        halt   立刻关机
        poweroff  立刻关机
        shutdown -h now 立刻关机(root用户使用)
        shutdown -h 10 10分钟后自动关机
        如果是通过shutdown命令设置关机的话，可以用shutdown -c命令取消重启        
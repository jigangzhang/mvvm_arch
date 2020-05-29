
## Linux文件操作

#### Linux文件结构

    linux中，一切（或几乎一切）都是文件
    5个基本函数：open、close、read、write、ioctl
    linux中任何事物都可以用一个文件来表示，或者通过特殊文件提供
    
    一些特殊文件：
    目录：
        文件有内容、名字和一些属性（管理信息，包括文件的创建/修改日期和访问权限）
        文件的属性被保存在文件的inode（节点）中，一个特殊的数据块，还包含文件的长度和文件在磁盘上的存放位置
        系统使用的是文件的inode编号
        
        目录是用于保存其他文件的节点号和名字的文件
        目录文件中的每个数据项都是指向某个文件节点的链接，删除文件名就等于删除与之对应的链接
        ls -i，查看文件的节点号
        ln命令，在不同的目录中创建指向同一个文件的链接（）
        
        删除文件，实质上是删除该文件对应的目录项，同时指向该文件的链接数减1，该文件中的数据可能能通过其他指向同一文件的链接访问到
        指向某个文件的链接数变为0（ls -l查看），就表示该节点以及其指向的数据不再被使用，磁盘上的相应位置会被标记为可用空间
        
        ~user，进入其他用户家目录，标准库函数不认~，必须使用真实文件名
        /bin，存放系统程序（二进制可执行文件）
        /etc，存放系统配置文件
        /lib，存放系统函数库
        /dev，代表物理设备并为设备提供接口的文件
        /home，存放各个用户家目录

    文件和设备
        硬件设备通常也被映射为文件
        mount -t iso9660 /dev/hdc /mnt/cdrom，将ide CD-ROM驱动器挂载为一个文件
        接入系统的硬件会在/dev目录下生成一个文件，如上的hdc，硬盘等会有不同的名称
        使用mount命令，将硬件挂载到/mnt目录下，即可以文件形式访问硬件内容
        比较重要的设备文件：
            /dev/console：
                系统控制台，错误信息、诊断信息通常会被发送到这个设备，显示屏/终端接收控制台消息（打印终端等）
            /dev/tty：
                进程的控制终端（键盘、显示屏、窗口的别名），
                ls -R | more，显示长目录列表
                开多个tty，访问不同的物理设备
            /dev/null：
                空设备，写向这个设备的所有输出都将被丢弃，读这个设备会立刻返回一个文件尾标志，可用于创建空文件
                touch file
                cp /dev/null file，创建空文件
        设备被分为字符设备和块设备，区别在于访问设备时是否需要一次读写一整块
        块设备一般是那些支持某些类型文件系统的设备，如硬盘


#### 系统调用和设备驱动程序

    系统调用，用少量的函数就可以对文件和设备进行访问和控制，由UNIX直接提供
    内核是一组设备驱动程序，它们是一组对系统硬件进行控制的底层接口
    设备驱动程序封装了所有与硬件相关的特性，为用户提供一致的接口
    硬件的特有功能通常可通过ioctl（I/O控制）系统调用来提供
    用于访问设备驱动程序的底层函数（系统调用）：
        open，打开文件或设备
        read，从打开的文件或设备中读数据
        write，向文件或设备写数据
        close，关闭文件或设备
        ioctl，把控制信息传递给设备驱动程序
    每个驱动程序都有自己的一组ioctl命令
    系统调用的文档一般放在手册页的第二节

#### 库函数

    使用系统调用会影响系统的性能，比函数调用的开销要大些（会从用户态切换到内核态，然后再返回用户代码）
    每次的系统调用尽可能完成更多的工作，如：每次读取大量的数据而不是一个字符
    硬件会限制对底层系统调用一次所能读写的数据块大小
    
    标准库函数：
        由函数构成的集合
        可认为是对系统调用的封装，更高层的接口
        库函数文档一般放在手册页的第三节
    
      用户程序    |     <--用户空间
    库           |
    -------------
        || 调用
        \/        
     系统调用        |
    ----------------|  <--内核空间
    设备驱动程序| 内核|   
        ||
        \/    
      硬件设备

#### 底层文件访问

    进程 process，有一些与之关联的文件描述符，是一些小值整数，可以通过它们访问打开的文件或设备
    文件描述符的数量取决于系统配置
    一个程序开始运行时，一般会有3个已经打开的文件描述符：
        0：标准输入； 1：标准输出； 2：标准错误；
    使用自动打开的文件描述符就可以通过write系统调用来创建一些简单的程序
    
    write系统调用：
        把缓冲区buf的前nbytes个字节写入与文件描述符fildes关联的文件中，返回实际写入的字节数，0 未写入，-1 write出错，错误代码保存在全局变量errno
        size_t write(int fildes, const void *buf, size_t nbytes);   （头文件：unistd.h）
        fildes 为 0、1、2时，表示把数据写入到标准输入、输出、错误中
        write写入的字节可能比要求的少，要检查errno以发现错误，然后再次调用write写入剩余数据
    
    read系统调用：
        从与文件描述符fildes关联的文件中读入nbytes个字节数据放到缓冲区buf中，返回实际读入的字节数（可能会小于请求的字节数），0 未读入，-1 read出错，错误代码保存在全局变量errno
        size_t read(int fildes, void *buf, size_t nbytes);  （头文件：unistd.h）
        fildes 可以为 0、1、2，如：把标准输入复制到标准输出等

    open系统调用：
        创建一个新的文件描述符，建立一条到文件或设备的访问路径
        int open(const char *path, int oflags);     （头文件：fcntl.h、sys/types.h、sys/stat.h）
        int open(const char *path, int oflags, mode_t mode);
        返回一个可以被read、write、其他系统调用使用的文件描述符（非负整数），是唯一的，不会与运行中的其他进程共享
        两个程序同时打开一个文件，会分别得到两个不同的文件描述符，都作写操作时，它们会各写各的，数据不会交织在一起，而是彼此互相覆盖，可以使用文件锁功能来防止出现冲突
        path，为文件或设备名称
        oflags，用于指定打开文件所采取的动作
        oflags是通过命令文件访问模式与其他可选模式相结合的方式来指定的：
            O_RDONLY，以只读方式打开
            O_WRONLY，以只写方式打开
            O_RDWR，  以读写方式打开
        oflags参数的可选模式（“按位或”操作）：
            O_APPEND，写入数据追加在文件末尾
            O_TRUNC， 把文件长度设置为0，丢弃已有内容
            O_CREAT， 如果需要，按参数mode中给出的访问模式创建文件
            O_EXCL，  与O_CREAT一起使用，确保调用者创建出文件，open调用是原子操作，只执行一个函数调用，使用这个可选模式可以防止两个程序同时创建同一个文件，如果文件已存在，open调用将失败
        open失败时返回-1，并设置全局变量errno来指明失败的原因
        类似函数creat调用，相当于以oflags标志 O_CREAT|O_WRONLY|O_TRUNC来调用open，创建并打开文件
        一个进程可同时打开的文件数的限制在 limits.h 头文件中的OPEN_MAX定义的，其值至少为16，linux中这个限制可在运行时调整，通常开始为256
        
    访问权限的初始值：
        使用O_CREAT标志的open调用，必须使用mode（按位或）：
            S_IRUSR，读权限，文件属主（头文件：sys/stat.h）
            S_IWUSR，写权限，文件属主
            S_IXUSR，执行权限，文件属主
            S_IRGRP，读权限，文件属组
            S_IWGRP，写权限，文件属组
            S_IXGRP，执行权限，文件属组
            S_IROTH，读权限，其他用户
            S_IWOTH，写权限，其他用户
            S_IXOTH，执行权限，其他用户
        open("myfile", O_CREAT, S_IRUSR|S_IXOTH); //文件属主有读权限，other有执行权限，且只有这两个权限
        创建的文件权限会被用户掩码（shell的umask命令设定）影响，mode与umask的反值做AND操作，即open发出设置权限请求，是否会被设置取决于当时的umask
        umask：
            是一个系统变量，作用是：文件被创建时，为文件的访问权限设定一个掩码
            umask命令可修改这个变量的值，是由3个八进制数字组成的值，每个数字都是八进制1、2、4的OR操作结果
            3个数字分别对应 用户（user）、组（group）、其他用户（other），类似 755、 777这些权限设置
            0：允许任何权限；4：禁止读权限；2：禁止写权限；1：禁止执行权限
            eg：禁止组的写、执行权限，other的写权限， u：0， g：2 1， o：2
                u g o 的取值OR，即 group 2|1 为 3，最终umask值为：032
            在mode中被设置的位如果在umask中也被设置了，那么它就会从文件的访问权限中删除
            umask不能阻止 chmod命令（或者在程序中执行chmod系统调用）修改权限
    
        close系统调用：
            终止文件描述符fildes与其对应文件之间的关联
            文件描述符被释放并能够重新使用
            close成功返回0，出错返回-1
            int close(int fildes);  （unistd.h）
            检查close调用的返回结果，有的文件系统不会在关闭文件之前报告写操作中出现的错误，是因为执行写操作时数据可能未被确认写入
            
        ioctl系统调用：
            提供了一个用于控制设备及其描述符行为和配置底层服务的接口
            终端、文件描述符、socket等都可以有为它们定义的ioctl
            int ioctl(int fildes, int cmd, ...);    （unistd.h）
            对描述符fildes引用的对象执行cmd参数中给出的操作
            ioctl(tty_fd, KDSETLED, LED_NUM|LED_CAP|LED_SCR);   //linux上打开键盘上的LED灯
    
    其他与文件管理有关的系统调用：
        lseek系统调用：
            对文件描述符fildes的读写指针进行设置，即用它设置文件的下一个读写位置
            读写指针即可被设置为文件中的某个绝对位置，也可设置为相对于当前位置或文件尾的某个相对位置
            off_t lseek(int fildes, off_t offset, int whence);  （unistd.h）
            offset 指定位置，whence 定义该偏移值的用法
            whence的取值：
                SEEK_SET，offset是绝对位置
                SEEK_CUR，offset是相对于当前位置的一个相对位置
                SEEK_END，offset是相对于文件尾的一个相对位置
            返回值是从文件头到文件指针被设置处的字节偏移值，失败返回-1， off_t 整数类型，定义在 sys/types.h
        
        fstat、stat和lstat系统调用：
            fstat返回与打开的文件描述符相关的文件的状态信息，该信息写到buf中，buf的地址以参数形式传递给fstat
            int fstat(int fildes, struct stat *buf);
            int stat(const char *path, struct stat *buf);
            int lstat(const char *path, struct stat *buf);  （unistd.h、sys/types.h、sys/stat.h）
            stat、lstat返回的是通过文件名查到的状态信息，结果相同
            当文件是符号链接时，lstat返回的是该符号链接本身的信息，stat返回的是链接指向的文件的信息
            stat结构：
                st_mode，文件权限和文件类型信息
                st_ino，与该文件关联的inode
                st_dev，保存文件的设备
                st_uid，文件属主的UID号
                st_gid，文件属主的GID号
                st_atime，文件上一次被访问的时间
                st_ctime，文件的权限、属主、组或内容上一次被改变的时间
                st_mtime，文件的内容上一次被修改的时间
                st_nlink，该文件上硬链接的个数
            st_mode标志关联的宏：对访问权限、文件类型标志、权限的掩码、帮助测试特定类型等的定义
                访问权限标志与open系统调用中的内容一样
                文件类型标志：
                    S_IFBLK，文件是一个特殊的块设备
                    S_IFDIR，文件是一个目录
                    S_IFCHR，文件是一个特殊的字符设备
                    S_IFIFO，文件是一个FIFO（命名管道？）
                    S_IFREG，文件是一个普通文件
                    S_IFLNK，文件是一个符号链接
                其他模式标志：
                    S_IFUID，文件设置了SUID位
                    S_IFGID，文件设置了SGID位
                解释st_mode标志的掩码：
                    S_IFMT，文件类型
                    S_IRWXU，属主的读写执行权限
                    S_IRWXG，属组的读写执行权限
                    S_IRWXO，其他用户的读写执行权限
                帮助确定文件类型的宏定义（对经过掩码处理的模式标志和相应的设备类型标志进行比较）：
                    S_ISBLK，测试是否是特殊的块设备文件
                    S_ISCHR，测试是否是特殊的字符设备文件
                    S_ISDIR，测试是否是目录，S_ISDIR(statbuf.st_mode)
                    S_ISFIFO，测试是否是FIFO
                    S_ISREG，测试是否是普通文件
                    S_ISLNK，测试是否是符号链接
            测试一个文件不是一个目录，设置了属主的执行权限，并且没有其他权限
            stat("filename", &statbuf);
            modes = statbuf.st_mode;
            if(!S_ISDIR(modes) && (modes & S_IRWXU) == S_IXUSR) ...
            
        dup和dup2系统调用：
            dup提供了一种复制文件描述符的方法，使能够通过两个或更多不同的描述符来访问同一个文件
            可以用于在文件的不同位置对数据进行读写
            dup 复制文件描述符返回新的描述符
            dup2 通过指定目标描述符来把文件描述符复制为另外一个
            int dup(int fildes);
            int dup2(int fildes, int fildes2);

#### 标准 I/O库

    标准IO库及其头文件stdio.h为底层I/O系统调用提供了一个通用的接口，ANSI标准C的一部分
    格式化输出和扫描输入、满足设备的缓存需求
    与底层文件描述符对应的是流（stream），它被实现为指向结构FILE的指针
    启动程序时，有3个文件流是自动打开的：stdin、stdout、stderr（stdio.h中定义），与底层文件描述符 0、1、2相对应
    
    fopen函数：
        类似于底层的open系统调用，主要用于文件和终端的输入输出
        对设备进行明确的控制时最好使用底层系统调用，潜在问题：输入输出缓冲
        FILE *fopen(const char *filename, const char *mode);
        打开由filename指定的文件，并与一个文件流关联起来
        mode，打开方式：
            r或rb，以只读方式打开
            w或wb，以写方式打开，并把文件长度截短为0
            a或ab，以写方式打开，新内容追加在文件尾
            r+或rb+或r+b，以更新方式打开（读和写）
            w+或wb+或w+b，以更新方式打开，并把文件长度截短为0
            a+或ab+或a+b，以更新方式打开，新内容追加在文件尾
        b表示文件是一个二进制文件而不是文本文件（linux把所有文件都看作为二进制文件，不区分文本和二进制文件，MS-DOS区分）
        失败返回NULL，文件流数量限制由stdio.h中的FOPEN_MAX来定义的，其值至少为8，linux中通常是16
        
    fread函数：
        用于从一个文件流里读取数据
        size_t fread(void *ptr, size_t size, size_t nitems, FILE *stream);
        数据从文件流stream读到ptr指向的缓存区，对数据记录进行操作，size指定每个数据记录的长度，nitems给出要传输的记录个数
        返回值是成功读到数据缓冲区的记录个数（不是字节数），到达文件尾时，返回值可能会小于nitems，甚至为0
        为数据分配空间和检查错误是程序员的责任
        
    fwrite函数：
        从指定的数据缓冲区里取出数据记录，并写到输出流中，返回值是成功写入的记录个数
        size_t fwrite(const void *ptr, size_t size, size_t nitems, FILE *stream);
        与fread相似
    
    fclose函数：
        关闭指定的文件流stream，使所有尚未写出的数据都写出
        若要确保数据全部写出，就应该调用fclose
        程序正常结束时，会自动对所有还打开的文件流调用fclose
        int fclose(FILE *stream);   //失败返回错误码
    
    fflush函数：
        把文件流里的所有未写出数据立刻写出
        如：确保在读入一个用户相应之前，先向终端送出一个交互提示符
        fclose函数隐含执行一次flush操作，不必在fclose之前调用fflush
        int fflush(FILE *stream);
    
    fseek函数：
        与lseek系统调用对应，在文件流里为下一次读写操作指定位置
        int fseek(FILE *stream, long int offset, int whence);
        offset、whence与lseek完全一样
        返回值：0表示成功，-1表示失败并设置errno指出错误
        
    fgetc、getc和getchar函数：
        fgetc从文件流里取出下一个字节并把它作为一个字符返回，到达文件尾或出错时返回EOF，通过ferror或feof区分这两种情况
        int fgetc(FILE *stream);
        int getc(FILE *stream);
        int getchar();
        getc函数作用和fgetc一样，有可能被实现为一个宏，不能保证能够使用getc的地址作为一个函数指针，fgetc更好？
        getchar函数作用相当于getc(stdin)，从标准输入里读取下一个字符
    
    fputc、putc和putchar函数：
        fputc把一个字符写到一个输出文件流中，返回写入的值，失败返回EOF
        int fputc(int c, FILE *stream);
        int putc(int c, FILE *stream);
        int putchar(int c);
        类似fgetc和getc之间的关系，putc作用也相当于fputc，但它可能被实现为一个宏
        putchar相当于putc(c, stdout)，把单个字符写到标准输出
        getchar、putchar都把字符当作int类型而不是char类型来使用，这就允许文件尾标识取值-1，是一个超出字符数字编码范围的值
    
    fgets和gets函数：
        fgets从输入文件流stream里读取一个字符串
        char *fgets(char *s, int n, FILE *stream);
        char *gets(char *s);
        fgets把读到的字符写到s指向的字符串，直到出现下面某种情况：
            遇到换行符、已经传输了n-1个字符、或者到达文件尾
        把换行符传递到接收字符串里，再加上一个表示结尾的空字节 \0
        一次调用最多只能传输n-1个字符，因为必须把空字节加上以结束字符串
        成功时，fgets返回指向字符串s的指针，如果文件流到达文件尾，fgets会设置这个文件流的EOF标识并返回一个空指针
        错误时，fgets返回一个空指针并设置errno以指出错误的类型
        gets类似于fgets，从标准输入读取数据并丢弃遇到的换行符，在接收字符串的尾部加上一个null字节
        gets对传输字符个数没有限制，可能会溢出传输缓冲区，应该使用fgets代替

#### 格式化输入和输出

    按设计格式输入/输出数据
    
    printf、fprintf和sprintf函数：
        对各种不同类型的参数进行格式编排和输出
        每个参数在输出流中的表示形式由格式参数format控制，它是一个包含需要输出的普通字符和称为转换控制符代码的字符串
        转换控制符规定了其余的参数应该以何种方式被输出到何种地方
        int printf(const char *format, ...);        //把自己的输出送到标准输出
        int sprintf(char *s, const char *format, ...);  //把自己的输出和一个结尾空字符写到作为参数传递的字符串s里
        int fprintf(FILE *stream, const char *format, ...);  //把自己的输出送到一个指定的文件流
        转换控制符：
            %d，%i，以十进制格式输出一个整数
            %o，%x，以八进制或十六进制格式输出一个整数
            %c，输出一个字符
            %s，输出一个字符串
            %f，输出一个单精度浮点型
            %e，以科学计数法格式输出一个双精度浮点数
            %g，以通用格式输出一个双精度浮点数
            %%，输出%字符
        长度限定符：
            %hd，短整数 short int；%ld，长整数 long int
        gcc -Wformat，对printf语句进行检查（参数匹配）
        字段限定符：
            对输出数据的间隔进行控制，设置浮点数的小数位数或设置字符串两端的空格数
            是转换控制符里紧跟在%字符后面的数字
            %10s，输出到一个10个字符宽的区域，字符串右对齐
            %-10s，输出到一个10个字符宽的区域，字符串左对齐
            %*s， 10,"abs"，可变字段宽度用*表示，下一个参数表示字段宽
            %010d，%后以0开头表示数据前面要用数字0填充
        printf不对数据字段进行截断，而是扩充数据字段以适应数据宽度
        printf返回一个整数表明它输出的字符个数
        sprintf的返回值没有算上结尾的null空字符
        出错，返回负值，并设置errno
    
    scanf、fscanf和sscanf函数：
        工作方式与printf相似
        从一个文件流里读取数据，并把数据值放到以指针参数传递的地址处的变量中，也使用格式字符串控制输入数据的转换
        int scanf(const char *format, ...);     //读入的值保存到对应的变量中，变量类型必须正确
        int fscanf(FILE *stream, const char *format, ...);
        int sscanf(const char *s, const char *format, ...);
        format格式字符串同时包含普通字符串和转换字符串，普通字符串是用于指定在输入数据里必须出现的字符
        格式字符串中的空格用于忽略输入数据中位于转换控制符之间的各种空白字符
        其他转换控制符：
            %d，读取一个十进制整数
            %o、%x，读取一个八进制或十六进制整数
            %f、%e、%g，读取浮点数
            %c，读取一个字符（不会忽略空格），不会跳过起始的空白字符
            %s，读取字符串，会跳过起始的空白字符，在字符串里出现的第一个空白字符处停下
            %[]，读取一个字符集合
            %%，读取一个%字符
        长度限定符：%hd，%lg，主要是 h l
        以*开头的控制符表示对应位置上的输入数据将被忽略，不会被保存，不需要一个变量来接收
        %s，读取的字符串长度没有限制，接收字符串必须有足够的空间来容纳，最好使用一个字段限定符，或结合使用fgets、sscanf读入一行数据，再对它进行扫描
        %[A-Z]，读取一个由大写字母构成的字符串
        %[^,]，读取一个带空格的字符串，并且遇到第一个逗号时停止
        %[^\n]，读取字符串至换行符结束
        返回值是它成功读取的数据项个数，读第一项时失败，返回0
        在匹配第一个数据项之前就已经到达输入的结尾，返回EOF
        文件流发生读错误，流错误标志被设置，并且错误变量errno被设置以指明错误类型
        应该用fread、fgets读取输入行，再用字符串函数分解
    
    其他流函数：
        fgetpos，获得文件流的当前（读写）位置
        fsetpos，设置文件流的当前（读写）位置
        ftell，返回文件流当前（读写）位置的偏移值
        rewind，重置文件流里的读写位置
        freopen，重新使用一个文件流
        setvbuf，设置文件流的缓冲机制
        remove，相当于unlink函数，path参数是目录的话，就相当于rmdir
        在手册页的第三节有说明
    
    文件流错误：
        出现错误时，stdio库函数返回一个超出范围的值，如空指针或EOF常数，错误由外部变量errno指出
        extern int errno; （errno.h）
        只有在函数调用失败时才有意义，必须在函数表明失败之后立刻对其进行检查，在使用之前先复制到另一个变量中
        很多函数都可能改变errno的值
        通过检查文件流的状态来确定是否发生错误，或者是否到达文件尾
        int ferror(FILE *stream);   //测试一个文件流的错误标识，该标识被设置返回非0值，否则返回0
        int feof(FILE *stream);     //测试一个文件流的文件尾标识，被设置返回非0值，否则返回0， if(feof(stream))  是在文件尾
        void clearerr(FILE *stream);//清除由stream指向的文件流的文件尾标识和错误标识，错误解决后，使从文件流的错误状态中恢复
        
    文件流和文件描述符：
        每个文件流都和一个底层文件描述符相关联
        int fileno(FILE *stream);   //返回文件流对应的文件描述符，失败返回-1
        FILE *fdopen(int fildes, const char *mode);  //为一个已经打开的文件描述符提供stdio缓冲区
        mode参数与fopen函数的完全一样，fdopen返回一个新的文件流，失败返回NULL

#### 文件和目录的维护

    chmod系统调用：
        改变文件或目录的访问权限，shell chmod的基础
        int chmod(const char *path, mode_t mode);   （sys/stat.h）
        path指定的文件被修改为具有mode参数给出的访问权限，mode与open系统调用一样，按位OR操作，一般属主或root可修改
        
    chown系统调用：
        改变一个文件的属主
        int chown(const char *path, uid_t owner, gid_t group);  （sys/types.h，unistd.h）
        使用的是用户Id、组Id的数字值（getuid、getgid调用获得）和一个用于限定谁可以修改文件属主的系统值
        
    unlink、link和symlink系统调用：
        可以使用unlink系统调用删除一个文件
        unlink删除一个文件的目录项并减少它的链接数，成功返回0，失败返回-1
        若想成功删除文件，必须拥有该文件所属目录的写和执行权限
        int unlink(const char *path);   （unistd.h）
        int link(const char *path1, const char *path2);   //创建一个指向已有文件path1的新链接，新目录项由path2给出
        int symlink(const char *path1, const char *path2);  //创建符号链接，文件的符号链接不会增加该文件的链接数，不会防止文件被删除
        一个文件的链接数减少到0，并且没有进程打开它，这个文件就会被删除
        目录项总是被立刻删除，文件占用的空间要等到最后一个进程关闭它之后才会被系统回收
        rm程序使用的就是这个调用
        ln程序，创建文件上其他的链接
        先open创建一个文件，然后对其调用unlink，创建临时文件
        
    mkdir和rmdir系统调用：
        int mkdir(const char *path, mode_t mode);   （sys/types.h，sys/stat.h）
        创建目录，mode设定权限，O_CREAT和umask的设置
        int rmdir(const char *path);    （unistd.h）
        用于删除目录，只有在目录为空时才行， rmdir程序就是用此系统调用完成
    
    chdir系统调用和getcwd函数：
        int chdir(const char *path); （unistd.h）//浏览目录，切换目录（cd命令）
        char *getcwd(char *buf, size_t size); //确定自己的当前工作目录
        getcwd把当前目录的名字写到buf缓冲区，如果目录名的长度超出size给出的缓冲区（ERANGE错误），返回NULL，成功返回指针buf
        程序运行时，目录被删除（EINVAL错误），有关权限发生变化（EACCESS错误），getcwd也可能返回NULL

#### 扫描目录

    扫描目录：确定一个特定目录下存放的文件
    与目录有关的函数在dirent.h中声明
    目录流指向 DIR* 完成各种目录操作，与FILE*（文件流）相似
    目录数据项本身在dirent结构中返回
    
    opendir函数：
        打开一个目录并建立一个目录流
        DIR *opendir(const char *name); （sys/types.h，dirent.h）
        失败返回空指针
        目录流使用底层文件描述符来访问目录本身（目录流有数量限制）
    
    readdir函数：
        struct dirent *readdir(DIR *dirp); （sys/types.h，dirent.h）
        返回一个指针，指针指向的结构保存着目录流dirp中下一个目录项的有关资料
        后续的readdir调用将返回后续的目录项，错误或到达目录尾返回NULL
        dirent结构：
            ino_t d_ino，文件的inode结点号
            char d_name[]，文件名
        要了解目录中的某个文件，使用上面提到的stat调用
        
    telldir函数：
        long int telldir(DIR *dirp); （sys/types.h，dirent.h）
        返回值记录着一个目录流里的当前位置，可在seekdir中使用来重置目录扫描到当前位置
    
    seekdir函数：
        void seekdir(DIR *dirp, long int loc);
        设置目录流dirp的目录项指针，loc的值用来设置指针位置，通过telldir获得
    
    closedir函数：
        int closedir(DIR *dirp); （sys/types.h，dirent.h）
        关闭一个目录流并释放与之关联的资源，成功返回0，错误返回-1

#### 错误处理

    系统调用和函数会在失败时设置外部变量errno的值来表明失败的原因
    很多函数库都把这个变量用作报告错误的标准方法
    必须在函数报告错误之后立刻检查errno变量，因为可能被下一个函数调用所覆盖
    
    错误代码的取值的含义（errno.h）：
        EPERM，操作不允许
        ENOENT，文件或目录不存在
        EINTR，系统调用被中断
        EIO，IO错误
        EBUSY，设备或资源忙
        EEXIST，文件存在
        EINVAL，无效参数
        EMFILE，打开的文件过多
        ENODEV，设备不存在
        EISDIR，是一个目录
        ENOTDIR，不是一个目录
    
    strerror函数：
        把错误码映射为一个字符串，该字符串对发生的错误类型进行说明
        char *strerror(int errnum); （string.h）
    
    perror函数：
        把errno变量中报告的当前错误映射到一个字符串，并把它输出到标准错误输出流
        该字符串的前面先加上字符串s中给出的信息，再加上一个冒号和一个空格
        void perror(const char *s); （stdio.h）
        perror("program");  == program: Too many open files

#### /proc文件系统

    硬件设备在文件系统中有对应的条目，使用系统调用通过/dev目录中的文件来访问硬件
    控制硬件的软件驱动程序
    与设备驱动程序进行通信的工具--hdparm（配置一些磁盘参数）、ifconfig（报告网络统计信息）
    更一致的访问驱动程序的信息的方式
    特殊的文件系统procfs，通常以/proc目录的形式呈现，该目录包含许多特殊文件用来对驱动程序和内核信息进行更高层的访问，程序有访问权限，就可以通过读写这些文件来获得信息或设置参数
    /proc目录中的文件随系统的不同而不同，驱动程序和设施支持procfs文件系统时，该目录中文件会更多
    多数情况下，只需读取这些文件就可以获得状态信息
    
    cat /proc/cpuinfo   （读取cpu详细信息）
    cat /proc/meminfo   （内存使用情况）
    cat /proc/version   （内核版本信息）每次读取时都会更新最新的信息
    cat /proc/net/sockstat   （获得网络套接字的使用统计）
    
    /proc目录中的有些条目不仅可以被读取，而且可以被修改
    cat /proc/sys/fs/file-max   （系统中所有运行的程序同时能打开的文件总数），该值可被修改
    echo 8000 >/proc/sys/fs/file-max    修改为8000
    
    /proc目录中以数字命名的子目录用于提供正在运行的程序的信息（程序以进程的方式执行）
    每个进程都有一个唯一标识符：1~32000，ps -a 查看当前运行的进程列表
    ls -l /proc/9118，9118为ps查看到的进程标识符，这样查看进程的更多细节
    od -c /proc/9118/cmdline，查看进程的启动命令？
    od -c /proc/9118/environ，查看进程的shell环境？
    cwd，对应当前工作目录？
    exe，进程对应的程序
    ls -l /proc/9118/fd
        fd子目录提供该进程正在使用的打开的文件描述符信息；
        该信息可用于确定一个程序同时打开了多少文件等；
        0 1 2 3 4 ...，0 1 2 分别对应标准输入、输出、错误输出，其余为其他文件

#### 高级主题：fcntl 和 mmap

    fcntl系统调用：
        对底层文件描述符提供更多的操纵方法
        int fcntl(int fildes, int cmd);
        int fcntl(int fildes, int cmd, long arg); （fcntl.h）
        对打开的文件描述符执行各种操作，进行复制、获取和设置文件描述符标志、获取和设置文件状态标志、管理建议性文件锁等
        cmd参数的取值，定义在fcntl.h：
            fcntl(fildes, F_DUPFD, newfd)，返回一个新的文件描述符，其数值大于等于整数newfd，是fildes的一个副本，效果可能和系统调用dup一样
            fcntl(fildes, F_GETFD)，返回在fcntl.h里定义的文件描述符标志，包括FD_CLOEXEC，它的作用是决定是否在成功调用某个exec系列的系统调用之后关闭该文件描述符
            fcntl(fildes, F_SETFD, flags)，设置文件描述符标志，通常仅用来设置FD_CLOEXEC
            fcntl(fildes, F_GETFL)和fcntl(fildes, F_SETFL, flags)：
                分别用来获取和设置文件状态标志和访问模式
                可以利用fcntl.h中的掩码O_ACCMODE来提取出文件的访问模式
                其他标志包括open调用使用O_CREAT打开文件时作为第三参数出现的标志，不能通过fcntl设置文件的权限
    
    mmap函数：
        UNIX允许程序共享内存，Linux内核从2.0版本开始支持这一功能
        mmap（内存映射）函数的作用是建立一段可以被两个或更多个程序读写的内存，一个程序对它做出的修改可以被其他程序看见
        由记录组成的文件，记录又能够用C语言中的结构体描述，就可以通过访问结构数组来更新文件内容    
        这一功能可以用在文件的处理上，通过使用带特殊权限集的虚拟内存段来实现，对虚拟内存段的读写会使操作系统去读写磁盘文件中与之对应的部分
        
        void *mmap(void *addr, size_t len, int prot, int flags, int fildes, off_t off);
        mmap函数创建一个指向一段内存区域的指针，该内存区域与可以通过一个打开的文件描述符访问的文件的内容相关联
        off参数 改变经共享内存段访问的文件中数据的起始偏移值，fildes参数 打开的文件描述符
        len 可以访问的数据量（内存段的长度）
        通过addr参数请求使用某个特定的内存地址，如果addr取值为0，结果指针就将自动分配（推荐使用），不同系统上的可用地址范围不一样
        prot参数 用于设置内存段的访问权限（下列值的按位OR结果）：
            PROT_READ，允许读该内存段
            PROT_WRITE，允许写该内存段
            PROT_EXEC，允许执行该内存段
            PROT_NONE，该内存段不能被访问
        flags参数 控制程序对该内存段的改变所造成的影响：
            MAP_PRIVATE，内存段是私有的，对它的修改只对本进程有效
            MAP_SHARED，把对该内存段的修改保存到磁盘文件中
            MAP_FIXED，该内存段必须位于addr指定的地址处
    
    msync函数：
        把在该内存段的某个部分或整段中的修改写回到被映射的文件中（或从被映射的文件里读出）
        int msync(void *addr, size_t len, int flags);
        内存段需要修改的部分由起始地址addr和长度len确定
        flags控制执行修改的具体方式：
            MS_ASYNC，采用异步写方式
            MS_SYNC，采用同步写方式
            MS_INVALIDATE，从文件中读回数据
    
    munmap函数：
        释放内存段
        int munmap(void *addr, size_t len);
    
    2.0版本前的Linux内核不支持mmap
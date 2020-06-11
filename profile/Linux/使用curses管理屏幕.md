
## 使用curses函数库管理基于文本的屏幕

    函数库curses提供了终端无关的方式来编写全屏幕的基于字符的程序
    比直接使用escape转义序列要容易
    curses与X视窗系统（xterm窗口）中运行，其他有偏差

#### 用curses函数库进行编译

    curses函数库能优化光标的移动并最小化需要对屏幕进行的刷新，从而减少必须向字符终端发送的字符数目
    Linux使用的curses版本是ncurses（new curses）
    Linux下可能需要安装curses软件包：libncurses5-dev（或centos下ncurses-devel？）
    X/Open规范定义了两个级别的curses函数库：基本curses函数库和扩展curses函数库
    编译时：头文件curses.h，链接curses函数库：-lcurses 或 -lncurses
    gcc -I/usr/include/ncurses xxx.c -o xxx -lncurses，-I指定搜索头文件的目录
    参考ncurses的手册页查看配置？

#### curses术语和概念

    curses工作在屏幕、窗口和子窗口上
    curses用两个数据结构来映射终端屏幕，是stdscr和curscr
    stdscr会在curses函数库产生输出时被刷新，它对应的是标准屏幕，工作方式与stdio函数库中的stdout（标准输出）相似，是curses程序中的默认输出窗口
    curscr和stdscr相似，对应的是当前屏幕的样子
    程序调用refresh函数之前，输出到stdscr上的内容不会显示在屏幕上，refresh被调用时curses函数库会比较stdscr和curscr之间的不同之处，然后用它们之间的差异来刷新屏幕
    stdscr结构与具体实现相关，决不能被直接访问，curses无需使用curscr数据结构
    在curses程序中输出字符的过程：
        1）使用curses函数刷新逻辑屏幕；2）要求curses用refresh函数刷新物理屏幕
    逻辑屏幕的布局通过一个字符数组实现，以屏幕左上角为起点（0,0），通过行号和列号来组织（行,列），y值在前，x值在后
    每个位置包含该处的字符和属性，可显示的属性依赖物理终端的功能标志，一般支持粗体和下划线属性
    开始使用时进行初始化，结束使用后恢复原先设置，由initscr和endwin函数完成

#### 屏幕

    所有的curses程序必须以initscr函数开始，以endwin函数结束
    WINDOW *initscr(void);  （curses.h）//只能调用一次，成功返回指向stdscr结构的指针，失败输出诊断错误信息并使程序退出
    int endwin(void);   //成功返回OK，失败返回ERR
    
    输出到屏幕：
        int addch(const chtype char_to_add);
        int addchstr(chtype *const string_to_add);  //在当前位置添加指定的字符或字符串
        
        int printw(char *format, ...);  //与printf相同的字符串格式化，然后添加到光标的当前位置
        int refresh(void);  //刷新物理屏幕，成功返回OK，失败返回ERR
        int box(WINDOW *win_ptr, chtype vertical_char, chtype horizontal_char); //围绕一个窗口绘制方框
        
        int insch(chtype char_to_insert);   //插入一个字符，将已有字符右移
        int insertln(void); //插入一个空白行
        
        int delch(void);
        int deleteln(void); //与insert函数相反
        
        int beep(void); //发声，终端不能发声则使屏幕闪烁
        int flash(void);    //使屏幕闪烁，不能闪烁将尝试发声
        chtype，字符类型，比char类型包含更多的二进制位（unsigned long）

    从屏幕读取：
        chtype inch(void);  //总是可用，返回光标当前位置的字符及其属性信息
        int instr(char *string);    //不总被支持
        int innstr(char *string, int number_of_characters); //同上，将返回内容写到字符数组中
    清除屏幕：
        int erase(void);    //在每个屏幕位置写上空白字符
        int clear(void);    //类似erase，使用终端命令来清屏，clear+refresh，还通过咋内部调用一个底层函数clearok来强制重现屏幕原文，在下次调用refresh函数时重现屏幕原文
        int clrtobot(void); //清除当前光标位置直到屏幕结尾的所有内容
        int clrtoeol(void); //清除当前光标位置直到光标所在行行尾的所有内容
    移动光标：
        int move(int new_y, int new_x);     //将逻辑光标的位置移到指定地点，LINES、COLUMNS决定取值的最大值，move之后立刻调用refresh函数光标才会立刻显示在指定位置
        int leaveok(WINDOW *window_ptr, bool leave_flag);//设置一个标志，用于控制在屏幕刷新后curses将物理光标放置的位置，默认为false，屏幕刷新后硬件光标停在逻辑光标处，true 硬件光标会被随机放置在屏幕上的任意位置
    
    字符属性：
        每个curses字符都可以有一些属性用于控制该字符在屏幕上的显示方式（硬件设备能够支持属性）
        预定义的属性有：A_BLINK、A_BOLD、A_DIM、A_REVERSE、A_STANDOUT和A_UNDERLINE
        int attron(chtype attribute);   //在不影响其他属性的前提下启用指定属性
        int attroff(chtype attribute);  //在不影响其他属性的前提下关闭指定属性
        int attrset(chtype attribute);  //设置curses属性，单个或多个
        int standout(void);     //在不影响其他属性的前提下启用指定属性
        int standend(void);     //在不影响其他属性的前提下启用指定属性
    使用效果见 moveadd.c

#### 键盘

    控制键盘的简单方法
    键盘模式：
        键盘读取例程由键盘模式控制
        int echo(void);     //开启输入字符的回显功能
        int noecho(void);   //关闭输入字符的回显功能
        int cbreak(void);
        int nocbreak(void); //用于控制在终端上输入的字符传送给curses程序的方式
        int raw(void);
        int noraw(void);    //用于控制在终端上输入的字符传送给curses程序的方式
        调用initscr后，输入模式被设置为预处理模式（cooked模式），所有处理都是基于行的，只在按下回车后输入数据才被传送给程序
        预处理模式下键盘特殊字符被启用，组合键有效，流控也处于启用状态
        cbreak将输入模式设置为cbreak模式，字符一经键入就立刻传递给程序，不像cooked模式下先缓存
        cbreak模式下特殊字符被启用，但一些简单的特殊字符会被传递给程序，如Backspace，退格功能要自己实现
        raw调用的作用是关闭特殊字符的处理，特殊字符或组合键产生信号或进行流控就不可能了
        nocbreak将输入模式重置为cooked模式，特殊字符处理方式不变
        noraw同时恢复cooked模式和特殊字符处理功能
        
    键盘输入：
        int getch(void);
        int getstr(char *string);   //对返回的字符串长度没有限制，小心使用
        int getnstr(char *string, int number_of_characters);    //对读取的字符数加以限制，用它替换getstr
        int scanw(char *format, ...);
        与getchar、gets、scanf相似

#### 窗口

    可以用curses函数库在物理屏幕上同时显示多个不同尺寸的窗口
    许多函数只被X/Open规范定义的扩展curses函数库支持，ncurses函数库也支持
    多窗口的使用方法，以及将使用的函数通用化
    WINDOW结构：
        标准屏幕stdscr只是WINDOW结构的一个特例
        WINDOW *newwin(int num_of_lines, int num_of_cols, int start_y, int start_x);
        int delwin(WINDOW *window_to_delete);
        newwin创建一个新窗口，窗口从(start_y, start_x)开始，行数、列数，失败返回null
        将行、列设为0，则新窗口的右下角正好在屏幕的右下角
        所有的窗口范围都必须在当前屏幕范围内，否则newwin调用将失败，新窗口完全独立于所有已存在的窗口
        delwin删除一个通过newwin创建的窗口，回收内存
        千万不要删除curses自己的窗口stdscr和curscr
    
    通用函数：
        上面的一些函数可以通过加上前缀变为通用函数
        前缀w用于窗口，mv用于光标移动，mvw用于在窗口中移动光标
        w前缀的参数加上WINDOW，mv前缀的参数加上坐标值（y, x），坐标相对于窗口，（0,0）代表窗口左上角
        mvw前缀的参数加上WINDOW、y和x坐标值
        一些例子：
        int addch(const chtype char);
        int waddch(WINDOW *window_pointer, const chtype char);
        int mvaddch(int y, int x,  const chtype char);
        int mvwaddch(WINDOW *window_pointer, int y, int x, const chtype char);
        int printw(char *format, ...);
        int wprintw(WINDOW *window_pointer, char *format, ...);
        int mvprintw(int y, int x, char *format, ...);
        int mvwprintw(WINDOW *window_pointer, int y, int x, char *format, ...);
    
    移动和更新窗口：
        int mvwin(WINDOW *window_to_move, int new_y, int new_x);    //在屏幕上移动窗口，移出屏幕区域外，将调用失败
        int wrefresh(WINDOW *window_ptr);
        int wclear(WINDOW *window_ptr);
        int werase(WINDOW *window_ptr);     //同上面的相同
        int touchwin(WINDOW *window_ptr);   //通知curses其指针指向的窗口内容已发生改变，即下次调用wrefresh时，curses必须重绘该窗口，如窗口重叠时使用该函数安排要显示的窗口
        int scrollok(WINDOW *window_ptr, bool scroll_flag);//控制窗口的卷屏，true（非0值）允许卷屏，默认不能
        int scroll(WINDOW *window_ptr);     //把窗口内容上卷一行，wsctl函数可以指定卷行行数
    
    curses函数库不存储关于窗口之间的层次关系，必须自己管理多窗口的层次关系
    
    优化屏幕刷新：
        对通过慢速链路连接到主机的终端进行刷新
        尽量减少需要绘制的字符数目
        int wnoutrefresh(WINDOW *window_ptr);   //用于决定把哪些字符发送到屏幕上，但并不真正发送这些字符
        int doupdate(void);     //真正将更新发送到终端

#### 子窗口

    WINDOW *subwin(WINDOW *parent, int num_of_lines, int num_of_cols, int start_y, int start_x);
    int delwin(WINDOW *window_to_delete);
    subwin与newwin类似
    delwin也和上述一样
    可以使用以mvw前缀的函数写子窗口
    子窗口没有独立的屏幕字符存储空间，与其父窗口共享同一字符存储空间
    子窗口最主要的用途是提供一种简洁的方式来卷动另一窗口里的部分内容，卷动屏幕的某个小区域（小区域定义为子窗口）
    对子窗口：应用刷新屏幕之前必须先对其父窗口调用touchwin
    子窗口实际更新的是父窗口中的字符数据

#### keypad模式

    方向键、功能键、数字小键盘等，会发送以escape字符开头的字符串序列
    单独按下Escape键 和 按下某个功能键而生成的以Escape字符开头的字符序列 的区分
    不同终端对于同一逻辑按键使用不同字符串序列的情况
    curses.h以一组KEY_为前缀的定义来管理逻辑键     
    功能键对应的逻辑序列保存在terminfo结构中
    curses在启动时会关闭转义序列与逻辑键之间的转换功能，需要通过keypad来启用
    int keypad(WINDOW *window_ptr, bool keypad_on);
    成功返回OK，否则返回ERR，keypad_on为true，然后调用keypad函数启用keypad模式
    keypad模式下，curses将接管按键转义序列的处理工作，读键盘操作返回用户按下的键和与逻辑按键对应的KEY_定义
    keypad模式的三个限制：
        识别escape转义序列的过程是与时间相关的；（在慢速链路的数据传输和识别问题）
        为了curses能够区分 单独按下Escape键 和 按下某个功能键而生成的以Escape字符开头的字符序列，它必须等待一小段时间
        curses不能处理二义性的escape转义序列（终端上两个不同按键产生相同的转义序列，curses将不会处理，因为不知要返回哪个逻辑按键）
    键盘上的按键都将生成escape转义序列    
    键盘识别见：keypad.c

#### 彩色显示

    屏幕上每个字符位置都可以选择一种颜色作为前景色或背景色
    必须同时定义一个字符的前景色和背景色，称之为颜色组合（colorpair），字符颜色的定义和背景色的定义不独立
    bool has_colors(void);  （surses.h）//检查是否支持彩色显示，返回true，支持
    int start_color(void);  //初始化颜色显示功能，成功返回OK
    必须先检查当前终端是否支持彩色显示功能，然后对颜色例程进行初始化
    start_color调用成功，变量COLOR_PAIRS将被设置为终端所能支持的颜色组合数目的最大值，常见最大值为64
    变量COLORS定义可用颜色数目的最大值，一般只有8种，可用颜色以从0到63作为唯一ID号
    
    首先调用init_pair对准备使用的颜色组合进行初始化，然后才使用颜色属性，COLOR_PAIR函数对颜色属性进行访问
    int init_pair(short pair_number, short foreground, short background);
    int COLOR_PAIR(int pair_number);
    int pair_content(short pair_number, short *foreground, short *background);
    curses.h中一些以COLOR_为前缀的基本颜色
    pair_content获取已定义的颜色组合的信息
    init_pair(1, COLOR_RED, COLOR_GREEN)，定义一号颜色组合
    wattron(window_ptr, COLOR_PAIR(1))，通过COLOR_PAIR，将该颜色组合作为属性来访问，把屏幕上后续添加的内容设置为绿色背景上的红色内容
    wattron(window_ptr, COLOR_PAIR(1) | A_BOLD)，COLOR_PAIR就是一个属性，可以与其他属性结合使用，按位或

    int init_color(short color_number, short red, short green, short blue);
    对颜色进行重新定义，将一个已有的颜色（0到COLORS）以新的亮度值重新定义，亮度值范围0~1000，类似GIF格式的图片定义颜色值

#### pad

    pad数据结构，可以控制尺寸大于正常窗口的逻辑屏幕
    pad结构类似WINDOW，所有执行写窗口操作的curses函数同样可用于pad，pad还有自己的创建和刷新函数
    WINDOW *newpad(int number_of_lines, int number_of_columns);
    创建pad，删除使用delwin
    
    int prefresh(WINDOW *pad_ptr, int pad_row, int pad_column, int screen_row_min,
                    int screen_col_min, int screen_row_max, int screen_col_max);
    刷新操作，指定希望放到屏幕上的pad范围及其放置在屏幕上的位置
    将pad从坐标(pad_row,pad_column)开始的区域写到屏幕上指定的显示区域，显示区域的范围坐标（最小值到最大值）
    
    pnoutrefresh函数，作用于wnoutrefresh一样，更有效地更新屏幕


#### CD唱片应用程序

    具体实现见代码：curses_app.c
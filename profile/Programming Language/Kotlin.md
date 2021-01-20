## Kotlin

    静态类型，编译期间确定所有表达式的类型
    可空类型
    函数类型，函数式编程
    数据类，创建不可变对象的简明语法

#### 函数式编程

    核心概念：
        头等函数：把函数当作值使用，可以用变量保存它，把它当作参数传递，或者当作其他函数的返回值
        不可变性：使用不可变对象，保证了它们的状态在其创建之后不能再变化
        无副作用：使用的是纯函数，不会修改其他对象的状态，也不会和外面的世界交互
    lambda表达式
    多线程安全：不可变数据结构和纯函数，保证不会发生多线程修改

#### Kotlin基础

    函数和变量：
        函数：
            fun funcName(a: Int): Int { return if (a>b) a else b }
            if是表达式，不是语句
            表达式有值，且能作为另一个表达式的一部分；
            语句总是包围着代码块中的顶层元素，没有自己的值；java中的控制结构都是语句，kotlin中除了循环以外大多数控制结构都是表达式
            表达式函数体：函数体是由单个表达式构成的，去掉花括号和return语句，还可以省略返回类型
        变量：
            val str : String， 关键字+变量名+变量类型（可以省略）
            val（不可变引用，对于java的final）、var（可变引用）
            应该尽可能地使用val关键字，不可变引用、不可变对象及无副作用的函数更接近函数式编程风格（且线程安全）
            val只能初始化一次，可以根据条件使用不同的值初始化
        字符串格式化：（字符串模板）
            "hello, $name"， "hello, ${if(a>b) a else "cc"}"

    类和属性：
        值对象： class Person(val name: String)，public是默认的可见性
        属性：
            java中字段和其访问器的组合被叫作属性，只读属性只有getter
            kotlin隐藏了getter和setter
        自定义访问器：
            val isA: Boolean
                get() {
                    return a=b
                }
        源码布局：目录和包
            每个kotlin文件都以package语句开头，文件中的所有声明都在包中
            相同的包可以直接使用，不同的包需要导入 import
            import可以导入任何种类的声明
            一个kotlin文件可以包含多个类，即一个kotlin文件可以作为一个java的包（一个文件就可以是一个包）
            包层级结构不需要遵循目录层级结构
            推荐将多个类放进同一个文件中，特别是很小的类

    枚举和 when：
        声明枚举类：
            enum class Color(val r: Int, val g: Int, val b: Int) {
                RED(255, 0, 0), BLUE(0, 0, 255), INDIGO(75, 0, 130);//kotlin中唯一使用分号的地方
                fun rgb() = (r*256 + g)*256 + b
            }

        使用 when 处理枚举类：
            对应java的switch，但是更强大
            可以直接返回when表达式的表达式体函数
            when(color) {
                RED, YELLOW -> "warm"
                BLUE -> "cold"
            }
            when的实参可以是任何对象
            when(setOf(c1, c2)) {
                setOf(RED, YELLOW) -> ORANGE    //会创建额外的对象
                else -> XXX
            }
            when可以不带参数
            when {
                (a==b)||(a==c) -> ORANGE    //不会创建额外的对象
                else -> XXX
            }

        智能转换：合并类型检查和转换
            Expr接口，只是一个标记接口，用来给不同种类的表达式提供一个公共的类型
            class Num(val value: Int): Expr，：后面跟接口，标记类实现了接口
            a is b，类型检查，类似instanceOf
            a as b，显式的类型转换
            if(a is Sum)
                return eval(a.left)
            在类型检查之后不再需要显式的类型转换，由编译器执行，这种行为称为 智能转换
            智能转换的属性必须是val，且不能有自定义的访问器
        用when 代替 if：
            when与智能转换结合
            when(e) {
                is A -> e.value
                is B -> "b"
                else -> ""
            }
        代码块作为 if 和 when 的分支：
            代码块作为分支体，代码块中的最后一个表达式就是结果
            when(e) {
                is A -> {
                    println("logging")
                    e.value
                }
            }
            代码块中最后的表达式就是结果，在所有使用代码块并期望得到一个结果的地方都成立
            对try、catch、lambda表达式都适用
            对常规函数不成立

    迭代事物：while 循环和 for 循环
        while 循环：同java
        迭代数字：区间和数列
            区间，1..10，是包含或者闭合的，值是区间的一部分
            数列，迭代区间中所有的值的区间称作数列
            for (i in 1..100)，for (i in 'A'..'Z')，字符区间
            for (i in 100 downTo 1 step)，步长为2，方向倒序
            for (i in 0 until 100)，半闭合区间，不包含100，到100-1
            downTo、step、until
        迭代map：
            val bt = TreeMap<Char, String>()
            bt[key] = value
            for ((key, value) in bt)，for循环展开迭代中的集合的元素
            for ((index, element) in list.withIndex())，使用展开语法跟踪当前项的下标
        使用 in 检查集合和区间的成员：
            fun isLetter(c: Char) = c in 'a'..'z' || c in 'A'..'Z'
            fun isNotDigit(c: Char) = c !in '0'..'9'
            等同Java的 < > 比较操作符
            in、!in 也适用于when表达式
            in 也适用于实现Comparable接口的类 以及 集合（Set等）

    Kotlin中的异常：
        throw Exception()，抛出异常，throw结构是一个表达式，能作为另一个表达式的一部分使用
        val per = if (xx) num else throw Exception()，条件成立per被初始化，不成立抛出异常且不初始化变量
        try catch 和 finally：
            try {
            } catch(e) {} finally {}
            函数声明后不用写上 throws，即受检异常不需要显式地处理
            Kotlin不区分受检异常和未受检异常，可以不处理异常
        try 作为表达式：
            val num = try { 10 } catch(e) { return }，return 或 null
            和 if when 一样，表达式体

#### 函数的定义与调用

    在Kotlin中创建集合：
        val set = hashSetOf(1, 3, 90)
        val list = arrayListOf(1, 3, 99)
        val map = hashMapOf(1 to "one", 5 to "five")，to 是个普通函数
        list.last()，set.max()，kotlin扩展函数

    函数调用：
        命名参数：
            func(sep = "", prefix = " ", postfix = ".")
            调用函数时可以显式的标明一些参数的名称
            调用Java函数时不能采用命名参数
        默认参数值：
            声明函数时可以指定参数的默认值，这样可以避免创建重载的函数
            fun func(sep = "", prefix = " ", postfix = ".")
            常规调用语法必须按参数顺序来给定参数，命名参数更灵活
            @JvmOverloads，注解方法时会为该方法生成Java重载函数（从最后一个开始省略参数）
        消除静态工具类：顶层函数和属性
            Java工具类不适用类的概念
            Kotlin中把这些东西放到代码文件的顶层，不用从属于任何的类
            包外访问需要 import
            编译成Java类为 代码文件名作为类名，方法为静态方法，在Java中访问时 文件名.function()
            修改文件生成的类名使用@JvmName注解，要放在文件开头，包名前面
            eg: @file:JvmName("className")
                package xxx
                fun func()
            调用时： className.func()

            顶层属性：
            和函数一样，属性也放到文件顶层
            会存储到静态字段中
            val可定义常量
            const修饰（使用于所有基本数据类型以及String类型）
            const val xx 等同于 Java代码：public static final String xx

    给别人的类添加方法：扩展函数和属性
        扩展函数：
            就是一个类的成员函数，定义在类的外面
            fun String.lastChar() = this.get(this.length-1)
            类的名称称为接收者类型，调用扩展函数的对象叫作接收者对象
            可以像调用类的普通成员函数一样去调用扩展函数
            扩展函数中可以直接访问被扩展的类的其他方法和属性（不能访问私有或受保护的成员）
        导入和扩展函数：
            导入单个的函数，import strings.lastChar
            *
            使用关键字as 修改导入的类或函数名称，import strings.lastChar as last
            as 用来解决命名冲突
        从Java中调用扩展函数：
            扩展函数实质上是静态函数，把调用调用对象作为第一个参数
            扩展函数被声明为顶层函数，被编译为一个静态函数，文件名+函数名调用（UtilKt.func()）
        作为扩展函数的工具函数：
            可使用泛型作工具函数等
            使用泛型指定类型等
        不可重写的扩展函数：
            类的重写成员函数是可以的，但不能重写扩展函数
            扩展函数是静态函数，调用重名的扩展函数时，由该变量的静态类型决定，而不是变量的运行时类型
            静态函数不存在重写
            注意：一个类的成员函数和扩展函数有相同的签名，成员函数会被优先使用（从而使扩展函数失效）
        扩展属性：
            val String.lastChar: Char
                get() = get(length-1)
            必须定义getter函数，因为没有默认的get实现，初始化也不行，因为没有地方存储初始值
            var、val 根据实际请求设置可变性
            Java调用时 StringUtilKt.getLastChar("Java")

    处理集合：可变参数、中缀调用和库的支持
        可变参数（vararg）、中缀表示法、解构声明（把组合值展开到多个变量中）
        扩展Java集合的API：
            last、max、创建集合的函数
        可变参数：让函数支持任意数量的参数
            fun listOf<T>(vararg values: T): List<T> {...}
            vararg修饰符（可变参，将任意个数参数值打包到数组中）
            可变参传递数组时需要显式地解包数组（Java可按原样传递）
            解包数组被称为展开运算符（*），eg：listOf("args: ", *args)
            可以单个调用中组合数组的值和某些固定值（Java不支持）
        键值对的处理：中缀调用和解构声明
            mapOf(1 to "one", 2 to "two")
            to 是一种特殊的函数调用，称为中缀调用
            1.to("one") 等价于 1 to "one"
            中缀调用可以与只有一个参数的函数一起使用，需要使用infix修饰符标记
            infix fun Any.to(other: Any) = Pair(this, other)
            直接用Pair的内容初始化两个变量：
                val (number, name) = 1 to "one"，称为解构声明
                用to函数创建一个Pair，然后用解构声明来展开
            解构声明的使用：for((index, item) in collection.withIndex())
            to函数可以创建一对任何元素

    字符串和正则表达式的处理：
        分割字符串：
            Java的split不支持 . \ | - ，它把正则表达式作为参数
            Kotlin重载该方法，需要显式地传递正则，或者是支持指定多个分隔符
        正则表达式和三重引号的字符串：
            val regex = """(.+)/(.+)\.(.+)""".toRegex()，
            val mr = regex.matchEntire("xxxxx")， val (dir, name, ext) = mr.destructured
            Kotlin中正则表达式的使用
            三重引号的字符串中不需要对任何字符进行转义，包括\
        多行三重引号的字符串：
            三重引号可以包含任何字符，包括换行符等
            可以向字符串添加前缀标记边距的结尾，然后调用trimMargin(".")删除每行中的前缀和前面的空格
            包含换行（不是 \n），不必转义字符\
            使用字符串模板必须使用嵌入式表达式，eg："""${'$'}99.9"""

    让代码更整洁：局部函数和扩展
        在函数中嵌套提取的函数
        fun outFunction() {
            fun innerFunction() {
            }
            innerFunction()//局部函数调用
            innerFunction()
        }
        局部函数可以访问所在函数中的所有参数和变量
        类的API只能包含必需的方法
        扩展函数也可以被声明为局部函数，不建议使用多层嵌套

#### 类、对象和接口

    Kotlin接口可以包含属性声明
    Kotlin的声明默认是final和public的
    Kotlin嵌套类默认不是内部类（没有包含对其外部类的隐式引用）

    类继承结构：
        Kotlin中的接口：
            interface Clickable {
                fun click()
                fun showOff() = xxxx //默认实现（Java8需要default关键字）
            }
            与Java8中的相似，可以包含抽象方法以及非抽象方法的实现，不能包含状态
            class Button : Clickable {
                override fun click() = xxx
            }
            : 代替了extends和implement关键字，一个类可实现多个接口，但只能继承一个类
            override修饰符（强制使用）类似Java的注解
            同时实现的多个接口中有同名方法时必须得显式实现（使用 super<Clickable>.showOff() 表明想要调用哪个父类的方法）
            Java实现Kotlin接口时必须为所有的方法都提供自己的实现（即使是默认方法也要）
        open、final和abstract修饰符：默认为final
            没有特别需要在子类中被重写的类和方法应该被显式地标注为final
            Java类和方法默认是open的，Kotlin中默认都是final的
            需要被继承的类和被重写的属性或方法要使用open修饰符标示
            open class Rich : Clickable {
                open fun animate() {}
                final override fun click() {}
            }
            重写了的成员（方法、属性）同样默认是open的，需要显式地标注final（禁止重写）
            抽象类（类似Java抽象类）：
            abstract class Animated {
                abstract fun animate()
            }
            抽象成员始终是open的，不需要显式地使用open，非抽象函数默认不是open的
            接口中的成员始终是open的，不能将其声明为final
            类中的访问修饰符：
            final（不能被重写）、open（可以被重写）、abstract（必须被重写）、override（重写的成员）
        可见性修饰符：默认为public
            public（所有地方可见）、protected（子类中可见）
            private（类中可见或文件中可见）、internal（表示只在模块内部可见）
            一个模块就是一组一起编译的Kotlin文件
            Kotlin允许在顶层声明中使用private可见性，包括类、函数和属性，这些声明就只在声明的文件中可见
            类的扩展函数不能访问它的private和protected成员
        内部类和嵌套类：默认是嵌套类
            Kotlin的嵌套类不能访问外部类的实例
            内部类需要使用inner修饰符（持有外部类的引用）
            嵌套类：class A，内部类：inner class A。同Java的静态内部类和内部类
            内部类访问外部类，使用 this@Outer
            class Outer {
                inner class Inner {
                    fun getOuterRef() = this@Outer
                }
            }
        密封类：定义受限的类继承结构
            sealed类，为父类添加一个sealed修饰符，对子类做出严格的限制，所有的直接子类必须嵌套在父类中
            sealed class Expr {
                class Num(): Expr()
                class Sum(): Expr()
            }
            sealed标记为密封的，将所有可能的类作为嵌套类列出
            sealed隐含这个类是open的
            密封类不能在类外部拥有子类

    声明一个带非默认构造方法或属性的类：
        主构造方法（在类体外部声明）和从构造方法（在类体内部声明），初始化语句块
        初始化类：主构造方法和初始化语句块
            主构造方法：class User(val nickname: String)，表明构造方法的参数，以及定义属性
            class User constructor(name: String) {
                val nickname: String
                init { nickname = name }
            }
            constructor关键字用来声明一个主构造方法或从构造方法
            init关键字用来引入一个初始化语句块，类被创建时执行的代码
            如果主构造方法没有注解或可见性修饰符，可以去掉constructor关键字
            构造方法中的 val 意味着相应的属性会用构造方法的参数来初始化
            可以为构造方法参数提供一个默认值，也可以显式地为某些参数标明名称，同普通函数使用一样
            如果所有参数都有默认值，编译器会生成一个无参构造方法
            class TUser(nickname: String) : User(nickname) {...}
            主构造方法需要初始化父类，基类提供父类构造方法的参数
            open class Button {}，不带参数，会有默认构造方法
            class SButton: Button()，必须显式地调用父类的构造方法（带有括号），接口不需要括号（这就是区别）
            class Service private constructor() {}，不被其他代码实例化
            Java的静态工具类可以使用顶层函数，单例可以使用对象声明（伴生对象）
        构造方法：用不同的方式来初始化父类
            open class View {
                constructor(ctx: Context, attr: Attr) {...}
            }
            class Button : View {
                constructor(ctx: Context): this(ctx, NULL) {...}
                constructor(ctx: Context, attr: Attr): super(ctx, attr) {...}
            }
            如果类没有主构造方法，那每个从构造方法必须初始化基类（super）或委托（this）给另一个这样做了的构造方法
        实现在接口中声明的属性：
            interface User {
                val nickname: String        //抽象属性声明
            }
            实现接口的类需要提供一个取得属性值的方法（getter）
            class PUser(override val nickname: String) : User   //主构造方法属性
            class SBUser(val email: String) : User {
                override val nickname: String
                    get() = email.sub()     //自定义getter
            }
            class FBUser(val id: Int): User {
                override val nickname = getFBName(id)       //属性初始化
            }
            接口还可以包含具有getter和setter的属性，只要它们没有引用一个支持字段（具有默认getter的属性可以被继承，可以不重写）
        通过getter或setter访问支持字段：
            存储值的属性和具有自定义访问器在每次访问时计算值的属性
            val address: String
                set(value: String) {field = value ...}
                get() {...  return field}
            使用field则含有支持字段，不适用field，则支持字段不会被呈现出来
            val 就是getter，var 则是两个访问器
        修改访问器的可见性：
            访问器的可见性默认与属性可见性相同
            val counter: Int = 0
                private set         //修改可见性，不能在类外部修改这个属性
            lateinit修饰符，这个属性将初始化推迟到构造方法被调用过后
            惰性初始化属性，委托属性的一部分
            const修饰符，注解的使用

    编译器生成的方法：数据类和类委托
        通用对象方法：
            toString、equals、hashCode
            重写，override
            对象相等性，equals，Kotlin中的 == 代表equals
            ===运算符与Java中的 == 一样（比较对象引用）
            is、!is （同instanceof）
            hashCode 通常与 equals 一起被重写
            如果两个对象相等，它们必须有着相同的hash值
            hashCode 重写一般为： value.hashCode() * 31 + num
        数据类：自动生成通用方法的实现
            data class Client(val name: String, val num: Int)
            Kotlin为 data 类自动生成 toString、equals、hashCode
            equals和hashCode会将所有在主构造方法中声明的属性纳入考虑
            没有在主构造方法中声明的属性将不会加入到相等性检查和哈希值计算
            数据类和不可变性：copy()方法
            推荐使用val 让数据类实例不可变
            Kotlin生成的创建副本的方法，copy()
                fun copy(name: String = this.name, num: Int = this.num) = Client(name, num)
            copy的同时可以修改某些属性的值
        类委托：使用 by 关键字
            装饰器模式，向不可被修改的类扩展其功能，拥有一个原始类实例，组合和转发
            Kotlin将委托作为一个语言级别的功能，可以使用by关键字将接口的实现委托到另一个对象
            class DCollection(innerList: Collection = ArrayList()) : Collection by innerList {
                //可以重写需要修改的方法
                override fun add(element: T): Boolean {
                    //do something
                    return innerList.add(element)
                }
            }
            by将DCollection的实现委托给 innerList，可以重写需要实现的方法

    object 关键字：将声明一个类与创建一个实例结合起来
        object关键字定义一个类并同时创建一个实例（对象），使用场景：
            对象声明是定义单例的一种方式
            伴生对象可以持有工厂方法和其他与这个类相关，但调用时不依赖类实例的方法，通过类名来访问
            对象表达式用来替代Java的匿名内部类
        对象声明：创建单例易如反掌
            对象声明将类声明与该类的单一实例声明结合到了一起
            object Singleton {
                fun do() {}
            }
            对象声明通过object关键字引入，对象声明不允许构造方法（主、从构造方法），其他与类一样（属性、方法、初始化语句块等）
            对象声明在定义的时候就立即创建了，不需要在其他地方调用构造方法
            使用方式：Singleton.do()，方法、属性都直接使用对象名+点的方式
            可以在类中声明对象，这样的对象同样只有一个单一实例（多个类实例共用一个声明对象？）
            data class Person(val name: String) {
                object NameComparator: Comparator<Person> {...}
            }
            Kotlin中的对象声明被编译成通过静态字段来持有它的单一实例的类（字段名始终都是INSTANCE，通过INSTANCE使用单例）
        伴生对象：工厂方法和静态成员的地盘
            Kotlin的类不能有静态成员（static）
            顶层函数和对象声明（包括静态方法和静态字段）作为static的替代
            顶层函数不能访问类的private成员，但是类中的对象声明可以
            class A {
                companion object {
                    fun bar() {...}
                }
            }
            调用方式：A.bar()
            companion，使可以通过容器类名称（A）直接访问声明对象的方法和属性
            适合使用工厂方法
        作为普通对象使用的伴生对象：
            伴生对象是一个声明在类中的普通对象，可以有名字、实现接口、有扩展函数或属性
            class P() {
                companion object Loader {
                    fun from(): P {}
                }
            }
            在伴生对象中实现接口，可以直接将包含它的类的名字当作实现了该接口的对象实例来使用
            class P() {
                companion object: JSONFactory {...}
            }
            fun loadFromP(factory: JSONFactory): P {...}
            loadFromP(P)    //直接使用容器类名 P
            伴生对象会被编译成 类中的一个引用了它实例的静态字段，若伴生对象没有名字，静态字段默认为Companion
            @JvmStatic、@JvmField 注解声明static字段（Java使用）
            伴生对象扩展
            如果类C有一个伴生对象，在C.Companion上定义一个扩展函数func，那可以通过C.func()来调用
            class P() {
                companion object {}
            }
            fun P.Companion.fromJSON(json: String): P {}
            P.fromJSON(json)    //调用
        对象表达式：改变写法的匿名内部类
            object声明匿名对象，替代了Java中的匿名内部类
            window.addMouseListener(object: MouseAdapter() {
                override fun clicked(){...}
            })
            匿名对象可以实现多个接口或者不实现接口
            匿名对象不是单例，每次对象表达式被执行都会创建一个新的对象实例
            对象表达式能修改在创建对象的作用域中定义的变量，Java中访问被限制在final变量

#### Lambda编程

    lambda，本质上就是可以传递给其他函数的一小段代码
    Lambda表达式和成员引用
        Lambda简介：作为函数参数的代码块
            函数式编程，把函数当作值来对待，直接传递函数（不需要先声明类传递类实例）
            lambda表达式可以直接传递代码块作为函数参数
            lambda被当作只有一个方法的匿名对象的替代品
        Lambda和集合：
            避免重复代码，使用库函数
            maxBy函数，可以在任何集合上调用
            成员引用，list.maxBy(Person::age)
        Lambda表达式的语法：
            val sum = {x: Int, y: Int -> x + y}，Kotlin的lambda表达式始终用花括号包围，实参没有用()括起来
            可以把lambda表达式存储在一个变量中，把变量当作普通函数对待（通过实参调用），如：sum(1, 2)
            lambda表达式的简化：
            1、如果lambda表达式是函数调用的最后一个实参，它可以放到括号外边，如：list.maxBy() {...}
            2、当lambda是函数唯一的实参时，还可以去掉调用代码中的空括号，如：list.maxBy {...}
            3、如果lambda参数的类型可以被推导出来，那就不需要显式地指定它，如：list.maxBy { p -> p.name }
            4、使用默认参数it代替命名参数（如果lambda只有一个参数且这个参数类型可以推断处理，就会生成it），如：list.maxBy{ it.name }
                仅在实参名称没有显式指定时才会生成默认名称it
            lambda表达式中的最后一个表达式就是它的结果
        在作用域中访问变量：
            在函数内部使用lambda，它可以访问这个函数的参数，还有在lambda之前定义的局部变量
            Kotlin的lambda中允许访问非final变量，还可以修改这些变量
            从lambda内访问外部变量，称这些变量被lambda捕捉
            捕捉可变变量的实现细节：final变量的值会被拷贝下来，而可变变量的值被作为Ref类的一个实例被存储下来，Ref变量是final的，实际值被存储在其字段中，这样就可以在lambda内修改
        成员引用：
            把函数转换成一个值，就可以传递它，使用::运算符来转换，eg：val getAge = Person::age
            这种表达式称为成员引用，创建一个调用单个方法或者访问单个属性的函数值（方法或属性），后面不能加括号
            成员引用和lambda具有一样的类型，可以互换使用
            fun salute() = println("xxx")
            run(::salute)，引用顶层函数（不是类的成员）
            可以用构造方法引用 存储或延期执行创建类实例的动作，构造方法引用的形式是在::后指定类名
            class P(val name: String)，val createP = ::P，val p = createP("jec")
            扩展函数也可以使用成员引用的方式
            绑定引用
    集合的函数式API
        函数式编程风格，通用的库函数
        基础：filter和map
            filter和map是集合操作的基础，很多集合操作都是借助它们达成的
            filter函数遍历集合并选出应用给定lambda后会返回true的那些元素（过滤符合条件的元素）
            list.filter{ it%2==0 }，返回值是个集合
            map函数对集合中的每一个元素应用给定的函数并把结果收集到一个新集合（转换每个元素）
            map的结果是新集合，但包含的元素个数不变
            使用这些运算符时注意不要重复运算等操作
        all、any、count 和 find：对集合应用判断式
            all、any函数，用于检查集合中所有元素是否都符合某个条件（或是否存在符合的元素）
            count函数，检查有多少元素满足判断式
            find函数，返回第一个符合条件的元素（同firstOrNull）
            all 是否所有元素都满足，any 集合中是否至少存在一个匹配的元素
        groupBy：把列表转换成分组的map
            把所有元素按照不同的特征分成不同的分组
            list.groupBy{...}，结果是一个map，key是元素分组依据的键，value是元素
            list.groupBy(String::first)，把字符串按照首字母分组，first为扩展函数
        flatMap和flatten：处理嵌套集合中的元素
            books.flatMap {it.authors}.toSet()
            flatMap首先对集合中的每个元素做变换（或映射，通过给定的函数），然后把多个列表合并（平铺）成一个列表
            flatten函数，只是平铺一个集合（把多个列表合并成一个）
    惰性集合操作：序列
        链式集合操作的代码的性能
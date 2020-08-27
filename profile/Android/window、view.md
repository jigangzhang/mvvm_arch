## Activity、Window、View

    View、Window以及Activity主要是用于显示并与用户交互
    window	view	activity	surfaceView

	MeasureSpec.EXACTLY = match_parent；1073741824	
	MeasureSpec.AT_MOST = wrap_content；-2147483648

#### 三者间的关系

	在Activity中要设置View时，通常用的方法是:setContentView(id);
	而实际在Activity类中用的是:getWindow().setContentView(id);
	此即view是放到window上的，获得window的方法是:activity.getWindow();
	Activity实际上是继承 Context类
	window是在Activity的 attach()	方法中实例化的:	window = new PhoneWindow(this, window);
	
	attach 优先于 onCreate 调用
	即:activity调用attach创建window(PhoneWindow)对象，在activity中调用setContentView()向window中添加view		PhoneWindow
	实际上是在PhoneWindow 中的setContentView()方法中向ViewGroup(root)中添加view
	
	dialog 	Toast都是在window上显示的
	
	window代表一块可绘制显示的区域，系统给window提供可绘制图形的surface对象，而不管如何绘制；
	window是为了管理view，为每个view提供canvas，去绘制.
	
	Activity相当于一个界面，可直接在Activity里处理事件，Activity初始化时得到一个window对象，window用于管理view，view即实际显示到界面上的图形...

#### View绘制流程

	view树结构	onMeasure() --> onLayout() --> onDraw()
	measure：测量组件大小	layout：确定组件在视图中的位置	draw：根据位置和大小画组件
	视图绘制的起点在ViewRootImpl类的performTraversals()方法，该方法完成的工作主要是： 
	    根据之前的状态，判定是否重新计算测试视图大小（measure）、是否重新放置视图位置（layout）和是否重新重绘视图（draw） 
	
	setContentView流程，setContentView整个过程主要是把Activity的布局文件或者java的View添加至窗口里，重点概括为：
		创建一个DecorView的对象mDecor，该mDecor对象将作为整个应用窗口的根视图。
		依据Feature等style theme创建不同的窗口修饰布局文件，
        并且通过findViewById获取Activity布局文件该存放的地方（窗口修饰布局文件中id为content的FrameLayout）。
		将Activity的布局文件添加至id为content的FrameLayout内。
		当setContentView设置显示OK以后会回调Activity的onContentChanged方法。
        Activity的各种View的findViewById()方法等都可以放到该方法中，系统会帮忙回调。

    ViewRoot是连接WindowManager与DecorView的纽带，View的整个绘制流程的三大步（measure、layout、draw）都是通过ViewRootImpl完成的。
    当Activity对象被创建完毕后，会将DecorView添加到Window中（Window是对窗口的抽象，DecorView是一个窗口的顶级容器View，其本质是一个FrameLayout），
    同时会创建ViewRootImpl（ViewRoot的实现类）对象，并将ViewRootImpl与DecorView建立关联。
    关于ViewRoot，我们只需要知道它是联系GUI管理系统和GUI呈现系统的纽带。
    View的绘制流程从ViewRoot的performTraversals方法开始，经过measure、layout、draw三大过程完成对一个View的绘制工作。

    peformTraversal方法内部会调用measure、layout、draw这三个方法，这三个方法内部又分别调用onMeasure、onLayout、onDraw方法。		
    ViewRoot对应于ViewRootImpl类，它是连接WindowManager和DecorView的纽带，View的三大流程是通过VeiwRoot来完成的。在ActivityThread中，
    当Activity对象被创建完毕后，会将DecorVeiw添加到Window中，同时会创建ViewRootImpl对象，并将ViewRootImpl对象和DecorView建立关联。

    Activity内部有个Window成员，它的实例为PhoneWindow，PhoneWindow有个内部类是DecorView，这个DecorView就是存放布局文件的，
    里面有TitleActionBar[TitleView(ActionBar的容器)]和我们setContentView传入进去的layout布局文件

	使用View的getMeasuredWidth()和getMeasuredHeight()方法来获取View测量的宽高，
	必须保证这两个方法在onMeasure流程之后被调用才能返回有效值。
	使用View的getWidth()和getHeight()方法来获取View测量的宽高，必须保证这两个方法在onLayout流程之后被调用才能返回有效值。

    onDraw：
        区分View动画和ViewGroup布局动画，前者指的是View自身的动画，可以通过setAnimation添加，
        后者是专门针对ViewGroup显示内部子视图时设置的动画，可以在xml布局文件中对ViewGroup设置layoutAnimation属性
        （譬如对LinearLayout设置子View在显示时出现逐行、随机、下等显示等不同动画效果）。
	
    invalidate和postInvalidate方法：
        请求重新绘制视图，调用draw
        invalidate在主线程调用
        postInvalidate是在非主线程调用
	
    View的requestLayout方法：
        requestLayout()方法会调用measure过程和layout过程，不会调用draw过程，也不会重新绘制任何View包括该调用者本身。	
	
	viewRoot	DecorView
	ViewRoot实际是一个Handler，ViewRoot建立主View与WindowsManger通讯的桥梁。ViewRoot在本质上一个Handler。
	ViewRoot 对应于ViewRootImpl类，View的三大流程都是通过ViewRoot来完成的。在ActivityThread中，当Activity对象被创建完毕之后，
	会将DecorView添加到Window中，同时会创建ViewRootImpl对象，并将ViewRootImpl对象和DecorView建立关联。
	
	DecorView作为顶级View，一般情况下它内部会包含一个竖直方向的LinearLayout，在这个LinearLayout里面有上下两个部分，
	上面是标题栏，下面是内容栏。DecorView其实是一个Framglayout，View层的事件都先经过DecorView，然后才传递给我们的View。

#### window

	Window： 表示一个窗口，不一定有屏幕那么大，可以很大也可以很小；
             它包含一个View tree和窗口的layout 参数。
             View tree的root View可以通过getDecorView得到。还可以设置Window的Content View。
	
	WindowManager并不是整个系统的窗口管理器，而是所在应用进程的窗口管理器。
	系统全局的窗口管理器运行在SystemServer进程中，是一个Service。
	
	每个窗口对应着一个Window对象，一个根View和一个ViewRoot对象。要想创建一个窗口，可以调用
	WindowManager的addView方法，作为参数的view将作为在该窗口上显示的根view 

	window里显示的东西是view(activity是逻辑上的东 西，增加了生命周期管理等. 里面具体的东西也是view。
	而且启动activity的实现也是往window里加view),往window里加view,是通过调用 WindowManager（WindowManagerImpl）.addView()来实现的。
	
	在 addView里，会为每个view创建一个viewRoot(这是逻辑上的东西，用来负责view的事件处理和逻辑处理，
	并和 WindowsManagerService建立联系），而在WindowManagerImpl里，会维护viewRoot的数组。
	最终会调用ViewRoot.setView()，在setView里会显示该view等。在setView的实现里，会调用Session.add（）来 addWindow,通过这个方法，
	会将ViewRoot和WindowManagerService联系起来（比如说分发事件等）,并且这个方法里会调 用PhoneWindowManager.prepareAddWindowLw()。

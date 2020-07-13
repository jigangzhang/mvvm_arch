#include <stdio.h>

//满二叉树，每个分支都有左右子结点，且所有叶子结点都集中在二叉树最下一层
//完全二叉树，满二叉树从右至左，从下至上，依次挨个删除结点所得
//总分支数=总结点数-1， 总分支数=单分支结点数+双分支结点数 * 2
//二叉树第 i 层结点数<=2^(i-1)
//二叉树总结点数 <= 2^k - 1
//第 i 个结点的双亲结点编号为：i/2，以0开始时为： i/2 - 1
//第 i 个结点的左孩子编号为：2*i， 2*i>n 表示无左孩子； 以0开始时为：2*i+1
//第 i 个结点的右孩子编号为：2*i+1，2*i+1>n 表示无右孩子；以0开始时为：2*i+2
//顺序存储结构：使用数组存储，最适合于完成二叉树，用于存储一般二叉树会浪费大量存储空间，父子结点关系如上

//链式存储结构
typedef struct BTNode {
	char data;
	struct BTNode *lchild;
	struct BTNode *rchild;
}BTNode;

//先序遍历，根 左 右
void preorder(BTNode *p) {
	if(p != NULL)
		visit(p);
	preorder(p->lchild);
	preorder(p->rchild);
}

//中序遍历，左 根 右
void inorder(BTNode *p) {
	if(p != NULL) {
		inorder(p->lchild);
		visit(p);
		inorder(p->rchild);
	}
}

//后序遍历，左 右 根
void postorder(BTNode *p) {
	if(p != NULL) {
		postorder(p->lchild);
		postorder(p->rchild);
		visit(p);
	}
}

int getDepth(BTNode *p) {
	if(p == NULL)
		return 0;
	int ld, rd;
	ld = getDepth(p->lchild);
	rd = getDepth(p->rchild);
	return (ld > rd ? ld : rd) +1;
}

BTNode *search(BTNode *p, char key) {
	if(p == NULL)
		return NULL;
	if(p->data == key)
		return p;
	else {
		BTNode *t =  search(p->lchild);
		if(t == NULL)
		t = search(p->rchild);
		return t;
	}
}

//层次遍历，自左至右，自上至下，使用队列，对每个结点进行入队 出队操作
void level(BTNode *p) {
	int maxSize = 10;
	int front, rear;
	BTNode *que[maxSize];
	front = rear = 0;
	BTNode *q;
	if(p != NULL) {
		rear = (rear + 1) % maxSize;
		que[rear] = p;
		while(front != rear) {
			front = (front + 1) % maxSize;
			q = que[front];
			visit(q);
			if(q->lchild != NULL) {
				rear = (rear + 1) % maxSize;
				que[rear] = q->lchild;
			}
			if(q->rchild != NULL){
				rear = (rear+1) % maxSize;
				que[rear] = q->rchild;
			}
		}
	}
}

//先序遍历的非递归实现
void preorderNonrecursion(BTNode *pt) {
	if(p == NULL) return;
	int maxSize = 10;
	BTNode *stack[maxSize];
	int top = -1;
	BTNode *p;
	stack[++top] = pt;
	while(top != -1) {
		p = stack[top--];
		visit(p);
		if(p->rchild != NULL)
			stack[++top] = p->rchild;
		if(p->lchild != NULL)
			stack[++top] = p->lchild;
	}
}

//中序遍历非递归
void inorderNonrecursion(BTNode *pt) {
	if(pt == NULL) return;
	BTNode *stack[maxSize];
	int top = -1;
	BTNode *p = pt;
	while(top != -1 || p != NULL) {
		while(p != NULL){
				stack[++top] = p;
			p = p->lchild;
		}
		if(top != -1) {
			p = stack[top--];
			visit(p);
			p->rchild;
		}
	}
}

//线索二叉树，把二叉树的遍历过程线性化，对二叉树中所用结点的空指针域按照某种方式加线索的过程叫做线索化
//线索化，把树中的空指针利用起来作为寻找当前结点前驱或后继的线索，被线索化的二叉树叫做线索二叉树
typedef struct TBTNode {
	char data;
	int ltag, rtag;	//线索标记， 0表示child为指针，指向子结点；1表示child为线索，指向结点的直接前驱（左）、直接后继（右）
	struct TBTNode *lchild;
	struct TBTNode *rchild;
}TBTNode;

//将一颗树转换为二叉树：使一个结点的lchild指向其子结点，使其rchild指向它的兄弟结点
//二叉树转换为树：上面的逆过程
//森林转换为二叉树：先将每棵树转换为二叉树，然后根结点的右子树指向下一颗二叉树，依次连接
//二叉树转换为森林：将根结点有右孩子的二叉树的右孩子链接断开，直到不存在根结点有右孩子的二叉树为止，然后将多个二叉树转换为树

//图的存储结构：邻接矩阵、邻接表
//邻接矩阵：是表示顶点之间相邻关系的矩阵，A[i][j]=1 表示顶点i与顶点j邻接，即i与j之间存在边；A[i][j]=0 表示不邻接
//邻接矩阵是图的顺序存储结构，由邻接矩阵的行数或列数可知图中的顶点数
//对无向图，邻接矩阵是对称的，矩阵中 1 的个数为图中总边数的两倍，矩阵第i行或第i列的元素之和即为顶点i的度
//对有向图，矩阵中1 的个数为图的边数，矩阵中第i行的元素之和即为顶点i的出度，第j列的元素之和即为顶点j的入度
//对有权图，邻接矩阵中的值即为权

typedef struct {
	int no;	//顶点编号
	char info;	//顶点其他信息
}VertexType;

int maxSize = 10;
typedef struct {
	int edges[maxSize][maxSize];	//矩阵中的值，0 不邻接 1 邻接；如果是有权图，此为权，可使用float
	int n, e;	//分别是顶点数和边数
	VertexType vex[maxSize];	//存放结点信息
}MGraph;	//图的邻接矩阵类型

//邻接表：是图的一种链式存储结构，对图中的每个顶点i建立一个单链表，单链表第一个结点存放顶点信息，为表头，其余结点存放边的信息
//邻接表由单链表的表头形成的顶点表和单链表其余结点形成的边表两部分组成
//边结点存放与当前顶点相邻接顶点的序号和指向下一个边结点的指针

typedef struct ArcNode {
	int adjvex;	//该边指向的结点的位置
	struct ArcNode *nextarc;	//指向下一条边的指针
	int info;	//边的相关信息
}ArcNode;

typedef struct VNode {
	char data;	//顶点信息
	ArcNode *firstarc;	//指向边的指针
}VNode;

typedef struct {
	VNode adjlist[maxSize];	//邻接表
	int n, e;	//顶点数和边数
}AGraph;

//图的深度优先搜索遍历，DFS：任取一个顶点，访问它，然后检查这个顶点的所有邻接顶点，递归访问其中未被访问过的顶点
int visit[maxSize];

void DFS(AGraph *G, int v) {
	ArcNode *p;
	visit[v] = 1;
	visit(v);
	p= G->adjlist[v].firstarc;	//指向边
	while(p!= NULL) {
		if(visit[p->adjvex] == 0)	//顶点未被访问
				DFS(G, p->adjvex);
			p=p->nextarc;	//指向顶点v的下一条边的终点 
	}
}

//图的广度优先搜索遍历，BFS
void BFS(AGraph *G, int v, int visit[maxSize]) {
	ArcNode *p;
	int que[maxSize];
	int front=0, rear=0;
	int j;
	visit(v);
	visit[v]=1;
	rear = (rear + 1) % maxSize;
	que[rear] = v;
	while(front != rear) {
		front = (front + 1) % maxSize;
		j = que[front];
		p = G->adjlist[j].firstarc;	//指向出队顶点j的第一条边
		while(p != NULL){
			while(visit[p->adjvex] == 0) {	//当前邻接点未被访问
				visit(p->adjvex);
				visit[p->adjvex] = 1;
				rear = (rear + 1) % maxSize;
				que[rear] = p->adjvex;
			}
			p = p->nextarc;		//指向j的下一条边
		}		
	}
}


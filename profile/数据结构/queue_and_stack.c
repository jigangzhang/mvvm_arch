#include <stdio.h>

#define N	50

#define MAXSIZE N

typedef struct {
	int data[MAXSIZE];
	int top;
} SqStack;

typedef struct {
	int data;
	struct LNode *next;
}LNode;

typedef struct {
	int data[MAXSIZE];
	int front;
	int rear;
}SqQueue;

typedef struct QNode {
	int data;
	struct QNode *next;
}QNode;

typedef struct {
	QNode *front;
	QNode *rear;
}LiQueue;

//顺序栈，关键在于top指针，top对应的数组的下标
int push(SqStack *stack, int x) {
	if(stack->top  >=MAXSIZE -1)
		return 0;
	stack->top++;
	stack->data[stack->top] = x;
	return 1;
}

int pop(SqStack *stack, int *p) {
	if(stack->top < 0) {
		return 0;
	}
	*p = stack->data[stack->top];
	stack->top--;
	return 1;
}

//链 栈，采用头插法，链表头部指针就是栈顶
int push(LNode *q, int x) {
	LNode *s = (LNode *)malloc(sizeof(LNode));
	s->data = x;
	s->next = q;
	q = s;
	return 1;
}

int pop(LNode *q, int *x) {
	if(q == NULL)
		return 0;
	LNode *p = q;
	x = q->data;
	q = q->next;
	free(p);
	return 1;
}

int isQueueEmpty(SqQueue * sq) {
	if(sq->front == sq->rear && sq->data[sq->front] == NULL)
		return 1;
	return 0;
}

int enQueue(SqQueue *sq, int x) {
	if(sq->front == (sq->rear+1) % MAXSIZE)
		return 0;
	sq->rear = (sq->rear+1) % MAXSIZE;
	sq->data[sq->rear] = x;
	return 1;
}

//此处有问题，链表是否是有头结点？ 一个空结点是否占有一个位置？
int deQueue(SqQueue *sq, int *x) {
	if(isQueueEmpty(sq))
		return 0;
		*x = sq->data[sq->front];
	sq->front = (sq->front+1)%MAXSIZE;
}

//链队，	初始时，队首尾都为空； 入队时，要判断队空的情况
void initQueue(LiQueue *lq) {
	lq = (LiQueue *) malloc(sizeof(LiQueue));
	lq->front = lq->rear = NULL;
}

int isQueueEmpty(LiQueue *lq) {
	if(lq->front == NULL || lq->rear == NULL) {
		return 1;
	}
	return 0;
}

void enQueue(LiQueue *lq, int x) {
	QNode *q = (QNode *)malloc(sizeof(QNode));
	q->data = x;
	q->next = NULL;
	if(lq->front == NULL)
		lq->front = lq->rear = q;
	else {
		lq->rear->next = q;
		lq->rear = q;
	}
}

int deQueue(LiQueue *lq, int *x) {
	if(lq->front == NULL || lq->rear == NULL)
		return 0;
	QNode *s = lq->front;
	if(lq->front == lq->rear) 
		lq->front = lq->rear = NULL;
	else{
		lq->front = s->next;
	}
	*x = s->data;
	free(s);
	return 1;
}


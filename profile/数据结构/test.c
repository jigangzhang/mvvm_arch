#include <stdio.h>
#include <string.h>

void reverse(int nums[], int l, int r) {
	int i, j;
	for (i=l, j=r; i<j; i++, j--) {
		int temp = nums[i];
			nums[i] = nums[j];
		nums[j] = temp;
	}
}

void RCR(int nums[], int n,  int p) {
	if(p <=0 || p>= n) 
		return;
	reverse(nums, 0, p-1);
	reverse(nums, p, n -1);
	reverse(nums, 0, n - 1);
}

typedef struct LNode {
	int data;
	struct LNode *next;
} LNode;

LNode *merge(LNode *a, LNode *b) {
	LNode *p = a;
	LNode *q = b;
	LNode *c;
	if(p->data < q->data){
		c = p;
		p = p->next;
	}else{
		c = q;
		q = q->next;
	}
	c->next = NULL;
	LNode *cp;
	cp = c;
	while(p!=NULL && q !=NULL) {
		if(p->data < q->data) {
			cp->next = p;
			p = p->next;
			cp = cp->next;
			} else {
			cp->next = q;
			q = q->next;
			cp = cp->next;
		}
	}
	if(p != NULL)
		cp->next = p;
	if(q != NULL)
		cp->next = q;
	return c;
}

LNode *createLNode(int nums[], int m) {
	LNode *p;
	LNode *n;
	n = (LNode *)malloc(sizeof(LNode));
	n->data = nums[0];
	p = n;
	int i;
	for (i=1; i<m; i++) {
	LNode *t = (LNode *)malloc(sizeof(LNode));
		t->data = nums[i];
		n->next = t;
		n = n->next; 
	}
	n->next = NULL;
	return p;
}

LNode *reverseLNode(LNode *p) {
	LNode *head;
	LNode *tmp;
	head = p;
	p = p->next;
	head->next = NULL;
	while(p->next != NULL) {
		tmp = p->next;
		p->next = head;
		head = p;
		p = tmp;
	}
	p->next = head;
	return p;
}

typedef struct BTNode {
	int data;
	struct BTnode *lchild;
	struct BTnode *rchild;
}BTNode;


void main() {
	int nums[] = {1,2,3,4,5,6,7,8,9,10};
	LNode *p = createLNode(nums, 10);
	LNode *tmp = p;
	while (p !=NULL) {
		printf("node: %d", p->data);
		p = p->next;
	}
	printf("\n");
	LNode *q = reverseLNode(tmp);
	while(q != NULL) {
		printf("reverse: %d", q->data);
		q = q->next;
	}
	printf("\n");
	int i;	
	for(i=0; i<sizeof(nums)/sizeof(int); i++)
		printf("%d", nums[i]);
	printf("\n");
}

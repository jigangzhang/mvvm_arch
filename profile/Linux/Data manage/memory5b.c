#include <unistd.h>
#include <stdlib.h>
#include <stdio.h>

int main()
{
	char z  = *(const char *)0;	//null pointer
	printf("I read from location zero %c\n", z);
	//sprintf(some_memory, "A write to null\n");
	exit(EXIT_SUCCESS);
}

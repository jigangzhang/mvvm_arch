#include <unistd.h>
#include <stdlib.h>
#include <stdio.h>

#define A_MEGABYTE (1024 * 1024)	//1MB=1024KB=1024 * 1024Byte=1024 * 1024 * 8bit

int main()
{
	char *some_memory;
	int megabyte = A_MEGABYTE;
	int exit_code = EXIT_FAILURE;
	
	some_memory = (char *)malloc(megabyte);
	if (some_memory != NULL) {
		sprintf(some_memory, "Hello world!\n");
		printf("data: %s, size: %d", some_memory, sizeof(some_memory));
		exit_code = EXIT_SUCCESS;
	}
	exit(exit_code);
}

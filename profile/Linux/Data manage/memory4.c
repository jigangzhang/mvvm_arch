#include <stdlib.h>
#include <stdio.h>

#define ONE_K (1024)

int main()
{
	char *some_memory;
	char *scan_ptr;

	some_memory = (char *)malloc(ONE_K);
	if (some_memory == NULL)
		exit(EXIT_FAILURE);

	scan_ptr = some_memory;
	while(1) {
		//*scan_ptr = 'a';
		printf("scan--%s\n", scan_ptr);
		//scan_ptr++;
		*scan_ptr = '\0';
		scan_ptr++;
	}
	exit(EXIT_SUCCESS);
}

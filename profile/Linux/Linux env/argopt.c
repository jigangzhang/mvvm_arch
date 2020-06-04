#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>

int main(int argc, char *argv[])
{
	int arg;
	for(arg = 0; arg < argc; arg++) {
		if(argv[arg][0] == '-')
			printf("option: %s\n", argv[arg]+1);
		else
			printf("argument %d: %s\n", arg, argv[arg]);
	}
	printf("\n*******************\n");

	int opt;
	while((opt = getopt(argc, argv, ":if:lr")) != -1) {
		switch(opt) {
			case 'i':
			case 'l':
			case 'r':
				printf("option: %c\n", opt);
				break;
			case 'f':
				printf("filename: %s\n", optarg);
				break;
			case ':':
				printf("option needs a value\n");
				break;
			case '?':
				printf("unknown option: %c\n", optopt);
				break;
		}
	}
	for(; optind < argc; optind++)
		printf("argument: %s\n", argv[optind]);
	exit(0);
}

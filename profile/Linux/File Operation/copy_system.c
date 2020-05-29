#include <unistd.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <stdlib.h>

//Read 1 byte, and write 1 byte into file out
//TIMEFORMAT="" time ./exec, execute this command, the run time will be out

void copy_block();
void copy_std();

int main()
{
	copy_std();
	return 0;
	copy_block();
	return 0;

	char c;
	int in, out;
	in = open("sys_read.c", O_RDONLY);
	out = open("file.out", O_WRONLY|O_CREAT, S_IRUSR|S_IWUSR);
	while(read(in, &c, 1) == 1)
		write(out, &c, 1);
	
	exit(0);
}

//Improve for above, the system call take more time
void copy_block()
{
	char block[1024];
	int in, out;
	int nread;

	in = open("sys_read.c", O_RDONLY);
	out = open("file1.out", O_WRONLY|O_CREAT, S_IRUSR|S_IWUSR);
	while(nread = read(in, block, sizeof(block)) > 0)
		write(out, block, nread);

	exit(0);
}

//copy with standard libs
//fgets, read line
//fread, read buf
#include <stdio.h>
#include <stdlib.h>
void copy_std()
{
	int c;
	FILE *in, *out;
	in = fopen("sys_read.c", "r");
	out = fopen("file2.out", "w");

	while((c = fgetc(in)) != EOF)
		fputc(c, out);

	exit(0);
}

#include <unistd.h>
#include <stdlib.h>

//write something into standard out or standard error
int main ()
{
	if((write(1, "Here is some data!\n", 19)) != 18)
		write(2, "A write error has occurred on file descriptor 1\n", 46);

	exit(0);
}

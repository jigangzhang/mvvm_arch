#include <unistd.h>
#include <stdio.h>
#include <stdlib.h>

//an argument list
char *const ps_argv[] = {"ps", "ax", 0};

//environment, not terribly useful
char *const ps_envp[] = {"PATH=/bin:/usr/bin", "TERM=console", 0};

int main()
{
	printf("Running ps with execlp\n");
	execl("/bin/ps", "ps", "ax", 0);	//assumes ps is in /bin
	execlp("ps", "ps", "ax", 0);		//assumes /bin is in PATH
	execle("/bin/ps", "ps", "ax", 0, ps_envp);//passes own environment

	execv("/bin/ps", ps_argv);
	execvp("ps", ps_argv);
	execve("/bin/ps", ps_argv, ps_envp);
	printf("Done.\n");
	exit(0);
}

#include <syslog.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

int main()
{
	int logmask;
	openlog("logmask", LOG_PID|LOG_CONS, LOG_USER);
	syslog(LOG_INFO, "informative message, pid = %d", getpid());
	syslog(LOG_DEBUG, "debug message, should appear");
	logmask = setlogmask(LOG_UPTO(LOG_NOTICE));
	syslog(LOG_DEBUG, "debug message, should not appear");
	
	FILE *f;
	f = fopen("not_here", "r");
	if(!f)
		syslog(LOG_ERR|LOG_USER, "oops - %m\n");
	exit(0);
}

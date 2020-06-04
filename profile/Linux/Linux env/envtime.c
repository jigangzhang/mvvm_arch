#define _XOPEN_SOURCE

#include <time.h>
#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>

void printTime()
{
	int i;
	time_t the_time;
	for(i=1; i<=10; i++) {
		the_time = time((time_t *)0);
		printf("The time is %ld\n", the_time);
		sleep(2);
	}
}

int GMTime()
{
	struct tm *tm_ptr;
	time_t the_time;
	
	(void) time(&the_time);
	tm_ptr = gmtime(&the_time);

	printf("Raw time is %ld\n", the_time);
	printf("gmtime gives:\n");
	printf("date: %02d/%02d/%02d\n",
			tm_ptr->tm_year, tm_ptr->tm_mon+1, tm_ptr->tm_mday);
	printf("time: %02d:%02d:%02d\n",
			tm_ptr->tm_hour, tm_ptr->tm_min, tm_ptr->tm_sec);
	exit(0);
}

void printLocalTime()
{
	struct tm *tm_ptr;
	time_t the_time;
	
	(void) time(&the_time);
	printf("The date is: %s\n", ctime(&the_time));
	tm_ptr = localtime(&the_time);

	printf("Raw time is %ld\n", the_time);
	printf("gmtime gives:\n");
	printf("date: %02d/%02d/%02d\n",
			tm_ptr->tm_year, tm_ptr->tm_mon+1, tm_ptr->tm_mday);
	printf("time: %02d:%02d:%02d\n",
			tm_ptr->tm_hour, tm_ptr->tm_min, tm_ptr->tm_sec);
}

int main()
{
	struct tm *tm_ptr, timestruct;
	time_t the_time;
	char buf[256];
	char *result;

	(void)time(&the_time);
	tm_ptr = localtime(&the_time);
	strftime(buf, 256, "%A %d %B, %I:%M %p", tm_ptr);

	printf("strftime gives: %s\n", buf);

	strcpy(buf, "Thu 26 July 2020, 18:00 will do fine");

	printf("calling strptime with: %s\n", buf);

	tm_ptr = &timestruct;
	result = strptime(buf, "%a %d %b %Y, %R", tm_ptr);
	printf("strptime consumed up to: %s\n", result);

	printf("strptime gives:\n");
	printf("date: %02d/%02d/%02d\n",
			tm_ptr->tm_year%100, tm_ptr->tm_mon+1, tm_ptr->tm_mday);
	printf("time: %02d:%02d\n",
			tm_ptr->tm_hour, tm_ptr->tm_min);

	exit(0);
}

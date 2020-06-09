#include <stdio.h>
#include <term.h>
#include <curses.h>
#include <stdlib.h>

int badterm()
{
	setupterm("unlisted", fileno(stdout), (int *)0);
	printf("Done.\n");
	exit(0);
}

int main()
{
	int nrows, ncolumns;
	setupterm(NULL, fileno(stdout), (int *)0);
	nrows = tigetnum("lines");
	ncolums = tigetnum("cols");
	printf("This terminal has %d columns and %d rows.\n", ncolumns, nrows);
	exit(0);
}

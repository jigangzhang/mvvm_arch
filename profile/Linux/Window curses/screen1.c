#include <unistd.h>
#include <stdlib.h>
#include <curses.h>

int main()
{
	initscr();
	move(5, 15);
	printw("%s", "Hello World!");
	refresh();

	sleep(2);
	endwin();

	//clearok(stdscr, 1);          //make window disable
	//refresh();
	exit(EXIT_SUCCESS);
}

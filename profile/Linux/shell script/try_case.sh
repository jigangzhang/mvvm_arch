#!/bin/sh

echo -n "Is it morning? Please answer yes or no: "
read timeofday

case "$timeofday" in
	yes) echo "Good Morning";;
	no) echo "Good Afternoon";;
	*) echo "Sorry, answer not recognized"
		exit 1;;
esac

echo -n "Is it morning? Please answer yes or no: "
read timeofday

case "$timeofday" in
	yes | y | Yes | YES) echo "Good Morning";;
	n* | N*) echo "Good Afternoon";;
	*) echo "Sorry, answer not recognized"
		exit 1;;
esac

echo -n "Is it morning? Please answer yes or no: "
read timeofday

case "$timeofday" in
	[yY] | [yY][eE][sS])
		echo "Good Morning"
		echo "Up bright and early this morning"
		;;
	[nN]*)
		echo "Good Afternoon"
		;;
	*)
		echo "Sorry, answer not recognized"
		echo "Please answer yes or no"
		exit 1
		;;
esac

exit 0

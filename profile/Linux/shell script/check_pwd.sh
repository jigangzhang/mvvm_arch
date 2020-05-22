#!/bin/sh

echo -n "Please enter password: "
read pwd

while [ "$pwd" != "secret" ];do
	printf "sorry, try again."
	read pwd
done
exit 0

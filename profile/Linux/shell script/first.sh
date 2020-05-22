#!/bin/sh

#first
#this is my first shell script
#look through all the files in the current directory
#for the string bill,and then prints the names of those files to the standard output.

for file in *
do
	if grep -q bill $file
	then
		echo $file
	fi
done

exit 0
#exit ensure return a useful result code, 0 is success

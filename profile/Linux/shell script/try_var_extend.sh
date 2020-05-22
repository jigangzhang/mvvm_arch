#!/bin/sh

unset foo
echo ${foo:-bar}

foo=fud
echo ${foo:-bar}

foo=$(pwd)/fred.c
echo $foo
echo ${foo#*/}
echo ${foo##*/}

bar=$(pwd)/lib.h
echo $bar
echo ${bar%home*}
echo ${bar%%home*}

exit 0

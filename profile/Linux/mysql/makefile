all: app

app: app_mysql.c app_test.c app_myql.h
	gcc -o app -I/usr/include/mysql app_mysql.c app_test.c -lmysqlclient -L/usr/lib64/mysql


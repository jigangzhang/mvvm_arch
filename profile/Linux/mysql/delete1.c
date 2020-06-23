#include <stdlib.h>
#include <stdio.h>

#include "mysql.h"

int main(int argc, char *argv[]) {
	MYSQL my_connection;
	int res;

	mysql_init(&my_connection);

	if(mysql_real_connect(&my_connection, "localhost", "root", "19931031zjg", "db_cd", 0, NULL, 0)) {
		printf("Connection success\n");

	//	res = mysql_query(&my_connection, "insert into children(name, age) values('Ann', 5)");
		res = mysql_query(&my_connection, "delete from children where age > 10");
	//	res = mysql_query(&my_connection, "update children set age = 14 where name = 'Ann'");
		if (!res) {
			printf("Deleted %lu rows\n", (unsigned long)mysql_affected_rows(&my_connection));
		} else {
			fprintf(stderr, "Delete error %d: %s\n", mysql_errno(&my_connection), mysql_error(&my_connection));
		}

		mysql_close(&my_connection);
	} else {
		fprintf(stderr, "Connection failed\n");
		if (mysql_errno(&my_connection)) {
			fprintf(stderr, "Connection error %d: %s\n", mysql_errno(&my_connection), mysql_error(&my_connection));
		}
	}
	
	return EXIT_SUCCESS;
}

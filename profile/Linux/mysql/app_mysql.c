#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#include "mysql.h"
#include "app_mysql.h"

static MYSQL my_connection;
static int dbconnected = 0;

static int get_artist_id(char *artist);

int database_start(char *name, char *pwd) {
	if (dbconnected) return 1;
	mysql_init(&my_connection);
	if(!mysql_real_connect(&my_connection, "localhost", name, pwd, "blpcd", 0, NULL, 0)) {
		fprintf(stderr, "Database connection failure: %d, %s\n", mysql_errno(&my_connection), mysql_error(&my_connection));
		return 0;
	}
	dbconnected = 1;
	return 1;
}

void database_end() {
	if(dbconnected) mysql_close(&my_connection);
	dbconnected = 0;
}

int add_cd(char *artist, char *title, char *catalogue, int *cd_id) {
	MYSQL_RES *res_ptr;
	MYSQL_ROW *mysqlrow;

	int res;
	char is[250];
	char es[250];
	int artist_id = -1;
	int new_cd_id = -1;

	if(!dbconnected) return 0;
	artist_id = get_artist_id(artist);
	mysql_escape_string(es, title, strlen(title));
	sprintf(is, "insert into cd(title, artist_id, catalogue) values('%s', %d, '%s')", es, artist_id, catalogue);
	res = mysql_query(&my_connection, is);
	if (res) {
		fprintf(stderr, "Insert error %d: %s\n", mysql_errno(&my_connection), mysql_error(&my_connection));
		return 0;
	}

	res = mysql_query(&my_connection, "select LAST_INSERT_ID()");
	if(res) {
		printf("SELECT error: %s\n", mysql_error(&my_connection));
		return 0;
	} else {
		res_ptr = mysql_use_result(&my_connection);
		if(res_ptr) {
			if ((mysqlrow = mysql_fetch_row(res_ptr))) {
				sscanf(mysqlrow[0], "%d", &new_cd_id);
			}
			mysql_free_result(res_ptr);
		}
		*cd_id = new_cd_id;
		if(new_cd_id != -1)
			return 1;
		return 0;
	}
}

static int get_artist_id(char *artist) {
	MYSQL_RES *res_ptr;
	MYSQL_ROW mysqlrow;

	int res;
	char qs[250];
	char is[250];
	char es[250];
	int artist_id = -1;

	mysql_escape_string(es, artist, strlen(artist));
	sprintf(qs, "select id from artist where name = '%s'", es);

	res = mysql_query(&my_connection, qs);
	if(res) {
		fprintf(stderr, "SELECT error: %s\n", mysql_error(&my_connection));
	} else {
		res_ptr = mysql_store_result(&my_connection);
		if(res_ptr) {
			if(mysql_num_rows(res_ptr) > 0)
				if(mysqlrow = mysql_fetch_row(res_ptr))
					sscanf(mysqlrow[0], "%d", &artist_id);
			mysql_free_result(res_ptr);
		}
	}
	if(artist_id != -1)
		return artist_id;

	sprintf(is, "insert into artist(name) values('%s')", es);
	res = mysql_query(&my_connection, is);
	if(res) {
		fprintf(stderr, "Insert error %d: %s\n", mysql_errno(&my_connection), mysql_error(&my_connection));
		return 0;
	}
	res = mysql_query(&my_connection, "select LAST_INSERT_ID()");
	if(res) {
		printf("SELECT error: %s\n", mysql_error(&my_connection));
		return 0;
	} else {
		res_ptr = mysql_use_result(&my_connection);
		if(res_ptr) {
			if((mysqlrow = mysql_fetch_row(res_ptr)))
				sscanf(mysqlrow[0], "%d", &artist_id);
			mysql_free_result(res_ptr);
		}
	}
	return artist_id;
}

int add_tracks(struct current_tracks_st *tracks) {
	int res;
	char is[250];
	char es[250];
	int i;
	if(!dbconnected) return 0;

	i = 0;
	while (tracks->track[i][0]) {
		mysql_escape_string(es, tracks->track[i], strlen(tracks->track[i]));
		sprintf(is, "insert into track(cd_id, track_id, title) values(%d, %d, '%s')", tracks->cd_id, i+1, es);
		res = mysql_query(&my_connection, is);
		if(res) {
			fprintf(stderr, "Insert error: %d: %s\n", mysql_errno(&my_connection), mysql_error(&my_connection));
			return 0;
		}
		i++;
	}
	return 1;
}

int get_cd(int cd_id, struct current_cd_st *dest) {
	MYSQL_RES *res_ptr;
	MYSQL_ROW *mysqlrow;
	int res;
	char qs[250];
	if(!dbconnected) return 0;
	memset(dest, 0, sizeof(*dest));
	dest->artist_id = -1;

	sprintf(qs, "select artist.id, cd.id, artist.name, cd.title, cd.catalogue from artist, cd where artist.id = cd.artist_id and cd.id = %d", cd_id);

	res = mysql_query(&my_connection, qs);
	if(res) {
		fprintf(stderr, "SELECT error: %s\n", mysql_error(&my_connection));
	} else {
		res_ptr = mysql_store_result(&my_connection);
		if(res_ptr) {
			if(mysql_num_rows(res_ptr) > 0) {
				if(mysqlrow = mysql_fetch_row(res_ptr)) {
					sscanf(mysqlrow[0], "%d", &dest->artist_id);
					sscanf(mysqlrow[1], "%d", &dest->cd_id);
					strcpy(dest->artist_name, mysqlrow[2]);
					strcpy(dest->title, mysqlrow[3]);
					strcpy(dest->catalogue, mysqlrow[4]);
				}
			}
			mysql_free_result(res_ptr);
		}
	}
	if(dest->artist_id != -1)
		return 1;
	return 0;
}

int get_cd_tracks(int cd_id, struct current_tracks_st *dest) {
	MYSQL_RES *res_ptr;
	MYSQL_ROW mysqlrow;

	int res;
	char qs[250];
	int i = 0, num_tracks = 0;
	if(!dbconnected) return 0;

	memset(dest, 0, sizeof(*dest));
	dest->cd_id = -1;
	sprintf(qs, "select track_id, title from track where track.cd_id = %d order by track_id", cd_id);

	res = mysql_query(&my_connection, qs);
	if(res) {
		fprintf(stderr, "Select error: %s\n", mysql_error(&my_connection));
	} else {
		res_ptr = mysql_store_result(&my_connection);
		if(res_ptr) {
			if((num_tracks = mysql_num_rows(res_ptr)) > 0) {
				while(mysqlrow = mysql_fetch_row(res_ptr)) {
					strcpy(dest->track[i], mysqlrow[1]);
					i++;
				}
				dest->cd_id = cd_id;
			}
			mysql_free_result(res_ptr);
		}
	}
	return num_tracks;
}

int find_cds(char *search_str, struct cd_search_st *dest) {
	MYSQL_RES *res_ptr;
	MYSQL_ROW mysqlrow;
	int res;
	char qs[500];
	int i = 0;
	char ss[250];
	int num_rows = 0;
	if(!dbconnected) return 0;

	memset(dest, 0, sizeof(*dest));
	mysql_escape_string(ss, search_str, strlen(search_str));

	sprintf(qs, "select distinct artist.id, cd.id from artist, cd where artist.id = cd.artist_id and (artist.name like '%%%s%%' or cd.title like '%%%s%%' or cd.catalogue like '%%%s%%')", ss, ss, ss);
	res = mysql_query(&my_connection, qs);
	if(res) {
		fprintf(stderr, "SELECT error: %s\n", mysql_error(&my_connection));
	} else {
		res_ptr = mysql_store_result(&my_connection);
		if(res_ptr) {
			num_rows = mysql_num_rows(res_ptr);
			if(num_rows > 0) {
				while((mysqlrow = mysql_fetch_row(res_ptr)) && i < MAX_CD_RESULT) {
					sscanf(mysqlrow[1], "%d", &dest->cd_id[i]);
					i++;
				}
			}
			mysql_free_result(res_ptr);
		}
	}
	return num_rows;
}

int delete_cd(int cd_id) {
	int res;
	char qs[250];
	int artist_id, num_rows;
	MYSQL_RES *res_ptr;
	MYSQL_ROW mysqlrow;
	if(!dbconnected) return 0;

	artist_id = -1;
	sprintf(qs, "select artist_id from cd where artist_id = (select artist_id from cd where id = '%d')", cd_id);
	res = mysql_query(&my_connection, qs);
	if(res) {
		fprintf(stderr, "SELECT error: %s\n", mysql_error(&my_connection));
	} else {
		res_ptr = mysql_store_result(&my_connection);
		if(res_ptr) {
			num_rows = mysql_num_rows(res_ptr);
			if(num_rows == 1) {
				mysqlrow = mysql_fetch_row(res_ptr);
				sscanf(mysqlrow[0], "%d", &artist_id);
			}
			mysql_free_result(res_ptr);
		}
	}
	sprintf(qs, "delete from track where cd_id = '%d'", cd_id);
	res = mysql_query(&my_connection, qs);
	if(res) {
		fprintf(stderr, "Delete error (track) %d: %s\n", mysql_errno(&my_connection), mysql_error(&my_connection));
		return 0;
	}

	sprintf(qs, "delete from cd where id = '%d'", cd_id);
	res = mysql_query(&my_connection, qs);
	if(res) {
		fprintf(stderr, "Delete error (cd) %d: %s\n", mysql_errno(&my_connection), mysql_error(&my_connection));
		return 0;
	}

	if(artist_id != -1) {
		sprintf(qs, "delete from artist where id = '%d'", artist_id);
		res = mysql_query(&my_connection, qs);
		if(res) {
			fprintf(stderr, "Delete error (artist) %d: %s\n", mysql_errno(&my_connection), mysql_error(&my_connection));
		}
	}
	return 1;
}

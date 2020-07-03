all:	server	client

CC=cc
CFLAGS= -pedantic -Wall

#DFLAGS= -DDEBUG_TRACE=1 -g

DBM_INC_PATH=/usr/include/gdbm
DBM_LIB_PATH=/usr/lib
DBM_LIB_FILE=-lgdbm
#DBM_LIB_FILE=-lgdbm_compat -lgdbm

.c.o:
	$(CC) $(CFLAGS) -I$(DBM_INC_PATH) $(DFLAGS) -c $<

app_ui.o: app_ui.c cd_data.h
cd_dbm.o: cd_dbm.c cd_data.h
clientif.o: clientif.c cd_data.h cliserv.h
pipe_imp.o: pipe_imp.c cd_data.h cliserv.h
server.o: server.c cd_data.h cliserv.h

client:	app_ui.o clientif.o  pipe_imp.o
	$(CC) -o client $(DFLAGS) app_ui.o clientif.o pipe_imp.o

server:	server.o cd_dbm.o pipe_imp.o
	$(CC) -o server -L$(DBM_LIB_PATH) $(DFLAGS) server.o cd_dbm.o pipe_imp.o -l$(DBM_LIB_FILE)

clean:
	rm -f server client *.o *~

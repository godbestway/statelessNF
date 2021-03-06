#include "STLESS.h"
#include "conn.h"

#include "protoObject.h"

#include <netinet/in.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <json/json.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <pcap.h>
#include <stdint.h>
#include <pthread.h>

#define CONTENT_LEN 32
#define HEAD_LENGTH 4
pthread_mutex_t connac_lock_conn;
static void conn_writeRawVarint32(int conn, int value);

int send_conn_syn_message(int conn){


        //printf("try to build a syn Message");
	ConnSyn syn;
        conn_syn__init(&syn);

	int pid = getpid();
        char host[1024];
        memset(host, 0, 1024);
        gethostname(host, 1024);
        //printf("hostname %s\n", host);
        //printf("pid %d\n",pid);

	syn.has_pid = 1;
    	syn.pid=pid;
    	syn.host = (char*)malloc(CONTENT_LEN);
    	syn.host=host;

        MyConnMessage mes;
        my_conn_message__init(&mes);
   
        mes.data_type =  MY_CONN_MESSAGE__DATA_TYPE__SynType;
        mes.message_case = MY_CONN_MESSAGE__MESSAGE_CONNSYN;
        mes.connsyn = &syn;
 
        int len = my_conn_message__get_packed_size(&mes);
        //printf("size of MyMessage : %u\n", len);
        void *buf = malloc(len);
        my_conn_message__pack(&mes, buf);

    	int result = send_conn_proto_object(conn,buf, len );
	if(result < 0){
		printf("send failed");
		return -1;
	}

	free(buf);
	
	return result;
	

}



int send_conn_proto_object(int conn, uint8_t* buf, int len ){
	
	pthread_mutex_lock(&connac_lock_conn);
	if(buf == NULL){
		return -1;
	}
	conn_writeRawVarint32(conn, len);
        

	int result = conn_write(conn, buf, len);
	if(result < 0){
		return -1;
	}
	pthread_mutex_unlock(&connac_lock_conn);
 	
	return result;
}


static void conn_writeRawVarint32(int conn, int value) {
        char out;
	int result;
	
        while((value & -128) != 0) {
            out = (value & 127) |128;
	    uint8_t out_while = (uint8_t)out;
	    
            result = conn_write(conn, &out_while, 1);
	    if(result < 0){
		printf("write wrong1\n");
		}
            value = value >>7;
           
        }
        uint8_t out_last = (uint8_t)value;
	result = conn_write(conn, &out_last, 1);
	if(result < 0){
		printf("write wrong2\n");
	}
}
	

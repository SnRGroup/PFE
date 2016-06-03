#ifndef TYPES_H_
#define TYPES_H_

typedef struct model_bloc {
	char y[4];
	char cb;
	char cr;
} bloc_t;


typedef struct model_circ_buffer {
	void *buffer;
	void *buffer_end;
	size_t capacity;
	size_t sz;
	size_t count;
	void *head;
	void *tail;
} cbuf_t;

typedef struct model_shared {
	int zoiX;
	int zoiY;
	int imgCount;
	int socket;
	char *ip;
	int port;
} shared_t;

#endif

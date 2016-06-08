#ifndef TYPES_H_
#define TYPES_H_
#include <stdint.h>

typedef struct model_bloc {
	uint8_t y[4];
	uint8_t cb;
	uint8_t cr;
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

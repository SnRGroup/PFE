#ifndef TYPES_H_
#define TYPES_H_
#include <stdint.h>
#include <stdbool.h>

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

typedef struct node_zoi {
  int frame;
  int x;
  int y;
  struct node_zoi *next;
} zoiInfo;

typedef struct model_shared {
	int zoiX;
	int zoiY;
	int zoiYwanted;
	int zoiXwanted;
	int imgCount;
	int socket;
	char *ip;
	int port;
	bool sdlReady;
	bool processing;
	zoiInfo *zoiList;
} shared_t;


#endif

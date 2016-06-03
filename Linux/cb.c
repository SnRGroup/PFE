void cb_init(cbuf_t *cb, size_t capacity, size_t sz) {
	cb->buffer = malloc(capacity * sz);
	cb->buffer_end = (char*)cb->buffer + capacity * sz;
	cb->capacity = capacity;
	cb->count = 0;
	cb->sz = sz;
	cb->head = cb->buffer;
	cb->tail = cb->buffer;
}

void cb_push(cbuf_t *cb, const void *item) {
	pthread_mutex_lock(&mutexCb);
	if (cb->count == cb->capacity) {
		printf("ERROR CAPACITY\n");
	} else {
		memcpy(cb->head, item, cb->sz);
		cb->head = (char*)cb->head + cb->sz;
		if (cb->head == cb->buffer_end) {
			cb->head = cb->buffer;
		}
		cb->count++;
		//printf("Adding, count = %d\n",cb->count);
	}
	pthread_mutex_unlock(&mutexCb);
}

void cb_pop(cbuf_t *cb, void *item) {
	pthread_mutex_lock(&mutexCb);
	if (cb->count == 0) {
		printf("ERROR EMPTY\n");
	} else {
		memcpy(item, cb->tail, cb->sz);
		cb->tail = (char*)cb->tail + cb->sz;
		if (cb->tail == cb->buffer_end) {
			cb->tail = cb->buffer;
		}
		cb->count--;
	}
	pthread_mutex_unlock(&mutexCb);
}

int cb_empty(cbuf_t *cb) {
	pthread_mutex_lock(&mutexCb);
	int cnt = cb->count;
	pthread_mutex_unlock(&mutexCb);
	if (cnt == 0) {
		return 1;
	} else {
		return 0;
	}
}


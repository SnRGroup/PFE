#define _BSD_SOURCE
#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <math.h>
#include <string.h>
#include <fcntl.h>
#include <pthread.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <sys/time.h>
#include <semaphore.h>

#include "consts.h"
#include "types.h"
#include "video.h"

#define TCP_PORT 1234
#define UDP_PORT 1235

static int zoiStartBloc;


int pipeToF[2];
int pipeFromF[2];

int imgSize = WIDTH*HEIGHT*3/2;
int offsetY = WIDTH*HEIGHT;

unsigned char buffer[WIDTH*HEIGHT*3/2];
unsigned char buffer2[WIDTH*HEIGHT*3/2/2];
int n_read = 0 ;
int n = 0;

unsigned char globPkt[300];

pthread_mutex_t mutex;
pthread_mutex_t mutexCb;
pthread_cond_t cond;



void *task_video(void *data) {

	shared_t *shared = (shared_t*)data;

	printf("Thread A\n");
	while (1) {

		int zoiX = shared->zoiX;
		int zoiY = shared->zoiY;

		n_read = 0;
		while (n_read < imgSize) {
			while (( n = read(0, &buffer[n_read], imgSize - n_read)) > 0) {
				n_read += n;
			}
		}


		//pthread_mutex_lock(&mutexRaw);
		//pthread_cond_wait(&condRaw, &mutexRaw);

		bloc_t *bloc = malloc(sizeof(bloc_t));

		/*
			 for (int i = 0; i<230400; i++) {	
			 readBloc(buffer, bloc, i);
			 char tmp[4];
			 tmp[0] = bloc->y[3];
			 tmp[1] = bloc->y[2];
			 tmp[2] = bloc->y[1];
			 tmp[3] = bloc->y[0];
			 memcpy(&(bloc->y), &tmp, 4);
			 writeBloc(buffer2, bloc, 230400-1-i);
			 }
			 */

		//zoiStartBloc++;

		//zoiX++;
		//zoiY++;

		bloc_t *blocs = malloc(WIDTH/2*HEIGHT/2*sizeof(bloc_t));
		bloc_t *blocsDown;

		// ZOI	
		readBlocs(buffer, blocs, zoiX, zoiY, ZOIW, ZOIH);
		writeBlocs(buffer2, WIDTH/2, blocs, 0, HEIGHT-ZOIH, ZOIW, ZOIH);

		// LEFT SIDE
		readBlocs(buffer, blocs, 0, 0, zoiX, HEIGHT);
		blocsDown = downSampleBlocs(blocs, zoiX, HEIGHT, 2);	
		writeBlocs(buffer2, WIDTH/2, blocsDown, 0, 0, zoiX/2, HEIGHT/2);
		free(blocsDown);

		// RIGHT SIDE
		readBlocs(buffer, blocs, zoiX+ZOIW, 0, WIDTH-(zoiX+ZOIW), HEIGHT);
		blocsDown = downSampleBlocs(blocs, WIDTH-(zoiX+ZOIW), HEIGHT, 2);
		writeBlocs(buffer2, WIDTH/2, blocsDown, zoiX/2, 0, (WIDTH-(zoiX+ZOIW))/2, HEIGHT/2);
		free(blocsDown);

		// TOP BLOCK
		readBlocs(buffer, blocs, zoiX, 0, ZOIW, zoiY);
		blocsDown = downSampleBlocs(blocs, ZOIW, zoiY, 2);
		writeBlocs(buffer2, WIDTH/2, blocsDown, WIDTH/4, 0, ZOIW/2, zoiY/2);
		free(blocsDown);

		// BOTTOM BLOCK
		readBlocs(buffer, blocs, zoiX, zoiY + ZOIH, ZOIW, HEIGHT-(zoiY+ZOIH));
		blocsDown = downSampleBlocs(blocs, ZOIW, HEIGHT-(zoiY+ZOIH), 2);
		writeBlocs(buffer2, WIDTH/2, blocsDown, WIDTH/4, zoiY/2, ZOIW/2, (HEIGHT-(zoiY+ZOIH))/2);
		free(blocsDown);

		//writeBlocs(buffer2, blocs, 0, 360, ZOIW, ZOIH);
		//writeBlocs(buffer2, blocs, 640, 0, ZOIW, ZOIH);

		//writeBlocs(buffer2, nb, 640, 360, ZOIW/2, ZOIH/2);

		//write(1, buffer2, imgSize/2);



		printf("IMG %d\n", ++shared->imgCount);
		write(pipeToF[1], buffer2, imgSize/2);
		//printf("IMG2\n");
		//fwrite(buffer2, 1, imgSize/2, ffmpeg);

		free(blocs);
		free(bloc);


	}


}

void *task_network1(void *data) {
	shared_t *shared = (shared_t*)data;



	while(1) {
		char buf[101];
		int len;
		len = read(shared->socket, buf, 100);
		if (len == 0) {
			exit(0);
		}
		buf[len] = 0;
		printf("%s\n",buf);
		int a;
		int b;
		sscanf(buf,"%d,%d\n",&a, &b);
		printf("a=%d\n",a);
		printf("b=%d\n",b);
		shared->zoiX=a;
		shared->zoiY=b;
		sprintf(buf, "POS;%d;%d;%d\n", shared->imgCount, shared->zoiX, shared->zoiY);
		write(shared->socket, buf, strlen(buf)); 
	}

}

void *task_network2(void *data) {
	shared_t *shared = (shared_t*)data;

	while(1) {
		char* str = "Test\n";
		write(shared->socket,str,strlen(str));
		sleep(1);
	}
}

/*
void *task_b(void *data) {

	shared_t *shared = (shared_t*)data;
	printf("Thread B\n");
	//return;
	//
	//

	//FILE *file = fopen("test.ts","w");

	//unsigned char pkt[300];


	while (1) {
		int n = 0;
		int n_tot = 0;

		pkt_t pkt;

		while (n_tot < 188) {
			while((n = read(pipeFromF[0], &(pkt.data[n_tot]), 188 - n_tot)) > 0) {
				n_tot+=n;
			}
			//printf("W\n");
		}

		//printf("W\n");
		//pthread_mutex_lock(&mutex);
		//

		
		cb_push(&shared->pktList, &pkt);
		sem_post(&(shared->semaphore));
		//printf("Cnt=%d\n",shared->pktList.count);
		//sleep(0.1);

		//memcpy(globPkt, pkt, 188);

		//fwrite(pkt, 1, 188, file);

		//n_tot = read(pipeFromF[0], pkt, 188);
		//write(1, pkt, n);
		//printf("Sending %d\n",n_tot);
		//printf("S\n");

		//pthread_cond_signal(&cond);

		//pthread_mutex_unlock(&mutex);
	}
}
*/

/*
void *task_ca(void *data) {

	shared_t *shared = (shared_t*)data;
	printf("TASK C\n");

	int clientSocket, portNum, nBytes;
	struct sockaddr_in serverAddr;
	socklen_t addr_size;
	clientSocket = socket(PF_INET, SOCK_DGRAM, 0);
	int tmp=0;
	//int aa = setsockopt(clientSocket, SOL_SOCKET, SO_SNDBUF, &tmp, sizeof(tmp));
	//printf("SOCK=%d\n",aa);
	
int 	fl = fcntl(clientSocket, F_GETFL);
	fcntl(clientSocket, F_SETFL, fl | O_NONBLOCK);

	serverAddr.sin_family = AF_INET;
	serverAddr.sin_port = htons(1235);
	serverAddr.sin_addr.s_addr = inet_addr("192.168.1.124");
	memset(serverAddr.sin_zero, '\0', sizeof serverAddr.sin_zero);  

	addr_size = sizeof serverAddr;


	FILE *file = fopen("test.ts","w");

	while(1) {
		//pthread_mutex_lock(&mutex);
		//pthread_cond_wait(&cond, &mutex);

		pkt_t pkt;
		//printf("count=%d\n",shared->pktList.count);
		
		int ret = sem_wait(&(shared->semaphore));

		if (ret != 0) {
			printf("sem error\n");
		}

		//if (!cb_empty(&shared->pktList)) {
			//printf("Poping\n");
			cb_pop(&shared->pktList, &pkt);


			//printf("Sending\n");
			sendto(clientSocket,pkt.data,188,0,(struct sockaddr *)&serverAddr,addr_size);
			fwrite(pkt.data, 1, 188, file);

		//} else {
			//printf("Nothing\n");
			//sleep(1);
		//}
		//pthread_mutex_unlock(&mutex);

	}


}
*/

/*
void *task_d(void *p_data) {
	printf("Thread D\n");
}
*/

int main() {

	shared_t *shared = malloc(sizeof(shared_t));
	shared->imgCount=0;

	//sem_init(&(shared->semaphore), 0, 0);

	//cb_init(&shared->pktList, 10000, sizeof(pkt_t));

	int servSocket;
	struct sockaddr_in serv_addr, client_addr;
	int client_addr_length;
	client_addr_length = sizeof(client_addr);

	servSocket = socket(AF_INET, SOCK_STREAM, 0);
	int reuse=1;
	setsockopt(servSocket, SOL_SOCKET, SO_REUSEADDR, &reuse, sizeof(reuse));

	bzero((char*) &serv_addr, sizeof(serv_addr));
	serv_addr.sin_family=AF_INET;
	serv_addr.sin_addr.s_addr = INADDR_ANY;
	serv_addr.sin_port = htons(TCP_PORT);
	if (bind(servSocket, (struct sockaddr *)&serv_addr, sizeof(serv_addr)) < 0) {
		printf("Error on bind\n");
		exit(0);
	}
	listen(servSocket, 5);

	printf("Waiting for connection...\n");

	shared->socket = accept(servSocket, (struct sockaddr*)&client_addr, &client_addr_length);
	printf("Connect!\n");

	shared->ip = inet_ntoa(client_addr.sin_addr);
	shared->port = ntohs(client_addr.sin_port);

	printf("%s:%d\n",shared->ip,shared->port);

	write(shared->socket, "Hello\n", 7);	

	//zoiStartBloc = blocPos(480,270);
	pid_t pid = NULL;
	pipe(pipeToF);
	pipe(pipeFromF);
	pid = fork();

	if (pid == 0) {


		close(pipeToF[1]);
		close(pipeFromF[0]);

		//printf("Fils\n");
		dup2(pipeToF[0], STDIN_FILENO);
		dup2(pipeFromF[1], STDOUT_FILENO);
		//dup2(pipeFromF[1], STDERR_FILENO);

		//printf("Launching FFmpeg...\n");
		//
		//

		char destination[100];
		//shared->ip="192.168.1.189";
		sprintf(destination,"udp://%s:%d?pkt_size=188",shared->ip,UDP_PORT);

		execl("/usr/bin/ffmpeg", "ffmpeg", "-y", "-f", "rawvideo", "-s", "640x720", "-r", "10", "-pix_fmt", "yuv420p", "-i", "-", "-vcodec", "libx264", "-b:v", "1000k", 
				"-bsf:v", "h264_mp4toannexb",
				"-pix_fmt", "yuv420p", "-preset", "ultrafast", "-bufsize", "0", "-g", "10", "-tune", "zerolatency", "-fflags", "nobuffer", "-bufsize", "0", "-f", "mpegts",
				//"roffi.mp4");	
				destination, NULL);
			//"-", NULL);

		printf("End of child\n");

		exit(1);
	}

	close(pipeToF[0]);
	close(pipeFromF[1]);

	printf("Ready.\n");

	//sleep(1);

	pthread_t thVideo;
	pthread_t thNetwork1;
	pthread_t thNetwork2;
	pthread_t tca;
	pthread_t td;


	pthread_create(&thVideo, NULL, task_video, shared);
	pthread_create(&thNetwork1, NULL, task_network1, shared);
	pthread_create(&thNetwork2, NULL, task_network2, shared);
	//pthread_create(&tca, NULL, task_ca, shared);
	//pthread_create(&td, NULL, task_d, shared);

	unsigned char tmpBuf[imgSize];
	/*while (1) {
		printf("Got whole buffer\n");
		pthread_mutex_lock(&mutexRaw);
		printf("Copying...\n");
		memcpy(buffer, tmpBuf, imgSize);
		printf("Done.\n");
		pthread_cond_signal(&condRaw);
		pthread_mutex_unlock(&mutexRaw);
		}*/



	pthread_join(thVideo, NULL);
	pthread_join(thNetwork1, NULL);
	pthread_join(thNetwork2, NULL);
//	pthread_join(tca, NULL);

}






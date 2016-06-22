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

int pipeToF[2];
int pipeFromF[2];

int imgSize = WIDTH*HEIGHT*3/2;
int offsetY = WIDTH*HEIGHT;

unsigned char buffer[WIDTH*HEIGHT*3/2];
unsigned char buffer2[WIDTH*HEIGHT*3/2/2];

void *task_video(void *data) {

	shared_t *shared = (shared_t*)data;
	printf("Thread A\n");

	int n=0;
	int n_read=0;

	FILE *out = fopen("generated.ts","w");

	while (1) {

		int zoiX = shared->zoiX;
		int zoiY = shared->zoiY;

		n_read = 0;
		while (n_read < imgSize) {
			while (( n = read(0, &buffer[n_read], imgSize - n_read)) > 0) {
				n_read += n;
			}
		}


		bloc_t *bloc = malloc(sizeof(bloc_t));

		bloc_t *blocs = malloc(WIDTH/2*HEIGHT/2*sizeof(bloc_t));
		bloc_t *blocsDown;

		// ZOI	
		readBlocs(buffer, WIDTH, blocs, zoiX, zoiY, ZOIW, ZOIH);
		writeBlocs(buffer2, WIDTH/2, blocs, 0, HEIGHT-ZOIH, ZOIW, ZOIH);

		// LEFT SIDE
		readBlocs(buffer, WIDTH, blocs, 0, 0, zoiX, HEIGHT);
		blocsDown = downSampleBlocs(blocs, zoiX, HEIGHT, 2);	
		writeBlocs(buffer2, WIDTH/2, blocsDown, 0, 0, zoiX/2, HEIGHT/2);
		free(blocsDown);

		// RIGHT SIDE
		readBlocs(buffer, WIDTH, blocs, zoiX+ZOIW, 0, WIDTH-(zoiX+ZOIW), HEIGHT);
		blocsDown = downSampleBlocs(blocs, WIDTH-(zoiX+ZOIW), HEIGHT, 2);
		writeBlocs(buffer2, WIDTH/2, blocsDown, zoiX/2, 0, (WIDTH-(zoiX+ZOIW))/2, HEIGHT/2);
		free(blocsDown);

		// TOP BLOCK
		readBlocs(buffer, WIDTH, blocs, zoiX, 0, ZOIW, zoiY);
		blocsDown = downSampleBlocs(blocs, ZOIW, zoiY, 2);
		writeBlocs(buffer2, WIDTH/2, blocsDown, WIDTH/4, 0, ZOIW/2, zoiY/2);
		free(blocsDown);

		// BOTTOM BLOCK
		readBlocs(buffer, WIDTH, blocs, zoiX, zoiY + ZOIH, ZOIW, HEIGHT-(zoiY+ZOIH));
		blocsDown = downSampleBlocs(blocs, ZOIW, HEIGHT-(zoiY+ZOIH), 2);
		writeBlocs(buffer2, WIDTH/2, blocsDown, WIDTH/4, zoiY/2, ZOIW/2, (HEIGHT-(zoiY+ZOIH))/2);
		free(blocsDown);


		printf("FRAME=%d\n", ++shared->imgCount);
		write(pipeToF[1], buffer2, imgSize/2);
		//fwrite(buffer2, imgSize/2, 1, out);

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
		int a;
		int b;
		sscanf(buf,"%d,%d\n",&a, &b);
		shared->zoiX=a;
		shared->zoiY=b;
		sprintf(buf, "POS;%d;%d;%d\n", shared->imgCount+1, shared->zoiX, shared->zoiY);
		write(shared->socket, buf, strlen(buf));
		printf("Next ZOI position : %d,%d\n",a,b);
	}

}

void *task_network2(void *data) {
	shared_t *shared = (shared_t*)data;

	while(1) {
		char* str = "Test\n";
		//write(shared->socket,str,strlen(str));
		sleep(1);
	}
}


int main(int argc, char *argv[]) {

	shared_t *shared = malloc(sizeof(shared_t));
	shared->imgCount=0;

	if (argc >= 3) {
		shared->zoiX = atoi(argv[1]);
		shared->zoiY = atoi(argv[2]);
	}

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
	printf("Connected!\n");

	shared->ip = inet_ntoa(client_addr.sin_addr);
	shared->port = ntohs(client_addr.sin_port);

	printf("Client = %s:%d\n",shared->ip,shared->port);

	write(shared->socket, "Hello\n", 7);	

	pid_t pid;
	pipe(pipeToF);
	pipe(pipeFromF);
	pid = fork();

	if (pid == 0) {


		close(pipeToF[1]);
		close(pipeFromF[0]);

		dup2(pipeToF[0], STDIN_FILENO);
		dup2(pipeFromF[1], STDOUT_FILENO);
		//dup2(pipeFromF[1], STDERR_FILENO);

		char destination[100];

		sprintf(destination,"udp://%s:%d?pkt_size=188",shared->ip,UDP_PORT);

		execl("/usr/bin/ffmpeg", "ffmpeg", "-y", "-v", "0", "-f", "rawvideo", "-s", "640x720", "-r", "10", "-pix_fmt", "yuv420p", "-i", "-", "-vcodec", "libx264", "-b:v", "2000k", 
				"-bsf:v", "h264_mp4toannexb",
				"-pix_fmt", "yuv420p", "-preset", "ultrafast", "-bufsize", "0", "-g", "10", "-tune", "zerolatency", "-fflags", "nobuffer", "-bufsize", "0", "-f", "mpegts",
				//"roffi.mp4");	
			destination, NULL);
		//"-", NULL);

		exit(1);
	}

	close(pipeToF[0]);
	close(pipeFromF[1]);

	pthread_t thVideo;
	pthread_t thNetwork1;
	pthread_t thNetwork2;
	pthread_t tca;
	pthread_t td;


	pthread_create(&thVideo, NULL, task_video, shared);
	pthread_create(&thNetwork1, NULL, task_network1, shared);
	pthread_create(&thNetwork2, NULL, task_network2, shared);


	pthread_join(thVideo, NULL);
	pthread_join(thNetwork1, NULL);
	pthread_join(thNetwork2, NULL);

}






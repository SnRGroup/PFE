#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h> 
#include <pthread.h>
#include "SDL/SDL.h"
#include <fcntl.h>
#include <sys/stat.h>

#include "types.h"
#include "consts.h"
#include "video.h"

const int SCREEN_WIDTH = WIDTH;
const int SCREEN_HEIGHT = HEIGHT;

const int SCREEN_BPP;

#define BUFSIZE 1024

typedef struct yuv_pic
{
	char * pv_data [4];
	int v_linesize [4]; // number of bytes per line
} S_picture;



int pipeToF[2];
int pipeFromF[2];

int imgSize = WIDTH_2*HEIGHT_2*3/2;
int offsetY = WIDTH*HEIGHT;
unsigned char buffer[WIDTH_2*HEIGHT_2*3/2];
#define BUFFER2_SIZE WIDTH*HEIGHT*3/2
unsigned char buffer2[BUFFER2_SIZE];

pthread_mutex_t mutex;

pthread_mutex_t mutexCb;
pthread_cond_t cond;

void addFutureZoi(shared_t *shared, zoiInfo *zoi) {
	pthread_mutex_lock(&mutex);
	if (shared->zoiList == NULL) {
		shared->zoiList=zoi;
	} else {
		zoiInfo* ptr = shared->zoiList;
		while (ptr->next != NULL) {
			ptr=ptr->next;
		}
		ptr->next=zoi;
	}
	pthread_mutex_unlock(&mutex);
}

void updateZoi(shared_t *shared, int frame) {
	pthread_mutex_lock(&mutex);
	while (shared->zoiList != NULL) {
		if (shared->zoiList->frame <= frame) {
			shared->zoiX = shared->zoiList->x;
			shared->zoiY = shared->zoiList->y;

			zoiInfo *toDelete = shared->zoiList;
			shared->zoiList = shared->zoiList->next;
			free(toDelete);
			printf("ZOI updated !\n");
		} else {
			pthread_mutex_unlock(&mutex);
			break;
		}
	}
	pthread_mutex_unlock(&mutex);
}

void *task_video(void *data) {

	shared_t *shared = (shared_t*)data;

	SDL_Surface * ps_screen;
	SDL_Overlay * ps_bmp;
	SDL_Rect s_rect;

	char * pv_frame = NULL;
	int v_pic_size = 0, v_frame_size = 0;
	S_picture s_yuv_pic;

	if (SDL_Init (SDL_INIT_EVERYTHING) == -1)
	{
		fprintf (stderr, "Could not initialize SDL -% s \n", SDL_GetError ());
		return NULL;
	}

	ps_screen = SDL_SetVideoMode (SCREEN_WIDTH, SCREEN_HEIGHT, 0, SDL_SWSURFACE);
	if (! ps_screen)
	{
		fprintf (stderr, "SDL: could not set video mode-exiting \n");
		return NULL;
	}

	shared->sdlReady = true;
   
	SDL_WM_SetCaption("SNR Player", NULL);

	ps_bmp = SDL_CreateYUVOverlay (SCREEN_WIDTH, SCREEN_HEIGHT, SDL_IYUV_OVERLAY,
			ps_screen);

	v_frame_size = (SCREEN_WIDTH * SCREEN_HEIGHT);
	v_pic_size = ((v_frame_size * 3) / 2);
	if ((pv_frame = (char *) calloc (v_pic_size, 1)) == NULL)
	{
		fprintf (stderr, "SYS: could not allocate mem space \n");
		return NULL;
	}

	s_yuv_pic.pv_data [0] = buffer2;
	s_yuv_pic.pv_data [2] = (char *) (buffer2 + v_frame_size);
	s_yuv_pic.pv_data [1] = (char *) (buffer2 + v_frame_size + v_frame_size / 4);
	s_yuv_pic.v_linesize [0] = SCREEN_WIDTH;
	s_yuv_pic.v_linesize [1] = (SCREEN_WIDTH / 2);
	s_yuv_pic.v_linesize [2] = (SCREEN_WIDTH / 2);

	ps_bmp-> pixels [0] = (unsigned char *) (s_yuv_pic.pv_data [0]);
	ps_bmp-> pixels [2] = (unsigned char *) (s_yuv_pic.pv_data [1]);
	ps_bmp-> pixels [1] = (unsigned char *) (s_yuv_pic.pv_data [2]);

	ps_bmp-> pitches [0] = s_yuv_pic.v_linesize [0];
	ps_bmp-> pitches [2] = s_yuv_pic.v_linesize [1];
	ps_bmp-> pitches [1] = s_yuv_pic.v_linesize [2];

	s_rect.x = 0;
	s_rect.y = 0;
	s_rect.w = SCREEN_WIDTH + 8;
	s_rect.h = SCREEN_HEIGHT + 8;

	int i = 0;

	while (1) {
		int n_read = 0, n;
		while (n_read < imgSize) {
			while (( n = read(0, &buffer[n_read], imgSize - n_read)) > 0) {
				n_read += n;
			}
		}

		i++;


		if (! shared->processing) {
			// clear buffer
			memset(buffer2, 0, sizeof(unsigned char) * BUFFER2_SIZE);

			// process
			bloc_t *bloc = malloc(sizeof(bloc_t));
			bloc_t *blocs = malloc(WIDTH*HEIGHT*sizeof(bloc_t));
			bloc_t *blocsUp = NULL;

			readBlocs(buffer, WIDTH/2, blocs, 0, 0, ZOIW, HEIGHT);
			writeBlocs(buffer2, WIDTH, blocs, 0, 0, ZOIW, HEIGHT);
	
			free(blocs);
			free(bloc);
		}
		else {
			// get ZOI
			int zoiX = shared->zoiX;
			int zoiY = shared->zoiY;

			// process
			bloc_t *bloc = malloc(sizeof(bloc_t));
			bloc_t *blocs = malloc(WIDTH*HEIGHT*sizeof(bloc_t));
			bloc_t *blocsUp = NULL;

			// ZOI	
			readBlocs(buffer, WIDTH/2, blocs, 0, ZOIH, ZOIW, ZOIH);
			writeBlocs(buffer2, WIDTH, blocs, zoiX, zoiY, ZOIW, ZOIH);

			// LEFT SIDE
			readBlocs(buffer, WIDTH/2, blocs, 0, 0, zoiX/2, ZOIH);
			blocsUp = upSampleBlocs(blocs, zoiX/2, ZOIH, 2);		
			writeBlocs(buffer2, WIDTH, blocsUp, 0, 0, zoiX, HEIGHT);
			free(blocsUp);

			// RIGHT SIDE
			readBlocs(buffer, WIDTH/2, blocs, zoiX/2, 0, (ZOIW-zoiX)/2, ZOIH);
			blocsUp = upSampleBlocs(blocs, (ZOIW-zoiX)/2, ZOIH, 2);
			writeBlocs(buffer2, WIDTH, blocsUp, zoiX+ZOIW, 0, ZOIW-zoiX, HEIGHT);
			free(blocsUp);

			// TOP BLOCK
			readBlocs(buffer, WIDTH/2, blocs, ZOIW/2, 0, ZOIW/2, zoiY/2);
			blocsUp = upSampleBlocs(blocs, ZOIW/2, zoiY/2, 2);
			writeBlocs(buffer2, WIDTH, blocsUp, zoiX, 0, ZOIW, zoiY);
			free(blocsUp);

			// BOTTOM BLOC
			readBlocs(buffer, WIDTH/2, blocs, ZOIW/2, zoiY/2, ZOIW/2, (ZOIH-zoiY)/2);
			blocsUp = upSampleBlocs(blocs, ZOIW/2, (ZOIH-zoiY)/2, 2);
			writeBlocs(buffer2, WIDTH, blocsUp, zoiX, zoiY+ZOIH, ZOIW, ZOIH-zoiY);
			free(blocsUp);
				
			drawBlackLineY(buffer2, WIDTH, zoiX, zoiY - 2, ZOIH);
			drawBlackLineX(buffer2, WIDTH, zoiX, zoiY - 2, ZOIW);
			drawBlackLineY(buffer2, WIDTH, zoiX + ZOIW, zoiY - 2, ZOIH);
			drawBlackLineX(buffer2, WIDTH, zoiX, zoiY + ZOIH - 2, ZOIW);

			free(blocs);
			free(bloc);
	
		}

		SDL_LockYUVOverlay (ps_bmp);

		SDL_DisplayYUVOverlay (ps_bmp, & s_rect);

		SDL_UnlockYUVOverlay (ps_bmp);

		SDL_Delay (40);	
	}
}

void *task_network_receiver(void *data) {	
	shared_t *shared = (shared_t*)data;

	while(1) {
		char buf[101];
		int len = read(shared->socket, buf, 100);
		if (len == 0) {
			//error("ERROR reading socket TCP");
			printf("Control socket lost! Aborting mission...\n");
			SDL_Quit();
			exit(1);
		}
		buf[len] = 0;
		printf("TCPRECV=%s",buf);
		if (strncmp(buf,"POS;",4) == 0) {
			int frame;
			int newX;
			int newY;
			sscanf(buf,"POS;%d;%d;%d\n", &frame, &newX, &newY);
			printf("Future ZOI position : frame=%d -> %d,%d\n",frame, newX, newY);
			zoiInfo* nZoi = (zoiInfo*)calloc(1,sizeof(zoiInfo));
			nZoi->frame=frame;
			nZoi->x=newX;
			nZoi->y=newY;
			//printf("Adding...\n");
			addFutureZoi(shared, nZoi);
			//printf("Added\n");
		}
	}

}

void *task_network_sender(void *data) {	
	shared_t *shared = (shared_t*)data;

	SDL_Event event; 
	int continuer = 1;

	while(! shared->sdlReady){
		sleep(1);
	}

	int wantedZoiX = 0;
	int wantedZoiY = 0;

	while (continuer) 
	{
		SDL_WaitEvent(&event); 
		switch(event.type)
		{
			case SDL_QUIT:
				continuer = 0;
				printf("Exit\n");
				SDL_Quit();
				exit(0);
				break;
			case SDL_KEYDOWN:
				/* Check the SDLKey values and move change the coords */
				switch( event.key.keysym.sym ){
					case SDLK_LEFT:
						if (wantedZoiX >= 20+2){
							wantedZoiX -= 20;
						}
						break;
					case SDLK_RIGHT:
						if (wantedZoiX <= ZOIW-20){
							wantedZoiX += 20;
						}
						break;
					case SDLK_UP:
						if (wantedZoiY >= 20+2) {
							wantedZoiY -= 20;
						}
						break;
					case SDLK_DOWN:
						if (wantedZoiY <= ZOIH-20) {
							wantedZoiY += 20;
						}
						break;
					case SDLK_p:
						shared->processing = ! shared->processing;
						break;
				}
				char txt[100];
				sprintf(txt, "%d,%d\n", wantedZoiX, wantedZoiY);
				write(shared->socket, txt, strlen(txt));
				char title[100];
				sprintf(title, "SnR Player - ZOI=(%d, %d)", wantedZoiX, wantedZoiY);    
				SDL_WM_SetCaption(title, NULL);
				break;
		}
	}
	/*
		 while(1) {
		 char* str = "Test\n";  // TODO ecrire roi
		 int len = write(shared->socket,str,strlen(str));
		 if (len < 0) {
		 error("ERROR writing socket TCP");
		 }
		 sleep(1);
		 }
		 */
}



void *task_ffmpeg_control(void *data) {
	shared_t *shared = (shared_t*)data;
	FILE* fifo = fopen("ffmpegcontrolfifo", "r");
	char buf[500];
	while(1) {
		fgets(buf, 500, fifo);
		//printf("FFMPEG=%s\n",buf);
		char *ptsPos = strstr(buf,"pts:");
		if (ptsPos != 0) {
			//printf("FRAME!\n");
			char nbrs[11];
			int j=0;
			for (int i=0; i<10; i++) {
				char c = ptsPos[i+4];
				if (c != 32) {
					nbrs[j++] = c;
				} else {
					if (j > 0) {
						break;
					}
				}
			}
			nbrs[j]=0;
			int frame=atoi(nbrs)/9000;
			printf("FRAME=%d\n",frame);
			updateZoi(shared,frame);
		}
	}	
}

/* 
 * error - wrapper for perror
 */
void error(char *msg) {
	perror(msg);
	exit(0);
}

int main(int argc, char **argv) {
	int sockfd, portno, n;
	struct sockaddr_in serveraddr;
	char *addrStr;
	char buf[BUFSIZE];

	shared_t *shared = calloc(1, sizeof(shared_t));
	shared->sdlReady = false;
	shared->zoiList = NULL;

	/* check command line arguments */
	if (argc != 2) {
		fprintf(stderr,"usage: %s <ip>\n", argv[0]);
		exit(0);
	}
	addrStr = argv[1];

	/* socket: create the socket */
	shared->socket = socket(AF_INET, SOCK_STREAM, 0);
	if (sockfd < 0) 
		error("ERROR opening socket");

	/* build the server's Internet address */
	bzero((char *) &serveraddr, sizeof(serveraddr));
	serveraddr.sin_family = AF_INET;
	serveraddr.sin_addr.s_addr = inet_addr(addrStr);
	serveraddr.sin_port = htons(TCP_PORT);

	/* connect: create a connection with the server */
	if (connect(shared->socket, &serveraddr, sizeof(serveraddr)) < 0) 
	        error("ERROR connecting");

	pthread_t thNetworkReceiver;
	pthread_t thNetworkSender;
	pthread_t thVideo;
	pthread_t thFFmpegControl;

	pthread_create(&thVideo, NULL, task_video, shared);
	pthread_create(&thNetworkReceiver, NULL, task_network_receiver, shared);
	pthread_create(&thNetworkSender, NULL, task_network_sender, shared);
	pthread_create(&thFFmpegControl, NULL, task_ffmpeg_control, shared);

	pthread_join(thVideo, NULL);
	pthread_join(thNetworkReceiver, NULL);
	pthread_join(thNetworkSender, NULL);
	pthread_join(thFFmpegControl, NULL);

	return 0;
}

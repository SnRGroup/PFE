#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h> 
#include <pthread.h>
#include "SDL/SDL.h"

#include "types.h"
#include "consts.h"
#include "video.h"

/*
const int SCREEN_WIDTH = WIDTH_2;
const int SCREEN_HEIGHT = HEIGHT_2;
*/

const int SCREEN_WIDTH = WIDTH;
const int SCREEN_HEIGHT = HEIGHT;

const int SCREEN_BPP;

#define BUFSIZE 1024

typedef struct yuv_pic
{
	char * pv_data [4];
	int v_linesize [4]; // number of bytes per line
} S_picture;



static int zoiStartBloc;


int pipeToF[2];
int pipeFromF[2];
/*
int imgSize = WIDTH_2*HEIGHT_2*3/2;
int offsetY = WIDTH_2*HEIGHT_2;
unsigned char buffer[WIDTH_2*HEIGHT_2*3/2];
unsigned char buffer2[WIDTH*HEIGHT*3/2];
*/

int imgSize = WIDTH_2*HEIGHT_2*3/2;
int offsetY = WIDTH*HEIGHT;
unsigned char buffer[WIDTH_2*HEIGHT_2*3/2];
unsigned char buffer2[WIDTH*HEIGHT*3/2];

int n_read = 0 ;
int n = 0;

unsigned char globPkt[300];

pthread_mutex_t mutex;
pthread_mutex_t mutexCb;
pthread_cond_t cond;



void *task_video(void *data) {
	shared_t *shared = (shared_t*)data;
/*
	SDL_Surface * ps_screen;
	SDL_Overlay * ps_bmp;
	SDL_Rect s_rect;

	char * pv_frame = NULL;
	int v_pic_size = 0, v_frame_size = 0;
	S_picture s_yuv_pic;

	if (SDL_Init (SDL_INIT_EVERYTHING) == -1)
	{
		fprintf (stderr, "Could not initialize SDL -% s \n", SDL_GetError ());
		return 1;
	}

	ps_screen = SDL_SetVideoMode (SCREEN_WIDTH, SCREEN_HEIGHT, 0, SDL_SWSURFACE);
	if (! ps_screen)
	{
		fprintf (stderr, "SDL: could not set video mode-exiting \n");
		return 1;
	}

	SDL_WM_SetCaption ("YUV Window", NULL);

	ps_bmp = SDL_CreateYUVOverlay (SCREEN_WIDTH, SCREEN_HEIGHT, SDL_IYUV_OVERLAY,
	ps_screen);

	v_frame_size = (SCREEN_WIDTH * SCREEN_HEIGHT);
	v_pic_size = ((v_frame_size * 3) / 2);
	if ((pv_frame = (char *) calloc (v_pic_size, 1)) == NULL)
	{
		fprintf (stderr, "SYS: could not allocate mem space \n");
		return 1;
	}

	s_yuv_pic.pv_data [0] = buffer;
	s_yuv_pic.pv_data [2] = (char *) (buffer + v_frame_size);
	s_yuv_pic.pv_data [1] = (char *) (buffer + v_frame_size + v_frame_size / 4);
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
	s_rect.w = SCREEN_WIDTH +8;
	s_rect.h = SCREEN_HEIGHT +8;

	int i = 0, ret = 0;

	while (1) {

		int zoiX = shared->zoiX;
		int zoiY = shared->zoiY;

		n_read = 0;
		while (n_read < imgSize) {
			while (( n = read(0, &buffer[n_read], imgSize - n_read)) > 0) {
				n_read += n;
			}
		}

		i++;

		SDL_LockYUVOverlay (ps_bmp);

		SDL_DisplayYUVOverlay (ps_bmp, & s_rect);

		SDL_UnlockYUVOverlay (ps_bmp);

		SDL_Delay (40);
	}
*/

	SDL_Surface * ps_screen;
	SDL_Overlay * ps_bmp;
	SDL_Rect s_rect;

	char * pv_frame = NULL;
	int v_pic_size = 0, v_frame_size = 0;
	S_picture s_yuv_pic;

	if (SDL_Init (SDL_INIT_EVERYTHING) == -1)
	{
		fprintf (stderr, "Could not initialize SDL -% s \n", SDL_GetError ());
		return 1;
	}

	ps_screen = SDL_SetVideoMode (SCREEN_WIDTH, SCREEN_HEIGHT, 0, SDL_SWSURFACE);
	if (! ps_screen)
	{
		fprintf (stderr, "SDL: could not set video mode-exiting \n");
		return 1;
	}

	SDL_WM_SetCaption ("YUV Window", NULL);

	ps_bmp = SDL_CreateYUVOverlay (SCREEN_WIDTH, SCREEN_HEIGHT, SDL_IYUV_OVERLAY,
	ps_screen);

	v_frame_size = (SCREEN_WIDTH * SCREEN_HEIGHT);
	v_pic_size = ((v_frame_size * 3) / 2);
	if ((pv_frame = (char *) calloc (v_pic_size, 1)) == NULL)
	{
		fprintf (stderr, "SYS: could not allocate mem space \n");
		return 1;
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
	s_rect.w = SCREEN_WIDTH +8;
	s_rect.h = SCREEN_HEIGHT +8;

	int i = 0, ret = 0;

	while (1) {

		int zoiX = 200;//shared->zoiX;
		int zoiY = 200;//shared->zoiY;

		n_read = 0;
		while (n_read < imgSize) {
			while (( n = read(0, &buffer[n_read], imgSize - n_read)) > 0) {
				n_read += n;
			}
		}

		i++;

		bloc_t *bloc = malloc(sizeof(bloc_t));
		bloc_t *blocs = malloc(WIDTH*HEIGHT*sizeof(bloc_t));
		bloc_t *blocsUp = NULL;

		// ZOI	
		readBlocs(buffer, WIDTH/2, blocs, 0, HEIGHT-ZOIH, ZOIW, ZOIH);
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

		// BOTTOM BLOCK
		readBlocs(buffer, WIDTH/2, blocs, ZOIW/2, zoiY/2, ZOIW/2, (ZOIH-zoiY)/2);
		blocsUp = upSampleBlocs(blocs, ZOIW/2, (ZOIH-zoiY)/2, 2);
		writeBlocs(buffer2, WIDTH, blocsUp, zoiX, zoiY+ZOIH, ZOIW, ZOIH-zoiY);
		free(blocsUp);


		SDL_LockYUVOverlay (ps_bmp);

		SDL_DisplayYUVOverlay (ps_bmp, & s_rect);

		SDL_UnlockYUVOverlay (ps_bmp);

		SDL_Delay (40);		
printf("a\n");
		free(blocs);
		free(bloc);
printf("b\n");
	}

/*
	int i = 0, ret = 0;
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
		bloc_t *blocsUp;

		// ZOI	
		readBlocs(buffer, blocs, 0, HEIGHT-ZOIH, ZOIW, ZOIH);
		writeBlocs(buffer2, WIDTH, blocs, 0, 0, ZOIW, ZOIH);
		writeBlocs(buffer2, WIDTH, blocs, 0, ZOIH, ZOIW, ZOIH);
		writeBlocs(buffer2, WIDTH, blocs, ZOIW, 0, ZOIW, ZOIH);
		writeBlocs(buffer2, WIDTH, blocs, ZOIW, ZOIH, ZOIW, ZOIH);

		i++;
		printf("IMG %d\n", ++shared->imgCount);
		write(pipeToF[1], buffer2, imgSize);

		free(blocs);
		free(bloc);
	}
*/
}

void *task_network_receiver(void *data) {	
	shared_t *shared = (shared_t*)data;

	while(1) {
		char buf[101];
		int len = read(shared->socket, buf, 100);
		if (len == 0) {
			error("ERROR reading socket TCP");
		}
		buf[len] = 0;
		//printf("%s\n",buf);
/*
		int a;
		int b;
		sscanf(buf,"%d,%d\n",&a, &b);
		printf("a=%d\n",a);
		printf("b=%d\n",b);
		shared->zoiX=a;
		shared->zoiY=b;
		sprintf(buf, "POS;%d;%d;%d\n", shared->imgCount, shared->zoiX, shared->zoiY);
		write(shared->socket, buf, strlen(buf)); 
*/
	}

}

void *task_network_sender(void *data) {	
	shared_t *shared = (shared_t*)data;

	while(1) {
		char* str = "Test\n";  // TODO ecrire roi
		int len = write(shared->socket,str,strlen(str));
		if (len < 0) {
			error("ERROR writing socket TCP");
		}
		sleep(1);
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

    shared_t *shared = calloc(sizeof(shared_t), 0);

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
/*
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

		execl("/usr/bin/ffplay", "ffplay", "-f", "rawvideo", "-pix_fmt", "yuv420p", "-s", "1280x720", "-", NULL);

		printf("End of child\n");

		exit(1);
	}

	close(pipeToF[0]);
	close(pipeFromF[1]);

	printf("Ready.\n");
*/

    pthread_t thNetworkReceiver;
    pthread_t thNetworkSender;
    pthread_t thVideo;

    pthread_create(&thNetworkReceiver, NULL, task_network_receiver, shared);
    pthread_create(&thNetworkSender, NULL, task_network_sender, shared);
    pthread_create(&thVideo, NULL, task_video, shared);

    pthread_join(thNetworkReceiver, NULL);
    pthread_join(thNetworkSender, NULL);
    pthread_join(thVideo, NULL);

    return 0;
}

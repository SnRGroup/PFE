#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <math.h>
#include <string.h>

#define WIDTH 1280
#define HEIGHT 720

static int offsetY;

int position(int x, int y) {
	return (y*WIDTH+x)*3;
}

typedef struct model_bloc {
	char y[4];
	char cb;
  char cr;
} bloc_t;


void readBloc(unsigned char *source, bloc_t *bloc, int blocId) {
	//int posX = (blocId*2)%WIDTH;
	int posY = (blocId*2/WIDTH)*2;
	int posX = blocId*2 - posY*(WIDTH/2);
	int startPos = posY*WIDTH+posX;
	int posCb = offsetY + blocId;
	int posCr = posCb + offsetY/4;
	bloc->y[0] = source[startPos];
	bloc->y[1] = source[startPos+1];
	bloc->y[2] = source[startPos+WIDTH];
	bloc->y[3] = source[startPos+WIDTH+1];
	bloc->cb = source[posCb];
	bloc->cr = source[posCr];
}

void writeBloc(unsigned char *dest, bloc_t *bloc, int blocId) {
	int posX = (blocId*2)%WIDTH;
	int posY = (blocId*2/WIDTH)*2;
	int posCb = offsetY + blocId;
	int posCr = posCb + offsetY/4;
	dest[posY*WIDTH+posX] = bloc->y[0];
	dest[posY*WIDTH+posX+1] = bloc->y[1];
	dest[(posY+1)*WIDTH+posX] = bloc->y[2];
	dest[(posY+1)*WIDTH+posX+1] = bloc->y[3];
	dest[posCb] = bloc->cb;
	dest[posCr] = bloc->cr;
}

short readPixel(unsigned char *buffer, int x, int y) {
	int posEntier = (y*WIDTH+x)*3/2;
	int posRem = ((y*WIDTH+x)*3)%2;

	short value = 0;
	if (posRem == 0) {
		value = buffer[posEntier] << 4;
		value += buffer[posEntier+1] >> 4;
	} else {
		value = (buffer[posEntier] & 0x0f) << 8;
		value += buffer[posEntier+1];
	}

	return value;

}

void writePixel(unsigned char* buffer, int x, int y, short value) {
	int posEntier = (y*WIDTH+x)*3/2;
	int posRem = ((y*WIDTH+x)*3)%2;

	if (posRem == 0) {
		buffer[posEntier] = value >> 4;
		buffer[posEntier+1] |= (value & 0x0f << 4);
	} else {
		buffer[posEntier] |= (value & 0x0f00) >> 8;
		buffer[posEntier+1] = value & 0x00ff;
	}

}	

int main() {

	int imgSize = WIDTH*HEIGHT*3/2;
	offsetY = WIDTH*HEIGHT;
	
	unsigned char buffer[imgSize];
	unsigned char buffer2[imgSize];
	int n_read = 0 ;
	int n = 0;

	while (1) {
		n_read = 0;
		while (n_read < imgSize) {
			while (( n = read(STDIN_FILENO, &buffer[n_read], imgSize - n_read)) > 0) {
				n_read += n;
				//printf("%d\n",n);
			}


		}

		//printf("IMG\n");
		/*
			 for (int i=0; i < 700000*3; i+=3) {
			 buffer[i] = 0;
			 buffer[i+1] = 0;
			 buffer[i+2] = 0;
			 }

*/
		/*
			 for (int x=0; x < WIDTH/8; x++) {
			 for (int y=0; y < HEIGHT/8; y++) {

			 int posX = x*8;
			 int posY = y*8;

			 unsigned long moyR=0;
			 unsigned long moyG=0;
			 unsigned long moyB=0;

			 for (int i=posX; i<posX+8; i++) {
			 for (int j=posY; j<posY+8; j++) {
			 moyR += (unsigned char)buffer[position(i,j)];
			 moyG += (unsigned char)buffer[position(i,j)+1];
			 moyB += (unsigned char)buffer[position(i,j)+2];
			 }
			 }

		//fprintf(stderr,"%d,%d,%d\n",moyR,moyG,moyB);
		moyR /= 8*8;
		moyG /= 8*8;
		moyB /= 8*8;

		//fprintf(stderr,"%d,%d,%d\n",moyR,moyG,moyB);

		for (int i=posX; i<posX+8; i++) {
		for (int j=posY; j<posY+8; j++) {
		buffer[position(i,j)] = moyR;
		buffer[position(i,j)+1] = moyG;
		buffer[position(i,j)+2] = moyB;
		}
		}

		}
		}

*/

		/*
			 for (int x=0; x < WIDTH/16; x++) {

			 for (int y=0; y < HEIGHT/16; y++) {

			 if (x >= 30 && x <= 50 && y >= 15 && y <= 30) {
			 continue;
			 }

			 int posX = x * 16;
			 int posY = y * 16;

			 for (int i=posX; i<posX+16; i++) {
			 for (int j=posY; j<posY+16; j++) {
			 buffer[position(i,j)] = buffer[position(posX,posY)];
			 buffer[position(i,j)+1] = buffer[position(posX,posY)+1];
			 buffer[position(i,j)+2] = buffer[position(posX,posY)+2];
			 }
			 }

			 }
			 }
			 */




		//defish(buffer, buffer2, WIDTH, HEIGHT, 3, 2, 2);

		/*
			
		for (int x=0; x<WIDTH; x++) {
			for (int y=0; y<3; y++) {
				buffer[y*WIDTH+x] = 255;
			}
		}

		*/
		

		/*
		
		int yOffset = WIDTH*HEIGHT;
		for (int x=0; x<WIDTH; x++) {
			for (int y=0; y<HEIGHT; y++) {
				int posCb = yOffset + (y/2*WIDTH+x)/2;
				int posCr = posCb + yOffset/4;
				if (posCr >= WIDTH*HEIGHT*3/2) {
					exit(0);
				}
				buffer[posCb] = 0;
				buffer[posCr] = 0;
			}
		}
		*/
		
		bloc_t *bloc = malloc(sizeof(bloc_t));

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

		for (int i=0; i<100000; i++) {
			//writeBloc(buffer, bloc, i);
		}

		write(1, buffer2, imgSize);
	}


}


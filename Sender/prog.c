#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>

#define WIDTH 1280
#define HEIGHT 720

int position(int x, int y) {
	return (y*WIDTH+x)*3;
}

int main() {

	int imgSize = WIDTH*HEIGHT*3;
	char buffer[imgSize];
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

		write(1, buffer, imgSize);
	}


}

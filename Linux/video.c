#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include "consts.h"
#include "video.h"

int blocPos(int width, int x, int y) {
	int pos = (y/2*width + x)/2;
	return pos;
}

void readBloc(unsigned char *source, bloc_t *bloc, int blocId) {
	int posX = (blocId*2)%WIDTH;
	int posY = (blocId*2/WIDTH)*2;
	//int posX = blocId*2 - posY*(WIDTH/2);
	int startPos = posY*WIDTH+posX;
	int offsetY = WIDTH*HEIGHT;
	int posCb = offsetY + blocId;
	int posCr = posCb + offsetY/4;
	bloc->y[0] = source[startPos];
	bloc->y[1] = source[startPos+1];
	bloc->y[2] = source[startPos+WIDTH];
	bloc->y[3] = source[startPos+WIDTH+1];
	bloc->cb = source[posCb];
	bloc->cr = source[posCr];
}

void writeBloc(unsigned char *dest, int width, bloc_t *bloc, int blocId) {
	int posX = (blocId*2)%width;
	int posY = (blocId*2/width)*2;
	int startPos = posY*width+posX;
	int offsetY = width*HEIGHT;
	int posCb = offsetY + blocId;
	int posCr = posCb + offsetY/4;
	dest[startPos] = bloc->y[0];
	dest[startPos+1] = bloc->y[1];
	dest[startPos+width] = bloc->y[2];
	dest[startPos+width+1] = bloc->y[3];
	dest[posCb] = bloc->cb;
	dest[posCr] = bloc->cr;
}


void readBlocs(unsigned char *source, bloc_t *blocs, int x, int y, int w, int h) {
	int startBloc = blocPos(WIDTH, x, y);
	bloc_t *ptr = blocs;
	for (int j=0; j<h/2; j++) {
		for (int i=0; i<w/2; i++) {
			int blocIdx = startBloc + (j*WIDTH/2)+i;
			readBloc(source, ptr++, blocIdx);
		}
	}	
}

void writeBlocs(unsigned char *dest, int width, bloc_t *blocs, int x, int y, int w, int h) {
	int startBloc = blocPos(width, x, y);
	bloc_t *ptr = blocs;
	for (int j=0; j<h/2; j++) {
		for (int i=0; i<w/2; i++) {
			int blocIdx = startBloc + (j*width/2)+i;
			writeBloc(dest, width, ptr++, blocIdx);
		}
	}
}


char moyLuminance(bloc_t *bloc) {
	int moy=0;
	moy = bloc->y[0] + bloc->y[1] + bloc->y[2] + bloc->y[3];
	moy /= 4;
	return moy;
}

bloc_t* downSampleBlocs(bloc_t *blocs, int w, int h, int divider) {
	bloc_t *blocs2 = malloc(w/2*h/2/(divider*divider)*sizeof(bloc_t));
	bloc_t *ptr = blocs2;
	// i et j : indices des blocs de fin
	for (int j=0; j<(h/2/divider); j++) {
		for (int i=0; i<(w/2/divider); i++) {
			bloc_t b1;
			bloc_t b2;
			bloc_t b3;
			bloc_t b4;
			int startPos = i*divider + (j*(w/2))*divider;
			b1 = blocs[startPos];
			b2 = blocs[startPos + 1];
			b3 = blocs[startPos + w/2];
			b4 = blocs[startPos + w/2 + 1];

			bloc_t bf;

			bf.y[0] = moyLuminance(&b1);
			bf.y[1] = moyLuminance(&b2);
			bf.y[2] = moyLuminance(&b3);
			bf.y[3] = moyLuminance(&b4);

			bf.cb = (b1.cb + b2.cb + b3.cb + b4.cb)/4;
			bf.cr = (b1.cr + b2.cr + b3.cr + b4.cr)/4;


			memcpy(ptr, &bf, sizeof(bloc_t));
			ptr++;

		}
	}
	return blocs2;
}


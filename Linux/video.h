#include "types.h"

#ifndef VIDEO_H_
#define VIDEO_H_

void readBloc(unsigned char *source, int width, bloc_t *bloc, int blocId);
void writeBloc(unsigned char *dest, int width, bloc_t *bloc, int blocId);
void readBlocs(unsigned char *source, int width, bloc_t *blocs, int x, int y, int w, int h);
void writeBlocs(unsigned char *dest, int width, bloc_t *blocs, int x, int y, int w, int h);
char moyLuminance(bloc_t *bloc);
bloc_t* downSampleBlocs(bloc_t *blocs, int w, int h, int divider);
bloc_t* upSampleBlocs(bloc_t *blocs, int w, int h, int multiplier);
void drawBlackLineX(unsigned char *frame, int width, int x, int y, int len);
void drawBlackLineY(unsigned char *frame, int width, int x, int y, int len);

#endif

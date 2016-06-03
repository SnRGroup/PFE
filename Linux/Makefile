all: prog

prog: prog.o video.o
	gcc -lm -lpthread -o prog prog.o video.o

prog.o: prog.c
	gcc -std=c11 -Ofast -o prog.o -c prog.c

video.o: video.c
	gcc -std=c11 -Ofast -o video.o -c video.c

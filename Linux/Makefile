all: prog receiver

prog: prog.o video.o
	gcc -w -o prog prog.o video.o -lm -lpthread

prog.o: prog.c
	gcc -w -std=c11 -Ofast -o prog.o -c prog.c

receiver: receiver.o video.o
	gcc -w -o receiver receiver.o video.o -lm -lpthread -lSDLmain -lSDL

receiver.o: receiver.c
	gcc -w -std=c11 -Ofast -o receiver.o -c receiver.c

video.o: video.c
	gcc -w -std=c11 -Ofast -o video.o -c video.c

clean:
	rm prog receiver *.o

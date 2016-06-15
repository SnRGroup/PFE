all: prog receiver

runProg: prog
	ffmpeg -f v4l2 -s 1280x720 -r 10 -i /dev/video0  -pix_fmt yuv420p -bufsize 0 -f rawvideo - 2>/dev/null | ./prog

prog: prog.o video.o
	gcc -o prog prog.o video.o -lm -lpthread 

prog.o: prog.c
	gcc -std=c11 -Ofast -o prog.o -c prog.c

runReceiver: receiver
	if [[ -p ffmpegcontrolfifo ]]; then rm ffmpegcontrolfifo; fi
	mkfifo ffmpegcontrolfifo
	ffmpeg -nostats -f mpegts -i udp://127.0.0.1:1235 -f rawvideo -vf showinfo -pix_fmt yuv420p - 2>ffmpegcontrolfifo  | ./receiver 127.0.0.1

runRP: receiver	
	ffmpeg -nostats -f mpegts -s 640x720 -r 10 -tune zerolatency -buffer 1024 -i udp://192.168.1.171:1235 -f rawvideo -vf showinfo -pix_fmt yuv420p - 2>ffmpegcontrolfifo | ./receiver 192.168.1.171

receiver: receiver.o video.o
	gcc -o receiver receiver.o video.o -lm -lpthread -lSDLmain -lSDL

receiver.o: receiver.c
	gcc -std=c11 -Ofast -o receiver.o -c receiver.c

video.o: video.c
	gcc -std=c11 -Ofast -o video.o -c video.c

clean:
	rm prog receiver *.o

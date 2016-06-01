#include <stdio.h>
#include <stdlib.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <string.h>
#include <sys/stat.h>
#include<unistd.h>
#include<fcntl.h>
int main() {

	int clientSocket, portNum, nBytes;
	struct sockaddr_in serverAddr;
	socklen_t addr_size;
	clientSocket = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP);
	fcntl(clientSocket, F_SETFL, O_NONBLOCK);
	int tmp=0;
	//int aa = setsockopt(clientSocket, SOL_SOCKET, SO_SNDBUF, &tmp, sizeof(tmp));
	//  //printf("SOCK=%d\n",aa);
	//
	//  int   fl = fcntl(clientSocket, F_GETFL);
//	fcntl(clientSocket, F_SETFL, fl | O_NONBLOCK);

	serverAddr.sin_family = AF_INET;
	serverAddr.sin_port = htons(1235);
	serverAddr.sin_addr.s_addr = inet_addr("192.168.1.124");
	//inet_aton("192.168.1.189", &serverAddr.sin_addr);
	memset(serverAddr.sin_zero, '\0', sizeof serverAddr.sin_zero);

	addr_size = sizeof serverAddr;

	//int fd = open("./fifo", O_RDONLY);

	freopen(NULL, "rb", stdin);

	FILE *file = fopen("test.ts","w");
	while (1) {
		int n = 0;
		int n_tot = 0;

		unsigned char pkt[188];
/*
		while (n_tot < 188) {
			while((n = read(0, &(pkt[n_tot]), 188-n_tot)) > 0) {
				n_tot+=n;
			}
			//printf("W\n");
		}
		*/
		n = fread(pkt, 1, 188, stdin);

		if (n != 188) {
			printf("%d\n",n);
		}

		fwrite(pkt, 1, 188, file);
		sendto(clientSocket,pkt,188,0,(struct sockaddr *)&serverAddr,addr_size);


	}


}

package com.legsim.snr_cardboard;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created by legsim on 27/04/16.
 */
public class VideoWorkerNetwork implements VideoWorker {

    private Thread threadVideo;
    private MediaCodec decoder;
    private LinkedList<PESPacket> PESlist;
    private DatagramSocket clientsocketUDP;

    private Thread threadControl;
    private Socket clientsocketTCP;
    DataOutputStream outToServer;
    BufferedReader inFromServer;

    public HashMap<Integer, int[]> zoiPos;

    private int nextZoiX;
    private int nextZoiY;

    public int currentZoiX;
    public int currentZoiY;

    private MainActivity mAct;

    int firstImage = -1;
    int lastImage = 0;
    double lastPTS = 0;
    int lastCounter = 0;
    boolean stop;

    public VideoWorkerNetwork(MainActivity mAct) {
        this.mAct = mAct;
        try {
            decoder = MediaCodec.createDecoderByType("video/avc");
        } catch (Exception e) {

        }

        nextZoiX = -1;
        nextZoiY = -1;
        zoiPos = new HashMap<>();

        threadVideo = new Thread(new VideoReceiverRunnable());
        threadControl = new Thread(new ControlRunnable());
    }

    public void configure(Surface surface) {
        MediaFormat format = new MediaFormat();
        format.setString(MediaFormat.KEY_MIME, "video/avc");
        //format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 100000);
        format.setInteger(MediaFormat.KEY_WIDTH, 1280);
        format.setInteger(MediaFormat.KEY_HEIGHT, 720);
        format.setInteger("max-width", 1280);
        format.setInteger("max-height", 720);
        format.setInteger("push-blank-buffers-on-shutdown", 1);

        decoder.configure(format, surface, null, 0);
    }

    public void start() {
        threadVideo.start();
        threadControl.start();
    }

    public void finish() {
        this.stop = true;
    }

    @Override
    public int[] getZoi() {
        int[] ret = { currentZoiX, currentZoiY };
        return ret;
    }

    public void updateZoi(int x, int y) {
        nextZoiX = x;
        nextZoiY = y;
    }

    public class VideoReceiverRunnable implements Runnable {

        @Override
        public void run() {

            stop = false;

            decoder.setCallback(new MediaCodec.Callback() {
                @Override
                public void onInputBufferAvailable(MediaCodec codec, int index) {
                    //Log.d("CALLBACK", "INA");
                    ByteBuffer buf = codec.getInputBuffer(index);
                    if (PESlist.isEmpty()) {
                        codec.queueInputBuffer(index, 0, 0, 0, 0);
                    } else {
                        PESPacket nextPES = PESlist.removeFirst();

                        ByteBuffer inputBuffer = codec.getInputBuffer(index);
                        inputBuffer.clear();
                        inputBuffer.put(nextPES.data.toByteArray());

                        //Log.d("PTS", nextPES.getPts() / 90000.0 + "");
                        codec.queueInputBuffer(index, 0, nextPES.data.toByteArray().length, nextPES.getPts() * 1000 / 90, 0);
                        //Log.d("CALLBACK","Feeding");


                    }
                }

                @Override
                public void onOutputBufferAvailable(MediaCodec codec, int index, MediaCodec.BufferInfo info) {

                    ByteBuffer outputBuffer = codec.getOutputBuffer(index);
                    MediaFormat bufferFormat = codec.getOutputFormat(index);

                    //Log.d("Display", info.presentationTimeUs / 1000000.0 + "");

                    int imgCount = (int) (info.presentationTimeUs * 10 / 1000000);

                    if (firstImage == -1) {
                        firstImage = imgCount;
                    }

                    imgCount -= firstImage;

                    Log.d("IMGCOUNT", "" + imgCount);

                    int pos[] = null;
                    for (int i = lastImage+1; i <= imgCount; i++) {
                        Log.d("Checking",i+"");
                        int[] pos2 = zoiPos.get(i);
                        if (pos2 != null) pos=pos2;
                    }

                    if (pos != null) {
                        Log.d("New position",pos[0]+";"+pos[1]);
                        currentZoiX = pos[0];
                        currentZoiY = pos[1];
                    }

                    lastImage = imgCount;


                    codec.releaseOutputBuffer(index, info.presentationTimeUs);

                }

                @Override
                public void onError(MediaCodec codec, MediaCodec.CodecException e) {
                    Log.e("CALLBACK", "ERROR");
                }

                @Override
                public void onOutputFormatChanged(MediaCodec codec, MediaFormat format) {
                    Log.d("CALLBACK", "Format : " + format.toString());

                }
            });


            decoder.start();

            PESlist = new LinkedList<PESPacket>();
            PESPacket currentPES = null;
            boolean first = true;

            try {
                clientsocketUDP = new DatagramSocket(1235);
                byte[] receivedata = new byte[65535];

                while (!stop) {
                    DatagramPacket recv_packet = new DatagramPacket(receivedata, receivedata.length);

                    //Log.d("UDP","R");
                    clientsocketUDP.receive(recv_packet);
                    //Log.d("UDP","T");


                    //text("Received");

                    byte[] data = recv_packet.getData();

                    ByteBuffer raw = ByteBuffer.wrap(data);
                    raw.limit(recv_packet.getLength());

                    TSPacket tsp = new TSPacket(raw);


                    if (tsp.error || tsp.pid != 256) {
                        //Log.d("TSP","Unknown packet");
                        continue;
                    }

                    //Log.d("TSP", tsp.counter+"");

                    int diffCounter = tsp.counter - lastCounter;
                    if (diffCounter == -15) diffCounter = 1;

                    lastCounter = tsp.counter;

                    if (diffCounter != 1) {
                        //Log.d("TSP","DIFF = "+diffCounter);
                        //continue;
                    }


                    if (first && tsp.hasAdapt && tsp.adaptationField.rai) {
                        first = false;
                        Log.d("TSP", "First adaptation");
                    } else if (first && (!tsp.hasAdapt || !tsp.adaptationField.rai)) {
                        //Log.d("TSP", "Not yet");
                        continue;
                    }


                    if (tsp.pus) {

                        if (currentPES != null) {
                            //Log.d("Video", "PES!" + currentPES.data.size());

                            //Log.d("PES", String.format("%02X",currentPES.data.toByteArray()[currentPES.data.size()-1]));

                            //Log.d("PTS", "" + currentPES.getPts());

                            //toSend = new byte[currentPES.data.size()];

                            //Log.d("Add PTS",currentPES.getPts() / 90000.0+"");

                            double diffPTS = currentPES.getPts() / 90000.0 - lastPTS;
                            lastPTS = currentPES.getPts() / 90000.0;

                            //Log.d("PTS",""+diffPTS);

                            PESlist.add(currentPES);


                        }


                        // start of new PES packet
                        currentPES = new PESPacket(tsp);
                    } else if (currentPES != null) {
                        // continued PES

                        currentPES.append(tsp);

                    } else {
                        // haven't got a start pes yet
                        continue;
                    }


                }

                decoder.stop();
                decoder.release();

            } catch (Exception e) {

            }
        }
    }

    public class ControlRunnable implements Runnable {

        @Override
        public void run() {
            try {
                clientsocketTCP = new Socket("192.168.1.171", 1234);
                outToServer = new DataOutputStream((clientsocketTCP.getOutputStream()));
                inFromServer = new BufferedReader(new InputStreamReader(clientsocketTCP.getInputStream()));


                while (!stop) {

                    if (nextZoiX != -1) {
                        outToServer.write((nextZoiX+","+nextZoiY+"\n").getBytes());
                        outToServer.flush();
                        nextZoiX = -1;
                    }

                    if (inFromServer.ready()) {
                        String s = inFromServer.readLine();
                        Log.d("TCP", s);
                        String args[] = s.split(";");
                        String cmd = args[0];
                        if (cmd.equals("POS")) {
                            int imgCount = Integer.parseInt(args[1]);
                            int newX = Integer.parseInt(args[2]);
                            int newY = Integer.parseInt(args[3]);
                            Log.d("POS", imgCount + " - " + newX + " - " + newY);

                            zoiPos.put(imgCount+1, new int[]{newX, newY});

                        }
                    }
                }


                clientsocketTCP.close();

            } catch (IOException e) {

            }
        }
    }


}

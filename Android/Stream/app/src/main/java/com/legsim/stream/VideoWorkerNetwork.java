package com.legsim.stream;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Surface;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.LinkedList;

/**
 * Created by legsim on 27/04/16.
 */
public class VideoWorkerNetwork implements VideoWorker {

    private Thread thread;

    private MediaCodec decoder;

    private LinkedList<PESPacket> PESlist;

    private DatagramSocket clientsocketUDP;

    double lastPTS = 0;

    int lastCounter = 0;

    public VideoWorkerNetwork() {
        try {
            decoder = MediaCodec.createDecoderByType("video/avc");
        } catch (Exception e) {

        }
        thread = new Thread(new ReceiverRunnable());

    }

    public void configure(Surface surface, String filePath) {
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
        thread.start();
    }

    public class ReceiverRunnable implements Runnable {

        @Override
        public void run() {
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


                while (true) {
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


            } catch (Exception e) {

            }
        }
    }


}

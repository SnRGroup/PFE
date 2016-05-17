package com.legsim.stream;

import java.io.ByteArrayOutputStream;

/**
 * Created by legsim on 08/04/16.
 */
public class NAL {

    long timestamp;
    int type;

    ByteArrayOutputStream data;


    public NAL(long timestamp, int idr, byte[] initialPayload) {
        this.timestamp = timestamp;

        data = new ByteArrayOutputStream();


        data.write(0x00);
        data.write(0x00);
        data.write(0x00);
        data.write(0x01);
        data.write((byte)idr);


        data.write(initialPayload, 0, initialPayload.length);
    }

    public void add(byte[] fragment) {
        data.write(fragment, 0, fragment.length);
    }

}

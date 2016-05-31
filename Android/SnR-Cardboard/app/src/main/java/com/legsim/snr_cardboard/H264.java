package com.legsim.snr_cardboard;

import java.nio.ByteBuffer;

/**
 * Created by legsim on 08/04/16.
 */
public class H264 {


    int fragmentType;
    boolean start;
    boolean end;
    int idr;

    byte[] payload;

    public H264(RTPPacket rtp) {

        ByteBuffer raw = ByteBuffer.wrap(rtp.payload);
        byte b = raw.get();
        idr = b & 0xE0;
        fragmentType = b & 0x1F;

        b = raw.get();
        start = (b & 0x80) != 0;
        end = (b & 0x40) != 0;

        idr += (b & 0x1F);

        payload = new byte[raw.remaining()];
        raw.get(payload);


    }

}

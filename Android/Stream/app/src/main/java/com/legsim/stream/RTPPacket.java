package com.legsim.stream;

import java.nio.ByteBuffer;
import java.util.BitSet;

/**
 * Created by legsim on 08/04/16.
 */
public class RTPPacket {

    int version;
    boolean padding;
    boolean ext;
    int cc;
    boolean m;
    int payloadType;
    int seqNum;
    long timestamp;
    long ssrc;
    // CSRC, header ext

    byte[] payload;

    public RTPPacket(ByteBuffer raw) {
        byte b = raw.get();

        version = (b >> 6 & 0x02);

        BitSet flags = BitSet.valueOf(new byte[] {b});
        padding = flags.get(5);
        ext = flags.get(4);

        cc = (b >> 0) & 0x0f;

        b = raw.get();
        m = ((b >> 7) != 0);

        payloadType = b & 0x7f;

        seqNum = raw.getShort();

        timestamp = (long)raw.getInt() & 0xffffffffl;

        ssrc =(long)raw.getInt() & 0xffffffffl;

        for (int i=0; i<cc; i++) {
            raw.getInt();
        }

        payload = new byte[raw.remaining()];
        raw.get(payload);




    }

}

package com.legsim.stream;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.security.InvalidParameterException;
import java.util.BitSet;

/**
 * Created by legsim on 05/04/16.
 */
public class PESPacket {
    private final static String TAG = PESPacket.class.getName();

    int id;
    int length;

    boolean priority;
    boolean dai;
    boolean copyright;
    boolean origOrCopy;
    boolean hasPts;
    boolean hasDts;
    boolean hasEscr;
    boolean hasEsRate;
    boolean dsmtmf;
    boolean acif;
    boolean hasCrc;
    boolean pesef;
    int headerDataLength;


    byte[] headerData;
    ByteArrayOutputStream data = new ByteArrayOutputStream();



    public PESPacket(TSPacket ts) {
        if( ts == null || !ts.pus) {
            Log.e(TAG, "invalid ts passed in");
            throw new InvalidParameterException("invalid ts passed in");
        }

        ByteBuffer pes = ByteBuffer.wrap(ts.payload);

        // start code
        if( pes.get() != 0 || pes.get() != 0 || pes.get() != 1 ) {
            Log.e(TAG, "invalid start code");
            throw new InvalidParameterException("invalid start code");
        }

        // stream id
        id = pes.get() & 0xff;

        // packet length
        length = pes.getShort() & 0xffff;

        // this is supposedly allowed for video
        if( length == 0 ) {
            //Log.w(TAG, "got zero-length PES?");
        }

        if( id != 0xe0 ) {
            Log.e(TAG, String.format("unexpected stream id: 0x%x", id));
            // todo: ?
        }

        // for 0xe0 there is an extension header starting with 2 bits '10'
        byte b = pes.get();
        if( (b & 0x30) != 0 ) {
            Log.w(TAG, "scrambled ?!?!");
            // todo: ?
        }

        BitSet flags = BitSet.valueOf(new byte[]{b});
        priority = flags.get(3);
        dai = flags.get(2);
        copyright = flags.get(1);
        origOrCopy = flags.get(0);

        flags = BitSet.valueOf(new byte[]{pes.get()});
        hasPts = flags.get(7);
        hasDts = flags.get(6);
        hasEscr = flags.get(5);
        hasEsRate = flags.get(4);
        dsmtmf = flags.get(3);
        acif = flags.get(2);
        hasCrc = flags.get(1);
        pesef = flags.get(0);

        headerDataLength = pes.get() & 0xff;

        if( headerDataLength > 0 ) {
            headerData = new byte[headerDataLength];
            pes.get(headerData);
        }

        WritableByteChannel channel = Channels.newChannel(data);
        try {
            channel.write(pes);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // length includes optional pes header,
        length = length - (3 + headerDataLength);
    }

    public void append(TSPacket ts) {
        if( ts.pus ) {
            Log.e(TAG, "don't add start of PES packet to another packet");

        }
        data.write(ts.payload, 0, ts.payload.length);
    }

    public boolean isFull() {
        return (data.size() >= length );
    }

    public long getPts() {
        if( !hasPts || headerDataLength < 5 )
            return 0;

        ByteBuffer hd = ByteBuffer.wrap(headerData);


        long pts = ( ((hd.get() & 0x0e) << 29)
                | ((hd.get() & 0xff) << 22)
                | ((hd.get() & 0xfe) << 14)
                | ((hd.get() & 0xff) << 7)
                | ((hd.get() & 0xfe) >>> 1));

        return pts;
    }
}
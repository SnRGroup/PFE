package com.legsim.stream;

import android.util.Log;

import java.nio.ByteBuffer;
import java.util.BitSet;

/**
 * Created by legsim on 05/04/16.
 */
public class TSPacket {
    private final static String TAG = TSPacket.class.getName();

    class AdaptationField {

        boolean di;
        boolean rai;
        boolean espi;
        boolean hasPcr;
        boolean hasOpcr;
        boolean spf;
        boolean tpdf;
        boolean hasExtension;

        byte[] data;

        public AdaptationField(ByteBuffer raw) {
            // first byte is size of field minus size byte
            int count = raw.get() & 0xff;

            // second byte is flags
            BitSet flags = BitSet.valueOf(new byte[]{ raw.get()});

            di = flags.get(7);
            rai = flags.get(6);
            espi = flags.get(5);
            hasPcr = flags.get(4);
            hasOpcr = flags.get(3);
            spf = flags.get(2);
            tpdf = flags.get(1);
            hasExtension = flags.get(0);

            // the rest is 'data'
            if( count > 1 ) {
                data = new byte[count-1];
                raw.get(data);
            }
        }
    }

    boolean tei;
    boolean pus;
    boolean tp;
    int pid;
    boolean hasAdapt;
    boolean hasPayload;
    int counter;
    AdaptationField adaptationField;
    byte[] payload;
    boolean error;



    public TSPacket(ByteBuffer raw) {
        // check for sync byte
        if( raw.get() != 0x47 ) {
            Log.e(TAG, "missing sync byte");
            error = true;
            return;
        }

        // next 3 bits are flags
        byte b = raw.get();
        BitSet flags = BitSet.valueOf(new byte[] {b});

        tei = flags.get(7);
        pus = flags.get(6);
        tp = flags.get(5);

        // then 13 bits for pid
        pid = ((b << 8) | (raw.get() & 0xff) ) & 0x1fff;

        b = raw.get();
        flags = BitSet.valueOf(new byte[]{b});

        // then 4 more flags
        if( flags.get(7) || flags.get(6) ) {
            Log.e(TAG, "scrambled?!?!");
            // todo: bail on this packet?
        }

        hasAdapt = flags.get(5);
        hasPayload = flags.get(4);

        // counter
        counter = b & 0x0f;

        // optional adaptation field
        if( hasAdapt ) {
            adaptationField = new AdaptationField(raw);
        }

        // optional payload field
        if( hasPayload ) {
            payload = new byte[raw.remaining()];
            raw.get(payload);
        }
    }

}
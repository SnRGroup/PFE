package com.legsim.snr_cardboard;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by raffi on 10/04/2016.
 */
public class VideoWorkerLocal extends Thread implements VideoWorker{

    private static final String TAG = VideoWorkerLocal.class.getSimpleName();

    private MediaExtractor extractor;
    private MediaCodec decoder;
    private Context context;
    private boolean stop;

    public VideoWorkerLocal(Context context){
        super();
        this.context = context;
    }

    public void finish() {
        this.stop = true;
    }

    @Override
    public void updateZoi(int x, int y) {

    }

    public void configure(Surface surface) {
        extractor = new MediaExtractor();
        try {
            AssetFileDescriptor afd = this.context.getResources().openRawResourceFd(R.raw.test2);
            extractor.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();

            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat format = extractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("video/")) {
                    extractor.selectTrack(i);
                    decoder = MediaCodec.createDecoderByType(mime);
                    decoder.configure(format, surface, null, 0);
                    break;
                }
            }

            if (decoder == null) {
                Log.e(TAG, "Can't find video info!");
                return;
            }

            decoder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void run() {
        ByteBuffer[] inputBuffers = decoder.getInputBuffers();
        ByteBuffer[] outputBuffers = decoder.getOutputBuffers();
        BufferInfo info = new BufferInfo();
        boolean isEOS = false;
        long startMs = System.currentTimeMillis();
        this.stop = false;

        while (! this.stop) {
            if (!isEOS) {
                int inIndex = decoder.dequeueInputBuffer(10000);
                if (inIndex >= 0) {
                    ByteBuffer buffer = inputBuffers[inIndex];
                    int sampleSize = extractor.readSampleData(buffer, 0);
                    if (sampleSize < 0) {
                        Log.d(TAG, "InputBuffer BUFFER_FLAG_END_OF_STREAM");
                        decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        isEOS = true;
                    } else {
                        decoder.queueInputBuffer(inIndex, 0, sampleSize, extractor.getSampleTime(), 0);
                        extractor.advance();
                    }
                }
            }

            int outIndex = decoder.dequeueOutputBuffer(info, 10000);
            switch (outIndex) {
                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                    Log.d(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
                    outputBuffers = decoder.getOutputBuffers();
                    break;
                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                    Log.d(TAG, "New format " + decoder.getOutputFormat());
                    break;
                case MediaCodec.INFO_TRY_AGAIN_LATER:
                    Log.d(TAG, "dequeueOutputBuffer timed out!");
                    break;
                default:
                    ByteBuffer buffer = outputBuffers[outIndex];
                    Log.v(TAG, "We can't use this buffer but render it due to the API limit, " + buffer);

                    // We use a very simple clock to keep the video FPS, or the video
                    // playback will be too fast
                    while (info.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            break;
                        }
                    }
                    decoder.releaseOutputBuffer(outIndex, true);
                    break;
            }

            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                Log.d(TAG, "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                break;
            }
        }

        decoder.stop();
        decoder.release();
        extractor.release();
    }
}

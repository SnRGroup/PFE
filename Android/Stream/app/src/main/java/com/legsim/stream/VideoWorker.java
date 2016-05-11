package com.legsim.stream;

import android.view.Surface;

/**
 * Created by raffi on 27/04/2016.
 */
public interface VideoWorker {
    void configure(Surface surface);
    void start();
    void finish();
}

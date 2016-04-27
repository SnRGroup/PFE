package com.legsim.stream;

import android.view.Surface;

/**
 * Created by raffi on 27/04/2016.
 */
public interface VideoWorker {
    public void configure(Surface surface, String filePath);
    public void start();
}

package com.legsim.stream;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.LinkedList;

public class MainActivity extends AppCompatActivity {
    final static String PREFERENCE_FILE_NAME = "preference file name";

    final static String VIDEO_WORKER_MODE = "VideoWorker mode";
    final static int VIDEO_WORKER_MODE_NETWORK = 1;
    final static int VIDEO_WORKER_MODE_LOCAL = 0;

    final static String PROCESSING_MODE= "Processing mode";
    final static int PROCESSING_MODE_WITH = 1;
    final static int PROCESSING_MODE_WITHOUT = 0;

    private VideoSurfaceView mVideoView;
    private ToggleButton btnNetwork;
    private ToggleButton btnProcessing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mVideoView = (VideoSurfaceView) findViewById(R.id.glsurfaceview);

        btnNetwork = (ToggleButton) findViewById(R.id.btnNetwork);
        btnNetwork.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                setWorkerMode(btnNetwork.isChecked() ?
                        VIDEO_WORKER_MODE_NETWORK :
                        VIDEO_WORKER_MODE_LOCAL);
                recreate();
            }
        });

        btnProcessing = (ToggleButton) findViewById(R.id.btnProcessing);
        btnProcessing.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                setProcessingMode(btnProcessing.isChecked() ?
                        PROCESSING_MODE_WITH :
                        PROCESSING_MODE_WITHOUT);
                recreate();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        btnNetwork.setChecked(getWorkerMode(getApplicationContext()) == VIDEO_WORKER_MODE_NETWORK ?
                true :
                false);
        btnProcessing.setChecked(getProcessingMode(getApplicationContext()) == PROCESSING_MODE_WITH ?
                true :
                false);

        mVideoView.onResume();
    }

    private void setWorkerMode(int newMode){
        setSharedPreference(VIDEO_WORKER_MODE, newMode);
    }

    static int getWorkerMode(Context context){
        return getSharedPreference(context, MainActivity.VIDEO_WORKER_MODE, MainActivity.VIDEO_WORKER_MODE_NETWORK);
    }

    private void setProcessingMode(int newMode){
        setSharedPreference(PROCESSING_MODE, newMode);
    }

    static int getProcessingMode(Context context){
        return getSharedPreference(context, MainActivity.PROCESSING_MODE, MainActivity.PROCESSING_MODE_WITHOUT);
    }

    private void setSharedPreference(String key, int value){
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
                PREFERENCE_FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(key, value);
        editor.commit();
    }

    private static int getSharedPreference(Context context, String key, int defValue){
        SharedPreferences sharedPref = context.getSharedPreferences(
                MainActivity.PREFERENCE_FILE_NAME, Context.MODE_PRIVATE);
        return sharedPref.getInt(key, defValue);
    }
}

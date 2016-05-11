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
import android.widget.TextView;
import android.widget.Toast;

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
    final static int VIDEO_WORKER_MODE_NETWORK = 0;
    final static int VIDEO_WORKER_MODE_LOCAL = 1;

    private VideoSurfaceView mVideoView = null;
    private TextView textInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textInfo = (TextView) findViewById(R.id.textInfo);
        mVideoView = (VideoSurfaceView) findViewById(R.id.glsurfaceview);
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

        String actualMode;
        switch(getWorkerMode(getApplicationContext())){
            case VIDEO_WORKER_MODE_LOCAL:
                actualMode = "local";
                break;
            case VIDEO_WORKER_MODE_NETWORK:
                actualMode = "network";
                break;
            default:
                actualMode = "invalid";
        }
        textInfo.setText("actual mode: " + actualMode);

        mVideoView.onResume();
    }

    public void btnPlayLocal(View view){
        Toast.makeText(MainActivity.this, "play local", Toast.LENGTH_SHORT).show();
        setWorkerMode(VIDEO_WORKER_MODE_LOCAL);
        recreate();
    }

    public void btnPlayNetwork(View view){
        Toast.makeText(MainActivity.this, "play network", Toast.LENGTH_SHORT).show();
        setWorkerMode(VIDEO_WORKER_MODE_NETWORK);
        recreate();
    }

    private void setWorkerMode(int newMode){
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
                PREFERENCE_FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(VIDEO_WORKER_MODE, newMode);
        editor.commit();
    }

    static int getWorkerMode(Context context){
        SharedPreferences sharedPref = context.getSharedPreferences(
                MainActivity.PREFERENCE_FILE_NAME, Context.MODE_PRIVATE);
        return sharedPref.getInt(MainActivity.VIDEO_WORKER_MODE, MainActivity.VIDEO_WORKER_MODE_NETWORK);
    }
}

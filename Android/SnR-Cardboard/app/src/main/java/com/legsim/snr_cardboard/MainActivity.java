package com.legsim.snr_cardboard;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.vrtoolkit.cardboard.CardboardActivity;
import com.google.vrtoolkit.cardboard.CardboardView;
import com.google.vrtoolkit.cardboard.Eye;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;

import javax.microedition.khronos.egl.EGLConfig;

public class MainActivity
        extends CardboardActivity
        implements CardboardView.StereoRenderer,
        SurfaceTexture.OnFrameAvailableListener {
    private CardboardView cardboardView;
    private static String TAG = "VideoRender";

    final static String PREFERENCE_FILE_NAME = "preference file name";

    final static String VIDEO_WORKER_MODE = "VideoWorker mode";
    final static int VIDEO_WORKER_MODE_NETWORK = 1;
    final static int VIDEO_WORKER_MODE_LOCAL = 0;

    final static String PROCESSING_MODE= "Processing mode";
    final static int PROCESSING_MODE_WITH = 1;
    final static int PROCESSING_MODE_WITHOUT = 0;

    /*
    private static final int FLOAT_SIZE_BYTES = 4;
    private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
    private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
    private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;
    private final float[] mTriangleVerticesData = new float[]{-2.0f, -2.0f, 0.0f, 0.0f, 0.0f, 2.0f, -2.0f, 0.0f, 1.0f, 0.0f, -2.0f, 2.0f, 0.0f, 0.0f, 1.0f, 2.0f, 2.0f, 0.0f, 1.0f, 1.0f};
    private FloatBuffer mTriangleVertices;
    */
    private FloatBuffer vertexBuffer, textureVerticesBuffer;
    private ShortBuffer drawListBuffer;



    private short drawOrder_WITHOUT_PROCESSING[] = { 0, 1, 2, 0, 2, 3 }; // order to draw vertices

    // number of coordinates per vertex in this array
    private static final int COORDS_PER_VERTEX_WITHOUT_PROCESSING = 2;

    private final int vertexStride_WITHOUT_PROCESSING = COORDS_PER_VERTEX_WITHOUT_PROCESSING * 4; // 4 bytes per vertex

    private float squareCoords_WITHOUT_PROCESSING[] = {
            -1.0f,  1.0f,
            -1.0f, -1.0f,
            1.0f, -1.0f,
            1.0f,  1.0f,
    };

    private float textureVertices_WITHOUT_PROCESSING[] = {
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
    };


    // position de la ROI
    private float X = 200f;
    private float Y = 200f;

    // dimension video recue
    private float L = 640f;
    private float H = 720f;

    // order to draw vertices
    private short drawOrder_WITH_PROCESSING[] = {
            // zone 1
            0,  1,   2,
            0,  2,   3,
            // zone 2
            4,  5,   6,
            4,  6,   7,
            // zone 3
            8,  9,   10,
            8,  10,  11,
            // ROI
            12, 13,  14,
            12, 14,  15,
            // zone 4,
            16, 17,  18,
            16, 18,  19
    };

    // number of coordinates per vertex in this array
    private static final int COORDS_PER_VERTEX_WITH_PROCESSING = 2;

    private final int vertexStride = COORDS_PER_VERTEX_WITH_PROCESSING * 4; // 4 bytes per vertex

    private float squareCoords_WITH_PROCESSING[] = {
            // zone 1
            -1f,            1f,
            -1f,            -1f,
            -1f + (X / L),  -1f,
            -1f + (X / L),  1f,
            // zone 2
            X / L,          1f,
            X / L,          -1f,
            1f,             -1f,
            1f,             1f,
            // zone 3
            -1f + (X / L),  1f,
            -1f + (X / L),  1f - (2f * Y / H),
            X / L,          1f - (2f * Y / H),
            X / L,          1f,
            // ROI
            -1f + (X / L),  1f - (2f* Y / H),
            -1f + (X / L),  - 2f * Y / H,
            X / L,          - 2f * Y / H,
            X / L,          1 - (2f * Y / H),
            // zone 4
            -1f + (X / L),  -2f * Y / H,
            -1f + (X / L),  -1f,
            X / L,          -1f,
            X / L,          -2f * Y / H
    };

    private final static float VERTICE_MARGE_X = 1f / 1920f;
    private final static float VERTICE_MARGE_Y = 1f / 1080f;

    private float textureVertices_WITH_PROCESSING[] = {
            // zone 1
            0f,             1f,
            0f,             1f / 2f,
            X / (2f * L),   1f / 2f,
            X / (2f * L),   1f,
            // zone 2
            X / (2f * L),   1f,
            X / (2f * L),   1f / 2f,
            1f / 2f,        1f / 2f,
            1f / 2f,        1f,
            // zone 3
            1f / 2f,        1f,
            1f / 2f,        1f - (Y / (2f * H)),
            1f,             1f - (Y / (2f * H)),
            1f,             1f,
            // ROI
            0f,             1f / 2f,
            0f,             0f,
            1f,             0f,
            1f,             1f / 2f,
            // zone 4
            1f / 2f,        1- (Y /(2f * H)),
            1f / 2f,        3f / 4f,
            1f,             3f / 4f,
            1f,             1- (Y /(2f * H))
    };


    private final String mVertexShader = "uniform mat4 uMVPMatrix;\nuniform mat4 uSTMatrix;\nattribute vec4 aPosition;\nattribute vec4 aTextureCoord;\nvarying vec2 vTextureCoord;\nvoid main() {\n  gl_Position = uMVPMatrix * aPosition;\n  vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n}\n";
    private final String mFragmentShader = "#extension GL_OES_EGL_image_external : require\nprecision mediump float;\nvarying vec2 vTextureCoord;\nuniform samplerExternalOES sTexture;\nvoid main() {\n  gl_FragColor = texture2D(sTexture, vTextureCoord);\n}\n";
    private float[] mMVPMatrix = new float[16];
    private float[] mSTMatrix = new float[16];
    private int mProgram;
    private int mTextureID;
    private int muMVPMatrixHandle;
    private int muSTMatrixHandle;
    private int maPositionHandle;
    private int maTextureHandle;
    private SurfaceTexture mSurface;
    private boolean updateSurface = false;
    private static int GL_TEXTURE_EXTERNAL_OES = 0x8D65;

    private VideoWorker videoWorker;

    private HeadTransform head;

    private double ref = 0.0;

    int processingMode;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_main);
        this.cardboardView = (CardboardView) this.findViewById(R.id.cardboardview);
        this.cardboardView.setRenderer(this);
        this.setCardboardView(this.cardboardView);
        GLSurfaceView glSurface = this.cardboardView.getGLSurfaceView();
        /*
        this.mTriangleVertices = ByteBuffer.allocateDirect(this.mTriangleVerticesData.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.mTriangleVertices.put(this.mTriangleVerticesData).position(0);
        */
        processingMode = MainActivity.getProcessingMode(getApplicationContext());


        // TODO to be static
        for (int i = 0; i < squareCoords_WITH_PROCESSING.length; i++){
            squareCoords_WITH_PROCESSING[i] *= 2;
        }
        for (int i = 0; i < squareCoords_WITHOUT_PROCESSING.length; i++){
            squareCoords_WITHOUT_PROCESSING[i] *= 2;
        }

        // ajout de marge sur les vertices
        for(int i = 0; i < textureVertices_WITH_PROCESSING.length; i++){
            float facteur;
            if (i <= 15) {  // zone 1 ou 2
                facteur = 1;
            }
            else {
                facteur = 1;
            }

            if ((i % 8 == 0) || (i % 8 == 2)) {
                textureVertices_WITH_PROCESSING[i] += (facteur * VERTICE_MARGE_X);
            }
            else if ((i % 8 == 4) || (i % 8 == 6)) {
                textureVertices_WITH_PROCESSING[i] -= (facteur * VERTICE_MARGE_X);
            }
            else if ((i % 8 == 3) || (i % 8 == 5)) {
                textureVertices_WITH_PROCESSING[i] += (facteur * VERTICE_MARGE_Y);
            }
            else {  // =  else if ((i % 8 == 7) || (i % 8 == 1))
                textureVertices_WITH_PROCESSING[i] -= (facteur * VERTICE_MARGE_Y);
            }
        }

        // initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(processingMode == MainActivity.PROCESSING_MODE_WITH ?
                squareCoords_WITH_PROCESSING.length * 4 :
                squareCoords_WITHOUT_PROCESSING.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(processingMode == MainActivity.PROCESSING_MODE_WITH ?
                squareCoords_WITH_PROCESSING :
                squareCoords_WITHOUT_PROCESSING);
        vertexBuffer.position(0);

        // initialize byte buffer for the draw list
        ByteBuffer dlb = ByteBuffer.allocateDirect(processingMode == MainActivity.PROCESSING_MODE_WITH ?
                drawOrder_WITH_PROCESSING.length * 2 :
                drawOrder_WITHOUT_PROCESSING.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(processingMode == MainActivity.PROCESSING_MODE_WITH ?
                drawOrder_WITH_PROCESSING :
                drawOrder_WITHOUT_PROCESSING);
        drawListBuffer.position(0);

        ByteBuffer bb2 = ByteBuffer.allocateDirect(processingMode == MainActivity.PROCESSING_MODE_WITH ?
                textureVertices_WITH_PROCESSING.length * 4 :
                textureVertices_WITHOUT_PROCESSING.length * 4);
        bb2.order(ByteOrder.nativeOrder());
        textureVerticesBuffer = bb2.asFloatBuffer();
        textureVerticesBuffer.put(processingMode == MainActivity.PROCESSING_MODE_WITH ?
                textureVertices_WITH_PROCESSING :
                textureVertices_WITHOUT_PROCESSING);
        textureVerticesBuffer.position(0);

        Matrix.setIdentityM(this.mSTMatrix, 0);

        if (getWorkerMode(getApplicationContext()) == MainActivity.VIDEO_WORKER_MODE_NETWORK) {
            this.videoWorker = new VideoWorkerNetwork(this);
        }
        else{
            this.videoWorker = new VideoWorkerLocal(getApplicationContext());
        }
    }


    public void onNewFrame(HeadTransform headTransform) {

        synchronized (this) {
            if (this.updateSurface) {
                this.mSurface.updateTexImage();
                this.mSurface.getTransformMatrix(this.mSTMatrix);
                this.updateSurface = false;
            }
        }
        this.head = headTransform;

    }

    public void onDrawEye(Eye eye) {
        GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glUseProgram(mProgram);
        checkGlError("glUseProgram");

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mTextureID);


        /*
        mTriangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
        GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
        checkGlError("glVertexAttribPointer maPosition");
        GLES20.glEnableVertexAttribArray(maPositionHandle);
        checkGlError("glEnableVertexAttribArray maPositionHandle");

        mTriangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
        GLES20.glVertexAttribPointer(maTextureHandle, 3, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
        this.checkGlError("glVertexAttribPointer maTextureHandle");
        GLES20.glEnableVertexAttribArray(maTextureHandle);
        this.checkGlError("glEnableVertexAttribArray maTextureHandle");
        */

        int coords_per_vertex = processingMode == MainActivity.PROCESSING_MODE_WITH ?
                COORDS_PER_VERTEX_WITH_PROCESSING :
                COORDS_PER_VERTEX_WITHOUT_PROCESSING;

        GLES20.glEnableVertexAttribArray(maPositionHandle);
        GLES20.glVertexAttribPointer(maPositionHandle, coords_per_vertex, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);

        GLES20.glEnableVertexAttribArray(maTextureHandle);
        GLES20.glVertexAttribPointer(maTextureHandle, coords_per_vertex, GLES20.GL_FLOAT, false, vertexStride, textureVerticesBuffer);

        float[] euler = new float[16];
        this.head.getEulerAngles(euler, 0);
        //Log.d((String)"Euler", (String)("" + euler[0] + ";" + euler[1] + ";" + euler[2]));

        double cur = euler[1];
        if (this.ref == 0.0) {
            Log.d((String) "REF", (String) "init");
            this.ref = cur;
        }

        double a = cur - this.ref;
        //Log.d((String)"A", (String)("" + a + ""));
        if (a < -Math.PI) {
            a = 2 * Math.PI + a;
        } else if (a > Math.PI) {
            a -= 2 * Math.PI;
        }

        double b = (-euler[0]) / 1.57f;

        a*=2;
        b*=2;

        //Log.d((String)"Posi", (String)("" + (a *= 2.0) + ";" + (b *= 2.0)));

        float[] transMatrix = new float[16];
        Matrix.setIdentityM(mMVPMatrix, 0);
        Matrix.setIdentityM(transMatrix, 0);
        Matrix.translateM(transMatrix, 0, (float) a, (float) b, 0.0f);
        Matrix.multiplyMM(transMatrix, 0, mMVPMatrix, 0, transMatrix, 0);
        GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, transMatrix, 0);
        GLES20.glUniformMatrix4fv(muSTMatrixHandle,  1,  false,  this.mSTMatrix, 0);

        /*
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        this.checkGlError("glDrawArrays");
        */
        int drawOrderLength = processingMode == MainActivity.PROCESSING_MODE_WITH ?
                drawOrder_WITH_PROCESSING.length :
                drawOrder_WITHOUT_PROCESSING.length;
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrderLength, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);
        checkGlError("glDrawElements");

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(maPositionHandle);
        GLES20.glDisableVertexAttribArray(maTextureHandle);

        GLES20.glFinish();
    }

    public void onRendererShutdown() {
    }

    public synchronized void onFrameAvailable(SurfaceTexture surface) {
        //Log.d((String) "FRAME", (String) "New frame");
        this.updateSurface = true;
    }

    private int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        if (shader != 0) {
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                Log.e(TAG, "Could not compile shader " + shaderType + ":");
                Log.e(TAG, GLES20.glGetShaderInfoLog(shader));
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }

    private int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }
        int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (pixelShader == 0) {
            return 0;
        }

        int program = GLES20.glCreateProgram();
        if (program != 0) {
            GLES20.glAttachShader(program, vertexShader);
            checkGlError("glAttachShader");
            GLES20.glAttachShader(program, pixelShader);
            checkGlError("glAttachShader");
            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES20.GL_TRUE) {
                Log.e(TAG, "Could not link program: ");
                Log.e(TAG, GLES20.glGetProgramInfoLog(program));
                GLES20.glDeleteProgram(program);
                program = 0;
            }
        }
        return program;
    }

    public void onSurfaceCreated(EGLConfig config) {
        Log.d((String) "EGL", (String) config.toString());
        mProgram = createProgram(mVertexShader, mFragmentShader);
        if (mProgram == 0) {
            return;
        }
        maPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
        checkGlError("glGetAttribLocation aPosition");
        if (maPositionHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aPosition");
        }
        maTextureHandle = GLES20.glGetAttribLocation(mProgram, "aTextureCoord");
        checkGlError("glGetAttribLocation aTextureCoord");
        if (maTextureHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aTextureCoord");
        }

        muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        checkGlError("glGetUniformLocation uMVPMatrix");
        if (muMVPMatrixHandle == -1) {
            throw new RuntimeException("Could not get attrib location for uMVPMatrix");
        }

        muSTMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uSTMatrix");
        checkGlError("glGetUniformLocation uSTMatrix");
        if (muSTMatrixHandle == -1) {
            throw new RuntimeException("Could not get attrib location for uSTMatrix");
        }


        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);

        mTextureID = textures[0];
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mTextureID);
        checkGlError("glBindTexture mTextureID");

        GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);

        mSurface = new SurfaceTexture(mTextureID);
        mSurface.setOnFrameAvailableListener(this);

        Surface surface = new Surface(mSurface);

        videoWorker.configure(surface);

        synchronized(this) {
            updateSurface = false;
        }

        videoWorker.start();
    }

    public void onFinishFrame(Viewport viewport) {
    }

    public void onSurfaceChanged(int w, int h) {
        Log.d((String) "Surface", (String) ("W=" + w + "; H=" + h));
    }

    private static void checkGLError(String label) {
        int error = GLES20.glGetError();
        if (error != 0) {
            Log.e((String) TAG, (String) (label + ": glError " + error));
            throw new RuntimeException(label + ": glError " + error);
        }
    }

    private void checkGlError(String op) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, op + ": glError " + error);
            throw new RuntimeException(op + ": glError " + error);
        }
    }

    public void onCardboardTrigger() {
        videoWorker.updateZoi((int)(Math.random()*200),(int)(Math.random()*200));
    }

    protected void onImageBtnSettings(View view){
        final AlertDialog.Builder alertDialog =   new AlertDialog.Builder(MainActivity.this);
        alertDialog.setTitle("Settings");
        LayoutInflater inflater= getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.dialog_settings, null);
        alertDialog.setView(dialogView);

        final ToggleButton btnNetwork = (ToggleButton) dialogView.findViewById(R.id.btnNetwork);
        btnNetwork.setChecked(getWorkerMode(getApplicationContext()) == VIDEO_WORKER_MODE_NETWORK ?
                true :
                false);
        final ToggleButton btnProcessing = (ToggleButton) dialogView.findViewById(R.id.btnProcessing);
        btnProcessing.setChecked(getProcessingMode(getApplicationContext()) == PROCESSING_MODE_WITH ?
                true :
                false);

        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });

        alertDialog.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                setWorkerMode(btnNetwork.isChecked() ?
                        VIDEO_WORKER_MODE_NETWORK :
                        VIDEO_WORKER_MODE_LOCAL);
                setProcessingMode(btnProcessing.isChecked() ?
                        PROCESSING_MODE_WITH :
                        PROCESSING_MODE_WITHOUT);

                recreate();
            }
        });

        alertDialog.show();
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

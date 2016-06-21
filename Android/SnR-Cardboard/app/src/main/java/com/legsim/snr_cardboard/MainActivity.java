package com.legsim.snr_cardboard;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.vrtoolkit.cardboard.CardboardActivity;
import com.google.vrtoolkit.cardboard.CardboardView;
import com.google.vrtoolkit.cardboard.Eye;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Calendar;

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

    final static String IP_PREFERENCE = "IP preference";
    final static String IP_DEFAULT = "192.168.1.171";

    private Surface surface;

    private FloatBuffer vertexBuffer, textureVerticesBuffer;
    private ShortBuffer drawListBuffer;

    private short drawOrder_WITHOUT_PROCESSING[] = { 0, 1, 2, 0, 2, 3 }; // order to draw vertices

    // number of coordinates per vertex in this array
    private static final int COORDS_PER_VERTEX_WITHOUT_PROCESSING = 2;

    private final int vertexStride_WITHOUT_PROCESSING = COORDS_PER_VERTEX_WITHOUT_PROCESSING * 4; // 4 bytes per vertex

    private float squareCoords_WITHOUT_PROCESSING[] = {
            -2.0f,  2.0f,
            -2.0f,  -2.0f,
            2.0f,   -2.0f,
            2.0f,   2.0f,
    };

    private float textureVertices_WITHOUT_PROCESSING[] = {
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
    };


    // position de la ROI
    private float X = 0;
    private float Y = 0;

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

    private final static float VERTICE_MARGE_X = 1f / 1920f;
    private final static float VERTICE_MARGE_Y = 1f / 1080f;


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

    private int[] currentZoi = {0, 0};

    private double transA;
    private double transB;

    private double ref = 0.0;

    int processingMode;
    int workerMode;

    private final static int WIDTH = 1280;
    private final static int HEIGTH = 720;
    private final static int ZOIW = 640;
    private final static int ZOIH = 360;

    /*
    private long headXave;
    private long headYave;
    private int headCount;
    private long headLastTime;
    private final static long HEAD_DELAY = 1000;
    */

    private HeadHistory headHistory;

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
        workerMode = MainActivity.getWorkerMode(getApplicationContext());

        float squareCoords[];
        short drawOrder[];
        float textureVertices[];
        if (processingMode == MainActivity.PROCESSING_MODE_WITH){
            squareCoords = getSquareCoords_WITH_PROCESSING(X, Y, L, H);
            drawOrder = drawOrder_WITH_PROCESSING;
            textureVertices = getTextureVertices_WITH_PROCESSING(X, Y, L, H);
        }
        else{
            squareCoords = squareCoords_WITHOUT_PROCESSING;
            drawOrder = drawOrder_WITHOUT_PROCESSING;
            textureVertices = textureVertices_WITHOUT_PROCESSING;
        }

        // initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(squareCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(squareCoords);
        vertexBuffer.position(0);

        ByteBuffer bb2 = ByteBuffer.allocateDirect(textureVertices.length * 4);
        bb2.order(ByteOrder.nativeOrder());
        textureVerticesBuffer = bb2.asFloatBuffer();
        textureVerticesBuffer.put(textureVertices);
        textureVerticesBuffer.position(0);

        // initialize byte buffer for the draw list
        ByteBuffer dlb = ByteBuffer.allocateDirect(drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);

        Matrix.setIdentityM(this.mSTMatrix, 0);

        headHistory = new HeadHistory();

        if (getWorkerMode(getApplicationContext()) == MainActivity.VIDEO_WORKER_MODE_NETWORK) {
            this.videoWorker = new VideoWorkerNetwork(this);
        }
        else{
            this.videoWorker = new VideoWorkerLocal(getApplicationContext());
        }
    }

    public void onNewFrame(HeadTransform headTransform) {
        MainActivity mainActivity = this;
        synchronized (mainActivity) {
            if (this.updateSurface) {
                this.mSurface.updateTexImage();
                this.mSurface.getTransformMatrix(this.mSTMatrix);
                this.updateSurface = false;
                currentZoi = videoWorker.getZoi();
            }
        }

        this.head = headTransform;


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

        transA = a;
        transB = b;

        //Log.d("a,b",a+";"+b);

        double abis = a;
        double bbis = b;

        if (abis > 1){

            abis = 1;
        }
        else if (abis < -1){
            abis = -1;
        }
        if (bbis > 1){
            bbis = 1;
        }
        else if (bbis < -1){
            bbis = -1;
        }


        double newZoiX = (1- abis) * (WIDTH / 2 - ZOIW/2);
        double newZoiY = (1 + bbis) * (HEIGTH / 2 - ZOIH/2);
        if (newZoiX < 0) newZoiX = 0;
        if (newZoiX > ZOIW){
            newZoiX = ZOIW;
        }
        if (newZoiY < 0) newZoiY = 0;
        if (newZoiY > ZOIH){
            newZoiY = ZOIH;
        }
        //Log.d("newZoi", Math.round(newZoiX) + ";" + Math.round(newZoiY));

        headHistory.addPosition((int)Math.round(newZoiX),(int)Math.round(newZoiY));

        if (headHistory.isStable()) { // Tête stable
            int[] average = headHistory.getAverage();
            int nextZoi[] = this.videoWorker.getNextZoi();
            if (Math.abs(average[0] - nextZoi[0]) >= 40 || Math.abs(average[1] - nextZoi[1]) >= 40) { // ZOI trop éloignée de la position
                Log.d("HeadPosition","Updating to "+average[0]+";"+average[1]);
                videoWorker.updateZoi(average[0], average[1]);
            }
        }

        //Log.d("HeadAverage",""+headHistory.getAverage()[0]+";"+headHistory.getAverage()[1]+";"+headHistory.isStable());
        //Log.d("HeadStable",""+headHistory.isStable());


        /*
        headCount++;
        headXave += newZoiX;
        headYave += newZoiY;

        long now = Calendar.getInstance().getTimeInMillis();
        if (now - headLastTime > HEAD_DELAY){
            if (headCount != 0){
                headYave /= headCount;
                headXave /= headCount;
            }
            //Log.d("headAve", headYave + ";" + headXave);

            if ((Math.abs(newZoiX - headXave) < 50) && (Math.abs(newZoiY - headYave) < 50)) {
                Log.d("headAve", "ok");
                int zoi[] = this.videoWorker.getZoi();
                boolean newZoi = (Math.abs(zoi[0] - newZoiX) > 25) || (Math.abs(zoi[1] - newZoiY) > 25)
                        ? true : false;

                if (newZoi) {
                    this.videoWorker.updateZoi((int) newZoiX, (int) newZoiY);
                    Log.d("updateZoi", newZoiX + ";" + newZoiY);
                }
            }

            headCount = 0;
            headLastTime = now;
            headXave = 0;
            headYave = 0;
        }
        */




        //Log.d((String)"Posi", (String)("" + a + ";" + b));
        //Log.d((String)"Posi", (String)("" + (a *= 2.0) + ";" + (b *= 2.0)));

    }

    public void onDrawEye(Eye eye) {
        float squareCoords[];
        float textureVertices[];
        if (processingMode == MainActivity.PROCESSING_MODE_WITH){
            if (workerMode == MainActivity.VIDEO_WORKER_MODE_NETWORK) {
                int[] zoi = currentZoi;
                squareCoords = getSquareCoords_WITH_PROCESSING(zoi[0], zoi[1], L, H);
                textureVertices = getTextureVertices_WITH_PROCESSING(zoi[0], zoi[1], L, H);
            }
            else {  // video test
                squareCoords = getSquareCoords_WITH_PROCESSING(200, 200, L, H);
                textureVertices = getTextureVertices_WITH_PROCESSING(200, 200, L, H);
            }
        }
        else{
            squareCoords = squareCoords_WITHOUT_PROCESSING;
            textureVertices = textureVertices_WITHOUT_PROCESSING;
        }

        vertexBuffer.clear();
        vertexBuffer.put(squareCoords);
        vertexBuffer.position(0);

        textureVerticesBuffer.clear();
        textureVerticesBuffer.put(textureVertices);
        textureVerticesBuffer.position(0);

        GLES20.glClearColor(0.0f, 1.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glUseProgram(mProgram);
        checkGlError("glUseProgram");

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mTextureID);




        int coords_per_vertex = processingMode == MainActivity.PROCESSING_MODE_WITH ?
                COORDS_PER_VERTEX_WITH_PROCESSING :
                COORDS_PER_VERTEX_WITHOUT_PROCESSING;

        GLES20.glEnableVertexAttribArray(maPositionHandle);
        GLES20.glVertexAttribPointer(maPositionHandle, coords_per_vertex, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);

        GLES20.glEnableVertexAttribArray(maTextureHandle);
        GLES20.glVertexAttribPointer(maTextureHandle, coords_per_vertex, GLES20.GL_FLOAT, false, vertexStride, textureVerticesBuffer);





        float[] transMatrix = new float[16];
        Matrix.setIdentityM(mMVPMatrix, 0);
        Matrix.setIdentityM(transMatrix, 0);
        Matrix.translateM(transMatrix, 0, (float) transA, (float) transB, 0.0f);
        Matrix.multiplyMM(transMatrix, 0, mMVPMatrix, 0, transMatrix, 0);
        GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, transMatrix, 0);
        GLES20.glUniformMatrix4fv(muSTMatrixHandle,  1,  false,  this.mSTMatrix, 0);

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

        this.surface = surface;
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
        //videoWorker.updateZoi((int)(Math.random()*200),(int)(Math.random()*200));
    }

    protected void onImageBtnSettings(View view){
        final AlertDialog.Builder alertDialog =   new AlertDialog.Builder(MainActivity.this);
        alertDialog.setTitle("Settings");
        LayoutInflater inflater= getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.dialog_settings, null);
        alertDialog.setView(dialogView);

        final EditText editIp = (EditText) dialogView.findViewById(R.id.ip);
        editIp.setText(getIpPreference());
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
                String ip = editIp.getText().toString();
                if(! ip.isEmpty()){
                    setIpPreference(ip);
                }
                setWorkerMode(btnNetwork.isChecked() ?
                        VIDEO_WORKER_MODE_NETWORK :
                        VIDEO_WORKER_MODE_LOCAL);
                setProcessingMode(btnProcessing.isChecked() ?
                        PROCESSING_MODE_WITH :
                        PROCESSING_MODE_WITHOUT);

                // restart the app
                Intent i = getBaseContext().getPackageManager()
                        .getLaunchIntentForPackage( getBaseContext().getPackageName() );
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
            }
        });

        alertDialog.show();
    }

    private void setIpPreference(String newIp){
        setSharedPreference(IP_PREFERENCE, newIp);
    }

    public String getIpPreference(){
        return getSharedPreference(getApplicationContext(), MainActivity.IP_PREFERENCE, MainActivity.IP_DEFAULT);
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

    private void setSharedPreference(String key, String value){
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
                PREFERENCE_FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(key, value);
        editor.commit();
    }

    private static String getSharedPreference(Context context, String key, String defValue){
        SharedPreferences sharedPref = context.getSharedPreferences(
                MainActivity.PREFERENCE_FILE_NAME, Context.MODE_PRIVATE);
        return sharedPref.getString(key, defValue);
    }

    private static int getSharedPreference(Context context, String key, int defValue){
        SharedPreferences sharedPref = context.getSharedPreferences(
                MainActivity.PREFERENCE_FILE_NAME, Context.MODE_PRIVATE);
        return sharedPref.getInt(key, defValue);
    }

    private static float[] getSquareCoords_WITH_PROCESSING(float x, float y, float l, float h){
        float[] res = new float[]{
            // zone 1
            -1f, 1f,
            -1f, -1f,
            -1f + (x / l), -1f,
            -1f + (x / l), 1f,
            // zone 2
            x / l, 1f,
            x / l, -1f,
            1f, -1f,
            1f, 1f,
            // zone 3
            -1f + (x / l), 1f,
            -1f + (x / l), 1f - (2f * y / h),
            x / l, 1f - (2f * y / h),
            x / l, 1f,
            // ROI
            -1f + (x / l), 1f - (2f * y / h),
            -1f + (x / l), -2f * y / h,
            x / l, -2f * y / h,
            x / l, 1 - (2f * y / h),
            // zone 4
            -1f + (x / l), -2f * y / h,
            -1f + (x / l), -1f,
            x / l, -1f,
            x / l, -2f * y / h
        };

        // TODO a inclure dans le tab ci-dessus
        for (int i = 0; i < res.length; i++) {
            res[i] *= 2;
        }

        return res;
    }

    private static float[] getTextureVertices_WITH_PROCESSING(float x, float y, float l, float h){
        float[] res = new float[] {
            // zone 1
            0f,             1f,
            0f,             1f / 2f,
            x / (2f * l),   1f / 2f,
            x / (2f * l),   1f,
            // zone 2
            x / (2f * l),   1f,
            x / (2f * l),   1f / 2f,
            1f / 2f,        1f / 2f,
            1f / 2f,        1f,
            // zone 3
            1f / 2f,        1f,
            1f / 2f,        1f - (y / (2f * h)),
            1f,             1f - (y / (2f * h)),
            1f,             1f,
            // ROI
            0f,             1f / 2f,
            0f,             0f,
            1f,             0f,
            1f,             1f / 2f,
            // zone 4
            1f / 2f,        1- (y /(2f * h)),
            1f / 2f,        3f / 4f,
            1f,             3f / 4f,
            1f,             1- (y /(2f * h))
        };

        // TODO a inclure dans le tab ci-dessus
        // ajout de marge sur les vertices
        for(int i = 0; i < res.length; i++){
            float facteur;
            if (i <= 15) {  // zone 1 ou 2
                facteur = 6;
            }
            else {
                facteur = 3;
            }

            if ((i % 8 == 0) || (i % 8 == 2)) {
                res[i] += (facteur * VERTICE_MARGE_X);
            }
            else if ((i % 8 == 4) || (i % 8 == 6)) {
                res[i] -= (facteur * VERTICE_MARGE_X);
            }
            else if ((i % 8 == 3) || (i % 8 == 5)) {
                res[i] += (facteur * VERTICE_MARGE_Y);
            }
            else {  // =  else if ((i % 8 == 7) || (i % 8 == 1))
                res[i] -= (facteur * VERTICE_MARGE_Y);
            }
        }

        return res;
    }
}
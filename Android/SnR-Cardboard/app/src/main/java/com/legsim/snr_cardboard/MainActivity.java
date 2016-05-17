package com.legsim.snr_cardboard;

import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;

import com.google.vrtoolkit.cardboard.CardboardActivity;
import com.google.vrtoolkit.cardboard.CardboardView;
import com.google.vrtoolkit.cardboard.Eye;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;

public class MainActivity
        extends CardboardActivity
        implements CardboardView.StereoRenderer,
        SurfaceTexture.OnFrameAvailableListener {
    private CardboardView cardboardView;
    private static String TAG = "VideoRender";
    private static final int FLOAT_SIZE_BYTES = 4;
    private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
    private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
    private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;
    private final float[] mTriangleVerticesData = new float[]{-2.0f, -2.0f, 0.0f, 0.0f, 0.0f, 2.0f, -2.0f, 0.0f, 1.0f, 0.0f, -2.0f, 2.0f, 0.0f, 0.0f, 1.0f, 2.0f, 2.0f, 0.0f, 1.0f, 1.0f};
    private FloatBuffer mTriangleVertices;
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

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_main);
        this.cardboardView = (CardboardView) this.findViewById(R.id.cardboardview);
        this.cardboardView.setRenderer(this);
        this.setCardboardView(this.cardboardView);
        GLSurfaceView glSurface = this.cardboardView.getGLSurfaceView();
        this.mTriangleVertices = ByteBuffer.allocateDirect(this.mTriangleVerticesData.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.mTriangleVertices.put(this.mTriangleVerticesData).position(0);
        Matrix.setIdentityM(this.mSTMatrix, 0);
        this.videoWorker = new VideoWorkerNetwork();
    }


    public void onNewFrame(HeadTransform headTransform) {
        MainActivity mainActivity = this;
        synchronized (mainActivity) {
            if (this.updateSurface) {
                this.mSurface.updateTexImage();
                this.mSurface.getTransformMatrix(this.mSTMatrix);
                this.updateSurface = false;
            }
        }
        this.head = headTransform;
        /*
        GLES20.glClearColor((float)1.0f, (float)1.0f, (float)0.0f, (float)1.0f);
        GLES20.glClear((int)16640);
        GLES20.glUseProgram((int)this.mProgram);
        this.checkGlError("glUseProgram");
        GLES20.glActiveTexture((int)33984);
        GLES20.glBindTexture((int)GL_TEXTURE_EXTERNAL_OES, (int)this.mTextureID);
        this.mTriangleVertices.position(0);
        GLES20.glVertexAttribPointer((int)this.maPositionHandle, (int)3, (int)5126, (boolean)false, (int)20, (Buffer)this.mTriangleVertices);
        this.checkGlError("glVertexAttribPointer maPosition");
        GLES20.glEnableVertexAttribArray((int)this.maPositionHandle);
        this.checkGlError("glEnableVertexAttribArray maPositionHandle");
        this.mTriangleVertices.position(3);
        GLES20.glVertexAttribPointer((int)this.maTextureHandle, (int)3, (int)5126, (boolean)false, (int)20, (Buffer)this.mTriangleVertices);
        this.checkGlError("glVertexAttribPointer maTextureHandle");
        GLES20.glEnableVertexAttribArray((int)this.maTextureHandle);
        this.checkGlError("glEnableVertexAttribArray maTextureHandle");
        float[] transMatrix = new float[16];
        Matrix.setIdentityM((float[])transMatrix, (int)0);
        Matrix.translateM((float[])transMatrix, (int)0, (float)150.0f, (float)150.0f, (float)0.0f);
        Matrix.multiplyMM((float[])transMatrix, (int)0, (float[])this.mMVPMatrix, (int)0, (float[])transMatrix, (int)0);
        GLES20.glUniformMatrix4fv((int)this.muMVPMatrixHandle, (int)1, (boolean)false, (float[])transMatrix, (int)0);
        GLES20.glUniformMatrix4fv((int)this.muSTMatrixHandle, (int)1, (boolean)false, (float[])this.mSTMatrix, (int)0);
        GLES20.glDrawArrays((int)5, (int)0, (int)4);
        this.checkGlError("glDrawArrays");
        GLES20.glFinish();
        Matrix.setLookAtM((float[])this.camera, (int)0, (float)0.0f, (float)0.0f, (float)0.01f, (float)0.0f, (float)0.0f, (float)0.0f, (float)0.0f, (float)1.0f, (float)0.0f);
        MainActivity.checkGLError("onReadyToDraw");
        */
    }

    public void onDrawEye(Eye eye) {
        GLES20.glClearColor(0.0f, 1.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glUseProgram(mProgram);
        checkGlError("glUseProgram");

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mTextureID);

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

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        this.checkGlError("glDrawArrays");

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
}
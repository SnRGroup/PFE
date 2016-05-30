package com.legsim.stream;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

@SuppressLint("ViewConstructor")
class VideoSurfaceView extends GLSurfaceView {

    private static final String TAG = VideoSurfaceView.class.getSimpleName();

    private static int width;
    private static int height;

    private VideoRender mRenderer;
    private VideoWorker videoWorker;

    public VideoSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setEGLContextClientVersion(2);

        switch(MainActivity.getWorkerMode(context)){
            case MainActivity.VIDEO_WORKER_MODE_LOCAL:
                videoWorker = new VideoWorkerLocal(context);
                break;
            case MainActivity.VIDEO_WORKER_MODE_NETWORK:
                videoWorker = new VideoWorkerNetwork();
                break;
            default:
                Log.e(TAG, "invalid video worker mode");
        }

        mRenderer = new VideoRender(context);
        setRenderer(mRenderer);
    }

    @Override
    public void onResume() {
        queueEvent(new Runnable(){
                public void run() {
                    mRenderer.setVideoDecoder(videoWorker);
                }});

        super.onResume();
    }

    private static class VideoRender
        implements Renderer, SurfaceTexture.OnFrameAvailableListener {
        private static String TAG = "VideoRender";

        private FloatBuffer vertexBuffer, textureVerticesBuffer;
        private ShortBuffer drawListBuffer;
/*
        private short drawOrder[] = { 0,1,2, 0,2,4, 5,3,6, 5,6,7 }; // order to draw vertices

        // number of coordinates per vertex in this array
        private static final int COORDS_PER_VERTEX = 2;

        private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

        static float squareCoords[] = {
                -1.0f,  1.0f,
                -1.0f, -1.0f,
                0.0f, -1.0f,
                0.0f, -1.0f,
                0.0f,  1.0f,
                0.0f,  1.0f,
                1.0f, -1.0f,
                1.0f, 1.0f,
        };

        static float textureVertices[] = {
                0.5f, 1.0f,
                0.5f, 0.0f,
                1.0f, 0.0f,
                0.0f, 0.0f,
                1.0f, 1.0f,
                0.0f, 1.0f,
                0.5f, 0.0f,
                0.5f, 1.0f,
        };
*/
/*
    private short drawOrder[] = { 0, 1, 2, 0, 2, 3 }; // order to draw vertices

    // number of coordinates per vertex in this array
    private static final int COORDS_PER_VERTEX = 2;

    private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

    private float squareCoords[] = {
       -1.0f,  1.0f,
       -1.0f, -1.0f,
        1.0f, -1.0f,
        1.0f,  1.0f,
    };

    private float textureVertices[] = {
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
    };
*/

        // position de la ROI
        private float X = 200f;
        private float Y = 200f;

        // dimension video recue
        private float L = 640f;
        private float H = 720f;

        // order to draw vertices
        private short drawOrder[] = {
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
        private static final int COORDS_PER_VERTEX = 2;

        private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

        private float squareCoords[] = {
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

        private float textureVertices[] = {
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


        private final static int VERTEX_SHADER_RESOURCE_ID = R.raw.vertex_shader;

        private final static int FRAGMENT_SHADER_RESOURCE_ID = R.raw.fragment_shader;


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

        private Context context;

        public VideoRender(Context context) {
            this.context = context;

            // initialize vertex byte buffer for shape coordinates
            ByteBuffer bb = ByteBuffer.allocateDirect(squareCoords.length * 4);
            bb.order(ByteOrder.nativeOrder());
            vertexBuffer = bb.asFloatBuffer();
            vertexBuffer.put(squareCoords);
            vertexBuffer.position(0);

            // initialize byte buffer for the draw list
            ByteBuffer dlb = ByteBuffer.allocateDirect(drawOrder.length * 2);
            dlb.order(ByteOrder.nativeOrder());
            drawListBuffer = dlb.asShortBuffer();
            drawListBuffer.put(drawOrder);
            drawListBuffer.position(0);

            ByteBuffer bb2 = ByteBuffer.allocateDirect(textureVertices.length * 4);
            bb2.order(ByteOrder.nativeOrder());
            textureVerticesBuffer = bb2.asFloatBuffer();
            textureVerticesBuffer.put(textureVertices);
            textureVerticesBuffer.position(0);

            Matrix.setIdentityM(mSTMatrix, 0);
        }

        public void setVideoDecoder(VideoWorker vdt) {
            videoWorker = vdt;
        }

        @Override
        public void onDrawFrame(GL10 glUnused) {
            //Log.d(TAG, "on draw");
            synchronized(this) {
                if (updateSurface) {
                    mSurface.updateTexImage();
                    mSurface.getTransformMatrix(mSTMatrix);
                    updateSurface = false;
                }
            }

            GLES20.glClearColor(0.0f, 1.0f, 0.0f, 1.0f);
            GLES20.glClear( GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

            GLES20.glUseProgram(mProgram);
            checkGlError("glUseProgram");

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mTextureID);
/*
            // test
            textureVertices[0] = (textureVertices[0] + 0.01f) % 1.0f;
            Log.d(TAG, "textureVertices[0]: " + textureVertices[0]);
            textureVerticesBuffer.position(0);
            textureVerticesBuffer.put(textureVertices);
            textureVerticesBuffer.position(0);
*/
            // Prepare the <insert shape here> coordinate data
            GLES20.glEnableVertexAttribArray(maPositionHandle);
            GLES20.glVertexAttribPointer(maPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);

            GLES20.glEnableVertexAttribArray(maTextureHandle);
            GLES20.glVertexAttribPointer(maTextureHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, textureVerticesBuffer);

            float[] transMatrix = new float[16];
            float[] scaleMatrix = new float[16];
            Matrix.setIdentityM(mMVPMatrix, 0);
            Matrix.setIdentityM(transMatrix, 0);
            Matrix.setIdentityM(scaleMatrix, 0);
            // Matrix.translateM(transMatrix, 0, -0.5f, -0.5f, 0.0f);
            // Matrix.scaleM(scaleMatrix, 0, 2.0f, 2.0f, 0.0f);
            Matrix.multiplyMM(mMVPMatrix, 0, transMatrix, 0, scaleMatrix, 0);
            GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0);
            GLES20.glUniformMatrix4fv(muSTMatrixHandle,  1,  false,  this.mSTMatrix, 0);

            /*GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
            checkGlError("glDrawArrays");
            */
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);
            checkGlError("glDrawElements");

            // Disable vertex array
            GLES20.glDisableVertexAttribArray(maPositionHandle);
            GLES20.glDisableVertexAttribArray(maTextureHandle);

            GLES20.glFinish();
        }

        @Override
        public void onSurfaceChanged(GL10 glUnused, int newWidth, int newHeight) {
            width = newWidth;
            height = newHeight;
        	Log.d(TAG, "onsurfacechanged: " + width + ", " + height);

        }

        @Override
        public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
            mProgram = createProgram(VERTEX_SHADER_RESOURCE_ID, FRAGMENT_SHADER_RESOURCE_ID);
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

            /*
             * Create the SurfaceTexture that will feed this textureID,
             * and pass it to the MediaPlayer
             */
            mSurface = new SurfaceTexture(mTextureID);
            mSurface.setOnFrameAvailableListener(this);

            Surface surface = new Surface(mSurface);

            videoWorker.configure(surface);

            synchronized(this) {
                updateSurface = false;
            }

            videoWorker.start();
        }

        synchronized public void onFrameAvailable(SurfaceTexture surface) {
            updateSurface = true;
        }

        private String readTextFileFromResource(int resourceId) {
            StringBuilder body = new StringBuilder();
            try {
                InputStream inputStream =
                        context.getResources().openRawResource(resourceId);
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String nextLine;
                while ((nextLine = bufferedReader.readLine()) != null) {
                    body.append(nextLine);
                    body.append('\n');
                }
            } catch (IOException e) {
                Log.e(TAG, "Could not open resource: " + resourceId, e);
            } catch (Resources.NotFoundException nfe) {
                Log.e(TAG, "Resource not found: " + resourceId, nfe);
            }
            return body.toString();
        }

        private int loadShader(int shaderType, int resourceId) {
            String source = readTextFileFromResource(resourceId);

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

        private int createProgram(int vertexResourceId, int fragmentResourceId) {
            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexResourceId);
            if (vertexShader == 0) {
                return 0;
            }
            int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentResourceId);
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

        private void checkGlError(String op) {
            int error;
            while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
                Log.e(TAG, op + ": glError " + error);
                throw new RuntimeException(op + ": glError " + error);
            }
        }

    }  // End of class VideoRender.

}  // End of class VideoSurfaceView.

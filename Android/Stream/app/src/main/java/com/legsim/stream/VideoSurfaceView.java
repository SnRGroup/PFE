package com.legsim.stream;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
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
import java.nio.IntBuffer;

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

        private static final int FLOAT_SIZE_BYTES = 4;
        private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
        private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
        private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;
        private final float[] mTriangleVerticesData = {
            // X, Y, Z, U, V
            -1.0f, -1.0f, 0, 0.f, 0.f,
            1.0f, -1.0f, 0, 1.f, 0.f,
            -1.0f,  1.0f, 0, 0.f, 1.f,
            1.0f,  1.0f, 0, 1.f, 1.f,
        };

        private FloatBuffer mTriangleVertices;

        private final static int VERTEX_SHADER_RESOURCE_ID = R.raw.vertex_shader;

        private final String mVertexShader =
                "uniform mat4 uMVPMatrix;\n" +
                "uniform mat4 uSTMatrix;\n" +
                "attribute vec4 aPosition;\n" +
                "attribute vec4 aTextureCoord;\n" +
                "varying vec2 vTextureCoord;\n" +
                "void main() {\n" +
                "  gl_Position = uMVPMatrix * aPosition;\n" +
                        "  gl_Position = vec4(gl_Position[0], gl_Position[1] + 10.0, gl_Position[2], gl_Position[3]);\n"+

                /*"if (gl_Position[1] > 0.0)\n" +
                "{\n"+
                "  gl_Position = vec4(gl_Position[0], gl_Position[1] - 1.0, gl_Position[2], gl_Position[3]);\n"+
                "}\n"+
                "else\n"+
                "{\n"+
                "  gl_Position = vec4(gl_Position[0], gl_Position[1] + 1.0, gl_Position[2], gl_Position[3]);\n"+
                "}\n"+*/
                 "  vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" +
                "}\n";

        private final static int FRAGMENT_SHADER_RESOURCE_ID = R.raw.fragment_shader;

        private final String mFragmentShader =
                "#extension GL_OES_EGL_image_external : require\n" +
                "precision mediump float;\n" +
                "varying vec2 vTextureCoord;\n" +
                "uniform samplerExternalOES sTexture;\n" +
                "void main() {\n" +
                        /*"if (vTextureCoord[1] > 0.5)" +
                        "{" +
                        "   vec2 tc = vec2(vTextureCoord[0], vTextureCoord[1] - 0.5);" +
                        "   gl_FragColor = texture2D( sTexture, tc );" +
                        "}" +
                        "else" +
                        "{" +
                        "   vec2 tc = vec2(vTextureCoord[0], vTextureCoord[1] - 0.5);" +
                        "   gl_FragColor = texture2D( sTexture, tc );" +
                        "}" +
*/
                "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                "}\n";

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

        private IntBuffer ib = IntBuffer.allocate(1);
        int b[]=new int[1];
        int bt[]=new int[1];

        private Context context;

        public VideoRender(Context context) {
            this.context = context;

            mTriangleVertices = ByteBuffer.allocateDirect(
                mTriangleVerticesData.length * FLOAT_SIZE_BYTES)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            mTriangleVertices.put(mTriangleVerticesData).position(0);

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

            mTriangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
            GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
            checkGlError("glVertexAttribPointer maPosition");
            GLES20.glEnableVertexAttribArray(maPositionHandle);
            checkGlError("glEnableVertexAttribArray maPositionHandle");

            mTriangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
            GLES20.glVertexAttribPointer(maTextureHandle, 3, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
            checkGlError("glVertexAttribPointer maTextureHandle");
            GLES20.glEnableVertexAttribArray(maTextureHandle);
            checkGlError("glEnableVertexAttribArray maTextureHandle");

           /* Matrix.setIdentityM(mMVPMatrix, 0);
            GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0);
            GLES20.glUniformMatrix4fv(muSTMatrixHandle, 1, false, mSTMatrix, 0);*/
            float[] transMatrix = new float[16];
            Matrix.setIdentityM(mMVPMatrix, 0);
            Matrix.setIdentityM(transMatrix, 0);
            Matrix.translateM(transMatrix, 0, 0, 0.0f, 0.0f);
            Matrix.multiplyMM(transMatrix, 0, mMVPMatrix, 0, transMatrix, 0);
            GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, transMatrix, 0);
            GLES20.glUniformMatrix4fv(muSTMatrixHandle,  1,  false,  this.mSTMatrix, 0);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
            checkGlError("glDrawArrays");


/*
            ib.position(0);
            GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGB , GLES20.GL_UNSIGNED_BYTE , ib );
            for(int i=0, k=0; i<height; i++, k++)
            {//remember, that OpenGL bitmap is incompatible with Android bitmap
                //and so, some correction need.
                for(int j=0; j<width; j++)
                {
                    int pix=b[i*width+j];
                    int pb=(pix>>16)&0xff;
                    int pr=(pix<<16)&0x00ff0000;
                    int pix1=(pix&0xff00ff00) | pr | pb;
                    bt[(height-k-1)*width+j]=pix1;
                }
            }

            Bitmap sb=Bitmap.createBitmap(bt, width, height, Bitmap.Config.ARGB_8888);
*/
            /*
            gst_gl_shader_set_uniform_matrix_4fv(shader, "u_transformation", 1, FALSE, identity_matrix);
            GLES20.
                    */

            GLES20.glFinish();



            //GLES20.glReadPixels();
            // int pixels[] = SavePixels(0, 0, width, height);
            // Log.d(TAG, "pixels len: " + pixels.length);
            //IntBuffer ib = SavePixels(0, 0, width, height);
            //Log.d(TAG, "ib.limit(): " + ib.limit());

        }

        @Override
        public void onSurfaceChanged(GL10 glUnused, int newWidth, int newHeight) {
            width = newWidth;
            height = newHeight;
            ib = IntBuffer.allocate(width * height);
            b = new int[width * height];
            bt = new int[width * height];
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

    public static IntBuffer SavePixels(int x, int y, int w, int h){
        //int b[]=new int[w*(y+h)];      // int ?
        //int bt[]=new int[w*h];  // int ?
        //IntBuffer ib = IntBuffer.wrap(b);
        IntBuffer ib = IntBuffer.allocate(w * (y + h));
        //ib.position(0);
        //GLES20.glReadPixels(0, 0, w, h, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, ib);
        GLES20.glReadPixels(0, 0, w, h, GLES20.GL_RGB , GLES20.GL_INT , ib );
        //return b;
        return ib;
        /*
        for(int i=0, k=0; i<h; i++, k++)
        {//remember, that OpenGL bitmap is incompatible with Android bitmap
            //and so, some correction need.
            for(int j=0; j<w; j++)
            {
                int pix=b[i*w+j];
                int pb=(pix>>16)&0xff;
                int pr=(pix<<16)&0x00ff0000;
                int pix1=(pix&0xff00ff00) | pr | pb;
                bt[(h-k-1)*w+j]=pix1;
            }
        }

        Bitmap sb=Bitmap.createBitmap(bt, w, h, Bitmap.Config.ARGB_8888);
        return sb;
        */
    }

}  // End of class VideoSurfaceView.

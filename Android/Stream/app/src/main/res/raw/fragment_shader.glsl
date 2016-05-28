#extension GL_OES_EGL_image_external : require

precision mediump float;
varying vec2 vTextureCoord;
uniform samplerExternalOES sTexture;

void main() {
    vec2 vtc;

    vec2 a1 = vec2(0.0, 0.0);
    vec2 c1 = vec2(200.0 / 1280.0, 1.0);
    vec2 e1 = vec2(0.0, 0.0);
    vec2 z1 = vec2(1.0, 2.0);

    vec2 a2 = vec2(840.0 / 1280.0, 0.0);
    vec2 c2 = vec2(1.0, 1.0);
    vec2 e2 = vec2(100.0 / 640.0, 0.0);
    vec2 z2 = vec2(1.0, 2.0);

    vec2 a3 = vec2(200.0 / 1280.0, 0.0);
    vec2 c3 = vec2(840.0 / 1280.0, 200.0 / 720.0);
    vec2 e3 = vec2(320.0 / 640.0, 0.0);
    vec2 z3 = vec2(1.0, 2.0);

    vec2 a4 = vec2(200.0 / 1280.0, 560.0 / 720.0);
    vec2 c4 = vec2(840.0 / 1280.0, 1.0);
    vec2 e4 = vec2(320.0 / 640.0, 100.0 / 720.0);
    vec2 z4 = vec2(1.0, 2.0);

    vec2 ar = vec2(200.0 / 1280.0, 200.0 / 720.0);
    vec2 cr = vec2(840.0 / 1280.0, 560.0 / 720.0);
    vec2 er = vec2(0.0, 360.0 / 720.0);
    vec2 zr = vec2(0.5, 1.0);

    if (vTextureCoord[0] >= a1[0] && vTextureCoord[0] <= c1[0]
        && vTextureCoord[1] >= a1[1] && vTextureCoord[1] <= c1[1]) {

        vec2 l = vec2(vTextureCoord[0] - a1[0], vTextureCoord[1] - a1[1]);
        vtc = vec2(e1[0] + (l[0] / z1[0]), e1[1] + (l[1] / z1[1]));
    }
    else if (vTextureCoord[0] >= a2[0] && vTextureCoord[0] <= c2[0]
        && vTextureCoord[1] >= a2[1] && vTextureCoord[1] <= c2[1]) {

        vec2 l = vec2(vTextureCoord[0] - a2[0], vTextureCoord[1] - a2[1]);
        vtc = vec2(e2[0] + (l[0] / z2[0]), e2[1] + (l[1] / z2[1]));
    }
    else if (vTextureCoord[0] >= a3[0] && vTextureCoord[0] <= c3[0]
        && vTextureCoord[1] >= a3[1] && vTextureCoord[1] <= c3[1]) {

        vec2 l = vec2(vTextureCoord[0] - a3[0], vTextureCoord[1] - a3[1]);
        vtc = vec2(e3[0] + (l[0] / z3[0]), e3[1] + (l[1] / z3[1]));
    }
    else if (vTextureCoord[0] >= a4[0] && vTextureCoord[0] <= c4[0]
        && vTextureCoord[1] >= a4[1] && vTextureCoord[1] <= c4[1]) {

        vec2 l = vec2(vTextureCoord[0] - a4[0], vTextureCoord[1] - a4[1]);
        vtc = vec2(e4[0] + (l[0] / z4[0]) , e4[1] + (l[1] / z4[1]));
    }
    else if (vTextureCoord[0] >= ar[0] && vTextureCoord[0] <= cr[0]
        && vTextureCoord[1] >= ar[1] && vTextureCoord[1] <= cr[1]) {

        vec2 l = vec2(vTextureCoord[0] - ar[0], vTextureCoord[1] - ar[1]);
        vtc = vec2(er[0] + (l[0] / zr[0]), er[1] + (l[1] / zr[1]));
    }

    vtc = vec2(vTextureCoord[0], vTextureCoord[1]);
    gl_FragColor = texture2D(sTexture, vtc);
}
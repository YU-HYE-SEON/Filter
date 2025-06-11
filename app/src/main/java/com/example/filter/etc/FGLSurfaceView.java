package com.example.filter.etc;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

public class FGLSurfaceView extends GLSurfaceView {
    private FGLRenderer renderer;

    private void init(Context context) {
        setEGLContextClientVersion(2);
        renderer = new FGLRenderer(context, this);
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    public FGLSurfaceView(Context context) {
        super(context);
        init(context);
    }

    public FGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public FGLRenderer getRenderer() {
        return renderer;
    }
}

package com.nvidia.developer.opengl.demos.os;

import com.nvidia.developer.opengl.app.NvInputDeviceType;
import com.nvidia.developer.opengl.app.NvPointerActionType;
import com.nvidia.developer.opengl.app.NvPointerEvent;
import com.nvidia.developer.opengl.app.NvSampleApp;

import org.lwjgl.util.vector.Matrix4f;

/**
 * Created by mazhen'gui on 2017/6/5.
 */

public class LauncherDemo extends NvSampleApp{

    OSRenderer m_renderer;
    final Matrix4f m_view = new Matrix4f();

    @Override
    protected void initRendering() {
        m_renderer = new OSRenderer();
        m_renderer.initlize();
    }

    @Override
    public void display() {
        m_transformer.getModelViewMat(m_view);
        m_renderer.drawScenes(m_view, getFrameDeltaTime() );
    }

    @Override
    public boolean handlePointerInput(NvInputDeviceType device, int action, int modifiers, int count, NvPointerEvent[] points) {
        if(action == NvPointerActionType.DOWN){

        }

        return super.handlePointerInput(device, action, modifiers, count, points);
    }

    @Override
    protected void reshape(int width, int height) {
        if(width <= 0 || height <= 0)
            return;

        m_renderer.onResize(width, height);
    }
}

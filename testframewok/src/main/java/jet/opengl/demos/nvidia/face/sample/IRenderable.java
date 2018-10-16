package jet.opengl.demos.nvidia.face.sample;

import com.nvidia.developer.opengl.models.obj.NvGLModel;

import org.lwjgl.util.vector.Vector3f;

import jet.opengl.postprocessing.common.GLFuncProvider;

abstract class IRenderable {

    float	m_uvScale = 1.f;				// Average world-space size of 1 UV unit
    final Vector3f m_posCenter = new Vector3f();

    abstract Vector3f getPosMin();
    abstract Vector3f getPosMax();
    abstract void Draw(int primitive);
}

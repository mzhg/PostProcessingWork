package jet.opengl.demos.nvidia.face.sample;

import org.lwjgl.util.vector.Vector3f;

import java.nio.ByteBuffer;
import java.util.HashMap;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.DebugTools;

final class CMeshDebug extends IRenderable implements Disposeable {
    private GLFuncProvider gl;
    private int m_modelVB;
    private int m_modelIB;

    private final Vector3f m_posMin = new Vector3f();
    private final Vector3f m_posMax = new Vector3f();

    private int m_indiceCount;

    CMeshDebug(){
        gl = GLFuncProviderFactory.getGLFuncProvider();
    }

    void loadModel(String filename){
        int index = filename.lastIndexOf('.');
        String vertexFileName = filename.substring(0, index) + "DX_Vertex.dat";
        ByteBuffer data = DebugTools.loadBinary(vertexFileName);

        m_modelVB = gl.glGenBuffer();
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, m_modelVB);
        gl.glBufferData(GLenum.GL_ARRAY_BUFFER, data, GLenum.GL_STATIC_DRAW);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);

        String indiceFileName = filename.substring(0, index) + "DX_Indice.dat";
        data = DebugTools.loadBinary(indiceFileName);
        m_indiceCount = data.remaining() / 4;
        m_modelIB = gl.glGenBuffer();
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, m_modelIB);
        gl.glBufferData(GLenum.GL_ARRAY_BUFFER, data, GLenum.GL_STATIC_DRAW);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);

        String meshInfo = filename.substring(0, index) + "DX_Info.txt";
        HashMap<String, Object> values = DebugTools.loadFileStreamTxt(meshInfo);

        m_uvScale = (Float)values.get("UVScale");
        m_posMin.set((Vector3f)values.get("PosMin"));
        m_posMax.set((Vector3f)values.get("PosMax"));

        Vector3f.mix(m_posMin, m_posMax, 0.5f, m_posCenter);
    }

    Vector3f getPosMin(){ return m_posMin;}
    Vector3f getPosMax(){ return m_posMax;}

    void Draw(int primitive){
        final int positionHandle = 0;
        final int normalHandle = 1;
        final int texcoordHandle = 2;
        final int tangentHandle = 3;
        final int curvatureHandle = 4;
        final int stride = (3 + 3 + 2 + 3 + 1) * 4;

//        m_model.drawElements(0, 1, 2, 3);
        int VB = m_modelVB;
        int IB = m_modelIB;

        gl.glBindVertexArray(0);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, VB);

        gl.glVertexAttribPointer(positionHandle, 3, GLenum.GL_FLOAT, false, stride, 0);
        gl.glEnableVertexAttribArray(positionHandle);

        if (normalHandle >= 0) {
            gl.glVertexAttribPointer(normalHandle, 3, GLenum.GL_FLOAT, false, stride, 12);
            gl.glEnableVertexAttribArray(normalHandle);
        }

        if (texcoordHandle >= 0) {
            gl.glVertexAttribPointer(texcoordHandle, 2, GLenum.GL_FLOAT, false, stride, 24);
            gl.glEnableVertexAttribArray(texcoordHandle);
        }

        if (tangentHandle >= 0) {
            gl.glVertexAttribPointer(tangentHandle, 3, GLenum.GL_FLOAT, false, stride, 32);
            gl.glEnableVertexAttribArray(tangentHandle);
        }

        gl.glVertexAttribPointer(curvatureHandle,  1, GLenum.GL_FLOAT, false, stride, 36);
        gl.glEnableVertexAttribArray(curvatureHandle);

        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, IB);
        gl.glDrawElements(primitive, m_indiceCount, GLenum.GL_UNSIGNED_INT, 0);

        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);

        gl.glDisableVertexAttribArray(0);
        gl.glDisableVertexAttribArray(1);
        gl.glDisableVertexAttribArray(2);
        gl.glDisableVertexAttribArray(3);
        gl.glDisableVertexAttribArray(4);
    }

    @Override
    public void dispose() {
        gl.glDeleteBuffer(m_modelIB);
        gl.glDeleteBuffer(m_modelVB);

        m_modelIB = 0;
        m_modelVB = 0;
    }
}

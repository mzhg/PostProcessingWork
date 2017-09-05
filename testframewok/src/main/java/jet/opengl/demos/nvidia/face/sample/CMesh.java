package jet.opengl.demos.nvidia.face.sample;

import org.lwjgl.util.vector.Vector3f;

import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.StackInt;

/**
 * Created by mazhen'gui on 2017/9/5.
 */

final class CMesh implements Disposeable{
    final List<Vertex> m_verts = new ArrayList<>();
    final StackInt m_indices = new StackInt(16);

    int				m_pVtxBuffer;
    int				m_pIdxBuffer;
    int						m_vtxStride;			// Vertex stride for IASetVertexBuffers
    int						m_cIdx;					// Index count for DrawIndexed
    int	m_primtopo;
    final Vector3f m_posMin = new Vector3f(), m_posMax = new Vector3f();		// Bounding box in local space
    final Vector3f			m_posCenter = new Vector3f();			// Center of bounding box
    float						m_diameter;				// Diameter of bounding box
    float						m_uvScale = 1.f;				// Average world-space size of 1 UV unit
    private GLFuncProvider gl;

    CMesh(){
        gl = GLFuncProviderFactory.getGLFuncProvider();
    }

    void Draw(){
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, m_pVtxBuffer);
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, m_pIdxBuffer);
        gl.glDrawElements(m_primtopo, m_cIdx, GLenum.GL_UNSIGNED_INT, 0);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    @Override
    public void dispose() {
        gl.glDeleteBuffer(m_pVtxBuffer);
        gl.glDeleteBuffer(m_pIdxBuffer);
    }
}

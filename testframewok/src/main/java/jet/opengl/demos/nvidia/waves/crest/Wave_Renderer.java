package jet.opengl.demos.nvidia.waves.crest;

import org.lwjgl.util.vector.Matrix4f;

import jet.opengl.demos.nvidia.waves.ocean.Technique;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.CacheBuffer;

public class Wave_Renderer {
    private final WaveRenderDesc m_Desc = new WaveRenderDesc();
    private final Wave_Shading_ShaderData m_ShaderData = new Wave_Shading_ShaderData();
    private Wave_CDClipmap m_Clipmap;
    private Wave_Simulation m_Simulation;

    private Technique m_ShadingShader;

    public void init(Wave_CDClipmap clipmap, Wave_Simulation simulation){
        m_Clipmap = clipmap;
        m_Simulation = simulation;
    }

    public void waveShading(Matrix4f cameraProj, Matrix4f cameraView, boolean wireframe){
        m_Desc.debugWireframe = wireframe;

        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        gl.glEnable(GLenum.GL_DEPTH_TEST);
        gl.glDisable(GLenum.GL_CULL_FACE);
        gl.glDisable(GLenum.GL_BLEND);

        m_ShadingShader = ShaderManager.getInstance().getWaveRender(m_Desc);
//        m_ShadingShader.getDepthStencil().depthEnable = true;
//        m_ShadingShader.getDepthStencil().depthFunc = GLenum.GL_LESS;
//        m_ShadingShader.getDepthStencil().depthWriteMask = true;
//        m_ShadingShader.getRaster().cullFaceEnable = false;
//        m_ShadingShader.getBlend().blendEnable = false;
        m_ShadingShader.setStateEnabled(false);
        if(wireframe){
            gl.glPolygonMode(GLenum.GL_FRONT_AND_BACK, GLenum.GL_LINE);
        }

        final Matrix4f clipmapTransform = CacheBuffer.getCachedMatrix();
        final Matrix4f nodeTransform = CacheBuffer.getCachedMatrix();

        Matrix4f.mul(cameraProj, cameraView, m_ShaderData.UNITY_MATRIX_VP);

        m_Clipmap.getData(m_ShaderData, clipmapTransform);
        int nodeCount = m_Clipmap.getNodeCount();
        for(int i = 0; i < nodeCount; i++){
            m_Clipmap.getNodeInfo(i, m_Simulation, m_ShaderData, nodeTransform);
            Matrix4f.mul(clipmapTransform, nodeTransform, m_ShaderData.unity_ObjectToWorld);

            m_ShadingShader.enable(m_ShaderData);
            m_Clipmap.drawNode(i);
        }

        if(wireframe){
            gl.glPolygonMode(GLenum.GL_FRONT_AND_BACK, GLenum.GL_FILL);
        }

        CacheBuffer.free(clipmapTransform);
        CacheBuffer.free(nodeTransform);

//        if(frameCount == 300)
        m_ShadingShader.printOnce();

        frameCount++;
    }

    private int frameCount = 0;
}

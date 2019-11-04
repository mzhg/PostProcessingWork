package jet.opengl.demos.nvidia.waves.crest;

import org.lwjgl.util.vector.Matrix4f;

import jet.opengl.demos.nvidia.waves.ocean.Technique;
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

        m_ShadingShader = ShaderManager.getInstance().getWaveRender(m_Desc);
        if(wireframe){
            m_ShadingShader.getRaster().fillMode = GLenum.GL_LINE;
        }

        final Matrix4f clipmapTransform = CacheBuffer.getCachedMatrix();
        final Matrix4f nodeTransform = CacheBuffer.getCachedMatrix();

        Matrix4f.mul(cameraProj, cameraView, m_ShaderData.UNITY_MATRIX_VP);

        int nodeCount = m_Clipmap.getNodeCount();
        for(int i = 0; i < nodeCount; i++){

        }
    }
}

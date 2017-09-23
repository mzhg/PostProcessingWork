package jet.opengl.demos.intel.antialiasing;

import com.nvidia.developer.opengl.models.GLVAO;

import org.lwjgl.util.vector.Vector4f;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.Texture2D;

/**
 * Created by mazhen'gui on 2017/9/23.
 */

final class SMAAEffect implements Disposeable{
    private float					m_ScreenWidth;
    private float					m_ScreenHeight;

    private GLSLProgram         m_SMAAEdgeDetectionProgram;
    private GLSLProgram         m_SMAABlendingWeightCalculationProgram;
    private GLSLProgram         m_SMAANeighborhoodBlendingProgram;

    private BufferGL            m_constantsBuffer;

    private Texture2D m_depthStencilTex;
    private Texture2D    m_depthStencilTexDSV;

    private Texture2D            m_workingColorTexture;
    private Texture2D     m_workingColorTextureRTV;
    private Texture2D   m_workingColorTextureSRV;

    private Texture2D           m_edgesTex;
    private Texture2D    m_edgesTexRTV;
    private Texture2D   m_edgesTexSRV;

    private Texture2D            m_blendTex;
    private Texture2D     m_blendTexRTV;
    private Texture2D   m_blendTexSRV;

    //ID3D11Texture2D*            m_areaTex;
    private Texture2D   m_areaTexSRV;

    //ID3D11Texture2D*            m_searchTex;
    private Texture2D   m_searchTexSRV;

//    ID3D11SamplerState*         m_samplerState;
//    class SMAAEffect_Quad *     m_quad;
    private GLVAO  m_quad;

    SMAAEffect(){

    }

    void OnCreate(/*ID3D11Device* pD3dDevice, ID3D11DeviceContext* pContext, IDXGISwapChain* pSwapChain*/){

    }

    void OnSize(/*ID3D11Device* pD3DDevice,*/ int width, int height){

    }

    void    OnShutdown(){

    }

    void    Draw(/*ID3D11DeviceContext* pD3DImmediateContext,*/ float PPAADEMO_gEdgeDetectionThreshold, Texture2D sourceColorSRV_SRGB,
                 Texture2D sourceColorSRV_UNORM, boolean showEdges ){

    }

    @Override
    public void dispose() {

    }

    public static class SMAAConstants{
        static final int SIZE = Vector4f.SIZE * 2;
        final float[] c_PixelSize = new float[2];
        final float[] c_Dummy = new float[2];

        // This is only required for temporal modes (SMAA T2x).
        float[] c_SubsampleIndices = new float[4];

        //float threshld;
        //float maxSearchSteps;
        //float maxSearchStepsDiag;
        //float cornerRounding;
    }
}

package jet.opengl.demos.nvidia.waves.samples;

import org.lwjgl.util.vector.Matrix4f;

import java.io.IOException;
import java.nio.IntBuffer;

import jet.opengl.demos.nvidia.waves.wavework.GFSDK_WaveWorks;
import jet.opengl.demos.nvidia.waves.wavework.GFSDK_WaveWorks_Quadtree;
import jet.opengl.demos.nvidia.waves.wavework.GFSDK_WaveWorks_Quadtree_Params;
import jet.opengl.demos.nvidia.waves.wavework.GFSDK_WaveWorks_Quadtree_Stats;
import jet.opengl.demos.nvidia.waves.wavework.GFSDK_WaveWorks_Savestate;
import jet.opengl.demos.nvidia.waves.wavework.GFSDK_WaveWorks_Simulation;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.postprocessing.util.NvImage;

/**
 * Created by mazhen'gui on 2017/8/19.
 */

final class OceanSurfaceTest {

    OceanSurfaceParameters m_params;
    // Color look up 1D texture
    int m_pBicolorMap;			// (RGBA8)
    int m_pCubeMap;
    Texture2D m_pFoamIntensityMap;
    Texture2D m_pFoamDiffuseMap;

    //    ID3DX11Effect* m_pOceanFX;
//    ID3DX11EffectTechnique* m_pRenderSurfaceTechnique;
    OceanSurfaceShadedProgram m_pRenderSurfaceShadedPass;
    private GLFuncProvider gl;
    private boolean m_printOnce;

    void init(OceanSurfaceParameters params){
        m_params = params;
        gl = GLFuncProviderFactory.getGLFuncProvider();
        if(m_pRenderSurfaceShadedPass == null){
            m_pRenderSurfaceShadedPass = new OceanSurfaceShadedProgram(true);
        }

        loadTextures("nvidia/WaveWorks/textures/");
    }

    // create color/fresnel lookup table.
    void createFresnelMap(){
        final int g_SkyBlending = 16;
        final int FRESNEL_TEX_SIZE = 256;
        IntBuffer buffer = CacheBuffer.getCachedIntBuffer(FRESNEL_TEX_SIZE);
        for(int i = 0; i < FRESNEL_TEX_SIZE; i++){
            float cos_a = (float)i / FRESNEL_TEX_SIZE;
            // Using water's refraction index 1.33
            int fresnel = (int)(Numeric.fresnelTerm(cos_a, 1.33f) * 255);  // TODO

            int sky_blend = (int)(Math.pow(1.0 / (1 + cos_a), g_SkyBlending) * 255);

            buffer.put((sky_blend << 8) | fresnel);
        }
        buffer.flip();

        m_pBicolorMap = gl.glGenTexture();
        gl.glBindTexture(GLenum.GL_TEXTURE_1D, m_pBicolorMap);
        gl.glTexImage1D(GLenum.GL_TEXTURE_1D, 0, GLenum.GL_RGBA8, FRESNEL_TEX_SIZE, 0, GLenum.GL_RGBA, GLenum.GL_UNSIGNED_BYTE, buffer);
//		GL11.glTexParameteri(GL11.GL_TEXTURE_1D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
//		GL11.glTexParameteri(GL11.GL_TEXTURE_1D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
//		GL11.glTexParameteri(GL11.GL_TEXTURE_1D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        gl.glBindTexture(GLenum.GL_TEXTURE_1D, 0);
    }

    // ---------------------------------- GPU shading data ------------------------------------

    // D3D objects
//    ID3D11Device* m_pd3dDevice;
//    ID3D11InputLayout* m_pQuadLayout;


//    ID3DX11EffectPass* m_pRenderSurfaceWireframePass;
//    ID3DX11EffectMatrixVariable* m_pRenderSurfaceMatViewProjVariable;
//    ID3DX11EffectVectorVariable* m_pRenderSurfaceSkyColorVariable;
//    ID3DX11EffectVectorVariable* m_pRenderSurfaceWaterColorVariable;
//    ID3DX11EffectVectorVariable* m_pRenderSurfacePatchColorVariable;
//    ID3DX11EffectShaderResourceVariable* m_pRenderSurfaceColorMapVariable;
//    ID3DX11EffectShaderResourceVariable* m_pRenderSurfaceCubeMapVariable;
//    ID3DX11EffectShaderResourceVariable* m_pRenderSurfaceFoamIntensityMapVariable;
//    ID3DX11EffectShaderResourceVariable* m_pRenderSurfaceFoamDiffuseMapVariable;

    // --------------------------------- Rendering routines -----------------------------------

    // Rendering
    void renderShaded(IsParameters params, GFSDK_WaveWorks_Simulation hSim, GFSDK_WaveWorks_Savestate hSavestate, boolean freeze_cam){
        m_pRenderSurfaceShadedPass.enable();
        m_pRenderSurfaceShadedPass.setUniforms(params);

        gl.glActiveTexture(GLenum.GL_TEXTURE15);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D, 0);

        gl.glActiveTexture(GLenum.GL_TEXTURE14);
        gl.glBindTexture(m_pFoamDiffuseMap.getTarget(), m_pFoamDiffuseMap.getTexture());
        gl.glBindSampler(14, IsSamplers.g_SamplerLinearMipmapWrap);

        gl.glActiveTexture(GLenum.GL_TEXTURE13);
        gl.glBindTexture(m_pFoamIntensityMap.getTarget(), m_pFoamIntensityMap.getTexture());
        gl.glBindSampler(13, IsSamplers.g_SamplerLinearMipmapWrap);

        gl.glActiveTexture(GLenum.GL_TEXTURE8);
        gl.glBindTexture(GLenum.GL_TEXTURE_1D, m_pBicolorMap);
        gl.glBindSampler(8, OceanSamplers.g_pFresnelSampler);


        gl.glActiveTexture(GLenum.GL_TEXTURE9);
        gl.glBindTexture(GLenum.GL_TEXTURE_CUBE_MAP, m_pCubeMap);
        gl.glBindSampler(9, OceanSamplers.g_pCubeSampler);

        params.g_Wireframe = false;
        if(params.g_Wireframe){
            gl.glPolygonMode(GLenum.GL_FRONT_AND_BACK, GLenum.GL_LINE);
        }

        GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_SetRenderStateD3D11(hSim, params.g_ModelViewMatrix, null, hSavestate);
        GFSDK_WaveWorks.GFSDK_WaveWorks_Quadtree_DrawD3D11(m_hOceanQuadTree, params.g_ModelViewMatrix, params.g_Projection, null, hSavestate);

        if(params.g_Wireframe) {
            gl.glPolygonMode(GLenum.GL_FRONT_AND_BACK, GLenum.GL_FILL);
        }

        gl.glActiveTexture(GLenum.GL_TEXTURE8);
        gl.glBindTexture(GLenum.GL_TEXTURE_1D, 0);


        gl.glActiveTexture(GLenum.GL_TEXTURE9);
        gl.glBindTexture(GLenum.GL_TEXTURE_CUBE_MAP, 0);

        if(!m_printOnce){
            m_pRenderSurfaceShadedPass.setName("Render Ocean Surface");
            m_pRenderSurfaceShadedPass.printPrograminfo();
            m_printOnce = true;
        }
    }

    void renderWireframe(Matrix4f matView, Matrix4f matProj, GFSDK_WaveWorks_Simulation hSim, GFSDK_WaveWorks_Savestate hSavestate, boolean freeze_cam){

    }

    void getQuadTreeStats(GFSDK_WaveWorks_Quadtree_Stats stats){
        GFSDK_WaveWorks.GFSDK_WaveWorks_Quadtree_GetStats(m_hOceanQuadTree, stats);
    }

    // --------------------------------- Surface geometry -----------------------------------
    GFSDK_WaveWorks_Quadtree m_hOceanQuadTree;
    void initQuadTree(GFSDK_WaveWorks_Quadtree_Params params){
        if(null == m_hOceanQuadTree)
        {
            m_hOceanQuadTree = GFSDK_WaveWorks.GFSDK_WaveWorks_Quadtree_CreateD3D11(params);
        }
        else
        {
            GFSDK_WaveWorks.GFSDK_WaveWorks_Quadtree_UpdateParams(m_hOceanQuadTree, params);
        }
    }

//    UINT* m_pQuadTreeShaderInputMappings_Shaded;
//    UINT* m_pQuadTreeShaderInputMappings_Wireframe;
//
//    UINT* m_pSimulationShaderInputMappings_Shaded;
//    UINT* m_pSimulationShaderInputMappings_Wireframe;

    private void loadTextures(String prefix){
        try {
            int foam_intensity  = NvImage.uploadTextureFromDDSFile(prefix + "foam_intensity_perlin2.dds");
            int foam = NvImage.uploadTextureFromDDSFile(prefix + "foam24bit.dds");
            m_pCubeMap = NvImage.uploadTextureFromDDSFile(prefix + "reflect_cube.dds");
            m_pFoamIntensityMap = TextureUtils.createTexture2D(GLenum.GL_TEXTURE_2D, foam_intensity);
            m_pFoamDiffuseMap = TextureUtils.createTexture2D(GLenum.GL_TEXTURE_2D, foam);

        } catch (IOException e) {
            e.printStackTrace();
        }
//
//        gl.glBindTexture(GLenum.GL_TEXTURE_2D, 0);
//        gl.glBindTexture(GLenum.GL_TEXTURE_CUBE_MAP, 0);

        createFresnelMap();
    }
}

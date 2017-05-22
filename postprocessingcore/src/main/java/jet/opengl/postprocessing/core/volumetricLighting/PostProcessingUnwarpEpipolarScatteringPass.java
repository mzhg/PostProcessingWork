package jet.opengl.postprocessing.core.volumetricLighting;

import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.PostProcessingParameters;
import jet.opengl.postprocessing.core.PostProcessingRenderContext;
import jet.opengl.postprocessing.core.PostProcessingRenderPass;
import jet.opengl.postprocessing.core.PostProcessingRenderPassOutputTarget;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;

/**
 * Input0: cam space z<br>
 * Input1: scene color texture<br>
 * Input2: epipolar camera space z<br>
 * Input3: scattered color<br>
 * Input4: slice end points<br>
 * Created by mazhen'gui on 2017-05-19 12:09:09.
 */

final class PostProcessingUnwarpEpipolarScatteringPass extends PostProcessingRenderPass{

    private VolumetricLightingProgram g_UnwarpEpipolarSctrImgTechProgram = null;
    private SharedData m_sharedData;
    private final Texture2D[] m_RenderTargets;

    public PostProcessingUnwarpEpipolarScatteringPass(SharedData sharedData, Texture2D colorTexture, Texture2D depthStencilTexture) {
        super("UnwarpEpipolarScattering");

        m_sharedData = sharedData;
//        Uniform [sampler2D name=g_tex2DCamSpaceZ, location=10, value = 1]
//        Uniform [sampler2D name=g_tex2DColorBuffer, location=11, value = 11]
//        Uniform [sampler2D name=g_tex2DEpipolarCamSpaceZ, location=12, value = 4]
//        Uniform [sampler2D name=g_tex2DScatteredColor, location=13, value = 12]
//        Uniform [sampler2D name=g_tex2DSliceEndPoints, location=14, value = 2]
        // outputCount 1 or 2.
        set(5, 1);

        if(colorTexture != null){
            setOutputTarget(PostProcessingRenderPassOutputTarget.INTERNAL);
        }

        if(depthStencilTexture != null || colorTexture != null){
            m_RenderTargets = new Texture2D[2];
            m_RenderTargets[0] = colorTexture;
            m_RenderTargets[1] = depthStencilTexture;
        }else{
            m_RenderTargets = null;
        }
    }

    @Override
    public void process(PostProcessingRenderContext context, PostProcessingParameters parameters) {
        if(g_UnwarpEpipolarSctrImgTechProgram == null /*|| g_GenerateSliceEndpointsProgram.getProgram() == 0*/){
            g_UnwarpEpipolarSctrImgTechProgram = m_sharedData.getUnwarpEpipolarSctrImgTechProgram();
        }

        Texture2D output = getOutputTexture(0);

        output.setName("UnwarpEpipolarScatteringTexture");
        context.setViewport(0,0, output.getWidth(), output.getHeight());
        context.setVAO(null);
        context.setProgram(g_UnwarpEpipolarSctrImgTechProgram);
        m_sharedData.setUniforms(g_UnwarpEpipolarSctrImgTechProgram);

        Texture2D tex2DCamSpaceZ = getInput(0);
        Texture2D tex2DColorBuffer = getInput(1);
        Texture2D tex2DEpipolarCamSpaceZ = getInput(2);
        Texture2D tex2DScatteredColor = getInput(3);
        Texture2D tex2DSliceEndPoints = getInput(4);

//        m_ptex2DCameraSpaceZSRV.bind(RenderTechnique.TEX2D_CAM_SPACEZ, m_psamLinearClamp);
//        frameAttribs.ptex2DSrcColorBufferSRV.bind(RenderTechnique.TEX2D_COLOR_BUFFER, m_psamLinearClamp);
//        m_ptex2DEpipolarCamSpaceZSRV.bind(RenderTechnique.TEX2D_EPIPOLAR_CAM, m_psamLinearClamp);
//        m_ptex2DScatteredLightSRV.bind(RenderTechnique.TEX2D_SCATTERED_COLOR, m_psamLinearClamp);
//        m_ptex2DSliceEndpointsSRV.bind(RenderTechnique.TEX2D_SLICE_END_POINTS, m_psamLinearClamp);
        int m_psamLinearClamp = m_sharedData.m_psamLinearClamp;
        context.bindTexture(tex2DCamSpaceZ, VolumetricLightingProgram.TEX2D_CAM_SPACEZ, m_psamLinearClamp);
        context.bindTexture(tex2DColorBuffer, VolumetricLightingProgram.TEX2D_COLOR_BUFFER, m_psamLinearClamp);
        context.bindTexture(tex2DEpipolarCamSpaceZ, VolumetricLightingProgram.TEX2D_EPIPOLAR_CAM, m_psamLinearClamp);
        context.bindTexture(tex2DScatteredColor, VolumetricLightingProgram.TEX2D_SCATTERED_COLOR, m_psamLinearClamp);
        context.bindTexture(tex2DSliceEndPoints, VolumetricLightingProgram.TEX2D_SLICE_END_POINTS, m_psamLinearClamp);

        context.setBlendState(null);
        context.setDepthStencilState(m_sharedData.m_pDisableDepthTestIncrStencilDS);
        context.setRasterizerState(null);
        if(m_RenderTargets != null){
            if(m_RenderTargets[0] == null)
                m_RenderTargets[0] = output;
            context.setRenderTargets(m_RenderTargets);
        }else{
            context.setRenderTarget(output);
        }

        GLFuncProviderFactory.getGLFuncProvider().glClearBufferfi(GLenum.GL_DEPTH_STENCIL, 0, 0.0f, 0);

        context.drawFullscreenQuad();
    }

    @Override
    public Texture2D getOutputTexture(int idx) {
        if(getOutputTarget() == PostProcessingRenderPassOutputTarget.INTERNAL){
            return idx == 0 ? m_RenderTargets[0] : null;
        }else {
            return super.getOutputTexture(idx);
        }
    }

    @Override
    public void computeOutDesc(int index, Texture2DDesc out) {
        out.arraySize = 1;
        out.sampleCount = 1;
        if(index == 0){
            out.format = GLenum.GL_RGBA8;
        }else{  // index == 1
            out.format = GLenum.GL_DEPTH24_STENCIL8;
        }

        super.computeOutDesc(index, out);
    }
}

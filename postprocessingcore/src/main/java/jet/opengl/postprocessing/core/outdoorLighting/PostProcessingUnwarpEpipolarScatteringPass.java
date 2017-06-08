package jet.opengl.postprocessing.core.outdoorLighting;

import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.PostProcessingParameters;
import jet.opengl.postprocessing.core.PostProcessingRenderContext;
import jet.opengl.postprocessing.core.PostProcessingRenderPass;
import jet.opengl.postprocessing.core.PostProcessingRenderPassOutputTarget;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureUtils;

/**
 * Created by mazhen'gui on 2017/6/5.
 */

final class PostProcessingUnwarpEpipolarScatteringPass extends PostProcessingRenderPass{

    static final int sm_iLowResLuminanceMips = 7;

    private SharedData m_sharedData;
    private final boolean m_bRenderLuminance;
    private Texture2D m_ptex2DLowResLuminance;
    private Texture2D[] m_RenderTargets;

    private RenderTechnique m_UnwarpEpipolarSctrImgProgram;
    private RenderTechnique m_UnwarpAndRenderLuminanceTech;

    public PostProcessingUnwarpEpipolarScatteringPass(SharedData sharedData, boolean bRenderLuminance) {
        super("UnwarpEpipolarScattering");

        m_sharedData = sharedData;
        m_bRenderLuminance = true;

//        Uniform [sampler2D name=g_tex2DCamSpaceZ, location=8, value = 1]              :0
//        Uniform [sampler2D name=g_tex2DColorBuffer, location=9, value = 10]           :1
//        Uniform [sampler2D name=g_tex2DEpipolarCamSpaceZ, location=10, value = 4]     :2
//        Uniform [sampler2D name=g_tex2DEpipolarExtinction, location=11, value = 13]   :3
//        Uniform [sampler2D name=g_tex2DScatteredColor, location=12, value = 11]       :4
//        Uniform [sampler2D name=g_tex2DSliceEndPoints, location=13, value = 2         :5
        set(6, 1);
        setOutputTarget(PostProcessingRenderPassOutputTarget.INTERNAL);
    }

    public PostProcessingUnwarpEpipolarScatteringPass(SharedData sharedData, Texture2D colorTexture, Texture2D depthStencilTexture, boolean bAutoExposure) {
        super("UnwarpEpipolarScattering");

        m_sharedData = sharedData;
        m_bRenderLuminance = false;

//        Uniform [sampler2D name=g_tex2DCamSpaceZ, location=8, value = 1]              :0
//        Uniform [sampler2D name=g_tex2DColorBuffer, location=9, value = 10]           :1
//        Uniform [sampler2D name=g_tex2DEpipolarCamSpaceZ, location=10, value = 4]     :2
//        Uniform [sampler2D name=g_tex2DEpipolarExtinction, location=11, value = 13]   :3
//        Uniform [sampler2D name=g_tex2DScatteredColor, location=12, value = 11]       :4
//        Uniform [sampler2D name=g_tex2DSliceEndPoints, location=13, value = 2         :5
//        Uniform [sampler2D name=g_tex2DAverageLuminance, location=7, value = 20]      :6[optionl]
        int inputCount = bAutoExposure ? 7 : 6;
        set(inputCount, 1);

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
        if(m_bRenderLuminance){
            m_UnwarpAndRenderLuminanceTech = m_sharedData.getUnwarpAndRenderLuminanceProgram();
            context.setProgram(m_UnwarpAndRenderLuminanceTech);
            m_sharedData.setUniforms(m_UnwarpAndRenderLuminanceTech);

            if(m_ptex2DLowResLuminance == null){
                Texture2DDesc LowResLuminanceTexDesc = new Texture2DDesc
                (
                        1 << (sm_iLowResLuminanceMips-1),   //UINT Width;
                        1 << (sm_iLowResLuminanceMips-1),   //UINT Height;
                        sm_iLowResLuminanceMips,            //UINT MipLevels;
                        1,                                  //UINT ArraySize;
                        GLenum.GL_R16F,              			//DXGI_FORMAT Format;
                        1                              	//DXGI_SAMPLE_DESC SampleDesc;
                );

                m_ptex2DLowResLuminance = TextureUtils.createTexture2D(LowResLuminanceTexDesc, null);
            }
        }else{
            m_UnwarpEpipolarSctrImgProgram = m_sharedData.getUnwarpEpipolarSctrImgProgram();
            context.setProgram(m_UnwarpEpipolarSctrImgProgram);
            m_sharedData.setUniforms(m_UnwarpEpipolarSctrImgProgram);
        }

        Texture2D output = getOutputTexture(0);

        context.setViewport(0,0, output.getWidth(), output.getHeight());
        context.setVAO(null);
//        Uniform [sampler2D name=g_tex2DCamSpaceZ, location=8, value = 1]              :0
//        Uniform [sampler2D name=g_tex2DColorBuffer, location=9, value = 10]           :1
//        Uniform [sampler2D name=g_tex2DEpipolarCamSpaceZ, location=10, value = 4]     :2
//        Uniform [sampler2D name=g_tex2DEpipolarExtinction, location=11, value = 13]   :3
//        Uniform [sampler2D name=g_tex2DScatteredColor, location=12, value = 11]       :4
//        Uniform [sampler2D name=g_tex2DSliceEndPoints, location=13, value = 2         :5
//        Uniform [sampler2D name=g_tex2DAverageLuminance, location=7, value = 20]      :6[optionl]

//        m_ptex2DCameraSpaceZ.bind(ScatteringRenderTechnique.TEX2D_CAM_SPACE, m_psamLinearClamp);
//        FrameAttribs.ptex2DSrcColorBuffer.bind(ScatteringRenderTechnique.TEX2D_COLOR, m_psamLinearClamp);
//        m_ptex2DEpipolarCamSpaceZ.bind(ScatteringRenderTechnique.TEX2D_EPIPOLAR_CAM_SPACE, m_psamLinearClamp);
//        m_ptex2DEpipolarInscattering.bind(ScatteringRenderTechnique.TEX2D_SCATTERED_COLOR, m_psamLinearClamp);
//        m_ptex2DSliceEndpoints.bind(ScatteringRenderTechnique.TEX2D_SLICE_END_POINTS, m_psamLinearClamp);
//        m_ptex2DEpipolarExtinction.bind(ScatteringRenderTechnique.TEX2D_EPIPOLAR_EXTINCTION, m_psamLinearClamp);
//        if(m_ptex2DAverageLuminance != null)
//            m_ptex2DAverageLuminance.bind(ScatteringRenderTechnique.TEX2D_AVERAGE_LUMINACE, m_psamLinearClamp);
        Texture2D ptex2DCameraSpaceZSRV = getInput(0);
        Texture2D ptex2DSrcColorBufferSRV = getInput(1);
        Texture2D ptex2DEpipolarCamSpaceZ = getInput(2);
        Texture2D ptex2DEpipolarExtinction = getInput(3);
        Texture2D ptex2DEpipolarInscattering = getInput(4);
        Texture2D ptex2DSliceEndpoints = getInput(5);
        Texture2D ptex2DAverageLuminance = getInput(6);


        int m_psamLinearClamp = m_sharedData.m_psamLinearClamp;
        context.bindTexture(ptex2DCameraSpaceZSRV, RenderTechnique.TEX2D_CAM_SPACE, m_psamLinearClamp);
        context.bindTexture(ptex2DSrcColorBufferSRV, RenderTechnique.TEX2D_COLOR, m_psamLinearClamp);
        context.bindTexture(ptex2DEpipolarCamSpaceZ, RenderTechnique.TEX2D_EPIPOLAR_CAM_SPACE, m_psamLinearClamp);
        context.bindTexture(ptex2DEpipolarExtinction, RenderTechnique.TEX2D_EPIPOLAR_EXTINCTION, m_psamLinearClamp);
        context.bindTexture(ptex2DEpipolarInscattering, RenderTechnique.TEX2D_SCATTERED_COLOR, m_psamLinearClamp);
        context.bindTexture(ptex2DSliceEndpoints, RenderTechnique.TEX2D_SLICE_END_POINTS, m_psamLinearClamp);
        if(ptex2DAverageLuminance != null)
            context.bindTexture(ptex2DAverageLuminance, RenderTechnique.TEX2D_AVERAGE_LUMINACE, m_psamLinearClamp);

        context.setBlendState(null);
        context.setRasterizerState(null);
        if(m_RenderTargets != null){
            if(m_RenderTargets[0] == null)
                m_RenderTargets[0] = output;
            context.setRenderTargets(m_RenderTargets);
        }else{
            context.setRenderTarget(output);
        }

        if(m_bRenderLuminance || m_RenderTargets == null) {
            context.setDepthStencilState(null);
        }else{
            context.setDepthStencilState(m_sharedData.m_pDisableDepthTestIncrStencilDS);
        }
        GLFuncProviderFactory.getGLFuncProvider().glClearBufferfi(GLenum.GL_DEPTH_STENCIL, 0, 0.0f, 0);

        context.drawFullscreenQuad();
//        context.bindTexture(null, RenderTechnique.TEX2D_CAM_SPACE, 0);
//        context.bindTexture(null, RenderTechnique.TEX2D_COLOR, 0);
//        context.bindTexture(null, RenderTechnique.TEX2D_EPIPOLAR_CAM_SPACE, 0);
//        context.bindTexture(null, RenderTechnique.TEX2D_EPIPOLAR_EXTINCTION, 0);
//        context.bindTexture(null, RenderTechnique.TEX2D_SCATTERED_COLOR, 0);
//        context.bindTexture(null, RenderTechnique.TEX2D_SLICE_END_POINTS, 0);

        if(m_bRenderLuminance){
//            context.bindTexture(output, 1, 0);  // generate mipmap
        }
    }

    @Override
    public void dispose() {
        if(m_ptex2DLowResLuminance != null){
            m_ptex2DLowResLuminance.dispose();
            m_ptex2DLowResLuminance = null;
        }
    }

    @Override
    public Texture2D getOutputTexture(int idx) {
        if(getOutputTarget() == PostProcessingRenderPassOutputTarget.INTERNAL) {
            if (idx == 0) {
                return m_bRenderLuminance ? m_ptex2DLowResLuminance : m_RenderTargets[0];
            }
        }else{
            return super.getOutputTexture(idx);
        }

        return null;
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

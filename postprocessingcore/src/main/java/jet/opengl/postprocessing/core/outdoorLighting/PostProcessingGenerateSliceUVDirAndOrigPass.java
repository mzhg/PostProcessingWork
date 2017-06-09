package jet.opengl.postprocessing.core.outdoorLighting;

import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.PostProcessingParameters;
import jet.opengl.postprocessing.core.PostProcessingRenderContext;
import jet.opengl.postprocessing.core.PostProcessingRenderPass;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;

/**
 * Created by mazhen'gui on 2017/6/3.
 */

final class PostProcessingGenerateSliceUVDirAndOrigPass extends PostProcessingRenderPass{

    private RenderTechnique m_RenderSliceUVDirInSMProgram;
    private SharedData m_sharedData;
    public PostProcessingGenerateSliceUVDirAndOrigPass(SharedData sharedData) {
        super("GenerateSliceUVDirAndOrig");

        m_sharedData = sharedData;
        set(1,1);
    }

    @Override
    public void process(PostProcessingRenderContext context, PostProcessingParameters parameters) {
        if(m_RenderSliceUVDirInSMProgram == null /*|| g_GenerateSliceEndpointsProgram.getProgram() == 0*/){
            m_RenderSliceUVDirInSMProgram = m_sharedData.getRenderSliceUVDirInSMProgram();
        }

        Texture2D output = getOutputTexture(0);
        output.setName("SliceUVDirAndOrigTexture");
        context.setViewport(0,m_sharedData.m_ScatteringInitAttribs.m_iFirstCascade, output.getWidth(), output.getHeight() - m_sharedData.m_ScatteringInitAttribs.m_iFirstCascade);
        context.setVAO(null);
        context.setProgram(m_RenderSliceUVDirInSMProgram);
        m_sharedData.setUniforms(m_RenderSliceUVDirInSMProgram);

        context.bindTexture(getInput(0), RenderTechnique.TEX2D_SLICE_END_POINTS, 0);  // g_tex2DSliceEndPoints

        context.setBlendState(null);
        context.setDepthStencilState(null);
        context.setRasterizerState(null);
        context.setRenderTarget(output);

        context.drawFullscreenQuad();

        if(m_sharedData.m_CommonFrameAttribs.outputCurrentFrameLog){
            SharedData.saveTextureAsText(output, "SliceUVDirAndOriginDX.txt");
        }
    }

    @Override
    public void computeOutDesc(int index, Texture2DDesc out) {
        out.arraySize = 1;
        out.sampleCount = 1;
        out.mipLevels = 1;
        out.width = m_sharedData.m_ScatteringInitAttribs.m_uiNumEpipolarSlices;
        out.height = m_sharedData.m_CommonFrameAttribs.cascadeShadowMapAttribs.numCascades;
        out.format = GLenum.GL_RGBA32F;  // 16FP or 32FP
    }
}

package jet.opengl.postprocessing.core.outdoorLighting;

import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.PostProcessingParameters;
import jet.opengl.postprocessing.core.PostProcessingRenderContext;
import jet.opengl.postprocessing.core.PostProcessingRenderPass;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;

/**
 * Created by mazhen'gui on 2017/5/17.
 */

final class PostProcessingGenerateSliceEndpointsPass extends PostProcessingRenderPass{

    private RenderTechnique g_GenerateSliceEndpointsProgram = null;
    private SharedData m_sharedData;

    public PostProcessingGenerateSliceEndpointsPass(SharedData sharedData) {
        super("GenerateSliceEndpoints");

        m_sharedData = sharedData;

        // no inputs.
        set(0, 1);
    }

    @Override
    public void process(PostProcessingRenderContext context, PostProcessingParameters parameters) {
        if(g_GenerateSliceEndpointsProgram == null /*|| g_GenerateSliceEndpointsProgram.getProgram() == 0*/){
            g_GenerateSliceEndpointsProgram = m_sharedData.getRenderSliceEndpointsProgram();
        }

        Texture2D output = getOutputTexture(0);
        output.setName("SliceEndpointsTexture");
        context.setViewport(0,0, output.getWidth(), output.getHeight());
//        context.setViewport(0,0, 1280, 720);
        context.setVAO(null);
        context.setProgram(g_GenerateSliceEndpointsProgram);
        m_sharedData.setUniforms(g_GenerateSliceEndpointsProgram);

        context.setBlendState(null);
        context.setDepthStencilState(null);
        context.setRasterizerState(null);
        context.setRenderTarget(output);

        context.drawFullscreenQuad();
    }

    @Override
    public void computeOutDesc(int index, Texture2DDesc out) {
        out.arraySize = 1;
        out.sampleCount = 1;
//        out.width = m_sharedData.m_ScatteringAttribs.m_uiNumEpipolarSlices;
//        out.height = 1;
        out.format = GLenum.GL_RGBA16F;  // 16FP or 32FP

        super.computeOutDesc(index, out);
    }
}

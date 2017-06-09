package jet.opengl.postprocessing.core.outdoorLighting;

import java.nio.FloatBuffer;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.PostProcessingParameters;
import jet.opengl.postprocessing.core.PostProcessingRenderContext;
import jet.opengl.postprocessing.core.PostProcessingRenderPass;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/6/3.
 */

final class PostProcessingGenerateCoordinateTexturePass extends PostProcessingRenderPass{

    private RenderTechnique g_GenerateCoordinateTextureProgram = null;
    private SharedData m_sharedData;
    private final Texture2D[] m_RenderTargets = new Texture2D[3];

    public PostProcessingGenerateCoordinateTexturePass(SharedData sharedData) {
        super("GenerateCoordinateTexture");

        m_sharedData = sharedData;

        // input0:  CameraSpaceZ texture
        // input1:  SliceEndpoints texture.
        // output0: CoordianteTexture
        // output1: EpipolarCamSpaceZ
        set(2, 2);
    }

    @Override
    public void process(PostProcessingRenderContext context, PostProcessingParameters parameters) {
        if(g_GenerateCoordinateTextureProgram == null /*|| g_GenerateSliceEndpointsProgram.getProgram() == 0*/){
            g_GenerateCoordinateTextureProgram = m_sharedData.getRenderCoordinateTextureProgram();
        }

        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        Texture2D input0 = getInput(0);
        Texture2D input1 = getInput(1);

        Texture2D output0 = getOutputTexture(0);  // Coordinate Texture
        Texture2D output1 = getOutputTexture(1);  // Epipolar Came space Z.

        m_RenderTargets[0] = output0;
        m_RenderTargets[1] = output1;
        m_RenderTargets[2] = m_sharedData.getEpipolarImageDSV();

        // binding input textures.
        context.bindTexture(input0, RenderTechnique.TEX2D_CAM_SPACE, m_sharedData.m_psamLinearClamp);
        context.bindTexture(input1, RenderTechnique.TEX2D_SLICE_END_POINTS, m_sharedData.m_psamLinearClamp);

        output0.setName("CoordinateTexture");
        output1.setName("EpipolarCamespaceZTexture");
        context.setViewport(0,0, output0.getWidth(), output0.getHeight());
        context.setVAO(null);
        context.setProgram(g_GenerateCoordinateTextureProgram);
        m_sharedData.setUniforms(g_GenerateCoordinateTextureProgram);

        context.setBlendState(null);
        context.setDepthStencilState(m_sharedData.m_pDisableDepthTestIncrStencilDS);
//        gl.glEnable(GLenum.GL_STENCIL_TEST);
//        gl.glStencilFunc(GLenum.GL_ALWAYS, 0, 0xFF);
//        gl.glStencilOp(GLenum.GL_INCR, GLenum.GL_INCR, GLenum.GL_INCR);
//        gl.glStencilMask(0xFF);
        context.setRasterizerState(null);
        context.setRenderTargets(m_RenderTargets);

        // Clear depth stencil view. Since we use stencil part only, there is no need to clear depth
        // Set stencil value to 0

        gl.glClearBufferfi(GLenum.GL_DEPTH_STENCIL, 0, 0.0f, 0);

        final float fInvalidCoordinate = -1e+30f;
        FloatBuffer invalidCoords = CacheBuffer.wrap(fInvalidCoordinate, fInvalidCoordinate,fInvalidCoordinate,fInvalidCoordinate);
        // Clear both render targets with values that can't be correct projection space coordinates and camera space Z:
        gl.glClearBufferfv(GLenum.GL_COLOR, 0, invalidCoords);
        gl.glClearBufferfv(GLenum.GL_COLOR, 1, invalidCoords);

        context.drawFullscreenQuad();

        if(m_sharedData.m_CommonFrameAttribs.outputCurrentFrameLog){
            gl.glFlush();
            SharedData.saveTextureAsText(output0, "CoordianteTextureDX.txt");
            SharedData.saveTextureAsText(output1, "EpipolarCamSpaceZDX.txt");
            SharedData.saveTextureAsText(m_sharedData.getEpipolarImageDSV(), "EpipolarImage_RenderCoordinateDX.txt");
        }
    }

    @Override
    public void computeOutDesc(int index, Texture2DDesc out) {
        out.arraySize = 1;
        out.sampleCount = 1;
        out.mipLevels = 1;
        out.width = m_sharedData.m_ScatteringInitAttribs.m_uiMaxSamplesInSlice;
        out.height = m_sharedData.m_ScatteringInitAttribs.m_uiNumEpipolarSlices;
        if(index == 0)
            out.format = GLenum.GL_RG32F;  // 16FP or 32FP
        else
            out.format = GLenum.GL_R32F;
    }
}

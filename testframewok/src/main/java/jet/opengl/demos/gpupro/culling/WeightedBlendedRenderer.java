package jet.opengl.demos.gpupro.culling;

import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.RenderTargets;
import jet.opengl.postprocessing.texture.SamplerDesc;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;

final class WeightedBlendedRenderer extends TransparencyRenderer{
    private GLSLProgram m_shaderWeightedBlend;
    private GLSLProgram m_shaderWeightedFinal;

    private int m_pointSampler;

    private final Texture2D[] m_accumulationTexId = new Texture2D[2];
    private RenderTargets mFBO;

    @Override
    protected void onCreate() {
        super.onCreate();

        final String root = "gpupro/OIT/shaders/";
        m_shaderWeightedBlend = GLSLProgram.createProgram(root + "base_shade_vertex.vert", root + "weighted_blend.frag", null);
        m_shaderWeightedFinal = GLSLProgram.createProgram(root + "base_vertex.vert", root + "weighted_final.frag", null);

        mFBO = new RenderTargets();

        if(m_pointSampler == 0){
            SamplerDesc desc = new SamplerDesc();
            desc.minFilter = desc.magFilter = GLenum.GL_NEAREST;
            m_pointSampler = SamplerUtils.createSampler(desc);
        }
    }

    @Override
    protected void onResize(int width, int height) {
        m_accumulationTexId[0] = TextureUtils.resizeTexture2D(m_accumulationTexId[0], width, height, GLenum.GL_RGBA16F);
        m_accumulationTexId[1] = TextureUtils.resizeTexture2D(m_accumulationTexId[1], width, height, GLenum.GL_R8);
    }

    @Override
    OITType getType() {
        return OITType.WeightedBlend;
    }

    @Override
    void renderScene(Renderer sceneRender, Scene scene) {
        gl.glDisable(GLenum.GL_DEPTH_TEST);

        {
            // ---------------------------------------------------------------------
            // 1. Geometry pass
            // ---------------------------------------------------------------------
            /*glBindFramebuffer(GL_FRAMEBUFFER, m_accumulationFboId);
            const GLenum drawBuffers[] = { GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1 };
            glDrawBuffers(2, drawBuffers);*/

            mFBO.bind();
            mFBO.setRenderTextures(m_accumulationTexId, null);
            gl.glViewport(0,0, m_accumulationTexId[0].getWidth(), m_accumulationTexId[0].getHeight());

            // Render target 0 stores a sum (weighted RGBA colors). Clear it to 0.f.
            // Render target 1 stores a product (transmittances). Clear it to 1.f.
//            float clearColorZero[4] = { 0.f, 0.f, 0.f, 0.f };
//            float clearColorOne[4]  = { 1.f, 1.f, 1.f, 1.f };
            gl.glClearBufferfv(GLenum.GL_COLOR, 0, CacheBuffer.wrap(0.f, 0.f, 0.f, 0.f));
            gl.glClearBufferfv(GLenum.GL_COLOR, 1, CacheBuffer.wrap(1.f, 1.f, 1.f, 1.f));

            gl.glEnable(GLenum.GL_BLEND);
            gl.glBlendEquation(GLenum.GL_FUNC_ADD);
            gl.glBlendFunci(0, GLenum.GL_ONE, GLenum.GL_ONE);
            gl.glBlendFunci(1, GLenum.GL_ZERO, GLenum.GL_ONE_MINUS_SRC_COLOR);

            m_shaderWeightedBlend.enable();
//            m_shaderWeightedBlend->setUniform1f("uAlpha", m_opacity);
//            m_shaderWeightedBlend->setUniform1f("uDepthScale", m_weightParameter);
//            DrawModel(m_shaderWeightedBlend);  todo
            m_shaderWeightedBlend.disable();

            gl.glDisable(GLenum.GL_BLEND);
        }

        {
            // ---------------------------------------------------------------------
            // 2. Compositing pass
            // ---------------------------------------------------------------------

//            glBindFramebuffer(GL_FRAMEBUFFER, getMainFBO());
//            glDrawBuffer(GL_BACK);

            /*m_shaderWeightedFinal->enable();  todo
            m_shaderWeightedFinal->setUniform3f("uBackgroundColor", m_backgroundColor[0], m_backgroundColor[1], m_backgroundColor[2]);
            m_shaderWeightedFinal->bindTextureRect("ColorTex0", 0, m_accumulationTexId[0]);
            m_shaderWeightedFinal->bindTextureRect("ColorTex1", 1, m_accumulationTexId[1]);
            RenderFullscreenQuad(m_shaderWeightedFinal);
            m_shaderWeightedFinal.disable();*/
        }

    }

    @Override
    public void dispose() {

    }
}

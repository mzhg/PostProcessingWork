package jet.opengl.demos.gpupro.culling;

import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.GLSLUtil;
import jet.opengl.postprocessing.texture.RenderTargets;
import jet.opengl.postprocessing.texture.SamplerDesc;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.CommonUtil;

final class DeepPeelingRenderer extends TransparencyRenderer{
    private GLSLProgram m_shaderPeelingInit;
    private GLSLProgram m_shaderPeelingPeel;
    private GLSLProgram m_shaderPeelingBlend;
    private GLSLProgram m_shaderPeelingFinal;

    private RenderTargets mFBO;
    private final Texture2D[] m_frontDepthTexId = new Texture2D[2];
    private final Texture2D[] m_frontColorTexId = new Texture2D[2];
    private Texture2D m_frontColorBlenderTexId;

    private int m_queryId;

    private int m_pointSampler;
    private Renderer m_renderer;

    private static final int MAX_PEELED_LAYERS = 64;

    @Override
    protected void onCreate() {
        super.onCreate();

        final String root = "gpupro/OIT/shaders/";
        m_shaderPeelingInit = GLSLProgram.createProgram(root + "base_shade_vertex.vert", root + "front_peeling_init.frag", null);
        m_shaderPeelingPeel = GLSLProgram.createProgram(root+"base_shade_vertex.vert", root+"front_peeling_peel.frag", null);
        m_shaderPeelingBlend = GLSLProgram.createProgram(root+"base_vertex.vert", root+"front_peeling_blend.frag", null);
        m_shaderPeelingFinal = GLSLProgram.createProgram(root+"base_vertex.vert", root+"front_peeling_final.frag", null);

        mFBO = new RenderTargets();

        if(m_pointSampler == 0){
            SamplerDesc desc = new SamplerDesc();
            desc.minFilter = desc.magFilter = GLenum.GL_NEAREST;
            m_pointSampler = SamplerUtils.createSampler(desc);
        }

        m_queryId = gl.glGenQuery();
    }

    @Override
    protected void onResize(int width, int height) {
        m_frontColorTexId[0] = TextureUtils.resizeTexture2D(m_frontColorTexId[0], width, height, GLenum.GL_RGBA8);
        m_frontColorTexId[1] = TextureUtils.resizeTexture2D(m_frontColorTexId[1], width, height, GLenum.GL_RGBA8);

        m_frontDepthTexId[1] = TextureUtils.resizeTexture2D(m_frontDepthTexId[1], width, height, GLenum.GL_DEPTH_COMPONENT32F);
        m_frontDepthTexId[1] = TextureUtils.resizeTexture2D(m_frontDepthTexId[1], width, height, GLenum.GL_DEPTH_COMPONENT32F);

        m_frontColorBlenderTexId = TextureUtils.resizeTexture2D(m_frontColorBlenderTexId, width, height, GLenum.GL_RGBA8);
    }

    @Override
    void renderScene(Renderer sceneRender, Scene scene) {

        // ---------------------------------------------------------------------
        // 1. Peel the first layer
        // ---------------------------------------------------------------------
        mFBO.bind();
        mFBO.setRenderTextures(CommonUtil.toArray(m_frontColorBlenderTexId, m_frontDepthTexId[0]), null);
        gl.glViewport(0,0, m_frontColorBlenderTexId.getWidth(), m_frontColorBlenderTexId.getHeight());
        gl.glClearBufferfv(GLenum.GL_COLOR, 0, CacheBuffer.wrap(0.f, 0.f, 0.f, 1.f));
        gl.glClearBufferfv(GLenum.GL_DEPTH, 0, CacheBuffer.wrap(1.f));
        gl.glEnable(GLenum.GL_DEPTH_TEST);

        m_shaderPeelingInit.enable();
        renderMeshes(scene, m_shaderPeelingFinal);

        // ---------------------------------------------------------------------
        // 2. Depth Peeling + Blending
        // ---------------------------------------------------------------------
        for (int layer = 1; layer < MAX_PEELED_LAYERS; layer++)
        {
            // ---------------------------------------------------------------------
            // 2.2. Peel the next depth layer
            // ---------------------------------------------------------------------
            int currId = layer % 2;
            int prevId = 1 - currId;

            mFBO.setRenderTextures(CommonUtil.toArray(m_frontColorTexId[currId], m_frontDepthTexId[currId]), null);

            gl.glClearBufferfv(GLenum.GL_COLOR, 0, CacheBuffer.wrap(0.f, 0.f, 0.f, 1.f));
            gl.glClearBufferfv(GLenum.GL_DEPTH, 0, CacheBuffer.wrap(1.f));
            gl.glEnable(GLenum.GL_DEPTH_TEST);

            gl.glDisable(GLenum.GL_BLEND);

            gl.glBeginQuery(GLenum.GL_SAMPLES_PASSED, m_queryId);

            m_shaderPeelingPeel.enable();
//            m_shaderPeelingPeel->bindTextureRect("DepthTex", 0, m_frontDepthTexId[prevId]);
//            m_shaderPeelingPeel->setUniform1f("uAlpha", m_opacity);
            renderMeshes(scene, m_shaderPeelingPeel);
            m_shaderPeelingPeel.disable();

            gl.glEndQuery(GLenum.GL_SAMPLES_PASSED);

            // ---------------------------------------------------------------------
            // 2.2. Blend the current layer
            // ---------------------------------------------------------------------
//            glBindFramebuffer(GL_FRAMEBUFFER, m_frontColorBlenderFboId);
//            glDrawBuffer(GL_COLOR_ATTACHMENT0);

            mFBO.setRenderTextures(CommonUtil.toArray(m_frontColorBlenderTexId, m_frontDepthTexId[0]), null);
            gl.glViewport(0,0, m_frontColorBlenderTexId.getWidth(), m_frontColorBlenderTexId.getHeight());

            gl.glDisable(GLenum.GL_DEPTH_TEST);
            gl.glEnable(GLenum.GL_BLEND);

            // UNDER operator
            gl.glBlendEquation(GLenum.GL_FUNC_ADD);
            gl.glBlendFuncSeparate(GLenum.GL_DST_ALPHA, GLenum.GL_ONE,
                    GLenum.GL_ZERO, GLenum.GL_ONE_MINUS_SRC_ALPHA);

            m_shaderPeelingBlend.enable();
//            m_shaderPeelingBlend->bindTextureRect("TempTex", 0, m_frontColorTexId[currId]);  todo
//            RenderFullscreenQuad(m_shaderPeelingBlend);
            m_shaderPeelingBlend.disable();

            gl.glDisable(GLenum.GL_BLEND);

            int sample_count = gl.glGetQueryObjectuiv(m_queryId, GLenum.GL_QUERY_RESULT);
            if (sample_count == 0)
            {
                break;
            }
        }

        // ---------------------------------------------------------------------
        // 3. Compositing Pass
        // ---------------------------------------------------------------------

        m_renderer.setOutputRenderTaget();
        gl.glDisable(GLenum.GL_DEPTH_TEST);

        m_shaderPeelingFinal.enable();
//        m_shaderPeelingFinal.setUniform3f("uBackgroundColor",  todo
//                m_backgroundColor[0], m_backgroundColor[1], m_backgroundColor[2]);
//        m_shaderPeelingFinal.bindTextureRect("ColorTex", 0, m_frontColorBlenderTexId);
//        RenderFullscreenQuad(m_shaderPeelingFinal);
        m_shaderPeelingFinal.disable();
    }

    private void renderMeshes(Scene scene, GLSLProgram program){
        final int numMeshes = scene.mTransparencyMeshes.size();
        for(int meshIdx = 0; meshIdx < numMeshes; meshIdx++){
            Mesh mesh = scene.mExpandMeshes.get(meshIdx);
            if(scene.mExpandMeshVisible.get(meshIdx) /*&& mesh.frameNumber < mFrameNumber*/){
                Material material = scene.mMaterials.get(scene.mMeshMaterials.get(meshIdx));  // the material that the mesh related to
                Model model = scene.mModels.get(scene.mMeshModels.get(meshIdx));

                GLSLUtil.setFloat(program, "uAlpha", material.mColor.w);


                mesh.mVao.bind();
                mesh.mVao.draw(GLenum.GL_TRIANGLES);
                mesh.mVao.unbind();

//                mesh.frameNumber = mFrameNumber;
            }
        }
    }

    @Override
    final OITType getType() {
        return OITType.DeepPeeling;
    }

    @Override
    public void dispose() {

    }
}

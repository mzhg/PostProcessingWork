package jet.opengl.postprocessing.core.volumetricLighting;

import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.PostProcessingParameters;
import jet.opengl.postprocessing.core.PostProcessingRenderContext;
import jet.opengl.postprocessing.core.PostProcessingRenderPass;
import jet.opengl.postprocessing.core.PostProcessingRenderPassOutputTarget;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureUtils;

/**
 * The precomputed pass only invoke once.<p>
 * Created by mazhen'gui on 2017/5/18.
 */

final class PostProcessingPrecomputedPointLightInscatteringPass extends PostProcessingRenderPass{

    private VolumetricLightingProgram g_PrecomputePointLightInsctrTech = null;
    private SharedData m_sharedData;
    private boolean m_invoked = false;
    private Texture2D m_ptex2DPrecomputedPointLightInsctrSRV;

    public PostProcessingPrecomputedPointLightInscatteringPass(SharedData sharedData) {
        super("PrecomputedPointLightInscattering");

        m_sharedData = sharedData;

        // no inputs.
        set(0, 1);
        setOutputTarget(PostProcessingRenderPassOutputTarget.INTERNAL);
    }

    @Override
    public void process(PostProcessingRenderContext context, PostProcessingParameters parameters) {
        if(m_invoked)
            return;

        InscaterringIntegralEvalution m_uiInsctrIntglEvalMethod = m_sharedData.m_ScatteringInitAttribs.m_uiInsctrIntglEvalMethod;
        int format;
        // Must be 32Fp
        if( m_uiInsctrIntglEvalMethod == InscaterringIntegralEvalution.MY_LUT )
            format = GLenum.GL_RGBA32F;
        else if(m_uiInsctrIntglEvalMethod == InscaterringIntegralEvalution.SRNN05 )
            format = GLenum.GL_R32F;
        else
            throw new IllegalArgumentException("Invalid InscaterringIntegralEvalution value: " + m_uiInsctrIntglEvalMethod.name());
        Texture2DDesc desc = new Texture2DDesc(512, 512, format);
        m_ptex2DPrecomputedPointLightInsctrSRV = TextureUtils.createTexture2D(desc, null);
        g_PrecomputePointLightInsctrTech = new VolumetricLightingProgram("PrecomputePointLightInsctr.frag", m_sharedData.getMacros());

        Texture2D output = m_ptex2DPrecomputedPointLightInsctrSRV;
        output.setName("PrecomputedPointLightInscatteringTexture");
        context.setViewport(0,0, output.getWidth(), output.getHeight());
//        context.setViewport(0,0, 1280, 720);
        context.setVAO(null);
        context.setProgram(g_PrecomputePointLightInsctrTech);
        m_sharedData.setUniforms(g_PrecomputePointLightInsctrTech);

        context.setBlendState(null);
        context.setDepthStencilState(null);
        context.setRasterizerState(null);
        context.setRenderTarget(output);

        context.drawFullscreenQuad();
        g_PrecomputePointLightInsctrTech.dispose();  // release the program.
        m_invoked = true;
    }

    @Override
    public Texture2D getOutputTexture(int idx) {
        return idx == 0 ? m_ptex2DPrecomputedPointLightInsctrSRV : null;
    }

    @Override
    public void dispose() {
        if(m_ptex2DPrecomputedPointLightInsctrSRV !=null){
            m_ptex2DPrecomputedPointLightInsctrSRV.dispose();
            m_ptex2DPrecomputedPointLightInsctrSRV = null;
        }
    }
}

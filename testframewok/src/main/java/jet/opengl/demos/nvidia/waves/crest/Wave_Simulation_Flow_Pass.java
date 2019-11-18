package jet.opengl.demos.nvidia.waves.crest;

import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.util.CacheBuffer;

/** A persistent flow simulation that moves around with a displacement LOD. The input is fully combined water surface shape.*/
final class Wave_Simulation_Flow_Pass extends Wave_Simulation_Pass {
    public String SimName () { return "Flow"; }
    public int TextureFormat () { return GLenum.GL_RG16F; }

    boolean _targetsClear = false;

    private final String FLOW_KEYWORD = "_FLOW_ON";   // todo

    private static boolean g_PrintOnce;

    public void BuildCommandBuffer(float deltaTime)
    {
        super.BuildCommandBuffer(deltaTime);

        gl.glClearTexImage(_targets.getTexture(), 0, GLenum.GL_RG, GLenum.GL_FLOAT, null);

        // if there is nothing in the scene tagged up for depth rendering, and we have cleared the RTs, then we can early out
        if (m_Inputs.size() == 0 && _targetsClear)
        {
            return;
        }

        for (int lodIdx = m_Clipmap.m_LodTransform.LodCount() - 1; lodIdx >= 0; lodIdx--)
        {
            /*buf.SetRenderTarget(_targets, 0, CubemapFace.Unknown, lodIdx);
            buf.ClearRenderTarget(false, true, Color.black);
            buf.SetGlobalFloat(sp_LD_SliceIndex, lodIdx);
            SubmitDraws(lodIdx, buf);*/

            setRenderTarget(_targets, lodIdx);
            m_ShaderData._LD_SliceIndex = lodIdx;
            SubmitDraws(lodIdx);
        }

        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
        Wave_Simulation_Animation_Pass.saveTextur(_targets, "Flow.txt");


        // targets have now been cleared, we can early out next time around
        if (m_Inputs.size() == 0)
        {
            _targetsClear = true;
        }
    }

    @Override
    protected void applySampler(Wave_Simulation_ShaderData properties, boolean sourceLod, TextureGL applyData) {
        if(sourceLod){
            properties._LD_TexArray_Flow_Source = applyData;
        }else{
            properties._LD_TexArray_Flow = applyData;
        }
    }

    /* public static String TextureArrayName = "_LD_TexArray_Flow";
     private static LodDataMgr.TextureArrayParamIds textureArrayParamIds = new LodDataMgr.TextureArrayParamIds(TextureArrayName);
     public static int ParamIdSampler(boolean sourceLod *//*= false*//*) { return textureArrayParamIds.GetId(sourceLod); }*/
    public static void BindNull(Wave_Simulation_ShaderData properties, boolean sourceLod /*= false*/)
    {
//        properties.SetTexture(ParamIdSampler(sourceLod), TextureArrayHelpers.BlackTextureArray);
        if(sourceLod){
            properties._LD_TexArray_Flow_Source = null;
        }else{
            properties._LD_TexArray_Flow = null;
        }
    }
}

package jet.opengl.demos.nvidia.waves.crest;

import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.CacheBuffer;

/** A persistent flow simulation that moves around with a displacement LOD. The input is fully combined water surface shape.*/
final class Wave_Simulation_Flow_Pass extends Wave_Simulation_Pass {
    public String SimName () { return "Flow"; }
    public int TextureFormat () { return GLenum.GL_R16F; }

    boolean _targetsClear = false;

    private final String FLOW_KEYWORD = "_FLOW_ON";   // todo


    public void BuildCommandBuffer()
    {
        super.BuildCommandBuffer();

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
            gl.glClearBufferfv(GLenum.GL_COLOR, 0, CacheBuffer.wrap(0.f, 0.f, 0.f,0.f));
            m_ShaderData._LD_SliceIndex = lodIdx;
            SubmitDraws(lodIdx);
        }

        // targets have now been cleared, we can early out next time around
        if (m_Inputs.size() == 0)
        {
            _targetsClear = true;
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

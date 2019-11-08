package jet.opengl.demos.nvidia.waves.crest;

import org.lwjgl.util.vector.Vector3f;

import jet.opengl.postprocessing.buffer.BufferGL;

final class Wave_Query_Flow extends Wave_Query_Base {
    @Override
    protected String QueryShaderName() {
        return "QueryFlow";
    }

    @Override
    protected void bindInputsAndOutputs(Wave_Simulation_ShaderData wrapper, BufferGL resultsBuffer) {
        m_Simulation._lodDataFlow.BindResultData(wrapper);

        wrapper._LD_TexArray_Flow = m_Simulation._lodDataFlow.DataTexture();
        wrapper._ResultFlows = resultsBuffer;
    }

    public int query(int i_ownerHash, SamplingData i_samplingData, Vector3f[] i_queryPoints, Vector3f[] o_resultFlows)
    {
        return query(i_ownerHash, i_samplingData, i_queryPoints, o_resultFlows, null, null);
    }
}

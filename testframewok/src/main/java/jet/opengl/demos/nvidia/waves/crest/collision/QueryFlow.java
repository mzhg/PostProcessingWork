package jet.opengl.demos.nvidia.waves.crest.collision;

import org.lwjgl.util.vector.Vector3f;

import jet.opengl.demos.nvidia.waves.crest.CrestConst;
import jet.opengl.demos.nvidia.waves.crest.OceanRenderer;
import jet.opengl.demos.nvidia.waves.crest.SamplingData;
import jet.opengl.demos.nvidia.waves.crest.helpers.PropertyWrapperComputeStandalone;
import jet.opengl.postprocessing.buffer.BufferGL;

public class QueryFlow extends QueryBase implements CrestConst {
    final static int sp_LD_TexArray_Flow = 0; //Shader.PropertyToID("_LD_TexArray_Flow");
    final static int sp_ResultFlows = 0; //Shader.PropertyToID("_ResultFlows");

    @Override
    protected String QueryShaderName() {
        return SHADER_PATH + "QueryFlow";
    }

    private static QueryFlow gInstance;
    public static QueryFlow Instance() { return gInstance; }

    protected void OnEnable()
    {
        gInstance = this;

        super.OnEnable();
    }

    protected void OnDisable()
    {
        gInstance = null;

        super.OnDisable();
    }

    protected  void BindInputsAndOutputs(PropertyWrapperComputeStandalone wrapper, BufferGL resultsBuffer)
    {
        OceanRenderer.Instance._lodDataFlow.BindResultData(wrapper);
//        ShaderProcessQueries.SetTexture(_kernelHandle, sp_LD_TexArray_Flow, OceanRenderer.Instance._lodDataFlow.DataTexture);
//        ShaderProcessQueries.SetBuffer(_kernelHandle, sp_ResultFlows, resultsBuffer);
    }

    public int Query(int i_ownerHash, SamplingData i_samplingData, Vector3f[] i_queryPoints, Vector3f[] o_resultFlows)
    {
        return Query(i_ownerHash, i_samplingData, i_queryPoints, o_resultFlows, null, null);
    }
}

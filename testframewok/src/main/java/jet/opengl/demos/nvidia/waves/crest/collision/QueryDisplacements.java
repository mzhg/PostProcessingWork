package jet.opengl.demos.nvidia.waves.crest.collision;

import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;

import jet.opengl.demos.nvidia.waves.crest.AvailabilityResult;
import jet.opengl.demos.nvidia.waves.crest.CrestConst;
import jet.opengl.demos.nvidia.waves.crest.OceanRenderer;
import jet.opengl.demos.nvidia.waves.crest.SamplingData;
import jet.opengl.demos.nvidia.waves.crest.helpers.PropertyWrapperComputeStandalone;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.util.Rectf;

public class QueryDisplacements extends QueryBase implements ICollProvider, CrestConst {
    final static int sp_LD_TexArray_AnimatedWaves = 0; //Shader.PropertyToID("_LD_TexArray_AnimatedWaves");
    final static int sp_ResultDisplacements = 1; //Shader.PropertyToID("_ResultDisplacements");

//    protected override string QueryShaderName => "QueryDisplacements";
//    protected override string QueryKernelName => "CSMain";


    @Override
    protected String QueryShaderName() {
        return SHADER_PATH+ "QueryDisplacements.comp";
    }

    private static QueryDisplacements gInstance;

    public static QueryDisplacements Instance() { return gInstance;}

    protected void OnEnable()
    {
//        Debug.Assert(Instance == null);
        gInstance = this;

        super.OnEnable();
    }

    protected void OnDisable()
    {
        gInstance = null;

        super.OnDisable();
    }

    protected void BindInputsAndOutputs(PropertyWrapperComputeStandalone wrapper, BufferGL resultsBuffer)
    {
        OceanRenderer.Instance._lodDataAnimWaves.BindResultData(wrapper);
//        ShaderProcessQueries.SetTexture(_kernelHandle, sp_LD_TexArray_AnimatedWaves, OceanRenderer.Instance._lodDataAnimWaves.DataTexture);
//        ShaderProcessQueries.SetBuffer(_kernelHandle, sp_ResultDisplacements, resultsBuffer);
    }

    public boolean GetSamplingData(Rectf i_displacedSamplingArea, float i_minSpatialLength, SamplingData o_samplingData)
    {
        // Trivial. Will likely remove this in the future if we can deprecate the displacement texture readback stuff.
        o_samplingData._minSpatialLength = i_minSpatialLength;
        return true;
    }

    public void ReturnSamplingData(SamplingData i_data)
    {
        // Mark invalid
        i_data._minSpatialLength = -1f;
    }

    public int Query(int i_ownerHash, SamplingData i_samplingData, Vector3f[] i_queryPoints, float[] o_resultHeights, Vector3f[] o_resultNorms, Vector3f[] o_resultVels)
    {
        int result = (int)QueryStatus.OK;

        if (!UpdateQueryPoints(i_ownerHash, i_samplingData, o_resultNorms != null ? i_queryPoints : null, i_queryPoints))
        {
            result |= (int)QueryStatus.PostFailed;
        }

        if (!RetrieveResults(i_ownerHash, null, o_resultHeights, o_resultNorms))
        {
            result |= (int)QueryStatus.RetrieveFailed;
        }

        if (o_resultVels != null)
        {
            result |= CalculateVelocities(i_ownerHash, i_samplingData, i_queryPoints, o_resultVels);
        }

        return result;
    }

    public boolean SampleDisplacement(ReadableVector3f i_worldPos, SamplingData i_samplingData, Vector3f o_displacement)
    {
        throw new UnsupportedOperationException("Not implemented for the Compute collision provider - use the 'Query' functions.");
    }

    public long SampleHeight(ReadableVector3f i_worldPos, SamplingData i_samplingData/*, out float o_height*/)
    {
        throw new UnsupportedOperationException("Not implemented for the Compute collision provider - use the 'Query' functions.");
    }

    public AvailabilityResult CheckAvailability(ReadableVector3f i_worldPos, SamplingData i_samplingData)
    {
        throw new UnsupportedOperationException("Not implemented for the Compute collision provider - use the 'Query' functions.");
    }
}

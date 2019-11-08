package jet.opengl.demos.nvidia.waves.crest;

import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.util.Rectf;

final class Wave_Query_Displacements extends Wave_Query_Base implements Wave_Collision_Provider{
    @Override
    protected String QueryShaderName() { return "QueryDisplacements"; }

    @Override
    protected void bindInputsAndOutputs(Wave_Simulation_ShaderData wrapper, BufferGL resultsBuffer) {
        m_Simulation._lodDataAnimWaves.BindResultData(wrapper);

        wrapper._LD_TexArray_AnimatedWaves = m_Simulation._lodDataAnimWaves.DataTexture();
        wrapper._ResultDisplacements = resultsBuffer;
    }

    @Override
    public boolean getSamplingData(Rectf i_displacedSamplingArea, float i_minSpatialLength, SamplingData o_samplingData) {
        // Trivial. Will likely remove this in the future if we can deprecate the displacement texture readback stuff.
        o_samplingData._minSpatialLength = i_minSpatialLength;
        return true;
    }

    @Override
    public void returnSamplingData(SamplingData i_data) {
        // Mark invalid
        i_data._minSpatialLength = -1f;
    }

    @Override
    public boolean sampleDisplacement(ReadableVector3f i_worldPos, SamplingData i_samplingData, Vector3f o_displacement) {
        throw new UnsupportedOperationException("Not implemented for the Compute collision provider - use the 'Query' functions.");
    }

    @Override
    public long sampleDisplacementVel(ReadableVector3f i_worldPos, SamplingData i_samplingData, Vector3f o_displacement, Vector3f o_displacementVel) {
        throw new UnsupportedOperationException("Not implemented for the Compute collision provider - use the 'Query' functions.");
    }

    @Override
    public void sampleHeight(ReadableVector3f i_worldPos, SamplingData i_samplingData, SamplingHeight o_height) {
        throw new UnsupportedOperationException("Not implemented for the Compute collision provider - use the 'Query' functions.");
    }

    @Override
    public boolean sampleNormal(ReadableVector3f i_undisplacedWorldPos, SamplingData i_samplingData, Vector3f o_normal) {
        throw new UnsupportedOperationException("Not implemented for the Compute collision provider - use the 'Query' functions.");
    }

    @Override
    public boolean computeUndisplacedPosition(ReadableVector3f i_worldPos, SamplingData i_samplingData, Vector3f undisplacedWorldPos) {
        throw new UnsupportedOperationException("Not implemented for the Compute collision provider - use the 'Query' functions.");
    }

    @Override
    public int query(int i_ownerHash, SamplingData i_samplingData, Vector3f[] i_queryPoints, float[] o_resultHeights, Vector3f[] o_resultNorms, Vector3f[] o_resultVels) {
        int result = QueryStatus.OK;

        if (!updateQueryPoints(i_ownerHash, i_samplingData, o_resultNorms != null ? i_queryPoints : null, i_queryPoints))
        {
            result |= QueryStatus.PostFailed;
        }

        if (!RetrieveResults(i_ownerHash, null, o_resultHeights, o_resultNorms))
        {
            result |= QueryStatus.RetrieveFailed;
        }

        if (o_resultVels != null)
        {
            result |= CalculateVelocities(i_ownerHash, i_samplingData, i_queryPoints, o_resultVels);
        }

        return result;
    }

    @Override
    public AvailabilityResult checkAvailability(ReadableVector3f i_worldPos, SamplingData i_samplingData) {
        throw new UnsupportedOperationException("Not implemented for the Compute collision provider - use the 'Query' functions.");
    }
}

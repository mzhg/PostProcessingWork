package jet.opengl.demos.nvidia.waves.crest;

import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;

import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.postprocessing.util.Rectf;

final class Wave_Collision_Provider_Null implements Wave_Collision_Provider {

    private Wave_CDClipmap_Params m_Params;
    Wave_Collision_Provider_Null(Wave_CDClipmap_Params params){
        m_Params = params;
    }

    @Override
    public boolean getSamplingData(Rectf i_displacedSamplingArea, float i_minSpatialLength, SamplingData o_samplingData) {
        return true;
    }

    @Override
    public void returnSamplingData(SamplingData i_data) { }

    @Override
    public boolean sampleDisplacement(ReadableVector3f i_worldPos, SamplingData i_samplingData, Vector3f o_displacement) {
        o_displacement.set(0,0,0);
        return true;
    }

    @Override
    public long sampleDisplacementVel(ReadableVector3f i_worldPos, SamplingData i_samplingData, Vector3f o_displacement, Vector3f o_displacementVel) {
        o_displacement.set(0,0,0);
        boolean  o_displacementValid = true;
        o_displacementVel.set(0,0,0);
        boolean o_velValid = true;

        return Numeric.encode(1,1);
    }

    @Override
    public void sampleHeight(ReadableVector3f i_worldPos, SamplingData i_samplingData, SamplingHeight o_height) {
        o_height.height = m_Params.sea_level;
        o_height.valid = true;
    }

    @Override
    public boolean sampleNormal(ReadableVector3f i_undisplacedWorldPos, SamplingData i_samplingData, Vector3f o_normal) {
        o_normal.set(0,1,0);
        return true;
    }

    @Override
    public boolean computeUndisplacedPosition(ReadableVector3f i_worldPos, SamplingData i_samplingData, Vector3f undisplacedWorldPos) {
        undisplacedWorldPos.set(i_worldPos);
        undisplacedWorldPos.y = m_Params.sea_level;
        return true;
    }

    @Override
    public int query(int i_ownerHash, SamplingData i_samplingData, Vector3f[] i_queryPoints, float[] o_resultHeights, Vector3f[] o_resultNorms, Vector3f[] o_resultVels) {
        if (o_resultHeights != null)
        {
            for (int i = 0; i < o_resultHeights.length; i++)
            {
                o_resultHeights[i] = 0f;
            }
        }

        if (o_resultNorms != null)
        {
            for (int i = 0; i < o_resultNorms.length; i++)
            {
                o_resultNorms[i].set(0,1,0);
            }
        }

        if (o_resultVels != null)
        {
            for (int i = 0; i < o_resultVels.length; i++)
            {
                o_resultVels[i].set(0,0,0);
            }
        }

        return 0;
    }

    @Override
    public int query(int i_ownerHash, SamplingData i_samplingData, Vector3f[] i_queryPoints, Vector3f[] o_resultDisps, Vector3f[] o_resultNorms, Vector3f[] o_resultVels) {
        if (o_resultDisps != null)
        {
            for (int i = 0; i < o_resultDisps.length; i++)
            {
                o_resultDisps[i].set(0,0,0);
            }
        }

        if (o_resultNorms != null)
        {
            for (int i = 0; i < o_resultNorms.length; i++)
            {
                o_resultNorms[i].set(0,1,0);
            }
        }

        if (o_resultVels != null)
        {
            for (int i = 0; i < o_resultVels.length; i++)
            {
                o_resultVels[i].set(0,0,0);
            }
        }

        return 0;
    }

    @Override
    public boolean retrieveSucceeded(int queryStatus) {
        return true;
    }

    @Override
    public AvailabilityResult checkAvailability(ReadableVector3f i_worldPos, SamplingData i_samplingData) {
        return AvailabilityResult.DataAvailable;
    }
}

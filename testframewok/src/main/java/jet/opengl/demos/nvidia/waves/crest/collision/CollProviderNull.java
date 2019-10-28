package jet.opengl.demos.nvidia.waves.crest.collision;

import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;

import jet.opengl.demos.nvidia.waves.crest.OceanRenderer;
import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.postprocessing.util.Rectf;

public class CollProviderNull implements ICollProvider {
    public AvailabilityResult CheckAvailability(ReadableVector3f i_worldPos, SamplingData i_samplingData)
    {
        return AvailabilityResult.DataAvailable;
    }

    public boolean ComputeUndisplacedPosition(ReadableVector3f i_worldPos, Vector3f undisplacedWorldPos, float minSpatialLength)
    {
        undisplacedWorldPos.set(i_worldPos);
        undisplacedWorldPos.y = OceanRenderer.Instance.SeaLevel();
        return true;
    }

    public boolean ComputeUndisplacedPosition(ReadableVector3f i_worldPos, SamplingData i_samplingData, Vector3f undisplacedWorldPos)
    {
        undisplacedWorldPos.set(i_worldPos);
        undisplacedWorldPos.y = OceanRenderer.Instance.SeaLevel();
        return true;
    }

    public boolean GetSamplingData(Rectf i_displacedSamplingArea, float i_minSpatialLength, SamplingData o_samplingData2)
    {
        return true;
    }

    public void ReturnSamplingData(SamplingData i_data)
    {
    }

    public boolean SampleDisplacement(ReadableVector3f i_worldPos, SamplingData i_samplingData, Vector3f o_displacement)
    {
        o_displacement.set(0,0,0);
        return true;
    }

    public long SampleHeight(ReadableVector3f i_worldPos, SamplingData i_samplingData/*, out float o_height*/)
    {
//        o_height = OceanRenderer.Instance.SeaLevel;
//        return true;

        return Numeric.encode(1, Float.floatToIntBits(OceanRenderer.Instance.SeaLevel()));
    }

    public boolean SampleNormal(ReadableVector3f i_undisplacedWorldPos, SamplingData i_samplingData, Vector3f o_normal)
    {
        o_normal.set(0,1,0);
        return true;
    }

    public int Query(int i_ownerHash, SamplingData i_samplingData, Vector3f[] i_queryPoints, Vector3f[] o_resultDisps, Vector3f[] o_resultNorms, Vector3f[] o_resultVels)
    {
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

    public int Query(int i_ownerHash, SamplingData i_samplingData, Vector3f[] i_queryPoints, float[] o_resultHeights, Vector3f[] o_resultNorms, Vector3f[] o_resultVels)
    {
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

    public boolean RetrieveSucceeded(int queryStatus)
    {
        return true;
    }

    public static final CollProviderNull Instance = new CollProviderNull();
}

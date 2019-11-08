package jet.opengl.demos.nvidia.waves.crest;

import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import jet.opengl.demos.nvidia.waves.crest.gpureadback.IReadbackSettingsProvider;
import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.postprocessing.util.Rectf;

/**
 * Reads back displacements - this is the ocean shape, which includes any dynamic waves and any custom shape.
 */
final class Wave_GPUReadbackDisps extends Wave_GPUReadbackBase implements Wave_Collision_Provider {

    private PerLodData _areaData;

    protected boolean CanUseLastTwoLODs()
    {
//        get
//        {
            // The wave contents from the last LOD can be moved back and forth between the second-to-last LOD and it
            // results in pops if we use it
            return false;
//        }
    }

    @Override
    void init(Wave_CDClipmap clipmap, Wave_Simulation simulation) {
        super.init(clipmap, simulation);

        if (enabled == false)
        {
            return;
        }
//        _settingsProvider = OceanRenderer.Instance._simSettingsAnimatedWaves;

        _settingsProvider = new IReadbackSettingsProvider() {
            @Override
            public void GetMinMaxGridSizes(Vector2f gridSize) {
                // Wavelengths that repeat twice or more across the object are irrelevant and don't need to be read back.
                gridSize.x = 0.5f * m_Simulation.m_Params.min_object_width / m_Clipmap.getMinTexelsPerWave();
                gridSize.y = 0.5f * m_Simulation.m_Params.max_object_width / m_Clipmap.getMinTexelsPerWave();
            }
        };
    }

    @Override
    public boolean computeUndisplacedPosition(ReadableVector3f i_worldPos, SamplingData i_samplingData, Vector3f undisplacedWorldPos)
    {
        // Tag should not be null if the collision source is GPU readback.
        assert(i_samplingData._tag != null): "Invalid sampling data - LOD to sample from was unspecified.";

        PerLodData lodData = (PerLodData)i_samplingData._tag;

        // FPI - guess should converge to location that displaces to the target position
        Vector3f guess = new Vector3f(i_worldPos);
        // 2 iterations was enough to get very close when chop = 1, added 2 more which should be
        // sufficient for most applications. for high chop values or really stormy conditions there may
        // be some error here. one could also terminate iteration based on the size of the error, this is
        // worth trying but is left as future work for now.
        Vector3f disp = new Vector3f();

        for (int i = 0; i < 4 && lodData._resultData.InterpolateARGB16(guess, disp); i++)
        {
            float errorx = guess.x + disp.x - i_worldPos.getX();
            float errorz = guess.z + disp.z - i_worldPos.getZ();
            guess.x -= errorx;
            guess.z -= errorz;
        }

        undisplacedWorldPos.set(guess);
        undisplacedWorldPos.y = OceanRenderer.Instance.SeaLevel();

        return true;
    }

    public boolean computeUndisplacedPosition(ReadableVector3f i_worldPos, Vector3f undisplacedWorldPos, float minSpatialLength)
    {
        // FPI - guess should converge to location that displaces to the target position
        Vector3f guess = new Vector3f(i_worldPos);
        // 2 iterations was enough to get very close when chop = 1, added 2 more which should be
        // sufficient for most applications. for high chop values or really stormy conditions there may
        // be some error here. one could also terminate iteration based on the size of the error, this is
        // worth trying but is left as future work for now.
        Vector3f disp = new Vector3f();
        for (int i = 0; i < 4 && sampleDisplacement(guess, disp, minSpatialLength); i++)
        {
            float errorx = guess.x + disp.x - i_worldPos.getX();
            float errorz = guess.z + disp.z - i_worldPos.getZ();
            guess.x -= errorx;
            guess.z -= errorz;
        }

        undisplacedWorldPos.set(guess);
        undisplacedWorldPos.y = OceanRenderer.Instance.SeaLevel();

        return true;
    }

    public boolean sampleDisplacement(ReadableVector3f i_worldPos, Vector3f o_displacement)
    {
        PerLodData data = GetData(new Rectf(i_worldPos.getX(), i_worldPos.getZ(), 0f, 0f), 0f);
        if (data == null)
        {
            o_displacement.set(0,0,0);
            return false;
        }
        return data._resultData.InterpolateARGB16(i_worldPos, o_displacement);
    }

    public boolean sampleDisplacement(ReadableVector3f i_worldPos, Vector3f o_displacement, float minSpatialLength)
    {
        PerLodData data = GetData(new Rectf(i_worldPos.getX(), i_worldPos.getZ(), 0f, 0f), minSpatialLength);
        if (data == null)
        {
            o_displacement.set(0,0,0);
            return false;
        }
        return data._resultData.InterpolateARGB16(i_worldPos, o_displacement);
    }

    @Override
    public boolean sampleDisplacement(ReadableVector3f i_worldPos, SamplingData i_data, Vector3f o_displacement)
    {
        PerLodData lodData = (PerLodData)i_data._tag;
        if (lodData == null)
        {
            o_displacement.set(0,0,0);
            return false;
        }
        return lodData._resultData.InterpolateARGB16(i_worldPos, o_displacement);
    }

    public void sampleHeight(ReadableVector3f i_worldPos, SamplingData i_samplingData/*, out float height*/, SamplingHeight height)
    {
        Vector3f posFlatland = new Vector3f(i_worldPos);
        posFlatland.y = OceanRenderer.Instance.transform.getPositionY();

        Vector3f undisplacedPos = new Vector3f();
        computeUndisplacedPosition(posFlatland, i_samplingData, undisplacedPos);

        Vector3f disp = new Vector3f();
        sampleDisplacement(undisplacedPos, i_samplingData, disp);

        height.height = posFlatland.y + disp.y;
        height.valid = true;
//        return true;
//        return Numeric.encode(1, Float.floatToIntBits(posFlatland.y + disp.y));
    }

    public long sampleDisplacementVel(ReadableVector3f i_worldPos, SamplingData i_samplingData, Vector3f o_displacement, /*out bool o_displacementValid,*/ Vector3f o_displacementVel/*, out bool o_velValid*/)
    {
        PerLodData lodData = (PerLodData)i_samplingData._tag;

        boolean o_velValid;
        boolean o_displacementValid = lodData._resultData.InterpolateARGB16(i_worldPos, o_displacement);
        if (!o_displacementValid)
        {
            o_displacementVel.set(0,0,0);
            o_velValid = false;
            return 0;
        }

        // Check if this lod changed scales between result and previous result - if so can't compute vel. This should
        // probably go search for the results in the other LODs but returning 0 is easiest for now and should be ok-ish
        // for physics code.
        if (lodData._resultDataPrevFrame._renderData._texelWidth != lodData._resultData._renderData._texelWidth)
        {
            o_displacementVel.set(0,0,0);
            o_velValid = false;
            return 1;
        }

        Vector3f dispLast = new Vector3f();
        o_velValid = lodData._resultDataPrevFrame.InterpolateARGB16(i_worldPos, dispLast);
        if (!o_velValid)
        {
            o_displacementVel.set(0,0,0);
            return Numeric.encode(o_displacementValid ? 1:0, o_velValid?1:0);
        }

        assert(lodData._resultData.Valid() && lodData._resultDataPrevFrame.Valid());
//        o_displacementVel = (o_displacement - dispLast) / Mathf.Max(0.0001f, lodData._resultData._time - lodData._resultDataPrevFrame._time);
        Vector3f.sub(o_displacement, dispLast, o_displacementVel);
        o_displacementVel.scale(1.f/Math.max(0.0001f, lodData._resultData._time - lodData._resultDataPrevFrame._time));

        return Numeric.encode(o_displacementValid ? 1:0, o_velValid?1:0);
    }

    public boolean sampleNormal(ReadableVector3f i_undisplacedWorldPos, SamplingData i_samplingData, Vector3f o_normal)
    {
        PerLodData lodData = (PerLodData)i_samplingData._tag;
        float gridSize = lodData._resultData._renderData._texelWidth;

        if(o_normal == null)
            return true;

        Vector3f dispCenter = new Vector3f();
        if (!lodData._resultData.InterpolateARGB16(i_undisplacedWorldPos, dispCenter)) return false;

//        var undisplacedWorldPosX = i_undisplacedWorldPos + Vector3.right * gridSize;
        Vector3f undisplacedWorldPosX = Vector3f.linear(i_undisplacedWorldPos, Vector3f.X_AXIS, gridSize, null);
        Vector3f dispX = new Vector3f();
        if (!lodData._resultData.InterpolateARGB16(undisplacedWorldPosX, dispX)) return false;

        Vector3f undisplacedWorldPosZ = Vector3f.linear(i_undisplacedWorldPos, Vector3f.Z_AXIS, gridSize, null);
        Vector3f dispZ = new Vector3f();
        if (!lodData._resultData.InterpolateARGB16(undisplacedWorldPosZ, dispZ)) return false;

//        o_normal = Vector3.Cross(dispZ + Vector3.forward * gridSize - dispCenter, dispX + Vector3.right * gridSize - dispCenter).normalized;
        Vector3f zvalue = Vector3f.linear(dispZ, Vector3f.Z_AXIS, gridSize, undisplacedWorldPosX);
        Vector3f.sub( zvalue, dispCenter, zvalue);

        Vector3f xvalue = Vector3f.linear(dispX, Vector3f.X_AXIS, gridSize, undisplacedWorldPosZ);
        Vector3f.sub( xvalue, dispCenter, xvalue);

        Vector3f.cross(zvalue, xvalue, o_normal);
        o_normal.normalise();
        return true;
    }

    public int query(int i_ownerHash, SamplingData i_samplingData, Vector3f[] i_queryPoints, Vector3f[] o_resultDisps, Vector3f[] o_resultNorms, Vector3f[] o_resultVels)
    {
        int status = 0;

        if (o_resultDisps != null)
        {
            for (int i = 0; i < o_resultDisps.length; i++)
            {
                if (o_resultVels == null)
                {
                    if (!sampleDisplacement(i_queryPoints[i], i_samplingData, o_resultDisps[i]))
                    {
                        status = 1 | status;
                    }
                }
                else
                {
//                    bool dispValid, velValid;
                    long dispAndVel = sampleDisplacementVel(i_queryPoints[i], i_samplingData, o_resultDisps[i], /*dispValid,*/ o_resultVels[i]/*, velValid*/);
                    boolean dispValid = Numeric.decodeFirst(dispAndVel) != 0;
                    boolean velValid = Numeric.decodeSecond(dispAndVel) != 0;
                    if (!dispValid || !velValid)
                    {
                        status = 1 | status;
                    }
                }
            }
        }

        if (o_resultNorms != null)
        {
            for (int i = 0; i < o_resultNorms.length; i++)
            {
                Vector3f undispPos = new Vector3f();
                if (computeUndisplacedPosition(i_queryPoints[i], i_samplingData, undispPos))
                {
                    sampleNormal(undispPos, i_samplingData, o_resultNorms[i]);
                }
                else
                {
                    o_resultNorms[i] .set(0,1,0);
                    status = 1 | status;
                }
            }
        }

        return status;
    }

    public int query(int i_ownerHash, SamplingData i_samplingData, Vector3f[] i_queryPoints, float[] o_resultHeights, Vector3f[] o_resultNorms, Vector3f[] o_resultVels)
    {
        int status = 0;

        if (o_resultHeights != null)
        {
            for (int i = 0; i < o_resultHeights.length; i++)
            {
                if (o_resultVels == null)
                {
                    Vector3f disp = new Vector3f();
                    if (sampleDisplacement(i_queryPoints[i], i_samplingData, disp))
                    {
                        o_resultHeights[i] = OceanRenderer.Instance.SeaLevel() + disp.y;
                    }
                    else
                    {
                        status = 1 | status;
                    }
                }
                else
                {
                    Vector3f disp = new Vector3f();
//                    bool dispValid, velValid;
                    long dispAndVel = sampleDisplacementVel(i_queryPoints[i], i_samplingData, disp, /*out dispValid, out*/ o_resultVels[i]/*, out velValid*/);
                    if (/*dispValid && velValid*/dispAndVel > 1)
                    {
                        o_resultHeights[i] = m_Clipmap.getSeaLevel() + disp.y;
                    }
                    else
                    {
                        status = 1 | status;
                    }
                }
            }
        }

        if (o_resultNorms != null)
        {
            for (int i = 0; i < o_resultNorms.length; i++)
            {
                Vector3f undispPos = new Vector3f();
                if (computeUndisplacedPosition(i_queryPoints[i], i_samplingData, undispPos))
                {
                    sampleNormal(undispPos, i_samplingData, o_resultNorms[i]);
                }
                else
                {
                    o_resultNorms[i].set(0,1,0);
                    status = 1 | status;
                }
            }
        }

        return status;
    }

    public int query(int i_ownerHash, SamplingData i_samplingData, Vector3f[] i_queryDisplacementToPoints, Vector3f[] i_queryNormalAtPoint, Vector3f[] o_resultDisps, Vector3f[] o_resultNorms, Vector3f[] o_resultVels)
    {
        int status = 0;

        if (o_resultDisps != null)
        {
            for (int i = 0; i < o_resultDisps.length; i++)
            {
//                var dispValid = false;
//                var velValid = false;
                long dispAndVel = sampleDisplacementVel(i_queryDisplacementToPoints[i], i_samplingData, o_resultDisps[i], /*out dispValid, out*/ o_resultVels[i]/*, out velValid*/);
                boolean dispValid = Numeric.decodeFirst(dispAndVel) != 0;
                boolean velValid = Numeric.decodeSecond(dispAndVel) != 0;
                if (!dispValid || !velValid)
                {
                    status = 1 | status;
                }
            }
        }

        if (o_resultNorms != null)
        {
            for (int i = 0; i < o_resultNorms.length; i++)
            {
                Vector3f undispPos = new Vector3f();
                if (computeUndisplacedPosition(i_queryNormalAtPoint[i], i_samplingData, undispPos))
                {
                    sampleNormal(undispPos, i_samplingData, o_resultNorms[i]);
                }
                else
                {
                    o_resultNorms[i].set(0,1,0);
                    status = 1 | status;
                }
            }
        }

        return status;
    }

    public boolean retrieveSucceeded(int queryStatus)
    {
        return queryStatus == 0;
    }
}

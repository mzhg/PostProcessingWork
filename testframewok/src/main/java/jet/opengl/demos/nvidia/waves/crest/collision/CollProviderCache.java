package jet.opengl.demos.nvidia.waves.crest.collision;

import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;

import java.util.HashMap;
import java.util.Map;

import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.postprocessing.util.Rectf;

public class CollProviderCache implements ICollProvider {
    public float _cacheBucketSize = 0.1f;

    ICollProvider _collProvider;
    int _cacheHits, _cacheHitsLastFrame;
    int _cacheChecks, _cacheChecksLastFrame;
    float _cacheBucketSizeRecip = 0f;

//    readonly Dictionary<uint, float> _waterHeightCache = new Dictionary<uint, float>();
    final Map<Integer, Float> _waterHeightCache = new HashMap<>();

    public CollProviderCache(ICollProvider collProvider)
    {
        _collProvider = collProvider;
    }

    public void ClearCache()
    {
        _cacheBucketSizeRecip = 1f / Math.max(_cacheBucketSize, 0.00001f);

        _cacheChecksLastFrame = _cacheChecks;
        _cacheChecks = 0;
        _cacheHitsLastFrame = _cacheHits;
        _cacheHits = 0;

        _waterHeightCache.clear();
    }

    int CalcHash(ReadableVector3f i_wp)
    {
        int x = (int)(i_wp.getX() * _cacheBucketSizeRecip);
        int z = (int)(i_wp.getZ() * _cacheBucketSizeRecip);
        return (x + 32768 + ((z + 32768) << 16));
    }

    public AvailabilityResult CheckAvailability(ReadableVector3f i_worldPos, SamplingData i_samplingData)
    {
        return _collProvider.CheckAvailability(i_worldPos, i_samplingData);
    }

    public boolean GetSamplingData(Rectf i_displacedSamplingArea, float i_minSpatialLength, SamplingData o_samplingData)
    {
        return _collProvider.GetSamplingData(i_displacedSamplingArea, i_minSpatialLength, o_samplingData);
    }

    public void ReturnSamplingData(SamplingData i_data)
    {
        _collProvider.ReturnSamplingData(i_data);
    }

    public int Query(int i_ownerHash, SamplingData i_samplingData, Vector3f[] i_queryDisplacementToPoints, Vector3f[] i_queryNormalAtPoint, Vector3f[] o_resultDisps, Vector3f[] o_resultNorms)
    {
        // Displacements and normals not cached
        return _collProvider.Query(i_ownerHash, i_samplingData, i_queryDisplacementToPoints, i_queryNormalAtPoint, o_resultDisps, o_resultNorms);
    }

    public long SampleHeight(ReadableVector3f i_worldPos, SamplingData i_samplingData)
    {
        float o_height;
        int hash = CalcHash(i_worldPos);

        _cacheChecks++;

        Float value = _waterHeightCache.get(hash);

        if (/*_waterHeightCache.TryGetValue(hash, out o_height)*/value != null)
        {
            o_height = value.floatValue();
            // got it from the cache!
            _cacheHits++;
//            return true;

            return Numeric.encode(1, Float.floatToIntBits(o_height));
        }

        // compute the height
        long successAndHeight = _collProvider.SampleHeight(i_worldPos, i_samplingData/*, out o_height*/);

        o_height = Float.intBitsToFloat(Numeric.decodeSecond(successAndHeight));
        // populate cache (regardless of success for now)
        _waterHeightCache.put(hash, o_height);

        return successAndHeight;
    }

//        [Obsolete("The collision cache is obsolete.")]
    @Override
    public int Query(int i_ownerHash, SamplingData i_samplingData, Vector3f[] i_queryPoints, float[] o_resultHeights, Vector3f[] o_resultNorms, Vector3f[] o_resultVels)
    {
        int status = 0;

        if (o_resultHeights != null)
        {
            for (int i = 0; i < i_queryPoints.length; i++)
            {
                long successAndHeight = SampleHeight(i_queryPoints[i], i_samplingData/*, out o_resultHeights[i]*/);
                int success = Numeric.decodeFirst(successAndHeight);
                status = status | success;
                o_resultHeights[i] = Float.intBitsToFloat(Numeric.decodeSecond(successAndHeight));
            }
        }

        if (o_resultNorms != null)
        {
            // No caching for normals - go straight to source for these
            status = status | _collProvider.Query(i_ownerHash, i_samplingData, i_queryPoints, (float[])null, o_resultNorms, null);
        }

        return status;
    }

//    [Obsolete("This API is deprecated. Use the 'Query' APIs instead.")]
    public boolean SampleDisplacement(ReadableVector3f i_worldPos, SamplingData i_samplingData, Vector3f o_displacement)
    {
        return _collProvider.SampleDisplacement(i_worldPos, i_samplingData, o_displacement);
    }


    public boolean RetrieveSucceeded(int queryStatus)
    {
        return _collProvider.RetrieveSucceeded(queryStatus);
    }

    public int CacheChecks() { return _cacheChecksLastFrame; }
    public int CacheHits() { return _cacheHitsLastFrame; }
}

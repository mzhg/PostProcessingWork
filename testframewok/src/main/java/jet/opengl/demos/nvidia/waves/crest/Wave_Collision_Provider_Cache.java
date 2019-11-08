package jet.opengl.demos.nvidia.waves.crest;

import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;

import java.util.HashMap;

import jet.opengl.postprocessing.util.Rectf;

final class Wave_Collision_Provider_Cache implements Wave_Collision_Provider{

    private float _cacheBucketSize = 0.1f;

    private Wave_Collision_Provider _collProvider;
    private int _cacheHits, _cacheHitsLastFrame;
    private int _cacheChecks, _cacheChecksLastFrame;
    private float _cacheBucketSizeRecip = 0f;

    private final HashMap<Integer, Float> _waterHeightCache = new HashMap<>();

    public Wave_Collision_Provider_Cache(Wave_Collision_Provider collProvider)
    {
        _collProvider = collProvider;
    }

    public void clearCache()
    {
        _cacheBucketSizeRecip = 1f / Math.max(_cacheBucketSize, 0.00001f);

        _cacheChecksLastFrame = _cacheChecks;
        _cacheChecks = 0;
        _cacheHitsLastFrame = _cacheHits;
        _cacheHits = 0;

        _waterHeightCache.clear();
    }

    private int calcHash(ReadableVector3f i_wp)
    {
        int x = (int)(i_wp.getX() * _cacheBucketSizeRecip);
        int z = (int)(i_wp.getZ() * _cacheBucketSizeRecip);
        return (x + 32768 + ((z + 32768) << 16));
    }

    /**Height is the only thing that is cached right now. We could cache disps and normals too, but the height queries are heaviest.*/
    @Override
    public void sampleHeight(ReadableVector3f i_worldPos, SamplingData i_samplingData, SamplingHeight height)
    {
        int hash = calcHash(i_worldPos);

        Float o_height;
        _cacheChecks++;
        if (/*_waterHeightCache.TryGetValue(hash, out o_height)*/ (o_height = _waterHeightCache.get(hash)) != null)
        {
            // got it from the cache!
            _cacheHits++;
//            return true;
            height.valid = true;
            height.height = o_height;
            return;
        }

        // compute the height
        _collProvider.sampleHeight(i_worldPos, i_samplingData,  height);

        // populate cache (regardless of success for now)
        _waterHeightCache.put(hash, height.height);

//        return success;
    }
    @Override
    public AvailabilityResult checkAvailability(ReadableVector3f i_worldPos, SamplingData i_samplingData)
    {
        return _collProvider.checkAvailability(i_worldPos, i_samplingData);
    }

    @Override
    public boolean getSamplingData(Rectf i_displacedSamplingArea, float i_minSpatialLength, SamplingData o_samplingData)
    {
        return _collProvider.getSamplingData(i_displacedSamplingArea, i_minSpatialLength, o_samplingData);
    }

    @Override
    public void returnSamplingData(SamplingData i_data)
    {
        _collProvider.returnSamplingData(i_data);
    }

    @Override
    public boolean computeUndisplacedPosition(ReadableVector3f i_worldPos, SamplingData i_samplingData, Vector3f undisplacedWorldPos)
    {
        return _collProvider.computeUndisplacedPosition(i_worldPos, i_samplingData, undisplacedWorldPos);
    }

    @Override
    public long sampleDisplacementVel(ReadableVector3f i_worldPos, SamplingData i_samplingData, Vector3f o_displacement, Vector3f o_displacementVel)
    {
        return _collProvider.sampleDisplacementVel(i_worldPos, i_samplingData, o_displacement, o_displacementVel);
    }

    public boolean sampleNormal(ReadableVector3f i_undisplacedWorldPos, SamplingData i_samplingData, Vector3f o_normal)
    {
        return _collProvider.sampleNormal(i_undisplacedWorldPos, i_samplingData, o_normal);
    }

    public boolean sampleDisplacement(ReadableVector3f i_worldPos, SamplingData i_samplingData, Vector3f o_displacement)
    {
        return _collProvider.sampleDisplacement(i_worldPos, i_samplingData, o_displacement);
    }

    public int query(int i_ownerHash, SamplingData i_samplingData, Vector3f[] i_queryDisplacementToPoints, Vector3f[] i_queryNormalAtPoint, Vector3f[] o_resultDisps, Vector3f[] o_resultNorms)
    {
        // Displacements and normals not cached
        return _collProvider.query(i_ownerHash, i_samplingData, i_queryDisplacementToPoints, i_queryNormalAtPoint, o_resultDisps, o_resultNorms);
    }

    public int query(int i_ownerHash, SamplingData i_samplingData, Vector3f[] i_queryPoints, float[] o_resultHeights, Vector3f[] o_resultNorms, Vector3f[] o_resultVels)
    {
        int status = 0;

        if (o_resultHeights != null)
        {
            SamplingHeight o_height = new SamplingHeight();
            for (int i = 0; i < i_queryPoints.length; i++)
            {
                sampleHeight(i_queryPoints[i], i_samplingData, /*out o_resultHeights[i]*/o_height);
                status = status | (o_height.valid ? 0 : 1);
                o_resultHeights[i] = o_height.height;
            }
        }

        if (o_resultNorms != null)
        {
            // No caching for normals - go straight to source for these
            status = status | _collProvider.query(i_ownerHash, i_samplingData, i_queryPoints, (float[])null, o_resultNorms, null);
        }

        return status;
    }


    public boolean retrieveSucceeded(int queryStatus)
    {
        return _collProvider.retrieveSucceeded(queryStatus);
    }

//    public int CacheChecks { get { return _cacheChecksLastFrame; } }
//    public int CacheHits { get { return _cacheHitsLastFrame; } }
}

package jet.opengl.demos.nvidia.waves.crest;

import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;

import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.postprocessing.util.Rectf;

/**
 * Helper to obtain the ocean surface height at a single location. This is not particularly efficient to sample a single height,
 * but is a fairly common case.
 */
final class Wave_SampleHeight_Helper {
    private SamplingData _samplingData = new SamplingData();
    private Vector3f[] _queryPos = new Vector3f[1];
    private Vector3f[] _queryResult = new Vector3f[1];
    private Vector3f[] _queryResultNormal = new Vector3f[1];
    private Vector3f[] _queryResultVel = new Vector3f[1];

    private boolean _valid = false;
    private Wave_Collision_Provider _collision_provider;

    public Wave_SampleHeight_Helper(){
        _queryPos[0] = new Vector3f();
        _queryResult[0] = new Vector3f();
        _queryResultNormal[0] = new Vector3f();
        _queryResultVel[0] = new Vector3f();
    }

    /**
     * Call this to prime the sampling
     * @param collision_provider 
     * @param i_queryPos  World space position to sample 
     * @param i_minLength  The smallest length scale you are interested in. If you are sampling data for boat physics, pass in the boats width. Larger objects will ignore small wavelengths.
     * @return
     */
    public boolean init(Wave_Collision_Provider collision_provider, ReadableVector3f i_queryPos, float i_minLength)
    {
        _collision_provider = collision_provider;
        _queryPos[0].set(i_queryPos);
        Rectf rect = new Rectf(i_queryPos.getX(), i_queryPos.getZ(), 0f, 0f);
        return _valid = _collision_provider.getSamplingData(rect, i_minLength, _samplingData);
    }

    /// <summary>
    /// Call this to do the query. Can be called only once after Init().
    /// </summary>
    public long Sample(/*ref float o_height*/)
    {
        if (!_valid)
        {
            return 0;
        }

        int status = _collision_provider.query(hashCode(), _samplingData, _queryPos, _queryResult, null, null);

        _collision_provider.returnSamplingData(_samplingData);

        if (!_collision_provider.retrieveSucceeded(status))
        {
            _valid = false;
            return 0;
        }

        float o_height = _queryResult[0].y + OceanRenderer.Instance.SeaLevel();

        return Numeric.encode(1, Float.floatToIntBits(o_height));
    }

    public long Sample(/*ref float o_height,*/float seaLeval, Vector3f o_normal)
    {
        if (!_valid)
        {
            return 0;
        }

        int status = _collision_provider.query(hashCode(), _samplingData, _queryPos, _queryResult, _queryResultNormal, null);

        _collision_provider.returnSamplingData(_samplingData);

        if (!_collision_provider.retrieveSucceeded(status))
        {
            _valid = false;
            return 0;
        }

        float o_height = _queryResult[0].y + seaLeval;
        o_normal.set(_queryResultNormal[0]);

//        return true;
        return Numeric.encode(1, Float.floatToIntBits(o_height));
    }

    public long Sample(/*ref float o_height, ref*/ float seaLeval, Vector3f o_normal, Vector3f o_surfaceVel)
    {
        if (!_valid)
        {
            return 0;
        }

        int status = _collision_provider.query(hashCode(), _samplingData, _queryPos, _queryResult, _queryResultNormal, _queryResultVel);

        _collision_provider.returnSamplingData(_samplingData);

        if (!_collision_provider.retrieveSucceeded(status))
        {
            return 0;
        }

        float o_height = _queryResult[0].y + seaLeval;
        o_normal.set(_queryResultNormal[0]);
        o_surfaceVel.set(_queryResultVel[0]);

        return Numeric.encode(1, Float.floatToIntBits(o_height));
    }

    public boolean Sample(Vector3f o_displacementToPoint, Vector3f o_normal, Vector3f o_surfaceVel)
    {
        if (!_valid)
        {
            return false;
        }

        int status = _collision_provider.query(hashCode(), _samplingData, _queryPos, _queryResult, _queryResultNormal, _queryResultVel);

        _collision_provider.returnSamplingData(_samplingData);

        if (!_collision_provider.retrieveSucceeded(status))
        {
            return false;
        }

        o_displacementToPoint.set(_queryResult[0]);
        o_normal.set(_queryResultNormal[0]);
        o_surfaceVel.set(_queryResultVel[0]);

        return true;
    }
}

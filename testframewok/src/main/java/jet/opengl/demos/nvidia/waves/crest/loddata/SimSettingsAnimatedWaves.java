package jet.opengl.demos.nvidia.waves.crest.loddata;

import org.lwjgl.util.vector.Vector2f;

import jet.opengl.demos.nvidia.waves.crest.OceanRenderer;
import jet.opengl.demos.nvidia.waves.crest.collision.CollProviderCache;
import jet.opengl.demos.nvidia.waves.crest.collision.CollProviderNull;
import jet.opengl.demos.nvidia.waves.crest.collision.ICollProvider;
import jet.opengl.demos.nvidia.waves.crest.collision.QueryDisplacements;
import jet.opengl.demos.nvidia.waves.crest.gpureadback.GPUReadbackDisps;
import jet.opengl.demos.nvidia.waves.crest.gpureadback.IReadbackSettingsProvider;

public class SimSettingsAnimatedWaves implements IReadbackSettingsProvider,SimSettingsBase {

    public String name;
//    [Tooltip("How much waves are dampened in shallow water."), SerializeField, Range(0f, 1f)]
    private float _attenuationInShallows = 0.95f;
    public float AttenuationInShallows() { return _attenuationInShallows; }

    public enum CollisionSources
    {
        None,
        OceanDisplacementTexturesGPU,
        GerstnerWavesCPU,
        ComputeShaderQueries,
    }
//        [Header("Readback to CPU")]
//            [Tooltip("Where to obtain ocean shape on CPU for physics / gameplay."), SerializeField]
    public CollisionSources CollisionSource = CollisionSources.ComputeShaderQueries;

//        [SerializeField, Tooltip("Cache CPU requests for ocean height. Requires restart.")]
    public boolean CachedHeightQueries = false;

//        [Header("GPU Readback Settings")]
//            [Tooltip("Minimum floating object width. The larger the objects that will float, the lower the resolution of the read data. If an object is small, the highest resolution LODs will be sample for physics. This is an optimisation. Set to 0 to disable this optimisation and always copy high res data.")]
    public float _minObjectWidth = 3f;

    // By default copy waves big enough to do buoyancy on a 50m wide object. This ensures we get the wavelengths, and by extension makes
    // sure we get good range on wave physics.
//        [Tooltip("Similar to the minimum width, but this setting will exclude the larger LODs from being copied. Set to 0 to disable this optimisation and always copy low res data.")]
    public float _maxObjectWidth = 500f;

    public void GetMinMaxGridSizes(Vector2f girdSize)
    {
        // Wavelengths that repeat twice or more across the object are irrelevant and don't need to be read back.
        girdSize.x = 0.5f * _minObjectWidth / OceanRenderer.Instance.MinTexelsPerWave;
        girdSize.y = 0.5f * _maxObjectWidth / OceanRenderer.Instance.MinTexelsPerWave;
    }

    /// <summary>
    /// Provides ocean shape to CPU.
    /// </summary>
    public ICollProvider CreateCollisionProvider()
    {
        ICollProvider result = null;

        switch (CollisionSource)
        {
            case None:
                result = new CollProviderNull();
                break;
            case OceanDisplacementTexturesGPU:
                result = GPUReadbackDisps.Instance;
//                Debug.Assert(result != null, "Sampling collision too early, collision system has not been initialised.");
                break;
            case GerstnerWavesCPU:
                result = FindObjectOfType<ShapeGerstnerBatched>();
                break;
            case ComputeShaderQueries:
                result = QueryDisplacements.Instance();
                break;
        }

        if (result == null && CollisionSource == CollisionSources.OceanDisplacementTexturesGPU)
        {
            // can happen if async readback not supported on device
            result = new CollProviderNull();
        }

        if (result == null)
        {
            // this should not be hit - return null to create null ref exceptions
            assert(false): "Could not create collision provider. Collision source = " + _collisionSource.toString();
            return null;
        }

        if (_cachedHeightQueries)
        {
            result = new CollProviderCache(result);
        }

        return result;
    }
}

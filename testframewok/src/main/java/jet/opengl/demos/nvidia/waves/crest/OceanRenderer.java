package jet.opengl.demos.nvidia.waves.crest;

import com.nvidia.developer.opengl.models.obj.Material;

import org.lwjgl.util.vector.Transform;

import jdk.nashorn.internal.runtime.Debug;
import jet.opengl.demos.intel.fluid.scene.Light;
import jet.opengl.demos.nvidia.waves.crest.collision.CollProviderCache;
import jet.opengl.demos.nvidia.waves.crest.collision.ICollProvider;
import jet.opengl.demos.nvidia.waves.crest.collision.SampleHeightHelper;
import jet.opengl.demos.nvidia.waves.crest.helpers.Time;
import jet.opengl.demos.nvidia.waves.crest.loddata.LodDataMgrAnimWaves;
import jet.opengl.demos.nvidia.waves.crest.loddata.LodDataMgrDynWaves;
import jet.opengl.demos.nvidia.waves.crest.loddata.LodDataMgrFlow;
import jet.opengl.demos.nvidia.waves.crest.loddata.LodDataMgrFoam;
import jet.opengl.demos.nvidia.waves.crest.loddata.LodDataMgrSeaFloorDepth;
import jet.opengl.demos.nvidia.waves.crest.loddata.LodDataMgrShadow;
import jet.opengl.demos.nvidia.waves.crest.loddata.LodTransform;
import jet.opengl.demos.nvidia.waves.crest.loddata.SimSettingsAnimatedWaves;
import jet.opengl.demos.nvidia.waves.crest.loddata.SimSettingsFoam;
import jet.opengl.demos.nvidia.waves.crest.loddata.SimSettingsShadow;
import jet.opengl.demos.nvidia.waves.crest.loddata.SimSettingsWave;
import jet.opengl.postprocessing.util.LogUtil;
import jet.opengl.postprocessing.util.Numeric;

/**
 * The main script for the ocean system. Attach this to a GameObject to create an ocean. This script initializes the various data types and systems
 * and moves/scales the ocean based on the viewpoint. It also hosts a number of global settings that can be tweaked here.
 */
public class OceanRenderer extends MonoBehaviour {
//    [Tooltip("The viewpoint which drives the ocean detail. Defaults to main camera."), SerializeField]
    private final Transform _viewpoint = new Transform();
    public Transform Viewpoint(){ return _viewpoint;}
    public void Viewpoint(Transform transform) { _viewpoint.set(transform);}

//        [Tooltip("Optional provider for time, can be used to hard-code time for automation, or provide server time. Defaults to local Unity time."), SerializeField]
//    TimeProviderBase _timeProvider;
//    public float CurrentTime => _timeProvider.CurrentTime;
//    public float DeltaTime => _timeProvider.DeltaTime;
//    public float DeltaTimeDynamics => _timeProvider.DeltaTimeDynamics;

//        [Header("Ocean Params")]

//            [SerializeField, Tooltip("Material to use for the ocean surface")]
    Material _material = null;
    public Material OceanMaterial()  { return _material;  }

//        [SerializeField]
    String _layerName = "Water";
    public String LayerName() { return _layerName; }

//        [SerializeField, Delayed, Tooltip("Multiplier for physics gravity."), Range(0f, 10f)]
    float _gravityMultiplier = 1f;
    public float Gravity() { return _gravityMultiplier * /*Physics.gravity.magnitude*/ -9.8f; }


//        [Header("Detail Params")]

//            [Range(2, 16)]
//            [Tooltip("Min number of verts / shape texels per wave."), SerializeField]
//    float _minTexelsPerWave = 3f;
    public float MinTexelsPerWave = 3;

//        [Delayed, Tooltip("The smallest scale the ocean can be."), SerializeField]
    float _minScale = 8f;

//        [Delayed, Tooltip("The largest scale the ocean can be (-1 for unlimited)."), SerializeField]
    float _maxScale = 256f;

//        [Tooltip("Drops the height for maximum ocean detail based on waves. This means if there are big waves, max detail level is reached at a lower height, which can help visual range when there are very large waves and camera is at sea level."), SerializeField, Range(0f, 1f)]
    float _dropDetailHeightBasedOnWaves = 0.2f;

//        [SerializeField, Delayed, Tooltip("Resolution of ocean LOD data. Use even numbers like 256 or 384. This is 4x the old 'Base Vert Density' param, so if you used 64 for this param, set this to 256.")]
    int _lodDataResolution = 256;
    public int LodDataResolution () { return _lodDataResolution; }

//        [SerializeField, Delayed, Tooltip("How much of the water shape gets tessellated by geometry. If set to e.g. 4, every geometry quad will span 4x4 LOD data texels. Use power of 2 values like 1, 2, 4...")]
    int _geometryDownSampleFactor = 2;

//        [SerializeField, Tooltip("Number of ocean tile scales/LODs to generate."), Range(2, LodDataMgr.MAX_LOD_COUNT)]
    int _lodCount = 7;


//        [Header("Simulation Params")]
    public SimSettingsAnimatedWaves _simSettingsAnimatedWaves;

//        [Tooltip("Water depth information used for shallow water, shoreline foam, wave attenuation, among others."), SerializeField]
    public boolean CreateSeaFloorDepthData = true;

//        [Tooltip("Simulation of foam created in choppy water and dissipating over time."), SerializeField]
    public boolean CreateFoamSim = true;
    public SimSettingsFoam _simSettingsFoam;

//        [Tooltip("Dynamic waves generated from interactions with objects such as boats."), SerializeField]
    public boolean CreateDynamicWaveSim = false;
    public SimSettingsWave _simSettingsDynamicWaves;

//        [Tooltip("Horizontal motion of water body, akin to water currents."), SerializeField]
    public boolean CreateFlowSim = false;
//    public SimSettingsFlow _simSettingsFlow;

//        [Tooltip("Shadow information used for lighting water."), SerializeField]
    public boolean CreateShadowData = false;
//        [Tooltip("The primary directional light. Required if shadowing is enabled.")]
    public Light _primaryLight;
    public SimSettingsShadow _simSettingsShadow;


//        [Header("Debug Params")]
//            [Tooltip("Whether to generate ocean geometry tiles uniformly (with overlaps).")]
    public boolean _uniformTiles = false;
//        [Tooltip("Disable generating a wide strip of triangles at the outer edge to extend ocean to edge of view frustum.")]
    public boolean _disableSkirt = false;
//        [Tooltip("Move ocean with viewpoint.")]
    public boolean _followViewpoint = true;

    /// <summary>
    /// Current ocean scale (changes with viewer altitude).
    /// </summary>

    public float Scale = 1;
    public float CalcLodScale(float lodIndex) { return (float) (Scale * Math.pow(2f, lodIndex)); }
    public float CalcGridSize(int lodIndex) { return CalcLodScale(lodIndex) / _lodDataResolution; }

    /// <summary>
    /// The ocean changes scale when viewer changes altitude, this gives the interpolation param between scales.
    /// </summary>
    public float ViewerAltitudeLevelAlpha;

    /// <summary>
    /// Sea level is given by y coordinate of GameObject with OceanRenderer script.
    /// </summary>
    public float SeaLevel () { return transform.getPositionY();  }

    public LodTransform _lodTransform;
    public LodDataMgrAnimWaves _lodDataAnimWaves;
    public LodDataMgrSeaFloorDepth _lodDataSeaDepths;
    public LodDataMgrDynWaves _lodDataDynWaves;
    public LodDataMgrFlow _lodDataFlow;
    public LodDataMgrFoam _lodDataFoam;
    public LodDataMgrShadow _lodDataShadow;
    /// <summary>
    /// The number of LODs/scales that the ocean is currently using.
    /// </summary>
    public int CurrentLodCount()  { return _lodTransform.LodCount();  }

    /// <summary>
    /// Vertical offset of viewer vs water surface
    /// </summary>
    private float _ViewerHeightAboveWater;
    public float ViewerHeightAboveWater() { return _ViewerHeightAboveWater;}

    SampleHeightHelper _sampleHeightHelper = new SampleHeightHelper();

    static int sp_crestTime = 0; //Shader.PropertyToID("_CrestTime");
    static int sp_texelsPerWave = 1; //Shader.PropertyToID("_TexelsPerWave");


    void Awake()
    {
        if (!VerifyRequirements())
        {
            enabled = false;
            return;
        }

        Instance = this;
        Scale = Numeric.clamp(Scale, _minScale, _maxScale);

        OceanBuilder.GenerateMesh(this, _lodDataResolution, _geometryDownSampleFactor, _lodCount);

        /*if (null == GetComponent<BuildCommandBufferBase>())
        {
            gameObject.AddComponent<BuildCommandBuffer>();
        }*/

        InitViewpoint();
        InitTimeProvider();
    }

    boolean VerifyRequirements()
    {
        if (_material == null)
        {
            LogUtil.e(LogUtil.LogType.DEFAULT, "A material for the ocean must be assigned on the Material property of the OceanRenderer.");
            return false;
        }


        return true;
    }

    void InitViewpoint()
    {
        if (_viewpoint == null)
        {
            var camMain = Camera.main;
            if (camMain != null)
            {
                _viewpoint = camMain.transform;
            }
            else
            {
                Debug.LogError("Please provide the viewpoint transform, or tag the primary camera as MainCamera.", this);
            }
        }
    }

    /*void InitTimeProvider()
    {
        // Used assigned time provider, or use one attached to this game object
        if (_timeProvider == null && (_timeProvider = GetComponent<TimeProviderBase>()) == null)
        {
            // None found - create
            _timeProvider = gameObject.AddComponent<TimeProviderDefault>();
        }
    }*/

    void Update()
    {
        UpdateCollision();
    }

    void UpdateCollision()
    {
        if (_simSettingsAnimatedWaves.CachedHeightQueries)
        {
            ((CollProviderCache)CollisionProvider()).ClearCache();
        }
    }

    void LateUpdate()
    {
        // set global shader params
        Shader.SetGlobalFloat(sp_texelsPerWave, MinTexelsPerWave);
        Shader.SetGlobalFloat(sp_crestTime, CurrentTime);

        if (_viewpoint == null)
        {
            Debug.LogError("_viewpoint is null, ocean update will fail.", this);
        }

        if (_followViewpoint)
        {
            LateUpdatePosition();
            LateUpdateScale();
            LateUpdateViewerHeight();
        }

        LateUpdateLods();

        Build(null);
    }

    void Build(CommandBuffer buf)
    {
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // --- Ocean depths
        if (_lodDataSeaDepths !=null)
        {
            _lodDataSeaDepths.BuildCommandBuffer(this, buf);
        }

        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // --- Flow data
        if (_lodDataFlow != null)
        {
            _lodDataFlow.BuildCommandBuffer( this,buf);
        }

        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // --- Dynamic wave simulations
        if (_lodDataDynWaves != null)
        {
            _lodDataDynWaves.BuildCommandBuffer(this, buf);
        }

        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // --- Animated waves next
        if (_lodDataAnimWaves != null)
        {
            _lodDataAnimWaves.BuildCommandBuffer(this, buf);
        }

        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // --- Foam simulation
        if (_lodDataFoam != null)
        {
            _lodDataFoam.BuildCommandBuffer(this, buf);
        }
    }

    void LateUpdatePosition()
    {
        Vector3 pos = _viewpoint.position;

        // maintain y coordinate - sea level
        pos.y = transform.position.y;

        transform.position = pos;

        Shader.SetGlobalVector("_OceanCenterPosWorld", transform.position);
    }

    void LateUpdateScale()
    {
        // reach maximum detail at slightly below sea level. this should combat cases where visual range can be lost
        // when water height is low and camera is suspended in air. i tried a scheme where it was based on difference
        // to water height but this does help with the problem of horizontal range getting limited at bad times.
        float maxDetailY = SeaLevel() - _maxVertDispFromWaves * _dropDetailHeightBasedOnWaves;
        float camDistance = Math.abs(_viewpoint.getPositionY() - maxDetailY);

        // offset level of detail to keep max detail in a band near the surface
        camDistance = Math.max(camDistance - 4f, 0f);

        // scale ocean mesh based on camera distance to sea level, to keep uniform detail.
        final float HEIGHT_LOD_MUL = 1f;
        float level = camDistance * HEIGHT_LOD_MUL;
        level = Math.max(level, _minScale);
        if (_maxScale != -1f) level = Math.min(level, 1.99f * _maxScale);

        float l2 = (float) (Math.log(level) / Math.log(2f));
        float l2f = (float) Math.floor(l2);

        ViewerAltitudeLevelAlpha = l2 - l2f;

        Scale = (float) Math.pow(2f, l2f);
        transform.setScale(Scale, 1f, Scale);
    }

    void LateUpdateViewerHeight()
    {
        _sampleHeightHelper.Init(Viewpoint.position, 0f);

        float waterHeight = 0f;
        _sampleHeightHelper.Sample(ref waterHeight);

        ViewerHeightAboveWater = Viewpoint.position.y - waterHeight;
    }

    void LateUpdateLods()
    {
        // Do any per-frame update for each LOD type.

        _lodTransform.UpdateTransforms();

        if (_lodDataAnimWaves != null) _lodDataAnimWaves.UpdateLodData();
        if (_lodDataDynWaves != null) _lodDataDynWaves.UpdateLodData();
        if (_lodDataFlow != null) _lodDataFlow.UpdateLodData();
        if (_lodDataFoam != null) _lodDataFoam.UpdateLodData();
        if (_lodDataSeaDepths != null) _lodDataSeaDepths.UpdateLodData();
        if (_lodDataShadow != null) _lodDataShadow.UpdateLodData();
    }

    /// <summary>
    /// Could the ocean horizontal scale increase (for e.g. if the viewpoint gains altitude). Will be false if ocean already at maximum scale.
    /// </summary>
    public boolean ScaleCouldIncrease () { return _maxScale == -1f || transform.getScaleX() < _maxScale * 0.99f; }
    /// <summary>
    /// Could the ocean horizontal scale decrease (for e.g. if the viewpoint drops in altitude). Will be false if ocean already at minimum scale.
    /// </summary>
    public boolean ScaleCouldDecrease(){ return _minScale == -1f || transform.getScaleX() > _minScale * 1.01f; }

    /// <summary>
    /// User shape inputs can report in how far they might displace the shape horizontally and vertically. The max value is
    /// saved here. Later the bounding boxes for the ocean tiles will be expanded to account for this potential displacement.
    /// </summary>
    public void ReportMaxDisplacementFromShape(float maxHorizDisp, float maxVertDisp, float maxVertDispFromWaves)
    {
        if (Time.frameCount != _maxDisplacementCachedTime)
        {
            _maxHorizDispFromShape = _maxVertDispFromShape = _maxVertDispFromWaves = 0f;
        }

        MaxHorizDisplacement += maxHorizDisp;
        _maxVertDispFromShape += maxVertDisp;
        _maxVertDispFromWaves += maxVertDispFromWaves;

        _maxDisplacementCachedTime = Time.frameCount;
    }
    float _maxVertDispFromWaves = 0f;
    int _maxDisplacementCachedTime = 0;
    /// <summary>
    /// The maximum horizontal distance that the shape scripts are displacing the shape.
    /// </summary>
    public float MaxHorizDisplacement;
    /// <summary>
    /// The maximum height that the shape scripts are displacing the shape.
    /// </summary>
    public float MaxVertDisplacement ;

    public static OceanRenderer Instance;

    /// <summary>
    /// Provides ocean shape to CPU.
    /// </summary>
    ICollProvider _collProvider;
    public ICollProvider CollisionProvider (){ return _collProvider != null ? _collProvider : (_collProvider = _simSettingsAnimatedWaves.CreateCollisionProvider()); }

    private void OnValidate()
    {
        // Must be at least 0.25, and must be on a power of 2
        _minScale = (float) Math.pow(2f, Math.round(Numeric.log2(Math.max(_minScale, 0.25f))));

        // Max can be -1 which means no maximum
        if (_maxScale != -1f)
        {
            // otherwise must be at least 0.25, and must be on a power of 2
            _maxScale = (float) Math.pow(2f, Math.round(Numeric.log2(Math.max(_maxScale, _minScale))));
        }

        // Gravity 0 makes waves freeze which is weird but doesn't seem to break anything so allowing this for now
        _gravityMultiplier = Math.max(_gravityMultiplier, 0f);

        // LOD data resolution multiple of 2 for general GPU texture reasons (like pixel quads)
        _lodDataResolution -= _lodDataResolution % 2;

        _geometryDownSampleFactor = Numeric.nearestPowerOfTwo(Math.max(_geometryDownSampleFactor, 1));

        int remGeo = _lodDataResolution % _geometryDownSampleFactor;
        if (remGeo > 0)
        {
            int newLDR = _lodDataResolution - (_lodDataResolution % _geometryDownSampleFactor);
            LogUtil.w(LogUtil.LogType.DEFAULT, "Adjusted Lod Data Resolution from " + _lodDataResolution + " to " + newLDR + " to ensure the Geometry Down Sample Factor is a factor (" + _geometryDownSampleFactor + ").");
            _lodDataResolution = newLDR;
        }
    }

//        [UnityEditor.Callbacks.DidReloadScripts]
    private static void OnReLoadScripts()
    {
//        Instance = FindObjectOfType<OceanRenderer>();
    }
}

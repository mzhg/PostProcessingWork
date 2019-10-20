package jet.opengl.demos.nvidia.waves.crest;

import org.lwjgl.util.vector.Transform;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import jet.opengl.demos.amdfx.common.CFirstPersonCamera;
import jet.opengl.demos.nvidia.waves.crest.loddata.LodDataMgrAnimWaves;
import jet.opengl.demos.nvidia.waves.crest.loddata.LodDataMgrFlow;
import jet.opengl.demos.nvidia.waves.crest.loddata.LodDataMgrFoam;
import jet.opengl.demos.nvidia.waves.crest.loddata.LodDataMgrSeaFloorDepth;
import jet.opengl.demos.nvidia.waves.crest.loddata.LodDataMgrShadow;
import jet.opengl.postprocessing.util.BoundingBox;

/** Sets shader parameters for each geometry tile/chunk. */
public class OceanChunkRenderer extends MonoBehaviour {
    public boolean _drawRenderBounds = false;

    BoundingBox _boundsLocal;
    Mesh _mesh;
//    Renderer _rend;
//    PropertyWrapperMPB _mpb;

    // Cache these off to support regenerating ocean surface
    int _lodIndex = -1;
    int _totalLodCount = -1;
    int _lodDataResolution = 256;
    int _geoDownSampleFactor = 1;

    static int sp_ReflectionTex = 0; //Shader.PropertyToID("_ReflectionTex");
    static int sp_InstanceData = 1; //Shader.PropertyToID("_InstanceData");
    static int sp_GeomData = 2; //Shader.PropertyToID("_GeomData");
    static int sp_ForceUnderwater = 3; //Shader.PropertyToID("_ForceUnderwater");

    void Start()
    {
//        _rend = GetComponent<Renderer>();
        _mesh = GetComponent<MeshFilter>().mesh;
        _boundsLocal = _mesh.bounds;

        UpdateMeshBounds();
    }

    private void Update()
    {
        // This needs to be called on Update because the bounds depend on transform scale which can change. Also OnWillRenderObject depends on
        // the bounds being correct. This could however be called on scale change events, but would add slightly more complexity.
        UpdateMeshBounds();
    }

    void UpdateMeshBounds()
    {
//        var newBounds = _boundsLocal;
        ExpandBoundsForDisplacements(transform, _mesh.bounds );
//        _mesh.bounds = newBounds;
    }

    protected void OnEnable()
    {
/*#if UNITY_2018
        RenderPipeline.beginCameraRendering += BeginCameraRendering;
#else
        RenderPipelineManager.beginCameraRendering += BeginCameraRendering;
#endif*/
    }

    protected void OnDisable()
    {
/*#if UNITY_2018
        RenderPipeline.beginCameraRendering -= BeginCameraRendering;
#else
        RenderPipelineManager.beginCameraRendering -= BeginCameraRendering;
#endif*/
    }

    static CFirstPersonCamera _currentCamera = null;

/*#if UNITY_2018
    private void BeginCameraRendering(Camera camera)
#else
    private void BeginCameraRendering(ScriptableRenderContext context, Camera camera)
#endif
    {
        _currentCamera = camera;
    }*/

    // Called when visible to a camera
    void OnWillRenderObject()
    {
        // check if built-in pipeline being used
        /*if (Camera.current != null)
        {
            _currentCamera = Camera.current;
        }

        // Depth texture is used by ocean shader for transparency/depth fog, and for fading out foam at shoreline.
        _currentCamera.depthTextureMode |= DepthTextureMode.Depth;

        if (_rend.sharedMaterial != OceanRenderer.Instance.OceanMaterial)
        {
            _rend.sharedMaterial = OceanRenderer.Instance.OceanMaterial;
        }*/

        // per instance data

        /*if (_mpb == null)
        {
            _mpb = new PropertyWrapperMPB();
        }
        _rend.GetPropertyBlock(_mpb.materialPropertyBlock);*/

        // blend LOD 0 shape in/out to avoid pop, if the ocean might scale up later (it is smaller than its maximum scale)
        boolean needToBlendOutShape = _lodIndex == 0 && OceanRenderer.Instance.ScaleCouldIncrease();
        float meshScaleLerp = needToBlendOutShape ? OceanRenderer.Instance.ViewerAltitudeLevelAlpha : 0f;

        // blend furthest normals scale in/out to avoid pop, if scale could reduce
        boolean needToBlendOutNormals = _lodIndex == _totalLodCount - 1 && OceanRenderer.Instance.ScaleCouldDecrease();
        float farNormalsWeight = needToBlendOutNormals ? OceanRenderer.Instance.ViewerAltitudeLevelAlpha : 1f;
        _mpb.SetVector(sp_InstanceData, new Vector4f(meshScaleLerp, farNormalsWeight, _lodIndex, 0));

        // geometry data
        // compute grid size of geometry. take the long way to get there - make sure we land exactly on a power of two
        // and not inherit any of the lossy-ness from lossyScale.
        float scale_pow_2 = OceanRenderer.Instance.CalcLodScale(_lodIndex);
        float gridSizeGeo = scale_pow_2 / (0.25f * _lodDataResolution / _geoDownSampleFactor);
        float gridSizeLodData = gridSizeGeo / _geoDownSampleFactor;
        float mul = 1.875f; // fudge 1
        float pow = 1.4f; // fudge 2
        float normalScrollSpeed0 = (float) Math.pow(Math.log(1f + 2f * gridSizeLodData) * mul, pow);
        float normalScrollSpeed1 = (float) Math.pow(Math.log(1f + 4f * gridSizeLodData) * mul, pow);
        _mpb.SetVector(sp_GeomData, new Vector4f(gridSizeLodData, gridSizeGeo, normalScrollSpeed0, normalScrollSpeed1));

        // Assign LOD data to ocean shader
        LodDataMgrAnimWaves ldaws = OceanRenderer.Instance._lodDataAnimWaves;
        LodDataMgrSeaFloorDepth ldsds = OceanRenderer.Instance._lodDataSeaDepths;
        LodDataMgrFoam ldfoam = OceanRenderer.Instance._lodDataFoam;
        LodDataMgrFlow ldflow = OceanRenderer.Instance._lodDataFlow;
        LodDataMgrShadow ldshadows = OceanRenderer.Instance._lodDataShadow;

        _mpb.SetFloat(LodDataMgr.sp_LD_SliceIndex, _lodIndex);
        ldaws.BindResultData(_mpb);
        if (ldflow != null) ldflow.BindResultData(_mpb);
        if (ldfoam != null) ldfoam.BindResultData(_mpb); else LodDataMgrFoam.BindNull(_mpb);
        if (ldsds != null) ldsds.BindResultData(_mpb);
        if (ldshadows != null) ldshadows.BindResultData(_mpb); else LodDataMgrShadow.BindNull(_mpb);

        var reflTex = PreparedReflections.GetRenderTexture(_currentCamera.GetHashCode());
        if (reflTex != null)
        {
            _mpb.SetTexture(sp_ReflectionTex, reflTex);
        }
        else
        {
            _mpb.SetTexture(sp_ReflectionTex, Texture2D.blackTexture);
        }

        // Hack - due to SV_IsFrontFace occasionally coming through as true for back faces,
        // add a param here that forces ocean to be in underwater state. I think the root
        // cause here might be imprecision or numerical issues at ocean tile boundaries, although
        // i'm not sure why cracks are not visible in this case.
        float heightOffset = OceanRenderer.Instance.ViewerHeightAboveWater();
        _mpb.SetFloat(sp_ForceUnderwater, heightOffset < -2f ? 1f : 0f);

        _rend.SetPropertyBlock(_mpb.materialPropertyBlock);

        if (_drawRenderBounds)
        {
            _rend.bounds.DebugDraw();
        }
    }

    // this is called every frame because the bounds are given in world space and depend on the transform scale, which
    // can change depending on view altitude
    public static void ExpandBoundsForDisplacements(Transform transform, BoundingBox bounds)
    {
        float boundsPadding = OceanRenderer.Instance.MaxHorizDisplacement;
        float expandXZ = boundsPadding / transform.getScaleX();
        float boundsY = OceanRenderer.Instance.MaxVertDisplacement;
        // extend the kinematic bounds slightly to give room for dynamic sim stuff
        boundsY += 5f;
//        bounds.extents = new Vector3(bounds.extents.x + expandXZ, boundsY / transform.lossyScale.y, bounds.extents.z + expandXZ);
        Vector3f newExtents = Vector3f.sub(bounds._max, bounds._min, null);
        newExtents.scale(0.5f);
        newExtents.set(newExtents.x + expandXZ, boundsY / transform.getScaleY(), newExtents.z + expandXZ);
        Vector3f center = bounds.center(null);
        bounds.setFromExtent(center, newExtents);
    }

    public void SetInstanceData(int lodIndex, int totalLodCount, int lodDataResolution, int geoDownSampleFactor)
    {
        _lodIndex = lodIndex; _totalLodCount = totalLodCount; _lodDataResolution = lodDataResolution; _geoDownSampleFactor = geoDownSampleFactor;
    }
//}

    /*public class BoundsHelper
    {
        public static void DebugDraw(this Bounds b)
        {
            // source: https://github.com/UnityCommunity/UnityLibrary
            // license: mit - https://github.com/UnityCommunity/UnityLibrary/blob/master/LICENSE.md

            // bounding box using Debug.Drawline

            // bottom
            var p1 = new Vector3(b.min.x, b.min.y, b.min.z);
            var p2 = new Vector3(b.max.x, b.min.y, b.min.z);
            var p3 = new Vector3(b.max.x, b.min.y, b.max.z);
            var p4 = new Vector3(b.min.x, b.min.y, b.max.z);

            Debug.DrawLine(p1, p2, Color.blue);
            Debug.DrawLine(p2, p3, Color.red);
            Debug.DrawLine(p3, p4, Color.yellow);
            Debug.DrawLine(p4, p1, Color.magenta);

            // top
            var p5 = new Vector3(b.min.x, b.max.y, b.min.z);
            var p6 = new Vector3(b.max.x, b.max.y, b.min.z);
            var p7 = new Vector3(b.max.x, b.max.y, b.max.z);
            var p8 = new Vector3(b.min.x, b.max.y, b.max.z);

            Debug.DrawLine(p5, p6, Color.blue);
            Debug.DrawLine(p6, p7, Color.red);
            Debug.DrawLine(p7, p8, Color.yellow);
            Debug.DrawLine(p8, p5, Color.magenta);

            // sides
            Debug.DrawLine(p1, p5, Color.white);
            Debug.DrawLine(p2, p6, Color.gray);
            Debug.DrawLine(p3, p7, Color.green);
            Debug.DrawLine(p4, p8, Color.cyan);
        }
    }*/
}

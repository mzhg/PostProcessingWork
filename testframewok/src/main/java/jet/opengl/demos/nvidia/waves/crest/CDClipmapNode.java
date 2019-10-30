package jet.opengl.demos.nvidia.waves.crest;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector4f;
import org.lwjgl.util.vector.Transform;
import org.lwjgl.util.vector.Vector4f;

final class CDClipmapNode {
    int meshIndex = -1;
    final Transform transform = new Transform();
    public Matrix4f debugMatrix;
    // only for the debug
    String name;

    int sortingOrder;

    // Cache these off to support regenerating ocean surface
    int _lodIndex = -1;
    int _totalLodCount = -1;
    int _lodDataResolution = 256;
    int _geoDownSampleFactor = 1;

    private final Vector4f instanceData = new Vector4f();
    private final Vector4f geomData = new Vector4f();

    public ReadableVector4f getInstanceData(){ return instanceData;}
    public ReadableVector4f getGeomData()    { return geomData;}

    CDClipmapNode(String name){
        this.name = name;
    }

    public void update(Wave_CDClipmap waveClipmap){
        // blend LOD 0 shape in/out to avoid pop, if the ocean might scale up later (it is smaller than its maximum scale)
        boolean needToBlendOutShape = _lodIndex == 0 && waveClipmap.scaleCouldIncrease();
        float meshScaleLerp = needToBlendOutShape ? waveClipmap.getViewerAltitudeLevelAlpha() : 0f;

        // blend furthest normals scale in/out to avoid pop, if scale could reduce
        boolean needToBlendOutNormals = _lodIndex == _totalLodCount - 1 && waveClipmap.scaleCouldDecrease();
        float farNormalsWeight = needToBlendOutNormals ? waveClipmap.getViewerAltitudeLevelAlpha() : 1f;
//        _mpb.SetVector(sp_InstanceData, new Vector4(meshScaleLerp, farNormalsWeight, _lodIndex));
        instanceData.set(meshScaleLerp, farNormalsWeight, _lodIndex, 0);

        // geometry data
        // compute grid size of geometry. take the long way to get there - make sure we land exactly on a power of two
        // and not inherit any of the lossy-ness from lossyScale.
        float scale_pow_2 = waveClipmap.calcLodScale(_lodIndex);
        float gridSizeGeo = scale_pow_2 / (0.25f * _lodDataResolution / _geoDownSampleFactor);
        float gridSizeLodData = gridSizeGeo / _geoDownSampleFactor;
        float mul = 1.875f; // fudge 1
        float pow = 1.4f; // fudge 2
        float normalScrollSpeed0 = (float) Math.pow(Math.log(1f + 2f * gridSizeLodData) * mul, pow);
        float normalScrollSpeed1 = (float) Math.pow(Math.log(1f + 4f * gridSizeLodData) * mul, pow);
//        _mpb.SetVector(sp_GeomData, new Vector4(gridSizeLodData, gridSizeGeo, normalScrollSpeed0, normalScrollSpeed1));
        geomData.set(gridSizeLodData, gridSizeGeo, normalScrollSpeed0, normalScrollSpeed1);

        // Assign LOD data to ocean shader
        /*var ldaws = waveClipmap._lodDataAnimWaves;
        var ldsds = waveClipmap._lodDataSeaDepths;
        var ldfoam = waveClipmap._lodDataFoam;
        var ldflow = waveClipmap._lodDataFlow;
        var ldshadows = waveClipmap._lodDataShadow;

        _mpb.SetFloat(LodDataMgr.sp_LD_SliceIndex, _lodIndex);
        ldaws.BindResultData(_mpb);
        if (ldflow) ldflow.BindResultData(_mpb);
        if (ldfoam) ldfoam.BindResultData(_mpb); else LodDataMgrFoam.BindNull(_mpb);
        if (ldsds) ldsds.BindResultData(_mpb);
        if (ldshadows) ldshadows.BindResultData(_mpb); else LodDataMgrShadow.BindNull(_mpb);

        var reflTex = PreparedReflections.GetRenderTexture(_currentCamera.GetHashCode());
        if (reflTex)
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
        var heightOffset = waveClipmap.ViewerHeightAboveWater;
        _mpb.SetFloat(sp_ForceUnderwater, heightOffset < -2f ? 1f : 0f);*/
    }

}

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

    public void getData(Wave_CDClipmap waveClipmap, Wave_Simulation simulation, Wave_Shading_ShaderData shaderData){
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
        if(simulation != null){
            Wave_Simulation_Pass ldaws = simulation._lodDataAnimWaves;
            Wave_Simulation_Pass ldsds = simulation._lodDataSeaDepths;
            Wave_Simulation_Pass ldfoam = simulation._lodDataFoam;
            Wave_Simulation_Pass ldflow = simulation._lodDataFlow;
            Wave_Simulation_Pass ldshadows = simulation._lodDataShadow;

//            _mpb.SetFloat(LodDataMgr.sp_LD_SliceIndex, _lodIndex);
            shaderData._LD_SliceIndex = _lodIndex;

            ldaws.BindResultData(shaderData);
            if (ldflow!=null) ldflow.BindResultData(shaderData);
            if (ldfoam!=null) ldfoam.BindResultData(shaderData); else Wave_Simulation_Foam_Pass.BindNull(shaderData, false);
            if (ldsds!=null) ldsds.BindResultData(shaderData);
            if (ldshadows!=null) ldshadows.BindResultData(shaderData); else Wave_Simulation_Shadow_Pass.BindNull(shaderData, false);

            /*var reflTex = PreparedReflections.GetRenderTexture(_currentCamera.GetHashCode());  todo should not here.
            if (reflTex)
            {
                _mpb.SetTexture(sp_ReflectionTex, reflTex);
            }
            else
            {
                _mpb.SetTexture(sp_ReflectionTex, Texture2D.blackTexture);
            }*/

            // Hack - due to SV_IsFrontFace occasionally coming through as true for back faces,
            // add a param here that forces ocean to be in underwater state. I think the root
            // cause here might be imprecision or numerical issues at ocean tile boundaries, although
            // i'm not sure why cracks are not visible in this case.
            float heightOffset = waveClipmap.getViewerHeightAboveWater();
//            _mpb.SetFloat(sp_ForceUnderwater, heightOffset < -2f ? 1f : 0f);
            shaderData._ForceUnderwater = heightOffset < -2f ? 1f : 0f;
        }
    }

}

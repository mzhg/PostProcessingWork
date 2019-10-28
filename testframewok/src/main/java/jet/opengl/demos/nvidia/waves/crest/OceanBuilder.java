package jet.opengl.demos.nvidia.waves.crest;

import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import java.util.ArrayList;

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
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.BoundingBox;
import jet.opengl.postprocessing.util.LogUtil;
import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.postprocessing.util.StackInt;

/** Instantiates all the ocean geometry, as a set of tiles. */
public class OceanBuilder {

    /**
     * The comments below illustrate case when BASE_VERT_DENSITY = 2. The ocean mesh is built up from these patches. Rotational symmetry
     * is used where possible to eliminate combinations. The slim variants are used to eliminate overlap between patches.
     */
    enum PatchType
    {
        /// <summary>
        /// Adds no skirt. Used in interior of highest detail LOD (0)
        ///
        ///    1 -------
        ///      |  |  |
        ///  z   -------
        ///      |  |  |
        ///    0 -------
        ///      0     1
        ///         x
        ///
        /// </summary>
        Interior,

        /// <summary>
        /// Adds a full skirt all of the way around a patch
        ///
        ///      -------------
        ///      |  |  |  |  |
        ///    1 -------------
        ///      |  |  |  |  |
        ///  z   -------------
        ///      |  |  |  |  |
        ///    0 -------------
        ///      |  |  |  |  |
        ///      -------------
        ///         0     1
        ///            x
        ///
        /// </summary>
        Fat,

        /// <summary>
        /// Adds a skirt on the right hand side of the patch
        ///
        ///    1 ----------
        ///      |  |  |  |
        ///  z   ----------
        ///      |  |  |  |
        ///    0 ----------
        ///      0     1
        ///         x
        ///
        /// </summary>
        FatX,

        /// <summary>
        /// Adds a skirt on the right hand side of the patch, removes skirt from top
        /// </summary>
        FatXSlimZ,

        /// <summary>
        /// Outer most side - this adds an extra skirt on the left hand side of the patch,
        /// which will point outwards and be extended to Zfar
        ///
        ///    1 --------------------------------------------------------------------------------------
        ///      |  |  |                                                                              |
        ///  z   --------------------------------------------------------------------------------------
        ///      |  |  |                                                                              |
        ///    0 --------------------------------------------------------------------------------------
        ///      0     1
        ///         x
        ///
        /// </summary>
        FatXOuter,

        /// <summary>
        /// Adds skirts at the top and right sides of the patch
        /// </summary>
        FatXZ,

        /// <summary>
        /// Adds skirts at the top and right sides of the patch and pushes them to horizon
        /// </summary>
        FatXZOuter,

        /// <summary>
        /// One less set of verts in x direction
        /// </summary>
        SlimX,

        /// <summary>
        /// One less set of verts in both x and z directions
        /// </summary>
        SlimXZ,

        /// <summary>
        /// One less set of verts in x direction, extra verts at start of z direction
        ///
        ///      ----
        ///      |  |
        ///    1 ----
        ///      |  |
        ///  z   ----
        ///      |  |
        ///    0 ----
        ///      0     1
        ///         x
        ///
        /// </summary>
        SlimXFatZ,

        /// <summary>
        /// Number of patch types
        /// </summary>
//        Count,
    }

    public static void GenerateMesh(OceanRenderer ocean, int lodDataResolution, int geoDownSampleFactor, int lodCount)
    {
        if (lodCount < 1)
        {
            LogUtil.e(LogUtil.LogType.DEFAULT, ()->"Invalid LOD count: " + lodCount);
            return;
        }

/*#if UNITY_EDITOR
        if (!UnityEditor.EditorApplication.isPlaying)
        {
            Debug.LogError("Ocean mesh meant to be (re)generated in play mode", ocean);
            return;
        }
#endif*/

        int oceanLayer = 0 ;//LayerMask.NameToLayer(ocean.LayerName);
        /*if (oceanLayer == -1)
        {
            Debug.LogError("Invalid ocean layer: " + ocean.LayerName + " please add this layer.", ocean);
            oceanLayer = 0;
        }*/

/*#if PROFILE_CONSTRUCTION
        System.Diagnostics.Stopwatch sw = new System.Diagnostics.Stopwatch();
        sw.Start();
#endif*/

        // create mesh data
        Mesh[] meshInsts = new Mesh[PatchType.values().length];
        // 4 tiles across a LOD, and support lowering density by a factor
        int tileResolution = Math.round(0.25f * lodDataResolution / geoDownSampleFactor);
        for (int i = 0; i < meshInsts.length; i++)
        {
            meshInsts[i] = BuildOceanPatch(PatchType.values()[i], tileResolution);
        }

        ocean._lodTransform = new LodTransform(); //ocean.gameObject.AddComponent<LodTransform>();
        ocean._lodTransform.InitLODData(lodCount);

        // Create the LOD data managers
//        ocean._lodDataAnimWaves = LodDataMgr.Create<LodDataMgrAnimWaves, SimSettingsAnimatedWaves>(ocean.gameObject, ref ocean._simSettingsAnimatedWaves);
        ocean._simSettingsAnimatedWaves = new SimSettingsAnimatedWaves();
        ocean._lodDataAnimWaves = new LodDataMgrAnimWaves();
        ocean._lodDataAnimWaves.UseSettings(ocean._simSettingsAnimatedWaves);
        if (ocean.CreateDynamicWaveSim)
        {
//            ocean._lodDataDynWaves = LodDataMgr.Create<LodDataMgrDynWaves, SimSettingsWave>(ocean.gameObject, ref ocean._simSettingsDynamicWaves);
            ocean._simSettingsDynamicWaves = new SimSettingsWave();
            ocean._lodDataDynWaves = new LodDataMgrDynWaves();
            ocean._lodDataDynWaves.UseSettings(ocean._simSettingsDynamicWaves);
        }
        if (ocean.CreateFlowSim)
        {
//            ocean._lodDataFlow = LodDataMgr.Create<LodDataMgrFlow, SimSettingsFlow>(ocean.gameObject, ref ocean._simSettingsFlow);
            ocean._lodDataFlow = new LodDataMgrFlow();
        }
        if (ocean.CreateFoamSim)
        {
//            ocean._lodDataFoam = LodDataMgr.Create<LodDataMgrFoam, SimSettingsFoam>(ocean.gameObject, ref ocean._simSettingsFoam);
            ocean._simSettingsFoam = new SimSettingsFoam();
            ocean._lodDataFoam = new LodDataMgrFoam();
            ocean._lodDataFoam.UseSettings(ocean._simSettingsFoam);
        }
        if (ocean.CreateShadowData)
        {
//            ocean._lodDataShadow = LodDataMgr.Create<LodDataMgrShadow, SimSettingsShadow>(ocean.gameObject, ref ocean._simSettingsShadow);
            ocean._simSettingsShadow = new SimSettingsShadow();
            ocean._lodDataShadow = new LodDataMgrShadow();
            ocean._lodDataShadow.UseSettings(ocean._simSettingsShadow);
        }
        if (ocean.CreateSeaFloorDepthData)
        {
//            ocean._lodDataSeaDepths = ocean.gameObject.AddComponent<LodDataMgrSeaFloorDepth>();
            ocean._lodDataSeaDepths = new LodDataMgrSeaFloorDepth();
        }

        // Add any required GPU readbacks
        {
            SimSettingsAnimatedWaves ssaw = ocean._simSettingsAnimatedWaves;
            if (ssaw != null && ssaw.CollisionSource == SimSettingsAnimatedWaves.CollisionSources.OceanDisplacementTexturesGPU)
            {
//                ocean.gameObject.AddComponent<GPUReadbackDisps>();  todo
            }
            if (ssaw != null && ssaw.CollisionSource == SimSettingsAnimatedWaves.CollisionSources.ComputeShaderQueries)
            {
//                ocean.gameObject.AddComponent<QueryDisplacements>();  todo
            }

            if (ocean.CreateFlowSim)
            {
//                ocean.gameObject.AddComponent<QueryFlow>();  todo
            }
        }

        // Remove existing LODs
        /*for (int i = 0; i < ocean.transform.childCount; i++)
        {
            var child = ocean.transform.GetChild(i);
            if (child.name.StartsWith("Tile_L"))
            {
                child.parent = null;
                Object.Destroy(child.gameObject);
                i--;
            }
        }*/

        for (int i = 0; i < lodCount; i++)
        {
            CreateLOD(ocean, i, lodCount, meshInsts, lodDataResolution, geoDownSampleFactor, oceanLayer);
        }

/*#if PROFILE_CONSTRUCTION
        sw.Stop();
        Debug.Log( "Finished generating " + parms._lodCount.ToString() + " LODs, time: " + (1000.0*sw.Elapsed.TotalSeconds).ToString(".000") + "ms" );
#endif*/
    }

    static Mesh BuildOceanPatch(PatchType pt, float vertDensity)
    {
        ArrayList<Vector3f> verts = new ArrayList();
        StackInt indices = new StackInt();

        // stick a bunch of verts into a 1m x 1m patch (scaling happens later)
        float dx = 1f / vertDensity;


        //////////////////////////////////////////////////////////////////////////////////
        // verts

        // see comments within PatchType for diagrams of each patch mesh

        // skirt widths on left, right, bottom and top (in order)
        float skirtXminus = 0f, skirtXplus = 0f;
        float skirtZminus = 0f, skirtZplus = 0f;
        // set the patch size
        if (pt == PatchType.Fat) { skirtXminus = skirtXplus = skirtZminus = skirtZplus = 1f; }
        else if (pt == PatchType.FatX || pt == PatchType.FatXOuter) { skirtXplus = 1f; }
        else if (pt == PatchType.FatXZ || pt == PatchType.FatXZOuter) { skirtXplus = skirtZplus = 1f; }
        else if (pt == PatchType.FatXSlimZ) { skirtXplus = 1f; skirtZplus = -1f; }
        else if (pt == PatchType.SlimX) { skirtXplus = -1f; }
        else if (pt == PatchType.SlimXZ) { skirtXplus = skirtZplus = -1f; }
        else if (pt == PatchType.SlimXFatZ) { skirtXplus = -1f; skirtZplus = 1f; }

        float sideLength_verts_x = 1f + vertDensity + skirtXminus + skirtXplus;
        float sideLength_verts_z = 1f + vertDensity + skirtZminus + skirtZplus;

        float start_x = -0.5f - skirtXminus * dx;
        float start_z = -0.5f - skirtZminus * dx;
        float end_x = 0.5f + skirtXplus * dx;
        float end_z = 0.5f + skirtZplus * dx;

        for (float j = 0; j < sideLength_verts_z; j++)
        {
            // interpolate z across patch
            float z = Numeric.mix(start_z, end_z, j / (sideLength_verts_z - 1f));

            // push outermost edge out to horizon
            if (pt == PatchType.FatXZOuter && j == sideLength_verts_z - 1f)
                z *= 100f;

            for (float i = 0; i < sideLength_verts_x; i++)
            {
                // interpolate x across patch
                float x = Numeric.mix(start_x, end_x, i / (sideLength_verts_x - 1f));

                // push outermost edge out to horizon
                if (i == sideLength_verts_x - 1f && (pt == PatchType.FatXOuter || pt == PatchType.FatXZOuter))
                    x *= 100f;

                // could store something in y, although keep in mind this is a shared mesh that is shared across multiple lods
                verts.add(new Vector3f(x, 0f, z));
            }
        }


        //////////////////////////////////////////////////////////////////////////////////
        // indices

        int sideLength_squares_x = (int)sideLength_verts_x - 1;
        int sideLength_squares_z = (int)sideLength_verts_z - 1;

        for (int j = 0; j < sideLength_squares_z; j++)
        {
            for (int i = 0; i < sideLength_squares_x; i++)
            {
                boolean flipEdge = false;

                if (i % 2 == 1) flipEdge = !flipEdge;
                if (j % 2 == 1) flipEdge = !flipEdge;

                int i0 = i + j * (sideLength_squares_x + 1);
                int i1 = i0 + 1;
                int i2 = i0 + (sideLength_squares_x + 1);
                int i3 = i2 + 1;

                if (!flipEdge)
                {
                    // tri 1
                    indices.push(i3);
                    indices.push(i1);
                    indices.push(i0);

                    // tri 2
                    indices.push(i0);
                    indices.push(i2);
                    indices.push(i3);
                }
                else
                {
                    // tri 1
                    indices.push(i3);
                    indices.push(i1);
                    indices.push(i2);

                    // tri 2
                    indices.push(i0);
                    indices.push(i2);
                    indices.push(i1);
                }
            }
        }


        //////////////////////////////////////////////////////////////////////////////////
        // create mesh

        Mesh mesh = new Mesh();
        if (verts.size() > 0)
        {
            Vector3f[] arrV = new Vector3f[verts.size()];
            verts.toArray(arrV);

//            int[] arrI = new int[indices.size()];
            int[] arrI = indices.toArray();

//            mesh.SetIndices(null, MeshTopology.Triangles, 0);
            mesh.vertices = arrV;
//            mesh.normals = null;
            mesh.SetIndices(arrI, /*MeshTopology.Triangles*/ GLenum.GL_TRIANGLES);

            // recalculate bounds. add a little allowance for snapping. in the chunk renderer script, the bounds will be expanded further
            // to allow for horizontal displacement
            mesh.RecalculateBounds();
            BoundingBox bounds = mesh.bounds;
//            bounds.extents = new Vector3(bounds.extents.x + dx, 100f, bounds.extents.z + dx);
            Vector3f newExtents = Vector3f.sub(bounds._max, bounds._min, null);
            newExtents.scale(0.5f);
            newExtents.set(newExtents.x + dx, 100f, newExtents.z + dx);
            Vector3f center = bounds.center(null);
            bounds.setFromExtent(center, newExtents);

//            mesh.bounds = bounds;
            mesh.name = pt.toString();
        }
        return mesh;
    }

    static void CreateLOD(OceanRenderer ocean, int lodIndex, int lodCount, Mesh[] meshData, int lodDataResolution, int geoDownSampleFactor, int oceanLayer)
    {
        float horizScale = (float) Math.pow(2f, lodIndex);

        boolean isBiggestLOD = lodIndex == lodCount - 1;
        boolean generateSkirt = isBiggestLOD && !ocean._disableSkirt;

        Vector2f[] offsets;
        PatchType[] patchTypes;

        PatchType leadSideType = generateSkirt ? PatchType.FatXOuter : PatchType.SlimX;
        PatchType trailSideType = generateSkirt ? PatchType.FatXOuter : PatchType.FatX;
        PatchType leadCornerType = generateSkirt ? PatchType.FatXZOuter : PatchType.SlimXZ;
        PatchType trailCornerType = generateSkirt ? PatchType.FatXZOuter : PatchType.FatXZ;
        PatchType tlCornerType = generateSkirt ? PatchType.FatXZOuter : PatchType.SlimXFatZ;
        PatchType brCornerType = generateSkirt ? PatchType.FatXZOuter : PatchType.FatXSlimZ;

        if (lodIndex != 0)
        {
            // instance indices:
            //    0  1  2  3
            //    4        5
            //    6        7
            //    8  9  10 11
            offsets = new Vector2f[] {
                    new Vector2f(-1.5f,1.5f),    new Vector2f(-0.5f,1.5f), new Vector2f(0.5f,1.5f), new Vector2f(1.5f,1.5f),
                    new Vector2f(-1.5f,0.5f),                                                            new Vector2f(1.5f,0.5f),
                    new Vector2f(-1.5f,-0.5f),                                                           new Vector2f(1.5f,-0.5f),
                    new Vector2f(-1.5f,-1.5f),   new Vector2f(-0.5f,-1.5f),   new Vector2f(0.5f,-1.5f),    new Vector2f(1.5f,-1.5f),
            };

            // usually rings have an extra side of verts that point inwards. the outermost ring has both the inward
            // verts and also and additional outwards set of verts that go to the horizon
            patchTypes = new PatchType[] {
                    tlCornerType,         leadSideType,           leadSideType,         leadCornerType,
                    trailSideType,                                                      leadSideType,
                    trailSideType,                                                      leadSideType,
                    trailCornerType,      trailSideType,          trailSideType,        brCornerType,
            };
        }
        else
        {
            // first LOD has inside bit as well:
            //    0  1  2  3
            //    4  5  6  7
            //    8  9  10 11
            //    12 13 14 15
            offsets = new Vector2f[] {
                    new Vector2f(-1.5f,1.5f),    new Vector2f(-0.5f,1.5f),    new Vector2f(0.5f,1.5f),     new Vector2f(1.5f,1.5f),
                    new Vector2f(-1.5f,0.5f),    new Vector2f(-0.5f,0.5f),    new Vector2f(0.5f,0.5f),     new Vector2f(1.5f,0.5f),
                    new Vector2f(-1.5f,-0.5f),   new Vector2f(-0.5f,-0.5f),   new Vector2f(0.5f,-0.5f),    new Vector2f(1.5f,-0.5f),
                    new Vector2f(-1.5f,-1.5f),   new Vector2f(-0.5f,-1.5f),   new Vector2f(0.5f,-1.5f),    new Vector2f(1.5f,-1.5f),
            };


            // all interior - the "side" types have an extra skirt that points inwards - this means that this inner most
            // section doesn't need any skirting. this is good - this is the highest density part of the mesh.
            patchTypes = new PatchType[] {
                    tlCornerType,       leadSideType,           leadSideType,           leadCornerType,
                    trailSideType,      PatchType.Interior,     PatchType.Interior,     leadSideType,
                    trailSideType,      PatchType.Interior,     PatchType.Interior,     leadSideType,
                    trailCornerType,    trailSideType,          trailSideType,          brCornerType,
            };
        }

        // debug toggle to force all patches to be the same. they'll be made with a surrounding skirt to make sure patches
        // overlap
        if (ocean._uniformTiles)
        {
            for (int i = 0; i < patchTypes.length; i++)
            {
                patchTypes[i] = PatchType.Fat;
            }
        }

        // create the ocean patches
        for (int i = 0; i < offsets.length; i++)
        {
            // instantiate and place patch
            GameObject patch = new GameObject(String.format("Tile_L{%d}", lodIndex));
            patch.layer = oceanLayer;
            patch.parent = ocean.transform;
            Vector2f pos = offsets[i];
//            patch.transform.localPosition = horizScale * new Vector3(pos.x, 0f, pos.y);
            patch.transform.setPosition(horizScale * pos.x, 0, horizScale * pos.y);
            // scale only horizontally, otherwise culling bounding box will be scaled up in y
            patch.transform.setScale(horizScale, 1f, horizScale);

            patch.renderer = new OceanChunkRenderer();
            patch.renderer.SetInstanceData(lodIndex, lodCount, lodDataResolution, geoDownSampleFactor);
            patch.mesh = meshData[patchTypes[i].ordinal()];

//            var mr = patch.AddComponent<MeshRenderer>();

            // Sorting order to stop unity drawing it back to front. make the innermost 4 tiles draw first, followed by
            // the rest of the tiles by LOD index. all this happens before layer 0 - the sorting layer takes priority over the
            // render queue it seems! ( https://cdry.wordpress.com/2017/04/28/unity-render-queues-vs-sorting-layers/ ). This pushes
            // ocean rendering way early, so transparent objects will by default render afterwards, which is typical for water rendering.
            patch.sortingOrder = -lodCount + (patchTypes[i] == PatchType.Interior ? -1 : lodIndex);

            // I don't think one would use light probes for a purely specular water surface? (although diffuse foam shading would benefit)
//            mr.lightProbeUsage = UnityEngine.Rendering.LightProbeUsage.Off;
//            mr.shadowCastingMode = UnityEngine.Rendering.ShadowCastingMode.Off; // arbitrary - could be turned on if desired
//            mr.receiveShadows = false; // this setting is ignored by unity for the transparent ocean shader
//            mr.motionVectorGenerationMode = MotionVectorGenerationMode.ForceNoMotion;
//            patch.material = ocean.OceanMaterial;

            // rotate side patches to point the +x side outwards
            boolean rotateXOutwards = patchTypes[i] == PatchType.FatX || patchTypes[i] == PatchType.FatXOuter || patchTypes[i] == PatchType.SlimX || patchTypes[i] == PatchType.SlimXFatZ;
            if (rotateXOutwards)
            {
                if (Math.abs(pos.y) >= Math.abs(pos.x)) {
//                    patch.transform.localEulerAngles = -Vector3.up * 90f * Mathf.Sign(pos.y);
                    Quaternion rot = new Quaternion();
                    rot.setFromAxisAngle(0, 1, 0, (float)Math.toRadians(-90 * Math.signum(pos.y)));
                    patch.transform.setRotation(rot);
                }else {
//                    patch.transform.localEulerAngles = pos.x < 0f ? Vector3.up * 180f : Vector3.zero;
                    Quaternion rot = new Quaternion();

                    if(pos.x < 0f)
                        rot.setFromAxisAngle(0, 1, 0, (float)Math.toRadians(180f));
                    patch.transform.setRotation(rot);
                }
            }

            // rotate the corner patches so the +x and +z sides point outwards
            boolean rotateXZOutwards = patchTypes[i] == PatchType.FatXZ || patchTypes[i] == PatchType.SlimXZ || patchTypes[i] == PatchType.FatXSlimZ || patchTypes[i] == PatchType.FatXZOuter;
            if (rotateXZOutwards)
            {
                // xz direction before rotation
                Vector3f from = new Vector3f(1f, 0f, 1f);
                from.normalise();
                // target xz direction is outwards vector given by local patch position - assumes this patch is a corner (checked below)
                Vector3f to = patch.transform.getPosition(null);
                to.normalise();
                if (Math.abs(patch.transform.getPositionX()) < 0.0001f || Math.abs(Math.abs(patch.transform.getPositionX()) - Math.abs(patch.transform.getPositionZ())) > 0.001f)
                {
                    LogUtil.w(LogUtil.LogType.DEFAULT, "Skipped rotating a patch because it isn't a corner, click here to highlight.");
                    continue;
                }

                // Detect 180 degree rotations as it doesn't always rotate around Y
                if (Vector3f.dot(from, to) < -0.99f) {
                    Quaternion rot = new Quaternion();
                    rot.setFromAxisAngle(0, 1, 0, (float)Math.toRadians(180));
//                    patch.transform.localEulerAngles = Vector3.up * 180f;
                    patch.transform.setRotation(rot);
                }else {
//                    patch.transform.localRotation = Quaternion.FromToRotation(from, to);
                    Quaternion rot = new Quaternion();
                    Vector3f axis = Vector3f.cross(from, to, null);
                    float angle = (float)Math.acos(Vector3f.dot(from, to));
                    rot.setFromAxisAngle(axis, angle);
                    patch.transform.setRotation(rot);
                }
            }
        }
    }
}

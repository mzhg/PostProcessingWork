package jet.opengl.demos.nvidia.waves.crest;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import jet.opengl.postprocessing.buffer.AttribDesc;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.GLSLUtil;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.CommonUtil;
import jet.opengl.postprocessing.util.DebugTools;
import jet.opengl.postprocessing.util.LogUtil;
import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.postprocessing.util.StackInt;

public class Wave_CDClipmap {

    private final Wave_CDClipmap_Params m_Params = new Wave_CDClipmap_Params();
    Wave_LOD_Transform m_LodTransform;
    private final ArrayList<CDClipmapNode> m_QuadNodes = new ArrayList<>();

    private final Vector3f m_EyePos = new Vector3f();

    private Wave_Mesh[] m_Meshes = new Wave_Mesh[PatchType.values().length];

    private float m_Scale = 1;

    private float viewerAltitudeLevelAlpha;
    private float viewerHeightAboveWater;

    private float m_MaxHorizDispFromShape = 0f;
    private float m_MaxVertDispFromShape = 0f;
    private float m_MaxVertDispFromWaves = 0f;

    public void init(Wave_CDClipmap_Params params){
        m_Params.set(params);
        m_LodTransform = new Wave_LOD_Transform(this);
        validParams();
        generateMesh();
    }

    private void validParams(){
        // Must be at least 0.25, and must be on a power of 2
        m_Params.minScale = (float) Math.pow(2f, Math.round(Numeric.log2(Math.max(m_Params.minScale, 0.25f))));

        // Max can be -1 which means no maximum
        if (m_Params.maxScale != -1f)
        {
            // otherwise must be at least 0.25, and must be on a power of 2
            m_Params.maxScale = (float) Math.pow(2f, Math.round(Numeric.log2(Math.max(m_Params.maxScale, m_Params.minScale))));
        }

        // Gravity 0 makes waves freeze which is weird but doesn't seem to break anything so allowing this for now
//        m_Params.gravityMultiplier = Mathf.Max(m_Params.gravityMultiplier, 0f);

        // LOD data resolution multiple of 2 for general GPU texture reasons (like pixel quads)
        m_Params.lodDataResolution -= m_Params.lodDataResolution % 2;

//        m_Params.geometryDownSampleFactor = Numeric.nearestPowerOfTwo(Math.max(m_Params.geometryDownSampleFactor, 1));

        int remGeo = m_Params.lodDataResolution % m_Params.geometryDownSampleFactor;
        if (remGeo > 0)
        {
            int newLDR = m_Params.lodDataResolution - (m_Params.lodDataResolution % m_Params.geometryDownSampleFactor);
//            Debug.LogWarning("Adjusted Lod Data Resolution from " + _lodDataResolution + " to " + newLDR + " to ensure the Geometry Down Sample Factor is a factor (" + _geometryDownSampleFactor + ").", this);
            m_Params.lodDataResolution = newLDR;
        }
    }

    public int getLodDataResolution() { return m_Params.lodDataResolution;}
    public float getMinTexelsPerWave() { return m_Params.minTexelsPerWave;}

    private void generateMesh(){
        // 4 tiles across a LOD, and support lowering density by a factor
        int tileResolution = Math.round(0.25f * m_Params.lodDataResolution / m_Params.geometryDownSampleFactor);
        for (int i = 0; i < m_Meshes.length; i++)
        {
            CommonUtil.safeRelease(m_Meshes[i]);
            m_Meshes[i] = BuildOceanPatch(PatchType.values()[i], tileResolution);
        }

        m_LodTransform.InitLODData(m_Params.lodCount);

        for (int i = 0; i < m_Params.lodCount; i++)
        {
            createLOD(m_Params.disableSkirt, i,m_Params.lodCount, m_Meshes, m_Params.lodDataResolution, m_Params.geometryDownSampleFactor, m_Params.uniformTiles);
        }
    }

    private static Wave_Mesh BuildOceanPatch(PatchType pt, float vertDensity)
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

        float xMin = 10000f;
        float xMax = -xMin;
        float zMin = 10000f;
        float zMax = -xMin;
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

                xMin = Math.min(x, xMin);
                zMin = Math.min(z, zMin);
                xMax = Math.max(x, xMax);
                zMax = Math.max(z, zMax);
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

        Wave_Mesh mesh = null;
        if (verts.size() > 0)
        {
            AttribDesc attribute_descs[] =
            {
                new AttribDesc(0, 3, GLenum.GL_FLOAT, false, 0, 0)	// vPos
            };

            BufferGL vertexBuffer = new BufferGL();
            vertexBuffer.initlize(GLenum.GL_ARRAY_BUFFER, Vector3f.SIZE*verts.size(), CacheBuffer.wrap(verts), GLenum.GL_STATIC_DRAW);

            BufferGL indexBuffer = new BufferGL();
            indexBuffer.initlize(GLenum.GL_ELEMENT_ARRAY_BUFFER, 4*indices.size(), CacheBuffer.wrap(indices.getData(),0, indices.size()), GLenum.GL_STATIC_DRAW);

            mesh = new Wave_Mesh(attribute_descs, vertexBuffer, indexBuffer, 0);
            mesh.setIndice(GLenum.GL_TRIANGLES, indices.size());
        }
        return mesh;
    }

    private void createLOD(boolean disableSkirt, int lodIndex, int lodCount, Wave_Mesh[] meshData, int lodDataResolution, int geoDownSampleFactor, boolean uniformTiles)
    {
        float horizScale = (float) Math.pow(2f, lodIndex);

        boolean isBiggestLOD = lodIndex == lodCount - 1;
        boolean generateSkirt = isBiggestLOD && !disableSkirt;

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
        if (uniformTiles)
        {
            for (int i = 0; i < patchTypes.length; i++)
            {
                patchTypes[i] = PatchType.Fat;
            }
        }

        ByteBuffer debugMatrixs = DebugTools.loadBinary(String.format("E:\\textures\\Mat_Lod%d.dat", lodIndex));
        if(lodIndex > 7)
            throw new UnsupportedOperationException("invalid lod");

        // create the ocean patches
        for (int i = 0; i < offsets.length; i++)
        {
            // instantiate and place patch
            CDClipmapNode patch = new CDClipmapNode(String.format("Tile_L{%d}", lodIndex));
            Vector2f pos = offsets[i];
//            patch.transform.localPosition = horizScale * new Vector3(pos.x, 0f, pos.y);
            patch.transform.setPosition(horizScale * pos.x, 0, horizScale * pos.y);
            // scale only horizontally, otherwise culling bounding box will be scaled up in y
            patch.transform.setScale(horizScale, 1f, horizScale);

//            patch.renderer = new OceanChunkRenderer();
//            patch.renderer.SetInstanceData(lodIndex, lodCount, lodDataResolution, geoDownSampleFactor);
//            patch.mesh = meshData[patchTypes[i].ordinal()];
            patch.meshIndex = patchTypes[i].ordinal();
            patch._lodDataResolution = lodDataResolution;
            patch._lodIndex = lodIndex;
            patch._totalLodCount = lodCount;
            patch._geoDownSampleFactor = geoDownSampleFactor;

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
                    rot.setFromEulerAngles(0,-(float)Math.toRadians(90 * Sign(pos.y)),0);
                    patch.transform.setRotation(rot);
                }else {
//                    patch.transform.localEulerAngles = pos.x < 0f ? Vector3.up * 180f : Vector3.zero;
                    Quaternion rot = new Quaternion();

                    if(pos.x < 0f)
                        rot.setFromEulerAngles(0, (float)Math.toRadians(180), 0);
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
//                patch.transform.setPosition(to.x, to.y, to.z);
                if (Math.abs(patch.transform.getPositionX()) < 0.0001f || Math.abs(Math.abs(patch.transform.getPositionX()) - Math.abs(patch.transform.getPositionZ())) > 0.001f)
                {
                    LogUtil.w(LogUtil.LogType.DEFAULT, "Skipped rotating a patch because it isn't a corner, click here to highlight.");
                    continue;
                }

                // Detect 180 degree rotations as it doesn't always rotate around Y

                if (Vector3f.dot(from, to) < -0.99f) {
                    Quaternion rot = new Quaternion();
                    rot.setFromEulerAngles(0, (float)Math.toRadians(180), 0);
//                    patch.transform.localEulerAngles = Vector3.up * 180f;
                    patch.transform.setRotation(rot);
                }else {
//                    patch.transform.localRotation = Quaternion.FromToRotation(from, to);
                    Quaternion rot = new Quaternion();
                    rot.fromToRotation(from, to);
                    patch.transform.setRotation(rot);
                }
            }

            if(debugMatrixs != null) {
                Matrix4f debugMat = new Matrix4f();
                debugMat.load(debugMatrixs);
                patch.debugMatrix = debugMat;
            }

            m_QuadNodes.add(patch);
        }
    }

    private static float Sign(float f) { return f>=0 ? 1:-1;}

    public void updateWave(Matrix4f cameraView) {

        m_MaxHorizDispFromShape = 0;
        m_MaxVertDispFromShape = 0;
        m_MaxVertDispFromWaves = 0;

        if (m_Params.followViewpoint)
        {
            lateUpdatePosition(cameraView);
            lateUpdateScale();
            lateUpdateViewerHeight();
        }

        LateUpdateLods();
    }

    private void lateUpdatePosition(Matrix4f cameraView) {
        Matrix4f.decompseRigidMatrix(cameraView, m_EyePos, null, null);
    }

    private void lateUpdateScale()
    {
        // reach maximum detail at slightly below sea level. this should combat cases where visual range can be lost
        // when water height is low and camera is suspended in air. i tried a scheme where it was based on difference
        // to water height but this does help with the problem of horizontal range getting limited at bad times.
        float maxDetailY = m_Params.sea_level - m_MaxVertDispFromWaves * m_Params.dropDetailHeightBasedOnWaves;
        float camDistance = Math.abs(m_EyePos.y - maxDetailY);

        // offset level of detail to keep max detail in a band near the surface
        camDistance = Math.max(camDistance - 1, 0f);

        // scale ocean mesh based on camera distance to sea level, to keep uniform detail.
        final float HEIGHT_LOD_MUL = 1f;
        float level = camDistance * HEIGHT_LOD_MUL;
        level = Math.max(level, m_Params.minScale);
        if (m_Params.maxScale != -1f) level = Math.min(level, 0.99f * m_Params.maxScale);

        float l2 = (float) (Math.log(level) / Math.log(2f));
        float l2f = (float) Math.floor(l2);

        viewerAltitudeLevelAlpha = l2 - l2f;
        m_Scale = (float) Math.pow(2f, l2f);
    }

    private void lateUpdateViewerHeight(){
        // todo  need readback.

        viewerHeightAboveWater = m_EyePos.y - m_Params.sea_level;
    }

    public float getViewerHeightAboveWater(){ return viewerHeightAboveWater;}
    public float getSeaLevel()   { return m_Params.sea_level;}

    private void LateUpdateLods()
    {
        // Do any per-frame update for each LOD type.
        m_LodTransform.updateTransforms(m_Params.lodDataResolution, this, m_EyePos, m_Params.sea_level);
    }

    /**
     * User shape inputs can report in how far they might displace the shape horizontally and vertically. The max value is
     * saved here. Later the bounding boxes for the ocean tiles will be expanded to account for this potential displacement.
     * @param maxHorizDisp
     * @param maxVertDisp
     * @param maxVertDispFromWaves
     */
    void ReportMaxDisplacementFromShape(float maxHorizDisp, float maxVertDisp, float maxVertDispFromWaves) {
        m_MaxHorizDispFromShape += maxHorizDisp;
        m_MaxVertDispFromShape += maxVertDisp;
        m_MaxVertDispFromWaves += maxVertDispFromWaves;
    }

    public float getViewerAltitudeLevelAlpha() { return viewerAltitudeLevelAlpha;}

    /** Could the ocean horizontal scale increase (for e.g. if the viewpoint gains altitude). Will be false if ocean already at maximum scale.*/
    public boolean scaleCouldIncrease (){ return m_Params.maxScale == -1f || m_Scale < m_Params.maxScale * 0.99f; }

    /** Could the ocean horizontal scale decrease (for e.g. if the viewpoint drops in altitude). Will be false if ocean already at minimum scale.*/
    public boolean scaleCouldDecrease () { return m_Params.minScale == -1f || m_Scale > m_Params.minScale * 1.01f; }

    public float calcLodScale(float lodIndex) { return (float) (m_Scale * Math.pow(2f, lodIndex)); }
    public float calcGridSize(int lodIndex) { return calcLodScale(lodIndex) / getLodDataResolution(); }

    public float getScale() { return m_Scale;}

    public void getGloaltoWorldTransform(Matrix4f localToWord){
        localToWord.setIdentity();

        localToWord.m30 = m_EyePos.x;
        localToWord.m31 = m_Params.sea_level;
        localToWord.m32 = m_EyePos.z;

        localToWord.scale(m_Scale, 1, m_Scale);
    }

    private GLSLProgram m_DebugProg;

    int getNodeCount() { return m_QuadNodes.size();}
    void getNodeInfo(int index, Wave_Simulation simulation, Wave_Shading_ShaderData shaderData, Matrix4f nodeTransform){
        CDClipmapNode node = m_QuadNodes.get(index);
        node.transform.getMatrix(nodeTransform);
        node.getData(this, simulation, shaderData);
        shaderData._InstanceData = node.getInstanceData();
        shaderData._GeomData = node.getGeomData();
        shaderData._LD_SliceIndex = node._lodIndex;
    }

    void drawNode(int index){
        CDClipmapNode node = m_QuadNodes.get(index);
        m_Meshes[node.meshIndex].Draw();
    }

    void getData(Wave_Shading_ShaderData shaderData, Matrix4f clipmapTransform){
        shaderData._OceanCenterPosWorld.set(m_EyePos.x, m_Params.sea_level, m_EyePos.z);
//        m_LodTransform.bindData(shaderData, false);

        getGloaltoWorldTransform(clipmapTransform);
    }

    public void debugDrawWave(Matrix4f cameraProj, Matrix4f cameraView){
        if(m_DebugProg == null){
            m_DebugProg = GLSLProgram.createProgram("nvidia/WaveWorks/shaders/DebugWave.vert","nvidia/WaveWorks/shaders/DebugWave.frag", null);
        }

        updateWave(cameraView);

        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        gl.glEnable(GLenum.GL_DEPTH_TEST);
        gl.glDisable(GLenum.GL_CULL_FACE);
        gl.glDisable(GLenum.GL_BLEND);
        gl.glPolygonMode(GLenum.GL_FRONT_AND_BACK, GLenum.GL_LINE);
        m_DebugProg.enable();
        GLSLUtil.setMat4(m_DebugProg, "g_View", cameraView);
        GLSLUtil.setMat4(m_DebugProg, "g_Proj", cameraProj);
        GLSLUtil.setFloat3(m_DebugProg, "_OceanCenterPosWorld", m_EyePos.x, m_Params.sea_level, m_EyePos.z);
        GLSLUtil.setFloat4(m_DebugProg, "_LD_Pos_Scale", m_LodTransform.getPosScales());

        Matrix4f gloabl = CacheBuffer.getCachedMatrix();
        Matrix4f local = CacheBuffer.getCachedMatrix();

        getGloaltoWorldTransform(gloabl);

        for(CDClipmapNode node : m_QuadNodes){
            node.transform.getMatrix(local);
//            local.load(node.debugMatrix);
            node.getData(this, null, null);

            GLSLUtil.setFloat4(m_DebugProg, "_InstanceData", node.getInstanceData());
            GLSLUtil.setFloat4(m_DebugProg, "_GeomData", node.getGeomData());
            GLSLUtil.setInt(m_DebugProg, "_LD_SliceIndex", node._lodIndex);

            Matrix4f world =  Matrix4f.mul(gloabl, local, local);

            GLSLUtil.setMat4(m_DebugProg, "g_Local", world);
            m_Meshes[node.meshIndex].Draw();
        }

        CacheBuffer.free(gloabl);
        CacheBuffer.free(local);
        gl.glPolygonMode(GLenum.GL_FRONT_AND_BACK, GLenum.GL_FILL);
    }

}

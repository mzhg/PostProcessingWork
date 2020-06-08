package jet.opengl.demos.nvidia.shadows;

import com.nvidia.developer.opengl.app.NvCameraMotionType;
import com.nvidia.developer.opengl.app.NvCameraXformType;
import com.nvidia.developer.opengl.app.NvInputTransformer;
import com.nvidia.developer.opengl.utils.ShadowmapGenerateProgram;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;

import jet.opengl.demos.scene.BaseScene;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.volumetricLighting.LightType;
import jet.opengl.postprocessing.shader.GLSLUtil;
import jet.opengl.postprocessing.texture.SamplerDesc;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureDataDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.BoundingBox;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.LogUtil;
import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.postprocessing.util.NvImage;

/**
 * Created by mazhen'gui on 2017/11/4.
 */
final class SoftShadowScene extends BaseScene implements ShadowSceneController{
    public static final int POS_ATTRIB_LOC = 0;
    public static final int NOR_ATTRIB_LOC = 1;
    private static final float GROUND_PLANE_RADIUS = 8.0f;

    private static final String ROCK_DIFFUSE_MAP_FILENAME = "lichen6_rock.dds";
    private static final String GROUND_DIFFUSE_MAP_FILENAME = "lichen6.dds";
    private static final String GROUND_NORMAL_MAP_FILENAME = "lichen6_normal.dds";
//    private RigidMesh m_knightMesh;
//    private RigidMesh m_podiumMesh;

    private List<MeshInstance> m_meshInstances = new ArrayList<>();
    private MeshInstance m_knightMesh;
    private MeshInstance m_podiumMesh;

    // Ground plane
    private float m_groundHeight;
    private float m_groundRadius;
    private int   m_groundVertexBuffer;
    private int   m_groundIndexBuffer;

    private Texture2D m_GroundDiffuseTex;
    private Texture2D m_GroundNormalTex;
    private Texture2D m_RockDiffuseTex;
    private Texture2D m_SobelTex;
    private SoftShadowSceneRenderProgram m_SceneRenderProgram;

    private boolean m_useTexture = true;
    private final Matrix4f m_tempMat0 = new Matrix4f();
    private float m_worldHeightOffset = 0.2f;
    private float m_worldWidthOffset = 0.2f;
    private boolean m_printOnce = false;
    private ShadowConfig m_ShadowConfig = new ShadowConfig();

    private int m_SamplerDepthTex;
    private int m_SamplerShadowTex;

    private ShadowMapGenerator m_ShadowGen;
    private final Matrix4f m_ScreenToShadowMap = new Matrix4f();

    public SoftShadowScene(ShadowMapGenerator shadowGenerator){
        m_ShadowGen = shadowGenerator;
    }

    @Override
    protected void onCreate(Object prevSavedData) {
        m_ShadowGen.setShadowScene(this);
        KnightModel.loadData();
        // Build the scene
        RigidMesh knightMesh = addAutoRelease(new RigidMesh(
                KnightModel.vertices,
                KnightModel.numVertices,
                KnightModel.indices,
                KnightModel.numIndices));
        RigidMesh podiumMesh = addAutoRelease(new RigidMesh(
                PodiumModel.vertices,
                PodiumModel.numVertices,
                PodiumModel.indices,
                PodiumModel.numIndices));

        // Build the scene
        m_knightMesh = addMeshInstance(knightMesh, null, "Knight");
        m_podiumMesh = addMeshInstance(podiumMesh, null, "Podium");
        mNVApp.getInputTransformer().setMotionMode(NvCameraMotionType.DUAL_ORBITAL);

        // Setup the ground plane
        MeshInstance knight = m_knightMesh;
        Vector3f extents = knight.getExtents();
        Vector3f center = knight.getCenter();
        float height = center.y - extents.y;
        setGroundPlane(height, GROUND_PLANE_RADIUS);

        // Setup the eye's view parameters
        initCamera(
                NvCameraXformType.MAIN,
                new Vector3f(-0.644995f * 1.5f, 0.614183f * 1.5f, 0.660632f * 1.5f), // position
                new Vector3f(0.0f, 0.0f, 0.0f));                       // look at point

        // Setup the light's view parameters
        initCamera(
                NvCameraXformType.SECONDARY,
                new Vector3f(3.57088f * 1.5f, 6.989f * 1.5f, 5.19698f * 1.5f), // position
                new Vector3f(0.0f, 0.0f, 0.0f));                 // look at point

        createGeometry();
        createTextures();
        m_SceneRenderProgram = addAutoRelease(new SoftShadowSceneRenderProgram());

        GLCheck.checkError();
        m_ShadowConfig.shadowMapFiltering = ShadowMapGenerator.ShadowMapFiltering.PCF;
        m_ShadowConfig.shadowType = ShadowMapGenerator.ShadowType.SHADOW_MAPPING;
        m_ShadowConfig.lightType = LightType.DIRECTIONAL;
        m_ShadowConfig.spotHalfAngle = 10;
        m_ShadowConfig.shadowMapSplitting = ShadowMapGenerator.ShadowMapSplitting.NONE;
        m_ShadowConfig.checkCameraFrustumeVisible = false;
        m_ShadowConfig.lightNear = 0.1f;
        m_ShadowConfig.lightFar = 32.0f;
        m_ShadowConfig.shadowMapFormat = GLenum.GL_DEPTH_COMPONENT16;
        m_ShadowConfig.shadowMapSampleCount = 1;
        m_ShadowConfig.shadowMapSize = 1024;
        m_ShadowConfig.shadowMapPattern = ShadowMapGenerator.ShadowMapPattern.POISSON_100_100;

        SamplerDesc desc = new SamplerDesc();
        desc.minFilter = GLenum.GL_NEAREST;
        desc.magFilter = GLenum.GL_NEAREST;
        desc.borderColor = 0xFFFFFFFF;  // white
        desc.wrapR = GLenum.GL_CLAMP_TO_BORDER;
        desc.wrapS = GLenum.GL_CLAMP_TO_BORDER;
        desc.wrapT = GLenum.GL_CLAMP_TO_BORDER;
        m_SamplerDepthTex = SamplerUtils.createSampler(desc);

        desc.compareMode = GLenum.GL_COMPARE_REF_TO_TEXTURE;
        desc.compareFunc = GLenum.GL_LEQUAL;
        m_SamplerShadowTex = SamplerUtils.createSampler(desc);

        GLCheck.checkError();
    }

    @Override
    protected void update(float dt) {
        updateCamera();
        updateLightCamera();

        m_ShadowGen.setShadowConfig(m_ShadowConfig);
    }

    private void updateCamera(){
        /*Matrix4f proj = m_projection;
        Matrix4f view = m_camera.getModelViewMat(tmp_mat);
        m_viewProj.load(proj);
        m_viewProj.translate(m_worldWidthOffset, 0.0f, m_worldHeightOffset);
        Matrix4f.mul(m_viewProj, view, m_viewProj);*/

        Matrix4f viewMat = mSceneData.getViewMatrix();
        mNVApp.getInputTransformer().getModelViewMat(viewMat);
        Matrix4f shiftView = m_tempMat0;
        shiftView.setTranslate(m_worldWidthOffset, 0.0f, m_worldHeightOffset);
        Matrix4f.mul(shiftView, viewMat, viewMat);
        mSceneData.setViewAndUpdateCamera(viewMat);
    }

    private final Vector3f s_rot = new Vector3f();
    private final Vector3f s_trans = new Vector3f();
    private float s_scale;

    private void updateLightCamera(){
        final NvInputTransformer camera = mNVApp.getInputTransformer();
        Matrix4f view = camera.getModelViewMat(NvCameraXformType.SECONDARY, m_tempMat0);
        Matrix4f inverseView = Matrix4f.invert(view, view);
        Vector3f lightCenterWorld = Matrix4f.transformVector(inverseView, Vector3f.ZERO, s_rot);  // s_rot for templing use.
        float lightCenterWorldY = lightCenterWorld.y;

        // If the light source is high enough above the ground plane
        if (lightCenterWorldY > 1.0f)
        {
            s_rot.set(camera.getRotationVec(NvCameraXformType.SECONDARY));
            s_trans.set(camera.getTranslationVec(NvCameraXformType.SECONDARY));
            s_scale = camera.getScale(NvCameraXformType.SECONDARY);
        }
        else
        {
            camera.setRotationVec(s_rot, NvCameraXformType.SECONDARY);
            camera.setTranslationVec(s_trans, NvCameraXformType.SECONDARY);
            camera.setScale(s_scale, NvCameraXformType.SECONDARY);
            camera.update(0.0f);
        }

        updateLightCamera(camera.getModelViewMat(NvCameraXformType.SECONDARY, m_tempMat0));
    }

    private void updateLightCamera(Matrix4f view) {
        Matrix4f inverseView = Matrix4f.invert(view, view);
        Matrix4f.transformVector(inverseView, Vector3f.ZERO, m_ShadowConfig.lightPos);
        Vector3f.sub(Vector3f.ZERO, m_ShadowConfig.lightPos, m_ShadowConfig.lightDir);
        m_ShadowConfig.lightDir.normalise();
    }

    @Override
    public void onShadowRender(ShadowMapParams shadowMapParams, ShadowmapGenerateProgram program, int cascade) {
        drawMeshesShadow(program, shadowMapParams.m_LightViewProj);

        if(!m_printOnce){
            LogUtil.i(LogUtil.LogType.DEFAULT, "onShadowRender is called");
        }
    }

    @Override
    protected void onRender(boolean clearFBO) {
        m_ShadowGen.generateShadow(getSceneData());

        if(clearFBO){
            gl.glClearColor(0,0,0,0);
            gl.glClearDepthf(1.0f);
            gl.glClear(GLenum.GL_COLOR_BUFFER_BIT|GLenum.GL_DEPTH_BUFFER_BIT);

            gl.glEnable(GLenum.GL_DEPTH_TEST);
        }

        gl.glBindVertexArray(0);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);
        gl.glActiveTexture(GLenum.GL_TEXTURE1);
        gl.glBindTexture(m_GroundDiffuseTex.getTarget(), m_GroundDiffuseTex.getTexture());

        gl.glActiveTexture(GLenum.GL_TEXTURE2);
        gl.glBindTexture(m_GroundNormalTex.getTarget(), m_GroundNormalTex.getTexture());

        gl.glActiveTexture(GLenum.GL_TEXTURE3);
        Texture2D shadowMap =m_ShadowGen.getShadowMap();
        gl.glBindTexture(shadowMap.getTarget(), shadowMap.getTexture());
        gl.glBindSampler(3, m_SamplerDepthTex);
        gl.glActiveTexture(GLenum.GL_TEXTURE4);
        gl.glBindTexture(shadowMap.getTarget(), shadowMap.getTexture());
        gl.glBindSampler(4, m_SamplerShadowTex);

        gl.glActiveTexture(GLenum.GL_TEXTURE6);
        gl.glBindTexture(m_SobelTex.getTarget(), m_SobelTex.getTexture());

        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(m_RockDiffuseTex.getTarget(), m_RockDiffuseTex.getTexture());

        m_SceneRenderProgram.enable();
        m_SceneRenderProgram.setViewProj(mSceneData.getViewProjMatrix());
        m_SceneRenderProgram.setLightPos(m_ShadowConfig.lightPos);
        m_SceneRenderProgram.setPodiumCenterWorldPos(m_podiumMesh.getCenter());
        m_SceneRenderProgram.setShadowUniforms(m_ShadowConfig, m_ShadowGen.getShadowMapParams());

        // GetLightSourceAngle returns the full angle.
        float DirectionalLightAngle = 10;  // 10 degrees
        double TanLightSourceAngle = Math.tan(0.5 * Math.toRadians(DirectionalLightAngle));

        float MaxKernelSize  = 7;

        float SW = 2.0f * m_ShadowGen.getShadowCasterRadius();
        float SZ = m_ShadowGen.getShadowCasterDepthRange();

//        FVector4 PCSSParameterValues = FVector4(TanLightSourceAngle * SZ / SW, MaxKernelSize / float(ShadowInfo->ResolutionX), 0, 0);
        {
            float PCSSParameterValuesX = (float) (TanLightSourceAngle * SZ / SW);
            float PCSSParameterValuesY = MaxKernelSize / shadowMap.getWidth();
            GLSLUtil.setFloat4(m_SceneRenderProgram, "PCSSParameters", PCSSParameterValuesX,PCSSParameterValuesY,0,0);
        }

        {
            final Matrix4f screenToWorld = m_tempMat0;
            Matrix4f.invert(mSceneData.getViewProjMatrix(), screenToWorld);

            ShadowMapParams shadowData = m_ShadowGen.getShadowMapParams();
            Matrix4f.mul(shadowData.m_LightViewProj, screenToWorld, m_ScreenToShadowMap);
            screenToWorld.set(0.5f,0,0,0,
                    0, 0.5f,0,0,
                    0,0,0.5f, 0,
                    0.5f, 0.5f, 0.5f, 1.0f);

            Matrix4f.mul(screenToWorld, m_ScreenToShadowMap, m_ScreenToShadowMap);

            GLSLUtil.setMat4(m_SceneRenderProgram, "ScreenToShadowMatrix", m_ScreenToShadowMap);
        }

        drawMeshes(m_SceneRenderProgram);

        if(!m_printOnce){
            m_SceneRenderProgram.setName("Mesh Rendering");
            m_SceneRenderProgram.printPrograminfo();
        }

        drawGround(m_SceneRenderProgram);

        if(!m_printOnce){
            m_SceneRenderProgram.setName("Ground Rendering");
            m_SceneRenderProgram.printPrograminfo();
        }

        gl.glActiveTexture(GLenum.GL_TEXTURE1);
        gl.glBindTexture(m_GroundDiffuseTex.getTarget(), 0);
        gl.glActiveTexture(GLenum.GL_TEXTURE2);
        gl.glBindTexture(m_GroundNormalTex.getTarget(), 0);
        gl.glActiveTexture(GLenum.GL_TEXTURE3);
        gl.glBindTexture(shadowMap.getTarget(), 0);
        gl.glBindSampler(3, 0);
        gl.glActiveTexture(GLenum.GL_TEXTURE4);
        gl.glBindTexture(shadowMap.getTarget(), 0);
        gl.glBindSampler(4, 0);

        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(m_RockDiffuseTex.getTarget(), 0);

        m_printOnce = true;
    }

    @Override
    protected void onDestroy() {
        m_ShadowGen.dispose();
    }

    @Override
    public void onResize(int width, int height) {
        super.onResize(width, height);

        mSceneData.setProjection(45, (float)width/height, 0.1f, 100.0f);
    }

    @Override
    public void addShadowCasterBoundingBox(int index, BoundingBox boundingBox) {
        boundingBox.expandBy(m_meshInstances.get(index).getBounds());
    }

    @Override
    public int getShadowCasterCount() {
        return m_meshInstances.size();
    }

    MeshInstance addMeshInstance(RigidMesh mesh, Matrix4f worldTransform, String name) {
        MeshInstance instance = new MeshInstance(mesh, name, worldTransform);
        m_meshInstances.add(instance);
        return instance;
    }

    void removeMeshInstance(MeshInstance instance) {
        m_meshInstances.remove(instance);
    }

    void createGeometry()
    {
        m_groundIndexBuffer = gl.glGenBuffer();
        m_groundVertexBuffer = gl.glGenBuffer();
        createPlane( m_groundIndexBuffer, m_groundVertexBuffer,
                m_groundRadius, m_groundHeight);

        addAutoRelease(()->
        {
           gl.glDeleteBuffer(m_groundIndexBuffer);
           gl.glDeleteBuffer(m_groundVertexBuffer);
        });
    }

    void createPlane(int indexBuffer, int vertexBuffer, float radius, float height){
        float[] vertices =
        {
                -radius, height,  radius, 0.0f, 1.0f, 0.0f,
                radius, height,  radius, 0.0f, 1.0f, 0.0f,
                radius, height, -radius, 0.0f, 1.0f, 0.0f,
                -radius, height, -radius, 0.0f, 1.0f, 0.0f,
        };

        short[] indices = {0, 1, 2, 0, 2, 3};

        // Stick the data for the vertices into its VBO
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, vertexBuffer);
        gl.glBufferData(GLenum.GL_ARRAY_BUFFER, CacheBuffer.wrap(vertices), GLenum.GL_STATIC_DRAW);

        // Stick the data for the indices into its VBO
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, indexBuffer);
        gl.glBufferData(GLenum.GL_ELEMENT_ARRAY_BUFFER, CacheBuffer.wrap(indices), GLenum.GL_STATIC_DRAW);

        // Clear the VBO state
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    void createTextures()
    {
        final String m_texturePath = "nvidia/ShadowWorks/textures/";
        try {
            int groundDiffuseTex = NvImage.uploadTextureFromDDSFile(m_texturePath + GROUND_DIFFUSE_MAP_FILENAME);
            int groundNormalTex = NvImage.uploadTextureFromDDSFile(m_texturePath + GROUND_NORMAL_MAP_FILENAME);
            int rockDiffuseTex = NvImage.uploadTextureFromDDSFile(m_texturePath + ROCK_DIFFUSE_MAP_FILENAME);

            m_GroundDiffuseTex = addAutoRelease(TextureUtils.createTexture2D(GLenum.GL_TEXTURE_2D, groundDiffuseTex));
            m_GroundNormalTex = addAutoRelease(TextureUtils.createTexture2D(GLenum.GL_TEXTURE_2D, groundNormalTex));
            m_RockDiffuseTex = addAutoRelease(TextureUtils.createTexture2D(GLenum.GL_TEXTURE_2D, rockDiffuseTex));
        } catch (IOException e) {
            e.printStackTrace();
        }

        setTextureRepeatParams(m_GroundDiffuseTex);
        setTextureRepeatParams(m_GroundNormalTex);
        setTextureRepeatParams(m_RockDiffuseTex);

        gl.glBindTexture(m_GroundDiffuseTex.getTarget(), 0);

        Texture2DDesc desc = new Texture2DDesc(32, 16, GLenum.GL_R16UI);
        ShortBuffer destBuffer = CacheBuffer.getCachedShortBuffer(desc.width * desc.height);

        for(int y = 0; y < 16; y++){
            // 16x16 block stating at 0,0 = Sobol X,Y form bottom 4 bits of cell X,Y
            for(int x = 0; x < 16; x++){
                short v = ComputeGPUSpatialSeed(x,y,0);
                destBuffer.put(v);
            }

            // 16x16 block starting at 16,0 = Sobol X,Y from 2nd 4 bits of cell X,Y
            for(int x = 0; x < 16; x++){
                short v = ComputeGPUSpatialSeed(x,y,1);
                destBuffer.put(v);
            }
        }

        destBuffer.flip();
        TextureDataDesc initData = new TextureDataDesc();
        initData.format = TextureUtils.measureFormat(desc.format);
        initData.type = TextureUtils.measureDataType(desc.format);

        m_SobelTex = TextureUtils.createTexture2D(desc, initData);
    }

    // static
    private static short ComputeGPUSpatialSeed(int x, int y, int Index)
    {
        assert (x >= 0 && x < 16);
        assert (y >= 0 && y < 16);

        int Result = 0;
        if (Index == 0)
        {
            Result  = (x & 0x001)!=0 ? 0xf68e : 0;
            Result ^= (x & 0x002)!=0 ? 0x8e56 : 0;
            Result ^= (x & 0x004)!=0 ? 0x1135 : 0;
            Result ^= (x & 0x008)!=0 ? 0x220a : 0;
            Result ^= (y & 0x001)!=0 ? 0x94c4 : 0;
            Result ^= (y & 0x002)!=0 ? 0x4ac2 : 0;
            Result ^= (y & 0x004)!=0 ? 0xfb57 : 0;
            Result ^= (y & 0x008)!=0 ? 0x0454 : 0;
        }
        else if (Index == 1)
        {
            Result  = (x & 0x001)!=0 ? 0x4414 : 0;
            Result ^= (x & 0x002)!=0 ? 0x8828 : 0;
            Result ^= (x & 0x004)!=0 ? 0xe69e : 0;
            Result ^= (x & 0x008)!=0 ? 0xae76 : 0;
            Result ^= (y & 0x001)!=0 ? 0xa28a : 0;
            Result ^= (y & 0x002)!=0 ? 0x265e : 0;
            Result ^= (y & 0x004)!=0 ? 0xe69e : 0;
            Result ^= (y & 0x008)!=0 ? 0xae76 : 0;
        }
        else
        {
            assert (false);
        }

        return (short)(Result & Numeric.MAX_USHORT);
    }

    private void setTextureRepeatParams(Texture2D tex){
        gl.glBindTexture(tex.getTarget(), tex.getTexture());
        gl.glTexParameteri(tex.getTarget(), GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_REPEAT);
        gl.glTexParameteri(tex.getTarget(), GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_REPEAT);

        if(!gl.getGLAPIVersion().ES){
            int largest = gl.glGetInteger(GLenum.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT);
            gl.glTexParameteri(tex.getTarget(), GLenum.GL_TEXTURE_MAX_ANISOTROPY_EXT, largest);
        }
    }

    int getSceneStatsIndices(){
        int numIndices = 0;

        for(int i = 0; i < m_meshInstances.size(); i++){
            numIndices = m_meshInstances.get(i).accumStatsIndex(numIndices);
        }

        return numIndices;
    }

    int getSceneStatsVertices(){
        int numVertices = 0;

        for(int i = 0; i < m_meshInstances.size(); i++){
            numVertices = m_meshInstances.get(i).accumStatsVertex(numVertices);
        }

        return numVertices;
    }

    void setGroundPlane(float height, float radius)
    {
        m_groundHeight = height;
        m_groundRadius = radius;
    }

    void drawMeshes(SoftShadowSceneRenderProgram shader)
    {
        shader.setUseDiffuseTex(true);
        for (int i = 0; i < m_meshInstances.size(); i++)
        {
            MeshInstance instance = m_meshInstances.get(i);
            if (instance == m_podiumMesh)
                shader.setUseTexture(m_useTexture ? 2 : 0);
            else
                shader.setUseTexture(0);

//            instance.draw(shader);
            shader.setWorld(instance.getWorldTransform());
            instance.getMesh().render(POS_ATTRIB_LOC, NOR_ATTRIB_LOC);
        }
    }

    void drawMeshesShadow(ShadowmapGenerateProgram shader, Matrix4f lightViewProj){
        for (int i = 0; i < m_meshInstances.size(); i++)
        {
            MeshInstance instance = m_meshInstances.get(i);

            Matrix4f.mul(lightViewProj, instance.getWorldTransform(), m_tempMat0);
            shader.applyMVPMat(m_tempMat0);
            instance.getMesh().render(POS_ATTRIB_LOC, -1);
        }
    }

    void drawGround(SoftShadowSceneRenderProgram shader )
    {
        // Set uniforms
        shader.setUseDiffuseTex(false);
        shader.setUseTexture(m_useTexture ? 1 : 0);

        // Bind the VBO for the vertex data
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, m_groundVertexBuffer);

        // Set up attribute for the position (3 floats)
        int positionLocation = /*shader.getPositionAttrHandle()*/0;
        if (positionLocation >= 0)
        {
            gl.glVertexAttribPointer(positionLocation, 3, GLenum.GL_FLOAT, false, 24, 0);
            gl.glEnableVertexAttribArray(positionLocation);
        }

        // Set up attribute for the normal (3 floats)
        int normalLocation = /*shader.getNormalAttrHandle()*/1;
        if (normalLocation >= 0)
        {
            gl.glVertexAttribPointer(normalLocation, 3, GLenum.GL_FLOAT, false, 24, 12);
            gl.glEnableVertexAttribArray(normalLocation);
        }

        // Set up the indices
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, m_groundIndexBuffer);

        // Do the actual drawing
        gl.glDrawElements(GLenum.GL_TRIANGLES, 6, GLenum.GL_UNSIGNED_SHORT, 0);

        // Clear state
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
        if (positionLocation >= 0)
        {
            gl.glDisableVertexAttribArray(positionLocation);
        }
        if (normalLocation >= 0)
        {
            gl.glDisableVertexAttribArray(normalLocation);
        }
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);
    }
}

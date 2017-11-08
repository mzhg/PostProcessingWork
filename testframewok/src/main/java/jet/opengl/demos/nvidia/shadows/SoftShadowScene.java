package jet.opengl.demos.nvidia.shadows;

import com.nvidia.developer.opengl.app.NvCameraMotionType;
import com.nvidia.developer.opengl.app.NvCameraXformType;
import com.nvidia.developer.opengl.app.NvInputTransformer;
import com.nvidia.developer.opengl.utils.BoundingBox;
import com.nvidia.developer.opengl.utils.NvImage;
import com.nvidia.developer.opengl.utils.ShadowmapGenerateProgram;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jet.opengl.demos.scene.BaseScene;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.SamplerDesc;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.LogUtil;

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
    private SoftShadowSceneRenderProgram m_SceneRenderProgram;

    private boolean m_useTexture = true;
    private final Matrix4f m_tempMat0 = new Matrix4f();
    private float m_worldHeightOffset = 0.2f;
    private float m_worldWidthOffset = 0.2f;
    private boolean m_printOnce = false;
    private ShadowConfig m_ShadowConfig = new ShadowConfig();

    private int m_SamplerDepthTex;
    private int m_SamplerShadowTex;

    private ShadowGenerator m_ShadowGen;

    public SoftShadowScene(ShadowGenerator shadowGenerator){
        m_ShadowGen = shadowGenerator;
    }

    @Override
    protected void onCreate(Object prevSavedData) {
        m_ShadowGen.setShadowScene(this);
        KnightModel.loadData();
        // Build the scene
        RigidMesh knightMesh = new RigidMesh(
                KnightModel.vertices,
                KnightModel.numVertices,
                KnightModel.indices,
                KnightModel.numIndices);
        RigidMesh podiumMesh = (new RigidMesh(
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
        m_SceneRenderProgram = new SoftShadowSceneRenderProgram();

        GLCheck.checkError();
        m_ShadowConfig.shadowMapFiltering = ShadowGenerator.ShadowMapFiltering.PCSS;
        m_ShadowConfig.shadowType = ShadowGenerator.ShadowType.SHADOW_MAPPING;
        m_ShadowConfig.lightType = ShadowGenerator.LightType.DIRECTION;
        m_ShadowConfig.spotHalfAngle = 10;
        m_ShadowConfig.shadowMapSplitting = ShadowGenerator.ShadowMapSplitting.NONE;
        m_ShadowConfig.checkCameraFrustumeVisible = false;
        m_ShadowConfig.lightNear = 0.1f;
        m_ShadowConfig.lightFar = 32.0f;
        m_ShadowConfig.shadowMapFormat = GLenum.GL_DEPTH_COMPONENT16;
        m_ShadowConfig.shadowMapSampleCount = 1;
        m_ShadowConfig.shadowMapSize = 1024;
        m_ShadowConfig.shadowMapPattern = ShadowGenerator.ShadowMapPattern.POISSON_100_100;

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

    private void updateLightCamera(Matrix4f view)
    {
        /*// Assuming that the bbox of mesh1 contains everything
        Vector3f center = m_knightMesh.getWorldCenter();
        Vector3f extents = m_knightMesh.getExtents();

        Vector3f[] box = new Vector3f[2];
//		        box[0] = center - extents;
//		        box[1] = center + extents;
        box[0] = Vector3f.sub(center, extents, box[0]);
        box[1] = Vector3f.add(center, extents, box[1]);

        Vector3f[] bbox = new Vector3f[2];
        transformBoundingBox(box, view, bbox);

        float frustumWidth = Math.max(Math.abs(bbox[0].x), Math.abs(bbox[1].x)) * 2.0f;
        float frustumHeight = Math.max(Math.abs(bbox[0].y), Math.abs(bbox[1].y)) * 2.0f;
        float zNear = -bbox[1].z;
        float zFar = LIGHT_ZFAR;

        System.out.println("zNear = " + zNear);
        System.out.println("zFar = " + zFar);

        Matrix4f proj = tmp_mat1;
        Matrix4f.frustum(frustumWidth, frustumHeight, zNear, zFar, proj);
//		        m_lightViewProj = proj * view;
        Matrix4f.mul(proj, view, m_lightViewProj);

        Matrix4f clip2Tex = tmp_mat1;
        clip2Tex.setIdentity();

//		        clip2Tex.set_scale(nv.vec3f(0.5f, 0.5f, 0.5f));
//		        clip2Tex.set_translate(nv.vec3f(0.5f, 0.5f, 0.5f));
        clip2Tex.m00 = clip2Tex.m11 = clip2Tex.m22 = 0.5f;
        clip2Tex.m30 = clip2Tex.m31 = clip2Tex.m32 = 0.5f;

//		        nv.matrix4f viewProjClip2Tex = clip2Tex * m_lightViewProj;
        Matrix4f viewProjClip2Tex = Matrix4f.mul(clip2Tex, m_lightViewProj, clip2Tex);

//		        nv.matrix4f inverseView = nv.inverse(view);
        Matrix4f inverseView = Matrix4f.invert(view, tmp_mat2);
        Vector3f lightCenterWorld = Matrix4f.transformVector(inverseView, Vector3f.ZERO, null);

        if (m_shadowMapShader != null)
        {
            m_shadowMapShader.enable();
            m_shadowMapShader.setViewProjMatrix(m_lightViewProj);
            m_shadowMapShader.disable();
        }

        if (m_visTexShader != null)
        {
            m_visTexShader.enable();
            m_visTexShader.setLightZNear(zNear);
            m_visTexShader.setLightZFar(zFar);
            m_visTexShader.disable();
        }

        if (m_pcssShader != null)
        {
            m_pcssShader.enable();
            m_pcssShader.setLightViewMatrix(view);
            m_pcssShader.setLightViewProjClip2TexMatrix(viewProjClip2Tex);
            m_pcssShader.setLightZNear(zNear);
            m_pcssShader.setLightZFar(zFar);
            m_pcssShader.setLightPosition(lightCenterWorld);
            m_pcssShader.disable();
        }

        updateLightSize(frustumWidth, frustumHeight);*/

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

        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(m_RockDiffuseTex.getTarget(), m_RockDiffuseTex.getTexture());

        m_SceneRenderProgram.enable();
        m_SceneRenderProgram.setViewProj(mSceneData.getViewProjMatrix());
        m_SceneRenderProgram.setLightPos(m_ShadowConfig.lightPos);
        m_SceneRenderProgram.setPodiumCenterWorldPos(m_podiumMesh.getCenter());
        m_SceneRenderProgram.setShadowUniforms(m_ShadowConfig, m_ShadowGen.getShadowMapParams());
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

    void initCamera(int index, Vector3f eye, Vector3f at) {
        // Construct the look matrix
//	    	    Matrix4f look;
//	    	    lookAt(look, eye, at, nv.vec3f(0.0f, 1.0f, 0.0f));
        Matrix4f look = Matrix4f.lookAt(eye, at, Vector3f.Y_AXIS, null);

        // Decompose the look matrix to get the yaw and pitch.
        float pitch = (float) Math.atan2(-look.m21, /*_32*/ look.m22/*_33*/);
        float yaw = (float) Math.atan2(look.m20/*_31*/, new Vector2f(-look.m21/*_32*/, look.m22/*_33*/).length());

        // Initialize the camera view.
        NvInputTransformer m_camera = mNVApp.getInputTransformer();
        m_camera.setRotationVec(new Vector3f(pitch, yaw, 0.0f), index);
        m_camera.setTranslationVec(new Vector3f(look.m30/*_41*/, look.m31/*_42*/, look.m32/*_43*/), index);
        m_camera.update(0.0f);
    }

    void createGeometry()
    {
        m_groundIndexBuffer = gl.glGenBuffer();
        m_groundVertexBuffer = gl.glGenBuffer();
        createPlane( m_groundIndexBuffer, m_groundVertexBuffer,
                m_groundRadius, m_groundHeight);
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

            m_GroundDiffuseTex = TextureUtils.createTexture2D(GLenum.GL_TEXTURE_2D, groundDiffuseTex);
            m_GroundNormalTex = TextureUtils.createTexture2D(GLenum.GL_TEXTURE_2D, groundNormalTex);
            m_RockDiffuseTex = TextureUtils.createTexture2D(GLenum.GL_TEXTURE_2D, rockDiffuseTex);
        } catch (IOException e) {
            e.printStackTrace();
        }

        setTextureRepeatParams(m_GroundDiffuseTex);
        setTextureRepeatParams(m_GroundNormalTex);
        setTextureRepeatParams(m_RockDiffuseTex);

        gl.glBindTexture(m_GroundDiffuseTex.getTarget(), 0);
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

package jet.opengl.demos.nvidia.shadows;

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

import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/11/4.
 */

public class SoftShadowScene extends ShadowScene {
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

    @Override
    protected void onCreate(Object prevSavedData) {
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
    }

    @Override
    protected void update(float dt) {

    }

    @Override
    protected void onShadowRender(ShadowMapParams shadowMapParams, ShadowmapGenerateProgram program, int cascade) {

    }

    @Override
    protected void onSceneRender(boolean clearFBO) {

    }

    @Override
    protected void addShadowCasterBoundingBox(int index, BoundingBox boundingBox) {

    }

    @Override
    protected int getShadowCasterCount() {
        return 2;
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
        // Setup the samplers
        /*for (int unit = GroundDiffuseTextureUnit; unit <= RockDiffuseTextureUnit; ++unit)
        {
            GL33.glSamplerParameteri(m_samplers[unit], GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL33.glSamplerParameteri(m_samplers[unit], GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
            GL33.glSamplerParameteri(m_samplers[unit], GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
            GL33.glSamplerParameteri(m_samplers[unit], GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        }*/

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

    void drawMeshes(SSSceneShader shader)
    {
        shader.setUseDiffuse(true);
        for (int i = 0; i < m_meshInstances.size(); i++)
        {
            MeshInstance instance = m_meshInstances.get(i);
            if (instance == m_podiumMesh)
                shader.setUseTexture(m_useTexture ? 2 : 0);
            else
                shader.setUseTexture(0);

            instance.draw(shader);
        }
    }

    void drawGround(SSSceneShader shader)
    {
        // Set uniforms
        shader.setUseDiffuse(false);
        shader.setUseTexture(m_useTexture ? 1 : 0);

        // Bind the VBO for the vertex data
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, m_groundVertexBuffer);

        // Set up attribute for the position (3 floats)
        int positionLocation = shader.getPositionAttrHandle();
        if (positionLocation >= 0)
        {
            gl.glVertexAttribPointer(positionLocation, 3, GLenum.GL_FLOAT, false, 24, 0);
            gl.glEnableVertexAttribArray(positionLocation);
        }

        // Set up attribute for the normal (3 floats)
        int normalLocation = shader.getNormalAttrHandle();
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

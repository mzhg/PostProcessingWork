package jet.opengl.demos.gpupro.vct;

import com.nvidia.developer.opengl.models.GLVAO;
import com.nvidia.developer.opengl.models.ModelGenerator;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.FramebufferGL;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.Texture3D;
import jet.opengl.postprocessing.texture.Texture3DDesc;
import jet.opengl.postprocessing.texture.TextureAttachDesc;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;

final class VoxelConeTracingRenderer {

    enum RenderingMode {
        VOXELIZATION_VISUALIZATION, // Voxelization visualization.
        VOXEL_CONE_TRACING 			// Global illumination using voxel cone tracing.
    };

    // ----------------
    // GLSL uniform names.
    // ----------------
    private static final String PROJECTION_MATRIX_NAME = "P";
    private static final String VIEW_MATRIX_NAME = "V";
    private static final String CAMERA_POSITION_NAME = "cameraPosition";
    private static final String NUMBER_OF_LIGHTS_NAME = "numberOfLights";
    private static final String SCREEN_SIZE_NAME = "screenSize";
    private static final String APP_STATE_NAME = "state";

    // ----------------
    // Rendering.
    // ----------------
    private boolean shadows = true;
    private boolean indirectDiffuseLight = true;
    private boolean indirectSpecularLight = true;
    private boolean directLight = true;

    // ----------------
    // Voxelization.
    // ----------------
    private boolean automaticallyRegenerateMipmap = true;
    private boolean regenerateMipmapQueued = true;
    private boolean automaticallyVoxelize = true;
    private boolean voxelizationQueued = true;
    private int voxelizationSparsity = 1; // Number of ticks between mipmap generation.

    // ----------------
    // Voxel cone tracing.
    // ----------------
    private GLSLProgram voxelConeTracingMaterial;

    // ----------------
    // Voxelization.
    // ----------------
    private int ticksSinceLastVoxelization = voxelizationSparsity;
    private int voxelTextureSize = 64; // Must be set to a power of 2.
    private final Matrix4f voxelCamera = new Matrix4f();
    private GLSLProgram voxelizationMaterial;
    private Texture3D voxelTexture = null;

    private FramebufferGL vvfbo1, vvfbo2;
    private GLSLProgram worldPositionMaterial, voxelVisualizationMaterial;
    // --- Screen quad. ---
    private MeshRenderer quadMeshRenderer;
    private GLVAO quad;
    // --- Screen cube. ---
    private MeshRenderer cubeMeshRenderer;
    private GLVAO cubeShape;

    private GLFuncProvider gl;

    void onCreate(){
        gl = GLFuncProviderFactory.getGLFuncProvider();

        gl.glHint(GLenum.GL_PERSPECTIVE_CORRECTION_HINT, GLenum.GL_NICEST);
        gl.glEnable(GLenum.GL_MULTISAMPLE); // MSAA. Set MSAA level using GLFW (see Application.cpp).

        String root = "gpupro/VoxelConeTracing/shaders/";
        try {
            voxelConeTracingMaterial = GLSLProgram.createFromFiles(root + "voxel_cone_tracing.vert", root + "voxel_cone_tracing.frag");
        } catch (IOException e) {
            e.printStackTrace();
        }
//        voxelCamera = OrthographicCamera(viewportWidth / float(viewportHeight));
        initVoxelization();

    }

    /// <summary> Initializes rendering. </summary>
    void onResize(int viewportWidth, int viewportHeight){
        initVoxelVisualization(viewportWidth, viewportHeight);
    }

    /// <sumamry> Renders a scene using a given rendering mode. </summary>
    void render(Scene renderingScene, int viewportWidth,int viewportHeight,
                RenderingMode renderingMode /*= RenderingMode::VOXEL_CONE_TRACING*/){
        // Voxelize.
        boolean voxelizeNow = voxelizationQueued || (automaticallyVoxelize && voxelizationSparsity > 0 && ++ticksSinceLastVoxelization >= voxelizationSparsity);
        if (voxelizeNow) {
            voxelize(renderingScene, true);
            ticksSinceLastVoxelization = 0;
            voxelizationQueued = false;
        }

        // Render.
        switch (renderingMode) {
            case VOXELIZATION_VISUALIZATION:
                renderVoxelVisualization(renderingScene, viewportWidth, viewportHeight);
                break;
            case VOXEL_CONE_TRACING:
                renderScene(renderingScene, viewportWidth, viewportHeight);
                break;
        }
    }

    private void initVoxelization(){
        String root = "gpupro/VoxelConeTracing/shaders/";
        voxelizationMaterial = //MaterialStore::getInstance().findMaterialWithName("voxelization");
            GLSLProgram.createProgram(root+"voxelization.vert", root + "voxelization.geom", root + "voxelization.frag", null);

//        assert(voxelizationMaterial != nullptr);

//	const std::vector<GLfloat> texture3D(4 * voxelTextureSize * voxelTextureSize * voxelTextureSize, 0.0f);
//        voxelTexture = new Texture3D(texture3D, voxelTextureSize, voxelTextureSize, voxelTextureSize, true);
        Texture3DDesc desc = new Texture3DDesc(voxelTextureSize, voxelTextureSize, voxelTextureSize, (int)(Math.log(voxelTextureSize) + 1), GLenum.GL_RGBA8);
        voxelTexture = TextureUtils.createTexture3D(desc, null);

        GLCheck.checkError();
    }

    private void voxelize(Scene  renderingScene, boolean clearVoxelizationFirst /*= true*/){
        if (clearVoxelizationFirst) {
            /*GLfloat clearColor[4] = { 0, 0, 0, 0 };
            voxelTexture->Clear(clearColor);*/
            gl.glClearTexImage(voxelTexture.getTexture(), 0, GLenum.GL_RGBA, GLenum.GL_UNSIGNED_BYTE, null);
        }

        GLSLProgram material = voxelizationMaterial;
        material.enable();
//        glUseProgram(material->program);
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);

        // Settings.
        gl.glViewport(0, 0, voxelTextureSize, voxelTextureSize);
        gl.glColorMask(false, false, false, false);
        gl.glDisable(GLenum.GL_CULL_FACE);
        gl.glDisable(GLenum.GL_DEPTH_TEST);
        gl.glDisable(GLenum.GL_BLEND);

        // Texture.
//        voxelTexture->Activate(material->program, "texture3D", 0);
        gl.glBindImageTexture(0, voxelTexture.getTexture(), 0, true, 0, GLenum.GL_WRITE_ONLY, GLenum.GL_RGBA8);

        // Lighting.
        uploadLighting(renderingScene, material);

        // Render.
//        renderQueue(renderingScene.renderers, material->program, true);
        renderingScene.renderScene(material);
        if (automaticallyRegenerateMipmap || regenerateMipmapQueued) {
            gl.glGenerateTextureMipmap(voxelTexture.getTexture());
            regenerateMipmapQueued = false;
        }
        gl.glColorMask(true, true, true, true);

        GLCheck.checkError();
    }

    // ----------------
    // Voxelization visualization.
    // ----------------
    private void initVoxelVisualization(int viewportWidth, int viewportHeight){
        // Materials.
        String root = "gpupro/VoxelConeTracing/shaders/";
        worldPositionMaterial = GLSLProgram.createProgram(root + "world_position.vert", root + "world_position.frag", null);
        voxelVisualizationMaterial = GLSLProgram.createProgram(root + "voxel_visualization.vert", root + "voxel_visualization.frag", null);

        assert(worldPositionMaterial != null);
        assert(voxelVisualizationMaterial != null);

        Texture2DDesc colorDesc = new Texture2DDesc(viewportWidth, viewportHeight, GLenum.GL_RGB16F);
        Texture2DDesc depthDesc = new Texture2DDesc(viewportWidth, viewportHeight, GLenum.GL_DEPTH_COMPONENT24);

        TextureGL[] attachments0 = {
           TextureUtils.createTexture2D(colorDesc, null),
           TextureUtils.createTexture2D(depthDesc, null)
        };

        TextureGL[] attachments1 = {
                TextureUtils.createTexture2D(colorDesc, null),
                attachments0[1]
        };

        TextureAttachDesc[] attachDescs = {
                new TextureAttachDesc(),   // default is ok
                new TextureAttachDesc(),
        };


        // FBOs.
        vvfbo1 = new FramebufferGL();
        vvfbo1.bind();
        vvfbo1.addTextures(attachments0, attachDescs);

        vvfbo2 = new FramebufferGL();
        vvfbo2.bind();
        vvfbo1.addTextures(attachments1, attachDescs);

        // Rendering cube.
        cubeShape = //ObjLoader::loadObjFile("Assets\\Models\\cube.obj");
                ModelGenerator.genCube(1, true, false,false).genVAO();
//        assert(cubeShape.meshes.size() == 1);
        cubeMeshRenderer = new MeshRenderer(new Mesh(cubeShape));

        // Rendering quad.
        quad = ModelGenerator.genRect(-1,-1,1,1, true).genVAO();
        quadMeshRenderer = new MeshRenderer(new Mesh(quad));
    }

    private void renderVoxelVisualization(Scene renderingScene, int viewportWidth, int viewportHeight){
        // -------------------------------------------------------
        // Render cube to FBOs.
        // -------------------------------------------------------
//        Camera & camera = *renderingScene.renderingCamera;
        GLSLProgram program = worldPositionMaterial;
//        glUseProgram(program);
        program.enable();
        uploadCamera(renderingScene, program);

        // Settings.
        gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        gl.glClearDepthf(1.0f);
        gl.glEnable(GLenum.GL_CULL_FACE);
        gl.glEnable(GLenum.GL_DEPTH_TEST);

        // Back.
        gl.glCullFace(GLenum.GL_FRONT);
//        glBindFramebuffer(GL_FRAMEBUFFER, vvfbo1->frameBuffer);
        vvfbo1.bind();
        gl.glViewport(0, 0, vvfbo1.getWidth(), vvfbo1.getHeight());
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT | GLenum.GL_DEPTH_BUFFER_BIT);
        cubeMeshRenderer.render(program);

        // Front.
        gl.glCullFace(GLenum.GL_BACK);
//        gl.glBindFramebuffer(GL_FRAMEBUFFER, vvfbo2->frameBuffer);
        vvfbo2.bind();
        gl.glViewport(0, 0, vvfbo2.getWidth(), vvfbo2.getHeight());
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT | GLenum.GL_DEPTH_BUFFER_BIT);
        cubeMeshRenderer.render(program);

        // -------------------------------------------------------
        // Render 3D texture to screen.
        // -------------------------------------------------------
        program = voxelVisualizationMaterial;
        program.enable();
        uploadCamera(renderingScene, program);
        gl.glBindRenderbuffer(GLenum.GL_RENDERBUFFER, 0);
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);

        // Settings.
        uploadGlobalConstants(voxelVisualizationMaterial, viewportWidth, viewportHeight);
        gl.glDisable(GLenum.GL_DEPTH_TEST);
        gl.glEnable(GLenum.GL_CULL_FACE);

        // Activate textures.
        /*vvfbo1->ActivateAsTexture(program, "textureBack", 0);
        vvfbo2->ActivateAsTexture(program, "textureFront", 1);
        voxelTexture->Activate(program, "texture3D", 2);*/

        gl.glBindTextureUnit(0, vvfbo1.getAttachedTex(0).getTexture());
        gl.glBindTextureUnit(1, vvfbo2.getAttachedTex(0).getTexture());
        gl.glBindTextureUnit(2, voxelTexture.getTexture());

        int index = program.getUniformLocation("textureBack");
        if(index >=0 ) gl.glUniform1i(index, 0);

        index = program.getUniformLocation("textureFront");
        if(index >=0 ) gl.glUniform1i(index, 1);

        index = program.getUniformLocation("texture3D");
        if(index >=0 ) gl.glUniform1i(index, 2);

        // Render.
        gl.glViewport(0, 0, viewportWidth, viewportHeight);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT | GLenum.GL_DEPTH_BUFFER_BIT);
        quadMeshRenderer.render(program);
    }

    // ----------------
    // Rendering.
    // ----------------
    void renderScene(Scene renderingScene, int viewportWidth, int viewportHeight){
        // Fetch references.
//        auto & camera = *renderingScene.renderingCamera;
        GLSLProgram material = voxelConeTracingMaterial;
//        const GLuint program = material->program;

        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
        material.enable();

        // GL Settings.
        gl.glViewport(0, 0, viewportWidth, viewportHeight);
        gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT | GLenum.GL_DEPTH_BUFFER_BIT);
        gl.glEnable(GLenum.GL_DEPTH_TEST);
        gl.glEnable(GLenum.GL_CULL_FACE);
        gl.glCullFace(GLenum.GL_BACK);
        gl.glColorMask(true, true, true, true);
        gl.glEnable(GLenum.GL_BLEND);
        gl.glBlendFunc(GLenum.GL_SRC_ALPHA, GLenum.GL_ONE_MINUS_SRC_ALPHA);

        // Upload uniforms.
        uploadCamera(renderingScene, material);
        uploadGlobalConstants(material, viewportWidth, viewportHeight);
        uploadLighting(renderingScene, material);
        uploadRenderingSettings(material);

        // Render.
//        renderQueue(renderingScene.renderers, material->program, true);
        renderingScene.renderScene(material);
    }

//    void renderQueue(RenderingQueue renderingQueue, GLSLProgram program, bool uploadMaterialSettings = false) const;
    void uploadGlobalConstants(GLSLProgram program, int viewportWidth, int viewportHeight){
        int index = gl.glGetUniformLocation(program.getProgram(), APP_STATE_NAME);
        if(index >= 0 ) gl.glUniform1i(index, /*Application::getInstance().state*/0);
//        glm::vec2 screenSize(viewportWidth, viewportHeight);
    }

    void uploadCamera(Scene scene, GLSLProgram program){
        int index = gl.glGetUniformLocation(program.getProgram(), VIEW_MATRIX_NAME);
        if(index >= 0 ) gl.glUniformMatrix4fv(index, false, CacheBuffer.wrap(scene.view));
        index = gl.glGetUniformLocation(program.getProgram(), PROJECTION_MATRIX_NAME);
        if(index >= 0 ) gl.glUniformMatrix4fv(index, false, CacheBuffer.wrap(scene.projection));

        Vector3f position = new Vector3f();
        Matrix4f.decompseRigidMatrix(scene.view, position, null, null);
        index = gl.glGetUniformLocation(program.getProgram(), CAMERA_POSITION_NAME);
        if(index >= 0 ) gl.glUniform3f(index, position.x, position.y, position.z);
    }

    void uploadLighting(Scene renderingScene, GLSLProgram program){
        // Point lights.
        for (int i = 0; i < renderingScene.pointLights.size(); ++i)
            renderingScene.pointLights.get(i).Upload(program, i);

        // Number of point lights.
        int index = gl.glGetUniformLocation(program.getProgram(), NUMBER_OF_LIGHTS_NAME);
        if(index >= 0 ) gl.glUniform1i(index, renderingScene.pointLights.size());
    }

    void uploadRenderingSettings(GLSLProgram glProgram) {
        int program = glProgram.getProgram();
        int index = gl.glGetUniformLocation(program, "settings.shadows");
        if (index >= 0) gl.glUniform1i(index, shadows?1:0);
        index = gl.glGetUniformLocation(program, "settings.indirectDiffuseLight");
        if (index >= 0) gl.glUniform1i(index, indirectDiffuseLight?1:0);
        index = gl.glGetUniformLocation(program, "settings.indirectSpecularLight");
        if (index >= 0) gl.glUniform1i(index, indirectSpecularLight?1:0);
        index = gl.glGetUniformLocation(program, "settings.directLight");
        if (index >= 0) gl.glUniform1i(index, directLight?1:0);
    }
}

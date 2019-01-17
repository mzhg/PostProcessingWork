package jet.opengl.demos.gpupro.glitter;

import com.nvidia.developer.opengl.app.NvSampleApp;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import jet.opengl.demos.gpupro.rvi.SponzaMesh;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.util.CacheBuffer;

public class Glitter extends NvSampleApp {
    // Toggle display of SSAO buffer
    static final int DISPLAY_SSAO = 1; //scene with SSAO
    static final int DISPLAY_SSAO_BUFFER = 2; //the SSAO occlusion buffer
    int displayMode = 1;
    boolean useRadiosity = false; //turns radiosity on/off
    int whichRad = 0; //radiosity from both, first layer only, second layer only

    static final int NUM_LIGHTS = 3;
    static final float MOVE_LIGHT_SPEED = 0.5f;
    Light[] lights = new Light[NUM_LIGHTS];
    int moveLight = 0; // Which light to control

    /** Create G-Buffer for deferred shading */
    GBuffer gbuffer;
    /** Create buffers and textures for SSAO and SSDO */
    AmbientOcclusionBuffer ssao;
    /** Buffer and textures to blur SSAO buffer before using it in the lighting pass*/
    BlurBuffer blur;
    /** Create buffers for Radiosity */
    RadiosityBuffer radiosity;

    // Shader for rendering to shadow depth maps (first 3 passes)
    GLSLProgram depthShader; //(FileSystem::getPath("Shaders/depthMap.vert.glsl").c_str(), FileSystem::getPath("Shaders/depthMap.frag.glsl").c_str(), FileSystem::getPath("Shaders/depthMap.geom.glsl").c_str());
    // Shader for first pass to gbuffer
    GLSLProgram geometryShader; //(FileSystem::getPath("Shaders/geometry.vert.glsl").c_str(), FileSystem::getPath("Shaders/geometry.frag.glsl").c_str(), FileSystem::getPath("Shaders/geometry.geom.glsl").c_str());
    // Shader for second pass (SSAO)
    GLSLProgram ssaoShader; //(FileSystem::getPath("Shaders/ssao.vert.glsl").c_str(), FileSystem::getPath("Shaders/ssao.frag.glsl").c_str());
    // Shader for third pass (blurring SSAO)
    GLSLProgram blurShader; //(FileSystem::getPath("Shaders/ssao.vert.glsl").c_str(), FileSystem::getPath("Shaders/blur.frag.glsl").c_str());
    // Shader for fourth pass (lighting)
    GLSLProgram lightingShader; //(FileSystem::getPath("Shaders/lighting.vert.glsl").c_str(), FileSystem::getPath("Shaders/lighting.frag.glsl").c_str(), FileSystem::getPath("Shaders/lighting.geom.glsl").c_str());
    // Shader for fifth pass (radiosity)
    GLSLProgram radiosityShader; //(FileSystem::getPath("Shaders/ssao.vert.glsl").c_str(), FileSystem::getPath("Shaders/radiosity.frag.glsl").c_str());
    GLSLProgram blurRadiosityShader; //(FileSystem::getPath("Shaders/ssao.vert.glsl").c_str(), FileSystem::getPath("Shaders/combine.frag.glsl").c_str());
    // Shader for fifth pass (rendering light sources as white cubes)
    GLSLProgram lightSourceShader; //(FileSystem::getPath("Shaders/geometry.vert.glsl").c_str(), FileSystem::getPath("Shaders/lightSource.frag.glsl").c_str());
    // Shader for sixth pass (rendering environment map as a cube at infinity)
    GLSLProgram envShader; //(FileSystem::getPath("Shaders/envMap.vert.glsl").c_str(), FileSystem::getPath("Shaders/envMap.frag.glsl").c_str());

    SponzaMesh sampleModel;

    EnvironmentMap envMap;

    private final Matrix4f m_View = new Matrix4f();
    private final Matrix4f m_Proj = new Matrix4f();
    private final Matrix4f m_Model = new Matrix4f();

    private GLFuncProvider gl;

    @Override
    protected void initRendering() {
        gl = GLFuncProviderFactory.getGLFuncProvider();

        gbuffer = new GBuffer();
        ssao = new AmbientOcclusionBuffer();
        blur = new BlurBuffer();
        radiosity = new RadiosityBuffer();

        depthShader = createShader("depthMap.vert.glsl","depthMap.frag.glsl", "depthMap.gemo.glsl" );
        geometryShader = createShader("geometry.vert.glsl", "geometry.frag.glsl", "geometry.gemo.glsl");
        ssaoShader = createShader("ssao.vert.glsl", "ssao.frag.glsl");
        blurShader = createShader("ssao.vert.glsl", "blur.frag.glsl");
        lightingShader = createShader("lighting.vert.glsl", "lighting.frag.glsl", "lighting.gemo.glsl");
        radiosityShader = createShader("ssao.vert.glsl", "radiosity.frag.glsl");
        blurRadiosityShader = createShader("ssao.vert.glsl", "combine.frag.glsl");
        lightSourceShader = createShader("geometry.vert.glsl", "lightSource.frag.glsl");
        envShader = createShader("envMap.vert.glsl", "envMap.frag.glsl");

        sampleModel = new SponzaMesh();

        // todo eve

        // Create lights
        lights[0] = new Light(new Vector3f(0, 5, 0), new Vector3f(1, 1, 1), 0.0019f, 0.022f, 1, 0);
        lights[1] = new Light(new Vector3f(52, 5, 10), new Vector3f(1, 1, 1), 0.0019f, 0.022f, 1, 1);
        lights[2] = new Light(new Vector3f(-25, 35, -8), new Vector3f(1, 1, 1), 0.0007f, 0.0014f, 1, 2);
    }

    @Override
    public void display() {
        gl.glEnable(GLenum.GL_DEPTH_TEST);
        gl.glClearColor(0,0,0,0);
        gl.glClearDepthf(1.0f);
        gl.glDepthFunc(GLenum.GL_LESS);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT|GLenum.GL_DEPTH_BUFFER_BIT);
        gl.glViewport(0,0, getGLContext().width(), getGLContext().height());

        m_transformer.getModelViewMat(m_View);
        m_Model.m00 = m_Model.m11 = m_Model.m22 = 0.05f;

        //render to each light's depth cubemap
        depthShader.enable();
        for (int i = 0; i < NUM_LIGHTS; i++) {
            lights[i].BindFramebuffer(depthShader, m_View);
//            model = glm::mat4();
//            model = glm::scale(model, glm::vec3(0.05f));    // The sponza model is too big, scale it first
            gl.glUniformMatrix4fv(gl.glGetUniformLocation(depthShader.getProgram(), "model"), false, CacheBuffer.wrap(m_Model));
            gl.glUniformMatrix4fv(gl.glGetUniformLocation(depthShader.getProgram(), "view"), false, CacheBuffer.wrap(m_View));
//            sampleModel.Draw(depthShader);  todo render model
        }

        //1st pass: render to gbuffer
        gbuffer.BindFramebuffer();
        gbuffer.CopyAndBindDepthCompareLayer(geometryShader);
        gl.glViewport(0,0, getGLContext().width(), getGLContext().height());
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT|GLenum.GL_DEPTH_BUFFER_BIT);
        //render model
        geometryShader.enable();
        gl.glUniform1f(gl.glGetUniformLocation(geometryShader.getProgram(), "farPlane"),Light.mFar);
        gl.glUniform1f(gl.glGetUniformLocation(geometryShader.getProgram(), "nearPlane"), Light.mNear);
        gl.glUniformMatrix4fv(gl.glGetUniformLocation(geometryShader.getProgram(), "projection"),false, CacheBuffer.wrap(m_Proj));
        gl.glUniformMatrix4fv(gl.glGetUniformLocation(geometryShader.getProgram(), "view"), false, CacheBuffer.wrap(m_View));
//        model = glm::mat4();
//        model = glm::scale(model, glm::vec3(0.05f));    // The sponza model is too big, scale it first
        gl.glUniformMatrix4fv(gl.glGetUniformLocation(geometryShader.getProgram(), "model"), false, CacheBuffer.wrap(m_Model));
//        sampleModel.Draw(geometryShader); todo render model
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);

        //2nd pass: create ssao and render to quad
        ssao.BindFramebuffer();
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT);
        ssaoShader.enable();
        ssao.BindBuffersSSAO(ssaoShader, gbuffer);
        ssao.SetUniforms(ssaoShader);
        //glUniform1i(glGetUniformLocation(ssaoShader.Program, "which"), whichSSAO);
        //glUniformMatrix4fv(glGetUniformLocation(ssaoShader.Program, "view"), 1, GL_FALSE, glm::value_ptr(view));
        gl.glUniformMatrix4fv(gl.glGetUniformLocation(ssaoShader.getProgram(), "projection"), false, CacheBuffer.wrap(m_Proj));
        RenderQuad();
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);

        //3rd pass: blur ssao/ssdo output
        blur.BindFramebuffer();
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT);
        blurShader.enable();
        ssao.BindBuffersBlur(blurShader);
        RenderQuad();
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);

        //4th pass: lighting
        if (useRadiosity) {
            radiosity.BindFramebuffer();
            gl.glClear(GLenum.GL_COLOR_BUFFER_BIT);
        }
        else
            gl.glClear(GLenum.GL_COLOR_BUFFER_BIT | GLenum.GL_DEPTH_BUFFER_BIT);
        lightingShader.enable();
        blur.BindBuffersLighting(lightingShader, gbuffer);
        //set up lighting uniforms including shadow depth cubemaps
        for (int i = 0; i < NUM_LIGHTS; i++) {
            lights[i].SetUniforms(lightingShader, m_View);
            lights[i].BindBuffers(lightingShader);
        }
        gl.glUniform1f(gl.glGetUniformLocation(lightingShader.getProgram(), "farPlane"), Light.mFar);
        gl.glUniform1i(gl.glGetUniformLocation(lightingShader.getProgram(), "displayMode"), displayMode);
        RenderQuad();
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);

        //5th pass: radiosity
        if (useRadiosity ) {
            //glBindFramebuffer(GL_FRAMEBUFFER, 0);
            blur.BindFramebuffer();
            gl.glClear(GLenum.GL_COLOR_BUFFER_BIT);
            radiosityShader.enable();
            radiosity.BindBuffersRadiosity(radiosityShader, gbuffer);
            radiosity.SetUniforms(radiosityShader);
            gl.glUniform1i(gl.glGetUniformLocation(radiosityShader.getProgram(), "which"), whichRad);
            gl.glUniformMatrix4fv(gl.glGetUniformLocation(radiosityShader.getProgram(), "projection"), false, CacheBuffer.wrap(m_Proj));
            RenderQuad();
            gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);

            //6th: blur radiosity, combine with rest of lighting and display
            gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
            gl.glClear(GLenum.GL_COLOR_BUFFER_BIT | GLenum.GL_DEPTH_BUFFER_BIT);
            blurRadiosityShader.enable();
            blur.BindBuffersRadiosity(blurRadiosityShader);
            radiosity.BindBuffersBlur(blurRadiosityShader);
            RenderQuad();
        }

        //7th: render a cube for each light source
        //copy depth buffer from gbuffer to properly occlude light sources
        gl.glClear(GLenum.GL_DEPTH_BUFFER_BIT);
        gbuffer.CopyDepthBuffer();
        lightSourceShader.enable();
        gl.glUniformMatrix4fv(gl.glGetUniformLocation(lightSourceShader.getProgram(), "projection"), false, CacheBuffer.wrap(m_Proj));
        gl.glUniformMatrix4fv(gl.glGetUniformLocation(lightSourceShader.getProgram(), "view"), false, CacheBuffer.wrap(m_View));
        for (int i = 0; i < NUM_LIGHTS; i++) {
            m_Model.setTranslate(lights[i].position.x, lights[i].position.y, lights[i].position.z);
//            model = glm::translate(model, lights[i].position);
            gl.glUniformMatrix4fv(gl.glGetUniformLocation(lightSourceShader.getProgram(), "model"), false, CacheBuffer.wrap(m_Model));
            gl.glUniform3f(gl.glGetUniformLocation(lightSourceShader.getProgram(), "lightColor"), lights[i].specular.x, lights[i].specular.y, lights[i].specular.z);
            RenderCube();
        }

        //finally: render environment map wherever depth = infinity
        gl.glDepthFunc(GLenum.GL_LEQUAL);
        envShader.enable();
        /*model = glm::mat4();
        model = glm::scale(model, glm::vec3(2.0f));*/
        m_Model.setIdentity();
        m_Model.m00 = m_Model.m11 = m_Model.m22 = 2;
        //ignore translation component of view matrix
//        view = glm::mat4(glm::mat3(camera.GetViewMatrix()));
        m_View.m30 = m_Model.m31 = m_Model.m32 = 0;
        gl.glUniformMatrix4fv(gl.glGetUniformLocation(envShader.getProgram(), "model"),false, CacheBuffer.wrap(m_Model));
        gl.glUniformMatrix4fv(gl.glGetUniformLocation(envShader.getProgram(), "view"), false, CacheBuffer.wrap(m_View));
        gl.glUniformMatrix4fv(gl.glGetUniformLocation(envShader.getProgram(), "projection"), false, CacheBuffer.wrap(m_Proj));
        envMap.BindBuffers(envShader);
        RenderCube();
    }

    @Override
    protected void reshape(int width, int height) {
        if(width <= 0 || height <= 0)
            return;

        Matrix4f.perspective(60, (float)width/height, Light.mNear, Light.mFar, m_Proj);
    }

    private GLSLProgram createShader(String vert, String frag){
        final String root = "gpupro\\glitter\\shaders\\";
        return GLSLProgram.createProgram(root + vert, root + frag, null);
    }

    private GLSLProgram createShader(String vert, String frag, String gemo){
        final String root = "gpupro\\glitter\\shaders\\";
        return GLSLProgram.createProgram(root + vert,  root+gemo,root + frag, null);
    }

    // RenderQuad() Renders a quad that fills the screen
    private int quadVAO = 0;
    private int quadVBO;
    void RenderQuad()
    {
        if (quadVAO == 0)
        {
            float quadVertices[] = {
                    // Positions        // Texture Coords
                    -1.0f,  1.0f, 0.0f, 0.0f, 1.0f,
                    -1.0f, -1.0f, 0.0f, 0.0f, 0.0f,
                    1.0f,  1.0f, 0.0f, 1.0f, 1.0f,
                    1.0f, -1.0f, 0.0f, 1.0f, 0.0f,
            };
            // Setup plane VAO
            quadVAO = gl.glGenVertexArray();
            quadVBO = gl.glGenBuffer();
            gl.glBindVertexArray(quadVAO);
            gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, quadVBO);
            gl.glBufferData(GLenum.GL_ARRAY_BUFFER, CacheBuffer.wrap(quadVertices), GLenum.GL_STATIC_DRAW);
            gl.glEnableVertexAttribArray(0);
            gl.glVertexAttribPointer(0, 3, GLenum.GL_FLOAT, false, 5 * /*sizeof(GLfloat)*/4, 0);
            gl.glEnableVertexAttribArray(1);
            gl.glVertexAttribPointer(1, 2, GLenum.GL_FLOAT, false, 5 * /*sizeof(GLfloat)*/4, (3 * /*sizeof(GLfloat)*/4));
        }
        gl.glBindVertexArray(quadVAO);
        gl.glDrawArrays(GLenum.GL_TRIANGLE_STRIP, 0, 4);
        gl.glBindVertexArray(0);
    }

    // RenderCube() Renders a 1x1 3D cube in NDC.
    private int cubeVAO = 0;
    private int cubeVBO = 0;
    void RenderCube()
    {
        // Initialize (if necessary)
        if (cubeVAO == 0)
        {
            float vertices[] = {
                    // Back face
                    -0.5f, -0.5f, -0.5f,  0.0f,  0.0f, -1.0f, 0.0f, 0.0f, // Bottom-left
                    0.5f,  0.5f, -0.5f,  0.0f,  0.0f, -1.0f, 1.0f, 1.0f, // top-right
                    0.5f, -0.5f, -0.5f,  0.0f,  0.0f, -1.0f, 1.0f, 0.0f, // bottom-right
                    0.5f,  0.5f, -0.5f,  0.0f,  0.0f, -1.0f, 1.0f, 1.0f,  // top-right
                    -0.5f, -0.5f, -0.5f,  0.0f,  0.0f, -1.0f, 0.0f, 0.0f,  // bottom-left
                    -0.5f,  0.5f, -0.5f,  0.0f,  0.0f, -1.0f, 0.0f, 1.0f,// top-left
                    // Front face
                    -0.5f, -0.5f,  0.5f,  0.0f,  0.0f,  1.0f, 0.0f, 0.0f, // bottom-left
                    0.5f, -0.5f,  0.5f,  0.0f,  0.0f,  1.0f, 1.0f, 0.0f,  // bottom-right
                    0.5f,  0.5f,  0.5f,  0.0f,  0.0f,  1.0f, 1.0f, 1.0f,  // top-right
                    0.5f,  0.5f,  0.5f,  0.0f,  0.0f,  1.0f, 1.0f, 1.0f, // top-right
                    -0.5f,  0.5f,  0.5f,  0.0f,  0.0f,  1.0f, 0.0f, 1.0f,  // top-left
                    -0.5f, -0.5f,  0.5f,  0.0f,  0.0f,  1.0f, 0.0f, 0.0f,  // bottom-left
                    // Left face
                    -0.5f,  0.5f,  0.5f, -1.0f,  0.0f,  0.0f, 1.0f, 0.0f, // top-right
                    -0.5f,  0.5f, -0.5f, -1.0f,  0.0f,  0.0f, 1.0f, 1.0f, // top-left
                    -0.5f, -0.5f, -0.5f, -1.0f,  0.0f,  0.0f, 0.0f, 1.0f,  // bottom-left
                    -0.5f, -0.5f, -0.5f, -1.0f,  0.0f,  0.0f, 0.0f, 1.0f, // bottom-left
                    -0.5f, -0.5f,  0.5f, -1.0f,  0.0f,  0.0f, 0.0f, 0.0f,  // bottom-right
                    -0.5f,  0.5f,  0.5f, -1.0f,  0.0f,  0.0f, 1.0f, 0.0f, // top-right
                    // Right face
                    0.5f,  0.5f,  0.5f,  1.0f,  0.0f,  0.0f, 1.0f, 0.0f, // top-left
                    0.5f, -0.5f, -0.5f,  1.0f,  0.0f,  0.0f, 0.0f, 1.0f, // bottom-right
                    0.5f,  0.5f, -0.5f,  1.0f,  0.0f,  0.0f, 1.0f, 1.0f, // top-right
                    0.5f, -0.5f, -0.5f,  1.0f,  0.0f,  0.0f, 0.0f, 1.0f,  // bottom-right
                    0.5f,  0.5f,  0.5f,  1.0f,  0.0f,  0.0f, 1.0f, 0.0f,  // top-left
                    0.5f, -0.5f,  0.5f,  1.0f,  0.0f,  0.0f, 0.0f, 0.0f, // bottom-left
                    // Bottom face
                    -0.5f, -0.5f, -0.5f,  0.0f, -1.0f,  0.0f, 0.0f, 1.0f, // top-right
                    0.5f, -0.5f, -0.5f,  0.0f, -1.0f,  0.0f, 1.0f, 1.0f, // top-left
                    0.5f, -0.5f,  0.5f,  0.0f, -1.0f,  0.0f, 1.0f, 0.0f,// bottom-left
                    0.5f, -0.5f,  0.5f,  0.0f, -1.0f,  0.0f, 1.0f, 0.0f, // bottom-left
                    -0.5f, -0.5f,  0.5f,  0.0f, -1.0f,  0.0f, 0.0f, 0.0f, // bottom-right
                    -0.5f, -0.5f, -0.5f,  0.0f, -1.0f,  0.0f, 0.0f, 1.0f, // top-right
                    // Top face
                    -0.5f,  0.5f, -0.5f,  0.0f,  1.0f,  0.0f, 0.0f, 1.0f,// top-left
                    0.5f,  0.5f , 0.5f,  0.0f,  1.0f,  0.0f, 1.0f, 0.0f, // bottom-right
                    0.5f,  0.5f, -0.5f,  0.0f,  1.0f,  0.0f, 1.0f, 1.0f, // top-right
                    0.5f,  0.5f,  0.5f,  0.0f,  1.0f,  0.0f, 1.0f, 0.0f, // bottom-right
                    -0.5f,  0.5f, -0.5f,  0.0f,  1.0f,  0.0f, 0.0f, 1.0f,// top-left
                    -0.5f,  0.5f,  0.5f,  0.0f,  1.0f,  0.0f, 0.0f, 0.0f // bottom-left
            };
            cubeVAO = gl.glGenVertexArray();
            cubeVBO = gl.glGenBuffer();
            // Fill buffer
            gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, cubeVBO);
            gl.glBufferData(GLenum.GL_ARRAY_BUFFER, CacheBuffer.wrap(vertices), GLenum.GL_STATIC_DRAW);
            // Link vertex attributes
            gl.glBindVertexArray(cubeVAO);
            gl.glEnableVertexAttribArray(0);
            gl.glVertexAttribPointer(0, 3, GLenum.GL_FLOAT, false, 8 * /*sizeof(GLfloat)*/4, 0);
            gl.glEnableVertexAttribArray(1);
            gl.glVertexAttribPointer(1, 3, GLenum.GL_FLOAT, false, 8 * /*sizeof(GLfloat)*/4, (3 * 4));
            gl.glEnableVertexAttribArray(2);
            gl.glVertexAttribPointer(2, 2, GLenum.GL_FLOAT, false, 8 * /*sizeof(GLfloat)*/4, (6 * 4));
            gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
            gl.glBindVertexArray(0);
        }
        // Render Cube
        gl.glBindVertexArray(cubeVAO);
        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 36);
        gl.glBindVertexArray(0);
    }
}

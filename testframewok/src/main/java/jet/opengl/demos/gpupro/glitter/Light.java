package jet.opengl.demos.gpupro.glitter;

import org.lwjgl.util.vector.Matrix;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.ReadableVector4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import jet.opengl.demos.nvidia.shadows.ShadowMapGenerator;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.util.CacheBuffer;

final class Light {
    static final int SHADOW_MAP_RESOLUTION = 1024;
    static final float SHADOW_NEAR = 1.0f;
    static final float SHADOW_FAR = 25.0f;

    static final int mWidth = 1000;
    static final int mHeight = 800;
    static final float mNear = 0.1f;
    static final float mFar = 1000.f;

    //Shadows
    //depth map
    private int depthCubemap;
    //framebuffer
    private int depthMapFBO;

    //Unique ID
    private int id;

    private final Matrix4f m_ShadowProj = new Matrix4f();
    private final Matrix4f[] m_ShadowProjViews = new Matrix4f[6];

    //Light Properties
    //location
    final Vector3f position = new Vector3f();
    //colors
    final Vector3f ambient = new Vector3f();
    final Vector3f diffuse = new Vector3f();
    final Vector3f specular = new Vector3f();
    //attenuation constants
    float a, b, c;

    Light() {
        ambient.set(0.1f, 0.1f, 0.1f);
        diffuse.set(0.7f, 0.7f, 0.7f);
        specular.set(1.0f, 1.0f, 1.0f);
        a = 0.017f;
        b = 0.07f;
        c = 1.0f;
    }

    Light(ReadableVector3f position, ReadableVector3f color, float a, float b, float c, int id) {
        this.position.set(position);
//        this.ambient = color * 0.4f;
        Vector3f.scale(color, 0.4f, ambient);
//        this->diffuse = color * 0.7f;
        Vector3f.scale(color, 0.7f, diffuse);
        this.specular.set(color);
        this.a = a;
        this.b = b;
        this.c = c;
        this.id = id;

        initializeDepthMap();
    }

    Light(ReadableVector3f position, ReadableVector3f ambient, ReadableVector3f diffuse, ReadableVector3f specular,
          float a, float b, float c, int id) {
        this.position.set(position);
        this.ambient.set(ambient);
        this.diffuse.set(diffuse);
        this.specular.set(specular);
        this.a = a;
        this.b = b;
        this.c = c;
        this.id = id;

        initializeDepthMap();
    }

    void BindFramebuffer(GLSLProgram depthShader, Matrix4f view) {
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        gl.glViewport(0, 0, SHADOW_MAP_RESOLUTION, SHADOW_MAP_RESOLUTION);
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, depthMapFBO);
        gl.glClear(GLenum.GL_DEPTH_BUFFER_BIT);

        //also set uniforms needed for depth map rendering
        //convert position to view space
//        glm::vec3 posView = glm::vec3(view * glm::vec4(position, 1.0));
        Vector3f posView = Matrix4f.transformVector(view, position, null);

        //transformations to light-space for each face
        /*std::vector<glm::mat4> shadowTransforms;
        //create light-space transformation matrices for each cube face
        glm::mat4 shadowProj = glm::perspective(glm::radians(90.0f), 1.0f, SHADOW_NEAR, SHADOW_FAR);
        shadowTransforms.push_back(shadowProj * glm::lookAt(posView, posView + glm::vec3(1.0, 0.0, 0.0), glm::vec3(0.0, -1.0, 0.0)));
        shadowTransforms.push_back(shadowProj * glm::lookAt(posView, posView + glm::vec3(-1.0, 0.0, 0.0), glm::vec3(0.0, -1.0, 0.0)));
        shadowTransforms.push_back(shadowProj * glm::lookAt(posView, posView + glm::vec3(0.0, 1.0, 0.0), glm::vec3(0.0, 0.0, 1.0)));
        shadowTransforms.push_back(shadowProj * glm::lookAt(posView, posView + glm::vec3(0.0, -1.0, 0.0), glm::vec3(0.0, 0.0, -1.0)));
        shadowTransforms.push_back(shadowProj * glm::lookAt(posView, posView + glm::vec3(0.0, 0.0, 1.0), glm::vec3(0.0, -1.0, 0.0)));
        shadowTransforms.push_back(shadowProj * glm::lookAt(posView, posView + glm::vec3(0.0, 0.0, -1.0), glm::vec3(0.0, -1.0, 0.0)));*/

        ShadowMapGenerator.buildCubeShadowMatrices(posView, SHADOW_NEAR, SHADOW_FAR, m_ShadowProj, m_ShadowProjViews);
        for(int i = 0; i < m_ShadowProjViews.length; i++){
            Matrix4f.mul(m_ShadowProj, m_ShadowProjViews[i], m_ShadowProjViews[i]);
        }

        //set uniforms
        depthShader.enable();
//        for (int i = 0; i < 6; ++i)
//            glUniformMatrix4fv(glGetUniformLocation(depthShader.Program, ("shadowMatrices[" + std::to_string(i) + "]")), 1, GL_FALSE, glm::value_ptr(shadowTransforms[i]));
        int loc = depthShader.getUniformLocation("shadowMatrices");
        gl.glUniformMatrix4fv(loc, false, CacheBuffer.wrap(m_ShadowProjViews));

        gl.glUniform1f(gl.glGetUniformLocation(depthShader.getProgram(), "farPlane"), mFar);
        gl.glUniform3f(gl.glGetUniformLocation(depthShader.getProgram(), "lightPos"), posView.x, posView.y, posView.z);
    }

    void SetUniforms(GLSLProgram lightShader, Matrix4f view) {
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        lightShader.enable();
        //light properties
        //convert position to view space
//        glm::vec3 lightPosView = glm::vec3(view * glm::vec4(position, 1.0));
        Vector3f lightPosView = Matrix4f.transformVector(view, position, null);
        gl.glUniform3f(gl.glGetUniformLocation(lightShader.getProgram(), ("lights[" + id + "].pos")), lightPosView.x, lightPosView.y, lightPosView.z);
        gl.glUniform3f(gl.glGetUniformLocation(lightShader.getProgram(), ("lights[" + id + "].ambient")), ambient.x, ambient.y, ambient.z);
        gl.glUniform3f(gl.glGetUniformLocation(lightShader.getProgram(), ("lights[" + id + "].diffuse")), diffuse.x, diffuse.y, diffuse.z);
        gl.glUniform3f(gl.glGetUniformLocation(lightShader.getProgram(), ("lights[" + id + "].specular")), specular.x, specular.y, specular.z);
        gl.glUniform1f(gl.glGetUniformLocation(lightShader.getProgram(), ("lights[" + id + "].a")), a);
        gl.glUniform1f(gl.glGetUniformLocation(lightShader.getProgram(), ("lights[" + id + "].b")), b);
        gl.glUniform1f(gl.glGetUniformLocation(lightShader.getProgram(), ("lights[" + id + "].c")), c);
    }

    void BindBuffers(GLSLProgram lightShader) {
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        //bind depthmap to texture unit #id
        gl.glActiveTexture(GLenum.GL_TEXTURE0 + id);
        gl.glUniform1i(gl.glGetUniformLocation(lightShader.getProgram(), ("depthMap[" + id + "]")), id);
        gl.glBindTexture(GLenum.GL_TEXTURE_CUBE_MAP, depthCubemap);
    }

    private void initializeDepthMap() {
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        //create shadow cubemap
        depthCubemap = gl.glGenTexture();
        gl.glBindTexture(GLenum.GL_TEXTURE_CUBE_MAP, depthCubemap);
        for (int i = 0; i < 6; i++)
            gl.glTexImage2D(GLenum.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, GLenum.GL_DEPTH_COMPONENT32F, SHADOW_MAP_RESOLUTION, SHADOW_MAP_RESOLUTION, 0, GLenum.GL_DEPTH_COMPONENT, GLenum.GL_FLOAT, null);
        gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_NEAREST);
        gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_NEAREST);
        gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_WRAP_R, GLenum.GL_CLAMP_TO_EDGE);
        //create FBO
        depthMapFBO = gl.glGenFramebuffer();
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, depthMapFBO);
        gl.glFramebufferTexture(GLenum.GL_FRAMEBUFFER, GLenum.GL_DEPTH_ATTACHMENT, depthCubemap, 0);
        gl.glDrawBuffers(GLenum.GL_NONE);
        gl.glReadBuffer(GLenum.GL_NONE);
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
    }
}

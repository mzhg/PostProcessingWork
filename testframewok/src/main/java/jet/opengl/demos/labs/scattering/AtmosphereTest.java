package jet.opengl.demos.labs.scattering;

import com.nvidia.developer.opengl.app.NvCameraMotionType;
import com.nvidia.developer.opengl.app.NvSampleApp;
import com.nvidia.developer.opengl.models.DrawMode;
import com.nvidia.developer.opengl.models.GLVAO;
import com.nvidia.developer.opengl.models.QuadricBuilder;
import com.nvidia.developer.opengl.models.QuadricMesh;
import com.nvidia.developer.opengl.models.QuadricSphere;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.BitSet;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.FramebufferGL;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureAttachDesc;
import jet.opengl.postprocessing.texture.TextureDataDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.Numeric;

public class AtmosphereTest extends NvSampleApp {

    private final Vector3f m_vLight = new Vector3f(0, 0, 1000);
    private final Vector3f m_vLightDirection = new Vector3f();

    // Variables that can be tweaked with keypresses
    private BitSet m_bUseHDR;
    private int m_nSamples;
    private int m_nPolygonMode;
    private float m_Kr, m_Kr4PI;
    private float m_Km, m_Km4PI;
    private float m_ESun;
    private float m_g;
    private float m_fExposure;

    private float m_fInnerRadius;
    private float m_fOuterRadius;
    private float m_fScale;
    private final float[] m_fWavelength = new float[3];
    private final float[] m_fWavelength4 = new float[3];
    private float m_fRayleighScaleDepth;
    private float m_fMieScaleDepth;
//    CPixelBuffer m_pbOpticalDepth;

    private Texture2D m_tMoonGlow;
    private Texture2D m_tEarth;
    private Texture2D m_tCloudCell;
    private Texture2D m_t1DGlow;

    private GLSLProgram m_shSkyFromSpace;
    private GLSLProgram m_shSkyFromAtmosphere;
    private GLSLProgram m_shGroundFromSpace;
    private GLSLProgram m_shGroundFromAtmosphere;
    private GLSLProgram m_shSpaceFromSpace;
    private GLSLProgram m_shSpaceFromAtmosphere;
    private GLSLProgram m_shTexture;

    private BufferGL m_quadT3;
    private BufferGL m_quad;
    private GLVAO    mInnerSphere;
    private GLVAO    mOuterSphere;

    private FramebufferGL m_pBuffer;
    private GLFuncProvider gl;

    private final Matrix4f mProj = new Matrix4f();
    private final Matrix4f mView = new Matrix4f();

    private final Params mParams = new Params();
    private boolean mPrintOnce;

    @Override
    protected void initRendering() {
        gl = GLFuncProviderFactory.getGLFuncProvider();

        m_transformer.setMotionMode(NvCameraMotionType.FIRST_PERSON);
        m_transformer.setTranslation(0, 0, 11);

        m_pBuffer = new FramebufferGL();
        m_pBuffer.bind();
        m_pBuffer.addTexture2D(new Texture2DDesc(1280, 720, GLenum.GL_RGBA8), new TextureAttachDesc());  // RGBA16F
        m_pBuffer.addTexture2D(new Texture2DDesc(1280, 720, GLenum.GL_DEPTH24_STENCIL8), new TextureAttachDesc());  // depth-stencil buffer
        m_pBuffer.unbind();

        CPixelBuffer pb = new CPixelBuffer();

        // Initialize the shared cloud cell texture
        pb.Init(16, 16, 1, 2, GLenum.GL_RG8);  // GL_LUMINANCE_ALPHA
        pb.MakeCloudCell(2, 0);
//        m_tCloudCell.Init(&pb);
        Texture2DDesc desc = new Texture2DDesc(16, 16, GLenum.GL_RG8);
        TextureDataDesc initData = new TextureDataDesc(GLenum.GL_RG, GLenum.GL_UNSIGNED_BYTE, pb.GetBuffer());
        m_tCloudCell = TextureUtils.createTexture2D(desc, initData);

        pb.Init(64, 1, 1, 2, GLenum.GL_RG8);
        pb.MakeGlow1D();
        desc = new Texture2DDesc(64, 1, GLenum.GL_RG8);
        initData = new TextureDataDesc(GLenum.GL_RG, GLenum.GL_UNSIGNED_BYTE, pb.GetBuffer());
        m_t1DGlow = TextureUtils.createTexture2D(desc, initData);

        pb.Init(256, 256, 1,1, GLenum.GL_R8);
        pb.MakeGlow2D(40.0f, 0.1f);
        desc = new Texture2DDesc(256, 256, GLenum.GL_R8);
        initData = new TextureDataDesc(GLenum.GL_RED, GLenum.GL_UNSIGNED_BYTE, pb.GetBuffer());
        m_tMoonGlow= TextureUtils.createTexture2D(desc, initData);

        final float[] vertsT3 = {
            -4,4,-50,0,0,
            -4.0f, -4.0f, -50.0f,0, 1,
            4.0f, -4.0f, -50.0f, 1, 1,
            4.0f, 4.0f, -50.0f,1, 0
        };
        FloatBuffer vertsBuffer = CacheBuffer.wrap(vertsT3);
        m_quadT3 = new BufferGL();
        m_quadT3.initlize(GLenum.GL_ARRAY_BUFFER, 4 * vertsT3.length, vertsBuffer, GLenum.GL_STATIC_DRAW);

        final float[] verts = {
          -1, -1, 0,0,
          +1, -1, 1, 0,
          -1, 1, 0, 1,
          +1, +1, 1,1
        };
        vertsBuffer = CacheBuffer.wrap(verts);
        m_quad = new BufferGL();
        m_quad.initlize(GLenum.GL_ARRAY_BUFFER, 4 * verts.length, vertsBuffer, GLenum.GL_STATIC_DRAW);
        m_quad.unbind();

//        m_vLightDirection = m_vLight / m_vLight.Magnitude();
        m_vLight.normalise(m_vLightDirection);

        m_nSamples = 3;		// Number of sample rays to use in integral equation
        m_Kr = 0.0025f;		// Rayleigh scattering constant
        m_Kr4PI = m_Kr*4.0f* Numeric.PI;
        m_Km = 0.0010f;		// Mie scattering constant
        m_Km4PI = m_Km*4.0f*Numeric.PI;
        m_ESun = 20.0f;		// Sun brightness constant
        m_g = -0.990f;		// The Mie phase asymmetry factor
        m_fExposure = 2.0f;

        m_fInnerRadius = 10.0f;
        m_fOuterRadius = 10.25f;
        m_fScale = 1 / (m_fOuterRadius - m_fInnerRadius);

        m_fWavelength[0] = 0.650f;		// 650 nm for red
        m_fWavelength[1] = 0.570f;		// 570 nm for green
        m_fWavelength[2] = 0.475f;		// 475 nm for blue
        m_fWavelength4[0] = (float)Math.pow(m_fWavelength[0], 4.0f);
        m_fWavelength4[1] = (float)Math.pow(m_fWavelength[1], 4.0f);
        m_fWavelength4[2] = (float)Math.pow(m_fWavelength[2], 4.0f);

        m_fRayleighScaleDepth = 0.25f;
        m_fMieScaleDepth = 0.1f;

        try {
            final String root = "labs\\AtmosphereTest\\shaders\\";
            m_shTexture = GLSLProgram.createFromFiles(root + "TextureVS.vert", root + "SpaceFromSpace.frag");
            m_shTexture.setName("Texture");
            m_shSkyFromSpace = GLSLProgram.createFromFiles(root + "SkyFromSpace.vert", root + "SkyFromSpace.frag");
            m_shSkyFromSpace.setName("SkyFromSpace");
            m_shSkyFromAtmosphere = GLSLProgram.createFromFiles(root + "SkyFromAtmosphere.vert", root + "SkyFromAtmosphere.frag");
            m_shSkyFromAtmosphere.setName("SkyFromAtmosphere");
            m_shGroundFromSpace = GLSLProgram.createFromFiles(root + "GroundFromSpace.vert", root + "GroundFromSpace.frag");
            m_shGroundFromSpace.setName("GroundFromSpace");
            m_shGroundFromAtmosphere = GLSLProgram.createFromFiles(root + "GroundFromAtmosphere.vert", root + "GroundFromAtmosphere.frag");
            m_shGroundFromAtmosphere.setName("GroundFromAtmosphere");
            m_shSpaceFromSpace = GLSLProgram.createFromFiles(root + "SpaceFromSpace.vert", root + "SpaceFromSpace.frag");
            m_shSpaceFromSpace.setName("SpaceFromSpace");
            m_shSpaceFromAtmosphere = GLSLProgram.createFromFiles(root + "SpaceFromAtmosphere.vert", root + "SpaceFromAtmosphere.frag");
            m_shSpaceFromAtmosphere.setName("SpaceFromAtmosphere");

            m_tEarth = TextureUtils.createTexture2DFromFile("labs\\AtmosphereTest\\textures\\earthmap1k.jpg", false, true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        QuadricBuilder builder = new QuadricBuilder();
        builder.setXSteps(30).setYSteps(30);
        builder.setDrawMode(DrawMode.FILL);
        builder.setCenterToOrigin(true);
        builder.setPostionLocation(0);
        builder.setNormalLocation(1);
        builder.setTexCoordLocation(2);

        mInnerSphere = new QuadricMesh(builder, new QuadricSphere(m_fInnerRadius)).getModel().genVAO();
        mOuterSphere = new QuadricMesh(builder, new QuadricSphere(m_fOuterRadius)).getModel().genVAO();
    }

    @Override
    public void display() {
        m_transformer.getModelViewMat(mView);
        Matrix4f.mul(mProj, mView, mParams.uMVP);
        Matrix4f.decompseRigidMatrix(mView, mParams.v3CameraPos, null, null);

        m_pBuffer.bind();
        gl.glViewport(0,0,1280, 720);
        gl.glEnable(GLenum.GL_DEPTH_TEST);
        gl.glClearDepthf(1.f);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT|GLenum.GL_DEPTH_BUFFER_BIT);

        Vector3f vCamera = mParams.v3CameraPos;

        // 0, render the moon
        GLSLProgram pSpaceShader = null;
        if(vCamera.length() < m_fOuterRadius)
            pSpaceShader = m_shSpaceFromAtmosphere;
        else if(vCamera.z > 0.0f)
            pSpaceShader = m_shSpaceFromSpace;
        else
            pSpaceShader = m_shTexture;

        pSpaceShader.enable();
        setUniforms(pSpaceShader);
        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(m_tMoonGlow.getTarget(), m_tMoonGlow.getTexture());

        m_quadT3.bind();
        gl.glVertexAttribPointer(0, 3, GLenum.GL_FLOAT, false, 20, 0);
        gl.glEnableVertexAttribArray(0);
        gl.glVertexAttribPointer(1, 2, GLenum.GL_FLOAT, false, 20, 12);
        gl.glEnableVertexAttribArray(1);
        gl.glDrawArrays(GLenum.GL_TRIANGLE_FAN, 0, 4);
        m_quadT3.unbind();
        gl.glBindTexture(m_tMoonGlow.getTarget(), 0);
        gl.glDisableVertexAttribArray(0);
        gl.glDisableVertexAttribArray(1);

        if(pSpaceShader != null)
            gl.glUseProgram(0);

        // 1, render the ground
        GLSLProgram pGroundShader;
        if(vCamera.length() >= m_fOuterRadius)
            pGroundShader = m_shGroundFromSpace;
        else
            pGroundShader = m_shGroundFromAtmosphere;
        pGroundShader.enable();
        setUniforms(pGroundShader);
        gl.glBindTexture(m_tEarth.getTarget(), m_tEarth.getTexture());
        mInnerSphere.bind();
        mInnerSphere.draw(GLenum.GL_TRIANGLES);
        mInnerSphere.unbind();
        pGroundShader.disable();

        // 2, render the sky
        GLSLProgram pSkyShader;
        if(vCamera.length() >= m_fOuterRadius)
            pSkyShader = m_shSkyFromSpace;
	    else
            pSkyShader = m_shSkyFromAtmosphere;
        pSkyShader.enable();
        setUniforms(pSkyShader);
        gl.glEnable(GLenum.GL_BLEND);
        gl.glBlendFunc(GLenum.GL_ONE, GLenum.GL_ONE);
        mOuterSphere.bind();
        mOuterSphere.draw(GLenum.GL_TRIANGLES);
        mOuterSphere.unbind();
        pSkyShader.disable();

        // 3, blit the results to default framebuffer.
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
        gl.glViewport(0,0, getGLContext().width(), getGLContext().height());
        gl.glDisable(GLenum.GL_DEPTH_TEST);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT|GLenum.GL_DEPTH_BUFFER_BIT);

        gl.glBindFramebuffer(GLenum.GL_DRAW_FRAMEBUFFER,0);
        gl.glBindFramebuffer(GLenum.GL_READ_FRAMEBUFFER, m_pBuffer.getFramebuffer());
        gl.glBlitFramebuffer(0, 0, 1280, 720,
                0, 0, getGLContext().width(), getGLContext().height(),
                GLenum.GL_COLOR_BUFFER_BIT, GLenum.GL_NEAREST);

        gl.glBindFramebuffer(GLenum.GL_READ_FRAMEBUFFER, 0);

        /*gl.glBindTexture(m_pBuffer.getAttachedTex(0).getTarget(), m_pBuffer.getAttachedTex(0).getTexture());
        m_quad.bind();
        gl.glVertexAttribPointer(0, 2, GLenum.GL_FLOAT, false, 16, 0);
        gl.glEnableVertexAttribArray(0);
        gl.glVertexAttribPointer(1, 2, GLenum.GL_FLOAT, false, 16, 8);
        gl.glEnableVertexAttribArray(1);
        gl.glDrawArrays(GLenum.GL_TRIANGLE_STRIP, 0, 4);
        m_quad.unbind();
        gl.glDisableVertexAttribArray(0);
        gl.glDisableVertexAttribArray(1);
        m_shTexture.disable();*/

        mPrintOnce=true;
    }

    @Override
    protected void reshape(int width, int height) {
        if(width <= 0 || height <=0)
            return;

        Matrix4f.perspective(60, (float)width/height, 0.1f, 1000.f, mProj);
    }

    private void setUniforms(GLSLProgram program){
        int index;
        index = program.getUniformLocation("v3CameraPos", true);
        if(index >= 0) gl.glUniform3f(index, mParams.v3CameraPos.x, mParams.v3CameraPos.y, mParams.v3CameraPos.z);

        index = program.getUniformLocation("v3LightPos" , true);
        if(index >= 0) gl.glUniform3f(index, m_vLightDirection.x, m_vLightDirection.y, m_vLightDirection.z);

        index = program.getUniformLocation("v3InvWavelength", true);
        if(index >= 0) gl.glUniform3f(index, 1/m_fWavelength4[0], 1/m_fWavelength4[1], 1/m_fWavelength4[2]);

        index = program.getUniformLocation("fCameraHeight", true);
        if(index >= 0) gl.glUniform1f(index, mParams.v3CameraPos.length());

        index = program.getUniformLocation("fCameraHeight2", true);
        if(index >= 0) gl.glUniform1f(index, mParams.v3CameraPos.lengthSquared());

        index = program.getUniformLocation("fOuterRadius", true);
        if(index >= 0) gl.glUniform1f(index, m_fOuterRadius);

        index = program.getUniformLocation("fOuterRadius2", true);
        if(index >= 0) gl.glUniform1f(index, m_fOuterRadius * m_fOuterRadius);

        index = program.getUniformLocation("fInnerRadius", true);
        if(index >= 0) gl.glUniform1f(index, m_fInnerRadius);

        index = program.getUniformLocation("fInnerRadius2", true);
        if(index >= 0) gl.glUniform1f(index, m_fInnerRadius * m_fInnerRadius);

        index = program.getUniformLocation("fKrESun", true);
        if(index >= 0) gl.glUniform1f(index, m_Kr*m_ESun);

        index = program.getUniformLocation("fKmESun", true);
        if(index >= 0) gl.glUniform1f(index, m_Km*m_ESun);

        index = program.getUniformLocation("fKr4PI", true);
        if(index >= 0) gl.glUniform1f(index, m_Kr4PI);

        index = program.getUniformLocation("fKm4PI", true);
        if(index >= 0) gl.glUniform1f(index, m_Km4PI);

        index = program.getUniformLocation("fScale", true);
        if(index >= 0) gl.glUniform1f(index, 1.0f / (m_fOuterRadius - m_fInnerRadius));

        index = program.getUniformLocation("fScaleDepth", true);
        if(index >= 0) gl.glUniform1f(index, m_fRayleighScaleDepth);

        index = program.getUniformLocation("fScaleOverScaleDepth", true);
        if(index >= 0) gl.glUniform1f(index, (1.0f / (m_fOuterRadius - m_fInnerRadius)) / m_fRayleighScaleDepth);

        index = program.getUniformLocation("g", true);
        if(index >= 0) gl.glUniform1f(index, m_g);

        index = program.getUniformLocation("g2", true);
        if(index >= 0) gl.glUniform1f(index, m_g * m_g);

        index = program.getUniformLocation("s2Test", true);
        if(index >= 0) gl.glUniform1i(index, 0);

        index = program.getUniformLocation("uMVP", true);
        if(index >= 0) gl.glUniformMatrix4fv(index, false, CacheBuffer.wrap(mParams.uMVP));

        if(!mPrintOnce)
            program.printPrograminfo();
    }

    private static final class Params{
        /** The camera's current position */
        final Vector3f v3CameraPos = new Vector3f();
        final Matrix4f  uMVP = new Matrix4f();
    }
}

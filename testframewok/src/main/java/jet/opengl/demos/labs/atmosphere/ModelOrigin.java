package jet.opengl.demos.labs.atmosphere;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.RenderTargets;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.Texture3D;
import jet.opengl.postprocessing.texture.Texture3DDesc;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.texture.TextureUtils;

class ModelOrigin {
     static final float Rg = 6360.0f;
     static final float Rt = 6420.0f;
     static final float RL = 6421.0f;

     static final int TRANSMITTANCE_W = 256;
     static final int TRANSMITTANCE_H = 64;

     static final int SKY_W = 64;
     static final int SKY_H = 16;

     static final int RES_R = 32;
     static final int RES_MU = 128;
     static final int RES_MU_S = 32;
     static final int RES_NU = 8;

     private static final int reflectanceUnit = 0;
    private static final int transmittanceUnit = 1;
    private static final int irradianceUnit = 2;
    private static final int inscatterUnit = 3;
    private static final int deltaEUnit = 4;
    private static final int deltaSRUnit = 5;
    private static final int deltaSMUnit = 6;
    private static final int deltaJUnit = 7;

    private Texture2D transmittanceTexture;//unit 1, T table
    private Texture2D irradianceTexture;//unit 2, E table
    private Texture3D inscatterTexture;//unit 3, S table
    private Texture2D deltaETexture;//unit 4, deltaE table
    private Texture3D deltaSRTexture;//unit 5, deltaS table (Rayleigh part)
    private Texture3D deltaSMTexture;//unit 6, deltaS table (Mie part)
    private Texture3D deltaJTexture;//unit 7, deltaJ table

    private GLSLProgram transmittanceProg;
    private GLSLProgram irradiance1Prog;
    private GLSLProgram inscatter1Prog;
    private GLSLProgram copyIrradianceProg;
    private GLSLProgram copyInscatter1Prog;
    private GLSLProgram jProg;
    private GLSLProgram irradianceNProg;
    private GLSLProgram inscatterNProg;
    private GLSLProgram copyInscatterNProg;

    private RenderTargets fbo;
     private GLFuncProvider gl;

     private int quad_vao;

     void Init(){
         gl = GLFuncProviderFactory.getGLFuncProvider();
         quad_vao = gl.glGenVertexArray();

         transmittanceTexture = NewTexture2d(TRANSMITTANCE_W, TRANSMITTANCE_H);
         irradianceTexture = NewTexture2d(SKY_W, SKY_H);
         inscatterTexture = NewTexture3d(RES_MU_S * RES_NU, RES_MU, RES_R);

         deltaSRTexture = NewTexture3d(RES_MU_S * RES_NU, RES_MU, RES_R);
         deltaSMTexture = NewTexture3d(RES_MU_S * RES_NU, RES_MU, RES_R);
         deltaJTexture = NewTexture3d(RES_MU_S * RES_NU, RES_MU, RES_R);
         deltaETexture = NewTexture2d(SKY_W, SKY_H);

         gl.glBindTextureUnit(transmittanceUnit, transmittanceTexture.getTexture());
         gl.glBindTextureUnit(irradianceUnit, irradianceTexture.getTexture());
         gl.glBindTextureUnit(inscatterUnit, inscatterTexture.getTexture());
         gl.glBindTextureUnit(deltaSRUnit, deltaSRTexture.getTexture());
         gl.glBindTextureUnit(deltaSMUnit, deltaSMTexture.getTexture());
         gl.glBindTextureUnit(deltaJUnit, deltaJTexture.getTexture());
         gl.glBindTextureUnit(deltaEUnit, deltaETexture.getTexture());

         int linearSampler = SamplerUtils.getDefaultSampler();

         for(int i = 0; i <= 7; i++){
             gl.glBindSampler(i, linearSampler);
         }

         // create shaders.
         final String shaderPath = "labs/Atmosphere/shaders/";
         final String kVertexShader = "shader_libs/PostProcessingDefaultScreenSpaceVS.vert";

         transmittanceProg = GLSLProgram.createProgram(kVertexShader, shaderPath+"transmittance.glsl", null);
         irradiance1Prog =GLSLProgram.createProgram(kVertexShader, shaderPath+"irradiance1.glsl", null);
         inscatter1Prog =GLSLProgram.createProgram(kVertexShader, shaderPath+"inscatter1.glsl", null);
         copyIrradianceProg =GLSLProgram.createProgram(kVertexShader, shaderPath+"copyIrradiance.glsl", null);
         copyInscatter1Prog =GLSLProgram.createProgram(kVertexShader, shaderPath+"copyInscatter1.glsl", null);
         jProg =GLSLProgram.createProgram(kVertexShader, shaderPath+"inscatterS.glsl", null);
         irradianceNProg =GLSLProgram.createProgram(kVertexShader, shaderPath+"irradianceN.glsl", null);
         copyInscatterNProg =GLSLProgram.createProgram(kVertexShader, shaderPath+"copyInscatterN.glsl", null);
         inscatterNProg =GLSLProgram.createProgram(kVertexShader, shaderPath+"inscatterN.glsl", null);

         fbo = new RenderTargets();
         fbo.bind();
         // computes transmittance texture T (line 1 in algorithm 4.1)
//         glFramebufferTextureEXT(GL_FRAMEBUFFER_EXT, GL_COLOR_ATTACHMENT0_EXT, transmittanceTexture, 0);
         fbo.setRenderTexture(transmittanceTexture, null);
         gl.glViewport(0, 0, TRANSMITTANCE_W, TRANSMITTANCE_H);
         transmittanceProg.enable();
         drawQuad();

         // computes irradiance texture deltaE (line 2 in algorithm 4.1)
//         glFramebufferTextureEXT(GL_FRAMEBUFFER_EXT, GL_COLOR_ATTACHMENT0_EXT, deltaETexture, 0);
         fbo.setRenderTexture(deltaETexture, null);
         gl.glViewport(0, 0, SKY_W, SKY_H);
         gl.glUseProgram(irradiance1Prog.getProgram());
         gl.glUniform1i(gl.glGetUniformLocation(irradiance1Prog.getProgram(), "transmittanceSampler"), transmittanceUnit);
         drawQuad();

         // computes single scattering texture deltaS (line 3 in algorithm 4.1)
         // Rayleigh and Mie separated in deltaSR + deltaSM
         /*glFramebufferTextureEXT(GL_FRAMEBUFFER_EXT, GL_COLOR_ATTACHMENT0_EXT, deltaSRTexture, 0);
         glFramebufferTextureEXT(GL_FRAMEBUFFER_EXT, GL_COLOR_ATTACHMENT1_EXT, deltaSMTexture, 0);
         unsigned int bufs[2] = { GL_COLOR_ATTACHMENT0_EXT, GL_COLOR_ATTACHMENT1_EXT };
         glDrawBuffers(2, bufs);*/

         fbo.setRenderTextures(new TextureGL[]{deltaSRTexture,deltaSMTexture}, null);
         gl.glViewport(0, 0, RES_MU_S * RES_NU, RES_MU);
         gl.glUseProgram(inscatter1Prog.getProgram());
         gl.glUniform1i(gl.glGetUniformLocation(inscatter1Prog.getProgram(), "transmittanceSampler"), transmittanceUnit);
         for (int layer = 0; layer < RES_R; ++layer) {
             setLayer(inscatter1Prog, layer);
             drawQuad();
         }
//         glFramebufferTexture2DEXT(GL_FRAMEBUFFER_EXT, GL_COLOR_ATTACHMENT1_EXT, GL_TEXTURE_2D, 0, 0);
//         glDrawBuffer(GL_COLOR_ATTACHMENT0_EXT);

         // copies deltaE into irradiance texture E (line 4 in algorithm 4.1)
//         glFramebufferTextureEXT(GL_FRAMEBUFFER_EXT, GL_COLOR_ATTACHMENT0_EXT, irradianceTexture, 0);
         fbo.setRenderTexture(irradianceTexture, null);
         gl.glViewport(0, 0, SKY_W, SKY_H);
         gl.glUseProgram(copyIrradianceProg.getProgram());
         gl.glUniform1f(gl.glGetUniformLocation(copyIrradianceProg.getProgram(), "k"), 0.0f);
         gl.glUniform1i(gl.glGetUniformLocation(copyIrradianceProg.getProgram(), "deltaESampler"), deltaEUnit);
         drawQuad();

         // copies deltaS into inscatter texture S (line 5 in algorithm 4.1)
//         glFramebufferTextureEXT(GL_FRAMEBUFFER_EXT, GL_COLOR_ATTACHMENT0_EXT, inscatterTexture, 0);
         fbo.setRenderTexture(inscatterTexture, null);
         gl.glViewport(0, 0, RES_MU_S * RES_NU, RES_MU);
         gl.glUseProgram(copyInscatter1Prog.getProgram());
         gl.glUniform1i(gl.glGetUniformLocation(copyInscatter1Prog.getProgram(), "deltaSRSampler"), deltaSRUnit);
         gl.glUniform1i(gl.glGetUniformLocation(copyInscatter1Prog.getProgram(), "deltaSMSampler"), deltaSMUnit);
         for (int layer = 0; layer < RES_R; ++layer) {
             setLayer(copyInscatter1Prog, layer);
             drawQuad();
         }

         // loop for each scattering order (line 6 in algorithm 4.1)
         for (int order = 2; order <= 4; ++order) {

             // computes deltaJ (line 7 in algorithm 4.1)
//             glFramebufferTextureEXT(GL_FRAMEBUFFER_EXT, GL_COLOR_ATTACHMENT0_EXT, deltaJTexture, 0);
             fbo.setRenderTexture(deltaJTexture, null);
             gl.glViewport(0, 0, RES_MU_S * RES_NU, RES_MU);
             gl.glUseProgram(jProg.getProgram());
             gl.glUniform1f(gl.glGetUniformLocation(jProg.getProgram(), "first"), order == 2 ? 1.0f : 0.0f);
             gl.glUniform1i(gl.glGetUniformLocation(jProg.getProgram(), "transmittanceSampler"), transmittanceUnit);
             gl.glUniform1i(gl.glGetUniformLocation(jProg.getProgram(), "deltaESampler"), deltaEUnit);
             gl.glUniform1i(gl.glGetUniformLocation(jProg.getProgram(), "deltaSRSampler"), deltaSRUnit);
             gl.glUniform1i(gl.glGetUniformLocation(jProg.getProgram(), "deltaSMSampler"), deltaSMUnit);
             for (int layer = 0; layer < RES_R; ++layer) {
                 setLayer(jProg, layer);
                 drawQuad();
             }

             // computes deltaE (line 8 in algorithm 4.1)
//             glFramebufferTextureEXT(GL_FRAMEBUFFER_EXT, GL_COLOR_ATTACHMENT0_EXT, deltaETexture, 0);
             fbo.setRenderTexture(deltaETexture, null);
             gl.glViewport(0, 0, SKY_W, SKY_H);
             gl.glUseProgram(irradianceNProg.getProgram());
             gl.glUniform1f(gl.glGetUniformLocation(irradianceNProg.getProgram(), "first"), order == 2 ? 1.0f : 0.0f);
             gl.glUniform1i(gl.glGetUniformLocation(irradianceNProg.getProgram(), "transmittanceSampler"), transmittanceUnit);
             gl.glUniform1i(gl.glGetUniformLocation(irradianceNProg.getProgram(), "deltaSRSampler"), deltaSRUnit);
             gl.glUniform1i(gl.glGetUniformLocation(irradianceNProg.getProgram(), "deltaSMSampler"), deltaSMUnit);
             drawQuad();

             // computes deltaS (line 9 in algorithm 4.1)
//             glFramebufferTextureEXT(GL_FRAMEBUFFER_EXT, GL_COLOR_ATTACHMENT0_EXT, deltaSRTexture, 0);
             fbo.setRenderTexture(deltaSRTexture, null);
             gl.glViewport(0, 0, RES_MU_S * RES_NU, RES_MU);
             gl.glUseProgram(inscatterNProg.getProgram());
             gl.glUniform1f(gl.glGetUniformLocation(inscatterNProg.getProgram(), "first"), order == 2 ? 1.0f : 0.0f);
             gl.glUniform1i(gl.glGetUniformLocation(inscatterNProg.getProgram(), "transmittanceSampler"), transmittanceUnit);
             gl.glUniform1i(gl.glGetUniformLocation(inscatterNProg.getProgram(), "deltaJSampler"), deltaJUnit);
             for (int layer = 0; layer < RES_R; ++layer) {
                 setLayer(inscatterNProg, layer);
                 drawQuad();
             }

             gl.glEnable(GLenum.GL_BLEND);
             gl.glBlendEquationSeparate(GLenum.GL_FUNC_ADD, GLenum.GL_FUNC_ADD);
             gl.glBlendFuncSeparate(GLenum.GL_ONE, GLenum.GL_ONE, GLenum.GL_ONE, GLenum.GL_ONE);

             // adds deltaE into irradiance texture E (line 10 in algorithm 4.1)
//             glFramebufferTextureEXT(GL_FRAMEBUFFER_EXT, GL_COLOR_ATTACHMENT0_EXT, irradianceTexture, 0);
             fbo.setRenderTexture(irradianceTexture, null);
             gl.glViewport(0, 0, SKY_W, SKY_H);
             gl.glUseProgram(copyIrradianceProg.getProgram());
             gl.glUniform1f(gl.glGetUniformLocation(copyIrradianceProg.getProgram(), "k"), 1.0f);
             gl.glUniform1i(gl.glGetUniformLocation(copyIrradianceProg.getProgram(), "deltaESampler"), deltaEUnit);
             drawQuad();

             // adds deltaS into inscatter texture S (line 11 in algorithm 4.1)
//             glFramebufferTextureEXT(GL_FRAMEBUFFER_EXT, GL_COLOR_ATTACHMENT0_EXT, inscatterTexture, 0);
             fbo.setRenderTexture(inscatterTexture, null);
             gl.glViewport(0, 0, RES_MU_S * RES_NU, RES_MU);
             gl.glUseProgram(copyInscatterNProg.getProgram());
             gl.glUniform1i(gl.glGetUniformLocation(copyInscatterNProg.getProgram(), "deltaSSampler"), deltaSRUnit);
             for (int layer = 0; layer < RES_R; ++layer) {
                 setLayer(copyInscatterNProg, layer);
                 drawQuad();
             }

             gl.glDisable(GLenum.GL_BLEND);
         }

//         glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, 0);
        fbo.unbind();

         fbo.dispose();
         gl.glDeleteVertexArray(quad_vao);

         deltaSRTexture.dispose();
         deltaSMTexture.dispose();
         deltaJTexture.dispose();
         deltaETexture.dispose();

         gl.glUseProgram(0);

         transmittanceProg.dispose();
         irradiance1Prog.dispose();
         inscatter1Prog.dispose();
         copyIrradianceProg.dispose();
         copyInscatter1Prog.dispose();
         jProg.dispose();
         irradianceNProg.dispose();
         copyInscatterNProg.dispose();

         for(int i = 0; i <= 7; i++){
             gl.glBindSampler(i, 0);
         }
     }

    private void drawQuad()
    {
        gl.glBindVertexArray(quad_vao);
        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
        gl.glBindVertexArray(0);
    }

    private void setLayer(GLSLProgram prog, int layer)
    {
        double r = layer / (RES_R - 1.0);
        r = r * r;
        r = Math.sqrt(Rg * Rg + r * (Rt * Rt - Rg * Rg)) + (layer == 0 ? 0.01 : (layer == RES_R - 1 ? -0.001 : 0.0));
        double dmin = Rt - r;
        double dmax = Math.sqrt(r * r - Rg * Rg) + Math.sqrt(Rt * Rt - Rg * Rg);
        double dminp = r - Rg;
        double dmaxp = Math.sqrt(r * r - Rg * Rg);

        gl.glUniform1f(gl.glGetUniformLocation(prog.getProgram(), "r"), (float)(r));
        gl.glUniform4f(gl.glGetUniformLocation(prog.getProgram(), "dhdH"), (float)(dmin), (float)(dmax), (float)(dminp), (float)(dmaxp));
        gl.glUniform1i(gl.glGetUniformLocation(prog.getProgram(), "layer"), layer);
    }

    private Texture2D NewTexture2d(int width, int height){
        //       int format = rgb_format_supported_ ? (half_precision_?GLenum.GL_RGB16F:GLenum.GL_RGB32F):(half_precision_? GL_RGBA16F:GLenum.GL_RGBA32F);
        int format = GLenum.GL_RGB32F;
        Texture2DDesc desc2D = new Texture2DDesc(width, height, format);
        return TextureUtils.createTexture2D(desc2D, null);
    }

    private Texture3D NewTexture3d(int width, int height, int depth){
        int format = GLenum.GL_RGBA16F;

        Texture3DDesc desc3D = new Texture3DDesc(width, height, depth, 1, format);
        return TextureUtils.createTexture3D(desc3D, null);
    }

    void bindRenderingResources(int linearSampler){
//         gl.glBindTextureUnit();
    }
}

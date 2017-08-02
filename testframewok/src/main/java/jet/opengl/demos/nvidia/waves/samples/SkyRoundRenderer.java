package jet.opengl.demos.nvidia.waves.samples;

import org.lwjgl.util.vector.Matrix4f;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.Texture2D;

/**
 * Created by mazhen'gui on 2017/8/2.
 */

public class SkyRoundRenderer {
    private SkyRoundGenerator m_SkyVB;
    private Texture2D m_SkyTexture;
    private IsSkyProgram skyProgram;
    private GLFuncProvider gl;

    public void initlize(SkyRoundGenerator generator, Texture2D skyTexture){
        m_SkyVB = generator;
        m_SkyTexture = skyTexture;

        skyProgram = new IsSkyProgram("nvidia/WaveWorks/shaders/");
        gl = GLFuncProviderFactory.getGLFuncProvider();
    }

    public void render(Matrix4f mvp, boolean wireframe) {
        skyProgram.enable();
        skyProgram.setModelViewProjectionMatrix(mvp);
        GLCheck.checkError();
        if(wireframe)
            skyProgram.setupColorPass();
        else
            skyProgram.setupSkyPass();
        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D, m_SkyTexture.getTexture());
        gl.glBindSampler(0, IsSamplers.g_SamplerLinearWrap);

        m_SkyVB.draw(0, 1);

        skyProgram.disable();
        gl.glBindTexture(GLenum.GL_TEXTURE_2D, 0);
        gl.glBindSampler(0, 0);

        GLCheck.checkError();
    }
}

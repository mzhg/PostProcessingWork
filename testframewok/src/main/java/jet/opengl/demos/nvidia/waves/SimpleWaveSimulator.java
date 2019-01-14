package jet.opengl.demos.nvidia.waves;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.texture.RenderTargets;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.StackFloat;

/**
 * This class implements the wave algorithons in the paper "http://www.matthiasmueller.info/talks/GDC2008.pdf"
 */
public class SimpleWaveSimulator implements Disposeable {

    private static final int
        STRATEGY_UPDATE_MAP4 = 1,// update the velocity height map
        STRATEGY_UPDATE_MAP8 = 2,//
        STRATEGY_ADD_DROP = 3,  //
        STRATEGY_UPDATE_MAP4_ADD_DROP = 4,
        STRATEGY_UPDATE_MAP8_ADD_DROP = 5,
        STRATEGY_COUNT = 6;


    public static final class Params{
        public float		damping = 0.99f;
        public float        timeScale = 1.f;
        public boolean      nicest = true;

        public void set(Params ohs){
            damping = ohs.damping;
            timeScale = ohs.timeScale;
            nicest = ohs.nicest;
        }
    }

    private UpdateVelocityHeightMapProgram[] m_UpdateVelocityHeightProgram = new UpdateVelocityHeightMapProgram[STRATEGY_COUNT];
    private GLSLProgram m_CalculateGradientProgram;

    /** We use the R-channal to store the velocity, G-channal storing the height field. */
    private Texture2D[] m_VelocityHeightMap = new Texture2D[2];
    private Texture2D   m_GradientMap;

    private GLFuncProvider gl;
    private RenderTargets m_FBO;
    private int m_DummyVAO;
    private int m_sampler;

    private int m_TextureWidth = 256;
    private int m_TextureHeight = 256;
    private boolean m_TextureSizeChanged = true;

    private int m_PingPop = 0;
    private final StackFloat m_Drops = new StackFloat(32);

    public SimpleWaveSimulator(){ }

    public SimpleWaveSimulator(int textureWidth, int textureHeight){
        setTextureSize(textureWidth, textureHeight);
    }

    public void setTextureSize(int textureWidth, int textureHeight){
        if(m_TextureWidth != textureWidth || m_TextureHeight != textureHeight){
            this.m_TextureWidth = textureWidth;
            this.m_TextureHeight = textureHeight;

            m_TextureSizeChanged = true;
        }
    }

    public int getTextureWidth() { return m_TextureWidth;}
    public int getTextureHeight() { return m_TextureHeight;}

    public void simulate(Params params, float dt){
        if(gl == null){
            gl = GLFuncProviderFactory.getGLFuncProvider();
            m_FBO = new RenderTargets();

            m_DummyVAO = gl.glGenVertexArray();
            m_sampler = SamplerUtils.getDefaultSampler();
        }

        // Create the textures.
        if(m_TextureSizeChanged){
            m_TextureSizeChanged = false;
            if(m_GradientMap != null){
                m_GradientMap.dispose();
                m_GradientMap = null;

                m_VelocityHeightMap[0].dispose();
                m_VelocityHeightMap[1].dispose();
            }

            Texture2DDesc desc = new Texture2DDesc(m_TextureWidth, m_TextureHeight, GLenum.GL_RG16F);
            m_VelocityHeightMap[0] = TextureUtils.createTexture2D(desc, null);
            m_VelocityHeightMap[1] = TextureUtils.createTexture2D(desc, null);
            m_GradientMap = TextureUtils.createTexture2D(desc, null);

            gl.glClearTexImage(m_VelocityHeightMap[0].getTexture(), 0, GLenum.GL_RG, GLenum.GL_FLOAT, null);
            gl.glClearTexImage(m_VelocityHeightMap[1].getTexture(), 0, GLenum.GL_RG, GLenum.GL_FLOAT, null);
            gl.glClearTexImage(m_GradientMap.getTexture(), 0, GLenum.GL_RG, GLenum.GL_FLOAT, null);
        }

        boolean isCullFace = gl.glIsEnabled(GLenum.GL_CULL_FACE);
        boolean isBlend = gl.glIsEnabled(GLenum.GL_BLEND);
        boolean isDepthTest = gl.glIsEnabled(GLenum.GL_DEPTH_TEST);
        boolean isStencilTest = gl.glIsEnabled(GLenum.GL_STENCIL_TEST);

        IntBuffer int4 = CacheBuffer.getCachedIntBuffer(4);
        gl.glGetIntegerv(GLenum.GL_VIEWPORT, int4);

        int viewportX = int4.get(0);
        int viewportY = int4.get(1);
        int viewportW = int4.get(2);
        int viewportH = int4.get(3);

        gl.glGetIntegerv(GLenum.GL_COLOR_WRITEMASK, int4);

        boolean redMask = int4.get(0) != 0;
        boolean greenMask = int4.get(1) != 0;
        boolean blueMask = int4.get(2) != 0;
        boolean alphaMask = int4.get(3) != 0;

        gl.glDisable(GLenum.GL_CULL_FACE);
        gl.glDisable(GLenum.GL_BLEND);
        gl.glDisable(GLenum.GL_DEPTH_TEST);
        gl.glDisable(GLenum.GL_STENCIL_TEST);
        gl.glColorMask(true, true, false, false);
        gl.glViewport(0, 0, m_TextureWidth, m_TextureHeight);
        gl.glBindVertexArray(m_DummyVAO);
        gl.glBindSampler(0, m_sampler);

        updateVelHeiAndAddDrop(params, dt);

        calcGradient();

        if(isCullFace) gl.glEnable(GLenum.GL_CULL_FACE);
        if(isBlend) gl.glEnable(GLenum.GL_BLEND);
        if(isDepthTest) gl.glEnable(GLenum.GL_DEPTH_TEST);
        if(isStencilTest) gl.glEnable(GLenum.GL_STENCIL_TEST);
        gl.glColorMask(redMask, greenMask, blueMask, alphaMask);
        gl.glViewport(viewportX, viewportY, viewportW, viewportH);
        gl.glBindVertexArray(0);
        gl.glBindSampler(0, 0);
    }

    private void updateVelHeiAndAddDrop(Params params, float dt){
        int dropCount = m_Drops.size()/4;
        int progIndex;
        if(params.nicest) {
            if (dropCount > 0) {
                progIndex = STRATEGY_UPDATE_MAP8_ADD_DROP;
            } else {
                progIndex = STRATEGY_UPDATE_MAP8;
            }
        }else {
            if (dropCount > 0) {
                progIndex = STRATEGY_UPDATE_MAP4_ADD_DROP;
            } else {
                progIndex = STRATEGY_UPDATE_MAP4;
            }
        }

        UpdateVelocityHeightMapProgram program = getVelHeiProg(progIndex);

        int passCount = Math.min(dropCount, 4);
        program.enable();
        program.setDamping(params.damping);
        program.setDropCount(passCount);
        program.setTextureSize(m_TextureWidth, m_TextureHeight);
        program.setDropInfos(passCount * 4, 0, m_Drops.getData());

        final float timeStep = 16f/1000f;
        float timeScale = dt/timeStep * params.timeScale;
//        System.out.println("timeScale = " + timeScale);
        program.setTimeScale(timeScale);

        int source = m_PingPop;
        int destion = 1 - m_PingPop;

        m_FBO.bind();
        m_FBO.setRenderTexture(m_VelocityHeightMap[destion], null);
        gl.glClearBufferfv(GLenum.GL_COLOR, 0, CacheBuffer.wrap(0.f, 0.f, 0.f,0.f));
        gl.glBindTextureUnit(0, m_VelocityHeightMap[source].getTexture());

        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
        m_PingPop = 1 - m_PingPop;  // swap the buffers

        // Only add the drop to the height maps.
        dropCount -= passCount;
        int offset = passCount* 4;

        while (dropCount > 0){
            passCount = Math.min(dropCount, 4);
            program = getVelHeiProg(STRATEGY_ADD_DROP);
            program.enable();
            program.setDamping(params.damping);
            program.setDropCount(passCount);
            program.setTextureSize(m_TextureWidth, m_TextureHeight);
            program.setDropInfos(passCount * 4, offset, m_Drops.getData());

            source = m_PingPop;
            destion = 1 - m_PingPop;

            m_FBO.setRenderTexture(m_VelocityHeightMap[destion], null);
            gl.glClearBufferfv(GLenum.GL_COLOR, 0, CacheBuffer.wrap(0.f, 0.f, 0.f,0.f));
            gl.glBindTextureUnit(0, m_VelocityHeightMap[source].getTexture());

            gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
            m_PingPop = 1 - m_PingPop;  // swap the buffers
            offset += passCount* 4;
            dropCount -= passCount;
        }

        m_Drops.clear();

        gl.glBindTextureUnit(0, 0);
    }

    private void calcGradient(){
        if(m_CalculateGradientProgram == null){
            String root = "shader_libs\\WaveWork\\";
            m_CalculateGradientProgram = GLSLProgram.createProgram("shader_libs\\PostProcessingDefaultScreenSpaceVS.vert", root + "CalculateGradientPS.frag", null);
        }

        m_FBO.bind();
        m_FBO.setRenderTexture(m_GradientMap, null);
        m_CalculateGradientProgram.enable();
        gl.glBindTextureUnit(0, m_VelocityHeightMap[m_PingPop].getTexture());
        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
        gl.glBindTextureUnit(0, 0);
    }

    private UpdateVelocityHeightMapProgram getVelHeiProg(int index){
        if(m_UpdateVelocityHeightProgram[index] == null){
            m_UpdateVelocityHeightProgram[index] = new UpdateVelocityHeightMapProgram(index);
        }

        return m_UpdateVelocityHeightProgram[index];
    }

    /**
     * Add a water drop.
     * @param x The drop x position in the normalized texture coordinate.
     * @param y The drop y position in the normalized texture coordinate.
     * @param radius The drop radius in the normalized texture coordinate.
     */
    public void addDrop(float x, float y, float radius, float strength){
        m_Drops.push(x);
        m_Drops.push(y);
        m_Drops.push(radius);
        m_Drops.push(strength);
    }

    public Texture2D getHeightMap() { return m_VelocityHeightMap[m_PingPop];}
    public Texture2D getGradientMap() { return m_GradientMap;}

    @Override
    public void dispose() {
        if(m_GradientMap != null){
            m_GradientMap.dispose();
            m_GradientMap = null;

            m_VelocityHeightMap[0].dispose();
            m_VelocityHeightMap[1].dispose();

            m_VelocityHeightMap[0] = null;
            m_VelocityHeightMap[1] = null;
        }
    }

    private final class UpdateVelocityHeightMapProgram extends GLSLProgram {
        private int m_TextureSize;
        private int m_DropInfos;
        private int m_DropCount;
        private int m_Damping;
        private int m_TimeScale;

        UpdateVelocityHeightMapProgram(int strategy){
            String root = "shader_libs\\WaveWork\\";
            try {
                setSourceFromFiles("shader_libs\\PostProcessingDefaultScreenSpaceVS.vert",
                        root + "UpdateVelocityHeightMapPS.frag", new Macro("STRATEGY_PROFILE", strategy));
            } catch (IOException e) {
                e.printStackTrace();
            }

            m_TextureSize = getUniformLocation("g_TextureSize");
            m_DropInfos = getUniformLocation("g_DropInfos");
            m_DropCount = getUniformLocation("g_DropCount");
            m_TimeScale = getUniformLocation("g_TimeScale");
            m_Damping = getUniformLocation("g_damping");
        }

        void setTextureSize(float width, float height){
            if(m_TextureSize >= 0)
                gl.glUniform4f(m_TextureSize, width, height, 1.f/width, 1.f/height);
        }

        void setDropInfos(int length, int offset, float[] data){
            if(m_DropInfos >=0) {
                FloatBuffer buffer = CacheBuffer.getCachedFloatBuffer(16);
                buffer.put(data, offset, length);
                for(int i = length; i < 16; i++){
                    buffer.put(0);
                }

                buffer.flip();
                gl.glUniform4fv(m_DropInfos, buffer);
            }
        }

        void setDropCount(int count){
            if(m_DropCount >= 0)
                gl.glUniform1i(m_DropCount, count);
        }

        void setTimeScale(float scale){
            if(m_TimeScale >= 0)
                gl.glUniform1f(m_TimeScale, scale);
        }

        void setDamping(float damping){
            if(m_Damping >= 0)
                gl.glUniform1f(m_Damping, damping);
        }
    }
}

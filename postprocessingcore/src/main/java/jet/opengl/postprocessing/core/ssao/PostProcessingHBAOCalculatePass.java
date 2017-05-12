package jet.opengl.postprocessing.core.ssao;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector4f;

import java.io.IOException;
import java.nio.ByteBuffer;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.PostProcessingFrameAttribs;
import jet.opengl.postprocessing.core.PostProcessingParameters;
import jet.opengl.postprocessing.core.PostProcessingRenderContext;
import jet.opengl.postprocessing.core.PostProcessingRenderPass;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.LogUtil;
import jet.opengl.postprocessing.util.Numeric;

import static jet.opengl.postprocessing.core.ssao.PostProcessingDeinterleavePass.HBAO_RANDOM_ELEMENTS;

/**
 * Created by mazhen'gui on 2017-05-11 16:36:43.
 */

final class PostProcessingHBAOCalculatePass extends PostProcessingRenderPass {

    static final int MAX_SAMPLES = 8;
    static final Vector4f[] hbaoRandom = new Vector4f[PostProcessingDeinterleavePass.HBAO_RANDOM_ELEMENTS * MAX_SAMPLES];

    static {
        final float numDir = 8; // keep in sync to glsl
        for(int i=0; i<HBAO_RANDOM_ELEMENTS*MAX_SAMPLES; i++)
        {
            float Rand1 = Numeric.random(0, 1);
            float Rand2 = Numeric.random(0, 1);

            // Use random rotation angles in [0,2PI/NUM_DIRECTIONS)
            float Angle = 2.f * Numeric.PI * Rand1 / numDir;
            hbaoRandom[i] = new Vector4f();
            hbaoRandom[i].x = (float) Math.cos(Angle);
            hbaoRandom[i].y = (float) Math.sin(Angle);
            hbaoRandom[i].z = Rand2;
            hbaoRandom[i].w = 0;
        }
    }

    private static PostProcessingHBAOProgram g_HBAOProgram = null;
    private final HBAOData hbaoUbo = new HBAOData();
    private int m_hbao_buffer;

    public PostProcessingHBAOCalculatePass() {
        super("HBAOCalculateBlur");

        // input0: linear depth tex array
        // input1: view normal tex
        set(2,1);
    }

    @Override
    public void process(PostProcessingRenderContext context, PostProcessingParameters parameters) {
        if(g_HBAOProgram == null){
            try {
                g_HBAOProgram = new PostProcessingHBAOProgram(1,1);
                addDisposedResource(g_HBAOProgram);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Texture2D input0 = getInput(0);
        Texture2D input1 = getInput(1);
        Texture2D output = getOutputTexture(0);
        if(input0 == null){
            LogUtil.e(LogUtil.LogType.DEFAULT, "ReinterleavePass:: Missing depth texture!");
            return;
        }

        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();

        if(!gl.glIsTexture(output.getTexture())){
            throw new NullPointerException();
        }

        {
            // prepare uniform data
            PostProcessingFrameAttribs frameAttribs = context.getFrameAttribs();
            prepareHbaoData(frameAttribs.projMat, frameAttribs.getViewProjMatrix(), frameAttribs.fov, frameAttribs.viewport.width, frameAttribs.viewport.height);
            if(m_hbao_buffer == 0){
                m_hbao_buffer = gl.glGenBuffer();
                gl.glBindBuffer(GLenum.GL_UNIFORM_BUFFER, m_hbao_buffer);
                gl.glBufferData(GLenum.GL_UNIFORM_BUFFER, HBAOData.SIZE, GLenum.GL_DYNAMIC_DRAW);
            }

            gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 0, m_hbao_buffer);
            ByteBuffer buf = CacheBuffer.getCachedByteBuffer(HBAOData.SIZE);
            hbaoUbo.store(buf);
            buf.flip();
            gl.glBufferSubData(GLenum.GL_UNIFORM_BUFFER, 0, buf);
        }

        context.setViewport(0,0, output.getWidth(), output.getHeight());
        context.setVAO(null);
        context.setProgram(g_HBAOProgram);
//        g_HBAOProgram.setUniform();  TODO uniform data...

        context.bindTexture(input0, 0, 0);
        context.bindTexture(input1, 1, 0);
        context.setBlendState(null);
        context.setDepthStencilState(null);
        context.setRasterizerState(null);

        if(GLCheck.CHECK)
            GLCheck.checkError("HBAOCalculatePass");
        context.setRenderTarget(output);

        context.drawArrays(GLenum.GL_TRIANGLES, 0, 3*PostProcessingDeinterleavePass.HBAO_RANDOM_ELEMENTS);

        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 0, 0);

        if(GLCheck.CHECK)
            GLCheck.checkError("HBAOCalculatePass");
    }

    private void prepareHbaoData(Matrix4f projMat, Matrix4f viewProjMatrix, float fov, int width, int height){
        float           intensity = 1.5f;
        float           bias = 0.1f;
        float           radius = 2.f;

//        Matrix4f projMat = projection.matrix;

        boolean ortho = false;
        hbaoUbo.projOrtho = 0; //projection.ortho ? 1 : 0;
        if(!ortho){
            hbaoUbo.projInfo.set(
                    2.0f/projMat.m00,     // (x) * (R - L)/N
                    2.0f/projMat.m11,     // (y) * (T - B)/N
                    -( 1.0f - projMat.m20) / projMat.m00, // L/N
                    -( 1.0f + projMat.m21) / projMat.m11  // B/N
            );
        }else{
            hbaoUbo.projInfo.set(
                    2.0f/projMat.m00,     // (x) * (R - L)
                    2.0f/projMat.m11,     // (y) * (T - B)
                    -( 1.0f + projMat.m30) / projMat.m00, // L
                    -( 1.0f - projMat.m31) / projMat.m11  // B
            );
        }

        float projScale;
        if (ortho){
            projScale = height / hbaoUbo.projInfo.y /*(projInfoOrtho[1])*/;
        }
        else {
            projScale = (float) (height / (Math.tan( fov * 0.5f) * 2.0f));
        }

        // radius
        float meters2viewspace = 1.0f;
        float R = radius * meters2viewspace;
        hbaoUbo.R2 = R * R;
        hbaoUbo.NegInvR2 = -1.0f / hbaoUbo.R2;
        hbaoUbo.RadiusToScreen = R * 0.5f * projScale;

        // ao
        hbaoUbo.PowExponent = Math.max(intensity,0.0f);
        hbaoUbo.NDotVBias = Math.min(Math.max(0.0f, bias),1.0f);
        hbaoUbo.AOMultiplier = 1.0f / (1.0f - hbaoUbo.NDotVBias);

        // resolution
        int quarterWidth  = ((width+3)/4);
        int quarterHeight = ((height+3)/4);

        hbaoUbo.InvQuarterResolution.set(1.0f/(quarterWidth),1.0f/(quarterHeight));
        hbaoUbo.InvFullResolution.set(1.0f/(width),1.0f/(height));

        for (int i = 0; i < PostProcessingDeinterleavePass.HBAO_RANDOM_ELEMENTS; i++){
            hbaoUbo.float2Offsets[i].set((i % 4) + 0.5f, (i / 4) + 0.5f);
            hbaoUbo.jitters[i].set(hbaoRandom[i]);
        }

        hbaoUbo.projMat.load(projMat);
        Matrix4f.invert(viewProjMatrix, hbaoUbo.viewProjInvMat);
    }

    @Override
    public void computeOutDesc(int index, Texture2DDesc out) {
        Texture2D input = getInput(0);
        if(input != null){
            input.getDesc(out);
            out.format = GLenum.GL_RG16F;
        }

        super.computeOutDesc(index, out);
    }
}

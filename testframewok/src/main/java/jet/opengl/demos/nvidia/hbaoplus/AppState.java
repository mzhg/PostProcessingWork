package jet.opengl.demos.nvidia.hbaoplus;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.CacheBuffer;

final class AppState {
	
	public static final int GL_BLEND_COLOR = 0x8005;

	private int m_DrawFBO;
    private int m_VAO;

    private int m_ContextUBO;
    private final int[] m_BindPointUBOs = new int[3];

    private int m_ActiveTexture;
    private int m_TextureBinding2D;
    private int m_TextureBinding2DArray;

    private int m_Program;

    private final int[] m_Viewport = new int[4];
    private float m_PolygonOffsetFactor;
    private float m_PolygonOffsetUnits;

    private boolean m_CullFace;
    private boolean m_ScissorTest;
    private boolean m_Multisample;
    private boolean m_DepthTest;
    private boolean m_StencilTest;
//    private boolean m_ColorLogicOp;
    private boolean m_SampleCoverage;
    private boolean m_SampleAlphaToCoverage;

    private boolean m_Blend;
    private int m_BlendSrcRGB;
    private int m_BlendDstRGB;
    private int m_BlendSrcAlpha;
    private int m_BlendDstAlpha;
    private int m_BlendEquationRGB;
    private int m_BlendEquationAlpha;
    private final float[] m_BlendColor = new float[4];
    private final boolean[] m_ColorWriteMask = new boolean[4];
    private GLFuncProvider gl;

    void initlizeGL(){
        gl = GLFuncProviderFactory.getGLFuncProvider();
    }

    void enableState(boolean isEnabled, int cap)
    {
        if (isEnabled)
        {
            gl.glEnable(cap);
        }
        else
        {
            gl.glDisable(cap);
        }
    }
    
    void save(){
        GLCheck.checkError();

    	m_DrawFBO 		= gl.glGetInteger(GLenum.GL_DRAW_FRAMEBUFFER_BINDING);
    	m_VAO     		= gl.glGetInteger(GLenum.GL_VERTEX_ARRAY_BINDING);
    	m_ContextUBO    = gl.glGetInteger(GLenum.GL_UNIFORM_BUFFER_BINDING);
    	
    	for(int bindingPoint = 0; bindingPoint < m_BindPointUBOs.length; bindingPoint ++){
    		m_BindPointUBOs[bindingPoint] = gl.glGetIntegeri(GLenum.GL_UNIFORM_BUFFER_BINDING, bindingPoint);
    	}
    	
    	m_ActiveTexture = gl.glGetInteger(GLenum.GL_ACTIVE_TEXTURE);
    	m_TextureBinding2D = gl.glGetInteger(GLenum.GL_TEXTURE_BINDING_2D);
    	m_TextureBinding2DArray = gl.glGetInteger(GLenum.GL_TEXTURE_BINDING_2D_ARRAY);
    	m_Program 		= gl.glGetInteger(GLenum.GL_CURRENT_PROGRAM);
        IntBuffer viewport = CacheBuffer.getCachedIntBuffer(4);
        gl.glGetIntegerv(GLenum.GL_VIEWPORT, viewport);
        viewport.get(m_Viewport);

    	m_PolygonOffsetFactor = gl.glGetFloat(GLenum.GL_POLYGON_OFFSET_FACTOR);
    	m_PolygonOffsetUnits = gl.glGetFloat(GLenum.GL_POLYGON_OFFSET_UNITS);
    	
    	m_CullFace = gl.glIsEnabled(GLenum.GL_CULL_FACE);
    	m_ScissorTest = gl.glIsEnabled(GLenum.GL_SCISSOR_TEST);
    	m_Multisample = gl.glIsEnabled(GLenum.GL_MULTISAMPLE);
    	m_DepthTest = gl.glIsEnabled(GLenum.GL_DEPTH_TEST);
    	m_StencilTest = gl.glIsEnabled(GLenum.GL_STENCIL_TEST);
    	
//#if !USE_GLES
//        m_ColorLogicOp = GL11.glIsEnabled(GL11.GL_COLOR_LOGIC_OP);
//#endif
    	
    	m_SampleCoverage = gl.glIsEnabled(GLenum.GL_SAMPLE_COVERAGE);
        m_SampleAlphaToCoverage = gl.glIsEnabled(GLenum.GL_SAMPLE_ALPHA_TO_COVERAGE);
        m_Blend = gl.glIsEnabledi(GLenum.GL_BLEND, 0);
        
        m_BlendSrcRGB = gl.glGetInteger(GLenum.GL_BLEND_SRC_RGB);
        m_BlendSrcAlpha = gl.glGetInteger(GLenum.GL_BLEND_SRC_ALPHA);
        m_BlendDstRGB = gl.glGetInteger(GLenum.GL_BLEND_DST_RGB);
        m_BlendDstAlpha = gl.glGetInteger(GLenum.GL_BLEND_DST_ALPHA);
        m_BlendEquationRGB = gl.glGetInteger(GLenum.GL_BLEND_EQUATION_RGB);
        m_BlendEquationAlpha = gl.glGetInteger(GLenum.GL_BLEND_EQUATION_ALPHA);
        FloatBuffer blendColor = CacheBuffer.getCachedFloatBuffer(4);
        gl.glGetFloatv(GL_BLEND_COLOR, blendColor);
        blendColor.get(m_BlendColor);

        ByteBuffer bufs = CacheBuffer.getCachedByteBuffer(4);
        gl.glGetBooleanv(GLenum.GL_COLOR_WRITEMASK, bufs);

        m_ColorWriteMask[0] = bufs.get() != 0;
        m_ColorWriteMask[1] = bufs.get() != 0;
        m_ColorWriteMask[2] = bufs.get() != 0;
        m_ColorWriteMask[3] = bufs.get() != 0;
        
//        m_BlendSrcRGB = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
//        m_BlendSrcRGB = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        
        GLCheck.checkError();
    }
    
    void restore(){
        gl.glBindFramebuffer(GLenum.GL_DRAW_FRAMEBUFFER, m_DrawFBO);
        gl.glBindVertexArray(m_VAO);
        gl.glBindBuffer(GLenum.GL_UNIFORM_BUFFER, m_ContextUBO);
        for (int BindingPoint = 0; BindingPoint < m_BindPointUBOs.length; ++BindingPoint)
        {
            gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, BindingPoint, m_BindPointUBOs[BindingPoint]);
        }

        gl.glActiveTexture(m_ActiveTexture);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D, m_TextureBinding2D);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D_ARRAY, m_TextureBinding2DArray);
        gl.glUseProgram(m_Program);
        gl.glViewport(m_Viewport[0], m_Viewport[1], m_Viewport[2], m_Viewport[3]);
        gl.glPolygonOffset(m_PolygonOffsetFactor, m_PolygonOffsetUnits);

        enableState(m_CullFace, GLenum.GL_CULL_FACE);
        enableState(m_ScissorTest, GLenum.GL_SCISSOR_TEST);
        enableState(m_Multisample, GLenum.GL_MULTISAMPLE);
        enableState(m_DepthTest, GLenum.GL_DEPTH_TEST);
        enableState(m_StencilTest, GLenum.GL_STENCIL_TEST);
        
//    #if !USE_GLES
//        EnableState(GL, m_ColorLogicOp, GL_COLOR_LOGIC_OP);
//    #endif
        
        enableState(m_SampleCoverage, GLenum.GL_SAMPLE_COVERAGE);
        enableState(m_SampleAlphaToCoverage, GLenum.GL_SAMPLE_ALPHA_TO_COVERAGE);
        enableState(m_Blend, GLenum.GL_BLEND);

        gl.glBlendFuncSeparate(m_BlendSrcRGB, m_BlendDstRGB, m_BlendSrcAlpha, m_BlendDstAlpha);
        gl.glBlendEquationSeparate(m_BlendEquationRGB, m_BlendEquationAlpha);
        gl.glBlendColor(m_BlendColor[0], m_BlendColor[1], m_BlendColor[2], m_BlendColor[3]);
        gl.glColorMask(m_ColorWriteMask[0], m_ColorWriteMask[1], m_ColorWriteMask[2], m_ColorWriteMask[3]);

        GLCheck.checkError();
    }
}

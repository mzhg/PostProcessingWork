package jet.opengl.demos.nvidia.hbaoplus;

import java.io.IOException;

import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.ShaderLoader;
import jet.opengl.postprocessing.shader.ShaderSourceItem;
import jet.opengl.postprocessing.shader.ShaderType;
import jet.opengl.postprocessing.util.CacheBuffer;

abstract class BaseProgram extends GLSLProgram {
	
	static CharSequence getFullscreenTriangle_VS_GLSL(){
		try {
			return ShaderLoader.loadShaderFile("shader_libs/Quad_VS.vert", false);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}

	void create(CharSequence pVertexShaderGLSL, CharSequence pFragmentShaderGLSL){
		addLinkTask((programID)->
		{
			for(int colorIndex = 0; colorIndex < 8; colorIndex ++){
				String variableName = "PixOutput" + colorIndex;
				gl.glBindFragDataLocation(programID, colorIndex, variableName);
			}
		});

		setSourceFromStrings(pVertexShaderGLSL, pFragmentShaderGLSL);
	}
	
	void create(CharSequence pVertexShaderGLSL, CharSequence pGeometryShaderGLSL, CharSequence pFragmentShaderGLSL){
		setSourceFromStrings(new ShaderSourceItem(pVertexShaderGLSL, ShaderType.VERTEX),
							 new ShaderSourceItem(pGeometryShaderGLSL, ShaderType.GEOMETRY),
							 new ShaderSourceItem(pFragmentShaderGLSL, ShaderType.FRAGMENT));
	}
	
	int getUniformBlockIndex(/*const GFSDK_SSAO_GLFunctions& GL,*/ String uniformBlock)
    {
        int Result = gl.glGetUniformBlockIndex(getProgram(), uniformBlock);
        return Result;
    }

	void setTexture(int Target, int UniformLocation, int TexId, int TexUnit){
		setTexture(Target, UniformLocation, TexId, TexUnit, GLenum.GL_NEAREST, GLenum.GL_CLAMP_TO_EDGE);
	}
	
	private static final float[] borderColor = {-Float.MAX_VALUE,-Float.MAX_VALUE,-Float.MAX_VALUE,-Float.MAX_VALUE,};

    void setTexture(/*const GFSDK_SSAO_GLFunctions& GL,*/ int Target, int UniformLocation, int TexId, int TexUnit, int Filter /*= GL_NEAREST*/, int WrapMode /*= GL_CLAMP_TO_EDGE*/)
    {
//        ASSERT(UniformLocation != -1);

//        GL20.glUseProgram(programId);
        gl.glUniform1i(UniformLocation, TexUnit);

		gl.glActiveTexture(GLenum.GL_TEXTURE0 + TexUnit);
		gl.glBindTexture(Target, TexId);

        if (Target != GLenum.GL_TEXTURE_2D_MULTISAMPLE)
        {
            gl.glTexParameteri(Target, GLenum.GL_TEXTURE_MIN_FILTER, Filter);
			gl.glTexParameteri(Target, GLenum.GL_TEXTURE_MAG_FILTER, Filter);
			gl.glTexParameteri(Target, GLenum.GL_TEXTURE_WRAP_S, WrapMode);
			gl.glTexParameteri(Target, GLenum.GL_TEXTURE_WRAP_T, WrapMode);
        }

        if (WrapMode == GLenum.GL_CLAMP_TO_BORDER)
        {
//            const GLfloat BorderColor[4] = { -FLT_MAX };
            gl.glTexParameterfv(Target, GLenum.GL_TEXTURE_BORDER_COLOR, CacheBuffer.wrap(borderColor));
        }
    }
}

package com.nvidia.developer.opengl.tests;

import com.nvidia.developer.opengl.app.NvSampleApp;
import com.nvidia.developer.opengl.ui.NvTweakEnumi;
import com.nvidia.developer.opengl.utils.FieldControl;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.util.Arrays;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLUtil;
import jet.opengl.postprocessing.shader.ShaderType;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * This sample demonstrates the use of approximating Catmull-clark subdivision 
 * surfaces with gregory patches.
 * @port_author mazhen'gui 2015-02-03
 * @email mzhg001@sina.com
 *
 */
public class TessellationPatterns extends NvSampleApp{

	static final int EQUAL = 0;
	static final int FRACTIONAL_ODD = 1;
	static final int FRACTIONAL_EVENT = 2;
	
	static final int TRIANGLE = 1;
	static final int QUAD = 0;
	
	int g_tessellationMode = EQUAL;
	
	final int[] g_outterTess = new int[4];
	final int[] g_innerTess = new int[2];
	final int[][] mPrograms = new int[2][3];
	final int[][] mUniformLoc = new int[2][3];
	final Matrix4f f4x4WorldViewProjection = new Matrix4f(); // World * View * Projection matrix
	final Matrix4f mProjection = new Matrix4f();
	final int[] mVB = new int[2];
	final int[] mVAO = new int[2];
	
	private GLFuncProvider gl;
    
    @Override
    protected void initRendering() {
    	gl = GLFuncProviderFactory.getGLFuncProvider();
    	
//    	setAppTitle("OpenGL Tessellation Pattern");
    	Arrays.fill(g_innerTess, 4);
    	Arrays.fill(g_outterTess, 6);
    	
    	CharSequence vertSource = null;
    	CharSequence fragSource = null;
    	String tessSource = null;
    	
//    	try {
//    		vertSource = FileUtils.loadText(new File("assets/TessellationPatterns/shaders/tessellation_patterns.vert"));
//    		tessSource = FileUtils.loadText(new File("assets/TessellationPatterns/shaders/tessellation_patterns.glte")).toString();
//    		fragSource = FileUtils.loadText(new File("assets/TessellationPatterns/shaders/tessellation_patterns.frag"));
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
    	
    	// build program.
    	int vs = GLSLUtil.compileShaderFromSource(vertSource, ShaderType.VERTEX, true);
    	int fs = GLSLUtil.compileShaderFromSource(fragSource, ShaderType.FRAGMENT, true);
    	final String tag = "#pattern";
    	final String[] types = {"quads", "triangles"};
    	final String[] func = {"Quad", "Triangle"};
    	final String[] modes = {"equal_spacing", "fractional_even_spacing", "fractional_odd_spacing"};
    	final String pattern = "layout (%s, %s, ccw) in;\n";
    	
    	for(int i = 0; i < 2; i++){
    		for(int j = 0; j < 3; j++){
    			String pat = String.format(pattern, types[i], modes[j]);
    			String testring = tessSource.replace(tag, pat);
    			int te = GLSLUtil.compileShaderFromSource(testring, ShaderType.TESS_EVAL, true);
    			
    			int program = GLSLUtil.createProgramFromShaders(vs, 0, te, 0, fs, null);
    			gl.glDeleteShader(te);
    			
    			gl.glUseProgram(program);
//    			int funIndex = gl.glGetSubroutineIndex(program, GLenum.GL_TESS_EVALUATION_SHADER, func[i]);
//    			gl.glUniformSubroutinesui(GLenum.GL_TESS_EVALUATION_SHADER, funIndex);
    			gl.glUseProgram(0);
    			
    			mPrograms[i][j] = program;
    			mUniformLoc[i][j] = gl.glGetUniformLocation(program, "g_f4x4WorldViewProjection");
    		}
    	}
    	
    	gl.glDeleteShader(vs);
    	gl.glDeleteShader(fs);
    	
    	float width = 20.0f;
    	float offset = 24.0f;
    	Vector3f[] data = new Vector3f[4];
    	data[0] = new Vector3f(-width+offset,-width,0.0f); 
    	data[1] = new Vector3f(width+offset,-width,0.0f); 
    	data[2] = new Vector3f(width+offset,width,0.0f); 
    	data[3] = new Vector3f(-width+offset,width,0.0f);
    	
    	createBuffer(data, QUAD);
    	
    	offset = 24;
    	data = new Vector3f[3];
    	data[0] = new Vector3f(-width-offset,-width,0.0f); 
    	data[1] = new Vector3f(width-offset,-width,0.0f); 
    	data[2] = new Vector3f(-offset,width/1.5f,0.0f);
    	
    	createBuffer(data, TRIANGLE);
    	
    	m_transformer.setTranslationVec(new Vector3f(0.0f, 0.0f, -100.0f));
    }
    
    @Override
    public void initUI() {
    	if (mTweakBar != null) {
    		NvTweakEnumi[] enums = {
				new NvTweakEnumi("Equal", EQUAL),	
				new NvTweakEnumi("Even", FRACTIONAL_EVENT),	
    			new NvTweakEnumi("Odd", FRACTIONAL_ODD),	
    		};
    		
    		mTweakBar.addMenu("Fractional", createControl("g_tessellationMode"), enums, 0);
    		
    		String[] names = {"Left", "Bottom", "Right", "Top"};
    		for(int i = 0; i < 4; i++){
    			FieldControl control = new FieldControl(this, i, "g_outterTess", FieldControl.CALL_FIELD);
    			mTweakBar.addValue("Outter " + names[i], control, 1, 30, 1, 0);
    		}
    		
    		String[] inames = {"Vertical", "Horizental"};
    		for(int i = 0; i < 2; i++){
    			FieldControl control = new FieldControl(this, i, "g_innerTess", FieldControl.CALL_FIELD);
    			mTweakBar.addValue("Inner " + inames[i], control, 1, 30, 1, 0);
    		}
    	}
    }
    
    @Override
    protected void reshape(int width, int height) {
    	gl.glViewport(0, 0, width, height);
    	Matrix4f.perspective(45, (float)width/height, 0.1f, 5000, mProjection);
    }
    
    @Override
    public void display() {
    	gl.glEnable(GLenum.GL_DEPTH_TEST);
    	gl.glPolygonMode(GLenum.GL_FRONT_AND_BACK, GLenum.GL_LINE);
    	
    	gl.glClear(GLenum.GL_DEPTH_BUFFER_BIT | GLenum.GL_COLOR_BUFFER_BIT);
    	
    	// render the quad
    	gl.glBindVertexArray(mVAO[QUAD]);
    	int program = mPrograms[QUAD][g_tessellationMode];
    	gl.glUseProgram(program);
    	
    	// TODO For Nvidia driver, we have to specify the subroutine index every time. 
//    	int funIndex = gl.glGetSubroutineIndex(program, GLenum.GL_TESS_EVALUATION_SHADER, "Quad");
//    	gl.glUniformSubroutinesui(GLenum.GL_TESS_EVALUATION_SHADER, funIndex);
//    	VectorUtil.lookAt(vec3(0,0,100), vec3(0,0,0), vec3(0,1,0), f4x4WorldViewProjection);
    	m_transformer.getModelViewMat(f4x4WorldViewProjection);
    	// MVP = P * MV
    	Matrix4f.mul(mProjection, f4x4WorldViewProjection, f4x4WorldViewProjection);
    	gl.glUniformMatrix4fv(mUniformLoc[QUAD][g_tessellationMode], false, CacheBuffer.wrap(f4x4WorldViewProjection));
    	
//    	gl.glPatchParameterfv(GLenum.GL_PATCH_DEFAULT_OUTER_LEVEL, CacheBuffer.wrap((float)g_outterTess[0], g_outterTess[1], g_outterTess[2], g_outterTess[3]));
//    	gl.glPatchParameterfv(GLenum.GL_PATCH_DEFAULT_INNER_LEVEL, CacheBuffer.wrap((float)g_innerTess[0], g_innerTess[1], 0, 0));
    	gl.glPatchParameteri(GLenum.GL_PATCH_VERTICES, 4);
    	gl.glDrawArrays(GLenum.GL_PATCHES, 0, 4);
//    	GL30.glBindVertexArray(0);
//    	GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    	
    	// render the triangle
    	gl.glBindVertexArray(mVAO[TRIANGLE]);
    	program = mPrograms[TRIANGLE][g_tessellationMode];
    	gl.glUseProgram(program);
//    	funIndex = gl.glGetSubroutineIndex(program, GLenum.GL_TESS_EVALUATION_SHADER, "Triangle");
//    	gl.glUniformSubroutinesui(GLenum.GL_TESS_EVALUATION_SHADER, funIndex);
    	gl.glUniformMatrix4fv(mUniformLoc[TRIANGLE][g_tessellationMode], false, CacheBuffer.wrap(f4x4WorldViewProjection));
    	
//    	gl.glPatchParameterfv(GLenum.GL_PATCH_DEFAULT_OUTER_LEVEL, CacheBuffer.wrap((float)g_outterTess[0], g_outterTess[1], g_outterTess[2], g_outterTess[3]));
//    	gl.glPatchParameterfv(GLenum.GL_PATCH_DEFAULT_INNER_LEVEL, CacheBuffer.wrap((float)g_innerTess[0], g_innerTess[1], 0, 0));
    	gl.glPatchParameteri(GLenum.GL_PATCH_VERTICES, 3);
    	gl.glDrawArrays(GLenum.GL_PATCHES, 0, 3);
    	gl.glBindVertexArray(0);
    	gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
    	
    	gl.glPolygonMode(GLenum.GL_FRONT_AND_BACK, GLenum.GL_FILL);
    }
    
    public static void main(String[] args) {
//    	System.setProperty("java.library.path", "");
//    	System.setProperty("org.lwjgl.util.Debug", "true");
    	
//		new TessellationPatterns().start(1200,720);
	}
	
    void createBuffer(Vector3f[] data, int shape){
    	mVB[shape] = gl.glGenBuffer();
    	gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, mVB[shape]);
    	gl.glBufferData(GLenum.GL_ARRAY_BUFFER, CacheBuffer.wrap(data), GLenum.GL_STATIC_DRAW);
    	
    	mVAO[shape] = gl.glGenVertexArray();
    	gl.glBindVertexArray(mVAO[shape]);
    	{
    		gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, mVB[shape]);
    		
    		gl.glVertexAttribPointer(0, 3, GLenum.GL_FLOAT, false, 0, 0);
    		gl.glEnableVertexAttribArray(0);
    	}
    	gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
    	gl.glBindVertexArray(0);
    }
}

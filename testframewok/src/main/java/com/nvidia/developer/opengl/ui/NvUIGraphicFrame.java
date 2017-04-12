//----------------------------------------------------------------------------------
// File:        NvUIGraphicFrame.java
// SDK Version: v1.2 
// Email:       gameworks@nvidia.com
// Site:        http://developer.nvidia.com/
//
// Copyright (c) 2014, NVIDIA CORPORATION. All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
//  * Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
//  * Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
//  * Neither the name of NVIDIA CORPORATION nor the names of its
//    contributors may be used to endorse or promote products derived
//    from this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS ``AS IS'' AND ANY
// EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
// PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
// CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
// EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
// PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
// PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
// OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//
//----------------------------------------------------------------------------------
package com.nvidia.developer.opengl.ui;

import org.lwjgl.util.vector.Vector2f;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.Numeric;

/**
 * A graphical frame element, representing a background and border combined.<p>
 * This class implements a flexible/dynamic frame system based off the general
 * NvUIGraphic base class, letting the base class manage texture loading, etc.<p>

 * Then, this subclass adds the concept of drawing a properly-formed frame
 * texture which has designed in a non-stretched border area which is used in
 * calculating mapping from texel space (source) to pixel space (destination).
 * The frame object can then be resized with the frame border remaining a fixed
 * 'thickness', but the edges and center stretching and contracting to match the
 * given size/rect.
 * @author Nvidia 2014-9-8 : 21: 20
 *
 */
public class NvUIGraphicFrame extends NvUIGraphic{

	private static boolean s_staticCount;
	private static final NvGraphicFrameShader ms_shader = new NvGraphicFrameShader();
	
	private static int ms_gfvbo = 0;
	private static int ms_gfibo = 0;
	
	private static float s_gfpixelToClipMatrix[][] = new float[4][4];
	private static float s_gfpixelScaleFactorX = 2.0f / 800.0f;
	private static float s_gfpixelScaleFactorY = 2.0f / 480.0f;
	private static int   s_gfpixelXLast = 800;
	private static int   s_gfpixelYLast = 480;
	
	private static final String s_frameVertShader = 
			"#version 100\n"+
			"// this is set from higher level.  think of it as the upper model matrix\n"+
			"uniform mat4 pixelToClipMat;\n"+
			"uniform vec2 thickness;\n"+
			"uniform vec2 texBorder;\n"+
			"attribute vec2 border;\n"+
			"attribute vec2 position;\n"+
			"attribute vec2 tex;\n"+
			"varying vec2 tex_coord;\n"+
			"void main()\n"+
			"{\n"+
			"    vec2 invBorder = vec2(1,1) - border;\n"+
			"    vec2 shiftedPosition = (position-thickness*invBorder*position);\n"+
			"    // we need to convert from -1,1 coords into 0,1 coords before xform.\n"+
			"    shiftedPosition *= 0.5;\n"+
			"    shiftedPosition += 0.5;\n"+
			"    // then we multiply like uigraphic normally would\n"+
			"    gl_Position = pixelToClipMat * vec4(shiftedPosition, 0, 1);\n"+
			"    tex_coord = tex + invBorder * -position * texBorder;\n"+
			"}\n";
	
	// note this is same as uigraphic's frag shader, minus colorization removed.
	private static final String s_frameFragShader =
			"#version 100\n"+
			"precision mediump float;\n"+
			"varying vec2 tex_coord;\n"+
			"uniform sampler2D sampler;\n"+
			"uniform float alpha;\n"+
			"uniform vec4 color;\n"+
			"void main()\n"+
			"{\n"+
			"    gl_FragColor = texture2D(sampler, tex_coord) * vec4(color.r,color.g,color.b,alpha);\n"+
			"}\n";
	
	/** How many pixels in the texture is considered to be a border */
	protected final Vector2f m_texBorder = new Vector2f();
	/** Thickness of the border when drawing */
	protected final Vector2f m_borderThickness = new Vector2f();
	/** Whether or not to draw the center piece or just the border */
	protected boolean m_drawCenter = true;
	
	/** Constructor for a texture file that has a single x/y border thickness. */
	public NvUIGraphicFrame(String texname, float border){
		super(texname, 0, 0);
		
		m_texBorder.x = border;
	    m_texBorder.y = border;
	    m_borderThickness.x = border;
	    m_borderThickness.y = border;
	    staticInit();
	}
	
	/** Constructor for a texture file with independent x and y border thicknesses. */
	public NvUIGraphicFrame(String texname, float borderX, float borderY){
		super(texname, 0, 0);
		
		m_texBorder.x = borderX;
	    m_texBorder.y = borderY;
	    m_borderThickness.x = borderX;
	    m_borderThickness.y = borderY;
	    staticInit();
	}
    /** Constructor for an existing NvUITexture that has a single x/y border thickness. */
	public NvUIGraphicFrame(NvUITexture uiTex, float border){
		super(uiTex, 0, 0);
		
		m_texBorder.x = border;
	    m_texBorder.y = border;
	    m_borderThickness.x = border;
	    m_borderThickness.y = border;
	    staticInit();
	}
	
	@Override
	public void dispose() {
		super.dispose();
		
		staticCleanup();
	}
	
	static void staticCleanup(){
		if (s_staticCount)
	    {
			GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
			gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
			gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);
			gl.glUseProgram(0);

//	        ms_shader.m_program.dispose();  TODO can't do this on Android 
	        ms_shader.m_program = null;

//	        GLES.glDeleteBuffers(ms_gfvbo);  TODO can't do this on Android 
//	        GLES.glDeleteBuffers(ms_gfibo);
	        ms_gfvbo = 0;
	        ms_gfibo = 0;
	        
	        s_staticCount = false;
	    }
	}
	
	/** Loads a texture from cache or file.
     * We override the default handling to force @p resetDimensions to false, as frames
     * inherently stretch to render/fill the specified destination rectangle, and thus
     * we don't want to take on the dimensions of the texture by default.
	 */
	public boolean loadTexture(String texname, boolean resetDimensions){
		// We pass false to inherited call to be explicit to leave our dimensions alone, and
	    // not change them to match the texture's size, as frames inherently 'stretch' at draw
	    // time to match the destination size.
		if(super.loadTexture(texname, false))
			return true;
		return false;
	}
	
	private static boolean staticInit(){
	    if (!s_staticCount)
	    {
			GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
	        // TODO: disable drawing of the center piece?
//	    	typedef struct
//	    	{
//	    	    float x, y;     // position
//	    	    float s, t;     // texture coordinate
//	    	    float bx, by;   // whether or not this vertex is a border vertex in x and y
//	    	} NvFrameVertex;
	        float vert[][] = new float[16][6];
	        float temp[][] =
	        {
	            {-1,  1, 0, 1, 0, 0},
	            { 1,  1, 1, 1, 0, 0},
	            {-1, -1, 0, 0, 0, 0},
	            { 1, -1, 1, 0, 0, 0}
	        };

	        for (int y = 0; y < 4; y++)
	        {
	            for (int x = 0; x < 4; x++)
	            {
	                int src = ((y >> 1)*2) + (x >> 1);  //0,0,1,1,0,0,1,1,2,2,3,3,2,2,3,3
	                int dst = y*4+x;                    //0,1,2,3,4,5,6,7...
//	                memcpy(&vert[dst], &temp[src], sizeof(NvFrameVertex));
	                System.arraycopy(temp[src], 0, vert[dst], 0, 6);
	                if (y == 0 || y == 3)
	                    vert[dst][5] = 1;
	                if (x == 0 || x == 3)
	                    vert[dst][4] = 1;
	            }
	        }

	        short indices[] = {
	                  0, 4, 1, 5, 2, 6, 3, 7,         7, // first row
	          4,      4, 8, 5, 9,                     9, // left panel
	          6,      6, 10, 7, 11, 11, // right panel
	          8,      8, 12, 9, 13, 10, 14, 11, 15,   15, // bottom
	          5,      5, 9, 6, 10 // center piece
	        };

	        ms_shader.load(s_frameVertShader, s_frameFragShader);

	        ms_gfibo = gl.glGenBuffer();
	        ms_gfvbo = gl.glGenBuffer();

			gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, ms_gfvbo);
			gl.glBufferData(GLenum.GL_ARRAY_BUFFER, CacheBuffer.wrap(vert), GLenum.GL_STATIC_DRAW);

			gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, ms_gfibo);
			gl.glBufferData(GLenum.GL_ELEMENT_ARRAY_BUFFER, CacheBuffer.wrap(indices), GLenum.GL_STATIC_DRAW);

			gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
			gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);

	        // The following entries are const
	        // so we set them up now and never change
	        s_gfpixelToClipMatrix[2][0] = 0.0f;
	        s_gfpixelToClipMatrix[2][1] = 0.0f;

	        s_gfpixelToClipMatrix[0][2] = 0.0f;
	        s_gfpixelToClipMatrix[1][2] = 0.0f;
	        s_gfpixelToClipMatrix[2][2] = 1.0f;
	        s_gfpixelToClipMatrix[3][2] = 0.0f;

	        s_gfpixelToClipMatrix[0][3] = 0.0f;
	        s_gfpixelToClipMatrix[1][3] = 0.0f;
	        s_gfpixelToClipMatrix[2][3] = 0.0f;
	        s_gfpixelToClipMatrix[3][3] = 1.0f;
	        
	        s_staticCount = true;
	    }

	    return true;
	}
	
	/** Set a single x/y border thickness for this frame. */
	public void setBorderThickness(float thickness){
		m_borderThickness.x = thickness;
	    m_borderThickness.y = thickness;
	}
	
	/** Set independent x and y border thicknesses for this frame. */
	public void setBorderThickness(float width, float height){
		m_borderThickness.x = width;
	    m_borderThickness.y = height;
	}
	
	/** Get the x and y border thicknesses for this frame. return the reference.*/
	public Vector2f getBorderThickness(){
		return m_borderThickness;
	}
	
	/** Flag for whether to draw the center piece of the 3x3 frame grid, or ignore that quad. */
	public void setDrawCenter(boolean drawCenter){
		m_drawCenter = drawCenter;
	}
	
	/** Renders the frame texture appropriately stretched to fit the target position/dimensions. */
	public void draw(NvUIDrawState drawState) // Override parent class drawing
	{
		if (!m_isVisible) return;
	    if (m_tex == null) return;
		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();

	    // calculate internal alpha value...
	    float myAlpha = m_alpha;
	    if (drawState.alpha != 1.0f)
	        myAlpha *= drawState.alpha;

	    // pick correct shader based on alpha...
	    ms_shader.m_program.enable();

	    // then if alpha shader, set alpha uniform...
	    if (ms_shader.m_alphaIndex >= 0)
			gl.glUniform1f(ms_shader.m_alphaIndex, myAlpha);

	    // then if colorizing shader, set color uniform...
	    if (ms_shader.m_colorIndex >= 0)
	    {   // optimize it a bit...  // !!!!TBD alpha in color not just sep value?
	        if ((m_color & 0xFFFFFF) == 0xFFFFFF)
				gl.glUniform4f(ms_shader.m_colorIndex, 1,1,1,1);
	        else
				gl.glUniform4f(ms_shader.m_colorIndex,
						Numeric.getRedFromRGBf(m_color),
						Numeric.getGreenf(m_color),
						Numeric.getBlueFromRGBf(m_color),
	                            1); // !!!!TBD
	    }

	    // update the transform matrix.
	    int designWidth, designHeight;
	    if (drawState.designWidth != 0)
	    {
	        designWidth = drawState.designWidth;
	        designHeight = drawState.designHeight;
	    }
	    else
	    {
	        designWidth = drawState.width;
	        designHeight = drawState.height;
	    }
	    
	    // update the scale factors ONLY IF cached design size changed.
	    if (s_gfpixelXLast != designWidth)
	    {
	        s_gfpixelXLast = designWidth;
	        s_gfpixelScaleFactorX = 2.0f / s_gfpixelXLast;
	    }
	    if (s_gfpixelYLast != designHeight)
	    {
	        s_gfpixelYLast = designHeight;
	        s_gfpixelScaleFactorY = 2.0f / s_gfpixelYLast;
	    }

	    float rad = (float)(drawState.rotation / 180.0f * 3.14159f); // [-1,2]=>[-90,180] in radians...
	    float cosf = (float)StrictMath.cos(rad);
	    float sinf = (float)StrictMath.sin(rad);

	    final float wNorm = s_gfpixelScaleFactorX;
	    final float hNorm = s_gfpixelScaleFactorY;

	    s_gfpixelToClipMatrix[0][0] = wNorm * m_rect.width  * cosf;
	    s_gfpixelToClipMatrix[1][0] = hNorm * m_rect.height * -sinf;
	    s_gfpixelToClipMatrix[0][1] = wNorm * m_rect.width  * sinf;
	    s_gfpixelToClipMatrix[1][1] = hNorm * m_rect.height * cosf;

	    s_gfpixelToClipMatrix[3][0] = ( wNorm * m_rect.left - 1) * cosf
	                              - ( 1 - hNorm * (m_rect.top + m_rect.height))  * sinf;
	    s_gfpixelToClipMatrix[3][1] = ( wNorm * m_rect.left - 1 ) * sinf
	                              + ( 1 - hNorm * (m_rect.top + m_rect.height))  * cosf;

		gl.glUniformMatrix4fv(ms_shader.m_matrixIndex, false, CacheBuffer.wrap(s_gfpixelToClipMatrix));

	    float thicknessx = m_borderThickness.x; 
	    float thicknessy = m_borderThickness.y;
	    if (thicknessx > m_rect.width / 2)
	        thicknessx = m_rect.width / 2;
	    if (thicknessy > m_rect.height / 2)
	        thicknessy = m_rect.height / 2;
	    thicknessx /= m_rect.width/2;
	    thicknessy /= m_rect.height/2;

		gl.glUniform2f(ms_shader.m_texBorderIndex,
	                    m_texBorder.x / m_tex.getWidth(),
	                    m_texBorder.y / m_tex.getHeight());
		gl.glUniform2f(ms_shader.m_thicknessIndex, thicknessx, thicknessy);

	    // set up texturing.
	    boolean ae = false;
	    if (m_tex.getHasAlpha() || (myAlpha<1.0f))
	    {
	        ae = true;
			gl.glEnable(GLenum.GL_BLEND);
	        // Alpha sums in the destination channel to ensure that
	        // partially-opaque items do not decrease the destination
	        // alpha and thus "cut holes" in the backdrop
			gl.glBlendFuncSeparate(GLenum.GL_SRC_ALPHA, GLenum.GL_ONE_MINUS_SRC_ALPHA,
					GLenum.GL_ONE, GLenum.GL_ONE);
	    }
	    else
			gl.glDisable(GLenum.GL_BLEND);

//	    glUniform1i(shader.m_samplerIndex, 0); // texunit index zero.
		gl.glActiveTexture(GLenum.GL_TEXTURE0);
		gl.glBindTexture(GLenum.GL_TEXTURE_2D, m_tex.getGLTex());

	    // setup data buffers/attribs.
		gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, ms_gfvbo);
		gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, ms_gfibo);

		gl.glVertexAttribPointer(ms_shader.m_positionIndex, 2, GLenum.GL_FLOAT, false, 6 * 4, 0);
		gl.glEnableVertexAttribArray(ms_shader.m_positionIndex);
		gl.glVertexAttribPointer(ms_shader.m_uvIndex, 2, GLenum.GL_FLOAT, false, /*sizeof(NvFrameVertex)*/ 6 * 4, (2* 4));
		gl.glEnableVertexAttribArray(ms_shader.m_uvIndex);
		gl.glVertexAttribPointer(ms_shader.m_borderIndex, 2, GLenum.GL_FLOAT, false, /*sizeof(NvFrameVertex)*/ 6 * 4, (2*2* 4));
		gl.glEnableVertexAttribArray(ms_shader.m_borderIndex);

	    // draw it already!
		gl.glDrawElements(GLenum.GL_TRIANGLE_STRIP, m_drawCenter ? (30+6) : 30, GLenum.GL_UNSIGNED_SHORT, 0);

	    //nv_flush_tracked_attribs();
		gl.glDisableVertexAttribArray(ms_shader.m_positionIndex);
		gl.glDisableVertexAttribArray(ms_shader.m_uvIndex);
		gl.glDisableVertexAttribArray(ms_shader.m_borderIndex);

	    if (ae)
			gl.glDisable(GLenum.GL_BLEND);
	}
}

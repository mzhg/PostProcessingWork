//----------------------------------------------------------------------------------
// File:        NvBFText.java
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

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.util.CachaRes;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.LogUtil;
import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.postprocessing.util.StringUtils;

/**
 * BFText 'objects' are an abstraction of a 'text run' or drawable string, and
 * contain all information and buffers necessary to quickly draw the text
 * using GLES primitives.
 * @author Nvidia 2014-9-9 10:23
 * @see NvBitFont
 */
public class NvBFText implements NvUITextAlign, Disposeable {

	private static final int IND_PER_QUAD = 6;
	private static final int VERT_PER_QUAD = 4;
	
	static float s_bfShadowMultiplier = 0.80f; // this is for android.
//	static float s_bfShadowMultiplier = 0.40f;
	
	private static final int[] s_charColorTable = {
		Numeric.makeRGBA(0xFF, 0xFF, 0xFF, 0xFF), //white
		Numeric.makeRGBA(0x99, 0x99, 0x99, 0xFF), //medium-gray
		Numeric.makeRGBA(0x00, 0x00, 0x00, 0xFF), //black
		Numeric.makeRGBA(0xFF, 0x33, 0x33, 0xFF), //brightened red
		Numeric.makeRGBA(0x11, 0xFF, 0x11, 0xFF), //brighter green
		Numeric.makeRGBA(0x33, 0x33, 0xFF, 0xFF) //brightened blue
	};
	
	protected int m_stringMax;
	protected String m_string;  // !! ascii for now
    // !!!!TBD not sure how much these features will be allowed in unicode... hmmm...
	protected int m_stringCharsOut; // since string can have escape codes, need a sep count of REAL to output.
	protected int m_drawnChars = -1; // allowing 'clamping' the number of chars to actually draw.

    protected BFVert[] m_data;
    protected int m_vbo;
    
    protected int m_numLines;
    protected int m_calcLinesMax; // size of buffers allocated.
    protected int[] m_calcLineChars; // output char count per line
    protected float[] m_calcLineWidth; // line width

    protected int m_charColor = 0xFFFFFFFF; // base color.  set in vertices, can override with escape codes.

    protected boolean m_cached; // all vbo burn-in bits ready.
    protected boolean m_visible = true;
    protected byte m_fontNum;  /* 0 ~ 255 */
    protected float m_fontSize = 10;
    protected NvBitFont m_font;
    
    protected int m_hMode = LEFT;
    protected int m_vMode = TOP;
    protected float m_hPos = -1234; // 0-1 coordinates
    protected float m_vPos = -1234; // 0-1 coordinates
    protected float m_textLeft = -1234;
    protected float m_textTop = -1234; // screen coordinates

    protected float m_boxWidth; // max width before cap/wrap, zero if none.
    protected float m_boxHeight;
    protected int m_boxLines;
    
    protected boolean m_hasBox; // do we have a box set that we are going to adjust to?
    protected boolean m_doWrap;
    protected boolean m_doScissor; // !!!!TBD!!!!TBD
    protected boolean m_posCached; // position has alignment applied.
    
    protected int m_truncChar; // if a ... or other char.  zero if none.
    
    protected byte m_shadowDir; // signed so we can use it as shift amount
    protected int m_shadowColor = Numeric.NV_PC_PREDEF_BLACK;
    
    protected float m_pixelsWide;
    protected float m_pixelsHigh; // size of text.
    
    @Override
    public void dispose() {
    	// then clean up and NvFree.
//        if (m_calcLineChars )
//            free(m_calcLineChars);
        m_calcLineChars = null;

//        if (m_calcLineWidth)
//            free(m_calcLineWidth);
        m_calcLineWidth = null;

        if (m_vbo != 0)
            GLFuncProviderFactory.getGLFuncProvider().glDeleteBuffers(m_vbo);
        m_vbo = 0;

//        if (m_string)
//            free(m_string);
        m_string = null;

//        if (m_data)
//            free(m_data);
        m_data = null;
    }
    
    /** Set whether a given bftext is visible or invisible. */
    public void setVisiblity(boolean vis){
    	m_visible = vis;
    }
    
    /**
     * Set the RGBA color (using an {@link Numeric}) for a line of text.<p>
     * Since colors are per-character (stored in vertex data), changing the global text color
     * requires a rebuild of the string buffers.  If your intent is trying to fade (alpha),
     * or otherwise color-modulate a string on the fly, you can avoid rebuilding the cache
     * by using {@link #setMultiplyColor} instead.
     * NOTE: Embedded color literals/escapes inside the string data itself will take precedence
     * over this string-global color value.
     * @param color
     */
    public void setColor(int color){
    	if(m_charColor == color)
    		return;
    	
    	m_charColor = color;
        // flag we need to recache this bftext
        m_cached = false;
    }
    
    /**
     * Set the RGBA color (an {@link Numeric}) for a line of text.  As this color is multiplied
	 * in hardware, it doesn't require recaching the optimized vertex data for the string, thus
	 * allows for easy per-bftext alpha-fades and color modulation.
     * @param color
     */
    public void setMultiplyColor(int color){
    	
    }
    
    /**
     * Set the drop-shadow for a bftext.<p>
     * Activates a drop-shadow effect on given bftext output.<p>
     * 
     * @param offset the +/- offset of the shadow from the base, in 'texels' (not pixels)
     * @param color the shadow color as an NvPackedColor
     */
    public void setShadow(byte offset, int color){
    	if (m_shadowDir==offset && m_shadowColor == color)
            return; // no change.
        m_shadowDir = offset;
        m_shadowColor = color;
        // flag we need to recache this bftext
        m_cached = false;
    }
    
    /** Draw less than the full string.<p>
     * 
     * Switches bftext to draw only first num characters in a bftext string.
     * Most useful for helping do simple 'type on' effects.
     * Note that positioning on the screen is where the chars would be if
     * entire text was rendered -- thus left and top alignment are best.
	 */
	public void setDrawnChars(int num){
		 m_drawnChars = num;
	}
	
	/**
	 * Use a subrect of the screen for this bftext's raster operations.<p>
	 * Sets a subrect in the given screen size, in which processes such as alignment,
	 * multi-line wrapping, will occur.
     * @param width left/right alignment 'edges' (and wrap/clip boundary).
     * @param height top/bottom alignment 'edges'.
     * @param lines if greater than zero, specifies width is used to wrap text, and how many lines to wrap.
     * @param dots if greater than zero, specifies a character to repeat three times when needs wrap but out of lines.
	 */
	public void setBox(float width, float height, int lines, int dots){
		if (m_hasBox
			    &&  m_boxWidth == width
			    &&  m_boxHeight == height
			    &&  m_boxLines == lines
			    &&  m_truncChar == dots)
			        return; // no changes.
			    
			    m_doWrap = false;
			    m_truncChar = 0;
			    if (width==0 || height==0) // invalid, clear the box.
			    {
			        m_hasBox = false;
			        m_boxWidth = 0;
			        m_boxHeight = 0;
			        m_boxLines = 0;
			    }
			    else
			    {
			        m_hasBox = true;
			        m_boxWidth = width;
			        m_boxHeight = height;
			        m_boxLines = lines;

			        if (lines!=1) // don't wrap if explicitly one line only.
			            m_doWrap = true;
			        
			        if (0!=dots)
			            m_truncChar = dots;       
			    }
			        
			    // flag we need to recache this bftext
			    m_cached = false;
	}
	
	/** Helper to quickly update width and height of previously-set text box. */
	public void updateBox(float width, float height){
		if (m_hasBox)
	    {
	        if (m_boxWidth != width
	        ||  m_boxHeight != height)
	        {
	            m_boxWidth = width;
	            m_boxHeight = height;
	            // flag we need to recache this bftext
	            m_cached = false;
	        }
	    }
	}
	
	/** Select font 'face' to use for this bftext (font ID from NvBFGetFontID). */
    public void setFont(byte fontnum){
    	if (fontnum == m_fontNum) // same, we're done before we've even started.
            return;

        // flag we need to recache this bftext
        m_cached = false;

        // just cache the values.  we'll use them in the recache function.
        if (fontnum==0) // oops...
        {
            m_fontNum = 0; // tag this.
            m_font = null;
        }
        else
            m_font = NvBitFont.bitFontFromID(fontnum); // find our font and hold onto the ptr, for ease of coding.
        if (m_font!=null)
               m_fontNum = fontnum;
        else // handle the case that font wasn't loaded...
        {
            // !!!!TBD ASSERT????
            m_font = NvBitFont.bitFontLL; // whatever the first font is.
            if (m_font != null)
                m_fontNum = m_font.m_id;
            else
                m_fontNum = 0; // try and have 0 to test later!!!
        }
           
        if (m_fontNum==0) // something went wrong.
        {
            // what can we do? !!!!TBD
            m_cached = true; // so we try to do no more work!
        }
    }
    
    /** Set the output size of text in destination-space pixels, dependent on input font size/scale. */
    public void setSize(float size){
    	if (size == m_fontSize) // same, we're done before we've even started.
            return;

        // flag we need to recache this bftext
        m_cached = false;
        m_fontSize = size;
    }
    
    /** Set the text string for a given bftext. */
    public void setString(String str){
    	if (StringUtils.isEmpty(str))
        {
            LogUtil.i(LogUtil.LogType.NV_FRAMEWROK, "bitfont sent a null string ptr...");
//        if (m_string==NULL)
//            DEBUG_LOG("bitfont has no string allocated yet...");
            m_string = "";
        }
        
        if(m_stringMax > 0 && m_string.equals(str)) // same, we're done before we've even started.
        	return;

        // flag we need to recache this bftext
        m_cached = false;
        // clear other computed values.
        m_pixelsWide = 0;
        m_pixelsHigh = 0;

        // check that we have storage enough for the string, and for calc'd data
        // then, check that we have enough space in our bftext-local storage...
        // !!!!TBD Getuint8_tLength isn't definitively what I want, I don't think... might be multi-word chars in there.
        int m_stringChars = str.length();
        int charsToAlloc = 2*(m_stringChars+1); // doubled for shadow text... !!!!TBD remove if we do shadow in shader.
        if (charsToAlloc > m_stringMax-1) // need to account for null termination and for doubled shadow text
        {
//            if (m_stringMax) // allocated, NvFree structs.
//            {
//                free(m_string);
//                free(m_data);
//            }
            // reset max to base chars padded to 16 boundary, PLUS another 16 (8+8) for minor growth.
            m_stringMax = charsToAlloc + 16-((charsToAlloc)%16) + 16;
//            m_string = (char*)malloc(m_stringMax*sizeof(char)); // !!!!TBD should use TCHAR size here??
//            memset(m_string, 0, m_stringMax*sizeof(char));
//            m_data = (BFVert*)malloc(m_stringMax*sizeof(BFVert)*VERT_PER_QUAD);
            m_data = new BFVert[m_stringMax * VERT_PER_QUAD];
        }

//        memcpy(m_string, str, m_stringChars+1); // include the null.
        m_string = str;
    }
    
    public static void main(String[] args) {
		String str = "";
		int m_stringChars = str.length();
        int charsToAlloc = 2*(m_stringChars+1);
        int m_stringMax = charsToAlloc + 16-((charsToAlloc)%16) + 16;
        System.out.println(m_stringMax);
	}

    @CachaRes
    private static int updateMasterIndexBuffer(int stringMax, boolean internalCall){
    	int maxIndexChars = NvBitFont.maxIndexChars;
    	if (stringMax > maxIndexChars) // reallocate...
        {
//            if (NvBitFont.maxIndexChars > 0) // delete first..
//                free(masterTextIndexList);
        
    		maxIndexChars = stringMax; // easy solution, keep these aligned!
            int n = IND_PER_QUAD * maxIndexChars;
            short[] masterTextIndexList = new short[n];
        
            // re-init the index buffer.
            for (int c=0; c<maxIndexChars; c++) // triangle list indices... three per triangle, six per quad.
            {
                masterTextIndexList[c*6+0] = (short)(c*4+0);
                masterTextIndexList[c*6+1] = (short)(c*4+2);
                masterTextIndexList[c*6+2] = (short)(c*4+1);
                masterTextIndexList[c*6+3] = (short)(c*4+0);
                masterTextIndexList[c*6+4] = (short)(c*4+3);
                masterTextIndexList[c*6+5] = (short)(c*4+2);
            }
            
            NvBitFont.masterTextIndexList = masterTextIndexList;
            GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();

            if (!internalCall)
                gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, NvBitFont.masterTextIndexVBO);
            gl.glBufferData(GLenum.GL_ELEMENT_ARRAY_BUFFER, CacheBuffer.wrap(masterTextIndexList), GLenum.GL_STATIC_DRAW);
            if (!internalCall)
                gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0); // !!!!TBD resetting here, if we're INSIDE the render call, is wasteful... !!!!TBD
        }
    	NvBitFont.maxIndexChars = maxIndexChars;
        
        return 0;
    }
    
    private void adjustGlyphsForAlignment(){
    	if (m_hMode== LEFT)
            return; // nothing to do.

        LogUtil.i(LogUtil.LogType.NV_FRAMEWROK, "Adjusting glyphs for alignment...");
        final boolean center = (m_hMode== CENTER);
        BFVert []vp = m_data;
        int t = 0;
        // loop over the lines in this bftext...
        for (int i=0; i<m_numLines; i++)
        {
            final int max = m_calcLineChars[i];
            LogUtil.i(LogUtil.LogType.NV_FRAMEWROK, String.format("line %d, char count %d.", i, max));
            float w = m_calcLineWidth[i];
            if (center)
                w *= 0.5f;
            // loop through chars on this line, shifting X coords
            // note char count covers shadow glyphs properly.
            for (int j=0; j<max; j++)
            {
                for (int k=0; k<4; k++)
                {
                    vp[t].posX -= w; // shift back by half or full linewidth.
                    t++; // next vertex;
                }
            }
        }
    }
    
    private static int addGlyphVertex(BFVert[] vp,int index, float x, float y,
            float uvx, float uvy,
            int color ){
    	if(vp[index] == null){
    		vp[index] = new BFVert();
    	}
    	
    	vp[index].posX = x;
    	vp[index].posY = y;
    	vp[index].uvX = uvx;
    	vp[index].uvY = uvy;
    	vp[index].color = color;
    	
    	index ++;
    	return index;
    }
    
    private static float addOutputGlyph(AFontChar fc, AFont afont, BFVert[] vp, int index, float left,
                                        float t, float b, float hsizepertex, int color){
    	float pX = left + (fc.m_xOff * hsizepertex);
        float pY = t + (fc.m_yOff * hsizepertex);
        // adjust for baseline and a bit of lineheight, since we're positioning top-corner, NOT baseline...
        pY -= afont.m_charCommon.m_baseline * hsizepertex;
        pY += (afont.m_charCommon.m_lineHeight - afont.m_charCommon.m_baseline) * 0.3f * hsizepertex;
        float pH = fc.m_height * hsizepertex;
        float pW = fc.m_width * hsizepertex;
        left += (fc.m_xAdvance * hsizepertex);
        // must invert Y on uv as we flipped texture coming in.
        float tx = fc.m_x / afont.m_charCommon.m_pageWidth;
        float ty = fc.m_y / afont.m_charCommon.m_pageHeight;
        float tw = fc.m_width / afont.m_charCommon.m_pageWidth;
        float th = fc.m_height / afont.m_charCommon.m_pageHeight;
        float uvt = ty;
        float uvb = ty+th;

        index = addGlyphVertex(vp, index, pX, pY, tx, uvt, color);
        index = addGlyphVertex(vp, index, pX , pY + pH, tx, uvb, color);
        index = addGlyphVertex(vp, index, pX + pW, pY + pH, tx+tw, uvb, color);
        index = addGlyphVertex(vp, index, pX + pW, pY, tx+tw, uvt, color);
        
        return left;
    }
    
    private void trackOutputLines(float lineWidth){
    	final int charCount = m_stringCharsOut; // it's in bf, no need to pass.
        
        // then we want to track lines so we can align
        if (m_calcLinesMax==0)
        {
            m_calcLinesMax = 8; // something that seems decent for single lines
                                // or small boxes, we'll grow later.
//            m_calcLineChars = (int32_t *)malloc(sizeof(int32_t) * m_calcLinesMax);
            m_calcLineChars = new int[m_calcLinesMax];
            
//            m_calcLineWidth = (float *)malloc(sizeof(float) * m_calcLinesMax);
            m_calcLineWidth = new float[m_calcLinesMax];
        }
        
        if (m_numLines > m_calcLinesMax)
        { // then resizing.
            int newMax = (m_calcLinesMax*3)/2; // add 50% of current.  reasonable.
//            int *newLineChars = (int32_t *)malloc(sizeof(int32_t) * newMax);
            int[] newLineChars = new int[newMax];
            
//            float *newLineWidth = (float *)malloc(sizeof(float) * newMax);
            float[] newLineWidth = new float[newMax];
            // copy the line data.
            for (int i=0; i < m_calcLinesMax; i++)
            {
                newLineChars[i] = m_calcLineChars[i];
                newLineWidth[i] = m_calcLineWidth[i];
            }
            // free old data
//            free(m_calcLineChars);
//            free(m_calcLineWidth);
            // swap in new data.
            m_calcLineChars = newLineChars;
            m_calcLineWidth = newLineWidth;
            m_calcLinesMax = newMax;
        }
        
        // NOW, we actually get around to the tracking. :)
        int lineNum = 0;
        int prevChars = 0;
        if (m_numLines>0)
            lineNum = m_numLines - 1;
        for (int i=0; i<lineNum; i++)
            prevChars += m_calcLineChars[i];
        m_calcLineChars[lineNum] = charCount-prevChars;
        m_calcLineWidth[lineNum] = lineWidth;
    }

    /** Get the last calculated output width of the bftext string. */
    public float getWidth(){
    	return m_pixelsWide;
    }
    /** Get the last calculated output height of the bftext. */
    public float getHeight(){
    	return m_pixelsHigh;
    }
    
    //============================================================================
    // positioning the text output
    //============================================================================
    
    /**
     * Set the bftext horizontal and vertical alignment.<p>
     * Sets the alignment 'edges' for render offset calculation and direction.
     */
    public void setCursorAlign(int h, int v){
    	if (m_vMode!=v || m_hMode!=h) // reset
        {
            m_vMode = v;
            m_hMode = h;
            m_posCached = false; // FIXME for now, force recalc.
        }
    }



    /**
     * Set the bftext starting render position.<p>
     * Sets an inset/offset from the horizontal and vertical 'edges', as selected via
     * {@link #setCursorAlign}, in possible combination with {@link #setBox(float, float, int, int)}.
     * That is, the borders to offset from are "screen edges", unless {@link #setBox(float, float, int, int)}
     * was called in which case that box/rect is used to determine the 'edges' instead
     * to inset from.
     */
    public void setCursorPos(float h, float v){
    	if (m_hPos!=h || m_vPos!=v)
        {
            m_hPos = h;
            m_vPos = v;    
            m_posCached = false; // we need to adjust
//        m_hModeVBO = -1;
        }
    }
    
    //============================================================================
    // bitfont rendering functions
    //============================================================================
    
    /**
     * Prepare to render some bftext objects.<p>
     * This sets up necessary bits of the BitFont raster system.
     * From this until calling @ref NvBFRenderDone, do no other GL operations.
     * It can/should be called once before rendering one or more bftexts, for instance
     * looping over an array of text elements on screen.
     */
    public void renderPrep(){
    	if (gSaveRestoreState)
            saveGLState();

    	GLSLProgram fontProg = NvBitFont.fontProg;
        fontProg.enable();
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();

        // set up master rendering state
        gl.glDisable(GLenum.GL_CULL_FACE);
        gl.glDisable(GLenum.GL_DEPTH_TEST);
        gl.glDisable(GLenum.GL_STENCIL_TEST);
        gl.glDepthMask(false);
        
        // blending...
        gl.glEnable(GLenum.GL_BLEND);
        
        // texture to base.
        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        // do we need to loop over TUs and disable any we're not using!?!?! !!!!TBD

        // any master buffer...
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, NvBitFont.masterTextIndexVBO);

//        TestPrintGLError("Error 0x%x in NvBFText::RenderPrep...\n");

        // reset state caching!
        NvBitFont.lastFontTexture = 0;
        NvBitFont.lastTextMode = 111; // won't ever match... ;)
    }
    
    /** Conclude rendering of bftexts, making it safe to do other GLES calls. */
    public void renderDone(){
    	if (gSaveRestoreState)
            restoreGLState();
        else
        {
            GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
            gl.glBindTexture( GLenum.GL_TEXTURE_2D, 0 );
            NvBitFont.lastFontTexture = 0;

            gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);
            gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
            
            // !!!!TBD TODO do we need to get this working again??
            //nv_flush_tracked_attribs(); // clear any attrib binds.
            gl.glDisableVertexAttribArray(NvBitFont.fontProgAttribPos);
            gl.glDisableVertexAttribArray(NvBitFont.fontProgAttribTex);
            gl.glDisableVertexAttribArray(NvBitFont.fontProgAttribCol);
            
            NvBitFont.fontProg.disable();
            gl.glDepthMask(true);
            gl.glDisable(GLenum.GL_BLEND);
        }
        
        setMatrix(null); // explicitly clear each Done.
    }

    /** Set a specific transformation matrix to be used for rendering all text strings,
        until the next call to @ref RenderDone. */
    @CachaRes
    public void setMatrix(float[] mat){
    	NvBitFont.m_matrixOverride = mat;
    	
    	if(mat != null)
            GLFuncProviderFactory.getGLFuncProvider().glUniformMatrix4fv(NvBitFont.fontProgLocMat,false, CacheBuffer.wrap(mat));
    }
    
    private void updateTextPosition(){
    	if (m_posCached) return;
        
        m_textLeft = m_hPos;
        m_textTop = m_vPos;
        
        // horizontal adjustments
        if (m_hasBox)
        {
            if (m_hMode== CENTER)
                m_textLeft += (m_boxWidth*0.5f); // align box to point, center content within.
            else if (m_hMode== RIGHT)
                m_textLeft += m_boxWidth; // align to right of positioned box.
        }

        // vertical adjustments
        if (m_vMode== CENTER)
        {
            if (m_hasBox)
                m_textTop += (m_boxHeight*0.5f); // shift down half box size to center
            m_textTop -= (m_pixelsHigh*0.5f); // shift up half text height to center
        }
        else if (m_vMode== BOTTOM)
        {
            if (m_hasBox)
                m_textTop += m_boxHeight; // shift down by box height
            m_textTop -= m_pixelsHigh; // shift up by text height
        }
        
        m_posCached = true;
    }

    //============================================================================
    /** The main call to actually render a bftext using stored properties. */
    @CachaRes
    public void render(){
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        int count = m_drawnChars;
        int m_stringChars = m_string.length();

        if (!m_visible || m_fontNum == 0 || m_font == null) // nothing we should output...
            return;

        if (count<0)
            count = m_stringChars; // !!!TBD maybe negative means something else.
        else
        if (count>m_stringChars)
            count = m_stringChars;
        if (count==0)
            return; // done...
        if (m_shadowDir != 0)
            count *= 2; // so we draw char+shadow equally...

        // set up master rendering state
        {
            int offset = 0;
            if (m_vbo == 0)
            	m_vbo = gl.glGenBuffers(); // !!!!TBD TODO error handling.
            gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, m_vbo);

            gl.glVertexAttribPointer(NvBitFont.fontProgAttribPos, 2, GLenum.GL_FLOAT, false, 20, 0);
            gl.glEnableVertexAttribArray(NvBitFont.fontProgAttribPos);
            offset += 4 * 2; // jump ahead the two floats

            gl.glVertexAttribPointer(NvBitFont.fontProgAttribTex, 2, GLenum.GL_FLOAT, false, 20, offset); // !!!!TBD update this to use a var if we do 2 or 3 pos verts...
            gl.glEnableVertexAttribArray(NvBitFont.fontProgAttribTex);
            offset += 4 * 2; // jump ahead the two floats.

            gl.glVertexAttribPointer(NvBitFont.fontProgAttribCol, 4, GLenum.GL_UNSIGNED_BYTE, true, 20, offset); // !!!!TBD update this to use a var if we do 2 or 3 pos verts...
            gl.glEnableVertexAttribArray(NvBitFont.fontProgAttribCol);
            offset += 4;
        }

        // since buffer state is now set, can rebuild cache now without extra calls.
        if (!m_cached) // need to recache BEFORE we do anything using textwidth, etc.
            rebuildCache(true);
        if (count > m_stringCharsOut) // recheck count against CharsOut after rebuilding cache
            count = m_stringCharsOut;
        if (!m_posCached) // AFTER we may have rebuilt the cache, we check if we recalc pos.
            updateTextPosition();

        // set the model matrix offset for rendering this text based on position & alignment
        // first, do any pre-render position updates

        // we apply any global screen orientation/rotation so long as
        // caller hasn't specified their own transform matrix.
        if (NvBitFont.m_matrixOverride==null)
        {
        	float[][] s_pixelToClipMatrix = NvBitFont.s_pixelToClipMatrix;
            final float wNorm = NvBitFont.s_pixelScaleFactorX;
            final float hNorm = NvBitFont.s_pixelScaleFactorY;
            if (NvBitFont.dispRotation==0)
            { // special case no rotation to be as fast as possible...
                s_pixelToClipMatrix[0][0] = wNorm;
                s_pixelToClipMatrix[1][0] = 0;
                s_pixelToClipMatrix[0][1] = 0;
                s_pixelToClipMatrix[1][1] = -hNorm;

                s_pixelToClipMatrix[3][0] = (wNorm * m_textLeft) - 1;
                s_pixelToClipMatrix[3][1] = 1 - (hNorm * m_textTop);
            }
            else
            {
                float rad = (float)(3.14159f/180.0f);  // deg->rad
                float cosfv;
                float sinfv;

                rad = (NvBitFont.dispRotation * rad); // [-1,2]=>[-90,180] in radians...
                cosfv = (float)Math.cos(rad);
                sinfv = (float)Math.sin(rad);

                s_pixelToClipMatrix[0][0] = wNorm * cosfv;
                s_pixelToClipMatrix[1][0] = hNorm * sinfv;
                s_pixelToClipMatrix[0][1] = wNorm * sinfv;
                s_pixelToClipMatrix[1][1] = hNorm * -cosfv;

                s_pixelToClipMatrix[3][0] = (s_pixelToClipMatrix[0][0] * m_textLeft)
                                            - cosfv - sinfv
                                            + (s_pixelToClipMatrix[1][0] * m_textTop);
                s_pixelToClipMatrix[3][1] = (s_pixelToClipMatrix[0][1] * m_textLeft)
                                            - sinfv + cosfv
                                            + (s_pixelToClipMatrix[1][1] * m_textTop);
            }

            // upload our transform matrix.
            gl.glUniformMatrix4fv(NvBitFont.fontProgLocMat, false, CacheBuffer.wrap(s_pixelToClipMatrix));
        }

        // bind texture... now with simplistic state caching
        if (NvBitFont.lastFontTexture != m_font.m_tex)
        {
        	gl.glBindTexture( GLenum.GL_TEXTURE_2D, m_font.m_tex );
            NvBitFont.lastFontTexture = m_font.m_tex;
        }

        // now, switch blend mode to work for our luma-based text texture.
        if (m_font.m_alpha)
        {
            // We need to have the alpha make the destination alpha
            // so that text doesn't "cut through" existing opaque
            // destination alpha
            gl.glBlendFuncSeparate(GLenum.GL_SRC_ALPHA, GLenum.GL_ONE_MINUS_SRC_ALPHA, GLenum.GL_ONE, GLenum.GL_ONE);
            // glBlendAmout(0.5??) !!!!!TBD
        }

        // draw it already!
        //DEBUG_LOG("printing %d chars...", count);
        gl.glDrawElements(GLenum.GL_TRIANGLES, IND_PER_QUAD * count, GLenum.GL_UNSIGNED_SHORT, 0);

//        TestPrintGLError("Error 0x%x NvBFText::Render drawels...\n");
    }
    
    /**
     * Force a rebuilding of the cached vertex data for this bftext.<p>
     * This function recalculates all cached data related to a given bftext object,
	 * including vertex data for optimized rendering.  It does NOT normally need
	 * to be called by the programmer, but is useful in the specific case where you
	 * have a number of completely static strings, in which case calling this during
	 * the initialization process can allow the system to preallocate necessary
	 * structures and not need to recalc/allocate further at runtime.<p>
	
	 * Many BFText state-changing functions will flag that a given string requires
	 * its vertex data be rebuilt -- but the system waits until rendering is requested
	 * so that it only recomputes the cached data once, regardless of the number of
	 * states changed for that text since the previous render or rebuild call.
     * @param internal should be set to false, unless being called by an internal
     *  BitFont function which has already established the VBOs for this bftext.
     */
    public void rebuildCache(boolean internal){
    	int bfs = NvBftStyle.NORMAL;

        float vsize, hsize, hsizepertex, fullglyphwidth;
        float left, t, b;
        //float l,r;
        int n,j;
        int vp, lastvp;
        int vbmode = GLenum.GL_STATIC_DRAW /*GL_DYNAMIC_DRAW*/;
        final NvBitFont bitfont = m_font;
        int color;
        int linesign = 1;
        int lineheightdelta = 0; // !!!!TBD
        int lastlinestart = 0, lastwhitespacein = 0, lastwhitespaceout = 0; // so we can reset positions for wrappable text.
        float lastwhitespaceleft = 0;
        int realcharindex;
        float extrawrapmargin = 0;
        AFont currFont;
        AFontChar truncit = null, glyphit = null;
        final int m_stringChars = m_string.length();
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();

        if (m_cached) // then no work to do here, move along.
            return;
        if (m_fontNum == 0)
            return;
        if (bitfont == null)
            return;

        // first, check that our master index buffer is big enough.
        if (updateMasterIndexBuffer(m_stringMax, internal) != 0)
            return; // TODO FIXME error output/handling.

        // start with normal style
        currFont = bitfont.m_afont;

        // recalc size in terms of the screen res...
        j = m_fontNum & 0xFF - 1; // fontnum is 1-based, other stuff is 0-based
        vsize = m_fontSize;// *(high/((dispAspect<1)?640.0f:480.0f)); // need the texel-factor for the texture at the end...
        hsize = vsize;
        hsizepertex = vsize / bitfont.m_canonPtSize;

        // calc extra margin for wraps...
        if (m_hasBox && m_truncChar != 0)
        {
            // calculate the approx truncChar size needed.  Note we don't have
            // style info at this point, so this could be off by a bunch.  !!!!TBD FIXME
            extrawrapmargin = 0;
//            truncit = currFont->m_glyphs.find(m_truncChar);
//            if (currFont->m_glyphs.end() != truncit) // found it.
//                extrawrapmargin = truncit->second.m_xAdvance;
            truncit = currFont.m_glyphs.get(m_truncChar);
            if(truncit != null) // found it.
            	extrawrapmargin = truncit.m_xAdvance;
            extrawrapmargin *= 3; // for ...
        }
       
        // loop over the string, building up the output chars...
        left = 0;
        float maxWidth = 0;
        t = currFont.m_charCommon.m_baseline * hsizepertex;
        b = t + (linesign * vsize);
        vp = 0;
        color = m_charColor; // default to set color;
        m_stringCharsOut = 0;
        
        lastvp = vp;
        lastlinestart = 0;
        lastwhitespacein = 0;
        lastwhitespaceout = 0;
        n=0;
        m_numLines = 1;
        m_pixelsHigh = vsize * m_numLines;
        if(m_stringChars == 0)
        	return;
        
        while (n<m_stringChars)
        {
            // !!!!TBD THIS ISN'T UNICODE-READY!!!!
            realcharindex = m_string.charAt(n); // need to cast unsigned for accented chars...        
            if (realcharindex==0) // null.  done.
                break;

            if ((realcharindex=='\n') //==0x0A == linefeed.
            ||  (realcharindex=='\r')) //==0x0D == return.
            {
                if (m_hasBox && (m_boxLines > 0) &&
                        ((m_numLines + 1) > m_boxLines)) 
                    break; // exceeded line cap, break from cache-chars loop.
                n++;
                trackOutputLines(left);
                lastlinestart = n; // where we broke and restarted.
                lastwhitespacein = n; // so we can rollback input position..
                lastwhitespaceout = m_stringCharsOut; // so we can reset output position.
                lastvp = vp; // so we can reset output position.
                m_numLines++; // count lines!

                t = b + lineheightdelta; // move to next line.
                b = t + (linesign * vsize); // proper calc for bottom.
                left = 0; // reset to left edge.

                lastwhitespaceleft = 0; // on return, reset

                continue; // loop again.
            }

            // !!!!!TBD handling of unicode/multibyte at some point.
            if (realcharindex < 0x20) // embedded commands under 0x20, color table code under 0x10...
            {
                // first check any chars we want excluded!
                if (realcharindex=='\t')
                {
                    // do nothing here.  fall through, forward into processing.
                }
                else
                {
                    if (realcharindex < 0x10) // color table index
                    { // colorcodes are 1-based, table indices are 0-based
                        if (NvBftColorCode.NvBF_COLORCODE_MAX == realcharindex-1)
                            color = m_charColor; // default to set color;
                        else
                            color = s_charColorTable[realcharindex-1];
                    }
                    else // escape codes
                    {
                        if (realcharindex < NvBftStyle.MAX)
                            bfs = realcharindex;
                        if (bfs > NvBftStyle.NORMAL && bitfont.m_afontBold != null)
                            currFont = bitfont.m_afontBold;
                        else
                            currFont = bitfont.m_afont;
                    }
                    n++; // now proceed to next char.
                    continue; // loop again.
                }
            }

            // precalc the full glyph spacing, to optimize some of this processing.
            fullglyphwidth = 0;
            glyphit = currFont.m_glyphs.get(realcharindex);
            if (glyphit != null) // found it.
                fullglyphwidth = glyphit.m_xAdvance; // !!!!TBD TODO is this right???

            if (realcharindex==' ' || realcharindex=='\t') // hmmm, optimization to skip space/tab characters, since we encode the 'space' into the position.
            {
                // then, update the offset based on the total ABC width (which for a monospaced font should == fontCell size...)
                lastwhitespaceleft = left;
                left += fullglyphwidth;
                n++; // now proceed to next char.
                if (lastwhitespacein!=n-1) // then cache state
                {
                    lastwhitespacein = n; // so we can rollback input position..
                    lastwhitespaceout = m_stringCharsOut; // so we can reset output position.
                    lastvp = vp; // so we can reset output position.
                }
                // one more check+update
                if (lastwhitespacein==lastlinestart+1) { // was first char of our new line, reset linestart num
                    lastlinestart = n;
                }
                continue; // loop again.
            }
            
            // !!!!!TBD linewrap... should actually look ahead for space/lf, OR
            // needs to do a 'roll back' when it hits edge... but for extra long strings,
            // we need to realize we don't have a whitespace character we can pop back to,
            // and need to just immed character wrap.
            
            // check to see if we'd go off the 'right' edge (with spacing...)
            if ( m_hasBox && ((left + fullglyphwidth) > (m_boxWidth - extrawrapmargin)) )
            {
                if (!m_doWrap) // then character truncate!
                {
                }
                else // word wrapping, jump back IF it's sane to.
                if (lastwhitespacein!=lastlinestart)
                {
                    n = lastwhitespacein; // go back some chars.
                    m_stringCharsOut = lastwhitespaceout; // undo output buffering.
                    vp = lastvp; // undo output buffering.
                    left = lastwhitespaceleft; // undo word positioning.
                }
                else
                {
                    // !!!!TBD some update to outputs here.
                    // not resetting n, sco, or vp... keep going forward from HERE.
                }
                
                trackOutputLines(left);
                lastlinestart = n; // where we broke and restarted.
                lastwhitespacein = n; // so we can rollback input position..
                lastwhitespaceout = m_stringCharsOut; // so we can reset output position.
                lastvp = vp; // so we can reset output position.
                m_numLines++; // count lines!

                // !!!!TBD truncChar handling...
                // !!!!TBD how to insert truncChar's into output stream.  need sub-function to add char.
                
                if ((m_boxLines > 0) && (m_numLines > m_boxLines)) // FIXME lines+1????
                {
                    if (m_truncChar != 0)
                    {
                        int i;
                        if (m_doWrap) // if wrapping, shift to ... position.
                            left = lastwhitespaceleft;
                        if (truncit != null) // found it.
                            for (i=0; i<3; i++) // for ellipses style
                            {
                                if (m_shadowDir != 0)
                                {
                                    float soff = ((float)(m_shadowDir & 0xFF)) * s_bfShadowMultiplier;
                                    float tmpleft = left+soff; // so we don't really change position.
                                    tmpleft = addOutputGlyph( truncit, currFont, m_data, vp, tmpleft, t+soff, b+soff, hsizepertex, m_shadowColor );
                                    vp += 4;
                                    m_stringCharsOut++; // update number of output chars.
                                }        
                                
                                left = addOutputGlyph( truncit, currFont, m_data, vp, left, t, b, hsizepertex, color );
                                vp += 4;
                                m_stringCharsOut++; // update number of output chars.
                            }
                        
                        // update char count and line width since we're going to break out.
                        trackOutputLines(left);
                    }
                    break; // out of the output loop, we're done.
                }
               
                // if doing another line, reset variables.
                t = b + lineheightdelta; // move to next line.
                b = t + (linesign * vsize); // proper calc for bottom.
                if (maxWidth < left)
                    maxWidth = left;
                left = 0; // reset to left edge.
                lastwhitespaceleft = 0; // on return, reset
                
                continue; // restart this based on new value of n!
            }

            if (glyphit != null) // found the char above
            {
                if (m_shadowDir != 0)
                {
                    float soff = ((float)(m_shadowDir & 0xFF)) * s_bfShadowMultiplier;
                    float tmpleft = left+soff; // so we don't really change position.
                    tmpleft = addOutputGlyph( glyphit, currFont, m_data, vp, tmpleft, t+soff, b+soff, hsizepertex, m_shadowColor );
                    vp += 4;
                    m_stringCharsOut++; // update number of output chars.
                }        
                left = addOutputGlyph( glyphit, currFont, m_data, vp, left, t, b, hsizepertex, color );
                vp += 4;
                m_stringCharsOut++; // update number of output chars.
            }

            // now proceed to next char.
            n++; 
        }

        trackOutputLines(left);

        for (int i=0; i<m_numLines; i++)
            if (maxWidth < m_calcLineWidth[i])
                maxWidth = m_calcLineWidth[i];

        // if alignment is not left, we shift each line based on linewidth.
        adjustGlyphsForAlignment();
        
        //DEBUG_LOG(">> output glyph count = %d, stringMax = %d.", m_stringCharsOut, m_stringMax);
        
        if (!internal)
        {
            if (m_vbo == 0)
            	m_vbo = gl.glGenBuffers(); // !!!!TBD TODO error handling.
            gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, m_vbo);
        }

        gl.glBufferData(GLenum.GL_ARRAY_BUFFER, /*m_stringCharsOut*sizeof(BFVert)*VERT_PER_QUAD,*/ wrap(m_data, m_stringCharsOut * VERT_PER_QUAD), vbmode);
        if (!internal)
            gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0); // !!!!TBD resetting here, if we're INSIDE the render call, is wasteful... !!!!TBD

        m_pixelsWide = maxWidth; // cache the total width in output pixels, for justification and such.
        m_cached = true; // flag that we cached this.
        m_posCached = false; // flag that position needs recache.  FIXME could optimize...
    }
    
    static void saveGLState(){
    	int i;
        int tmpi;
        GLFuncProvider gl =GLFuncProviderFactory.getGLFuncProvider();
//        TestPrintGLError("Error 0x%x in SaveState @ start...\n");

        gStateBlock.programBound = gl.glGetInteger(GLenum.GL_CURRENT_PROGRAM);
//        for (i=0; i<SAVED_ATTRIBS_MAX; i++)
//        {
//        	tmpi = glGetVertexAttribiv(i, GLES20.GL_VERTEX_ATTRIB_ARRAY_ENABLED);
//            gStateBlock.attrib[i].enabled = (tmpi != 0);
//            if (tmpi != 0)
//            {
//            	gStateBlock.attrib[i].size = glGetVertexAttribiv(i, GLES20.GL_VERTEX_ATTRIB_ARRAY_SIZE);
//            	gStateBlock.attrib[i].stride = glGetVertexAttribiv(i, GLES20.GL_VERTEX_ATTRIB_ARRAY_STRIDE);
//            	gStateBlock.attrib[i].type = glGetVertexAttribiv(i, GLES20.GL_VERTEX_ATTRIB_ARRAY_TYPE);
//            	gStateBlock.attrib[i].norm = glGetVertexAttribiv(i, GLES20.GL_VERTEX_ATTRIB_ARRAY_NORMALIZED);
//            	// TODO We didn't kown the pointer size so far.
////            	gStateBlock.attrib[i].ptr = GL20.glGetVertexAttribPointer(i, GL20.GL_VERTEX_ATTRIB_ARRAY_POINTER, gStateBlock.attrib[i].ptr_size);
//           }
//        }

        gStateBlock.depthMaskEnabled = gl.glGetBoolean(GLenum.GL_DEPTH_WRITEMASK);
        gStateBlock.depthTestEnabled = gl.glIsEnabled(GLenum.GL_DEPTH_TEST);
        gStateBlock.blendEnabled = gl.glIsEnabled(GLenum.GL_BLEND);
        gStateBlock.cullFaceEnabled = gl.glIsEnabled(GLenum.GL_CULL_FACE);
//        glGetIntegerv(GL_CULL_FACE_MODE, &(gStateBlock.cullMode));

        gStateBlock.vboBound = gl.glGetInteger(GLenum.GL_ARRAY_BUFFER_BINDING);
        gStateBlock.iboBound = gl.glGetInteger(GLenum.GL_ELEMENT_ARRAY_BUFFER_BINDING);
        gStateBlock.texBound = gl.glGetInteger(GLenum.GL_TEXTURE_BINDING_2D);
        gStateBlock.texActive = gl.glGetInteger(GLenum.GL_ACTIVE_TEXTURE);

//        TestPrintGLError("Error 0x%x in SaveState @ end...\n");
    }
    
    static void restoreGLState(){
    	int i;
        GLFuncProvider gl =GLFuncProviderFactory.getGLFuncProvider();
        // !!!!TBD TODO probably should ensure we can do this still... wasn't before though.
//        nv_flush_tracked_attribs(); // turn ours off...

//        TestPrintGLError("Error 0x%x in RestoreState @ start...\n");

        gl.glUseProgram(gStateBlock.programBound);

        // set buffers first, in case attribs bound to them...
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, gStateBlock.vboBound);
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, gStateBlock.iboBound);

        if (gStateBlock.programBound != 0)
        { // restore program stuff..
//            for (i=0; i<SAVED_ATTRIBS_MAX; i++)
//            {
//                if (gStateBlock.attrib[i].enabled) // only restore enabled ones.. ;)
//                {
//                    gl.glVertexAttribPointer(i,
//                                        gStateBlock.attrib[i].size,
//                                        gStateBlock.attrib[i].type,
//                                        (gStateBlock.attrib[i].norm != 0),
//                                        gStateBlock.attrib[i].stride,
//                                        gStateBlock.attrib[i].ptr);
//                    gl.glEnableVertexAttribArray(i);
//               }
//               else
//                    gl.glDisableVertexAttribArray(i);
//            }
        }
        
        if (gStateBlock.depthMaskEnabled)
            gl.glDepthMask(true); // we turned off.
        if (gStateBlock.depthTestEnabled)
            gl.glEnable(GLenum.GL_DEPTH_TEST); // we turned off.
        if (!gStateBlock.blendEnabled)
            gl.glDisable(GLenum.GL_BLEND); // we turned ON.
        if (gStateBlock.cullFaceEnabled)
            gl.glEnable(GLenum.GL_CULL_FACE); // we turned off.
//        glGetIntegerv(GL_CULL_FACE_MODE, &(gStateBlock.cullMode));

        // restore tex BEFORE switching active state...
        gl.glBindTexture(GLenum.GL_TEXTURE_2D, gStateBlock.texBound);
        if (gStateBlock.texActive != GLenum.GL_TEXTURE0)
            gl.glActiveTexture(gStateBlock.texActive); // we set to 0

//        TestPrintGLError("Error 0x%x in RestoreState @ end...\n");
    }
    
    private static int glGetVertexAttribiv(int index, int pname){
    	return GLFuncProviderFactory.getGLFuncProvider().glGetVertexAttribi(index, pname);
    }
    
    private static ByteBuffer wrap(BFVert[] data, int length){
    	ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(4 * 5 * length);
    	for(int i = 0; i < length; i++){
    		BFVert v = data[i];
    		buffer.putFloat(v.posX).putFloat(v.posY);
    		buffer.putFloat(v.uvX).putFloat(v.uvY);
    		buffer.putInt(v.color);
    	}
    	
    	buffer.flip();
    	return buffer;
    }
    
    private static final class BFVert{
    	float posX, posY; // 8 or 12 bytes, depending on how we add Z support. !!!!TBD
    	float uvX, uvY; // 8 bytes => 16 or 20 total.
    	int color;     // packed 4 byte color => 20 or 24 total.  ABGR with A << 24...
    }
    
    private static final int SAVED_ATTRIBS_MAX = 16;
    
//    private static final class AttribInfo{
//    	boolean enabled;
//        int size;
//        int stride;
//        int type;
//        int norm;
//        int ptr_size;
//        ByteBuffer ptr;
//    }

    private static final class StateBlock{
    	int               programBound;
//        final AttribInfo[]  attrib = new AttribInfo[SAVED_ATTRIBS_MAX];

        boolean           depthMaskEnabled;    
        boolean           depthTestEnabled;
        boolean           cullFaceEnabled;
        boolean           blendEnabled;
        //gStateBlock.blendFunc // !!!!TBD
        //blendFuncSep // tbd
        
        int               vboBound;
        int               iboBound;
        int               texBound;
        int               texActive;
    }
    
    private static final StateBlock gStateBlock = new StateBlock();
    private static boolean gSaveRestoreState = false; // default was true, let's try false.
}

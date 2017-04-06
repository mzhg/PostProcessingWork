//----------------------------------------------------------------------------------
// File:        NvUIText.java
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

import jet.opengl.postprocessing.util.Numeric;

/**
 * A UI element that renders text strings to the view.<p>
 * This class wrappers rendering of a string to the screen using {@link NvBitFont}.<p>
 * It exposes all the matching functionality, including font, size, color,
 * drop-shadow, alignment 'text box', and multi-line output support.
 * @author Nvidia 2014-9-9 20:22
 * @see {@link NvBitFont}
 * @see {@link NvBFText}
 */
public class NvUIText extends NvUIElement {

	private static final String[][] fontFiles = {
		{"RobotoCondensed-Regular-24.fnt", "RobotoCondensed-Bold-24.fnt"},
	    {"Courier-24.fnt", "Courier-Bold-24.fnt"},
	};
	
	private static boolean ready = false;
	
	/** The default/canonical shadow offset value. */
	protected static final byte DEFAULT_SHADOW_OFFSET = 3;
	/** The NvBitFont BFText object that does actual text rendering. */
	protected NvBFText m_bftext;
	/** Local cache of the original font size. */
	protected float m_size;
	/** Modulation of the text with an RGB color */
	protected int m_color;
	/** Whether we wrap or truncate if exceed drawable width. */
	protected boolean m_wrap;
	
	/**
	 * Default constructor for onscreen text element.
	 * @param str Text string to display.
	 * @param font Font family to use for this text.
	 * @param size Font size to use for this text.
	 * @param halign Alignment to use for this text.
	 */
	public NvUIText(String str, int font, float size, int halign){
		m_size = size;
		
		m_bftext = new NvBFText();
		
		setColor(Numeric.NV_PC_PREDEF_WHITE); // white for modulated color == no change.
		
		// !!!!TBD how to correlate font enum to font index?
	    byte fontID = getFontID(font);
	    m_bftext.setFont(fontID);
	    m_bftext.setSize(size);
	    m_bftext.setString(str);

	    setAlignment(halign);
	    
	    m_bftext.rebuildCache(false); // so we can get the width!    
	    setDimensions(m_bftext.getWidth(), m_bftext.getHeight());
	}
	
	/** Static helper method to get the specific font ID for a given font family enum. */
	public static byte getFontID(int font){
		if (font >= NvUIFontFamily.COUNT)
	        font = NvUIFontFamily.DEFAULT;
	    return NvBitFont.getFontID(fontFiles[font][0]);
	}
	
	/** Static helper method for initializing underlying text system with current view size. */
	public static boolean staticInit(float width, float height){
		if (!ready)
	    {
	        //int32_t i=0;
	        if (NvBitFont.initialize(NvUIFontFamily.COUNT, fontFiles) != 0)
	            return false;        
	            
	        // then set up size, orientation...
	        
	        // these two are now BF defaults, no need to set.
	        //NvBFSetOrthoMode(kOrthoTL00);
	        //NvBFSetVGANormalized(0); // disable the normalizations.

	        // until people are calling this or the ResChanged function,
	        // need to still call explicitly or some apps won't have text
	        // res set at all...
	        NvBitFont.setScreenRes(width, height);
	        
	        ready = true;
	    }
	   
	    return true;
	}
	
	/** Static helper method for initializing underlying text system with view size of the (1280, 720). */
	public static boolean staticInit(){
		return staticInit(1280, 720);
	}
	
	/** Static helper method for cleaning up any static held items in the text system. */
    public static void staticCleanup(){
        NvBitFont.cleanup();
        NvUIGraphic.staticCleanup();
        NvUIGraphicFrame.staticCleanup();
        NvUITexture.staticCleanup();

        ready = false;
    }
    
    /** Set the horizontal alignment of the text. */
    public void setAlignment(int halign){
    	m_bftext.setCursorAlign(halign, NvUITextAlign.TOP);
    }
    
    /** Set the alpha transparency of the text. */
    public void setAlpha(float alpha){
    	super.setAlpha(alpha);
    	
    	m_color = Numeric.setAlpha(m_color, (int)(alpha * 255)); // TODO Fixed.
    	setColor(m_color);
    }
    
    /** Set the overall color of the text. */
    public void setColor(int color){
    	m_color = color;
    	m_bftext.setColor(color);
    }
    
    /**
     * Enable a drop-shadow visual under the text string.
     * @param offset A positive (down+right) or negative (up+left) pixel offset for shadow.  Default is +2 pixels.  Set 0 to disable.
     * @param color An NvPackedColor for what color the drop-shadow should be.  Default is black.
     */
    public void setShadow(byte offset, int color){
    	m_bftext.setShadow(offset, color);
    }
    
    /**
     * Enable a drop-shadow visual under the text string with shadow offset 3 and black color.
     */
    public void setShadow(){
    	setShadow(DEFAULT_SHADOW_OFFSET, Numeric.NV_PC_PREDEF_BLACK);
    }
    
    /**
     * Assign a box the text will render within.<p>
     * The box will be used to align, wrap, and crop the text.<ul>
     * <li>If <i>lines</i> is 1, only a single line will be drawn, cropped at the box width.
     * <li>If <i>lines</i> is 0, we will wrap and never crop.</li>
     * <li>If <i>lines</i> is >1, we will wrap for that many lines, and crop on the final line.</li>
     * <li>If <i>dots</i> is a valid character, when we crop we won't simply truncate, will
     * will back up enough to fit in three of the @p dots character as ellipses.</li>
     * <li>When a box is active, all alignment settings on the text become relative to the
     * box rather than the view.</li>
     * <li>If width or height is zero, the box will be cleared/disabled.</li>
     * </ul>
     * @param width Set the width of the box
     * @param height Set the height of the box
     * @param lines Set the number of vertical lines allowed for the text
     * @param dots Set the character to use as ellipses
     */
    public void setTextBox(float width, float height, int lines, int dots){
    	float w,h;

        w = width; // just to store locally, for hittest, etc.
        if (height>0)
            h = height;
        else
            h = m_size; // seems appropriate? !!!!TBD
        
        m_wrap = false;
        if (lines>1)
            m_wrap = true;

        m_bftext.setBox(w, h, lines, dots);
        
        // update our internal dimensions?  yes, for now. !!!!TBD
        setDimensions(w, h); // ends up calling UpdateBox, c'est la vie.
    }
    
    /** Override so if a box has been set, we update the width and height with these values. */
    public void setDimensions(float w, float h){
    	super.setDimensions(w, h);
    	
    	m_bftext.updateBox(w, h);
    }

    /** Make proper calls to the text rendering system to draw our text to the viewport. */
    public void draw( NvUIDrawState drawState) // leaf, needs to implement!
    {
    	if (m_isVisible)
        {
            float myAlpha = m_alpha;
            if (drawState.alpha != 1.0f)
            {
                myAlpha *= drawState.alpha;
                int col = m_color;
//                NV_PC_SET_ALPHA(col, (uint8_t)(myAlpha * 255));
                col = Numeric.setAlpha(col, (int)(myAlpha * 255)); // TODO FIXED.
                m_bftext.setColor(col);
            }

            m_bftext.renderPrep();
            
            m_bftext.setCursorPos(m_rect.left, m_rect.top);

            m_bftext.render();
            
            m_bftext.renderDone();
        }
    }

    /** Set the string to be drawn. */
    public void setString(String in){
    	m_bftext.setString(in);
    }
    
    /** Set the font size to use for our text. */
    public void setFontSize(float size){
    	m_bftext.setSize(size);
    }
    
    /** Get our font size value. */
    public float getFontSize() { return m_size; }

//    virtual void GetString(char *string, uint32_t buflen); // copies out to a buffer.

    /** Get the approximate output pixel width calculated for our text. */
    public float getStringPixelWidth(){
    	// call RebuildCache to guarantee we're cached and have width.
    	m_bftext.rebuildCache(false);
    	return m_bftext.getWidth();
    }

    /** Set an explicit character count truncation for drawing our text.
        This is handy helper function for doing simple 'type on' animated visuals. */
    public void setDrawnChars(int count){
    	m_bftext.setDrawnChars(count);
    }
}

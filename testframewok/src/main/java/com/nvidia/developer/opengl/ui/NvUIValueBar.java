//----------------------------------------------------------------------------------
// File:        NvUIValueBar.java
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

public class NvUIValueBar extends NvUIElement{

	/** The current value/progress of the bar. */
	protected float m_value;
	/** The minimum value/progress of the bar. */
	protected float m_maxValue;
	/** The maximum value/progress of the bar. */
	protected float m_minValue;
	/** Whether or not the bar should snap/round its value to an integer. */
	protected boolean m_integral;
	/** leave frame bordering set as passed in, don't try to be smart. */
	protected boolean m_useRawBorders;
	/** Flip x or y axis fill 'direction'. */
	protected boolean m_flipDirection;
	/** The NvUIGraphicFrame used for empty value/progress background. */
	protected NvUIGraphicFrame m_emptyFrame;
	/** The NvUIGraphicFrame used for the filled value/progress state indication. */
	protected NvUIGraphicFrame m_fullFrame;
	
	/**
	 * Default constructor which takes 'empty' and 'full' versions of a visual bar of some sort, overlaid to show it 'filling up'.
	 * @param emptybar The NvUIGraphicFrame visual for the empty/unfilled bar background
	 * @param fullbar The NvUIGraphicFrame visual for the filled bar overlay
	 * @param useRawBorders Whether to try to auto-set borders, or use the raw borders already set on the frame.  Defaults to false/auto.
	 * @param flipDir Whether to reverse the 'fill' direction (normally left->right or top->bottom).  Defaults false.
	 */
	public NvUIValueBar(NvUIGraphicFrame emptybar, NvUIGraphicFrame fullbar,
            boolean useRawBorders, boolean flipDir) {
		m_emptyFrame = emptybar;
		m_fullFrame = fullbar;
		m_flipDirection = flipDir;
		m_minValue = 0;
		m_maxValue = 100;
		m_useRawBorders = useRawBorders;
		
		NvUIRect rect = new NvUIRect();
		
		if (m_emptyFrame != null)
	        m_emptyFrame.getScreenRect(rect);
	    else if (m_fullFrame != null)
	        m_fullFrame.getScreenRect(rect);
	    if (m_emptyFrame != null || m_fullFrame != null)
	        setDimensions(rect.width, rect.height);
	}
	
	@Override
	public void dispose() {
		if (m_emptyFrame != null)
			m_emptyFrame.dispose();
	    if (m_fullFrame != null)
	    	m_fullFrame.dispose();
	    m_emptyFrame = null;
	    m_fullFrame = null;
	}
	
	/** Override to proxy dimensions to inactive bar frame, including auto-border sizing if set. */
    public void setDimensions(float w, float h){
    	super.setDimensions(w, h);
        if (m_emptyFrame != null) {
            m_emptyFrame.setDimensions(w, h);

            if (!m_useRawBorders)
            {
                float thick;
                if (w>=h) // horizontal, use half height
                    thick = h/2;
                else // vertical, use half width
                    thick = w/2;
                m_emptyFrame.setBorderThickness(thick);
            }
        }

        updateBar();
    }
    
    /** Override to proxy origin setting to our frames. */
    public void setOrigin(float x, float y){
    	super.setOrigin(x, y);
    	
    	if (m_emptyFrame != null)
            m_emptyFrame.setOrigin(x, y);
        if (m_fullFrame != null)
            m_fullFrame.setOrigin(x, y);
            
        updateBar();
    }

    /** Flag whether we represent integer or floating point values. */
    public void setIntegral(boolean isint){
    	m_integral = isint;
    }
    
    /** Get whether we represent integer or floating point values. */
    public boolean getIntegral(){
    	return m_integral;
    }

    /** Override to proxy alpha setting to our frames. */
    public void setAlpha(float alpha){
    	super.setAlpha(alpha);
    	
    	if (m_fullFrame != null)
            m_fullFrame.setAlpha(alpha);
        if (m_emptyFrame != null)
            m_emptyFrame.setAlpha(alpha);
    }

    /** Accessor to set current value for the bar. */
    public void setValue(float value){
    	if (value < m_minValue) value = m_minValue;
        else if (value > m_maxValue) value = m_maxValue;
        if (m_integral)
        {
            if (value < 0)
                value =  (int) (value-0.5f);
            else
                value =  (int) (value+0.5f);
        }
        if (m_value == value) return;

        m_value = value;
        updateBar();
    }
    
    /** Accessor to set maximum value visualized by the bar. */
    public void setMaxValue(float value){
    	if (value == m_maxValue) return;

        m_maxValue = value;
        setValue(m_value);
        updateBar();
    }
    
    /** Accessor to set minimum value visualized by the bar. */
    public void setMinValue(float value){
    	if (value == m_minValue) return;

        m_minValue = value;
        setValue(m_value);
        updateBar();
    }

    /** Accessor to get current value for the bar. */
    public float getValue(){
    	return m_value;
    }
    
    /** Accessor to get maximum value visualized by the bar. */
    public float getMaxValue(){
    	return m_maxValue;
    }
    
    /** Accessor to get minimum value visualized by the bar. */
    public float getMinValue(){
    	return m_minValue;
    }
    
    //NvUIGraphicFrame *GetActiveFrame() { return m_active; };
    
    /** Must override to proxy drawing to our two frames. */
	@Override
	public void draw(NvUIDrawState drawState) {
		if (!m_isVisible) return;

	    if (m_emptyFrame != null)
	        m_emptyFrame.draw(drawState);
	    if (m_fullFrame != null)
	        m_fullFrame.draw(drawState);
	}
	
	/** Update the filled bar sizing based on current/min/max values, and rect of the empty bar. */
	protected void updateBar(){
		if (m_fullFrame == null) return;
	    float valuePixels;
	    float size;
	    
	    float percent = (m_value - m_minValue) / (m_maxValue - m_minValue);
	    if (m_rect.width>=m_rect.height)
	    { // horizontal bar
	        valuePixels = m_rect.width*percent;

	        if (!m_useRawBorders)
	            m_fullFrame.setBorderThickness(m_rect.height/2);

	        // Avoid artifacts at the near-zero progress value
//	        m_fullFrame.getBorderThickness(&size, NULL);
	        size = m_fullFrame.getBorderThickness().x;
	        if (valuePixels < size*2)
	            valuePixels = size*2;

	        m_fullFrame.setDimensions(valuePixels, m_rect.height);
	        if (m_flipDirection)
	            m_fullFrame.setOrigin(m_rect.left+(m_rect.width-valuePixels), m_rect.top);
	        else
	            m_fullFrame.setOrigin(m_rect.left, m_rect.top);
	    }
	    else
	    { // vertical bar.
	        valuePixels = m_rect.height*percent;

	        // Avoid artifacts at the near-zero progress value
	        if (!m_useRawBorders)
	            m_fullFrame.setBorderThickness(m_rect.width/2);

	        // Avoid artifacts at the near-zero progress value
//	        m_fullFrame->GetBorderThickness(NULL, &size);
	        size = m_fullFrame.getBorderThickness().y;
	        if (valuePixels < size*2)
	            valuePixels = size*2;

	        m_fullFrame.setDimensions(m_rect.width, valuePixels);
	        if (m_flipDirection)
	            m_fullFrame.setOrigin(m_rect.left, m_rect.top+(m_rect.height-valuePixels));
	        else
	            m_fullFrame.setOrigin(m_rect.left, m_rect.top);
	    }
	}
}

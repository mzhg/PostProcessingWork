//----------------------------------------------------------------------------------
// File:        NvUISlider.java
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

public class NvUISlider extends NvUIValueBar {

	/** The graphic drawn over the NvUIValueBar as the draggable 'thumb' piece. */
	protected NvUIGraphic m_thumb;
	/**
	 * Used by {@link #handleEvent(NvGestureEvent, long, NvUIElement)} and
	 * internal hit-tracking. Set when we had a successful press/click in the
	 * thumb, so we are then tracking for drag/flick/release.
	 */
	protected boolean m_wasHit;
	/**
	 * The thumb 'value' cached at the start of a drag, in order to reset back
	 * on a 'failed' drag interaction.
	 */
	protected float m_startValue;
	/**
	 * The amount to step the value at a time while dragging the thumb -- 'tick
	 * marks'.
	 */
	protected float m_stepValue;
	/**
	 * The NvUIReaction code 'posted' by the event handler when the thumb is
	 * released in a valid position.
	 */
	protected int m_action;
	/**
	 * If set true, this causes constant reactions during live dragging of the
	 * thumb. Defaults false.
	 */
	protected boolean m_smooth;
	
	/** Extra horizontal hit margin for the thumb button. */
	private float m_hitMarginWide;
	/** Extra vertical hit margin for the thumb button. */
	private float m_hitMarginTall;

	/**
	 * Default constructor specializing on NvUIValueBar to make an interactive slider.
     * Adds an interactive 'thumb' on top of a value bar to allow the user to manually adjust its value.
	 * @param emptybar The NvUIGraphicFrame visual for the empty/unfilled bar background
	 * @param fullbar The NvUIGraphicFrame visual for the filled bar overlay
	 * @param thumb An NvUIGraphic for the draggable 'thumb' button that sits over the bar.
	 * @param actionCode The action code sent in a reaction when there has been a value change.
	 */
	public NvUISlider(NvUIGraphicFrame emptybar, NvUIGraphicFrame fullbar,
                      NvUIGraphic thumb, int actionCode) {
		super(emptybar, fullbar, false, false);
		
		m_thumb = thumb;
		m_action = actionCode;
		
		positionThumb();
	}
	
	@Override
	public void dispose() {
		if(m_thumb != null)
			m_thumb.dispose();
	}
	
	/** Override for specialized input handling of dragging the thumb on the bar and generating reactions. */
    public int handleEvent(NvGestureEvent gdata, long timeUST, NvUIElement hasInteract) // interactive, must override
    {
    	if (!m_isVisible) return nvuiEventNotHandled;

        // verify we've been hit..
        boolean hit = false;
        
        if (gdata.kind==NvGestureKind.PRESS)
        {
            NvUIRect tr = m_thumb.getScreenRect();
            hit = tr.inside(gdata.x, gdata.y, m_hitMarginWide, m_hitMarginTall);
            m_wasHit = hit; // first press, stash
            m_startValue = getValue();
        }
        else
        {
            if (!m_wasHit) // if initial press wasn't in the thumb, nothing to do.
                return nvuiEventNotHandled;
                
            if (gdata.kind == NvGestureKind.DRAG
            ||  (gdata.kind & NvGestureKind.MASK_RELEASED) != 0) // then we switch to alt rect for hysterisis (sp?)
                hit = m_rect.inside((float)(gdata.x+gdata.dx),
                                    (float)(gdata.y+gdata.dy), m_hitMarginWide, m_hitMarginTall);
        }
                
        if (hit)
        {
        }
        else
        {
            // !!!!TBD at some point, if released WELL outside of bounds rect, ignore.
            //m_wasHit = false;
        }                


        // move thumb if dragging
        if (gdata.kind == NvGestureKind.DRAG)
        {
            float newoff = m_startValue;
            NvUIRect br = getScreenRect();

            float len = (m_maxValue - m_minValue);
            if (br.width>=br.height) //horizontal, just use dx
                newoff += (gdata.dx/br.width)* len;
            else // vertical
                newoff += (gdata.dy/br.height) * len;
            setValueReal(newoff);

            if (m_smooth)
            {
                NvUIReaction react = getReactionEdit(true);
                react.uid = m_uiuid;
                react.code = m_action;
                // pass along value as adjusted by SetValueReal...
                react.ival = (int)m_value;
                react.fval = m_value; 
                return nvuiEventHandledReaction;
            }
        }

        // if we came this far, we're hit.  are we done?
        if ((gdata.kind & NvGestureKind.MASK_RELEASED) != 0 )// we're done?
        {
            if (m_wasHit)
            {
                // leave value/thumb in last good position
                //Trigger();
                // broadcast value change
                m_wasHit = false;

                NvUIReaction react = getReactionEdit(true);
                react.uid = m_uiuid;
                react.code = m_action;
                // pass along value as adjusted by SetValueReal...
                react.ival = (int)m_value;
                react.fval = m_value; 
                return nvuiEventHandledReaction;
            }
            else // put everything back
            {
                setValueReal(m_startValue);
                m_startValue = 0;
            }
            
            return nvuiEventHandled;
        }
        if (m_wasHit || this==hasInteract) // keep Interact.
            return nvuiEventHandledInteract;
        
        return nvuiEventNotHandled;
    }
    
    /** Override for handling someone other than use send a reaction with our code as a way to pass along value changes. */
    public int handleReaction(NvUIReaction react) // interactive, must override
    {
    	//NvUIEventResponse r = INHERITED.HandleReaction(react);
        // look specifically for a case where a UID >other< than ours
        // is sending our reaction code, for us to update our value.
        // NOTE: integral values should be in state, float in val.
        if ((react.uid!=getUID())
            && (m_action != 0 && (react.code==m_action)))
        {
            if (m_integral) // uses integer value.
                setValue(react.ival);
            else // uses the float value
                setValue(react.fval);
        }
        return nvuiEventNotHandled;
    }
    
    /** Override for handling INC/DEC action events when bar has focus. */
    public int handleFocusEvent(int evt){
    	final int rno = nvuiEventNotHandled;
        if (!getVisibility()) return rno;

        // we only handle INC/DEC focus actions for right now.
        if (evt!=NvFocusEvent.ACT_INC && evt!=NvFocusEvent.ACT_DEC) return rno;

        int st = getDrawState();
        // we don't handle actions if we're inactive.
        if (st == NvUIButtonState.INACTIVE) return rno;

        float dval = 1;
        if (evt==NvFocusEvent.ACT_DEC) dval = -1;
        if (m_stepValue != 0) dval *= m_stepValue;
        setValueReal(m_value + dval);

        NvUIReaction react = getReactionEdit(true);
        react.uid = m_uiuid;
        react.code = m_action;
        // pass along value as adjusted by SetValueReal...
        react.ival = (int)m_value;
        react.fval = m_value; 
        return nvuiEventHandledReaction;
    }

    @Override
    public void setDimensions(float w, float h){
    	super.setDimensions(w, h);
    	
    	positionThumb();
    }
    
    /** Override to handle proxying to thumb element. */
    public void setOrigin(float x, float y){
    	super.setOrigin(x, y);
    	
    	positionThumb();
    }
    
    /** Override to handle proxying to thumb element. */
    public void setAlpha(float a){
    	super.setAlpha(a);
    	
    	m_thumb.setAlpha(a);
    }

    /** Override to have better control over when value really changes, when visual elements update. */
    public void setValue(float value){
    	if (!m_wasHit)
        {
            // Only update slider if we aren't sliding it manually (by sliding the thumb)
            setValueReal(value);
        }
    }

    /** Add extra horizontal/vertical margin to thumb hit rect to make it easier to grab. */
    public void setHitMargin(float hitwide, float hittall){
    	m_hitMarginWide = hitwide;
        m_hitMarginTall = hittall;
    }

    /** Accessor to retrieve the UI-space NvUIRect for this element's focus rectangle. */
    public void getFocusRect(NvUIRect rect){
    	NvUIRect tr = m_thumb.getScreenRect();
    	getScreenRect(rect);
    	// grow to somewhat encapsulate the thumb
        rect.grow(tr.width/2, tr.height - rect.height*2); // grow width a small amount, height the delta of the two elements.
    }

    /** Override to handle drawing thumb element over base valuebar. */
    public void draw(NvUIDrawState drawState){
    	if (!m_isVisible) return;

        super.draw(drawState);
        m_thumb.draw(drawState);
    }

    /** Set true to enable posting of reaction during movement of the thumb.<p>

        When disabled/false, we only post reaction when the thumb is released
        in its final position. When true, we update constantly while the thumb
        is being dragged.  The default is false/disabled.
    */
    public void setSmoothScrolling(boolean smooth) { m_smooth = smooth; }

    /** Set incremental 'steps' for value changes, like 'tick marks' on a ruler.
        This enforces a given stepping of values along the min/max range.  Most
        helpful when the range is large, but you want specific 'stop' points.
    */
    public void setStepValue(float step) { m_stepValue = step; }

    /** Returns the action code we use when raising reaction to user interaction. */
    public int getActionCode() { return m_action; }
    
    /** Update the position of the thumb based on current/min/max values. */
    private void positionThumb(){
    	if(m_thumb == null)
    		return;
    	
    	NvUIRect tr = m_thumb.getScreenRect();
    	NvUIRect br = getScreenRect();
    	
    	float len = m_maxValue - m_minValue;
        float percent = 0;
        if (len > 0)
            percent = (getValue()-m_minValue) / len;

        if (br.width>=br.height) //horizontal
            m_thumb.setOrigin(
                br.left - (tr.width/2) + (br.width*percent),
                br.top - (tr.height/2) + (br.height/2) );
        else //vertical
            m_thumb.setOrigin(
                br.left - (tr.width/2) + (br.width/2),
                br.top - (tr.height/2) + (br.height*percent) );
    }
    
    // This function does the real work. This is because
    // when we are sliding the slider manually, we don't
    // want "external" SetValues to influence the position
    // of the thumb.
    /** Call inherited {@link #setValue} method, update our thumb, and optionally update linked text string. */
    private void setValueReal(float value){
    	super.setValue(value);
    	
    	// don't ever early-exit here, as we always need this to check
        // value and update thumb... we can NEVER assume
        // those things don't need updating.

        if (m_stepValue != 0)
        {
            // try to hit step points.
            float tmpval = m_value - m_minValue;
            int tmpinc = (int)((tmpval / m_stepValue) + 0.5f); // rounded number of increments.
            tmpval = m_minValue + (tmpinc*m_stepValue);
            if (tmpval != m_value) { // we modified..
                m_value = tmpval;
                updateBar(); // since we changed, need to re-update.
            }
        }

        positionThumb();
    }

}

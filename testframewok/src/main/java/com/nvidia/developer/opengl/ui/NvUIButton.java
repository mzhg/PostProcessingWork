//----------------------------------------------------------------------------------
// File:        NvUIButton.java
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

import com.nvidia.developer.opengl.utils.NvUtils;

import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.postprocessing.util.StringUtils;

/**
 * A class that implements a variety of standard interactive button widgets.<p>
 * This class uses abstract visual representations of the three button states,
 * combined with the logic behind the types of interactions of the three types of
 * buttons, and the concept in the system of a 'reaction' to interacting with a
 * button, and wraps it all up with end-to-end event handling.<p>
 * This can represent a single graphical or textual (or other) element on the
 * screen that is clickable.<p>
 * While tracking a press within the button, the {@link #handleEvent} method will attempt
 * to keep the focus on the button, so that it will get 'first shot' at further
 * event input in the system.
 * @author Nvidia 2014-9-10 11:04
 * @see {@link NvUIElement}
 * @see {@link NvUIReaction}
 * @see {@link NvUIEventResponse}
 */
public class NvUIButton extends NvUIElement{

	/** Abstract UI element array of visual representations we proxy as our button visual.  Must have at least the 'active' state defined.  Can be text, graphics, complex container, etc. */
	protected final NvUIElement[] m_visrep = new NvUIElement[NvUIButtonState.MAX];
	/** UIText-drawn title for a button, if one isn't baked into the graphic rep. */
	protected NvUIText m_title;
	/** The 'class' of button (which defines its 'style' of interaction): push, radio, or checkbox. */
	protected int m_type;
	/** The reaction code 'posted' by the event handler when the button is pressed and released such that state changed.  Radio buttons use this to both notify change up the tree AND to notify 'linked' alternate state buttons to turn themselves back 'off'.*/
	protected int m_action;
	/** A specialized reaction code used by some kinds of buttons (radio in particular) to denote which sub-item was picked by index. */
	protected int m_subcode;
	/** A gesture event UID of the last gesture that we triggered/reacted to, so we can quickly ignore further {@link #handleEvent} calls until the gesture UID increments. */
	protected int m_reactGestureUID;
	/** A gesture event UID of the last gesture that failed to hit the button, so we can quickly ignore further {@link #handleEvent} calls until the gesture UID increments. */
	protected int m_failedHitUID;
	/** Used by {@link #handleEvent} and internal hit-tracking.  Set when we had a successful press/click in the thumb, so we are then tracking for drag/flick/release. */
	protected boolean m_wasHit;
	/** An extra 'padding' or margin added to the button's normal hit-test width, in order to allow for a larger test region than the button's visual.  Important for touchscreen+finger interactivity. */
    protected float m_hitMarginWide;
    /** An extra 'padding' or margin added to the button's normal hit-test height, in order to allow for a larger test region than the button's visual.  Important for touchscreen+finger interactivity. */
    protected float m_hitMarginTall;
    /** Whether to retain the pressed-in state even if we drag outside bounds. */
    protected boolean m_stickyClick;
    
//    #ifdef BTN_SUPPORTS_HOVER
    private boolean m_wantsHover;
    private boolean m_wasHover;
//    #endif
    
    /**
     * Most common constructor for a graphical, multi-state visual button.<p>
     * This is the generally 'best' method for building a visual button, as you give it the
     * specific visual elements built for each state (active, pressed, inactive), and thus
     * have very precise control over the visual results of interaction.  Usually the visuals
     * are an NvUIGraphic or NvUIGraphicFrame, but they can be any UI element (including
     * containers with complex overlaid visuals, or custom visual subclasses).
     * @param btntype The type/style of button
     * @param actionCode The action code we raise in an NvUIReaction when our state changes
     * @param visrep Array of UI elements that are our visuals for each interaction state, could have text embedded.
     * @param title <b>[</b>optional<b>]</b> String to display for our title, default is null.
     * @param pt <b>[</b>optional<b>]</b> Font size to use if we supply title string, default is 0.
     * @param shadowed <b>[</b>optional<b>]</b> Whether to drop-shadow our title text, default is false.
     * @see {@link NvUIGraphic}
     */
    public NvUIButton(int btntype, int actionCode, NvUIElement[] visrep, String title, float pt, boolean shadowed){
    	privateInit(btntype, actionCode);
    	
    	for (int c = 0; c< NvUIButtonState.MAX && c < visrep.length; c++)
            m_visrep[c] = visrep[c];

        if (visrep.length >= 2 && m_visrep[1] == null)
            setMaxDrawState(0); // just base
        else
        if (visrep.length >= 3 && m_visrep[2] == null)
            setMaxDrawState(1); // just base+pressed
        else
            setMaxDrawState(2); // if all three.
                
        if (m_visrep[0] != null) // grab the active visrep's rectangle.
            m_visrep[0].getScreenRect(m_rect);

        if(!StringUtils.isEmpty(title))
    		setTitle(title, pt, shadowed);
    }
    
    /**
     * Most common constructor for a graphical, multi-state visual button.<p>
     * This is the generally 'best' method for building a visual button, as you give it the
     * specific visual elements built for each state (active, pressed, inactive), and thus
     * have very precise control over the visual results of interaction.  Usually the visuals
     * are an NvUIGraphic or NvUIGraphicFrame, but they can be any UI element (including
     * containers with complex overlaid visuals, or custom visual subclasses).
     * @param btntype The type/style of button
     * @param actionCode The action code we raise in an NvUIReaction when our state changes
     * @param visrep Array of UI elements that are our visuals for each interaction state, could have text embedded.
     * @see {@link NvUIGraphic}
     */
    public NvUIButton(int btntype, int actionCode, NvUIElement[] visrep){
    	this(btntype, actionCode, visrep, null, 0, false);
    }
    
    /**
     * Button constructor for a quick, single-visual-state button.<p>
     * This button will not show different visuals while interacting, but otherwise has the
     * ability to display any graphics/text to display as a button.
     * @param btntype The type/style of button
     * @param actionCode The action code we raise in an NvUIReaction when our state changes
     * @param visrep The UI element that is our visual representation, could have text embedded.
     * @param title <b>[</b>optional<b>]</b> String to display for our title, default is null.
     * @param pt <b>[</b>optional<b>]</b> Font size to use if we supply title string, default is 0.
     * @param shadowed <b>[</b>optional<b>]</b> Whether to drop-shadow our title text, default is false.
     * @see {@link NvUIGraphic}
     */
    public NvUIButton(int btntype, int actionCode, NvUIElement visrep, String title, float pt, boolean shadowed){
    	privateInit(btntype, actionCode);
    	
    	m_visrep[0] = visrep;

        if (m_visrep[0] != null) // grab the active visrep's rectangle.
            m_visrep[0].getScreenRect(m_rect);

        if(!StringUtils.isEmpty(title))
    		setTitle(title, pt, shadowed);
    }
    
    /**
     * Button constructor for a quick, single-visual-state button.<p>
     * This button will not show different visuals while interacting, but otherwise has the
     * ability to display any graphics/text to display as a button.
     * @param btntype The type/style of button
     * @param actionCode The action code we raise in an NvUIReaction when our state changes
     * @param visrep The UI element that is our visual representation, could have text embedded.
     * @see {@link NvUIGraphic}
     */
    public NvUIButton(int btntype, int actionCode, NvUIElement visrep){
    	this(btntype, actionCode, visrep, null, 0, false);
    }
    
    /**
     * Button constructor for a quick, text-only button.
     * @param btntype The type/style of button
     * @param actionCode The action code we raise in an NvUIReaction when our state changes
     * @param size The UI/view space rectangle to position the button
     * @param title The string to display for our title
     * @param pt The font size to use for our title
     * @param shadowed <b>[</b>optional<b>]</b> Whether to drop-shadow our title text
     */
    public NvUIButton(int btntype, int actionCode, NvUIRect size, String title, float pt, boolean shadowed){
        privateInit(btntype, actionCode);
    	
    	m_rect.set(size);
    	
    	if(!StringUtils.isEmpty(title))
    		setTitle(title, pt, shadowed);
    }
    
    /**
     * Set the title string of the button.<p>
     * Is used by constructors to set up our text string, but can be used to
     * re-set the title later on.
     * @param title The string to display for our title
     * @param ptSize The font size to use for our title.  Note if point size is passed in as 0, a
     * size will be selected based on the height of the element.
     * @param shadowed <b>[</b>optional<b>]</b> Whether to drop-shadow our title text
     */
    public void setTitle(String title, float ptSize, boolean shadowed){
    	final boolean push = (m_type== NvUIButtonType.PUSH);
        if (ptSize==0)
        	ptSize = getHeight() * 0.75f; // some default...

        if (m_title==null)
        {
            m_title = new NvUIText(title, NvUIFontFamily.DEFAULT, ptSize,
                                    push ? NvUITextAlign.CENTER : NvUITextAlign.LEFT);
        }
        else
        { // already have one, so just call proper setter methods
            m_title.setString(title);
            m_title.setFontSize(ptSize);
            m_title.setAlignment(push ? NvUITextAlign.CENTER : NvUITextAlign.LEFT);
        }

        setTitleColor(Numeric.NV_PC_PREDEF_WHITE);

        if (shadowed)
            m_title.setShadow((byte)4, Numeric.NV_PC_PREDEF_BLACK); // a little deeper shadow than the default 2px.
        // for checkbox/radio, the width is totally wrong at this point.
        //m_title->SetTextBox(m_rect.width, m_title->GetFontSize(), 0, 0);
    }
    
    /** Set the color of the title text. */
    public void setTitleColor(int c){
    	m_title.setColor(c);
    }

    /** Add extra horizontal/vertical margin to thumb hit rect to make it easier to grab. */
    public void setHitMargin(float hitwide, float hittall){
    	m_hitMarginWide = hitwide;
        m_hitMarginTall = hittall;
    }

    /** Draw the right UI element for current state, as well as optional title text. */
    public void draw(NvUIDrawState drawState) // visual, must implement
    {
    	if (!m_isVisible) return;

        final int state = getDrawState();
        // eventually this will do some state-based handling for active/inactive, pushed
        NvUIElement drawme = m_visrep[state];
        if (drawme!=null) // pass to our visrep.
            drawme.draw(drawState);
        if (m_title != null)
        {
        	NvUIDrawState newState = new NvUIDrawState(drawState);
            // !!!!TBD TODO
            // ... instead of using alpha, we could draw some a translucent box using our uirect.
            if (m_visrep[0]== null && m_visrep[1]==null && m_visrep[2]==null)
            {
                // then modulate alpha based on draw state.
                if (state== NvUIButtonState.INACTIVE)
                {
                    newState.alpha *= 0.25f; // very 'dim'
                }
                else
                if (state== NvUIButtonState.SELECTED)
                {
                    newState.alpha *= 0.75f; // slightly 'dimmed'
                }
                // else leave alone.
            }

            m_title.draw(newState);
        }
    }
    
    /** Position sub-elements and optional title based on the button type. */
    public void setOrigin(float x, float y){
    	super.setOrigin(x, y);
    	
    	for (int c = 0; c< NvUIButtonState.MAX; c++)
            if (m_visrep[c] != null) // pass to our visrep.
                m_visrep[c].setOrigin(x, y);

        if (m_title != null) // !!!!TBD rough margin inset for the title -- might want to calc center?
        {
            // !!!TBD TODO different origin depending on text alignment desired.
            // checkboxes and radios with a space to the left will be left aligned inset
            // normal pushbutton styling will be centered with centered position.
            final boolean push = (m_type== NvUIButtonType.PUSH);
            if (push)
                m_title.setOrigin(x+(getWidth()/2), y+((getHeight() - m_title.getHeight())*0.2f)); 
            else
            {
                float xin = getHeight()*1.2f;
                if (m_visrep[0] != null)
                    xin = m_visrep[0].getWidth()*1.2f;
                m_title.setOrigin(x+xin, y+((getHeight() - m_title.getHeight())*0.3f));
            }
        }
    }

    /** Handles tracking from press to release on the button object, and if needed posts appropriate NvUIReaction. */
    public int handleEvent( NvGestureEvent gdata, long timeUST, NvUIElement hasInteract) // interactive, must override.
    {
    	boolean hit = false;
    	
    	if(!m_isVisible || getDrawState() == NvUIButtonState.INACTIVE || checkHover(gdata))
    		return nvuiEventNotHandled;
    	
    	boolean possibleSlideTarget = false;
        
        if (gdata.uid == m_reactGestureUID) // then we're done?
        {
//            NvLogger.i("early exit same event UID");
            if (this==hasInteract)
                return nvuiEventHandledInteract;
            else
                return nvuiEventHandled; // since we flagged the triggered UID...
        }

        // check if this is a valid, non-focused/non-active slide-focus target
        if (gdata.kind==NvGestureKind.DRAG    // is a drag/slide going on
        &&  getSlideInteractGroup() !=0       // we have a focus group ID
        &&  getSlideInteractGroup() == getActiveSlideInteractGroup()) // and our ID matches active ID
        {
            if (!m_wasHit       // if we aren't flagged as active/hit
            &&  hasInteract != null  // focus isn't null (early exit) -- note focus could be a higher container rather than another button.
        //        &&  this!=hasInteract  // aren't the focused element (sorta redundant check)
                )
            {
                possibleSlideTarget = true;
            }
        }
            
        if (!possibleSlideTarget
        &&  gdata.uid == m_failedHitUID)
        {
//        	NvLogger.i("early exit failed hit");
            return nvuiEventNotHandled;
        }
            
        // verify we've been hit..
        hit = m_rect.inside((float)(gdata.x+gdata.dx), (float)(gdata.y+gdata.dy),
                                m_hitMarginWide, m_hitMarginTall);
        //NvLogger.i("[event 0x%x] hit = %s, focus = 0x%x\n", (unsigned int)(gdata.uid), hit?"yes":"no", (uint32_t)hasInteract);
        
        if (possibleSlideTarget)
        { // use drag dx/dy for hit detection of CURRENT position, not gesture start loc.
            if (!hit)
            {
                m_wasHit = false;
                m_failedHitUID = gdata.uid; // so we don't retest.
//                NvLogger.i("!!!!!> slide target %s not hit\n", "NvUIButton");
                return nvuiEventNotHandled; // we're done.
            }
//            NvLogger.i("!!!!!> slide target %s hit\n", "NvUIButton");
            m_wasHit = true;                
            m_failedHitUID = 0; // so we retest.
            setPrevDrawState(getDrawState());
            
            // .. anything special here??
            // lostInteract on other element will happen in container above us.
        }
        else
        // !!!!!TBD
        // I'm getting MULTIPLE PRESS EVENTS!!!?!?!
        if (!m_wasHit && gdata.kind==NvGestureKind.PRESS)
        {
            if (!hit)
            {
                m_wasHit = false;
                m_failedHitUID = gdata.uid; // so we don't retest.
                return nvuiEventNotHandled; // we're done.
            }
            m_wasHit = true;                
//            NvLogger.i("!!!!!> not hit -> got hit\n");
            setPrevDrawState(getDrawState());
            if (getDrawState()==0 && getSlideInteractGroup() != 0) // we set the active group on press.
                setActiveSlideInteractGroup(getSlideInteractGroup());
            else
                setActiveSlideInteractGroup(0);
            if (possibleSlideTarget) // we are now!
            {
                // .. anything special here??
                // lostInteract on other element will happen in container above us.
            }
        }
        else
        if (!m_wasHit)
        {
//        	NvLogger.i("!!!!!> not hit -> not hit\n");
            if(NvUtils.later){
            // if we get here we:
            // weren't a valid slide target
            // weren't getting a press (above)
            // hadn't gotten a press yet (!wasHit)
            // hadn't ignored a press (uid exit would have hit above)
            if (gdata.kind== 0 /*NV_GESTURE_NONE*/)
            { // TODO -- I realize, this isn't great code.  trying to get something starter-ish in place.
                // already calc'd hit.
                if (hit)
                {
                    if (!m_wasHover)
                        setDrawState(1);
                    return nvuiEventHandledHover;
                }
                else //!hit
                {   
                    if (!m_wasHover)
                        setDrawState(0);
                }            
            }
        }
            return nvuiEventNotHandled;
        }
        else
        if (m_wasHit && gdata.kind==NvGestureKind.DRAG)
        {
            // we're dragging.  but might not be INSIDE/hit.
//        	NvLogger.i("> drag\n");
        }
        else
        if (
        //  gdata.kind==NV_GESTURE_FLICK ||
            (gdata.kind&NvGestureKind.MASK_RELEASED) != 0) // any release state!!!
        {
            // already calc'd hit.
//        	NvLogger.i("!!!!!> got release\n");
        }
        else
        if (m_wasHit || this==hasInteract) // keep focus.
        {
//            NvLogger.i("!!!!!> was hit, keep focus\n");
            return nvuiEventHandledInteract;
        }

//        NvLogger.i("!!!!!> secondary processing...\n");
          
        if (hit)
        {
//            NvLogger.i("}}}} was hit\n");
            if (m_type== NvUIButtonType.CHECK)
            { // !!!!TBD
                if (getPrevDrawState() == getDrawState()) // we haven't flipped yet
                {
                    if (getDrawState()== NvUIButtonState.ACTIVE)
                        setDrawState(NvUIButtonState.SELECTED);
                    else
                        setDrawState(NvUIButtonState.ACTIVE);
                }
            }
            else
                setDrawState(NvUIButtonState.SELECTED);
        }
        else
        if (!m_stickyClick)
        {
//            NvLogger.i("}}}} not hit\n");
            if (m_type!= NvUIButtonType.PUSH)
            { // !!!!TBD
                //if (m_prevDrawState != m_currDrawState) // we flipped, put BACK
                    setDrawStatePrev();
            }
            else
                setDrawState(NvUIButtonState.ACTIVE);
        }                
            
        // if we came this far, we're hit.  are we done?
        if ((gdata.kind & NvGestureKind.MASK_RELEASED) != 0) // we're done?
        {
//            NvLogger.i("}}}} got release !!! !!! !!!! !!! !!!\n");
            int r = nvuiEventHandled;

            if (hit) // on the release, what's our curr state??
            {
//                NvLogger.i("}}}}}} hit, any reaction?\n");
                if (gdata.uid != m_reactGestureUID)
                {
                    // !!!!TBD if a radio button, we really want to stash the
                    // state at PRESSED time, so that we know whether to send
                    // an event at all!
                    m_reactGestureUID = gdata.uid; // so we don't retrigger.
                    
                    NvUIReaction react = getReactionEdit(true);
                    react.uid = m_uiuid;
                    react.code = m_action;
                    react.state = getDrawState();
                    // in case someone is looking for a value for this reaction.
                    if (m_type== NvUIButtonType.RADIO)
                        react.ival = m_subcode;
                    else
                        react.ival = react.state; // pass draw state as value.
                    react.flags = NvReactFlag.NONE;
                    react.causeKind = gdata.kind;
                    react.causeIndex = gdata.index;
                    r = nvuiEventHandledInteractReaction; // KEEP FOCUS FOR NOW -- else maybe->tap issues !!!!TBD
//                    NvLogger.i("}}}}}} prepped reaction");
                }
            }

            // reset draw state if button type is Push
            // AFTER reaction, so we get state correct in the message.
            if (m_type== NvUIButtonType.PUSH)
                setDrawState(NvUIButtonState.ACTIVE);
            
            m_wasHit = false; // reset!
            return r;
        }        
        
        if (m_wasHit || this==hasInteract) // keep focus.
        {
            int r = nvuiEventWantsInteract;
            if (hit) {
                r = nvuiEventHandledInteract;
            } else {
//                NvLogger.i("} keep focus ftm..\n");
            }
            return r;
        }

        return nvuiEventNotHandled;
    }
    
    private boolean checkHover(NvGestureEvent gdata){
    	if(!NvUtils.BTN_SUPPORTS_HOVER){
    		return gdata.kind<=NvGestureKind.HOVER;
    	}else{
    		return (gdata.kind<NvGestureKind.HOVER) ||(gdata.kind==NvGestureKind.HOVER && !m_wantsHover);
    	}
    }
    
    /**
     * Handles any reaction matching our action code.<p>
     * If the action matches our code, but the uid is zero, we assume it's a 'system message'
     * telling us to override our state to what's in the reaction.  Otherwise, if we're a
     * radio button and receive our code, but not our uid, we assume another radio button got
     * pressed, and we clear our state back to active.
     */
    public int handleReaction(NvUIReaction react){
    	if (react.code == m_action) 
        {
            if (react.uid == 0) // sent from 'system' rather than a control.
            {
                if (getDrawState() != react.state )
                {
                    setDrawState(react.state);
                    setPrevDrawState(0);
                }
            }
            else
            if (m_type== NvUIButtonType.RADIO)
            {
                if (react.uid != m_uiuid) // unselect any other buttons with same actionCode
                {
                    if (getDrawState() == NvUIButtonState.SELECTED )
                    {
                        setDrawState(NvUIButtonState.ACTIVE);
                        setPrevDrawState(0);
                    }
                }
                else // we sent the reaction...
                {
                }
            }
        }

        return nvuiEventNotHandled; // !!!!TBD
    }
    
    /** Virtual method for moving the highlight focus between UI elements or acting upon the selected one. 
    @return true if we were able to move the focus or act on it, false otherwise. */
    @Override
    public int handleFocusEvent(int evt){
    	int r = nvuiEventNotHandled;
        if (!getVisibility()) return r;

        // we only handle PRESS focus actions for right now.
        if (evt!=NvFocusEvent.ACT_PRESS) return r;

        int st = getDrawState();
        // we don't handle actions if we're inactive.
        if (st == NvUIButtonState.INACTIVE) return r;
        
        if (m_type== NvUIButtonType.CHECK)
        { // then toggle.
            if (st == NvUIButtonState.ACTIVE)
                st = NvUIButtonState.SELECTED;
            else
                st = NvUIButtonState.ACTIVE;
            setDrawState(st);
        }
        else
        if (m_type== NvUIButtonType.RADIO)
        { // then select THIS radio
            st = NvUIButtonState.SELECTED;
            setDrawState(st);
        }
        else // push button
        { // fake that we're pushed in...
            st = NvUIButtonState.ACTIVE;
            // but don't set drawstate!
        }

        NvUIReaction react = getReactionEdit(true);
        react.uid = m_uiuid;
        react.code = m_action;
        react.state = st;
        // in case someone is looking for a value for this reaction.
        if (m_type== NvUIButtonType.RADIO)
            react.ival = m_subcode;
        else
            react.ival = react.state; // pass draw state as value.
        react.flags = NvReactFlag.NONE;
        r = nvuiEventHandledInteractReaction;
//        NvLogger.i("}}}}}} prepped reaction\n");

        return r;
    }
    
    /** We override to clear hit tracking state, and reset draw state for push buttons. */
    public void lostInteract(){

        if (m_wasHit)
        {
//        	NvLogger.i("[!] wasHit losing focus...");
            m_wasHit = false;
        }

        if (m_type!= NvUIButtonType.CHECK &&  m_type!= NvUIButtonType.RADIO)
        {
            setDrawState(getPrevDrawState());
            setPrevDrawState(0);
        }
        
        super.lostInteract();
    }
    
    /** Accessor to get the reaction action code for this button. */
    public int getActionCode() { return m_action; };

    /** Accessor to set the reaction sub-code for this button. */
    public void setSubCode(int c) { m_subcode = c; };
    /** Accessor to get the reaction sub-code for this button. */
    public int getSubCode() { return m_subcode; };

    /** Set whether to retain the pressed-in even if user drags outside the hit rect. */
    public void setStickyClick(boolean b) { m_stickyClick = b; };

    /** Allow us to flag that we reacted to this gesture, so we don't handle again until new gesture UID. */
    public void consumeGesture(NvGestureEvent ev) { m_reactGestureUID = ev.uid; }
    
    private void privateInit(int btntype, int actionCode){
    	m_type = btntype;
        m_action = actionCode;
        m_subcode = 0;
        m_stickyClick = false;
        m_reactGestureUID = NvGestureEvent.NV_GESTURE_UID_INVALID;
        m_wasHit = false;
        m_failedHitUID = NvGestureEvent.NV_GESTURE_UID_INVALID;
        m_hitMarginWide = 0;
        m_hitMarginTall = 0;
        m_slideInteractGroup = 0;
        
        if(NvUtils.BTN_SUPPORTS_HOVER){
        	m_wantsHover = false;
            m_wasHover = false;
        }
    }
}

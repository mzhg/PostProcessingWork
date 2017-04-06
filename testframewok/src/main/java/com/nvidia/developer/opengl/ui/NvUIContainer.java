//----------------------------------------------------------------------------------
// File:        NvUIContainer.java
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

/**
 * An object that holds and manages a group of UI elements and interactions with
 * them.
 * <p>
 * 
 * This class implements a 'container' or 'holder' object, that has no
 * particular visual of its own, rather 'proxying' its visual to the children
 * held within it. It does do custom handling for having a specific 'background'
 * visual attached to it (such as a graphic or a frame) to draw beneath the
 * children.
 * <p>
 * 
 * The main interface is the @p Add method, which allows you to generally add
 * any kind of UI element into the container, at a specific position in space.
 * The container, if moved, will properly adjust the position of all contained
 * child UI elements to keep proper relative positioning between them. This
 * allows for a container to be 'shifted' around (jumped, or slowly slid, for
 * buttonbars or or 'submenus' or the like), as well as alpha-faded (as alpha in
 * the drawstate is passed through to child @p Draw calls), and hidden/shown
 * (visibility control).
 * <p>
 * 
 * The container also has quite detailed handling of NvGestureEvents and
 * NvUIReactions, actually implementing the main 'translation' between the two
 * systems (that is, NvGestureEvents come in that a child might handle and issue
 * an NvUIReaction to, and the container code passes the reaction up. The
 * NvUIWindow class is a specialized container that automatically calls down
 * into its children after event handling to allow them a chance at processing
 * any Reaction, without further explicit code by the developer.
 * <p>
 * 
 * The container also specially manages the focus state, special-case handling
 * any incoming events, and passing them to the last-focused child for a 'first
 * shot' at those events before passing them to all children in general.
 * 
 * @author Nvidia 2014-9-10 18:42
 * @see {@link NvUIWindow}
 * @see {@link NvUIElement}
 * @see {@link NvUIEventResponse}
 * @see {@link NvUIReaction}
 */
public class NvUIContainer extends NvUIElement {

	/**
	 * A background visual to draw behind all children, stretched to fill our
	 * dimensions.
	 */
	protected NvUIGraphic m_background;

	/**
	 * A flag for whether to report events inside the container as handled even
	 * if no child wants them -- thus 'consuming' them and preventing others
	 * from trying to use them.
	 */
	protected boolean m_consumeClicks;
	/**
	 * Head of a doubly-linked, non-circular list of child elements.
	 * <p>
	 * 
	 * The use of internal linked-list fields of NvUIElement means child can
	 * only exist in one container at any one time.
	 * <p>
	 * 
	 * We walk the list forward to draw back-to-front visuals.
	 */
	protected NvUIElement m_childrenHead;
	/**
	 * Tail of a doubly-linked list of children.
	 * <p>
	 * 
	 * We need double-link so we can walk the list tail-to-head in order to
	 * process pointer events topmost-first.
	 */
	protected NvUIElement m_childrenTail;
	/** Number of child element on our linked-list. */
	protected int m_numChildren;
	/**
	 * The child that the user has most recently interacted with. Cleared if the
	 * child no longer responds that it wants focus, or if the container's
	 * {@link #dropFocus} method is explicitly called.
	 */
	protected NvUIElement m_childInteracting;
	/**
	 * A temporary (short-lived), popped-up element that we want drawn over
	 * everything else.
	 */
	protected NvUIElement m_popup;
	// NvUIContainerFlags::Enum m_flags; /**< Flags as defined by
	// NvUIContainerFlags. */

	/** The child that is currently showing focus. */
	protected NvUIElement m_childFocused;
	/** An overlaid graphic used to display which child currently has focus. */
	protected NvUIGraphic m_focusHilite;
	
	 /** Normal constructor.
    Takes width and height dimensions, optional background graphic, and optional flags.
    Origin is set separately. */
	public NvUIContainer(float width, float height, NvUIGraphic bg){
		setBackground(bg);
		setDimensions(width, height);
		
		m_canMoveFocus = true;
	}
	
	@Override
	public void dispose() {
		super.dispose();
		
		dropFocus();
		m_focusHilite.dispose();
		setFocusHilite(null);
		
		 // iterate and delete all the children, container OWNS them.
	    // easier to use while loop vs for loop for this type of operation.
		lostInteract();
		
		while (m_childrenHead != null)
	    {
	        NvUIElement delme = m_childrenHead;
	        m_childrenHead = m_childrenHead.m_llnext;
	        if (delme == m_childrenHead) // looped
	            m_childrenHead = null;
	        delme.dispose();
	    }
		
		m_childrenHead = m_childrenTail = null;

	    if (m_background != null)
	    	m_background.dispose();
	}
	
	/** Set the background graphic element to draw before all children. */
	public void setBackground(NvUIGraphic bg)
	{
	    m_background = bg;
	}
	
	/** Set whether or not to consume all unhandled input within our bounding rect. */
	public void setConsumeClicks(boolean b)
	{
	    m_consumeClicks = b;
	}
	
	public void setFocusHilite(NvUIGraphic hilite) // we should refcount users...
	{
	    m_focusHilite = hilite;
	}
	
	/** Add a child element to our list, at the desired top/left offset. */
	public void add(NvUIElement el, float desiredleft, float desiredtop){
		if (el == null) // !!!!TBD OOOPS
	    {
//	        NvLogger.e("!!> OOOOPS.  Tried to add NULL to container.");
	        return;
	    }

	    if (el.m_llnext != null) // !!!!TBD OOOPS already linked in somewhere.
	    {
//	    	NvLogger.e("!!> OOOOPS.  Tried to add a UI element to container, but it already is tagged as being in a container elsewhere.");
	        return;
	    }

	    add(el);
	    el.setOrigin(m_rect.left+desiredleft, m_rect.top+desiredtop);
	}
	
	/** Remove a child from our list. */
	public boolean remove(NvUIElement child){
		lostInteract();

	    NvUIElement findme = m_childrenHead;
	    // we search for this child before mucking with
	    // next/prev, in case it's not IN our list!
	    while (findme != null)
	    {
	        if (child.equals(findme)) // we're good
	        {
	            m_numChildren--; // dropping him.
	            if (m_numChildren==0) // was all we had, quick out.
	            {
	                m_childrenHead = null;
	                m_childrenTail = null;
	            }
	            else
	            {
	                if (child.equals(m_childrenHead)) 
	                    m_childrenHead = m_childrenHead.m_llnext;
	                else // then prev is valid.
	                    child.m_llprev.m_llnext = child.m_llnext;

	                if (child.equals(m_childrenTail))
	                    m_childrenTail = m_childrenTail.m_llprev;
	                else // then next is valid
	                    child.m_llnext.m_llprev = child.m_llprev;
	            }
	                
	            child.m_llnext = null;
	            child.m_llprev = null;

	            child.setParent(null);

	            return true;
	        }
	        
	        findme = findme.m_llnext;
	    }

	    return false;
	}
	
	/** Hold onto a specific UI element to be drawn 'popped up' on top of all other elements. */
	public void addPopup(NvUIElement el){
		NvUIContainer myparent = getParent();
	    if (myparent != null)
	        myparent.addPopup(el);
	    else
	        m_popup = el;
	}
	
	/** Clear prior popped-up element. */
	public void removePopup(NvUIElement el){
		NvUIContainer myparent = getParent();
	    if (myparent != null)
	        myparent.removePopup(el);
	    else if(m_popup == el)
	    	m_popup = null;
	}
	
	/** Set the top-left origin point of our bounding rect. */
	public void setOrigin(float x, float y){
		NvUIRect sr = new NvUIRect();

	    getScreenRect(sr); // grab the container loc

	    if (x==sr.left && y==sr.top)
	        return; // nothing to do!

	    // store delta offset for this 'move'...
	    float dx = x - sr.left;
	    float dy = y - sr.top;
	    
	    super.setOrigin(x, y);

	    for (NvUIElement child = m_childrenHead; child != null; child = child.m_llnext)
	    {
	        child.getScreenRect(sr); // grab the loc
	        child.setOrigin(sr.left+dx, sr.top+dy);
	    }

	    if (m_background != null)
	    {
	        m_background.getScreenRect(sr); // grab the loc
	        m_background.setOrigin(sr.left+dx, sr.top+dy);
	    }
	}
	
	/** Set width/height dimensions of our bounding rect.
	    These values are especially applicable if the container is flagged to clip children to its bounds. */
	public void setDimensions(float w, float h){
		super.setDimensions(w, h);
		if (m_background != null)
			m_background.setDimensions(w, h);
	}
	
	/** Implements dispatching HandleEvent calls through to all contained children.<p>
	    - The focused child always gets an early shot at the event, in order to shortcut all
	    further processing, since it was last to interact with the user and highly likely to
	    handle the next event.<p>
	    - If there is a popup attached to us, and was not the focused child, it gets the next
	    shot at the event, ahead of the normal loop over all children.<p>
	    - Lastly, we give the list of children a shot at the event. Note that we walk the child
	    list tail-to-head, in order to process clicks frontmost-first.<p>
	    - After all child processing is complete, if the event still has not been handled but
	    SetConsumesClicks(true) was called on us, if the event was inside us and the start of
	    or continuation of a chain of events, we will consume the event by returning it as
	    having been handled by us. */
	public int handleEvent(NvGestureEvent ev, long timeUST, NvUIElement hasInteract){
		int r = nvuiEventNotHandled;
	    NvUIElement childHadInteract = m_childInteracting;
	    NvUIElement childWantsInteract = null;

	    if (!m_isVisible)
	        return r;

	// !!!!TBD handling focus changes.
	    if (this!=hasInteract) // we lost it, cascade loss FIRST.
	        lostInteract();

	    if (m_childInteracting == null && hasInteract != null) // temporarily promote it to 'our child' status
	        childHadInteract = hasInteract;

	    // !!!!TBD for now, don't check/filter ev->type, we assume always basic gesture/input.
	    {
	        // NEW strategy.  Handle popup before anything else.
	        if (m_popup != null && m_popup!=childHadInteract)
	        {
	            NvUIElement dome = m_popup;
	            r = dome.handleEvent(ev, timeUST, childHadInteract);
	            if ((r&nvuiEventWantsInteract) != 0 && m_popup != null) // ensure we didn't unpop
	                childWantsInteract = dome;
	        }

	        if (m_childInteracting != null && (r&nvuiEventHandled)==0)
	        {
	            r = m_childInteracting.handleEvent(ev, timeUST, m_childInteracting);
	            if ((r&nvuiEventWantsInteract) == 0)
	                lostInteract(); // will clear the child AND tell it to clear...
	        }

	        if ((r&nvuiEventHandled) == 0
	        && ((m_childInteracting == null) || (getActiveSlideInteractGroup() != 0) ) // sliding going on...
	        && (/*ev.kind>NvGestureKind::PRESS ||*/ m_rect.inside(ev.x+ev.dx, ev.y+ev.dy, 0, 0)) // if not focused, only care about events inside.
	            )
	        { // we need to handle events BACKWARDS, so 'top' elements get first shot.
	            if ((r&nvuiEventHandled) == 0)
	                for (NvUIElement dome = m_childrenTail; dome != null; dome = dome.m_llprev)
	                {
	                    if (dome==childHadInteract)
	                        continue; // did me already.                
	                    r = dome.handleEvent(ev, timeUST, childHadInteract);
	                    if ((r&nvuiEventWantsInteract) != 0)
	                        childWantsInteract = dome;
	                    if ((r&nvuiEventHandled) != 0 )
	                        break;
	                }
	        }

	        if (childWantsInteract!=null)
	        {
	            lostInteract();
	            m_childInteracting = childWantsInteract;
	        }
	        
	        if (r != 0)
	            return r;
	    }
	    
	    if (m_consumeClicks)
	    {
	        boolean b = m_rect.inside(ev.x+ev.dx, ev.y+ev.dy, 0, 0);
	        if (b && ev.kind==NvGestureKind.PRESS)
	        {
	            if (m_childInteracting==null)
	            {
	                // we want to fake it up a layer.
	                return (nvuiEventHandled | nvuiEventWantsInteract); // as for container to keep our focus.
	            }
	        }
	        else
	        if (b && ev.kind>NvGestureKind.PRESS && hasInteract==this)
	        {
	            return nvuiEventHandled;
	        }
	    }

	    return nvuiEventNotHandled;
	}
	
	/** Draws a backgound if we have one, followed by children in order of the linked-list. */
	public void draw(NvUIDrawState drawState){
		if (!m_isVisible) return;

	    if (drawState.designHeight != 0 && drawState.designWidth!=0)
	    {
	        if( (m_rect.top>drawState.designHeight) ||
	            (m_rect.left>drawState.designWidth) ||
	            ((m_rect.top+m_rect.height)<0.0f) ||
	            ((m_rect.left+m_rect.width)<0.0f) )
	        {
	            return;
	        }
	    }
	    
	    NvUIDrawState myds;
	    if (m_alpha!=1.0f){
	    	myds = new NvUIDrawState(drawState);
	        myds.alpha *= m_alpha;
	    }else{
	    	myds = drawState;   // To avoid reallocate memory for myds 
	    }

	    if (m_background != null)
	        m_background.draw(myds);

	    if (m_hasFocus && m_childFocused != null && m_childFocused.showFocus())
	    {
	        if (m_focusHilite != null) // it should already be in position
	            m_focusHilite.draw(myds);
	    }

	    for (NvUIElement drawme = m_childrenHead; drawme != null; drawme = drawme.m_llnext)
	        drawme.draw(myds);

	    if (m_popup != null)
	        m_popup.draw(myds);
	}
	
	/** Implements dispatching HandleReaction calls through to all contained children.
	    As any contained NvUIElement might respond to the raised NvUIReaction, we walk the
	    linked-list in normal head-to-tail order, since it is no more likely that any
	    one element will handle the reaction. */
	public int handleReaction(NvUIReaction react){
		int r = nvuiEventNotHandled, rme;
//	    NvUIElement *active = NULL;
	    for (NvUIElement child = m_childrenHead; child != null; child = child.m_llnext)
	    {
	        rme = child.handleReaction(react); // !!!!TBD
//	        if (!active && (rme & nvuiEventHandledFocus))
//	            active = child;
	        if (rme != 0)
	            r = rme;
	    }
	    
//	    if (active)
//	        MakeChildFrontmost(active);

	    return r;
	}
	
	/** Handle change in view size.
	    Pass along the reshape to all child elements. */
	public void handleReshape(float w, float h){
		NvUIRect cr = new NvUIRect();
		NvUIRect uir = new NvUIRect();
		getScreenRect(cr);
		
		// in case inherited does something useful, call it.
		super.handleReshape(w, h);
		
		// then loop over children.
	    for (NvUIElement child = m_childrenHead; child != null; child = child.m_llnext)
	    {
	        child.handleReshape(w, h); // !!!!TBD

	        // let's do some hacked repositioning for elements that look like they
	        // are aligned right or bottom.
	        
	        // !!!!TBD TODO THIS IS AN UTTER HACK.
	        // Should have alignment properties at UIElement level,
	        // and have position relative to alignment, like TextBox does...
	        child.getScreenRect(uir);

	        float newleft, newtop;
	        newleft = uir.left;
	        newtop = uir.top;
	        // within 1/4 of view width, assume right aligned.
	        if (uir.left-cr.left > cr.width*0.75f)
	        {
	            newleft = newleft + (w - cr.width);
	            // ONLY adjust y position if we're right-aligned for the moment...
	            // within 1/4 of view height, assume bottom aligned.
	            if (uir.top-cr.top > cr.height*0.75f)
	                newtop = newtop + (h - cr.height);
	        }

	        // if values changed, set new origin.
	        if (newleft != uir.left || newtop != uir.top)
	            child.setOrigin(newleft, newtop);
	    }
	}
	
	/** Virtual method for moving the highlight focus between elements and acting on the selected element. 
	    @return true if we were able to move the focus or act on it, false otherwise. */
	public int handleFocusEvent(int evt){
		final int rno = nvuiEventNotHandled;

	    if (!m_isVisible)
	    {
	        m_hasFocus = false; // make sure state is correct.
	        return rno;
	    }

	    // popup always gets first shot.  if we get a clear, it takes it first to hide.
	    if (m_popup != null)
	    {
	        int pr = m_popup.handleFocusEvent(evt);
	        if ((pr & nvuiEventHandled) != 0)
	        {
	            //m_hasFocus = true;
	            return pr;
	        }
	        // else we should have dropped the popup, so move on to next...
	    }

	    if (evt == NvFocusEvent.FOCUS_CLEAR)
	    {
	        dropFocus();
	        return nvuiEventHandled; // since we handled.
	    }

	    if ((evt & NvFocusEvent.FLAG_MOVE) != 0)
	    {
	        int r = nvuiEventNotHandled;

	        // let focused child have first shot.
	        if (m_childFocused!= null && m_childFocused.canMoveFocus())
	            r = m_childFocused.handleFocusEvent(evt);
	        if ((r & nvuiEventHandled) != 0)
	        {
	            // focus didn't change at our level, just return response.
	            return r;
	        }

	        // !!!!TBD TODO for now, we're doing moveto==DOWN/NEXT
	        NvUIElement child = null;
	        if (evt==NvFocusEvent.MOVE_DOWN)
	        {
	            child = m_childFocused != null? m_childFocused.m_llnext : m_childrenHead;
	            for (; child != null; child = child.m_llnext)
	                if (child.canFocus())
	                    break; // found eligible child.
	        }
	        else
	        if (evt==NvFocusEvent.MOVE_UP)
	        {
	            child = m_childFocused != null ? m_childFocused.m_llprev : m_childrenTail;
	            for (; child != null; child = child.m_llprev)
	                if (child.canFocus())
	                    break; // found eligible child.
	        }
	        else
	        {
	            return rno; // don't handle that yet...  !!!!TBD TODO
	        }

	        m_childFocused = child; // which might be NULL.

	        updateFocusState(); // our local hilite.

	        if (child!= null && child.canMoveFocus())
	            child.handleFocusEvent(evt); // note each level of child might set it's own focus hilite...

	        if (child==null)
	        {
	            m_hasFocus = false;
	            return rno; // so not handled if no child to focus, OR we go off the end.
	        }

	        // else we handled this.
	        m_hasFocus = true;
	        return nvuiEventHandled;
	    }

	    // else FLAG_ACT
	    if (m_childFocused == null)
	    {
	        m_hasFocus = false;
	        return rno;
	    }

	    // pass it along as only the individual classes know what to do.
	    m_hasFocus = true;
	    return m_childFocused.handleFocusEvent(evt);
	}
	
	/** Virtual method for clearing focus from this container. */
	public void dropFocus(){
		if (m_popup != null)
	        m_popup.dropFocus(); // TODO!!!TBD

	    if (m_childFocused != null)
	    {
	        m_childFocused.dropFocus();
	        m_childFocused = null;
	    }

	    updateFocusState();

	    m_hasFocus = false;
	}
	
	/* Update the status of the focus, focushilite, etc., based on focused child pointer. */
	private void updateFocusState(){
		if (m_focusHilite==null) return;

	    if (m_childFocused==null)
	    {
	        m_hasFocus = false;
	        if (m_focusHilite != null)
	            m_focusHilite.setVisibility(false);
	    }
	    else
	    {
	        m_hasFocus = true;
	        if (m_focusHilite != null)
	        {
	            NvUIRect cr = new NvUIRect();
	            m_childFocused.getFocusRect(cr);
	            m_focusHilite.setOrigin(cr.left, cr.top);
	            m_focusHilite.setDimensions(cr.width, cr.height);
	            m_focusHilite.setVisibility(true);
	        }
	    }
	}
	
	/** Move a specified child to be drawn in front of all others. */
	public boolean makeChildFrontmost(NvUIElement child){
		if (child==null)
	        return false; // done.
	    if (child==m_childrenTail) // already done
	        return true;

	    NvUIElement findme = m_childrenHead;
	    // we search for this child before mucking with
	    // next/prev, in case it's not IN our list!
	    while (findme != null)
	    {
	        if (child==findme) // we're good
	        {
	            // first, pull from list.
	            if (child==m_childrenHead) 
	                m_childrenHead = child.m_llnext;
	            else // then prev is valid.
	                child.m_llprev.m_llnext = child.m_llnext;
	            // child can't be tail, we would have early-exited.
	            child.m_llnext.m_llprev = child.m_llprev;

	            // then, add to end.                
	            child.m_llnext = null; // next should be null
	            child.m_llprev = m_childrenTail; // prev pts to curr tail
	            m_childrenTail.m_llnext = child; // tail pts to us now.
	            m_childrenTail = child; // we take over as tail.

	            return true;
	        }
	        
	        findme = findme.m_llnext;
	    }
	    
	    return false;
	}
	
	/** Clear any state related to interaction. */
	public void lostInteract(){
		if (m_childInteracting != null)
	    {
	        m_childInteracting.lostInteract();
	        m_childInteracting = null;
	    }
	}
	
	/** Accessor for pointer to the child currently being interacted with. */
	public NvUIElement getInteractingChild()
	    {
	        return m_childInteracting;
	    }
	
	/** Accessor for the SlideInteractGroup identifier. */
	public int getSlideInteractGroup()
	    {
	        if (m_childInteracting != null)
	            return m_childInteracting.getSlideInteractGroup();
	        return super.getSlideInteractGroup(); // in case inherited does something...
	    }
	/* this internal/private method is what handles actually adding a
    child element to our linked list and setting back pointer. */
	private void add(NvUIElement el){
		if (el == null) // !!!!TBD OOOPS
	    {
//	        NvLogger.e("!!> OOOOPS.  Tried to add null to container.");
	        return;
	    }

	    if (el.m_llnext != null) // !!!!TBD OOOPS already linked in somewhere.
	    {
//	    	NvLogger.e("!!> OOOOPS.  Tried to add a UI element to container, but it already is on a LL apparently.");
	        return;
	    }

	    // !!!!TBD these are really sanity checks, but shouldn't be necessary
	    // if everything else is clean...
	    el.m_llnext = null;
	    el.m_llprev = null;

	    // if no list, we start it.  prev & next are us for single item.
	    if (m_childrenHead==null)
	    {
	        m_childrenHead = el;
	        m_childrenTail = el;
	    }
	    else
	    {
	        el.m_llprev = m_childrenTail; // prev pts to tail, next stays null.
	        m_childrenTail.m_llnext = el; // tail pts to us now.
	        m_childrenTail = el; // we take over as tail.
	    }

	    el.setParent(this);

	    m_numChildren++;
	}
}

//----------------------------------------------------------------------------------
// File:        NvUIPopupMenu.java
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
 * The NvUIPopupMenu is a specialized container class that simulates a popup
 * menu.
 * <p>
 * Note that this class itself does minimal work to make the 'menu magic'
 * happen. It is really the contents of our container, the 'menu item' elements
 * themselves, that control the real functionality and visuals of a given
 * NvUIPopupMenu.
 * <p>
 * 
 * There is no default implementation of menu items or other substructure, aside
 * from currently we add items vertically in order (a subclass could implement
 * different layout for a specialized menu with known visual structure). The
 * items should generally be NvUIButtons, but could also be static graphics for
 * dividers, or static text for other status in the menu.
 * 
 * @author Nvidia 2014-9-12 11:21
 * @see {@link NvUIPopup}
 * @see {@link NvUIContainer}
 */
public class NvUIPopupMenu extends NvUIContainer {
	/** The popup button that owns/shows/hides us. */
	private NvUIPopup m_myButton;

	/**
	 * Default constructor. Basically just sets up our NvUIContainer superclass
	 * for us.
	 * 
	 * @param btn
	 *            The NvUIPopup that owns this popup menu.
	 * @param width
	 *            The base width of the menu.
	 * @param height
	 *            The base height of the menu (noting actual container height
	 *            changes as items are added...).
	 * @param bg
	 *            <b>[</b>optional<b>]</b> Background graphic to display under
	 *            the menu items themselves. For a menu, handy to have an
	 *            NvUIGraphicFrame for the background element.
	 */
	public NvUIPopupMenu(NvUIPopup btn, float width, float height,
			NvUIGraphic bg) {
		super(width, height, bg);
		m_myButton = btn;
		
		m_canFocus = true;
	    m_canMoveFocus = true;
	}

	/**
	 * Override to ensure we take down the popup menu at the right times.
	 * <p>
	 * 
	 * We first allow the normal inherited container handling to occur. If
	 * nothing in the container handles the event, and the event was a press or
	 * a release, we tell our NvUIPopup button to finish the popup.
	 * <p>
	 * 
	 * If something in the container DID handle the event, and the event was a
	 * release, and there was NO reaction to the event, we presume there was a
	 * release inside the container but not inside an item, and thus also tell
	 * out button to finish the popup.
	 * 
	 * @see {@link NvUIPopup}
	 * @see {@link NvUIContainer}
	 */
	public int HandleEvent(NvGestureEvent ev, long timeUST,
			NvUIElement hasInteract) {
		int r = super.handleEvent(ev, timeUST, hasInteract);
	    if (r==nvuiEventNotHandled) // if we didn't handle the event...
	    { 
	        // and it wasn't INSIDE my button, which can handle itself...
	        if (!m_myButton.hit(ev.x, ev.y))
	        {
	            // and it was a press or a release...
	            if (ev.kind==NvGestureKind.PRESS) // || ev.kind&NvGestureKind::MASK_RELEASED)
	            { // then take down the popup...
	                m_myButton.popupFinish();
	                // and flag that we handled!
	                r = nvuiEventHandledInteract; // but NO reaction...
	            }
	        }
	    }
	    else // child handled in some form?
	    if ((ev.kind&NvGestureKind.MASK_RELEASED)!= 0 && (r & nvuiEventHadReaction) == 0) 
	    { // no reaction.  means handled, but nothing tripped.  out of hitrect 99.9%.
	        m_myButton.popupFinish();
	    }
	    else
	    {
	    }

	    return r;
	}

	/** Override to handle events so we stay up until otherwise put away... */
	public int handleFocusEvent(int evt) {
		if (!getVisibility())
	        return nvuiEventNotHandled;

	    if (evt == NvFocusEvent.FOCUS_CLEAR)
	    {
	        // close the menu.
	        m_myButton.popupFinish();
	        return nvuiEventHandled;
	    }

	    int r = super.handleFocusEvent(evt);
	    if ((r & nvuiEventHandled) == 0) 
	    {
	        if (evt == NvFocusEvent.ACT_PRESS)
	        { // then take down the popup...
	            m_myButton.popupFinish();
	            // and flag that we handled!
	            r = nvuiEventHandledInteract; // but NO reaction...
	        }
	    }
	    return (r | nvuiEventHandled); // while we're up, stay up.
	}
}

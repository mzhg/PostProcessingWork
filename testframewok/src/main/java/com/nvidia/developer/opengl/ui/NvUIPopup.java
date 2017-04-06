//----------------------------------------------------------------------------------
// File:        NvUIPopup.java
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

import java.util.HashMap;

/**
 * The NvUIPopup class encapsulates the text/button that causes the
 * NvUIPopupMenu to appear. This is the button object that when pressed causes a
 * menu to pop up in the NvUIWindow in which it is contained.
 * <p>
 * 
 * The menu contents are constructed by adding UI elements representing the
 * 'menu items' to the popup, one at a time, also passing in a text string/name
 * and a value for each item. The individual NvUIElements each handle what
 * occurs in the system when one is chosen, allowing for extreme flexibility in
 * construction and handling of menus. The string:value combinations are used
 * after the popup detects a menu item has been selected to appropriately change
 * the popup button's text to match the selection.
 * <p>
 * 
 * @see {@link NvUIPopupMenu}
 * @see {@link NvUIWindow}
 * @see {@link NvUIButton}
 * @author Nvidia 2014-9-12 11:38
 * 
 */
public class NvUIPopup extends NvUIButton {

	/** The popup menu we build and will display when user presses on us. */
	NvUIPopupMenu m_menu;
	/** A small indicator button that there's a popup/dropdown menu. */
	NvUIButton m_popper;
	/** The total number of menu items in our menu. */
	int m_itemCount;
	/** Our canonical height. */
	float m_lineHeight;
	/** The starting Y offset for the next item added to our menu. */
	float m_nextItemTop;
	/** Top padding for positioning items in the menu's container. */
	int m_padTop;
	/** Bottom padding for sizing the height of our menu's container. */
	int m_padBottom;
	/**
	 * Prefix string for our button title, for recomposing after new menu item
	 * selected.
	 */
	String m_titlePre;
	/** Map item value to a name, for setting title after pick. */
	HashMap<Integer, String> m_itemNames = new HashMap<Integer, String>(8);

	/**
	 * Default constructor for popup object.
	 * 
	 * @param actionCode
	 *            The action code generated when user presses on us, so we know
	 *            to display/hide popup menu.
	 * @param visrep
	 *            Standard array of visuals for button states
	 * @param popupFrame
	 *            A graphic frame surrounding the popup menu contents
	 * @param popper
	 *            A small button visual that denotes we have a menu for the user
	 * @param title
	 *            <b>[</b>optional<b>]</b> Prefix for our button title,
	 *            prepended to current menu item name. Defaults NULL.
	 * @param pt
	 *            <b>[</b>optional<b>]</b> Point size for font for our title.
	 *            Defaults to 0.
	 * @param shadowed
	 *            <b>[</b>optional<b>]</b> Whether our title should have drop
	 *            shadow. Defaults false.
	 */
	public NvUIPopup(int actionCode, NvUIElement[] visrep,
					 NvUIGraphicFrame popupFrame, NvUIButton popper, String title,
					 float pt, boolean shadowed) {
		super(NvUIButtonType.CHECK, actionCode, visrep, title, pt, shadowed);
		
		m_popper = popper;
		m_padTop = 8;
		m_padBottom = 12;
		
		m_canFocus = true;

	    m_lineHeight = getHeight(); // !!!!TBD
	    m_titlePre = title;

	    setMaxDrawState(1); // just base+pressed
	    setDrawState(0);
	    setStickyClick(true);
	    setSlideInteractGroup(actionCode); // HACK HACK HACK
	            
	    m_menu = new NvUIPopupMenu(this, getWidth(), getHeight(), popupFrame);
	    m_menu.setSlideInteractGroup(actionCode); // HACK HACK HACK
	    m_menu.setVisibility(false);

	    if (m_popper != null)
	        m_popper.setOrigin(m_rect.left+getWidth()-m_popper.getWidth()-8, m_rect.top+(getHeight()-m_popper.getHeight())*0.5f);
	}
	
	@Override
	public void dispose() {
		if(m_menu != null)
			m_menu.dispose();
		
		if(m_popper != null)
			m_popper.dispose();
	}

	/**
	 * Add an NvUIElement to our popup menu container.
	 * 
	 * @param el
	 *            NvUIElement to add next in order.
	 * @param name
	 *            Title for the value this item represents, for showing to user
	 *            when we're not popped up.
	 * @param value
	 *            Value sent through our reaction when this item is clicked in
	 *            the menu.
	 */
	public void addItem(NvUIElement el, String name, int value) {
		// the various offsets are really specific to the given popup
	    // and should be set as SetYPadTop and SetYPadBottom or the like.
	    // !!!!TBD TODO

	    // add new item at the next top position (plus top padding)
	    m_menu.add(el, 0, m_nextItemTop + m_padTop);

	    // attach new item to our slideInteractgroup, so we can slide through list without releasing.
	    el.setSlideInteractGroup(m_action);

	    // update the item name list
//	    m_itemNames[value] = name;
	    m_itemNames.put(value, name);
	    m_itemCount++;

	    // then update the bounds of the menu, including padding.
	    m_nextItemTop += el.getHeight(); // update based on bottom of curr element...
	    m_menu.setDimensions(getWidth(), m_nextItemTop + m_padTop + m_padBottom);
	}

	/** Call to set which item is active based on value of item (not 'index'). */
	public void setActiveItemValue(int value) {
		updateTitleValue(value);
	}

	/**
	 * Override so we can special-case presses.
	 * <p>
	 * 
	 * We override and call inherited method first. When it returns, if there
	 * was a reaction, and the gesture was a @p press, then depending on our
	 * state we either show or hide our popup menu.
	 * <p>
	 * 
	 * We also then clear the reaction flag, as we never want our base button to
	 * cause a reaction, only the menu items in the popup itself cause
	 * reactions.
	 * 
	 * @see {@link #popupStart()}
	 * @see {@link #popupFinish()}
	 * @see {@link NvUIButton}
	 */
	public int handleEvent(NvGestureEvent ev, long timeUST,
			NvUIElement hasInteract) {
		int r = super.handleEvent(ev, timeUST, hasInteract);
	    if ((r&nvuiEventHandled) != 0)
	    {
	        final boolean vis = m_menu.getVisibility();
	        if (ev.kind==NvGestureKind.PRESS)
	        {
	            if (!vis)
	            {
	                popupStart();
	            }
	            else if (vis) //&& ev.kind & NvGestureKind::MASK_RELEASED)
	            {
	                popupFinish();
	                consumeGesture(ev); // so we don't double-handle.
	            }
	        }
	    }
	    // but our button should NEVER cause a reaction on its own, right?? !!!!!TBD TODO
	    r = (r&nvuiEventNoReaction);
	    return r;
	}

	/**
	 * Override to handle successful selection of a menu item.
	 * 
	 * When a menu item is selected, we reset our button title to use the
	 * selected item's name, we finish/hide the popup menu, and we proxy the
	 * reaction to our menu so that it can also react as needed (highlighting
	 * the right item, etc.). Finally, call inherited method when we're all
	 * done.
	 * 
	 * @see {@link #popupFinish()}
	 * @see {@link NvUIButton}
	 * @see {@link NvUIPopupMenu}
	 * @see {@link NvUIReaction}
	 */
	public int handleReaction(NvUIReaction react) {
		// do inherited first, in case we override visual results...
	    super.handleReaction(react);
	    
	    if (react.code==m_action) // our actionCode
	    {
	        if (react.uid==m_uiuid) // this is the UIPopup
	        {
	            // when to tear down.
	        }
	        else // must be one of the menu items
	        {
	            // get the menu to actually handle the chosen event for us.
	            m_menu.handleReaction(react);

	            updateTitleValue(react.ival);

	            // tear down in case it's up.
	            popupFinish();
	        }
	    }
	    else
	    { // hmmm.  not our action.
	        // only continue IF NO FLAGS so clear and update
	        // don't cause us to tear-down menu... !!!!TBD TODO
	        if (react.flags == 0) 
	        {
	            // are we up?  if so, tear down.
	            if (m_menu.getVisibility())
	                popupFinish();
	        }
	    }

	    return nvuiEventNotHandled; // !!!!TBD
	}

	/** Override to handle focus events causing the menu to pop up/down. */
	public int handleFocusEvent(int evt) {
		if (evt != NvFocusEvent.ACT_PRESS) return nvuiEventNotHandled;

	    int r = super.handleFocusEvent(evt);
	    if ((r&nvuiEventHandled)!=0)
	    {
	        if (!m_menu.getVisibility())
	            popupStart();
	        else
	            popupFinish();
	    }
	    // but our button should NEVER cause a reaction on its own, right?? !!!!!TBD TODO
	    r = (r&nvuiEventNoReaction);
	    return r;
	}

	/** Override as we manually Draw our popper visual if we have one. */
	public void draw(NvUIDrawState ds) {
		super.draw(ds);
		
		if(m_popper != null)
			m_popper.draw(ds);
	}

	/**
	 * Override as we manually match our popper visual if we have one to our own
	 * draw state.
	 */
	public void setDrawState(int n) {
		super.setDrawState(n);
		
		if(m_popper != null)
			m_popper.setDrawState(n);
	}

	/**
	 * Override as we need to manually position sub-elements. Specifically, we
	 * position our popper visual if we have one, override our title position
	 * (as we know better than our button superclass), and set the popup menu
	 * position so it is ready to display.
	 */
	public void setOrigin(float x, float y) {
		super.setOrigin(x, y);
		m_menu.setOrigin(x, y + m_lineHeight-2);
		
		if (m_title != null) // override what button does...
	    {
	        m_title.setAlignment(NvUITextAlign.LEFT);
	        m_title.setOrigin(x+10,y+4);
	    }
	    if (m_popper != null)
	    {
	        m_popper.setOrigin(x+getWidth()-m_popper.getWidth()-8, y+(getHeight()-m_popper.getHeight())*0.5f);
	    }
	}

	/** Override to handle adjustmetns to the focus rect for the popup. */
	public void getFocusRect(NvUIRect rect) {
		getScreenRect(rect);
		rect.grow(8, 8);
	}

	/** Accessor to get at the popup menu itself. */
	public NvUIPopupMenu getUIPopupMenu() {
		return m_menu;
	}

	/** Display the popup in our window and flag our button as 'pressed'. */
	protected void popupStart() {
		// okay, only WE should be calling this, so safe hack here...
	    // shouldn't be another reaction in waiting yet.
	    // !!!!TBD yet another reason for a 'reaction queue'...
	    // clear the state of our interactive children that match our actioncode.
	    NvUIReaction react = getReactionEdit(true); // true to clear as new.
	    react.flags = NvReactFlag.CLEAR_STATE;
	    react.code = getActionCode();
	    react.state = 0; // clear
	    react.ival = 0xDEADBEEF; // something nobody would ever match.
	    // let the menu handle synchronously.
	    m_menu.handleReaction(react);

	    // then add popup to parent, make it visible
	    m_parent.addPopup(m_menu);
	    m_menu.setVisibility(true);
	    
	    // set button to active highlight
	    setDrawState(1);
	}

	/**
	 * Hide the popup, remove from window, and clear our button back to
	 * 'active'.
	 */
	protected void popupFinish() {
		// take down the menu and hide it.
	    if (m_parent != null) m_parent.removePopup(m_menu);
	    m_menu.dropFocus();
	    m_menu.setVisibility(false);

	    // clear us back to no-highlight.  TBD! TODO! focus rect for dpad nav.
	    setDrawState(0); 
	    setPrevDrawState(0);
	}

	/** Update our title with the name matching the given value. */
	private void updateTitleValue(int value) {
		// set our title appropriately, in case menu set state value.
	    if (m_title != null)
	    {
	        String bind = m_itemNames.get(value);
	        if (bind != null) {
	            // we have a binding.  do something with it.
	            String tmps;
//	            tmps = m_titlePre;
//	            tmps += ": ";
//	            tmps += bind->second;
	            tmps = m_titlePre + ": " + bind;
	            m_title.setString(tmps);
	        }
	    }
	}
}

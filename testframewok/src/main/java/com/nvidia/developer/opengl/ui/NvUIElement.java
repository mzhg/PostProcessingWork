package com.nvidia.developer.opengl.ui;

import com.nvidia.developer.opengl.utils.NvUtils;

/**
 * The abstract base class of the NvUI framework.<p>
 * All UI classes are derived from NvUIElement.  It provides all the needed
 * member variables and methods for onscreen user interface elements.
 * @author Nvidia 2014-9-2
 */
public abstract class NvUIElement implements NvUIEventResponse{

	/**
	 * Static object storing the 'current running' Reaction.<p>
	 * We keep a static member for easier access and to elimate any dynamic allocation.
	 * Also, there is should only ever be one reaction 'in processing' at any given time.
	 */
	private static final NvUIReaction ms_reaction = new NvUIReaction();
	/** Holds the UIUID index for the to-be-created-next NvUIElement. */
	private static int ms_uiuid_next;
	/** Optional design width for the entire UI hierarchy of a given application -- can be 0. */
	static int ms_designWidth = 1280;
	/** Optional design height for the entire UI hierarchy of a given application -- can be 0.*/
	static int ms_designHeight = 720;
	/** The current active SlideInteractGroup identifier set during most recent PRESS event. */
	private static int ms_activeSlideInteractGroup;
	/** Stores a unique 32-bit identifier (internal to a given app) for a given element.
    Useful for differentiating or identifying elements without referencing pointers
    directly, such as inside of a raised NvUIReaction. */
	protected int m_uiuid = ms_uiuid_next++;
	/** Stores the onscreen position and size. */
	protected final NvUIRect m_rect = new NvUIRect();
	/** Whether the element is currently visible/drawn. */
	protected boolean m_isVisible = true;
	/** Whether this object got the 'down' action, thus interacting with the user. */
	protected boolean m_isInteracting;
	/** Whether the element can take focus. */
	protected boolean m_canFocus;
	/** Whether the element or a child has the focus. */
	protected boolean m_hasFocus;
	/** Whether the element wants to have focus shown when it has focus; some cotainers may not. */
	protected boolean m_showFocus = true;
	/** Whether this element can move focus to another element, usually a container. */
	protected boolean m_canMoveFocus;
	/** Stores the current forced 'alpha-fade' level. Uses normal GL [0.0-1.0] expected range. */
	protected float m_alpha = 1.0f;
	
	/**
	 * An identifier for items which can have focus/hit 'slid' between them.
	 * The SlideInteractGroup is used to aid in cases where it is legal and necessary to 'slide'
	 * the user's interaction between a group of elements while the pointer is still held down,
	 * e.g. dragging through the menu items of a popup menu, or across keys of a     public keyboard. 
	 */
	protected int m_slideInteractGroup;
	/** The container that holds this element. */
	protected NvUIContainer m_parent;
	/** Stores the current drawing 'state' or index (useful for multi-state objects like Buttons). */
	private int m_currDrawState;
	/** The previous drawing 'state' or index (useful for things like Button hit-tracking). */
	private int m_prevDrawState;
	/** The maximum drawing 'state' or index. */
	private int m_maxDrawState;
	/**< built-in doubly-linked-list pointers. */
	NvUIElement m_llnext, m_llprev;
	
	/**
	 * Pure abstract rendering method for widgets.<p>
	 * Pure abstract as there is no base implementation, it must be implemented by each widget subclass.
	 * @param drawState
	 */
	public abstract void draw(NvUIDrawState drawState);
	
	/**     public user interaction handling method.
    We implement a base version to return not-handled, so that non-interactive classes don't have to.
    @param ev The current NvGestureEvent to handle
    @param timeUST A timestamp for the current interaction
    @param[in,out] hasInteract The element that had interaction last, or has taken over current interaction.
    */
	public int handleEvent(NvGestureEvent ev, long timeUST, NvUIElement hasInteract)
    {
       return NvUIEventResponse.nvuiEventNotHandled;
    }

/**     public method for handling NvUIReaction in response to user interaction.
    We implement a base version to return not-handled, so that non-reacting classes don't have to.
*/
    public int handleReaction(NvUIReaction react)
    {
       return NvUIEventResponse.nvuiEventNotHandled;
    }

   /**     public method for reacting to a change in viewport size.
    Not many UI elements will need to handle this direct, so base is a no-op method. */
    public void handleReshape(float w, float h)
    {
        /* no-op */
    }

/** Accessor for whether this element can be focused -- or has a child that can. */
    public boolean canFocus()
    {
        return m_isVisible && m_canFocus;
    }

/** Accessor for whether this element can move the focus itself, generally only true for containers. */
    public boolean canMoveFocus()
    {
        return m_canMoveFocus;
    }

/** Accessor for whether this element HAS the input focus (or has a child that does). */
    public boolean hasFocus()
    {
        return m_hasFocus;
    }

/** Accessor for whether this element's focus state should be shown (containers might not always want it shown). */
    public boolean showFocus()
    {
        return m_showFocus;
    }

/**     public method for moving the highlight focus between UI elements or acting upon the selected one. 
    @return true if we were able to move the focus or act on it, false otherwise. */
    public int handleFocusEvent(int evt)
    {
        return NvUIEventResponse.nvuiEventNotHandled;
    }

/**     public method for setting 2D origin point in UI/view space.
    Base implementation simply sets the NvUIElement's rectangle top-left to the passed in values.
        public as some subclasses may override to reposition children or account for padding/margins. */
    public void setOrigin(float x, float y)
    { // unless overridden, just drop into the m_rect top/left.
        m_rect.left = x;
        m_rect.top = y;
    }

/**     public method for setting the dimensions of this element in pixels.
    Base implementation simply sets the NvUIElements rectangle width and height to passed in values. */
    public void setDimensions(float w, float h)
    {
        m_rect.width = w;
        m_rect.height = h;
    }


/**     public method for changing just the height of this element.
    Leverages SetDimensions so all subclasses will get desired results without further effort. */
    public void SetHeight(float h)
    {
        setDimensions(m_rect.width, h);
    }

/** Accessor to set the Z/depth value of our UI rectangle. */
    public void setDepth(float z)
    {
        m_rect.zdepth = z;
    }

/** Method to test whether the element's bounding rect has a non-zero Z/depth value. */
    public boolean hasDepth()
    {
        return (m_rect.zdepth > 0);
    }

/** Accessor to retrieve the UI-space NvUIRect for this element into a passed-in reference. */
    public void getScreenRect(NvUIRect rect)
    {
        rect.set(m_rect);
    }
    
    /** Get the UI-space rect for this element.<br><b>NOTE:</b> Don't modifier the variable, or it will be occur an error. */
    public NvUIRect getScreenRect(){
    	return m_rect;
    }

/** Accessor to retrieve the UI-space NvUIRect for this element's focus rectangle. */
    public void getFocusRect(NvUIRect rect)
    {
        rect.set(m_rect);
    }

/** Accessor to get the width of this element's bounding rect. */
    public float getWidth()
    {
        return m_rect.width;
    }

/** Accessor to get the height of this element's bounding rect. */
    public float getHeight()
    {
        return m_rect.height;
    }
    
    /** Virtual method for changing just the width of this element.
    Leverages SetDimensions so all subclasses will get desired results without further effort. */
    public void setWidth(float w)
    {
        setDimensions(w, m_rect.height);
    }

/** Virtual method for changing just the height of this element.
    Leverages SetDimensions so all subclasses will get desired results without further effort. */
    public void setHeight(float h)
    {
        setDimensions(m_rect.width, h);
    }
  
/** Set whether or not this element is visible and thus to be drawn. */
    public void setVisibility(boolean show) //     public for customization.
    {
        m_isVisible = show;
    }

/** Get whether or not this element is visible and thus to be drawn. */
    public boolean getVisibility() //     public for customization.
    {
        return(m_isVisible);
    }

/** Set the alpha-blend amount for this element. */
    public void setAlpha(float a) //     public for customization.
    {
        m_alpha = a;
    }

/** Get the current alpha-blend override level for this element. */
    public float getAlpha()
    {
        return m_alpha;
    }

/** Get a the current/active NvUIReaction reference object.*/
    public static NvUIReaction getReaction()
    {
        return ms_reaction;
    }

/** Get the reference to the current NvUIReaction object for editing.
    @param clear Defaults to true to wipe our prior reaction state.  Pass false to leave intact to make minor changes to existing reaction. */
    public static NvUIReaction getReactionEdit(boolean clear)
    {
        if (clear)
        	ms_reaction.makeZero();
        return ms_reaction;
    }

/**     public method for telling this element it has lost interaction. */
    public void lostInteract()
    {
    }

/**     public method for telling this element its is no longer focus. */
    public void dropFocus()
    {
    }

/** Get the parent NvUIContainer, if one was set. */    
    public NvUIContainer getParent() { return m_parent; };
/** Set the parent NvUIContainer, so a child knows who currently 'owns' it. */    
    public void setParent(NvUIContainer p) { m_parent = p; };

/** Get the SlideInteractGroup identifier for this element. */
    public int getSlideInteractGroup() { return m_slideInteractGroup; };
/** Set the SlideInteractGroup identifier for this element. */
    public void setSlideInteractGroup(int group) { m_slideInteractGroup = group; };

/** Get the SlideInteractGroup identifier that is currently active during user interaction. */
    public static int getActiveSlideInteractGroup() { return ms_activeSlideInteractGroup; };
/** Set the SlideInteractGroup identifier that is currently active during user interaction. */
    public static void setActiveSlideInteractGroup(int group) { ms_activeSlideInteractGroup = group; };

/** Get the construction-time unique identifier for this element. */
    public int getUID()
    {
        return m_uiuid;
    }

/** Hit-test a given point against this element's UI rectangle, using no extra margin. */
    public boolean hit(float x, float y)
    {
        return (m_rect.inside(x, y, 0, 0));
    }

/** Notify the NvUI system of a system/window resolution change, so it can resize buffers and such. */
    public static void systemResChange(int w, int h){
    	if(NvUtils.later){
    		if (h<w) // landscape
    	    {
    	        ms_designWidth = 800;
    	        ms_designHeight = 480;
    	    }
    	    else
    	    {
    	        ms_designWidth = 480;
    	        ms_designHeight = 800;
    	    }
    	}
    	
    	NvBitFont.setScreenRes(w, h);
    }

/** Get the current drawing 'state' or index.
    Primarily used for multi-state objects like Buttons to have active vs selected/highlighted, vs inactive states tracked, and those states can then be used to render different visuals. */
    public final int getDrawState()
    {
        return m_currDrawState;
    }

/** Set the current drawing 'state' or index.
    Primarily used for multi-state objects like Buttons to have active vs selected/highlighted, vs inactive states tracked, and those states can then be used to render different visuals. */
    public void setDrawState(int n) // must be     public so we can catch it
    {
        if (n<=m_maxDrawState)
            m_currDrawState = n;
    }
    
/** Set the current drawing 'state' or index back to a stashed prior value.
    Primarily used for objects that want to temporarily, briefly change state, and then revert back -- such as a push-button widget going active->selected->active.  This restores to last stashed value. */
    public void setDrawStatePrev()
    {
        setDrawState(m_prevDrawState);
    }
    
/** Get the prior drawing 'state' or index.
    Primarily used for objects that want to temporarily, briefly change state, and then revert back -- such as a push-button widget going active->selected->active.  This gets the last stashed value. */
    public int getPrevDrawState()
    {
        return m_prevDrawState;
    }

/** Set the prior drawing 'state' or index.
    Primarily used for objects to stash prior visual state while they temporarily display a different state.  This stashes a passed value (generally the current draw state). */
    public void setPrevDrawState(int n)
    {
        if (n<=m_maxDrawState)
            m_prevDrawState = n;
    }

/** Get maximum draw state value supported by this object.
    Used for classes of objects which have the potential to have many different visual states, but may only be set up with physical visuals for a select set.  For example, many buttons may have active and pressed/selected visuals, but may not bother with inactive visual. */    
    public int getMaxDrawState()
    {
        return m_maxDrawState;
    }

/** Get maximum draw state value supported by this object.
    Used for classes of objects which have the potential to have many different visual states, but are only set up a select set. */
     public void setMaxDrawState(int n)
    {
        m_maxDrawState = n;
    }
     
     /** release the resource when this element will be deleted. */
     public void dispose(){
    	 
     }
}

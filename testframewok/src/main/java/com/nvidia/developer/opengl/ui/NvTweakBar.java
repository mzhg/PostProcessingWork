package com.nvidia.developer.opengl.ui;

import com.nvidia.developer.opengl.utils.FieldControl;
import com.nvidia.developer.opengl.utils.NvUtils;

import jet.opengl.postprocessing.util.Numeric;

/**
 * Class representing a visual 'bar' of widgets (a 'tweakbar'). This class shows
 * a contained group of user-interface widgets specific to the given
 * application, for directly interacting with core variables controlling the
 * application's functionality.
 * 
 * @author Nvidia 2014-9-13 16:55
 * 
 */
public class NvTweakBar extends NvUIContainer {

	/**
	 * This is a predefined base action code value for any codes
	 * defined/allocated inside of the NvTweakBar system itself.
	 */
	public static final int TWEAKBAR_ACTIONCODE_BASE = 0x43210000;
	private static float NVB_FONT_TO_HEIGHT_FACTOR;
	private static float NVB_HEIGHT_TO_FONT_FACTOR;
	
	private static boolean debugForceMenu = false;
	
	// this is the max items in an enum where we use radio group, else we switch to a popup menu.
	// normally 3, can reduce to 2 for testing menus easier, or increase higher to prevent auto-popups.
	private static final int ENUM_COUNT_THRESH_FOR_COMPACT_MENU = 3;
	
	static{
		if(NvUtils.SHOW_PRESSED_BUTTON){
			NVB_FONT_TO_HEIGHT_FACTOR = 2.0f;
			NVB_HEIGHT_TO_FONT_FACTOR = 0.5f * 1.1f;
		}else{
			NVB_FONT_TO_HEIGHT_FACTOR = 1.5f;
			NVB_HEIGHT_TO_FONT_FACTOR = 0.66667f * 1.1f;
		}
	}
	float m_lastElX;
	float m_lastElY;
	float m_xInset;
	float m_widthPadFactor = 0.1f;  // !!!!!TBD TODO picking padding that seems decent on L/R sides.
	float m_padHeight;
	int m_lastActionCode = TWEAKBAR_ACTIONCODE_BASE;  // so automatic codes are high up.
	int m_defaultRows = 15 /*In desktop this value is 20*/; //!!!!TBD TODO just picked a number that seems good atm.
	boolean m_textShadows = true;
	boolean m_compactLayout;

	float m_subgroupLastY;
	float m_subgroupMaxY;
	NvUIContainer m_subgroupContainer;
	NvTweakVarBase m_subgroupSwitchVar;

	int m_labelColor;
	int m_valueColor;

	/**
	 * Static method to create an NvTweakBar attached to a window. This method
	 * helps you to creates an instance of NvTweakBar that is properly connected
	 * up with the provided NvUIWindow.
	 * 
	 * @param window
	 *            The NvUIWindow to attach to.
	 */
	public static NvTweakBar createTweakBar(NvUIWindow window) {
		// !!!!TODO how to set width properly? Stupid design.
	    float bw = window.getWidth() * 0.3f;
	    float bh = window.getHeight();

	    NvTweakBar twb = new NvTweakBar(bw, bh);

	    // add it to the passed in window/container.
	    window.add(twb, 10, 0);

	    return twb;
	}

	/**
	 * Normal constructor.
	 * <p>
	 * Note the constructor should not need to be manually called unless you are
	 * creating a very customized user interface which is not relying on
	 * NvSampleApp, nor CreateTweakBar to link up with an NvUIWindow and
	 * auto-size the tweakbar to fit reasonably.
	 * 
	 * @param width
	 *            The width of the bar in pixels
	 * @param height
	 *            The height of the bar in pixels
	 */
	public NvTweakBar(float width, float height) {
		super(width, height, new NvUIGraphicFrame("info_text_box_thin.dds", 24, 24));
		
		m_canFocus = true;
		
		// trying a slightly mid-cyan color for labels.
	    m_labelColor = Numeric.makeRGBA(64,192,192,255);
	    // keep value white until we can pass through to buttons and others
	    m_valueColor = Numeric.makeRGBA(255,255,255,255);

	    m_lastElX = 0 + (getWidth() - getDefaultLineWidth())/2;
	    m_lastElY = getStartOffY();

	    // can't do this in constructor, order of ops not guaranteed somehow...
	    m_padHeight = getDefaultLineHeight()*0.5f;

	// !!!TBD Hack to make it a little more see-through for the moment...
//	    if (m_background)
//	        m_background.setAlpha(0.75f);

	    setConsumeClicks(true);
	    setFocusHilite(makeFocusFrame());
	}

	/**
	 * Note the destructor should not need to be manually called unless you are
	 * creating a custom user interface not relying on NvSampleApp.
	 */
	@Override
	public void dispose() {
	}

	/** Accessor to retrieve the default height of one row in the Tweakbar. */
	public float getDefaultLineHeight() {
		return getHeight() / m_defaultRows;
	}

	/** Accessor to retrieve the default width of a row in the Tweakbar. */
	public float getDefaultLineWidth() {
		return (getWidth() - (2 * m_widthPadFactor * getWidth()) - m_xInset);
	}

	/**
	 * Accessor to retrieve the starting vertical offset of elements in the
	 * Tweakbar. Primary use of this method is so external controls can be
	 * aligned with the first line of the Tweakbar, for visual continuity.
	 */
	public float getStartOffY() {
		return getDefaultLineHeight() * 0.5f;
	}

	/**
	 * Method to handle results of user interaction with widgets.
	 * 
	 * @param react
	 *            The reaction structure to process/handle.
	 */
	public int handleReaction(NvUIReaction react) {
		if (react.code==TWEAKBAR_ACTIONCODE_BASE)
	    {
	        setVisibility((react.state>0));
	        //return nvuiEventHandled;
	    }
		
		return super.handleReaction(react);
	}
	
	private void addElement(NvUIElement te, boolean autoSpace/*=true*/){
		NvUIRect mr = te.getScreenRect();
		
		if (m_subgroupContainer != null)
	    {
	        m_subgroupContainer.add(te, m_padHeight, m_subgroupLastY);
	        if (autoSpace)
	            m_subgroupLastY += mr.height;
	    }
	    else
	    {
	        super.add(te, m_lastElX+m_xInset, m_lastElY);
	        if (autoSpace)
	            m_lastElY += mr.height;
	    }
	}

	/**
	 * Method to handle the effect of window/screen resize.
	 * 
	 * @param w
	 *            The new view width.
	 * @param h
	 *            The new view height.
	 */
	public void handleReshape(float w, float h) {
		super.handleReshape(w, h);
		
		// what should a tweakbar do to adjust its component layout???
	    // !!!!!TBD TODO TODO TODO
	    // for now, just resize the background HEIGHT...
	    m_background.setHeight( h);
	}

	/**
	 * Method to notify Tweakbar widgets of possible changes to tracked
	 * variables.
	 * <p>
	 * This method is called to let the Tweakbar know that some outside system
	 * has (potentially) changed the underlying values of variables that
	 * Tweakbar widgets are also watching/controlling via NvTweakVars, and thus
	 * all widgets should go and refresh themselves.
	 */
	public void syncValues() {
		NvUIReaction react = getReactionEdit(true);
		react.code = 0; // match all.
	    react.flags = NvReactFlag.FORCE_UPDATE;
	    handleReaction(react);
	}

	/**
	 * Method to notify Tweakbar widgets of changes to a specific
	 * NvTweakVarBase.
	 * <p>
	 * This method is called to let the Tweakbar know that some outside system
	 * has (potentially) changed the value of a specific variable that Tweakbar
	 * widgets are watching, and thus matching widgets should refresh
	 * themselves.
	 */
	public void syncValue(NvTweakVarBase var) {
		NvUIReaction react = getReactionEdit(true);
		react.code = var.getActionCode();
	    react.flags = NvReactFlag.FORCE_UPDATE;
	    handleReaction(react);
	}
	
	private NvUIGraphic makeFocusFrame(){
		NvUIGraphicFrame frame = new NvUIGraphicFrame("rounding.dds", 4);
		frame.setDrawCenter(false);
//	    frame.setAlpha(0.5f);
	    frame.setColor(Numeric.makeRGBA(0x60, 0xFF, 0xFF, 0xFF));
	    return frame;
	}
	
	private NvUIPopup makeStdPopup(String name, NvTweakEnumVari refvar, NvTweakEnumi[] values, int actionCode /*=0*/){
		int code = actionCode==0?++m_lastActionCode:actionCode; // need to stash code so all radios use the same
	    float wide = getDefaultLineWidth();
	    float high = getDefaultLineHeight();
	    float fontFactor = NVB_HEIGHT_TO_FONT_FACTOR;    

	    NvUIButton btn = null;
	    NvUIGraphicFrame frame;
	    NvUIElement[] els = new NvUIElement[3];

	    els[2] = null;

	    els[0] = new NvUIGraphic("arrow_blue_down.dds", 0, 0);
	    els[0].setDimensions(high/2, high/2);
	    els[1] = new NvUIGraphic("arrow_pressed_down.dds", 0, 0);
	    els[1].setDimensions(high/2, high/2);
	    btn = new NvUIButton(NvUIButtonType.CHECK, code, els);

	    // first, the button styling
	    frame = new NvUIGraphicFrame("frame_thin.dds", 10);
	    frame.setAlpha(1.5f);
	    frame.setDimensions(wide, high);
	    els[0] = frame;
	    frame = new NvUIGraphicFrame("frame_thin.dds", 10);
	    frame.setAlpha(1.8f);
	    frame.setDimensions(wide, high);
	    frame.setColor(Numeric.makeRGBA(0xFF, 0xE0, 0x50, 0xFF)); // yellow
	    els[1] = frame;

	    // then, the popup menu framing
	    frame = new NvUIGraphicFrame("frame_thin.dds", 10);
	    frame.setAlpha(1.8f);

	    NvUIPopup dd = new NvUIPopup(code, els, frame, btn, name, high*fontFactor, true);
	    for (int i=0; i<values.length; i++)
	    {
	        frame = new NvUIGraphicFrame("icon_button_highlight_small.dds", 8);
	        frame.setDimensions(wide, high);
	        frame.setAlpha(0);
	        els[0] = frame;
	        frame = new NvUIGraphicFrame("icon_button_highlight_small.dds", 8);
	        frame.setDimensions(wide, high);
	        frame.setAlpha(0.5f);
	        frame.setColor(Numeric.makeRGBA(0xFF, 0xE0, 0x50, 0xFF)); // yellow
	        els[1] = frame;

	        btn = new NvUIButton(NvUIButtonType.PUSH, code, els, values[i].m_name, (high*fontFactor), true);
	        btn.setSubCode(i); // !!!!TBD should this be set inside AddItem??
	        btn.setHitMargin(high*0.05f, high*0.05f);
	        btn.setSlideInteractGroup(code);
	        //btn.setDrawState(0);
	        //btn.setDimensions(wide, high); // reset for check/radio buttons...

	        NvTweakEnumUIi twui = new NvTweakEnumUIi(refvar, i, btn, code);
	        dd.addItem(twui, values[i].m_name, values[i].m_value);
	    }

	    // refvar will convert to uint32 value for passing to SetItemValueActive
	    dd.setActiveItemValue(refvar.get());

	    // add focus frame to the popup menu
	    NvUIPopupMenu mnu = dd.getUIPopupMenu();
	    if(NvUtils.later){
		    frame = new NvUIGraphicFrame("frame_thin.dds", 10);
		    frame.setAlpha(0.5f);
		    frame.setColor(Numeric.makeRGBA(0xFF, 0xE0, 0x50, 0xFF)); // yellow
	    }
	    mnu.setFocusHilite(makeFocusFrame());

	    return dd;
	}
	
	private NvUIButton makeStdButton(String name, boolean val, int btntype/*= CHECK*/, int code/*=0*/, int subcode/*=0*/ ){
		int actionCode = code==0?++m_lastActionCode:code;
	    NvUIButton btn = null;
	    float wide = getDefaultLineWidth() * 1.2f;
	    float high = getDefaultLineHeight() * 1.2f;
	    float fontFactor = NVB_HEIGHT_TO_FONT_FACTOR;    
	    NvUIElement[] els = new NvUIElement[3];

	    if (btntype== NvUIButtonType.PUSH)
	    {
	        els[0] = new NvUIGraphicFrame("btn_round_blue.dds", 18, 18);
	        els[0].setDimensions(wide, high);
	        els[1] = new NvUIGraphicFrame("btn_round_pressed.dds", 18, 18);
	        els[1].setDimensions(wide, high);
	        els[2] = null;
	    }
	    else
	    if (btntype== NvUIButtonType.CHECK)
	    {
	        els[0] = new NvUIGraphic("btn_box_blue.dds", 0, 0);
	        els[0].setDimensions(high, high);
	        els[1] = new NvUIGraphic("btn_box_pressed_x.dds", 0, 0);
	        els[1].setDimensions(high, high);
	        els[2] = null;
	    }
	    else // radio
	    {
	        els[0] = new NvUIGraphicFrame("button_top_row.dds", 24);
	        els[0].setDimensions(high*0.8f, high);
	        els[1] = new NvUIGraphicFrame("button_top_row_pressed.dds", 24);
	        els[1].setDimensions(high*0.8f, high);
	        els[2] = null;
	    }

	    btn = new NvUIButton(btntype, actionCode, els, name, (high*fontFactor), m_textShadows);
	    btn.setSubCode(subcode);
	    btn.setHitMargin(high*0.05f, high*0.05f);
	    btn.setDrawState(val ? 1 : 0);
	    btn.setDimensions(wide, high); // reset for check/radio buttons...
	        
	    return btn;
	}
	
	private NvUISlider makeStdSlider(String name, float val, float min, float max, float step, boolean integral, int code/*==0*/){
		int actionCode = code==0?++m_lastActionCode:code;
	    float wide = getDefaultLineWidth();
	    float high = getDefaultLineHeight();

	    // !!!TBD TODO 
	    // text and output value update should be embedded in the slider itself...
	  
	    // this logic should all move into valuebar.
	/* !!!!TBD
	    if (m_compactLayout)
	    {
	        out.setDimensions(wide*0.2f, high);
	        text.setDimensions(wide*0.3f, high);
	    }
	*/

	    NvUIGraphicFrame emptybar = new NvUIGraphicFrame("slider_empty.dds", 8);
	    NvUIGraphicFrame fullbar = new NvUIGraphicFrame("slider_full.dds", 8);
	    fullbar.setAlpha(0.6f); // make the fill a bit see-through.
	    fullbar.setColor(Numeric.makeRGBA(0xFF, 0xE0, 0x50, 0xFF)); // yellow

	    NvUIGraphic thumb = new NvUIGraphic("slider_thumb.dds", 0, 0);
	    thumb.setDimensions(high*0.5f, high*1.25f);

	    NvUISlider sld = new NvUISlider(emptybar, fullbar, thumb, actionCode);
	    sld.setSmoothScrolling(true);
	    sld.setIntegral(integral);
	    sld.setMaxValue(max);
	    sld.setMinValue(min);
	    sld.setStepValue(step);
	    sld.setValue(val);
	    sld.setDimensions(wide, high/4);
	    sld.setHitMargin(high/4, high/4); // !!!!TBD TODO

	    return sld;
	}

	/**
	 * Method to request the Tweakbar try to use more compact versions of
	 * widgets, and spacing between widgets, when possible.
	 * 
	 * @param compact
	 *            Pass in true to enable compact layout logic.
	 */
	public void setCompactLayout(boolean compact) {
		m_compactLayout = compact;
	}

	/**
	 * Method to request automatic dropshadows for all Tweakbar text.
	 * 
	 * @param b
	 *            Pass true to enable automatic dropshadows. Default is false.
	 */
	public void textShadows(boolean b) {
		m_textShadows = b;
	}

	/**
	 * Method to request the start of a set of widgets be grouped together.
	 */
	public void subgroupStart() {
		m_xInset += getDefaultLineHeight();
	}

	/**
	 * Method to note the end of a grouped set of widgets.
	 */
	public void subgroupEnd() {
		m_xInset -= getDefaultLineHeight();
	}

	/**
	 * Method to request the start of a group of widgets, whose visibility is
	 * controlled by the value of a given NvTweakVarBase being tracked. This
	 * method starts a very special grouping of widgets, working like a 'case'
	 * statement, where each case/subgroup is only visible when the tracked
	 * variable matches the value of the given subgroup's case.
	 * <p>
	 * An example use might be something like the following:
	 * 
	 * <pre>
	 * // A temporary variable to hold the current NvTweakVarBase we're referencing.
	 * NvTweakVarBase *var;
	 * 
	 * // Add a variable to be managed by the tweakbar, and get the returned pointer
	 * var = mTweakBar->addValue("Auto LOD", mLod);
	 * 
	 * // Start a 'switch' based on the returned variable
	 * mTweakBar->subgroupSwitchStart(var);
	 * 
	 * // Handle the switch case for 'true'
	 * mTweakBar->subgroupSwitchCase(true);
	 * 
	 * // Add a variable to the current switch case.
	 * var = mTweakBar->addValue("Triangle size", mParams.triSize, 1.0f, 50.0f);
	 * 
	 * // Handle the switch case for 'false'
	 * mTweakBar->subgroupSwitchCase(false);
	 * 
	 * // Add a variable to the current switch case.
	 * var = mTweakBar->addValue("Inner tessellation factor", mParams.innerTessFactor, 1.0f, 64.0f);
	 * // Add another variable to the current case.
	 * var = mTweakBar->addValue("Outer tessellation factor", mParams.outerTessFactor, 1.0f, 64.0f);
	 * 
	 * // Close/end the switch group.
	 * mTweakBar->subgroupSwitchEnd();
	 * </pre>
	 * 
	 * @param var
	 *            The NvTweakVarBase object to track and switch visibility upon.
	 */
	public void subgroupSwitchStart(NvTweakVarBase var) {
		m_subgroupSwitchVar = var;
		m_subgroupMaxY = 0;
	    // TBD background !!!!!TBD TODO

	    // this tells us:
	    // 1) what container to add to
	    // 2) what the starting top-left is of the NEXT switch container
	    // ... but still need to track the HEIGHT max of all switches
	}

	/**
	 * Method for setting a case for a boolean tracked variable, with the value
	 * for the specific case.
	 * 
	 * @param val
	 *            The boolean case value for this subgroup/block.
	 */
	public void subgroupSwitchCase(boolean val) {
		subgroupSwitchCase(new NvTweakSwitchContainerbool(getDefaultLineWidth()+(2*m_padHeight), m_subgroupSwitchVar, val, null));
	}

	/**
	 * Method for setting a case for a floating-point tracked variable, with the
	 * value for the specific case to match upon.
	 * 
	 * @param val
	 *            The floating-point case value for this subgroup/block.
	 */
	public void subgroupSwitchCase(float val) {
		subgroupSwitchCase(new NvTweakSwitchContainerf(getDefaultLineWidth()+(2*m_padHeight), m_subgroupSwitchVar, val, null));
	}

	/**
	 * Method for setting a case for an integral tracked variable, with the
	 * value for the specific case to match upon.
	 * 
	 * @param val
	 *            The integral case value for this subgroup/block.
	 */
	public void subgroupSwitchCase(int val) {
		subgroupSwitchCase(new NvTweakSwitchContaineri(getDefaultLineWidth()+(2*m_padHeight), m_subgroupSwitchVar, val, null));
	}

	/** Method to note the end of the current subgroup 'switch' block. */
	public void subgroupSwitchEnd() {
		subgroupClose();

	    // bump lastElY by max subgroup height...
	    m_lastElY += m_subgroupMaxY;

	    // null out any control variables.
	    m_subgroupSwitchVar = null;
	}

	/**
	 * Method to set up a container for switching.
	 */
	private void subgroupSwitchCase(NvUIContainer c) {
		subgroupClose();
	    m_subgroupContainer = c;
	    NvUIGraphicFrame bg = new NvUIGraphicFrame("frame_thin.dds", 4);
	    bg.setAlpha(0.2f);
	    m_subgroupContainer.setBackground(bg);
	    m_subgroupContainer.setFocusHilite(makeFocusFrame());
	    super.add(m_subgroupContainer, m_lastElX+m_xInset-m_padHeight, m_lastElY);
	}

	/**
	 * Method to close the current subgroup/case.
	 */
	private void subgroupClose() {
		if (m_subgroupContainer!=null) // do we have a case already?
	    {
	        m_subgroupLastY += m_padHeight;
	        m_subgroupContainer.setHeight(m_subgroupLastY);
	        if (m_subgroupMaxY < m_subgroupLastY)
	            m_subgroupMaxY = m_subgroupLastY;
	    }
	    m_subgroupLastY = m_padHeight;
	    m_subgroupContainer = null;
	}

	/**
	 * Requests vertical padding be added prior to the next widget or subgroup.
	 * 
	 * @param multiplier
	 *            [optional] A factor to apply to the default padding amount.
	 *            Defaults to 1.0.
	 */
	public void addPadding(float multiplier/*=1*/) {
		if (m_subgroupContainer != null)
	        m_subgroupLastY += multiplier*m_padHeight;
	    else
	        m_lastElY += multiplier*m_padHeight;
	}
	
	/**
	 * Add a unit distance to the vertical padding for the next widget or subgroup. 
	 */
	public void addPadding(){
		addPadding(1.0f);
	}

	/** Add a static text label to the tweakbar. */
	public void addLabel(String title, boolean bold/*==false*/) {
		float dpsize = getDefaultLineHeight()*NVB_HEIGHT_TO_FONT_FACTOR * 1.25f;//*(small?0.85f:1.0f);
	    String s = bold? NvBftStyle.NVBF_STYLESTR_BOLD:"";
//	    s.append(title);
	    s += title;
	    NvUIText text = new NvUIText(s, NvUIFontFamily.DEFAULT, dpsize, NvUITextAlign.LEFT);
	    text.setColor(m_labelColor);
	    if (m_textShadows) text.setShadow();
	    addElement(text, true);
	    // bottom padding
//	    m_lastElY += (GetDefaultLineHeight()-text->GetHeight()); //*(small?0.5f:1.0f); // !!!TBD padding difference
	}
	
	/** Add a static text label to the tweakbar. */
	public void addLabel(String title) {
		addLabel(title, false);
	}

	// !!!!TBD TODO addValue that takes a string ref so
	// we can do dynamic labels controlled by the app.

	/** Add a textual readout for a float variable. */
	public NvTweakVarBase addValueReadout(String name, FieldControl var, float max /* =0 */, int code /* =0 */) {
		final float wide = getDefaultLineWidth();
	    final float high = getDefaultLineHeight();
	    int actionCode = (code==0) ? ++m_lastActionCode : code;
	    NvTweakVarf tvar = new NvTweakVarf(var, name, null);
	    tvar.setActionCode(actionCode);
	    NvUIValueText vtxt = new NvUIValueText(name, NvUIFontFamily.DEFAULT, (high*NVB_HEIGHT_TO_FONT_FACTOR), NvUITextAlign.LEFT,
	                                            (Float)var.getValue(), (max>100 ? 1 : (max>10 ? 2 : 3)), NvUITextAlign.RIGHT, actionCode);
	    vtxt.setWidth(wide); // override default string width...
	    vtxt.setColor(m_labelColor);
	    if (m_textShadows) vtxt.setShadow();
	    NvTweakVarUIProxyBase te = new NvTweakVarUIf(tvar, vtxt, actionCode);
	    te.setReadOnly(true);
	    addElement(te, true);
//	    AddElement(vtxt, !m_compactLayout);
	    return tvar;
	}

	/** Add a textual readout for a uint variable. */
	public NvTweakVarBase addValueReadout(String name, FieldControl var, int code /* =0 */) {
		final float wide = getDefaultLineWidth();
	    final float high = getDefaultLineHeight();
	    int actionCode = (code==0) ? ++m_lastActionCode : code;
	    NvTweakVari tvar = new NvTweakVari(var, name, null);
	    tvar.setActionCode(actionCode);
	    NvUIValueText vtxt = new NvUIValueText(name, NvUIFontFamily.DEFAULT, (high*NVB_HEIGHT_TO_FONT_FACTOR), NvUITextAlign.LEFT,
	                                            (Integer)var.getValue(), NvUITextAlign.RIGHT, actionCode);
	    vtxt.setWidth(wide); // override default string width...
	    vtxt.setColor(m_labelColor);
	    if (m_textShadows) vtxt.setShadow();
	    NvTweakVarUIProxyBase te = new NvTweakVarUIi(tvar, vtxt, actionCode);
	    te.setReadOnly(true);
	    addElement(te, true);
	    return tvar;
	}

	/**
	 * Add a textual readout for a pre-created floating-point NvTweakVar
	 * variable.
	 */
	public NvTweakVarBase addValueReadout(NvTweakVarf tvar, float max) {
		final float wide = getDefaultLineWidth();
	    final float high = getDefaultLineHeight();
	    NvUIValueText vtxt = new NvUIValueText(tvar.getName(), NvUIFontFamily.DEFAULT, (high*NVB_HEIGHT_TO_FONT_FACTOR), NvUITextAlign.LEFT,
	                                            tvar.get(), ((max>100) ? 1 : ((max>10) ? 2 : 3)), NvUITextAlign.RIGHT, tvar.getActionCode());
	    vtxt.setWidth(wide); // override default string width...
	    vtxt.setColor(m_labelColor);
	    if (m_textShadows) vtxt.setShadow();
	    // put value color back to white if based on tvar as that's a slider normally.
	    vtxt.setValueColor(Numeric.NV_PC_PREDEF_WHITE);
	    NvTweakVarUIProxyBase te = new NvTweakVarUIf(tvar, vtxt, tvar.getActionCode());
	    te.setReadOnly(true);
	    addElement(te, true);
	    return tvar;
	}

	/** Add a textual readout for a pre-created integral NvTweakVar variable. */
	public NvTweakVarBase addValueReadout(NvTweakVari tvar) {
		final float wide = getDefaultLineWidth();
	    final float high = getDefaultLineHeight();
	    NvUIValueText vtxt = new NvUIValueText(tvar.getName(), NvUIFontFamily.DEFAULT, (high*NVB_HEIGHT_TO_FONT_FACTOR), NvUITextAlign.LEFT,
	                                            tvar.get(), NvUITextAlign.RIGHT, tvar.getActionCode());
	    vtxt.setWidth(wide); // override default string width...
	    vtxt.setColor(m_labelColor);
	    if (m_textShadows) vtxt.setShadow();
	    // put value color back to white if based on tvar as that's a slider normally.
	    vtxt.setValueColor(Numeric.NV_PC_PREDEF_WHITE);
	    NvTweakVarUIProxyBase te = new NvTweakVarUIi(tvar, vtxt, tvar.getActionCode());
	    te.setReadOnly(true);
	    addElement(te, true);
	    return tvar;
	}

	// buttons/bools
	/**
	 * Add a bool variable to be controlled by the user.
	 * <p>
	 * This method adds the given bool variable to be tracked by the system and
	 * controlled by the user. By default, the variable is bound to a checkbox
	 * interface (so users can toggle between true and false), but passing true
	 * for the @p pushButton parameter will bind it to a push-button instead,
	 * only asserting a true value (relying on the application to handle and
	 * then reset the variable to false).
	 */
	public NvTweakVarBase addValue(String name, FieldControl var, boolean pushButton /* =false */, int actionCode/* =0 */) {
		// make a NvTweakVar on the fly. this will leak currently.
	    NvTweakVarbool tvar = new NvTweakVarbool(var, name, null);
	    NvUIButton btn = makeStdButton(name, (Boolean)var.getValue(), pushButton? NvUIButtonType.PUSH: NvUIButtonType.CHECK, actionCode, 1);
	    addElement(new NvTweakVarUIbool(tvar, btn, btn.getActionCode()), true);
	    return tvar;
	}
	
	/**
	 * Add a bool variable to be controlled by the user.
	 * <p>
	 * This method adds the given bool variable to be tracked by the system and
	 * controlled by the user. By default, the variable is bound to a checkbox
	 * interface (so users can toggle between true and false).
	 */
	public NvTweakVarBase addValue(String name, FieldControl var) {
		return addValue(name, var, false, 0);
	}

	/**
	 * Add an action-only push-button to the Tweakbar.
	 * <p>
	 * This method adds a push-button to the Tweakbar that is not tied to an
	 * application variable. Instead, it simply generates the given action code
	 * for a normal NvUIReaction handling by the application -- and as such, the
	 * actionCode parameter is required unlike other 'add' methods. This is not
	 * optional.
	 */
	public NvTweakVarBase addButton(String name, int actionCode) {
		// make a NvTweakVar on the fly. this will leak currently.
	    NvTweakVarbool tvar = new NvTweakVarbool(name, null); // make self-referencing by not passing a value.
	    NvUIButton btn = makeStdButton(name, false, NvUIButtonType.PUSH, actionCode, 1);
	    addElement(new NvTweakVarUIbool(tvar, btn, btn.getActionCode()), true);
	    return tvar;
	}

	// scalars/sliders
	/**
	 * Add a floating-point variable to the Tweakbar as a slider.
	 * <p>
	 * This method adds a slider to the Tweakbar, tracking the supplied
	 * floating-point variable.
	 */
	public NvTweakVarBase addValue(String name, FieldControl var, float min, float max, float step /* =0 */, int actionCode /* =0 */) {
		// make a NvTweakVar on the fly. this will leak currently.
	    NvTweakVarf tvar = new NvTweakVarf(var, name, min, max, step, null);
	    NvUISlider sld = makeStdSlider(name, (Float)var.getValue(), min, max, step, false, actionCode);
	    tvar.setActionCode(sld.getActionCode());
	    NvUIElement te = new NvTweakVarUIf(tvar, sld, sld.getActionCode());

	    addValueReadout(tvar, max);
	    addPadding(1.0f);
	    addElement(te, true);
	    if (m_compactLayout)
	    {
	        NvUIRect tr = te.getScreenRect();
	        te.setOrigin(tr.left + te.getWidth()*0.45f, tr.top-(0.4f*te.getHeight()));
	        te.setDimensions(te.getWidth()*0.35f, te.getHeight());
	    }
	    addPadding(1.0f);
	    return tvar;
	}
	
	/**
	 * Add a floating-point variable to the Tweakbar as a slider.
	 * <p>
	 * This method adds a slider to the Tweakbar, tracking the supplied
	 * floating-point variable.
	 */
	public NvTweakVarBase addValue(String name, FieldControl var, float min, float max){
		return addValue(name, var, min, max, 0, 0);
	}

	/**
	 * Add an integral variable to the Tweakbar as a slider.
	 * <p>
	 * This method adds a slider to the Tweakbar, tracking the supplied
	 * integer variable.
	 */
	public NvTweakVarBase addValue(String name, FieldControl var, int min, int max, int step /* =0 */, int actionCode /* =0 */) {
		// make a NvTweakVar on the fly. this will leak currently.
	    NvTweakVari tvar = new NvTweakVari(var, name, min, max, step, null);
	    NvUISlider sld = makeStdSlider(name, (Integer)var.getValue(), (float)min, (float)max, (float)step, true, actionCode);
	    tvar.setActionCode(sld.getActionCode()); // link the var with the code...
	    NvUIElement te = new NvTweakVarUIi(tvar, sld, sld.getActionCode());

	    addValueReadout(tvar);
	    addPadding(1.0f);
	    addElement(te, true);
	    if (m_compactLayout)
	    {
	        NvUIRect tr = te.getScreenRect();
	        te.setOrigin(tr.left + te.getWidth()*0.45f, tr.top-(0.4f*te.getHeight()));
	        te.setDimensions(te.getWidth()*0.35f, te.getHeight());
	    }
	    addPadding(1.0f);

	    return tvar;
	}
	
	/**
	 * Add an integral variable to the Tweakbar as a slider.
	 * <p>
	 * This method adds a slider to the Tweakbar, tracking the supplied
	 * integer variable.
	 * @see #addValue(String, FieldControl, int, int, int, int)
	 */
	public NvTweakVarBase addValue(String name, FieldControl var, int min, int max){
		return addValue(name, var, min, max, 1, 0);
	}

	// enums/radios/menus
	/**
	 * Add an integral variable to the Tweakbar as an radio group or dropdown
	 * menu with an enumerated set of values.
	 * <p>
	 * This method tracks and modifies the supplied variable using the passed in
	 * array of NvTweakEnum string-value pairs as individual items. It will
	 * create a radio-button group for two or three enum values, and a dropdown
	 * menu for four or more values.
	 */
	public NvTweakVarBase addEnum(String name, FieldControl var, NvTweakEnumi[] values, int actionCode /* =0 */) {
		final int valueCount = values.length;
		if (debugForceMenu || (m_compactLayout && valueCount > ENUM_COUNT_THRESH_FOR_COMPACT_MENU)) {
	        return addMenu(name, var, values, actionCode);
	    }

	    int minval = Integer.MAX_VALUE, maxval = 0;
	    for (int i=0; i<valueCount; i++)
	    {
	        if (minval>values[i].m_value)
	            minval = values[i].m_value;
	        if (maxval<values[i].m_value)
	            maxval = values[i].m_value;
	    }

	    // make a NvTweakVar on the fly. this will leak currently.
	    NvTweakEnumVari tvar = new NvTweakEnumVari(values, var, name, minval, maxval, 0, null);
	    tvar.setValLoop(true);

	    // add label
	    addLabel(name, false);

	    // for now, inset AFTER the label.
	    subgroupStart();

	    // add group, currently as radiobuttons, could do as popup in future.
	    int code = actionCode==0?++m_lastActionCode:actionCode; // need to stash code so all radios use the same
	    for (int i=0; i<valueCount; i++)
	    {
	        NvUIButton btn = makeStdButton(values[i].m_name, (values[i].get()==(Integer)var.getValue()), NvUIButtonType.RADIO, code, i);
	        addElement(new NvTweakEnumUIi(tvar, i, btn, btn.getActionCode()), true);
	    }

	    subgroupEnd();

	    return tvar;
	}

	/**
	 * Add an integral variable to the Tweakbar as a dropdown menu with an
	 * enumerated set of values. This method tracks and modifies the supplied
	 * variable using the passed in array of NvTweakEnum string-value pairs as
	 * the menu items in a dropdown menu.
	 */
	public NvTweakVarBase addMenu(String name, FieldControl var, NvTweakEnumi[] values, int actionCode /* =0 */) {
		final int valueCount = values.length;
		int minval = Integer.MAX_VALUE, maxval = 0;
	    for (int i=0; i<valueCount; i++)
	    {
	        if (minval>values[i].m_value)
	            minval = values[i].m_value;
	        if (maxval<values[i].m_value)
	            maxval = values[i].m_value;
	    }

	    // make a NvTweakVar on the fly. this will leak currently.
	    NvTweakEnumVari tvar = new NvTweakEnumVari(values, var, name, minval, maxval, 0, null);
	    tvar.setValLoop(true);

	    NvUIPopup el = makeStdPopup(name, tvar, values, actionCode);
	    addElement(el, true);
	    el.setParent(this); // point to our container so we can pop-up.

	    return tvar;
	}
	
	private static class NvTweakContainer extends NvUIContainer {
		public NvTweakContainer(float width, float height, NvUIGraphic bg) {
			super(width, height, bg);
			
			m_canFocus = true; // !!!!TBD TODO should do custom CanFocus override, which iterates to see if any children can take focus.
	        m_canMoveFocus = true;
	        m_showFocus = false; // we don't want to show outer focus.
		}
	}
	
	private static class NvTweakSwitchContainerf extends NvTweakContainer{
		private float m_baseVal;
		private int m_actionCode;
		
		public NvTweakSwitchContainerf(float width, NvTweakVarBase tvar, float val, NvUIGraphic bg) {
			super(width, 0, bg);
			m_baseVal = val;
			m_actionCode = tvar.getActionCode();
			
			setVisibility(tvar.equals(val));
		}
		
		@Override
		public int handleReaction(NvUIReaction react) {
			super.handleReaction(react);
			
			if ( (react.code != 0 && (react.code==m_actionCode))
			        || (react.flags & NvReactFlag.FORCE_UPDATE)!=0 )
			    {
			        // float TweakVar stashed value in fval in HandleReaction
			        setVisibility((m_baseVal == react.fval)); 
			    }
			    return nvuiEventNotHandled;
		}
	}
	
	private final static class NvTweakSwitchContaineri extends NvTweakContainer{
		private int m_baseVal;
		private int m_actionCode;
		
		public NvTweakSwitchContaineri(float width, NvTweakVarBase tvar, int val, NvUIGraphic bg) {
			super(width, 0, bg);
			m_baseVal = val;
			m_actionCode = tvar.getActionCode();
			
			setVisibility(tvar.equals(val));
		}
		
		@Override
		public int handleReaction(NvUIReaction react) {
			super.handleReaction(react);
			
			if ( (react.code != 0 && (react.code==m_actionCode))
			        || (react.flags & NvReactFlag.FORCE_UPDATE)!=0 )
			    {
			        // uint TweakVar stashed value in ival in HandleReaction
			        setVisibility((m_baseVal == react.ival)); 
			    }
			    return nvuiEventNotHandled;
		}
	}
	
	private final static class NvTweakSwitchContainerbool extends NvTweakContainer{
		private boolean m_baseVal;
		private int m_actionCode;
		
		public NvTweakSwitchContainerbool(float width, NvTweakVarBase tvar, boolean val, NvUIGraphic bg) {
			super(width, 0, bg);
			m_baseVal = val;
			m_actionCode = tvar.getActionCode();
			
			setVisibility(tvar.equals(val));
		}
		
		@Override
		public int handleReaction(NvUIReaction react) {
			super.handleReaction(react);
			
			if ( (react.code != 0 && (react.code==m_actionCode))
			        || (react.flags & NvReactFlag.FORCE_UPDATE)!=0 )
			    {
			        // boolean TweakVar stashed value in ival in HandleReaction
			        setVisibility(m_baseVal == (react.state > 0)); 
			    }
			    return nvuiEventNotHandled;
		}
	}
}

//----------------------------------------------------------------------------------
// File:        NvSampleApp.java
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
package com.nvidia.developer.opengl.app;

import com.nvidia.developer.opengl.ui.NvFocusEvent;
import com.nvidia.developer.opengl.ui.NvGestureEvent;
import com.nvidia.developer.opengl.ui.NvGestureKind;
import com.nvidia.developer.opengl.ui.NvInputEventClass;
import com.nvidia.developer.opengl.ui.NvReactFlag;
import com.nvidia.developer.opengl.ui.NvTweakBar;
import com.nvidia.developer.opengl.ui.NvTweakBind;
import com.nvidia.developer.opengl.ui.NvTweakVarBase;
import com.nvidia.developer.opengl.ui.NvUIButton;
import com.nvidia.developer.opengl.ui.NvUIButtonType;
import com.nvidia.developer.opengl.ui.NvUIDrawState;
import com.nvidia.developer.opengl.ui.NvUIElement;
import com.nvidia.developer.opengl.ui.NvUIEventResponse;
import com.nvidia.developer.opengl.ui.NvUIFontFamily;
import com.nvidia.developer.opengl.ui.NvUIGraphic;
import com.nvidia.developer.opengl.ui.NvUIReaction;
import com.nvidia.developer.opengl.ui.NvUITextAlign;
import com.nvidia.developer.opengl.ui.NvUIValueText;
import com.nvidia.developer.opengl.ui.NvUIWindow;
import com.nvidia.developer.opengl.utils.FieldControl;
import com.nvidia.developer.opengl.utils.NvGfxAPIVersion;
import com.nvidia.developer.opengl.utils.NvImage;
import com.nvidia.developer.opengl.utils.NvStopWatch;

import java.awt.Dimension;
import java.util.HashMap;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.util.LogUtil;
import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.postprocessing.util.StringUtils;

/**
 * Base class for sample apps.
 * <p>
 * Adds numerous features to NvAppBase that are of use to most or all sample
 * apps
 * 
 * @author Nvidia 2014-9-13 12:32
 */
public class NvSampleApp extends NvAppBase {

	private static final int TEST_MODE_ISSUE_NONE = 0;
	private static final int TEST_MODE_FBO_ISSUE = 1;
	private static final int TESTMODE_WARMUP_FRAMES = 10;
	protected NvFramerateCounter mFramerate;
	protected float mFrameDelta;
	protected final NvStopWatch mFrameTimer = new NvStopWatch();

	// no used
	protected final NvStopWatch mAutoRepeatTimer = new NvStopWatch();
	protected boolean mAutoRepeatButton;
	protected boolean mAutoRepeatTriggered;

	protected NvUIWindow mUIWindow;
	protected NvUIValueText mFPSText;
	protected NvTweakBar mTweakBar;
	protected NvUIButton mTweakTab;

	protected final NvInputTransformer m_transformer = new NvInputTransformer();
	protected NvInputHandler m_inputHandler;

	protected final HashMap<Integer, NvTweakBind> mKeyBinds = new HashMap<Integer, NvTweakBind>();
	protected final HashMap<Integer, NvTweakBind> mButtonBinds = new HashMap<Integer, NvTweakBind>();
	
	private float totalTime;

	protected FieldControl createControl(String varName){
		return new FieldControl(this, varName, FieldControl.CALL_FIELD);
	}
	
	protected static FieldControl createControl(String varName, Object obj){
		return new FieldControl(obj, varName, FieldControl.CALL_FIELD);
	}
	
//	protected NvUIWindow mUIWindow;
	public NvUIValueText getFPSUIText(){ return mFPSText;}
	
	public final void onCreate() {
	    // check extensions and enable DXT expansion if needed
		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
	    boolean hasDXT = gl.isSupportExt("GL_EXT_texture_compression_s3tc") ||
				gl.isSupportExt("GL_EXT_texture_compression_dxt1");
	    if (!hasDXT) {
			LogUtil.i(LogUtil.LogType.NV_FRAMEWROK, "Device has no DXT texture support - enabling DXT expansion");
	        NvImage.setDXTExpansion(true);
	    }

	    mFramerate = new NvFramerateCounter();
	    mFrameTimer.start();
	    
		super.onCreate();
	    baseInitUI();
	}
	
	private int testModeFrames;
	private NvStopWatch testModeTimer = new NvStopWatch();
	
	public void setInputHandler(NvInputHandler inputHandler) { m_inputHandler = inputHandler; }
	public NvInputHandler getInputHandler() { return m_inputHandler; }
	
	@Override
	public final void draw() {
		mFrameTimer.stop();

		mFrameDelta = mFrameTimer.getTime();
		// just an estimate
		totalTime += mFrameDelta;
		
		if (m_inputHandler != null)
		{
			m_inputHandler.update(mFrameDelta);
		}
		else
		{
			m_transformer.update(mFrameDelta);
		}
		
        mFrameTimer.reset();
        
        if(!isExiting()){
        	mFrameTimer.start();

            if (mAutoRepeatButton) {
                final float elapsed = mAutoRepeatTimer.getTime();
                if ( (!mAutoRepeatTriggered && elapsed >= 0.5f) ||
                     (mAutoRepeatTriggered && elapsed >= 0.04f) ) { // 25hz repeat
                    mAutoRepeatTriggered = true;
                    gamepadButtonChanged(1, true);
                }
            }

			update(getFrameDeltaTime());

            display();
			baseDrawUI();
            
            if (mFramerate.nextFrame()) {
                // for now, disabling console output of fps as we have on-screen.
                // makes it easier to read USEFUL log output messages.
//                NvLogger.i("fps: %.2f", mFramerate.getMeanFramerate());
            }
        }

	}

	private void baseInitUI(){
	    if (mUIWindow == null){
	        final int w = getGLContext().width(), h = getGLContext().height();
	        mUIWindow = new NvUIWindow((float)w, (float)h);
	        mFPSText = new NvUIValueText("", NvUIFontFamily.SANS, w/40.0f, NvUITextAlign.RIGHT, 0.0f, 1, NvUITextAlign.RIGHT, 0);
	        mFPSText.setColor(Numeric.makeRGBA(0x30,0xD0,0xD0,0xB0));
	        mFPSText.setShadow();
	        mUIWindow.add(mFPSText, (float)w-8, 0);

	        if (mTweakBar==null) {
	            mTweakBar = NvTweakBar.createTweakBar(mUIWindow); // adds to window internally.
	            mTweakBar.setVisibility(false);
	            
	            String mAppTitle = getGLContext().getAppTitle();
	            if (!StringUtils.isEmpty(mAppTitle)) {
	                mTweakBar.addLabel(mAppTitle, true);
	                mTweakBar.addPadding(1.0f);
	            }

	            // for now, app will own the tweakbar tab button
	            float high = mTweakBar.getDefaultLineHeight();
	            NvUIElement[] els = new NvUIElement[3];
	            els[0] = new NvUIGraphic("arrow_blue.dds", 0, 0);GLCheck.checkError();
	            els[0].setDimensions(high/2, high/2);
	            els[1] = new NvUIGraphic("arrow_blue_left.dds", 0 ,0 );GLCheck.checkError();
	            els[1].setDimensions(high/2, high/2);
	            els[2] = null;

	            mTweakTab = new NvUIButton(NvUIButtonType.CHECK, NvTweakBar.TWEAKBAR_ACTIONCODE_BASE, els);
	            mTweakTab.setHitMargin(high/2, high/2);
	            mUIWindow.add(mTweakTab, high*0.25f, mTweakBar.getStartOffY()+high*0.125f);
	        }

			GLCheck.checkError();
	    }

	    initUI();
	}

	/**
	 * UI init callback.
	 * <p>
	 * Called after rendering is initialized, to allow preparation of overlaid
	 * UI elements
	 */
	public void initUI() {
	}
	
	// Correspond the void baseReshape(int32_t w, int32_t h);
	@Override
	public final void onResize(int w, int h) {
		reshape(w, h);
		if (m_inputHandler != null)
		{
			m_inputHandler.setScreenSize(w, h);
		}
		else
		{
			m_transformer.setScreenSize(w, h);
		}

	    mUIWindow.handleReshape((float)w, (float)h);
	}
	
	private void baseDrawUI(){
		if (mUIWindow != null && mUIWindow.getVisibility()) {
	        if (mFPSText != null) {
	            mFPSText.setValue(mFramerate.getMeanFramerate());
	        }
	        long time = 0;
	        NvUIDrawState ds = new NvUIDrawState(time, getGLContext().width(), getGLContext().height());
	        mUIWindow.draw(ds);
	    }

	    drawUI();
	}
	
	private void baseHandleReaction(){
		int r;
		NvUIReaction react = NvUIElement.getReaction();
		// we let the UI handle any reaction first, in case there
	    // are interesting side-effects such as updating variables...
	    r = mUIWindow.handleReaction(react);
	    // then the app is always given a look, even if already handled...
	    //if (r==nvuiEventNotHandled)
	    r = handleReaction(react);
	}

	/**
	 * App-specific UI drawing callback.
	 * <p>
	 * Called to request the app render any UI elements over the frame.
	 */
	public void drawUI() {
	}

	/**
	 * Get UI window.
	 * <p>
	 * 
	 * @return a pointer to the UI window
	 */
	public NvUIWindow getUIWindow() {
		return mUIWindow;
	}

	/**
	 * Get the framerate counter.
	 * <p>
	 * The NvSampleApp initializes and updates an NvFramerateCounter in its
	 * mainloop implementation. It also draws it to the screen. The application
	 * may gain access if it wishes to get the data for its own use.
	 * 
	 * @return a pointer to the framerate counter object
	 */
	public NvFramerateCounter getFramerate() {
		return mFramerate;
	}

	/**
	 * Extension requirement declaration.
	 * <p>
	 * Allow an app to declare an extension as "required".
	 * 
	 * @param ext
	 *            the extension name to be required
	 * @param exitOnFailure
	 *            if true, then {@link #errorExit} is called to indicate the issue and
	 *            exit
	 * @return true if the extension string is exported and false if it is not
	 */
	public boolean requireExtension(String ext, boolean exitOnFailure/* = true */) {
		if (!GLFuncProviderFactory.getGLFuncProvider().isSupportExt(ext)) {
	        if (exitOnFailure) {
	            String caption = ("The current system does not appear to support the extension ")
	                + (ext) + (", which is required by the sample.  "+
	                "This is likely because the system's GPU or driver does not support the extension.  "+
	                "Please see the sample's source code for details");
	            errorExit(caption);
	        }

	        return false;
	    }

	    return true;
	}
	
	/**
	 * Extension requirement declaration.
	 * <p>
	 * Allow an app to declare an extension as "required". Return false the app will exit.
	 * 
	 * @param ext
	 *            the extension name to be required
	 * @return true if the extension string is exported and false if it is not
	 */
	public boolean requireExtension(String ext) {
		return requireExtension(ext, true);
	}

	/**
	 * GL Minimum API requirement declaration.
	 * 
	 * @param minApi
	 *            the minimum API that is required
	 * @param exitOnFailure
	 *            if true, then errorExit is called to indicate the issue and
	 *            exit
	 * @return true if the platform's GL[ES] API version is at least as new as
	 *         the given minApi. Otherwise, returns false
	 */
	public boolean requireMinAPIVersion(NvGfxAPIVersion minApi, boolean exitOnFailure /* = true */) {
		NvGfxAPIVersion api = getGLContext().getConfiguration().apiVer;
		
		if(api.compareTo(minApi) < 0){
			if (exitOnFailure) {
				String caption = String.format("The current system does not appear to support the minimum GL API required "+
		                "by the sample (requested: %s %d.%d, got: %s %d.%d).  This is likely because the system's GPU or driver "+
		                "does not support the API.  Please see the sample's source code for details", 
		                (minApi.isGLES) ? "GLES" : "GL", 
		                minApi.majVersion, minApi.minVersion,
		                (api.isGLES) ? "GLES" : "GL", api.majVersion, api.minVersion);
				errorExit(caption);
			}
			
			return false;
		}
		
		return true;
	}
	
	/**
	 * GL Minimum API requirement declaration.<p>
	 * When the platform's GL[ES] API version is less the specified version, the program will exit.
	 * 
	 * @param minApi
	 *            the minimum API that is required
	 * @return true if the platform's GL[ES] API version is at least as new as
	 *         the given minApi. Otherwise, returns false
	 */
	public boolean requireMinAPIVersion(NvGfxAPIVersion minApi) {
		return requireMinAPIVersion(minApi, true);
	}

	/**
	 * Exit with indication of an error
	 * <p>
	 * Exits in all cases; in normal mode it shows the string in a dialog and
	 * exits In test mode, it writes the string to the log file and exits
	 * 
	 * @param errorString
	 *            a null-terminated string indicating the error
	 */
	public void errorExit(String errorString) {
		// we set the flag here manually.  The exit will not happen until
		// the user closes the dialog.  But we want to act as if we are
		// already exiting (which we are), so we do not render
		m_requestedExit = true;
		getGLContext().showDialog("Fatal Error", errorString);
		getGLContext().requestExit();
	}

	/**
	 * Frame delta time.
	 * <p>
	 * 
	 * @return the time since the last frame in seconds
	 */
	public float getFrameDeltaTime() {
		return mFrameDelta;
	}

	/**
	 * Key binding. Adds a key binding.
	 * 
	 * @param var
	 *            the tweak variable to be bound
	 * @param incKey
	 *            the key to be bound to increment the tweak variable
	 * @param decKey
	 *            the key to be bound to decrement the tweak variable
	 */
	public void addTweakKeyBind(NvTweakVarBase var, int incKey, int decKey/* =0 */) {
//		mKeyBinds[incKey] = NvTweakBind(NvTweakCmd.INCREMENT, var);
		mKeyBinds.put(incKey, new NvTweakBind(NvTweakBind.INCREMENT, var));
	    if (decKey != 0)
//	        mKeyBinds[decKey] = NvTweakBind(NvTweakCmd.DECREMENT, var);
	    	mKeyBinds.put(decKey, new NvTweakBind(NvTweakBind.DECREMENT, var));
	}
	
	/**
	 * Key binding. Adds a key binding.
	 * 
	 * @param var
	 *            the tweak variable to be bound
	 * @param incKey
	 *            the key to be bound to increment the tweak variable
	 * @see #addTweakKeyBind(NvTweakVarBase, int, int)
	 */
	public void addTweakKeyBind(NvTweakVarBase var, int incKey) {
		mKeyBinds.put(incKey, new NvTweakBind(NvTweakBind.INCREMENT, var));
	}
	
	public final boolean keyInput(int code, NvKeyActionType action){
		// only do down and repeat for now.
	    if (NvKeyActionType.UP!=action) {
	        NvTweakBind bind = mKeyBinds.get(code);
	        if (bind != null) {
	            // we have a binding.  do something with it.
	            NvTweakVarBase var = bind.mVar;
	            if (var != null) {
	                switch (bind.mCmd) {
	                    case NvTweakBind.RESET:
	                        var.reset();
	                        break;
	                    case NvTweakBind.INCREMENT:
	                        var.increment();
	                        break;
	                    case NvTweakBind.DECREMENT:
	                        var.decrement();
	                        break;
	                    default:
	                        return false;
	                }

	                syncValue(var);
	                // we're done.
	                return true;
	            }
	        }
	    }

	    if (mTweakBar != null && NvKeyActionType.UP!=action) // handle down+repeat as needed
	    {
	        // would be nice if this was some pluggable class we could add/remove more easily like inputtransformer.
	        int r = NvUIEventResponse.nvuiEventNotHandled;
	        switch(code)
	        {
	            case NvKey.K_TAB: {
	                if (NvKeyActionType.DOWN!=action) break; // we don't want autorepeat...
	                NvUIReaction react = NvUIElement.getReactionEdit(true);
	                react.code = NvTweakBar.TWEAKBAR_ACTIONCODE_BASE;
	                react.state = mTweakBar.getVisibility() ? 0 : 1;
	                r = NvUIEventResponse.nvuiEventHandledReaction;
	                break;
	            }
	            case NvKey.K_ARROW_DOWN: {
	                if (NvKeyActionType.DOWN!=action) break; // we don't want autorepeat...
	                r = mUIWindow.handleFocusEvent(NvFocusEvent.MOVE_DOWN);
	                break;
	            }
	            case NvKey.K_ARROW_UP: {
	                if (NvKeyActionType.DOWN!=action) break; // we don't want autorepeat...
	                r = mUIWindow.handleFocusEvent(NvFocusEvent.MOVE_UP);
	                break;
	            }
	            case NvKey.K_ENTER: {
	                if (NvKeyActionType.DOWN!=action) break; // we don't want autorepeat...
	                r = mUIWindow.handleFocusEvent(NvFocusEvent.ACT_PRESS);
	                break;
	            }
	            case NvKey.K_BACKSPACE: {
	                if (NvKeyActionType.DOWN!=action) break; // we don't want autorepeat...
	                r = mUIWindow.handleFocusEvent(NvFocusEvent.FOCUS_CLEAR);
	                break;
	            }
	            case NvKey.K_ARROW_LEFT: {
	                r = mUIWindow.handleFocusEvent(NvFocusEvent.ACT_DEC);
	                break;
	            }
	            case NvKey.K_ARROW_RIGHT: {
	                r = mUIWindow.handleFocusEvent(NvFocusEvent.ACT_INC);
	                break;
	            }
	            default:
	                break;
	        }

	        if ((r&NvUIEventResponse.nvuiEventHandled) != 0)
	        {
	            if ((r&NvUIEventResponse.nvuiEventHadReaction)!=0)
	                baseHandleReaction();
	            return true;
	        }
	    }
	        
	    if (handleKeyInput(code, action))
	        return true;

	    // give last shot to transformer.
	 // give last shot to transformer.
		if (m_inputHandler != null)
		{
			return m_inputHandler.processKey(code, action);
		}
		else
		{
			return m_transformer.processKey(code, action);
		}
	}
	
	@Override
	public final boolean characterInput(char c) {
		 if (handleCharacterInput(c))
		     return true;
		 return false;
	}

	/**
	 * Gamepad Button binding. Adds a button binding.
	 * 
	 * @param var
	 *            the tweak variable to be bound
	 * @param incBtn
	 *            the button to be bound to increment the tweak variable
	 * @param decBtn
	 *            the button to be bound to decrement the tweak variable
	 */
	public void addTweakButtonBind(NvTweakVarBase var, int incBtn, int decBtn/*=0*/) {
		mButtonBinds.put(incBtn, new NvTweakBind(NvTweakBind.INCREMENT, var));
		if(decBtn != 0)
			mButtonBinds.put(decBtn, new NvTweakBind(NvTweakBind.DECREMENT, var));
	}
	
	@Override
	public boolean gamepadChanged(int changedPadFlags) {
		return super.gamepadChanged(changedPadFlags);
	}
	
	protected boolean gamepadButtonChanged(int button, boolean down){
		// unimplemented
		
		return false;
	}

	/**
	 * Window size request.
	 * <p>
	 * Allows the app to change the default window size.
	 * <p>
	 * While an app can override this, it is NOT recommended, as the base class
	 * parses the command line arguments to set the window size. Applications
	 * wishing to override this should call the base class version and return
	 * without changing the values if the base class returns true.
	 * <p>
	 * Application must return true if it changes the width or height passed in
	 * Not all platforms can support setting the window size. These platforms
	 * will not call this function
	 * <p>
	 * Most apps should be resolution-agnostic and be able to run at a given
	 * resolution
	 * 
	 * @param size
	 *            the default size is passed in. If the application wishes to
	 *            reuqest it be changed, it should change the value before
	 *            returning true
	 * @return whether the value has been changed. true if changed, false if not
	 */
	public boolean getRequestedWindowSize(Dimension size) {
		return false;
	}

	protected int handleReaction(NvUIReaction react) {
		return NvUIEventResponse.nvuiEventNotHandled;
	}

	/**
	 * Request to update any UI related to a given NvTweakVarBase Allows the
	 * framework to abstract the process by which we call HandleReaction to
	 * notify all the UI elements that a particular variable being tracked has
	 * had some kind of update.
	 * 
	 * @param var
	 *            the variable that changed
	 */
	public void syncValue(NvTweakVarBase var) {
		NvUIReaction react = NvUIElement.getReactionEdit(true);
	    react.code = var.getActionCode();
	    react.flags = NvReactFlag.FORCE_UPDATE;
	    baseHandleReaction();
	}
	
	private boolean isDown = false;
	private float startX = 0, startY = 0;
	
	public final boolean pointerInput(NvInputDeviceType device, int action, int modifiers, int count, NvPointerEvent[] points) {
		long time = 0;
//		    static bool isDown = false;
//		    static float startX = 0, startY = 0;
		boolean isButtonEvent = (action==NvPointerActionType.DOWN)||(action==NvPointerActionType.UP);
		if (isButtonEvent)
			isDown = (action==NvPointerActionType.DOWN);

		if (mUIWindow!= null) {
			int giclass = NvInputEventClass.MOUSE; // default to mouse
			int gikind;
			// override for non-mouse device.
			if (device==NvInputDeviceType.STYLUS)
				giclass = NvInputEventClass.STYLUS;
			else if (device==NvInputDeviceType.TOUCH)
				giclass = NvInputEventClass.TOUCH;
			// since not using a heavyweight gesture detection system,
			// determine reasonable kind/state to pass along here.
			if (isButtonEvent)
				gikind = (isDown ? NvGestureKind.PRESS : NvGestureKind.RELEASE);
			else
				gikind = (isDown ? NvGestureKind.DRAG : NvGestureKind.HOVER);
			float x=0, y=0;
			if (count != 0)
			{
				x = points[0].m_x;
				y = points[0].m_y;
			}
			NvGestureEvent gesture = new NvGestureEvent(giclass, gikind, x, y);
			if (isButtonEvent)
			{
				if (isDown)
				{
					startX = x;
					startY = y;
				}
			}
			else if (isDown)
			{
				gesture.x = startX;
				gesture.y = startY;
				gesture.dx = x - startX;
				gesture.dy = y - startY;
			}
			int r = mUIWindow.handleEvent(gesture, time, null);
			if ((r&NvUIEventResponse.nvuiEventHandled) != 0)
			{
				if ((r&NvUIEventResponse.nvuiEventHadReaction) != 0)
					baseHandleReaction();
				return true;
			}
		}

		if (handlePointerInput(device, action, modifiers, count, points))
			return true;

		if (m_inputHandler != null)
		{
			return m_inputHandler.processPointer(device, action, modifiers, count, points);
		}
		else
		{
			return m_transformer.processPointer(device, action, modifiers, count, points);
		}
	}

	public NvTweakBar getTweakBar() { return mTweakBar; }
	public NvInputTransformer getInputTransformer() { return m_transformer;}
}

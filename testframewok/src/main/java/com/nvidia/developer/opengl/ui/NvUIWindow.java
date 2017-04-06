//----------------------------------------------------------------------------------
// File:        NvUIWindow.java
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

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;

/**
 * A single, top-level container that can manage all NvUIElements in the view.
 * <p>
 * Most importantly, the <code>NvUIWindow</code> class automatically takes care
 * of calling {@link #handleReaction(NvUIReaction)} on its children after processing the
 * {@link #handleEvent(NvGestureEvent, long, NvUIElement)}.
 * 
 * @author Nvidia 2014-9-11 0:02
 * 
 */
public class NvUIWindow extends NvUIContainer {

	/**
	 * Default constructor, takes starting window/viewport width/height. Also
	 * responsible for initializing the NvUIText system with the same
	 * dimensions.
	 */
	public NvUIWindow(float width, float height) {
		super(width, height, null);
		
		NvUIText.staticInit(width, height);
		systemResChange((int)width, (int)height);
	}
	
	@Override
	public void dispose() {
		NvUIText.staticCleanup();
	}

	/**
	 * Overrides and calls normal @p HandleEvent, with pointer to @p this as
	 * element with focus.
	 */
	public int handleEvent(NvGestureEvent ev, long timeUST,
			NvUIElement hasInteract) {
		if (ev.kind > NvGestureKind.HOVER && hasFocus())
	        dropFocus();
	    // !!!!TBD note we ignore hasInteract, and just pass in the window as the top level focus holder.
	    int r = super.handleEvent(ev, timeUST, this);
	    return r;
	}

	/**
	 * Responsible for ensuring things get appropriate resized, in as much as
	 * that is possible given current NvUI system design constraints.
	 */
	public void handleReshape(float w, float h) {
		// !!!!!TBD TODO !!!!!TBD TODO
	    // resize container, notify contents
	    // update UIText/BitFont of view size change as needed.

	    super.handleReshape(w, h);

	    // most containers won't just resize, but we're THE WINDOW, we are the VIEW SIZE.
	    setDimensions(w, h);

	    // this changes our design w/h statics inside UIElement, so we MUST DO THIS LAST,
	    // so that children can compare current design w/h vs incoming new w/h...
	    // in the future with gravity positioning, and relative coord spaces, might not be necessary.
	    // !!!!TBD TODO for the moment, use SystemResChange to do what we want for text and similar systems.
	    systemResChange((int)w, (int)h);
	}

	/**
	 * We override to ensure we save and restore outside drawing state around
	 * the UI calls.
	 */
	public void draw(NvUIDrawState drawState) {
		if (!m_isVisible) return;

	    // !!!!TBD TODO we should have a separate helper utility
	    // for saving and restoring state.
	    // and another for caching filtered state so we don't
	    // redundantly set in all the UI rendering bits.

//        NvBFText.saveGLState();   // TODO may be have problem.

		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
		gl.glDisable(GLenum.GL_STENCIL_TEST);
		gl.glDisable(GLenum.GL_CULL_FACE);
		gl.glDisable(GLenum.GL_DEPTH_TEST);

	    super.draw(drawState);

//	    NvBFRestoreGLState();
//	    NvBFText.restoreGLState(); // TODO may be have problem.
	}
}

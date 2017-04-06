//----------------------------------------------------------------------------------
// File:        NvTweakVarUIProxyBase.java
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
 * An object that will help link an NvTweakVar with a proxied NvUIElement. This
 * class acts as the basis for NvTweakVarUI, connecting up a tweakable app
 * variable up with a particular NvUI widget class appropriate for interacting
 * with that variable -- such as buttons and sliders.
 * 
 * @author Nvidia 2014-9-13 16:51
 * 
 */
public class NvTweakVarUIProxyBase extends NvUIProxy {

	protected boolean m_readonly;

	/** Default constructor, takes the UI element we proxy to. */
	public NvTweakVarUIProxyBase(NvUIElement m_proxy) {
		super(m_proxy);
	}

	public void setReadOnly(boolean ro) {
		m_readonly = ro;
		m_canFocus = !ro;
	}

	// !!!TODO may want to override drawstate to only draw inactive state,
	// etc....

	/**
	 * We override HandleEvent to short-circuit any tweaks of read-only
	 * variables.
	 */
	public int handleEvent(NvGestureEvent ev, long timeUST,
			NvUIElement hasInteract) {
		if (m_readonly)
			return nvuiEventNotHandled;
		return super.handleEvent(ev, timeUST, hasInteract);
	}

	/**
	 * We override HandleEvent to short-circuit any tweaks of read-only
	 * variables.
	 */
	public int handleReaction(NvUIReaction react) {
		// if (m_readonly)
		// return nvuiEventNotHandled;
		return super.handleReaction(react);
	}
}

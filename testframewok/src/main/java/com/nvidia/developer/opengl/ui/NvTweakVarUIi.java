//----------------------------------------------------------------------------------
// File:        NvTweakVarUIi.java
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
 * A templated object that links NvTweakVar of some datatype with an appropriate
 * UI widget. This class connects a tweakable app variable, a specific
 * NvTweakVar template instance, with a UI widget appropriate for changing the
 * value of that variable, linked by an action code shared between the systems.
 * 
 * @author Nvidia 2014-9-13 20:54
 * 
 */
public class NvTweakVarUIi extends NvTweakVarUIProxyBase {

	/**
	 * The variable we hold a reference to in order to adjust its value based on
	 * the input/reaction from the user interacting with our proxied UI widget.
	 */
	protected NvTweakVari m_tvar;

	public NvTweakVarUIi(NvTweakVari tvar, NvUIElement el, int actionCode /* =0 */) {
		super(el);

		m_tvar = tvar;
		m_tvar.setActionCode(actionCode);
	}

	/**
	 * We override HandleReaction so that when there is an NvUIReaction passing
	 * through the system containing a value change for us or from our proxied
	 * UI, we can intercept and set the value of our NvTweakVar appropriately.
	 */
	@Override
	public int handleReaction(NvUIReaction react) {
		if (react.code != 0 && (react.code!=m_tvar.getActionCode()))
	        return nvuiEventNotHandled; // not a message for us.

	    if ((react.flags & NvReactFlag.FORCE_UPDATE) != 0)
	    {
	        NvUIReaction change = getReactionEdit(false); // false to not clear it!!!
	        change.ival = m_tvar.get(); // update to what's stored in the variable.
	    }

	    super.handleReaction(react);

	    int r = nvuiEventNotHandled;
	    if (   (react.uid==getUID()) // we always listen to our own sent messages.
	        || (react.code!= 0 && (react.code==m_tvar.getActionCode())) // we always listen to our action code
	        || (react.code == 0 && (react.flags & NvReactFlag.FORCE_UPDATE) != 0) ) // we listen to force-update only if NO action code
	    {
	        if (m_tvar.get() != react.ival)
	        {
	            m_tvar.set(react.ival); // float TweakVar stashed value in fval in HandleReaction
	            return nvuiEventHandled; // !!!!TBD TODO do we eat it here?
	        }
	    }

	    return r;
	}
	
	protected int superHandleReaction(NvUIReaction react){
		return super.handleReaction(react);
	}
}

//----------------------------------------------------------------------------------
// File:        NvAppBase/NvInputHandler.h
// SDK Version: v3.00 
// Email:       gameworks@nvidia.com
// Site:        http://developer.nvidia.com/
//
// Copyright (c) 2014-2015, NVIDIA CORPORATION. All rights reserved.
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

/**
 * Maps touch, mouse and gamepad into useful state that can be used to drive
 * various application state, including camera or object interaction
 */
public abstract class NvInputHandler {
	protected int m_width;
	protected int m_height;
	
	/**
	 * Reset all input values to default/starting points, restoring initial view.
	 */
    public abstract void reset();

    /**
     * Set screen size. <p>
     * Set the current screen size, so that the touch/mouse mapping methods
     * scale correctly.  Failure to keep this up to date with the screen size
     * can lead to wildly incorrect mouse/touch mappings
     * @param width the new window/screen width
     * @param height the new window/screen height
     */
    public void setScreenSize(int width, int height) { m_width = width; m_height = height; }

    /**
     * Update the state based on the current inputs, velocities, and delta time.
     * @param deltaTime the time since the last call to #update, in seconds
     */
    public abstract void update(float deltaTime);

    /**
     * Pointer event input.<p>
     * Used to pass pointer input events to the handler.  The signature explicitly matches
     * that of the input callbacks used to provide input to an app or app framework for ease
     * of calling
     * @param device the input device
     * @param action the input action
     * @param modifiers the input modifiers
     * @param count the number of elements in the #points array
     * @param points the input event points
     * @return true if the event was used and "eaten" by the input transformer and should
     * not be processed by any other input system.
     */
    protected abstract boolean processPointer(NvInputDeviceType device, int action,int modifiers, int count, NvPointerEvent[] points);

    /// 
    /// Used to pass gamepad state to the transformer.  The signature explicitly matches
    /// that of the input callbacks used to provide input to an app or app framework for ease
    /// of calling
    /// \param[in] changedPadFlags the changes pad flags
    /// \param[in] pad the gamepad device
    /// \return true if the event was used and "eaten" by the input transformer and should
    /// not be processed by any other input system.
//    public abstract boolean processGamepad(uint32_t changedPadFlags, NvGamepad& pad) = 0;

    /**
     * Key event input.<p>
     * Used to pass key input events to the transformer.  The signature explicitly matches
     * that of the input callbacks used to provide input to an app or app framework for ease
     * of calling
     * @param code the input keycode
     * @param action the input action
     * @return true if the event was used and "eaten" by the input transformer and should not be processed by any other input system.
     */
    protected abstract boolean processKey(int code,  NvKeyActionType action);
}

//----------------------------------------------------------------------------------
// File:        NvGestureEvent.java
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

import jet.opengl.postprocessing.util.Numeric;

/**
 * Object packaging up all the data related to an input event by the user.
 * @author Nvidia 2014-9-4
 *
 */
public final class NvGestureEvent {

	static final int NV_GESTURE_UID_INVALID = 0xFFFFFFFF;
	static final int NV_GESTURE_UID_MAX = 0xFFFFEEEE;
	
	private static int lastID;
	
	private static int internalGetNextUID(){
		long lastid = Numeric.unsignedInt(lastID);
		lastid++;
        if (lastid > Numeric.unsignedInt(NV_GESTURE_UID_MAX)) // some arbitrary cap...
        	lastid = 1; // do NOT reset to 0, it's special...
        
        lastID = (int)lastid;
        return lastID;
	}
	
	/** A unique ID for this event -- generally an auto-incrementing counter. */
	public int uid;
	/** The kind of event that occurred. ref to NvGestureKind.*/
	public int kind;
	/** The input device that generated the event: mouse, finger, stylus, etc... ref to NvInputEventClass*/
	public int type;
	/**
	 * new fields, matching the user input struct, for button index and other flags<br>
	 * Storing mouse button, gamepad button, or key identifier for event.<p>
	 * 
	 * This is a unsigned char type.
	 */
	public short index;
	/** x, y position at the START of the gesture. */
	public float x, y;
	/**
	 * Delta x, y value for the gesture.<p>
	 * These are overloaded, different gestures will interpret as different values.<br>
	 * could be things like:<ul>
	 * <li> DRAG: delta position
	 * <li> FLICK: velocities
	 * <li> ZOOM: second finger
	 * </ul>
	 */
	public float dx, dy;
	
	/** General constructor.
	    @param intype The input device generating the event. This is enum value defined in the NvInputEventClass
	    @param inkind The kind of event. This is enum value defined in the NvGestureKind
	    @param inx The starting x position
	    @param inx The starting y position
	 */
	public NvGestureEvent(int intype, int inkind, float inx, float iny) {
		if (inkind == NvGestureKind.PRESS) // change UID only at press
            uid = internalGetNextUID();
        else // keep existing ID
            uid = (int) lastID;
        kind = inkind;
        type = intype;
        index = 0;
        //flags = 0;
        x = inx;
        y = iny;
        dx = 0;
        dy = 0;
	}
}

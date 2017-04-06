//----------------------------------------------------------------------------------
// File:        NvTweakVarBase.java
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
 * This is an abstract base class for indirectly referencing app variables.
 * @author Nvidia 2014-9-13 16:40
 */
public abstract class NvTweakVarBase {

	/** A human-readable name/title of the variable. */
	protected String mName;
	/** An informative 'help' string for the variable. */
	protected String mDesc;
	/** A unique value for signalling changes across systems. */
	protected int mActionCode;
	
	/**
	 * Base constructor.<p>
	 * Note that the base constructor defaults mActionCode to 0, expecting
     * subclass constructors to set a meaningful, unique value afterward.
	 * @param name A title for this variable.
	 * @param description description An OPTIONAL help string.
	 */
	protected NvTweakVarBase(String name, String description) {
		mName = name;
		mDesc = description;
	}
	
	public abstract void increment();
    public abstract void decrement();
    public abstract void reset();
    
    public abstract boolean equals(boolean val);
    public abstract boolean equals(float val);
    public abstract boolean equals(int val);
    
    /** Accessor to retrieve pointer to the name string. */
    public String getName() { return mName; }
    /** Accessor to retrieve pointer to the description string. */
    public String getDesc() { return mDesc; }

    /** Set the action code value for this variable. */
    public void setActionCode(int code) { mActionCode = code; }
    /** Accessor to retrieve the action code value. */
    public int getActionCode() { return mActionCode; }
}

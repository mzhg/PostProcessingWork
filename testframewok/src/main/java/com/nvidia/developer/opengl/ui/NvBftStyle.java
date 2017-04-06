//----------------------------------------------------------------------------------
// File:        NvBftStyle.java
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
 * BitFont supports 'styles' via embedded character codes and string literals.<p>
 * You can directly use the following as character values for switching
 * between the top and bottom 'halves' of a 'split' font files.  This is
 * currently used as 'normal' and 'bold' styles, but the two halves could
 * actually be set up as different typeface families.  There are also matching
 * string literals for equivalent use directly in C quoted string composition.
 * @author Nvidia 2014-9-8 22:22
 *
 */
public interface NvBftStyle {

	/** Sets further text to normal style. */
	static final int NORMAL = 0x10;
	/** Sets further text to bold style, for fonts loaded with bold support. */
	static final int BOLD = 0x11;
	/** Used for programmatic range checking of embedded codes. */
	static final int MAX = 0x12;
	
	/** BitFont string literal for style reset to 'normal'. */
	static final String NVBF_STYLESTR_NORMAL = "\020";
	/** BitFont string literal to style further characters 'bold'. */
	static final String NVBF_STYLESTR_BOLD   = "\021";
}

//----------------------------------------------------------------------------------
// File:        NvBftColorCode.java
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
 * BitFont supports specific color constants embedded in a string.<p>
 * You can directly use the following as character values for switching
 * color 'runs' in the text.  Note that these embedded color changes will
 * completely override the base color specified for a given string.<p>
 * There are also string literals for use directly in C quoted string composition.
 * @author Nvidia 2014-9-9 9:45
 *
 */
public interface NvBftColorCode {

	/** Sets further text to be white. */
	static final int NvBF_COLORCODE_WHITE = 0;
	/** Sets further text to be gray. */
	static final int NvBF_COLORCODE_GRAY  = 1;
	/** Sets further text to be black. */
	static final int NvBF_COLORCODE_BLACK = 2;
	/** Sets further text to be red. */
	static final int NvBF_COLORCODE_RED   = 3; 
	/** Sets further text to be green. */
	static final int NvBF_COLORCODE_GREEN = 4;
	/** Sets further text to be blue. */
	static final int NvBF_COLORCODE_BLUE  = 5;
	/** Used for programmatic range checking of embedded codes. */
	static final int NvBF_COLORCODE_MAX   = 6; 
	
	/** Embedded string literal to change text coloring to white. */
	static final String NvBF_COLORSTR_WHITE  =   "\001";
	/** Embedded string literal to change text coloring to gray. */
	static final String NvBF_COLORSTR_GRAY   =   "\002";
	/** Embedded string literal to change text coloring to black. */
	static final String NvBF_COLORSTR_BLACK  =   "\003";
	/** Embedded string literal to change text coloring to red. */
	static final String NvBF_COLORSTR_RED    =   "\004";
	/** Embedded string literal to change text coloring to green. */
	static final String NvBF_COLORSTR_GREEN  =   "\005";
	/** Embedded string literal to change text coloring to blue. */
	static final String NvBF_COLORSTR_BLUE   =   "\006";
	/** Embedded string literal to restore text coloring to 'normal'. should be 'max' value. */
	static final String NvBF_COLORSTR_NORMAL =   "\007";
}

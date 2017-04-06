//----------------------------------------------------------------------------------
// File:        NvGfxAPIVersion.java
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
package com.nvidia.developer.opengl.utils;
public enum NvGfxAPIVersion implements Comparable<NvGfxAPIVersion>{
    GLES2(2,0, true), GLES3_0(3,0, true), GLES3_1(3,1, true),
	GLES3_2(3, 2, true),
    GL4(4,0, false), GL4_1(4,1, false), GL4_3(4,3, false),
    GL4_4(4,4, false);
	
	/** The major version (X.0) */
	public final int majVersion;
	/** The minor version (0.Y) */
	public final int minVersion;
	/** True represent this version is OpenGL ES. */
	public final boolean isGLES;
	
	public static NvGfxAPIVersion queryVersion(boolean isES, int major, int minor){
		if(isES){
			if(major == 2 && minor == 0){
					return GLES2;
			}else if(major == 3){
				if(minor == 0)
					return GLES3_0;
				else if(minor == 1)
					return GLES3_1;
				else if(minor == 2)
					return GLES3_2;
			}
		}else{
			if(major == 4){
				if(minor == 0)
					return GL4;
				else if(minor == 1)
					return GL4_1;
				else if(minor == 3)
					return GL4_3;
				else if(minor == 4){
					return GL4_4;
				}
			}
		}
		
		throw null;
	}
	
	private NvGfxAPIVersion(int majVersion, int minVersion, boolean isGLES) {
		this.majVersion = majVersion;
		this.minVersion = minVersion;
		this.isGLES     = isGLES;
	}
	
}

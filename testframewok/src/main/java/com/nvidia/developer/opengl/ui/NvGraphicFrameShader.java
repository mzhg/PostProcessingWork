//----------------------------------------------------------------------------------
// File:        NvGraphicFrameShader.java
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

import jet.opengl.postprocessing.common.GLFuncProviderFactory;

/** This adds NvUIGraphicFrame features to the NvGraphicShader 'template'. */
public class NvGraphicFrameShader extends NvGraphicShader{

	public int m_borderIndex;
	public int m_thicknessIndex;
	public int m_texBorderIndex;
	
	/**
	 * Helper for compiling the given shader strings and then retrieving indicies.
	 * Overrides the version in NvGraphicShader, calls the inherited version first,
	 * then retrieves our additional indices.
	 */
	public void load(String vs, String fs){
		super.load(vs, fs);
		
		// inherited Load doesn't keep program enabled for 'safety', so we
	    // re-enable here so we can reference ourselves...
	    m_program.enable();

	    m_borderIndex = m_program.getAttribLocation("border", false);

	    GLFuncProviderFactory.getGLFuncProvider().glUniform1i(m_program.getUniformLocation("sampler"), 0); // texunit index zero.

	    m_thicknessIndex = m_program.getUniformLocation("thickness");
	    m_texBorderIndex = m_program.getUniformLocation("texBorder");

	    m_program.disable();
	}
}

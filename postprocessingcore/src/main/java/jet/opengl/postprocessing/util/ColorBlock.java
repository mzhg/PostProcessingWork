//----------------------------------------------------------------------------------
// File:        ColorBlock.java
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
package jet.opengl.postprocessing.util;

/** Uncompressed 4x4 color block. */
final class ColorBlock {

	private final int[] m_color = new int[4 * 4];
	
	public ColorBlock() {
	}
	
	public ColorBlock(ColorBlock block) {
		System.arraycopy(block.m_color, 0, m_color, 0, 16);
	}
	
	public int[] colors(){
		return m_color;
	}
	
	public int get(int i){
		return m_color[i];
	}
	
	public void set(int i, int c){
		m_color[i] = c;
	}
	
	public int get(int x, int y){
		return m_color[y * 4 + x];
	}
	
	public void set(int x, int y, int c){
		m_color[y * 4 + x] = c;
	}
	
	public void setAlpha(int x, int y, int alpha){
		m_color[y * 4 + x] &= 0x00FFFFFF; // first clear the alpha
		m_color[y * 4 + x] |= (alpha << 24);
	}
	
	public void setAlpha(int i, int alpha){
		m_color[i] &= 0x00FFFFFF; // first clear the alpha
		m_color[i] |= (alpha << 24);
	}
}

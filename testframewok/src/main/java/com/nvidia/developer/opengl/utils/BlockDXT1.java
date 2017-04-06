//----------------------------------------------------------------------------------
// File:        BlockDXT1.java
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

import jet.opengl.postprocessing.util.Numeric;

final class BlockDXT1 {

	public final Color16 co10 = new Color16();
	public final Color16 co11 = new Color16();
	public int indices;
	
	/** Return true if the block uses four color mode, false otherwise. */
	public boolean isFourColorMode(){
		return Numeric.unsignedShort(co10.u) > Numeric.unsignedShort(co11.u);
	}
	
	public int load(byte[] buf, int offset){
		co10.u = Numeric.getShort(buf, offset);
		offset += 2;
		co11.u = Numeric.getShort(buf, offset);
		offset += 2;
		
		indices = Numeric.getInt(buf, offset);
		offset += 4;
		
		return offset;
	}
	
	public int evaluatePalette(Color32[] color_array){
		int sr = co10.getR();
		int sg = co10.getG();
		int sb = co10.getB();
		
		int b = (sb << 3) | (sb >> 2);
		int g = (sg << 2) | (sg >> 4);
		int r = (sr << 3) | (sr >> 2);
		int a = 255;
		
		color_array[0].set(r, g, b, a);
		
		sr = co11.getR();
		sg = co11.getG();
		sb = co11.getB();
		
		b = (sb << 3) | (sb >> 2);
		g = (sg << 2) | (sg >> 4);
		r = (sr << 3) | (sr >> 2);
		
		color_array[1].set(r, g, b, a);
		
		if(Numeric.unsignedShort(co10.u) > Numeric.unsignedShort(co11.u)){
			// Four-color block: derive the other two colors.
			r = (2 * color_array[0].getR() + color_array[1].getR()) / 3;
			g = (2 * color_array[0].getG() + color_array[1].getG()) / 3;
			b = (2 * color_array[0].getB() + color_array[1].getB()) / 3;
			
			color_array[2].set(r, g, b, a);
			
			r = (2 * color_array[1].getR() + color_array[0].getR()) / 3;
			g = (2 * color_array[1].getG() + color_array[0].getG()) / 3;
			b = (2 * color_array[1].getB() + color_array[0].getB()) / 3;
			
			color_array[3].set(r, g, b, a);
			
			return 4;
		}else{
			// Three-color block: derive the other color.
			r = (color_array[0].getR() + color_array[1].getR()) / 2;
			g = (color_array[0].getG() + color_array[1].getG()) / 2;
			b = (color_array[0].getB() + color_array[1].getB()) / 2;
			color_array[2].set(r, g, b, a);
			
			// Set all components to 0 to match DXT specs.
			r = g = b = a = 0x00;
			color_array[3].set(r, g, b, a);
			
			return 3;
		}
	}
	
	public int evaluatePalette(int[] color_array){
		int sr = co10.getR();
		int sg = co10.getG();
		int sb = co10.getB();
		
		int b = (sb << 3) | (sb >> 2);
		int g = (sg << 2) | (sg >> 4);
		int r = (sr << 3) | (sr >> 2);
		int a = 255;
		
		color_array[0] = Numeric.makeRGBA(r, g, b, a);
		
		sr = co11.getR();
		sg = co11.getG();
		sb = co11.getB();
		
		b = (sb << 3) | (sb >> 2);
		g = (sg << 2) | (sg >> 4);
		r = (sr << 3) | (sr >> 2);
		
		color_array[1] = Numeric.makeRGBA(r, g, b, a);
		
		if(Numeric.unsignedShort(co10.u) > Numeric.unsignedShort(co11.u)){
			// Four-color block: derive the other two colors.
			r = (2 * Numeric.getRedFromRGB( color_array[0]) + Numeric.getRedFromRGB( color_array[1])) / 3;
			g = (2 * Numeric.getGreen(      color_array[0]) + Numeric.getGreen(      color_array[1])) / 3;
			b = (2 * Numeric.getBlueFromRGB(color_array[0]) + Numeric.getBlueFromRGB(color_array[1])) / 3;
			color_array[2] = Numeric.makeRGBA(r, g, b, a);
			
			r = (2 * Numeric.getRedFromRGB( color_array[1]) + Numeric.getRedFromRGB( color_array[2])) / 3;
			g = (2 * Numeric.getGreen(      color_array[1]) + Numeric.getGreen(      color_array[2])) / 3;
			b = (2 * Numeric.getBlueFromRGB(color_array[1]) + Numeric.getBlueFromRGB(color_array[2])) / 3;
			color_array[3] = Numeric.makeRGBA(r, g, b, a);
			
			return 4;
		}else{
			// Three-color block: derive the other color.
			r = (Numeric.getRedFromRGB( color_array[0]) + Numeric.getRedFromRGB( color_array[1])) / 2;
			g = (Numeric.getGreen(      color_array[0]) + Numeric.getGreen(      color_array[1])) / 2;
			b = (Numeric.getBlueFromRGB(color_array[0]) + Numeric.getBlueFromRGB(color_array[1])) / 2;
			color_array[2] = Numeric.makeRGBA(r, g, b, a);
			
			// Set all components to 0 to match DXT specs.
			r = g = b = a = 0x00;
			color_array[3] = 0;
			
			return 3;
		}
	}
	
	/** Evaluate palette assuming 3 color block. */
	public void evaluatePalette3(int[] color_array){
		int sr = co10.getR();
		int sg = co10.getG();
		int sb = co10.getB();
		
		int b = (sb << 3) | (sb >> 2);
		int g = (sg << 2) | (sg >> 4);
		int r = (sr << 3) | (sr >> 2);
		int a = 255;
		
		color_array[0] = Numeric.makeRGBA(r, g, b, a);
		
		sr = co11.getR();
		sg = co11.getG();
		sb = co11.getB();
		
		b = (sb << 3) | (sb >> 2);
		g = (sg << 2) | (sg >> 4);
		r = (sr << 3) | (sr >> 2);
		
		color_array[1] = Numeric.makeRGBA(r, g, b, a);
		
		// Three-color block: derive the other color.
		r = (Numeric.getRedFromRGB( color_array[0]) + Numeric.getRedFromRGB( color_array[1])) / 2;
		g = (Numeric.getGreen(      color_array[0]) + Numeric.getGreen(      color_array[1])) / 2;
		b = (Numeric.getBlueFromRGB(color_array[0]) + Numeric.getBlueFromRGB(color_array[1])) / 2;
		color_array[2] = Numeric.makeRGBA(r, g, b, a);
		
		// Set all components to 0 to match DXT specs.
		r = g = b = a = 0x00;
		color_array[3] = 0;
	}
	
	/** Evaluate palette assuming 3 color block. */
	public void evaluatePalette3(Color32[] color_array){
		int sr = co10.getR();
		int sg = co10.getG();
		int sb = co10.getB();
		
		int b = (sb << 3) | (sb >> 2);
		int g = (sg << 2) | (sg >> 4);
		int r = (sr << 3) | (sr >> 2);
		int a = 255;
		
		color_array[0].set(r, g, b, a);
		
		sr = co11.getR();
		sg = co11.getG();
		sb = co11.getB();
		
		b = (sb << 3) | (sb >> 2);
		g = (sg << 2) | (sg >> 4);
		r = (sr << 3) | (sr >> 2);
		
		color_array[1].set(r, g, b, a);
		
		// Three-color block: derive the other color.
		r = (color_array[0].getR() + color_array[1].getR()) / 2;
		g = (color_array[0].getG() + color_array[1].getG()) / 2;
		b = (color_array[0].getB() + color_array[1].getB()) / 2;
		color_array[2].set(r, g, b, a);
		
		// Set all components to 0 to match DXT specs.
		r = g = b = a = 0x00;
		color_array[3].set(r, g, b, a);
	}
	
	/** Evaluate palette assuming 4 color block. */
	public void evaluatePalette4(int[] color_array){
		int sr = co10.getR();
		int sg = co10.getG();
		int sb = co10.getB();
		
		int b = (sb << 3) | (sb >> 2);
		int g = (sg << 2) | (sg >> 4);
		int r = (sr << 3) | (sr >> 2);
		int a = 255;
		
		color_array[0] = Numeric.makeRGBA(r, g, b, a);
		
		sr = co11.getR();
		sg = co11.getG();
		sb = co11.getB();
		
		b = (sb << 3) | (sb >> 2);
		g = (sg << 2) | (sg >> 4);
		r = (sr << 3) | (sr >> 2);
		
		color_array[1] = Numeric.makeRGBA(r, g, b, a);
		
		r = (2 * Numeric.getRedFromRGB( color_array[0]) + Numeric.getRedFromRGB( color_array[1])) / 3;
		g = (2 * Numeric.getGreen(      color_array[0]) + Numeric.getGreen(      color_array[1])) / 3;
		b = (2 * Numeric.getBlueFromRGB(color_array[0]) + Numeric.getBlueFromRGB(color_array[1])) / 3;
		color_array[2] = Numeric.makeRGBA(r, g, b, a);
		
		r = (2 * Numeric.getRedFromRGB( color_array[1]) + Numeric.getRedFromRGB( color_array[0])) / 3;
		g = (2 * Numeric.getGreen(      color_array[1]) + Numeric.getGreen(      color_array[0])) / 3;
		b = (2 * Numeric.getBlueFromRGB(color_array[1]) + Numeric.getBlueFromRGB(color_array[0])) / 3;
		color_array[3] = Numeric.makeRGBA(r, g, b, a);
	}
	
	/** Evaluate palette assuming 4 color block. */
	public void evaluatePalette4(Color32[] color_array){
		int sr = co10.getR();
		int sg = co10.getG();
		int sb = co10.getB();
		
		int b = (sb << 3) | (sb >> 2);
		int g = (sg << 2) | (sg >> 4);
		int r = (sr << 3) | (sr >> 2);
		int a = 255;
		
		color_array[0].set(r, g, b, a);
		
		sr = co11.getR();
		sg = co11.getG();
		sb = co11.getB();
		
		b = (sb << 3) | (sb >> 2);
		g = (sg << 2) | (sg >> 4);
		r = (sr << 3) | (sr >> 2);
		
		color_array[1].set(r, g, b, a);
		
		// Four-color block: derive the other two colors.
		r = (2 * color_array[0].getR() + color_array[1].getR()) / 3;
		g = (2 * color_array[0].getG() + color_array[1].getG()) / 3;
		b = (2 * color_array[0].getB() + color_array[1].getB()) / 3;
		
		color_array[2].set(r, g, b, a);
		
		r = (2 * color_array[1].getR() + color_array[0].getR()) / 3;
		g = (2 * color_array[1].getG() + color_array[0].getG()) / 3;
		b = (2 * color_array[1].getB() + color_array[0].getB()) / 3;
		
		color_array[3].set(r, g, b, a);
	}
	
	public void decodeBlock(ColorBlock block){
		int[] color_array = new int[4];
		evaluatePalette(color_array);
		
		// Write color block.
	    for( int j = 0; j < 4; j++ ) {
	        for( int i = 0; i < 4; i++ ) {
	        	int row = (indices >> j * 8) & 0xFF;
	            int idx = (row >> (2 * i)) & 3;
//	            block->color(i, j) = color_array[idx];
	            block.set(i, j, color_array[idx]);
	        }
	    }    
	}
	
	public void setIndices(int[] idx){
		indices = 0;
		for(int i = 0; i < 16; i++) {
	        indices |= (idx[i] & 3) << (2 * i);
	    }
	}
	
}

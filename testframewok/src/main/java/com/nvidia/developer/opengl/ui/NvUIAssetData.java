//----------------------------------------------------------------------------------
// File:        NvUIAssetData.java
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import jet.opengl.postprocessing.util.FileUtils;

final class NvUIAssetData {
	
	private static final Class<NvUIAssetData> clazz = NvUIAssetData.class;
	
	private static boolean inited = false;
	static final int staticAssetCount = 26;
    static final byte[][] staticAssetData = new byte[staticAssetCount][];
    static final String[] staticAssetNames = new String[staticAssetCount];
    static final int[]    staticAssetLen  = new int[staticAssetCount];
	
	static{
		init();
	}
	
	static synchronized void init(){
		if(inited)
			return;
		
		inited = true;
		String packName = clazz.getName();
		packName = packName.replace("NvUIAssetData", "");
		packName = packName.replace('.', '/');
		
//		BufferedReader in = new BufferedReader(new InputStreamReader(clazz.getClassLoader().getResourceAsStream(packName + "NvUIAssetData.h")));

		byte[] data = null;
	    
		int cursor = 0;
		int i = 0;
		String line;
		
		try (BufferedReader in = new BufferedReader(new InputStreamReader(FileUtils.open("shader_libs/NvUIAssetData.h")))){
			while((line = in.readLine()) != null){
				line = line.trim();
				
				if(line.length() == 0 || line.startsWith("//") || line.startsWith("/*"))
					continue;
				
				if(line.startsWith("const")){
					if(line.lastIndexOf(';') != -1){ // const long int %s = %d;
						String[] strs = line.split(" ");
						staticAssetLen[cursor] = Integer.parseInt(strs[5].replace(";", ""));
						data = new byte[staticAssetLen[cursor]];
						staticAssetData[cursor] = data;
					}else{ // const unsigned char %s[%d] = {
						String[] strs = line.split(" ");
						staticAssetNames[cursor] = strs[3].substring(0, strs[3].indexOf('['));
					}
				}else{
					if(line.equals("};")){
						if(i != staticAssetLen[cursor]){
							System.err.println("missing some data, the " + cursor + "th data's size is " + i +", less than " + staticAssetLen[cursor]);
						}
						cursor ++;
						i = 0;
					}else{
						String[] strs = line.split(",");
						for(int j = 0; j < strs.length; j++){
							if(strs[j].length() == 0)
								continue;
							
							data[i++] = (byte) Integer.parseInt(strs[j]);
						}
					}
				}
			}
			
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public static byte[] embeddedAssetLookup(String filename){
		for(int i = 0; i < NvUIAssetData.staticAssetCount; i++){
			if(filename.equals(NvUIAssetData.staticAssetFilenames[i])){
				return NvUIAssetData.staticAssetData[i];
			}
		}
		
		return null;
	}
	
	public static void main(String[] args) {
		NvUIAssetData.init();
	}
	
	static final String[] staticAssetFilenames = {
		"Courier-w-bold-24.dds",
	    "RobotoCond-w-bold-24.dds",
	    "arrow_blue.dds",
	    "arrow_blue_down.dds",
	    "arrow_blue_left.dds",
	    "arrow_pressed_down.dds",
	    "btn_box_blue.dds",
	    "btn_box_pressed_x.dds",
	    "btn_round_blue.dds",
	    "btn_round_pressed.dds",
	    "button_top_row.dds",
	    "button_top_row_locked.dds",
	    "button_top_row_pressed.dds",
	    "frame_thin.dds",
	    "frame_thin_dropshadow.dds",
	    "icon_button_highlight_small.dds",
	    "info_text_box_thin.dds",
	    "popup_frame.dds",
	    "rounding.dds",
	    "slider_empty.dds",
	    "slider_full.dds",
	    "slider_thumb.dds",
	    "Courier-24.fnt",
	    "Courier-Bold-24.fnt",
	    "RobotoCondensed-Bold-24.fnt",
	    "RobotoCondensed-Regular-24.fnt",
	};
	
	private NvUIAssetData(){}
}

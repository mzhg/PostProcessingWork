//----------------------------------------------------------------------------------
// File:        NvUtils.java
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

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class NvUtils {
	
	public static boolean BTN_SUPPORTS_HOVER = false;
	public static boolean later = false;
	public static boolean SHOW_PRESSED_BUTTON = false;
	
	/** A big bold constant to make pure virtual methods more visible in code. */
	static final int NV_PURE_VIRTUAL = 0;
	
	private static final float[] QUTES = new float[16];
	static{
		float v = 1.0f;
		for(int i = 0; i < 16; i++){
			QUTES[i] = v;
			v *= 10.0f;
		}
	}

	
	public static float toPrecisice(float v, int precision){
		if(precision >= 8)
			return v;
		if(precision < 0)
			throw new IllegalArgumentException("precision must be >= 0. precision = " + precision);
		
		double q = QUTES[precision];
		return (float) (Math.floor(v * q + 0.5)/q);
	}
	
	public static String formatPercisice(float v, int precision){
		if(precision < 0)
			throw new IllegalArgumentException("precision must be >= 0. precision = " + precision);
		
		String pattern = "%." + Integer.toString(precision) + 'f';
		return String.format(pattern, v);
	}
	
	public static String formatPercisice(double v, int precision){
		if(precision < 0)
			throw new IllegalArgumentException("precision must be >= 0. precision = " + precision);
		
		String pattern = "%." + Integer.toString(precision) + 'f';
		return String.format(pattern, v);
	}
	
	public static Method getMethod(Object obj, String name, Class<?>... args) {
		Method method = null;

		for (Class<?> clazz = obj.getClass(); clazz != Object.class; clazz = clazz
				.getSuperclass()) {
			try {
				method = clazz.getDeclaredMethod(name, args);

				if (method != null)
					return method;
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				// don't do anything here.
			}
		}

		System.err.printf("No such method named '%s' in the class %s.\n", name, obj
				.getClass().getName());
		return null;
	}

	public static Field getField(Object obj, String filedName) {
		Class<? extends Object> objectType = obj.getClass();

		boolean found = false;

		while (objectType != null) {
			for (Field f : objectType.getDeclaredFields())
				if (f.getName().equals(filedName)) {
					found = true;
					break;
				}

			if (found)
				break;
			else
				objectType = objectType.getSuperclass();
		}

		if (found) {
			try {
				Field field = objectType.getDeclaredField(filedName);

				try {
					field.setAccessible(true);
					return field;
				} catch (java.security.AccessControlException e) {
					e.printStackTrace();
				}
			} catch (NoSuchFieldException e) {
				e.printStackTrace();
			}
		} else {
			System.err.println("No such field named '" + filedName
					+ "' in the class " + obj.getClass().getName());
		}
		return null;
	}

	public static String[] generateGetterAndSetterName(String fieldName) {
		char first = fieldName.charAt(0);

		String getter = "get" + Character.toUpperCase(first)
				+ fieldName.substring(1);
		String setter = "set" + Character.toUpperCase(first)
				+ fieldName.substring(1);

		return new String[] { getter, setter };
	}
}

//----------------------------------------------------------------------------------
// File:        NvStopWatch.java
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


public class NvStopWatch {

	protected boolean m_running;
	
	private long start_time;
	private long end_time;
	
	/** Time difference between the last start and stop */
	private float diff_time;
	/** tick frequency */
	private double freq;
	
	/** Test construct */
	public NvStopWatch(){
		freq = 1000;
	}
	
	/** Starts time measurement */
	public void start(){
		start_time = _getTime();
		m_running = true;
	}
	
	/** Stop time measurement */
	public void stop(){
		end_time = _getTime();
		diff_time = (float)(((double) end_time - (double) start_time) / freq);
	}
	
	/** Reset time counters to zero */
	public void reset(){
		diff_time = 0;
		if(m_running)
			start_time = _getTime();
	}
	
	private final long _getTime(){
		return System.currentTimeMillis();
	}
	
	/**
	 * Get elapsed time<p>
	 * Time in seconds after start. If the stop watch is still running (i.e. there
     * was no call to #stop()) then the elapsed time is returned, otherwise the
     * summed time between all {@link #start()} and {@link #stop()} calls is returned
	 * @return The elapsed time in seconds
	 */
	public float getTime(){
		if(m_running)
			return getDiffTime();
		else
			return diff_time;
	}
	
	/** Get difference between start time and current time */
	private float getDiffTime(){
		long temp = _getTime();
		return (float)  (((double) temp - (double) start_time) / freq);
	}
	
	/**
	 * Test whether the timer is running
	 * @return true if the timer is running (between {@link #start()} and {@link #stop()} calls) and false if not
	 */
	public boolean isRunning(){return m_running;};
}

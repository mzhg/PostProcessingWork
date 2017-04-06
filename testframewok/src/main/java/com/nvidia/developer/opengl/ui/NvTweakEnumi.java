package com.nvidia.developer.opengl.ui;

public class NvTweakEnumi {

	public String m_name;
	public int m_value;
	
	public NvTweakEnumi() {
	}
	
	public NvTweakEnumi(String m_name, int m_value) {
		this.m_name = m_name;
		this.m_value = m_value;
	}

	public NvTweakEnumi(NvTweakEnumi o) {
		this.m_name = o.m_name;
		this.m_value = o.m_value;
	}
	
	public int get(){
		return m_value;
	}
	
	public void set(int v){
		m_value = v;
	}
	
	public String getName(){
		return m_name;
	}
	
	public void setName(String name){
		m_name = name;
	}
}

package com.nvidia.developer.opengl.ui;

public class NvTweakEnumf {

	public String m_name;
	public float m_value;
	
	public NvTweakEnumf() {
	}
	
	public NvTweakEnumf(String m_name, float m_value) {
		this.m_name = m_name;
		this.m_value = m_value;
	}

	public NvTweakEnumf(NvTweakEnumf o) {
		this.m_name = o.m_name;
		this.m_value = o.m_value;
	}

	public float get(){
		return m_value;
	}
	
	public void set(float v){
		m_value = v;
	}
	
	public String getName(){
		return m_name;
	}
	
	public void setName(String name){
		m_name = name;
	}
}

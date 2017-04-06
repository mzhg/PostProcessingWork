package com.nvidia.developer.opengl.ui;

public class NvTweakEnumbool {

	public String m_name;
	public boolean m_value;
	
	public NvTweakEnumbool() {
	}
	
	public NvTweakEnumbool(String m_name, boolean m_value) {
		this.m_name = m_name;
		this.m_value = m_value;
	}

	public NvTweakEnumbool(NvTweakEnumbool o) {
		this.m_name = o.m_name;
		this.m_value = o.m_value;
	}
	
	public boolean get(){
		return m_value;
	}
	
	public void set(boolean v){
		m_value = v;
	}
	
	public String getName(){
		return m_name;
	}
	
	public void setName(String name){
		m_name = name;
	}
}

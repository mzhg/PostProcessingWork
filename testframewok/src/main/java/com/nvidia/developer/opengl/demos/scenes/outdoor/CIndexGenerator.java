package com.nvidia.developer.opengl.demos.scenes.outdoor;

final class CIndexGenerator {

	private int m_iPitch;
	
	public CIndexGenerator(int pitch) {
		m_iPitch = pitch;
	}
	
	int indexOf(int iCol, int iRow ){
		return iCol + iRow*m_iPitch;
	}
}

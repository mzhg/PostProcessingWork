package com.nvidia.developer.opengl.ui;

public class AFontChar {
	public int m_idKey; // copy of our ascii/u8 index -- also the char lookup key
	public float m_x, m_y;
	public float m_width, m_height;
	public float m_xOff, m_yOff;
	public float m_xAdvance;
    public int m_pageID; // NVDHC: no plan to implement immediately
    public int m_channelIndex; // NVDHC: no plan to implement immediately
}

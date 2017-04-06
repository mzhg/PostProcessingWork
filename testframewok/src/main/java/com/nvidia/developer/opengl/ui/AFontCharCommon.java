package com.nvidia.developer.opengl.ui;

public class AFontCharCommon {

	public float m_lineHeight;
	public float m_baseline;
	public float m_pageWidth;
	public float m_pageHeight;
	public String m_filename;
    public int m_pageID;
    
    public void set(AFontCharCommon o){
    	m_lineHeight = o.m_lineHeight;
    	m_baseline = o.m_baseline;
    	m_pageWidth = o.m_pageWidth;
    	m_pageHeight = o.m_pageHeight;
    	m_filename = o.m_filename;
    	m_pageID = o.m_pageID;
    }
}

package com.nvidia.developer.opengl.ui;

import java.util.HashMap;

public class AFont {

	public final AFontInfo m_fontInfo = new AFontInfo();
	public final AFontCharCommon m_charCommon = new AFontCharCommon();
	public int m_charCount;
	public final HashMap<Integer, AFontChar> m_glyphs = new HashMap<Integer, AFontChar>();
}

package com.nvidia.developer.opengl.ui;

import com.nvidia.developer.opengl.utils.NvTokenizer;

public class AFontTokenizer extends NvTokenizer {

	// temp variable.
	private final AFontInfo finfo = new AFontInfo();
	private final AFontCharCommon fcommon = new AFontCharCommon();
	
	private final float[] tmpf = new float[1];
	private final boolean[] tmpb = new boolean[1];
	private final int[] tmpi = new int[1];
	
	public AFontTokenizer(CharSequence src) {
		super(src, null);
	}
	
	/**
	 * parse font info block<br>
	 * info face="RobotoCondensed-Light" size=36 bold=0 italic=0 charset="" unicode=1 stretchH=100 smooth=1 aa=2 padding=1,1,1,1 spacing=0,0 outline=0 
	 */
	public boolean parseAFontInfoBlock(AFontInfo finfo){
		if (null==mSrcBuf || pos == mSrcBuf.length() - 1)
            return false;
		
		if (!requireToken("info"))
            return false;
		if (!requireTokenDelim("face") || (finfo.m_name = getTokenString()) == null)
            return false;
		if (!requireTokenDelim("size") || !getTokenFloat(tmpf))
            return false;
		finfo.m_size = tmpf[0];
		
		if (!requireTokenDelim("bold") || !getTokenBool(tmpb))
            return false;
		finfo.m_isBold = tmpb[0];
		
		 if (!requireTokenDelim("italic") || !getTokenBool(tmpb))
	            return false;
		 finfo.m_isItalic = tmpb[0];
		 
		 if (!requireTokenDelim("charset") || (finfo.m_charset = getTokenString()) == null)
	            return false;
		 
		 if (!requireTokenDelim("unicode") || !getTokenBool(tmpb))
	            return false;
		 finfo.m_isUnicode = tmpb[0];
		 
		 if (!requireTokenDelim("stretchH") || !getTokenFloat(tmpf))
	            return false;
		 finfo.m_stretchHeight = tmpf[0];
		 
		// don't care about these values or errors on them.
	     requireTokenDelim("smooth"); getTokenFloat(tmpf);
	     requireTokenDelim("aa"); getTokenFloat(tmpf);
	  // read arrays....
        if (!requireTokenDelim("padding") || 4!=getTokenFloatArray(finfo.m_padding))
            return false;
        if (!requireTokenDelim("spacing") || 2!=getTokenFloatArray(finfo.m_spacing))
            return false;
        
        consumeToEOL();
        return true;
	}
	
	// parse common block
    // common lineHeight=42 base=33 scaleW=512 scaleH=204 pages=1 packed=0 alphaChnl=0 redChnl=0 greenChnl=0 blueChnl=0 page id=0 file="RobotoLight+Bold_36.png"
	public boolean parseAFontCommonBlock(AFontCharCommon fcommon){
		if (null==mSrcBuf || pos == mSrcBuf.length() - 1)
            return false;
		
		if (!requireToken("common"))
            return false;
		
		if (!requireTokenDelim("lineHeight") || !getTokenFloat(tmpf))
            return false;
		fcommon.m_lineHeight = tmpf[0];
		
        if (!requireTokenDelim("base") || !getTokenFloat(tmpf))
            return false;
        fcommon.m_baseline = tmpf[0];
        
        if (!requireTokenDelim("scaleW") || !getTokenFloat(tmpf))
            return false;
        fcommon.m_pageWidth = tmpf[0];
        
        if (!requireTokenDelim("scaleH") || !getTokenFloat(tmpf))
            return false;
        fcommon.m_pageHeight = tmpf[0];
        
     // don't care about these values or errors on them right now...
        requireTokenDelim("pages"); getTokenInt(tmpi);
        requireTokenDelim("packed"); getTokenInt(tmpi);
        requireTokenDelim("alphaChnl"); getTokenInt(tmpi);
        requireTokenDelim("redChnl"); getTokenInt(tmpi);
        requireTokenDelim("greenChnl"); getTokenInt(tmpi);
        requireTokenDelim("blueChnl"); getTokenInt(tmpi);

        consumeToEOL();
        
     // pages is a separate block really, but we're only using ONE PAGE per font,
        // one texture, so parse as part of common info.
        if (!requireToken("page"))
            return false;
        if (!requireTokenDelim("id") || !getTokenInt(tmpi))
            return false;
        fcommon.m_pageID = tmpi[0];
        
        if (!requireTokenDelim("file") || (fcommon.m_filename = getTokenString()) == null)
            return false;

        consumeToEOL();

        return true;
	}
	
	// parse a SINGLE char block
    // char id=10   x=19   y=31   width=0    height=0    xoffset=-1   yoffset=32   xadvance=7    page=0    chnl=0
	public boolean parseAFontChar(AFontChar fchar){
		if (null==mSrcBuf || pos == mSrcBuf.length() - 1)
            return false;
		
		//int32_t tmpi; // so we can eat values we don't care about.
        if (!requireToken("char"))
            return false;
        if (!requireTokenDelim("id") || !getTokenInt(tmpi))
            return false;
        fchar.m_idKey = tmpi[0];
        
        if (!requireTokenDelim("x") || !getTokenFloat(tmpf))
            return false;
        fchar.m_x = tmpf[0];
        
        if (!requireTokenDelim("y") || !getTokenFloat(tmpf))
            return false;
        fchar.m_y = tmpf[0];
        
        if (!requireTokenDelim("width") || !getTokenFloat(tmpf))
            return false;
        fchar.m_width = tmpf[0];
        
        if (!requireTokenDelim("height") || !getTokenFloat(tmpf))
            return false;
        fchar.m_height = tmpf[0];
        
        if (!requireTokenDelim("xoffset") || !getTokenFloat(tmpf))
            return false;
        fchar.m_xOff = tmpf[0];
        
        if (!requireTokenDelim("yoffset") || !getTokenFloat(tmpf))
            return false;
        fchar.m_yOff = tmpf[0];
        
        if (!requireTokenDelim("xadvance") || !getTokenFloat(tmpf))
            return false;
        fchar.m_xAdvance = tmpf[0];

        if (!requireTokenDelim("page") || !getTokenInt(tmpi))
            return false;
        fchar.m_pageID = tmpi[0];
        
        if (!requireTokenDelim("chnl") || !getTokenInt(tmpi))
            return false;
        fchar.m_channelIndex = tmpi[0];

        // adjust for texture sampling NOW
        final float fgrowth = 0.5f;
        fchar.m_x -= fgrowth;
        fchar.m_y -= fgrowth;
        fchar.m_width += (fgrowth*2);
        fchar.m_height += (fgrowth*2);
        //fchar.m_xAdvance += fgrowth; //!!!!TBD TODO should adjust advance??
        
        // offset backward to account for the position changes above.
        fchar.m_xOff -= fgrowth; 
        fchar.m_yOff -= fgrowth; // offset backward for the position change?

        consumeToEOL();
        return true;
	}
	
	public AFont parseAFont(){
		if (null==mSrcBuf || pos >= mSrcBuf.length() - 1)
            return null;
		// clear all temp structures!!!!. !!!!TBD TODO
		
		// parse info block
        if (!parseAFontInfoBlock(finfo))
            return null;

        if (!parseAFontCommonBlock(fcommon))
            return null;
        
        // parse chars block
        // chars count=96
        int charCount;
        if (!requireToken("chars"))
            return null;
        if (!requireTokenDelim("count") || !getTokenInt(tmpi))
            return null;
        charCount = tmpi[0];
        consumeToEOL();
        
     // okay, we've gotten far enough to allocate and not worry about throwing away...
        AFont font = new AFont(); // so we can start storing stuff...
        font.m_fontInfo.set(finfo);
        font.m_charCommon.set(fcommon);
        font.m_charCount = charCount;

        for (int i=0; i<charCount; i++) {
        	 AFontChar fchar = new AFontChar();
            if (!parseAFontChar(fchar))
                break;
//            font.m_glyphs[fchar.m_idKey] = fchar;
              font.m_glyphs.put(fchar.m_idKey, fchar);
        }

        // IGNORING kerning for now
        // kernings count=0

        return font;
	}
}

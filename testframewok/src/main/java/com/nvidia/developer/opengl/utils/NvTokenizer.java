//----------------------------------------------------------------------------------
// File:        NvTokenizer.java
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

import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.postprocessing.util.StringUtils;


public class NvTokenizer {

	private static final int NV_MAX_TOKEN_LEN = 1024;
	private static final int NV_MAX_DELIM_COUNT = 16;
	
	private boolean mConsumeWS = true;
	
	/** This holds a pointer to an immutable char buffer. */
	protected CharSequence mSrcBuf;
	protected int pos;
	/** This is a temporary buffer for collecting next token. */
	protected char[] mTokBuf = new char[NV_MAX_TOKEN_LEN];
	/** This is the character length of current token */
	protected int mTokLen;
	/** This is the character that caused us to stop parsing the current token. */
	protected char mTermChar;
	
	protected char[] mDelims = new char[NV_MAX_DELIM_COUNT];
	protected int mNumDelims;
	
	public NvTokenizer(CharSequence src){
		this(src, null);
	}
	
	public NvTokenizer(CharSequence src, String delims){
		mSrcBuf = src;
		
		if(delims == null){
			mNumDelims = 3;
			mDelims[0] = '=';
			mDelims[1] = ',';
			mDelims[2] = ':';
		}else{
			// note that we DO support empty string, in which case
            // we have no delims other than whitespace...
            int i = 0;
            mNumDelims = 0;
            while (mNumDelims < NV_MAX_DELIM_COUNT && i < delims.length())
            {
                final char c = delims.charAt(i++);
                // else skip built-in handled chars
                if (c==' ' || c=='\t' || // Whitespace
                    c=='"' || c=='\'' || // Quote
                    c=='\n'|| c=='\r') // EOL
                    continue;
                mDelims[mNumDelims++] = c;
            }
		}
	}
	
	public void setConsumeWS(boolean ws) { mConsumeWS = ws; }

	public boolean isWhitespace(final char c)
    {
        return (' '==c || '\t'==c);
    }

	public boolean isQuote(final char c)
    {
        return ('"'==c || '\''==c);
    }

	public boolean isEOL(final char c)
    {
        return ('\n'==c || '\r'==c);
    }

	public boolean isTerm(final char c)
    {
        return (isWhitespace(c) || isEOL(c));
    }

	public boolean isDelim(final char c)
    { // TBD any other delims we want to call out.
        for (int i=0; i<mNumDelims; i++)
            if (c==mDelims[i]) return true;
        return false;
    }
	
	public boolean atEOF(){
		return pos == mSrcBuf.length();
	}
	
	private final char getChar(){
		return atEOF() ? 0 : mSrcBuf.charAt(pos);
	}
	
	private final char nextChar(){
		pos++;
		return atEOF() ? 0 : mSrcBuf.charAt(pos);
	}
	
	public char consumeWhitespace()
    {
        //VERBOSE_TOKEN_DEBUG("consuming whitespace");
        mTermChar = 0;
        char c = getChar();
        while (!atEOF() && isWhitespace(c)){
            mTermChar = c;
            c = nextChar();
        }
        return mTermChar;
    }

    public char consumeOneDelim()
    {
        //VERBOSE_TOKEN_DEBUG("consuming delimiter");
        consumeWhitespace();
        mTermChar = 0;
        // eat ONE delimiter...
        char c = getChar();
        if (!atEOF() && isDelim(c)) {
        	mTermChar = c;
            c = nextChar();
        }
        
        if (isEOL(c))
            mTermChar = c; // so that we return the EOL but DON'T CONSUME IT.
        return mTermChar;
    }

    public void consumeToEOL()
    {
    	char c = getChar();
        while (!atEOF() && !isEOL(c)) // eat up to the EOL
        {
            c = nextChar();
        }
        while (!atEOF() && isEOL(c)) // if not null, then eat EOL chars until gone, in case of /r/n type stuff...
        {
            c = nextChar();
        }
    }
    
    public boolean readToken(){
    	char startedWithQuote = 0; // we'll store the character if we get a quote
        mTermChar = 0;
        mTokLen = 0;
        mTokBuf[0] = 0; // initialize
        if (atEOF())
            return false; // exit early...

        if (mConsumeWS)
            consumeWhitespace();
        
        char c = getChar();
        if (!atEOF() && isQuote(c)) {
            startedWithQuote = c; // save WHAT quote character so we match it.
            c = nextChar();
        }

        while (!atEOF()) { // termchar is already null so fine to exit loop.
            // look for hard terminations (right now, EOL and whitespace)
            if (isTerm(c)) {
                mTermChar = c;
                break;
            }
            // look to handle quoted strings, which CAN have delimiters validly in their contents.
            if (startedWithQuote != 0) {
                if (startedWithQuote== c) {
                    // consume and break.
//                    mSrcBuf++;
                    c = nextChar();
                    mTermChar = startedWithQuote;
                    break;
                }
            } else if (isDelim(c)) {
                // just break, leave delim.
                mTermChar = c;
                break;
            }

            mTokBuf[mTokLen] = c; // copy char
            mTokLen++; // inc length
//            mSrcBuf++; // inc buffer
            c = nextChar();
        }

        // null-term buffer, do NOT inc length.
        mTokBuf[mTokLen] = 0;
//        VERBOSE_TOKLOG_VAL("  > got: %s", (mTokLen==0)?(atEOF()?"{{EOF}}":"{{empty}}"):mTokBuf);

        return (mTokLen>0 || startedWithQuote != 0); // false if empty string UNLESS quoted empty string...
    }
    
    public boolean requireToken(String find){
    	if(StringUtils.isEmpty(find))
    		return false;
    	
    	if(!readToken())
    		return false;
    	
    	// let's eliminate str functions...
        final int  findlen = find.length();
        if (findlen != mTokLen)
            return false; // early out.
        for(int i = 0; i < findlen; i++)
        	if(find.charAt(i) != mTokBuf[i])
        		return false;
        // accepted.
        return true;
    }
    
    public boolean requireTokenDelim(String find)
    {
        if (!requireToken(find))
            return false;
        if (consumeOneDelim() == 0)
            return false;
        // accepted.
        return true;
    }

    /// accessor to get character that caused 'stop' of last token read
    public char getTermChar() {
        return mTermChar;
    }

    /// accessor to get last read token const char *
    public String getLastToken()
    {
        return new String(mTokBuf, 0, mTokLen);
    }

    /// accessor to get last read token length
    public int getLastTokenLen()
    {
        return mTokLen;
    }

    public String getTokenString()
    {
        if (!readToken())
            return null;
        return new String(mTokBuf, 0, mTokLen);
    }

    
    /** get next token as a floating-point number */
    public boolean getTokenFloat(float[] out)
    {
        if (!readToken())
            return false;
        out[0] = Float.parseFloat(new String(mTokBuf, 0, mTokLen));
        return true;
    }
    
    /** get next tokens as array of floating point numbers */
    public int getTokenFloatArray(float[] out){
    	int i = 0;
    	int size = out.length;
    	char firstdelim = 0, delim = 0;
    	
        while (i < size) {
            if (firstdelim != 0) {
                // we had a delim initially, so we require (but can have repeated).
                delim = consumeOneDelim();
                if (delim==firstdelim)
                    continue;
            }
            if (!readToken())
                break; // so we return what we've got.
            out[i++] = Float.parseFloat(new String(mTokBuf, 0, mTokLen));

            // OPTIONALLY consume a delimiter between each number.
            delim = consumeOneDelim(); 
            if (delim != 0 && isEOL(delim)) // we didn't consume EOL, we are AT it though.
                break;
            if (i==1) firstdelim = delim; // stash away.
        }
        return i; // return number of elements read.
    }
    
    /** get next tokens as array of integer numbers */
    public int getTokenIntArray(int[] out){
    	int i = 0;
    	int size = out.length;
    	char firstdelim = 0, delim = 0;
    	
        while (i < size) {
            if (firstdelim != 0) {
                // we had a delim initially, so we require (but can have repeated).
                delim = consumeOneDelim();
                if (delim==firstdelim)
                    continue;
            }
            if (!readToken())
                break; // so we return what we've got.
            out[i++] = Integer.parseInt(new String(mTokBuf, 0, mTokLen));

            // OPTIONALLY consume a delimiter between each number.
            delim = consumeOneDelim(); 
            if (delim != 0 && isEOL(delim)) // we didn't consume EOL, we are AT it though.
                break;
            if (i==1) firstdelim = delim; // stash away.
        }
        return i; // return number of elements read.
    }
    
  /// get next token as an integer
    public boolean getTokenInt(int[] out)
    {
        if (!readToken())
            return false;
        out[0] = Integer.parseInt(new String(mTokBuf, 0, mTokLen));
        return true;
    }

    /// get next token as an unsigned integer
    public boolean getTokenUint(long[] out)
    {
        if (!readToken())
            return false;
        out[0] = Numeric.unsignedInt(Integer.parseInt(new String(mTokBuf, 0, mTokLen)));
        return true;
    }

    /// get next token as some form of boolean value/string
    public boolean getTokenBool(boolean[] out)
    {
        if (!readToken())
            return false;
        if (mTokLen==1 &&
            (mTokBuf[0]=='0' || mTokBuf[0]=='1') ) {
            out[0] = (mTokBuf[0]=='1');
            return true;
        } else if ( (0==strcmp(mTokBuf, mTokLen, "true")) ||
                    (0==strcmp(mTokBuf, mTokLen, "TRUE")) ||
                    (0==strcmp(mTokBuf, mTokLen, "yes")) ||
                    (0==strcmp(mTokBuf, mTokLen, "YES")) ) {
            out[0] = true;
            return true;
        } else if ( (0==strcmp(mTokBuf, mTokLen, "false")) ||
                    (0==strcmp(mTokBuf, mTokLen, "FALSE")) ||
                    (0==strcmp(mTokBuf, mTokLen, "no")) ||
                    (0==strcmp(mTokBuf, mTokLen, "NO")) ) {
            out[0] = false;
            return true;
        }
        // ... otherwise, no boolean value detected.
        return false;
    }
    
    private static int strcmp(char[] chars, int size, String str){
    	int len = str.length();
    	if(size != len)
    		return 1;
    	
    	for(int i = 0; i < len; i++)
    		if(chars[i] != str.charAt(i))
    			return 1;
    	
    	return 0;
    }
}

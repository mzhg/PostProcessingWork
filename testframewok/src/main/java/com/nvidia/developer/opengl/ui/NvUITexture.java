//----------------------------------------------------------------------------------
// File:        NvUITexture.java
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
package com.nvidia.developer.opengl.ui;

import com.nvidia.developer.opengl.utils.NvImage;

import java.io.IOException;
import java.util.Arrays;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.StringUtils;

/**
 * Abstraction of a GL texture object, allowing for texture caching, lookup, and reuse/refcounting.
 * In addition, it stores a set of 'knowledge' about the texture data loaded in the GL object,
 * including things like the source filename and dimensions.
 * @author Nvidia 2014-9-4
 */
public class NvUITexture implements Disposeable {
	
	/** Some random prime num picked for hashing. */
	private static final int NV_UITEX_HASHMAX = 19;
	/** The texture hash table.  Using chaining for filled slots. */
	private static NvUITexture[] ms_texTable = new NvUITexture[NV_UITEX_HASHMAX];

	/** Internally linked list. */
	private NvUITexture m_llnext;
	/** The requested bitmap filename, used in hashing and debugging. */
	protected String m_filename;
	/** The GL texture ID allocated for our texture upload. */
	protected int m_glID;
	/** The loaded texture width or height. */
	protected int m_width, m_height;
	/** Flag for whether this texture is a null object or was successfully loaded. */
	protected boolean m_validTex;
	/** Flag for if this texture has data with an alpha channel. */
	protected boolean m_hasAlpha;
	/** Flag if we own the GL texture ID/object, or if someone else does. */
	protected boolean m_ownsID;
	/** Trivial internal refcount of a given texture object. */ // !!!!TBD TODO use a real ref system? */
	protected int m_refcount = 1;
	/** Whether or not the texture is cached in our master NvUITexture table. */
	protected boolean m_cached;
	
	/** Constructor for texture loaded from filename; may load from/to the texture cache. */
	public NvUITexture(String texname){
		this(texname, true);
	}
	/** Constructor for texture loaded from filename; may load from/to the texture cache. */
	public NvUITexture(String texname, boolean noMips){
		m_filename = texname;
		if(StringUtils.isEmpty(m_filename)){
			throw new NullPointerException("NvUITexture:: Passed an empty texture filename");
		}

		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
		// TODO !!!!!TBD we shouldn't be loading in constructor, bad.  need to revise.
	    m_glID = 0;
	    NvImage image = null;
	    if (null == (image = loadEmbeddedTexture(texname)))
			try {
				image = NvImage.createFromDDSFile(texname);
			} catch (IOException e) {
				e.printStackTrace();
			}
		if (image != null)
	    {
	        m_width = image.getWidth();
	        m_height = image.getHeight();
	        m_hasAlpha = image.hasAlpha();

	        m_glID = image.updaloadTexture();
	        //TestPrintGLError("Error 0x%x after texture upload...\n");

	        // set up any tweaks to texture state here...
			gl.glActiveTexture(GLenum.GL_TEXTURE0);
			gl.glBindTexture(GLenum.GL_TEXTURE_2D, m_glID);
			gl.glTexParameterf(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_LINEAR);
			gl.glTexParameterf(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_LINEAR);
			gl.glTexParameterf(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_CLAMP_TO_EDGE);
			gl.glTexParameterf(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_CLAMP_TO_EDGE);
	        // then turn the bind back off!
			gl.glBindTexture(GLenum.GL_TEXTURE_2D, 0);
	    }

	    if (m_glID==0) // !!!!!TBD
	    {
	        throw new RuntimeException("NvUITexture: FAILED to load texture file: " + m_filename);
	    }
	    else
	    {
	        m_ownsID = true; // our call created the gl tex ID.
	        // !!!!TBD TODO this was set to FALSE??
	        m_validTex = true; // as if we got here, we loaded a texture...
	    }
	}
	
	/**
	 * Constructor for texture loaded from existing GL tex ID; texture cache is not involved.
	 */
	public NvUITexture(int texID, boolean alpha, int srcw, int srch, boolean ownsID, boolean isCubeMap){
		m_glID = texID;
		m_width = srcw;
		m_height = srch;
		m_validTex = true;
		m_hasAlpha = alpha;
		m_ownsID = ownsID;
		m_refcount = 1;
		m_cached = false;
	}
	
	static void staticCleanup(){
		Arrays.fill(ms_texTable, null);
	}
	
	/** Accessor for texture width. */
    public int getWidth() { return m_width; };
    /** Accessor for texture height. */
    public int getHeight() { return m_height; };
    /** Accessor for GL texture object ID. */
    public int getGLTex() { return m_glID; };
    /** Accessor for whether texture was flagged as having alpha channel. */
    public boolean getHasAlpha() { return m_hasAlpha; };
    /** Add reference to this texture. */
    public void addRef() {m_refcount++;}
    
    /** Subtract a reference from this texture/ */
    public void delRef(){
    	if (m_refcount == 0)
        {
//            NvLogger.i("NvUITexture refcount is already 0!");
            return;
        }
        m_refcount--;

        if (m_refcount==0)
        {
            if (m_cached)
            {
//            	NvLogger.e("Refcount reached 0 for a texture flagged as cached!!!");
            }
            
            dispose();
        }
        else if (m_cached && m_refcount == 1)
        {
            // cached textures got 1 more refcount so calling
            // DerefTexture when the counter reaches 1 is correct
            derefTexture();
        }
    }
    
    /** Handles internal dereferencing of texture objects, may include removal of entry from cache. */
    private boolean derefTexture() {
    	if (m_refcount==0) // we shouldn't be here
            return false;

        m_refcount--;
        if (m_refcount>0) // okay, we're still alive
            return true;

        // otherwise, we need to pull from list and REALLY delete. 
        
        // find existing texture in table
        if (StringUtils.isEmpty(m_filename)) // then something created by passing in a gl ID
        { // !!!!TBD is this the right thing to do?  need to look at use-case.
            dispose();
            return true;
        }
        else // created by loading from filename, should be in list.
        {
            // calculate some hash based on the filename string.
            int namehash = calculateNameHash(m_filename);
            // now we have hash, look in table slot, walk ll
            NvUITexture list = ms_texTable[namehash], follow = null;
            while (list != null)
            {
                // !!!!!TBD this isn't case insensitive, and SHOULD BE!!!
                // also possible we need full paths in order to ensure uniqueness.
                if (m_filename.equals(list.m_filename))
                {
                    // pull out of list.
                    if (follow==null) // head of slot
                        ms_texTable[namehash] = list.m_llnext;
                    else
                        follow.m_llnext = list.m_llnext;
                    // now we can safely, REALLY delete    
                    list.dispose();
                    return true; // all done.
                }
                follow = list;
                list = list.m_llnext;
            }
        }

        return false;
	}
	/** Static method to help calculate a hash-table value based on a texture name string. */
    private static int calculateNameHash(String texname){
    	// calculate some hash based on the filename string.
        int namehash = 0;
        int namelen = texname.length();
        for (int i=0; i<namelen; i++)
            namehash += texname.charAt(i);
        namehash = namehash % NV_UITEX_HASHMAX;
        return namehash;
    }
    
    /** Static method for loading textures via internal cache.
    First tries to find existing object in the cache.  If not found in the cache, tries to
    load from disk, and if successful then store in the cache for later load attempts.
    */
    public static NvUITexture cacheTexture(String texname, boolean noMips){
    	NvUITexture tex = null;
        
    	   if (StringUtils.isEmpty(texname))
    	        throw new NullPointerException("texname is empty");

    	    // find existing texture in table
    	    // calculate some hash based on the filename string.
    	    int namehash = calculateNameHash(texname);
    	    // now we have hash, look in table slot, walk ll
    	    NvUITexture list = ms_texTable[namehash];
    	    while (list != null)
    	    {
    	        // !!!!!TBD this isn't case insensitive, and SHOULD BE!!!
    	        // also possible we need full paths in order to ensure uniqueness.
    	        if (list.m_filename.equals(texname))
    	        {
    	            tex = list;
    	            break;
    	        }
    	        list = list.m_llnext;
    	    }
    	    
    	    // if not exist, create a new one
    	    if (tex==null)
    	    {
    	        tex = new NvUITexture(texname, noMips);
    	        if (!tex.m_filename.equals(texname))
    	        {
    	            tex.delRef();
    	            return null;
    	        }
    	            
    	        tex.m_cached = true;
    	        // then use calc'd hash info from above to insert into table.
    	        list = ms_texTable[namehash]; // get current head.
    	        tex.m_llnext = list; // move in front.
    	        ms_texTable[namehash] = tex; // take over head position.
    	    }

    	    tex.m_refcount++; // increment regardless of cached or allocated.
    	    return tex;
    }
    
    private static NvImage loadEmbeddedTexture(String filename){
    	byte[] data = NvUIAssetData.embeddedAssetLookup(filename);
    	
    	if(data != null){
    		NvImage image = new NvImage();
    		if(!image.loadImageFromFileData(data, 0, data.length, "dds")){
    			return null;
    		}
    		
    		return image;
    	}
    	
    	return null;
    }
    
    /** Dispose the GL texture associated to the NvUITexture. */
	@Override
	public void dispose() {
		// refcount owned outside of us...
	    if (0==m_refcount)
	    {
	        if (m_glID != 0 && m_ownsID)
	        { // if this is bound, we could be in bad place... !!!TBD
				GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
				gl.glDeleteTextures(m_glID);
	        }
	        m_glID = 0;        
	    }
	    else
	    {
//	        NvLogger.e("NvUITexture refcount is not 0!\n");
	    }
	}
}

//----------------------------------------------------------------------------------
// File:        NvGLModel.java
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
package com.nvidia.developer.opengl.models.obj;

import org.lwjgl.util.vector.Vector3f;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Drawable geometric model using GL VBOs. Supports loading from OBJ file data.
 * Contains positions as well as optional normals, UVs, and tangent vectors
 * @author Nvidia 2014-9-1
 *
 */
public class NvGLModel {

	private NvModel model;
	private int model_vboID, model_iboID;
	private final Vector3f m_minExtent = new Vector3f();
	private final Vector3f m_maxExtent = new Vector3f();
	private final Vector3f m_radius = new Vector3f();
	
	public final Vector3f m_center = new Vector3f();
	private GLFuncProvider gl;
	
	/**
	 * Initialize internal model with passed in ptr. You should call this method in OpenGL context.
	 * @param model pointer to an NvModel to use for mesh data.
	 */
	public NvGLModel(NvModel model) {
		this.model = model;
		gl = GLFuncProviderFactory.getGLFuncProvider();
		
		model_vboID = gl.glGenBuffer();
		model_iboID = gl.glGenBuffer();
		
		if(model == null){
			this.model = new NvModel();
		}
	}
	/**
	 * Initialize internal model. You should call this method in OpenGL context.
	 */
	public NvGLModel() {
		this(null);
	}
	
	public void dispose(){
		gl.glDeleteBuffer(model_vboID);
		gl.glDeleteBuffer(model_iboID);
	}
	
	/**
	 * Loads a model from OBJ-formatted file.
	 * @param filename
	 */
	public void loadModelFromFile(String filename){
		model.loadModelFromFile(filename);
		
		computeCenter();
	}
	
	public void computeCenter(){
		model.computeBoundingBox(m_minExtent, m_maxExtent);
		((Vector3f)Vector3f.sub(m_maxExtent, m_minExtent, m_radius)).scale(0.5f);
		Vector3f.add(m_minExtent, m_radius, m_center);
	}
	
	/**
	 * Rescales the model geometry and centers it around the origin.  Does NOT update 
	 * the vertex buffers.  Applications should update the VBOs via #initBuffers
	 * @param radius the desired new radius.  The model geometry will be rescaled to
	 *  fit this radius
	 */
	public void rescaleModel(float radius){
		model.rescaleToOrigin(radius);
	}
	
	/** Initialize or update the model geometry VBOs
	 * @see #initBuffers(boolean) */
	public void initBuffers(){
		initBuffers(false);
	}
	
	/**
	 * Initialize or update the model geometry VBOs
	 * @param computeTangents if set to true, then tangent vectors will be computed
     * to be in the S texture coordinate direction.  This may require vertices to be
     * duplicated in order to allow multiple tangents at a point.  This can cause model
     * size explosion, and should be done only if required.
	 */
	public void initBuffers(boolean computeTangents){
		model.computeNormals();
		
		if(computeTangents)
			model.computeTangents();
		
		model.compileModel(NvModel.TRIANGLES);

		gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, model_vboID);
		gl.glBufferData(GLenum.GL_ARRAY_BUFFER, CacheBuffer.wrap(model.getCompiledVertices(), 0, model.getCompiledVertexCount() * model.getCompiledVertexSize()), GLenum.GL_STATIC_DRAW);
		gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);

		gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, model_iboID);
		gl.glBufferData(GLenum.GL_ELEMENT_ARRAY_BUFFER, CacheBuffer.wrap(model.getCompiledIndices(NvModel.TRIANGLES), 0, model.getCompiledIndexCount(NvModel.TRIANGLES)), GLenum.GL_STATIC_DRAW);
		gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);
	}
	
	private void bindBuffers(){
		gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, model_vboID);
		gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, model_iboID);
	}
	
	private void unbindBuffers()
	{
		gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
		gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);
	}

	/**
	 * Draw the model using the current shader (positions)<p>
	 * Binds the vertex position array to the given attribute array index and draws the
	 * model with the currently bound shader.
	 * @param positionHandle the vertex attribute array index that represents position in the current shader
	 */
	public void drawElements(int positionHandle)
	{
		drawElements(positionHandle, -1, -1, -1);
	}

	/**
	 * Draw the model using the current shader (positions and normals)<p>
	 * Binds the vertex position and normal arrays to the given attribute array indices and draws the
	 * model with the currently bound shader.
	 * @param positionHandle the vertex attribute array index that represents position in the current shader
	 * @param normalHandle the vertex attribute array index that represents normals in the current shader
	 */
	public void drawElements(int positionHandle, int normalHandle)
	{
		drawElements(positionHandle, normalHandle, -1, -1);
	}

	/**
	 * Draw the model using the current shader (positions, UVs and normals)<p>
	 * Binds the vertex position, UV and normal arrays to the given attribute array indices and draws the
	 * model with the currently bound shader.
	 * @param positionHandle the vertex attribute array index that represents position in the current shader
	 * @param normalHandle the vertex attribute array index that represents normals in the current shader
	 * @param texcoordHandle the vertex attribute array index that represents UVs in the current shader
	 */
	public void drawElements(int positionHandle, int normalHandle, int texcoordHandle)
	{
		drawElements(positionHandle, normalHandle, texcoordHandle, -1);
	}

	/**
	 * Draw the model using the current shader (positions, UVs, normals and tangents)<p>
	 * Binds the vertex position, UV, normal and tangent arrays to the given attribute array indices and draws the
	 * model with the currently bound shader.
	 * @param positionHandle the vertex attribute array index that represents position in the current shader
	 * @param normalHandle the vertex attribute array index that represents normals in the current shader
	 * @param texcoordHandle the vertex attribute array index that represents UVs in the current shader
	 * @param tangentHandle the vertex attribute array index that represents tangents in the current shader
	 */
	public void drawElements(int positionHandle, int normalHandle, int texcoordHandle, int tangentHandle)
	{
	    bindBuffers();
		gl.glVertexAttribPointer(positionHandle, model.getPositionSize(), GLenum.GL_FLOAT, false, model.getCompiledVertexSize() * 4, 0);
		gl.glEnableVertexAttribArray(positionHandle);

	    if (normalHandle >= 0) {
			gl.glVertexAttribPointer(normalHandle, model.getNormalSize(), GLenum.GL_FLOAT, false, model.getCompiledVertexSize() * 4, (model.getCompiledNormalOffset()*4));
			gl.glEnableVertexAttribArray(normalHandle);
	    }

	    if (texcoordHandle >= 0) {
			gl.glVertexAttribPointer(texcoordHandle, model.getTexCoordSize(), GLenum.GL_FLOAT, false, model.getCompiledVertexSize() * 4, (model.getCompiledTexCoordOffset()*4));
			gl.glEnableVertexAttribArray(texcoordHandle);
	    }

	    if (tangentHandle >= 0) {
			gl.glVertexAttribPointer(tangentHandle,  model.getTangentSize(), GLenum.GL_FLOAT, false, model.getCompiledVertexSize() * 4, (model.getCompiledTangentOffset()*4));
			gl.glEnableVertexAttribArray(tangentHandle);
	    }
		gl.glDrawElements(GLenum.GL_TRIANGLES, model.getCompiledIndexCount(NvModel.TRIANGLES), GLenum.GL_UNSIGNED_INT, 0);

		gl.glDisableVertexAttribArray(positionHandle);
	    if (normalHandle >= 0)
			gl.glDisableVertexAttribArray(normalHandle);
	    if (texcoordHandle >= 0)
			gl.glDisableVertexAttribArray(texcoordHandle);
	    if (tangentHandle >= 0)
			gl.glDisableVertexAttribArray(tangentHandle);
	    unbindBuffers();
	}

	/**
	 * Get the low-level geometry data.
	 * @return the underlying geometry model data instance
	 */
	public NvModel getModel()
	{
	    return model;
	}
	
	/** return a reference to the minExtent */
	public Vector3f getMinExt(){
		return m_minExtent;
	}
	
	/** return a reference to the maxExtent */
	public Vector3f getMaxExt(){
		return m_maxExtent;
	}
}

//----------------------------------------------------------------------------------
// File:        NvGLUtils/NvMeshExtGL.h
// SDK Version: v3.00 
// Email:       gameworks@nvidia.com
// Site:        http://developer.nvidia.com/
//
// Copyright (c) 2014-2015, NVIDIA CORPORATION. All rights reserved.
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

import org.lwjgl.util.vector.Matrix4f;

import java.util.List;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.CacheBuffer;

/** OpenGL geometric mesh data handling and rendering */
public class NvMeshExtGL implements Disposeable {
	

	private SubMesh m_pSrcMesh;

    // Index of the material used by this mesh
    private int m_materialID = -1;

    // GL IDs of the buffer objects
    private int m_vboID;
    private int m_iboID;
    private int m_indexCount;

    private int m_vertexCount;
    private int m_vertexSize;       // in floats

    private int m_positionSize = 3;     // in floats
    private int m_positionOffset;   // in bytes

    private int m_normalSize = 3;       // in floats
	private int m_normalOffset= -1;     // in bytes

    private int m_texCoordSize = 2;     // in floats
	private int m_texCoordOffset = -1;   // in bytes

    private int m_tangentSize = 3;      // in floats
	private int m_tangentOffset =-1;    // in bytes

	private int m_colorSize;        // in floats
	private int m_colorOffset = -1;      // in bytes

    private int m_parentNode = -1;
	private final Matrix4f m_offsetMatrix = new Matrix4f();
	private int m_vertexAttribCount;
	private GLFuncProvider gl;
	
	public NvMeshExtGL(){}
	
	public NvMeshExtGL( NvMeshExtGL other){
		m_pSrcMesh=(other.m_pSrcMesh);
		m_materialID=(other.m_materialID);
		m_vboID=(other.m_vboID);
		m_iboID=(other.m_iboID);
		m_indexCount=(other.m_indexCount);
		m_vertexCount=(other.m_vertexCount);
		m_vertexSize=(other.m_vertexSize);
		m_positionSize=(other.m_positionSize);
		m_positionOffset=(other.m_positionOffset);
		m_normalSize=(other.m_normalSize);
		m_normalOffset=(other.m_normalOffset);
		m_texCoordSize=(other.m_texCoordSize);
		m_texCoordOffset=(other.m_texCoordOffset);
		m_tangentSize=(other.m_tangentSize);
		m_tangentOffset=(other.m_tangentOffset);
		m_colorSize=(other.m_colorSize);
		m_colorOffset=(other.m_colorOffset);
        m_parentNode=(other.m_parentNode);
        m_offsetMatrix.load(other.m_offsetMatrix);
	}

    /// Returns the ID of the material used by this mesh.  The apps can map this to
	/// actual material info with #NvModelExtGL::GetMaterial
	/// \return index of the current mesh's material in the overall model's material list
	public int GetMaterialID() { return m_materialID; }

	/// \privatesection
	// Initialize mesh data from the given sub-mesh in the model
	public boolean InitFromSubmesh(NvModelExt pModel, int subMeshID){
		if (null == pModel)
        {
            return false;
        }

        SubMesh pSubMesh = pModel.GetSubMesh(subMeshID);
        if (null == pSubMesh)
        {
            return false;
        }

		gl = GLFuncProviderFactory.getGLFuncProvider();
		m_pSrcMesh = pSubMesh;

		m_materialID = m_pSrcMesh.m_materialId;

		m_vertexSize = m_pSrcMesh.m_vertSize; // In Floats

		// We will always have positions in our vertex, and we will put 
		// them first in the vertex layout
		m_positionOffset = 0;

		// Account for normals, if there are any
		m_normalOffset = m_pSrcMesh.m_normalOffset * 4;

		// Account for texture coordinates, if there are any
		m_texCoordOffset = m_pSrcMesh.m_texCoordOffset * 4;

		// Account for tangents, if there are any
		m_tangentOffset = m_pSrcMesh.m_tangentOffset * 4;

		// Account for colors, if there are any
		m_colorOffset = m_pSrcMesh.m_colorOffset * 4;

		int vertBytes = 4 * m_vertexSize;

		// Allocate a large enough vertex buffer to hold all vertices in the mesh
		m_vertexCount = m_pSrcMesh.m_vertexCount;

        m_parentNode = m_pSrcMesh.m_parentBone;

		// Create a vertex buffer and fill it with data
		int vboBytes = vertBytes * m_vertexCount;
		
		if(vboBytes != m_pSrcMesh.m_vertices.length * 4)
			throw new IllegalArgumentException();

        // Set up the GL data structures necessary for the vertex buffer object
		m_vboID = gl.glGenBuffer();
		gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, m_vboID);
		gl.glBufferData(GLenum.GL_ARRAY_BUFFER, CacheBuffer.wrap(m_pSrcMesh.m_vertices), GLenum.GL_STATIC_DRAW);
		gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);

        // Allocate a large enough index buffer to hold the indices for all primitives in the mesh
		m_indexCount = m_pSrcMesh.m_indexCount;
		
		if(m_indexCount != m_pSrcMesh.m_indices.length)
			throw new IllegalArgumentException();
        
        // Set up the GL data structures necessary for the index buffer object
		m_iboID = gl.glGenBuffer();
		gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, m_iboID);
		gl.glBufferData(GLenum.GL_ELEMENT_ARRAY_BUFFER, CacheBuffer.wrap(m_pSrcMesh.m_indices), GLenum.GL_STATIC_DRAW);
		gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);
        return true;
	}

	public SubMesh GetSubMesh() { return m_pSrcMesh; }

	public void DrawElements(int instanceCount, int positionHandle, int normalHandle /*= -1*/, int texcoordHandle /*= -1*/, int tangentHandle /*= -1*/){
		int vertexSizeInBytes = m_vertexSize * 4;
		gl.glBindVertexArray(0);
        // Bind our vertex/index buffers
		gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, m_vboID);
		gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, m_iboID);

		gl.glVertexAttribPointer(positionHandle, m_positionSize, GLenum.GL_FLOAT, false, vertexSizeInBytes, m_positionOffset);
		gl.glEnableVertexAttribArray(positionHandle);

        if (normalHandle >= 0) {
			gl.glVertexAttribPointer(normalHandle, m_normalSize, GLenum.GL_FLOAT, false, vertexSizeInBytes, m_normalOffset);
			gl.glEnableVertexAttribArray(normalHandle);
        }

        if (texcoordHandle >= 0) {
			gl.glVertexAttribPointer(texcoordHandle, m_texCoordSize, GLenum.GL_FLOAT, false, vertexSizeInBytes, m_texCoordOffset);
			gl.glEnableVertexAttribArray(texcoordHandle);
        }

        if (tangentHandle >= 0) {
			gl.glVertexAttribPointer(tangentHandle, m_tangentSize, GLenum.GL_FLOAT, false, vertexSizeInBytes, m_tangentOffset);
			gl.glEnableVertexAttribArray(tangentHandle);
        }

		if (instanceCount >= 1 /*&& (NvGLInstancingSupport::glDrawElementsInstancedInternal != nullptr)*/)
        {
//            NvGLInstancingSupport::glDrawElementsInstancedInternal(GL_TRIANGLES, m_indexCount, GL_UNSIGNED_INT, nullptr, instanceCount);
			gl.glDrawElementsInstanced(GLenum.GL_TRIANGLES, m_indexCount, GLenum.GL_UNSIGNED_INT, 0, instanceCount);
        }
        else
        {
			gl.glDrawElements(GLenum.GL_TRIANGLES, m_indexCount, GLenum.GL_UNSIGNED_INT, 0);
        }

        // Unbind our vertex/index buffers
		gl.glDisableVertexAttribArray(positionHandle);
        if (normalHandle >= 0)
			gl.glDisableVertexAttribArray(normalHandle);
        if (texcoordHandle >= 0)
			gl.glDisableVertexAttribArray(texcoordHandle);
        if (tangentHandle >= 0)
			gl.glDisableVertexAttribArray(tangentHandle);

        // Unbind our vertex/index buffers
		gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
		gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);
	}
	
	public void DrawElements(int instanceCount, int positionHandle){
		DrawElements(instanceCount, positionHandle, -1, -1, -1);
	}

	/// Returns the model-relative transform of the mesh
	public Matrix4f GetMeshOffset() { return m_offsetMatrix; }

    /// Copy the current transforms from bones in the given skeleton
	/// to the internal bone array in preparation for rendering
	/// \param pSrcSkel Pointer to the skeleton containing the current
	///                 transforms for all bones used by the mesh
	/// \return True if the mesh's transforms could be updated from 
	///         the given skeleton, false if they could not.
	public boolean UpdateBoneTransforms(NvSkeleton pSrcSkel){
		if ((null == m_pSrcMesh) || (null == pSrcSkel))
        {
            return false;
        }

        // Update the mesh offset from its parent node
        final List<Matrix4f> pSrcTransforms = pSrcSkel.GetTransforms();
        if ((m_parentNode >= 0) && (m_parentNode < pSrcSkel.GetNumNodes()))
        {
            m_offsetMatrix.load(pSrcTransforms.get(m_parentNode));
        }
        return true;
	}

    // Draw the mesh, using the given handles for vertex elements

    // Returns a bool indicating whether or not the mesh's vertices contain normals
	public boolean HasNormals() { return (m_normalOffset >= 0); }

    // Returns a bool indicating whether or not the mesh's vertices contain texture coordinates
	public boolean HasTexCoords() { return (m_texCoordOffset >= 0); }

    // Returns a bool indicating whether or not the mesh's vertices contain tangent vectors
	public boolean HasTangents() { return (m_tangentOffset >= 0); }

    // Mesh Format Inspection Accessors (for advanced rendering)
    
    /// Get the index count
    /// \return the index count
	public int GetIndexCount() { return m_indexCount; }    
    
    /// Get the vertex count
    /// \return the vertex count
	public int GetVertexCount() { return m_vertexCount; }

    /// Get the size of each vertex, in number of floats
    /// \return The size of the vertex in number of floats
	public int GetVertexSize() { return m_vertexSize; }

    /// Get the size of the Position component of the vertices
    /// \return The size of the Position component in number of floats.
    ///         A value of indicates that the vertices do not contain Position data
	public int GetPositionSize() { return m_positionSize; }

    /// Get the offset, in bytes, of the Position component within the
    /// vertex format.
    /// \return The offset, in bytes, of the Position component within
    ///         the vertex format.
	public int GetPositionOffset() { return m_positionOffset; }

    /// Get the size of the Normal component of the vertices
    /// \return The size of the Normal component in number of floats.
    ///         A value of indicates that the vertices do not contain Normal data
	public int GetNormalSize() { return m_normalSize; }

    /// Get the offset, in bytes, of the Normal component within the
    /// vertex format.
    /// \return The offset, in bytes, of the Normal component within
    ///         the vertex format.
	public int GetNormalOffset() { return m_normalOffset; }

    /// Get the size of the texture coordinate component of the vertices
    /// \return The size of the texture coordinate component in number of floats.
    ///         A value of indicates that the vertices do not contain texture coordinate data
	public int GetTexCoordSize() { return m_texCoordSize; }

    /// Get the offset, in bytes, of the texture coordinate component within the
    /// vertex format.
    /// \return The offset, in bytes, of the texture coordinate component within
    ///         the vertex format.
	public int GetTexCoordOffset() { return m_texCoordOffset; }

    /// Get the size of the Tangent component of the vertices
    /// \return The size of the Tangent component in number of floats.
    ///         A value of indicates that the vertices do not contain Tangent data
	public int GetTangentSize() { return m_tangentSize; }

    /// Get the offset, in bytes, of the Tangent component within the
    /// vertex format.
    /// \return The offset, in bytes, of the Tangent component within
    ///         the vertex format.
	public int GetTangentOffset() { return m_tangentOffset; }

    /// Get the size of the Color component of the vertices
    /// \return The size of the Color component in number of floats.
    ///         A value of indicates that the vertices do not contain Color data
	public int GetColorSize() { return m_colorSize; }

    /// Get the offset, in bytes, of the Color component within the
    /// vertex format.
    /// \return The offset, in bytes, of the Color component within
    ///         the vertex format.
	public int GetColorOffset() { return m_colorOffset; }
	
	private final void Clear(){
		if (m_vboID != 0)
        {
            gl.glDeleteBuffer(m_vboID);
            m_vboID = 0;
        }

        if (m_iboID != 0)
        {
			gl.glDeleteBuffer(m_iboID);
            m_iboID = 0;
        }

        m_indexCount = 0;
        m_vertexCount = 0;
        m_vertexSize = 0;
        m_positionSize = 0;
        m_positionOffset = 0;
        m_normalSize = 0;
        m_normalOffset = -1;
        m_texCoordSize = 0;
        m_texCoordOffset = -1;
        m_tangentSize = 0;
        m_tangentOffset = -1;
	}

	@Override
	public void dispose() {
		Clear();
	}
}

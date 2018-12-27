// File:        NvGLUtils/NvModelExtGL.h
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

import com.nvidia.developer.opengl.utils.NvImage;

import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;
import java.util.Arrays;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.FileUtils;
import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.postprocessing.util.StringUtils;

public class NvModelExtGL {

	// Pointer to the original source model that contains the data which the
	// GL model was derived from
	private NvModelExt m_pSourceModel;

	// Array of all sub-meshes within the model
	private NvMeshExtGL[] m_meshes = new NvMeshExtGL[0];

	// Array of all materials used by meshes within the model
	private NvMaterialGL[] m_materials = new NvMaterialGL[0];

    // Array of all textures used by meshes within the model
    private Texture2D[] m_textures = new Texture2D[0];

    // Shader binding locations
    private int m_diffuseTextureLocation;
    
    // A file folder that contains the textures.
    private static String m_texturePath = "";
    
    private NvModelExtGL(NvModelExt pSourceModel){
    	m_pSourceModel = pSourceModel;
    	m_diffuseTextureLocation = -1;
    }
    
    public static void setTexturePath(String path){ m_texturePath = StringUtils.safeStr(path);}
    public static String getTexturePath() { return m_texturePath;}
    
  /// Initialize or update the model geometry by processing the source model
	//// into GL renderable data
	/// \param[in] computeTangents if set to true, then tangent vectors will be computed
	/// to be in the S texture coordinate direction.  This may require vertices to be
	/// duplicated in order to allow multiple tangents at a point.  This can cause model
	/// size explosion, and should be done only if required.  If true, normals will also be
	/// computed, regardless of computeNormals value.
	/// \param[in] computeNormals if set to true, then normal vectors will be computed.
	private void PrepareForRendering(NvModelExt pModel){
		// Create GL textures of the referenced source files and keep a
		// mapping from texture index to the returned texture GL "name"
        int textureCount = m_pSourceModel.GetTextureCount();
        m_textures = Arrays.copyOf(m_textures, textureCount);
		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        for (int textureIndex = 0; textureIndex < textureCount; ++textureIndex)
        {
			String textureName = m_pSourceModel.GetTextureName(textureIndex);
			if(StringUtils.isBlank(textureName))
				continue;

			try {
				if(textureName.endsWith(".dds") || textureName.endsWith(".DDS")){
					int texID = NvImage.uploadTextureFromDDSFile(m_texturePath + textureName);
					gl.glGenerateTextureMipmap(texID);
					m_textures[textureIndex] = TextureUtils.createTexture2D(GLenum.GL_TEXTURE_2D, texID);
				}else{
					m_textures[textureIndex] = TextureUtils.createTexture2DFromFile(m_texturePath + textureName,false, true);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
 
		// Get GL usable versions of all the materials in the model
		int materialCount = pModel.GetMaterialCount();
		m_materials = Arrays.copyOf(m_materials, materialCount);
		if (materialCount > 0)
		{
			for (int materialIndex = 0; materialIndex < materialCount; ++materialIndex)
			{
				if(m_materials[materialIndex] == null)
					m_materials[materialIndex] = new NvMaterialGL();
				m_materials[materialIndex].InitFromMaterial(m_pSourceModel, materialIndex);
			}
		}

		// Get GL renderable versions of all meshes in the model
		int meshCount = pModel.GetMeshCount();
		m_meshes = Arrays.copyOf(m_meshes, meshCount);
		if (meshCount > 0)
		{
			for (int meshIndex = 0; meshIndex < meshCount; ++meshIndex)
			{
				m_meshes[meshIndex] = new NvMeshExtGL();
				m_meshes[meshIndex].InitFromSubmesh(pModel, meshIndex);
			}
		}
	}

    /// Sets state related to the given material, such as textures
    /// \param[in] materialId Index of the material to setup for rendering
	private void ActivateMaterial(int materialId){
		NvMaterialGL pMat = GetMaterial(materialId);
        if (null == pMat) { return; }
		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        if (m_diffuseTextureLocation >= 0)
        {
            int diffuseTextureIndex = pMat.m_diffuseTexture;
            if (diffuseTextureIndex >= 0)
            {
			    // We have been given a diffuse texture location in the shader and we have 
				// a diffuse texture to use, so bind it to the given location
                Texture2D texture = GetTexture(diffuseTextureIndex);
				gl.glActiveTexture(GLenum.GL_TEXTURE0 + m_diffuseTextureLocation);
				gl.glBindTexture(GLenum.GL_TEXTURE_2D, texture.getTexture());
            }
        }
	}
	
	/// Initialize internal model with passed in ptr
    /// \param[in] pSourceModel pointer to an NvModelExt to use for mesh data.  
    /// WARNING!!! This pointer is cached in the object, and must not be freed after this call returns WARNING!!!
	public static NvModelExtGL Create(NvModelExt pSourceModel){
		if (null == pSourceModel)
        {
            return null;
        }

		NvModelExtGL model = new NvModelExtGL(pSourceModel);
		model.PrepareForRendering(pSourceModel);
		return model;
	}

    /// Draw the model using the current shader (positions, UVs, normals and tangents)
    /// Binds the vertex position, UV, normal and tangent arrays to the given attribute array indices and draws the
    /// model with the currently bound shader. Use a negative value for a handle for any attributes that should
    /// not be bound for use in this draw call.
	/// \param[in] instanceCount if nonzero, draw using instancing and draw the given instance count.  If zero, use non-instanced rendering
	/// \param[in] positionHandle the vertex attribute array index that represents position in the current shader
	/// \param[in] normalHandle the vertex attribute array index that represents normals in the current shader
    /// \param[in] texcoordHandle the vertex attribute array index that represents UVs in the current shader
    /// \param[in] tangentHandle the vertex attribute array index that represents tangents in the current shader
	public void DrawElements(int instanceCount, int positionHandle, int normalHandle /*= -1*/, int texcoordHandle /*= -1*/, int tangentHandle /*= -1*/){
		int numMeshes = GetMeshCount();
		for (int meshIndex = 0; meshIndex < numMeshes; ++meshIndex)
		{
			NvMeshExtGL pMesh = GetMesh(meshIndex);
			int matId = pMesh.GetMaterialID();
            ActivateMaterial(matId);
            pMesh.DrawElements(instanceCount, positionHandle, normalHandle, texcoordHandle, tangentHandle);
		}
	}
	
	public final void DrawElements(int instanceCount, int positionHandle){
		DrawElements(instanceCount, positionHandle, -1, -1, -1);
	}
	
	public final void DrawElements(int instanceCount, int positionHandle, int normalHandle){
		DrawElements(instanceCount, positionHandle, normalHandle, -1, -1);
	}
	
	public final void DrawElements(int instanceCount, int positionHandle, int normalHandle, int texcoordHandle){
		DrawElements(instanceCount, positionHandle, normalHandle, texcoordHandle, -1);
	}

	/// Get the low-level geometry data.
	/// Returns the underlying geometry model data instance
	/// \return a pointer to the #NvModelExt instance that holds the client-memory data
	public NvModelExt GetModel() { return m_pSourceModel; }

	// Bounding box accessor methods
	/// Returns a vector containing the minimum coordinates of an axis-aligned bounding box for the model
	/// \return Point defining the minimum corner of the model's axis-aligned bounding box
	public Vector3f GetMinExt() { return m_pSourceModel.GetMinExt(); }

	/// Returns a vector containing the maximum coordinates of an axis-aligned bounding box for the model
	/// \return Point defining the maximum corner of the model's axis-aligned bounding box
	public Vector3f GetMaxExt() { return m_pSourceModel.GetMaxExt(); }

	/// Returns a vector containing the coordinates of the center of the axis-aligned bounding box for the model
	/// \return Point defining the center of the model's axis-aligned bounding box
	public Vector3f GetCenter() { return m_pSourceModel.GetCenter(); }

	// Sub-mesh access
	/// Returns the number of meshes that comprise the model
	/// \return Number of meshes contained in the model
	public int GetMeshCount() { return m_meshes.length; }

	/// Retrieves a mesh from the model
	/// \param[in] meshIndex Index of the mesh within the model to retrieve
	/// \return A pointer to the mesh at the requested index, if there is one.
	public NvMeshExtGL GetMesh(int meshIndex)
	{
		return m_meshes[meshIndex];
	}

	// Material Access
	/// Returns the number of materials used by the model
	/// \return Number of materials defined in the model
	public int GetMaterialCount() { return m_materials.length; }

	/// Retrieves a material from the model
	/// \param[in] materialIndex Index of the material within the model to retrieve
	/// \return A pointer to the material at the requested index, if there is one.
	public NvMaterialGL GetMaterial(int materialIndex)
	{
		return m_materials[materialIndex];
	}

    // Texture Access
    /// Returns the number of textures used by the model
    /// \return Number of textures defined in the model
    public int GetTextureCount() { return m_textures.length; }

    /// Retrieves a texture name from the model
    /// \param[in] textureIndex Index of the texture within the model to retrieve
    /// \return The GL Texture name for the texture at the requested index, if there is one.
    public Texture2D GetTexture(int textureIndex)
    {
        return m_textures[textureIndex];
    }
    
    /// Copy the current transforms from bones in the skeleton
    /// to contained meshes in preparation for rendering
    /// \return True if the mesh's transforms could be updated from 
    ///         the model's skeleton, false if they could not.
    public boolean UpdateBoneTransforms(){
    	boolean result = true;
    	
    	for(NvMeshExtGL meshIt : m_meshes){
    		result &= (meshIt).UpdateBoneTransforms(GetModel().GetSkeleton());
    	}
    	
    	return result;
    }

    // Very limited way to bind textures to shaders.  Use -1 to indicate no diffuse texture supported by shader.
    public void SetDiffuseTextureLocation(int textureLocation) { m_diffuseTextureLocation = textureLocation; }
}
